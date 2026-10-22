package com.luairan.service.paxoslease;

import com.alibaba.fastjson.JSON;
import com.luairan.service.context.*;
import com.luairan.service.websocket.WebSocketService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.annotation.*;
import java.net.URI;
import java.util.*;
import java.util.concurrent.*;

@Service
public class ProposerServiceImpl implements ProposerService {
    private final Logger log = LoggerFactory.getLogger(getClass());
    @Value("${paxos.peers:ws://localhost:8080/paxos}") private String configuredPeers;
    @Resource private WebSocketService webSocketService;
    @Resource private State state;
    private List<String> peers;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> task;
    private Proposer proposer;
    private long nextAttempt;
    private boolean running;
    @PostConstruct public void initialize() {
        Set<String> unique = new LinkedHashSet<>();
        for (String entry : configuredPeers.split(",")) {
            URI uri = URI.create(entry.trim()).normalize();
            if (!("ws".equals(uri.getScheme()) || "wss".equals(uri.getScheme())) || uri.getHost() == null || uri.getPort() < 1
                    || !"/paxos".equals(uri.getPath()) || uri.getQuery() != null || uri.getFragment() != null || uri.getUserInfo() != null)
                throw new IllegalArgumentException("Peers must be ws(s)://host:port/paxos");
            if (!unique.add(uri.toString())) throw new IllegalArgumentException("Duplicate peer");
        }
        peers = new ArrayList<>(unique);
    }
    public synchronized Proposer getCurrentProposer() { return proposer; }
    public synchronized void proposerStart() { proposer(); }
    public synchronized void proposer() {
        if (running) return;
        running = true; nextAttempt = state.now();
        task = scheduler.scheduleWithFixedDelay(this::tick, 0, 100, TimeUnit.MILLISECONDS);
    }
    private synchronized void tick() {
        if (!running || !state.isReady() || state.now() - nextAttempt < 0) return;
        try {
            if (proposer != null) proposer.cancel();
            proposer = new Proposer(state, peers);
            nextAttempt = state.now() + TimeUnit.MILLISECONDS.toNanos(3000 + ThreadLocalRandom.current().nextInt(1000));
            broadcast(proposer.propose());
        } catch (RuntimeException e) { log.error("Election attempt failed", e); }
    }
    public synchronized void receive(String peer, Response response) {
        if (!running || proposer == null || !peers.contains(peer) || response.getBallotNumber() != proposer.getBallot()) return;
        if (response.getType() == Type.Closed) { state.observeBallot(response.getHighestPromised()); return; }
        if (proposer.receivePrepare(peer, response)) proposerTwoBefore();
        else if (proposer.receiveAccept(peer, response)) proposerTwoAfter();
    }
    public synchronized void proposerTwoBefore() { broadcast(proposer.prepareRequestOne()); }
    public synchronized void proposerTwoAfter() { nextAttempt = state.now() + TimeUnit.SECONDS.toNanos(10); }
    private void broadcast(Request request) {
        String message = JSON.toJSONString(request);
        for (String peer : peers) {
            try { webSocketService.sendMessage(peer, message); }
            catch (Exception e) { log.warn("Cannot send to {}: {}", peer, e.toString()); }
        }
    }
    public synchronized void cancle() {
        running = false;
        if (task != null) task.cancel(false);
        if (proposer != null) proposer.cancel();
        state.revokeOwnership();
    }
    @PreDestroy public synchronized void close() { cancle(); scheduler.shutdownNow(); }
}
