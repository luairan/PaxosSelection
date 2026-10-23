package com.luairan;

import static org.junit.jupiter.api.Assertions.*;

import com.luairan.service.context.*;
import com.luairan.service.paxoslease.Proposer;
import com.luairan.service.paxoslease.Proposer.Phase;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

class ProposerStateMachineTest {
    @TempDir Path directory;
    private final AtomicLong now = new AtomicLong();
    private State state;
    private Proposer round;

    @BeforeEach
    void setup() throws Exception {
        state = new State(1, directory, now::get);
        round = new Proposer(state, List.of("a", "b", "c"));
    }

    @AfterEach
    void close() throws Exception {
        state.close();
    }

    private Response response(Type type) {
        Response response = new Response();
        response.setType(type);
        response.setBallotNumber(round.getBallot());
        if (type == Type.ProposeResponse) {
            Proposal proposal = new Proposal();
            proposal.setMachineId(state.getNodeId());
            proposal.setProposerId(Long.toString(round.getBallot()));
            proposal.setTimeout(State.MAX_LEASE_MILLIS);
            response.setAccpetedProposal(proposal);
        }
        return response;
    }

    private void prepare() {
        assertFalse(round.receivePrepare("a", response(Type.PrepareResponse)));
        assertTrue(round.receivePrepare("b", response(Type.PrepareResponse)));
        assertEquals(Phase.PREPARED, round.getPhase());
    }

    private void accepting() {
        prepare();
        round.prepareRequestOne();
    }

    private void succeed() {
        assertFalse(round.receiveAccept("a", response(Type.ProposeResponse)));
        assertTrue(round.receiveAccept("b", response(Type.ProposeResponse)));
    }

    @Test
    void followsQuorumTransitionsAndGrantsOnlyConfirmedBallot() {
        assertEquals(Phase.PREPARING, round.getPhase());
        assertThrows(IllegalStateException.class, round::prepareRequestOne);
        assertThrows(IllegalStateException.class, round::getSentAt);
        assertFalse(round.receiveAccept("a", response(Type.ProposeResponse)));
        prepare();
        assertFalse(state.isLeaseOwner());
        assertFalse(round.receiveAccept("a", response(Type.ProposeResponse)));
        round.prepareRequestOne();
        assertEquals(Phase.ACCEPTING, round.getPhase());
        state.nextBallotNumber(); // A later, unconfirmed ballot must never become this grant.
        succeed();
        assertEquals(Phase.SUCCEEDED, round.getPhase());
        assertTrue(round.isFinished());
        assertEquals(round.getBallot(), state.currentLease().ballot());
    }

    @Test
    void duplicateUnknownWrongPhaseAndWrongBallotResponsesDoNotAddVotes() {
        Response prepared = response(Type.PrepareResponse);
        assertFalse(round.receivePrepare("a", prepared));
        assertFalse(round.receivePrepare("a", prepared));
        assertFalse(round.receivePrepare("outsider", prepared));
        assertFalse(round.receivePrepare("b", response(Type.ProposeResponse)));
        Response stale = response(Type.PrepareResponse);
        stale.setBallotNumber(round.getBallot() - 1);
        assertFalse(round.receivePrepare("b", stale));
        assertFalse(round.receivePrepare("b", null));
        assertEquals(Phase.PREPARING, round.getPhase());
        assertTrue(round.receivePrepare("b", prepared));
        round.prepareRequestOne();
        assertFalse(round.receivePrepare("c", prepared));
        Response accepted = response(Type.ProposeResponse);
        assertFalse(round.receiveAccept("a", accepted));
        assertFalse(round.receiveAccept("a", accepted));
        assertFalse(round.receiveAccept("outsider", accepted));
        assertFalse(round.receiveAccept("b", prepared));
        accepted.setBallotNumber(round.getBallot() - 1);
        assertFalse(round.receiveAccept("b", accepted));
        assertFalse(state.isLeaseOwner());
        assertTrue(round.receiveAccept("b", response(Type.ProposeResponse)));
    }

