package com.luairan;

import com.luairan.service.context.*;
import com.luairan.service.paxoslease.*;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.Assert.*;

/** Deterministic multi-node protocol tests; no sleeps, wall clock, or shared node state. */
public class SuperannaApplicationTests {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();
    private final AtomicLong clock = new AtomicLong();
    private final List<State> opened = new ArrayList<>();
    private State node(int id) throws Exception {
        State s = new State(id, temporary.getRoot().toPath(), clock::get); opened.add(s); return s;
    }
    @After public void closeNodes() throws Exception { for (State s : opened) s.close(); }
    private void advance(long millis) { clock.addAndGet(TimeUnit.MILLISECONDS.toNanos(millis)); }
    private List<String> peers(int n) {
        List<String> p = new ArrayList<>(); for (int i = 0; i < n; i++) p.add("peer-" + i); return p;
    }
    private AcceptorServiceImpl[] acceptors(State[] states) {
        AcceptorServiceImpl[] a = new AcceptorServiceImpl[states.length];
        for (int i = 0; i < states.length; i++) a[i] = new AcceptorServiceImpl(states[i]); return a;
    }
    private boolean elect(State candidate, AcceptorServiceImpl[] acceptors, int... reachable) {
        List<String> members = peers(acceptors.length);
        Proposer p = new Proposer(candidate, members); Request prepare = p.propose(), accept = null;
        for (int i : reachable) if (p.receivePrepare(members.get(i), acceptors[i].prepareRequest(prepare))) accept = p.prepareRequestOne();
        if (accept == null) return false;
        for (int i : reachable) p.receiveAccept(members.get(i), acceptors[i].proposeRequest(accept));
        return p.isFinished();
    }
    @Test public void threeNodesRenewContinuouslyWithoutLosingLeadership() throws Exception {
        State[] s = {node(1), node(2), node(3)}; AcceptorServiceImpl[] a = acceptors(s); advance(30000);
        assertTrue(elect(s[0], a, 0, 1, 2));
        for (int round = 0; round < 12; round++) {
            advance(10000); assertTrue(s[0].isLeaseOwner());
            assertTrue("renewal " + round, elect(s[0], a, 0, 1, 2));
            assertFalse(elect(s[1], a, 0, 1, 2)); assertFalse(s[1].isLeaseOwner());
        }
    }
    @Test public void fiveNodePartitionRequiresMajorityAndTransfersAfterExpiry() throws Exception {
        State[] s = {node(1), node(2), node(3), node(4), node(5)}; AcceptorServiceImpl[] a = acceptors(s); advance(30000);
        assertTrue(elect(s[0], a, 0, 1, 2, 3, 4));
        advance(10000);
        assertFalse(elect(s[0], a, 0, 1)); assertFalse(elect(s[2], a, 2, 3, 4));
        advance(20001); assertFalse(s[0].isLeaseOwner());
        assertTrue(elect(s[2], a, 2, 3, 4)); assertFalse(elect(s[0], a, 0, 1));
        assertTrue(s[2].isLeaseOwner());
    }
    @Test public void restartedMajorityCannotRegrantOldLeaseDuringRecovery() throws Exception {
        State[] s = {node(1), node(2), node(3)}; AcceptorServiceImpl[] a = acceptors(s); advance(30000);
        assertTrue(elect(s[0], a, 0, 1, 2)); advance(5000);
        s[1].close(); s[2].close(); s[1] = node(2); s[2] = node(3); a = acceptors(s);
        assertFalse(elect(s[1], a, 1, 2)); assertTrue(s[0].isLeaseOwner());
        advance(29999); assertFalse(elect(s[1], a, 1, 2));
        advance(1); assertFalse(s[0].isLeaseOwner()); assertTrue(elect(s[1], a, 1, 2));
    }
    @Test public void delayedAcceptRepliesCannotResurrectExpiredLeadership() throws Exception {
        State[] s = {node(1), node(2), node(3)}; AcceptorServiceImpl[] a = acceptors(s); advance(30000);
        Proposer p = new Proposer(s[0], peers(3)); Request prepare = p.propose();
        assertFalse(p.receivePrepare("peer-0", a[0].prepareRequest(prepare)));
        assertTrue(p.receivePrepare("peer-1", a[1].prepareRequest(prepare)));
        Request accept = p.prepareRequestOne(); Response r0 = a[0].proposeRequest(accept), r1 = a[1].proposeRequest(accept);
        advance(30001); assertTrue(elect(s[2], a, 0, 1, 2));
        assertFalse(p.receiveAccept("peer-0", r0)); assertFalse(p.receiveAccept("peer-1", r1));
        assertFalse(s[0].isLeaseOwner()); assertTrue(s[2].isLeaseOwner());
    }
    @Test public void duplicateUnknownAndStaleVotesCannotFormQuorum() throws Exception {
        State[] s = {node(1), node(2), node(3)}; AcceptorServiceImpl[] a = acceptors(s); advance(30000);
        Proposer p = new Proposer(s[0], peers(3)); Request prepare = p.propose(); Response r = a[0].prepareRequest(prepare);
        assertFalse(p.receivePrepare("unknown", r));
        for (int i = 0; i < 20; i++) assertFalse(p.receivePrepare("peer-0", r));
        Response stale = a[1].prepareRequest(prepare); stale.setBallotNumber(stale.getBallotNumber() - 1);
        assertFalse(p.receivePrepare("peer-1", stale));
        assertTrue(p.receivePrepare("peer-1", a[1].prepareRequest(prepare)));
        Request accept = p.prepareRequestOne(); Response accepted = a[0].proposeRequest(accept);
        for (int i = 0; i < 20; i++) assertFalse(p.receiveAccept("peer-0", accepted));
        assertFalse(p.receiveAccept("unknown", accepted)); assertFalse(s[0].isLeaseOwner());
        assertTrue(p.receiveAccept("peer-1", a[1].proposeRequest(accept)));
    }
    @Test public void ballotsAreUniqueDurableAndJumpPastObservedBallot() throws Exception {
        State a = node(1), b = node(2); long old = a.nextBallotNumber();
        assertNotEquals(old, b.nextBallotNumber()); a.close(); a = node(1);
        assertTrue(a.nextBallotNumber() > old);
        a.observeBallot(9000000); assertTrue(a.nextBallotNumber() > 9000000);
    }
    @Test public void concurrentPrepareNeverLowersPromiseAndRejectsStaleAccept() throws Exception {
        State s = node(1); AcceptorServiceImpl a = new AcceptorServiceImpl(s); advance(30000);
        ExecutorService workers = Executors.newFixedThreadPool(8);
        try {
            CountDownLatch start = new CountDownLatch(1); List<Future<?>> futures = new ArrayList<>();
            for (int i = 1; i <= 100; i++) {
                final int ballot = i;
                futures.add(workers.submit(() -> { start.await(); a.prepareRequest(request(ballot, null)); return null; }));
            }
            start.countDown(); for (Future<?> f : futures) f.get(5, TimeUnit.SECONDS);
            assertEquals(100, s.getHighestPromissed());
            assertEquals(Type.Closed, a.proposeRequest(request(99, proposal("2"))).getType());
        } finally { workers.shutdownNow(); }
    }
    @Test public void higherBallotCannotOverwriteAnotherNodesUnexpiredLease() throws Exception {
        State s = node(1); AcceptorServiceImpl a = new AcceptorServiceImpl(s); advance(30000);
        assertEquals(Type.ProposeResponse, a.proposeRequest(request(1, proposal("2"))).getType());
        a.prepareRequest(request(2, null));
        assertEquals(Type.Closed, a.proposeRequest(request(2, proposal("3"))).getType());
        assertEquals("2", s.getAccpetedProposal().getMachineId());
        advance(30000); assertEquals(Type.ProposeResponse, a.proposeRequest(request(3, proposal("3"))).getType());
    }
    @Test public void recoveryAndMaximumLeaseBoundsAreEnforced() throws Exception {
        State s = node(1); AcceptorServiceImpl a = new AcceptorServiceImpl(s);
        assertEquals(Type.Closed, a.prepareRequest(request(1, null)).getType()); advance(30000);
        assertEquals(Type.PrepareResponse, a.prepareRequest(request(1, null)).getType());
        assertEquals(Type.PrepareResponse, a.prepareRequest(request(1, null)).getType());
        Proposal p = proposal("2"); p.setTimeout(30001);
        assertEquals(Type.Closed, a.proposeRequest(request(1, p)).getType());
    }
    @Test public void concurrentConflictingAcceptsGrantAtMostOneLiveOwner() throws Exception {
        State s = node(1); AcceptorServiceImpl a = new AcceptorServiceImpl(s); advance(30000);
        ExecutorService workers = Executors.newFixedThreadPool(2);
        try {
            CountDownLatch start = new CountDownLatch(1);
            Future<Response> first = workers.submit(() -> { start.await(); return a.proposeRequest(request(1, proposal("2"))); });
            Future<Response> second = workers.submit(() -> { start.await(); return a.proposeRequest(request(2, proposal("3"))); });
            start.countDown();
            int accepted = (first.get().getType() == Type.ProposeResponse ? 1 : 0)
                    + (second.get().getType() == Type.ProposeResponse ? 1 : 0);
            assertEquals(1, accepted);
        } finally { workers.shutdownNow(); }
    }
    @Test public void cancelledElectionIgnoresLateQuorum() throws Exception {
        State[] s = {node(1), node(2), node(3)}; AcceptorServiceImpl[] a = acceptors(s); advance(30000);
        Proposer p = new Proposer(s[0], peers(3)); Request prepare = p.propose();
        p.receivePrepare("peer-0", a[0].prepareRequest(prepare));
        assertTrue(p.receivePrepare("peer-1", a[1].prepareRequest(prepare)));
        Request accept = p.prepareRequestOne(); p.cancel();
        assertFalse(p.receiveAccept("peer-0", a[0].proposeRequest(accept)));
        assertFalse(p.receiveAccept("peer-1", a[1].proposeRequest(accept)));
        assertFalse(s[0].isLeaseOwner());
    }
    private Proposal proposal(String owner) { Proposal p = new Proposal(); p.setMachineId(owner); p.setProposerId("test"); p.setTimeout(30000); return p; }
    private Request request(long ballot, Proposal p) { Request r = new Request(); r.setBallotNumber(ballot); r.setProposal(p); return r; }
}
