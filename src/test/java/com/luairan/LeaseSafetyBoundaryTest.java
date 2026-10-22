package com.luairan;

import com.luairan.service.context.*;
import com.luairan.service.paxoslease.*;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.Assert.*;

public class LeaseSafetyBoundaryTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();
    private final AtomicLong realNanos = new AtomicLong();
    private final List<State> nodes = new ArrayList<>();
    private State node(int id, int ratePercent) throws Exception {
        // Different offsets and rates; no synchronized clocks are assumed.
        State s = new State(id, temporary.getRoot().toPath(), () -> realNanos.get() * ratePercent / 100 + id * 123456789L);
        nodes.add(s); return s;
    }
    @After public void close() throws Exception { for (State s : nodes) s.close(); }
    private Proposer prepared(State owner, AcceptorServiceImpl[] a) {
        Proposer p = new Proposer(owner, Arrays.asList("a", "b", "c")); Request r = p.propose();
        p.receivePrepare("a", a[0].prepareRequest(r));
        if (!p.receivePrepare("b", a[1].prepareRequest(r))) return null;
        return p;
    }
    private boolean elect(State owner, AcceptorServiceImpl[] a) {
        Proposer p = prepared(owner, a); if (p == null) return false;
        Request r = p.prepareRequestOne();
        p.receiveAccept("a", a[0].proposeRequest(r)); p.receiveAccept("b", a[1].proposeRequest(r)); return p.isFinished();
    }
    @Test public void slowOwnerExpiresBeforeFastAcceptorQuorumRegrants() throws Exception {
        State owner = node(1, 99), a = node(2, 101), b = node(3, 101);
        AcceptorServiceImpl[] acceptors = {new AcceptorServiceImpl(a), new AcceptorServiceImpl(b), new AcceptorServiceImpl(owner)};
        realNanos.set(31000000000L); assertTrue(elect(owner, acceptors));
        // Owner's 29 local seconds last 29.293 real seconds; acceptor's
        // 30 local seconds last 29.703 real seconds. Check both sides of the gap.
        realNanos.addAndGet(29400000000L); assertFalse(owner.isLeaseOwner()); assertFalse(elect(a, acceptors));
        realNanos.addAndGet(400000000L); assertTrue(elect(a, acceptors)); assertFalse(owner.isLeaseOwner());
    }
    @Test public void fastRestartRecoveryDoesNotOverlapSlowFormerOwner() throws Exception {
        State owner = node(1, 99), a = node(2, 101), b = node(3, 101);
        AcceptorServiceImpl[] acceptors = {new AcceptorServiceImpl(a), new AcceptorServiceImpl(b), new AcceptorServiceImpl(owner)};
        realNanos.set(31000000000L); assertTrue(elect(owner, acceptors));
        a.close(); b.close(); a = node(2, 101); b = node(3, 101);
        acceptors[0] = new AcceptorServiceImpl(a); acceptors[1] = new AcceptorServiceImpl(b);
        realNanos.addAndGet(29200000000L); assertTrue(owner.isLeaseOwner()); assertFalse(elect(a, acceptors));
        realNanos.addAndGet(600000000L); assertFalse(owner.isLeaseOwner()); assertTrue(elect(a, acceptors));
    }
    @Test public void repeatedPhaseTwoConstructionCannotExtendOldQuorumLease() throws Exception {
        State owner = node(1, 100), a = node(2, 100), b = node(3, 100);
        AcceptorServiceImpl[] acceptors = {new AcceptorServiceImpl(a), new AcceptorServiceImpl(b), new AcceptorServiceImpl(owner)};
        realNanos.set(31000000000L); Proposer p = prepared(owner, acceptors); assertNotNull(p);
        Request r = p.prepareRequestOne(); long originalStart = p.getSentAt();
        Response first = acceptors[0].proposeRequest(r), second = acceptors[1].proposeRequest(r);
        realNanos.addAndGet(28000000000L);
        p.prepareRequestOne(); // A retransmission must keep the original deadline.
        realNanos.addAndGet(2100000000L); assertTrue(elect(a, acceptors));
        assertFalse(p.receiveAccept("a", first)); assertFalse(p.receiveAccept("b", second));
        assertFalse(owner.isLeaseOwner()); assertTrue(a.isLeaseOwner());
        assertEquals(originalStart, p.getSentAt());
    }
}