    @Test
    void retransmissionAtZeroAndLaterKeepsOriginalDeadline() {
        accepting();
        assertEquals(0, round.getSentAt());
        now.set(TimeUnit.SECONDS.toNanos(28));
        Request retransmission = round.prepareRequestOne();
        assertEquals(round.getBallot(), retransmission.getBallotNumber());
        assertEquals(0, round.getSentAt());
        succeed();
        now.set(TimeUnit.SECONDS.toNanos(29));
        assertFalse(state.isLeaseOwner());
    }

    @Test
    void expiredAttemptCannotBeRevivedByRetransmissionOrLateQuorum() {
        accepting();
        now.set(TimeUnit.SECONDS.toNanos(29));
        assertFalse(round.receiveAccept("a", response(Type.ProposeResponse)));
        assertEquals(Phase.EXPIRED, round.getPhase());
        assertFalse(round.receiveAccept("b", response(Type.ProposeResponse)));
        assertThrows(IllegalStateException.class, round::prepareRequestOne);
        round.cancel();
        assertEquals(Phase.EXPIRED, round.getPhase());
        assertFalse(state.isLeaseOwner());
    }

    @Test
    void cancellationFromEachActivePhaseIsTerminal() {
        for (Phase phase : List.of(Phase.PREPARING, Phase.PREPARED, Phase.ACCEPTING)) {
            round = new Proposer(state, List.of("a", "b", "c"));
            if (phase != Phase.PREPARING) prepare();
            if (phase == Phase.ACCEPTING) round.prepareRequestOne();
            round.cancel();
            round.cancel();
            assertEquals(Phase.CANCELLED, round.getPhase());
            assertFalse(round.receivePrepare("c", response(Type.PrepareResponse)));
            assertFalse(round.receiveAccept("a", response(Type.ProposeResponse)));
            assertFalse(round.receiveAccept("b", response(Type.ProposeResponse)));
            assertThrows(IllegalStateException.class, round::prepareRequestOne);
            assertFalse(round.isFinished());
            assertFalse(state.isLeaseOwner());
        }
    }

    @Test
    void successfulAttemptIgnoresFurtherRepliesAndCannotRestart() {
        accepting();
        succeed();
        LeaseGrant grant = state.currentLease();
        now.set(TimeUnit.SECONDS.toNanos(10));
        assertFalse(round.receiveAccept("c", response(Type.ProposeResponse)));
        assertThrows(IllegalStateException.class, round::prepareRequestOne);
        round.cancel();
        assertEquals(Phase.SUCCEEDED, round.getPhase());
        assertEquals(grant, state.currentLease());
    }

    @Test
    void lateCancelledRoundCannotReplaceSuccessorsConfirmedLease() {
        accepting();
        Proposer old = round;
        Response late = response(Type.ProposeResponse);
        old.cancel();
        round = new Proposer(state, List.of("a", "b", "c"));
        accepting();
        succeed();
        LeaseGrant successor = state.currentLease();
        assertTrue(successor.ballot() > old.getBallot());
        assertFalse(old.receiveAccept("a", late));
        assertFalse(old.receiveAccept("b", late));
        assertEquals(Phase.CANCELLED, old.getPhase());
        assertEquals(successor, state.currentLease());
        old.cancel();
        assertEquals(successor, state.currentLease());
    }

    @Test
    void wrongOwnerAndWrongProposalCannotCompleteAcceptPhase() {
        accepting();
        Response wrong = response(Type.ProposeResponse);
        wrong.getAccpetedProposal().setMachineId("other");
        assertFalse(round.receiveAccept("a", wrong));
        wrong = response(Type.ProposeResponse);
        wrong.getAccpetedProposal().setProposerId("other-ballot");
        assertFalse(round.receiveAccept("b", wrong));
        Response missing = response(Type.ProposeResponse);
        missing.setAccpetedProposal(null);
        assertFalse(round.receiveAccept("c", missing));
        assertEquals(Phase.ACCEPTING, round.getPhase());
        assertFalse(state.isLeaseOwner());
        succeed();
    }
}
