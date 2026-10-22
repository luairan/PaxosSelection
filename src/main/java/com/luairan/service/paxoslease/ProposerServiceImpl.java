package com.luairan.service.paxoslease;

import com.luairan.service.context.*;
import com.luairan.service.util.Json;
import com.luairan.service.websocket.WebSocketService;

import jakarta.annotation.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;

@Service
public class ProposerServiceImpl implements ProposerService {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final WebSocketService webSocketService;
    private final State state;
    private final List<String> peers;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> task;
    private Proposer proposer;
    private final Map<String, ExecutorService> senders = new HashMap<>();
    private long nextAttempt;
    private boolean running;

    public ProposerServiceImpl(
            WebSocketService webSocketService, State state, PeerConfiguration configuration) {
        this.webSocketService = webSocketService;
        this.state = state;
        this.peers = configuration.peers();
    }

    public synchronized Proposer getCurrentProposer() {
        return proposer;
    }

    public synchronized void proposerStart() {
        proposer();
    }

    public synchronized void proposer() {
        if (running) return;
        running = true;
        nextAttempt = state.now();
        task = scheduler.scheduleWithFixedDelay(this::tick, 0, 100, TimeUnit.MILLISECONDS);
    }

    private synchronized void tick() {
        if (!running || !state.isReady() || state.now() - nextAttempt < 0) return;
        try {
            if (proposer != null) proposer.cancel();
            proposer = new Proposer(state, peers);
            nextAttempt =
                    state.now()
                            + TimeUnit.MILLISECONDS.toNanos(
                                    3000 + ThreadLocalRandom.current().nextInt(1000));
            broadcast(proposer.propose());
        } catch (RuntimeException e) {
            log.error("Election attempt failed", e);
        }
    }

    public synchronized void receive(String peer, Response response) {
        if (!running
                || proposer == null
                || !peers.contains(peer)
                || response.getBallotNumber() != proposer.getBallot()) return;
        if (response.getType() == Type.Closed) {
            state.observeBallot(response.getHighestPromised());
            return;
        }
        if (proposer.receivePrepare(peer, response)) proposerTwoBefore();
        else if (proposer.receiveAccept(peer, response)) proposerTwoAfter();
    }

    public synchronized void proposerTwoBefore() {
        broadcast(proposer.prepareRequestOne());
    }

    public synchronized void proposerTwoAfter() {
        nextAttempt = proposer.getSentAt() + TimeUnit.SECONDS.toNanos(10);
    }

    private void broadcast(Request request) {
        String message = Json.toJSONString(request);
        final long ballot = request.getBallotNumber();
        for (String peer : peers) {
            // One ordered, bounded queue per peer: a stalled minority cannot hold
            // the election lock, block other peers, or create an unbounded backlog.
            ExecutorService sender =
                    senders.computeIfAbsent(
                            peer,
                            key ->
                                    new ThreadPoolExecutor(
                                            1,
                                            1,
                                            0,
                                            TimeUnit.MILLISECONDS,
                                            new ArrayBlockingQueue<>(1),
                                            new ThreadPoolExecutor.DiscardOldestPolicy()));
            sender.execute(
                    () -> {
                        synchronized (ProposerServiceImpl.this) {
                            if (!running || proposer == null || proposer.getBallot() != ballot)
                                return;
                        }
                        try {
                            webSocketService.sendMessage(peer, message);
                        } catch (Exception e) {
                            log.warn("Cannot send to {}: {}", peer, e.toString());
                        }
                    });
        }
    }

    public synchronized void cancle() {
        running = false;
        if (task != null) task.cancel(false);
        if (proposer != null) proposer.cancel();
        state.revokeOwnership();
    }

    @PreDestroy
    public synchronized void close() {
        cancle();
        scheduler.shutdownNow();
        for (ExecutorService sender : senders.values()) sender.shutdownNow();
        senders.clear();
    }
}
