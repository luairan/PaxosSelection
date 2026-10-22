package com.luairan;

import com.luairan.service.context.*;
import com.luairan.service.paxoslease.*;
import com.luairan.service.websocket.WebSocketService;
import com.luairan.service.util.Json;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.Assert.*;

public class ElectionSchedulingTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();
    @Test public void blockedMinorityDoesNotBlockHealthyMajority() throws Exception {
        AtomicLong now = new AtomicLong();
        State owner = new State(1, temporary.getRoot().toPath(), now::get);
        State b = new State(2, temporary.getRoot().toPath(), now::get);
        State c = new State(3, temporary.getRoot().toPath(), now::get);
        java.util.concurrent.atomic.AtomicReference<ProposerServiceImpl> serviceRef = new java.util.concurrent.atomic.AtomicReference<>();
        CountDownLatch blocked = new CountDownLatch(1), release = new CountDownLatch(1);
        Map<String, AcceptorServiceImpl> acceptors = new HashMap<>();
        acceptors.put("ws://localhost:8002/paxos", new AcceptorServiceImpl(b));
        acceptors.put("ws://localhost:8003/paxos", new AcceptorServiceImpl(c));
        WebSocketService transport = (peer, message) -> {
            if (peer.equals("ws://localhost:8001/paxos")) {
                blocked.countDown();
                try { release.await(5, TimeUnit.SECONDS); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                return;
            }
            Request r = Json.parseObject(message, Request.class);
            AcceptorServiceImpl a = acceptors.get(peer);
            serviceRef.get().receive(peer, r.getType() == Type.PrepareRequest ? a.prepareRequest(r) : a.proposeRequest(r));
        };
        ProposerServiceImpl service = new ProposerServiceImpl(transport, owner,
            new PeerConfiguration("ws://localhost:8001/paxos,ws://localhost:8002/paxos,ws://localhost:8003/paxos"));
        serviceRef.set(service);
        now.set(TimeUnit.SECONDS.toNanos(30));
        try {
            service.proposer(); assertTrue(blocked.await(2, TimeUnit.SECONDS));
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
            while (!owner.isLeaseOwner() && System.nanoTime() < deadline) Thread.sleep(10);
            assertTrue("A blocked minority must not prevent majority election", owner.isLeaseOwner());
        } finally { release.countDown(); service.close(); owner.close(); b.close(); c.close(); }
    }
    @Test public void slowQuorumResponseDoesNotPostponeRenewalPastExpiry() throws Exception {
        AtomicLong now = new AtomicLong(); State owner = new State(1, temporary.getRoot().toPath(), now::get);
        ProposerServiceImpl service = new ProposerServiceImpl((peer, message) -> { }, owner,
            new PeerConfiguration("ws://localhost:8001/paxos"));
        try {
            Proposer round = new Proposer(owner, Arrays.asList("a"));
            Response prepared = new Response(); prepared.setType(Type.PrepareResponse); prepared.setBallotNumber(round.getBallot());
            assertTrue(round.receivePrepare("a", prepared)); round.prepareRequestOne();
            // Quorum arrives 28 seconds after phase two was sent: only one second remains.
            now.set(TimeUnit.SECONDS.toNanos(28));
            ReflectionTestUtils.setField(service, "proposer", round);
            service.proposerTwoAfter();
            long next = (Long) ReflectionTestUtils.getField(service, "nextAttempt");
            assertTrue("Overdue renewal must be scheduled immediately", next - now.get() <= 0);
        } finally { service.close(); owner.close(); }
    }
}
