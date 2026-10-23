package com.luairan.service.paxoslease;

import com.luairan.service.context.*;

import java.util.*;

/** One election attempt. Responses are counted once per configured peer and phase. */
public class Proposer {
    public enum Phase {
        PREPARING,
        PREPARED,
        ACCEPTING,
        SUCCEEDED,
        CANCELLED,
        EXPIRED
    }

    private final State state;
    private final Set<String> peers;
    private final Set<String> prepared = new HashSet<>();
    private final Set<String> accepted = new HashSet<>();
    private final long ballot;
    private final int quorum;
    private Phase phase = Phase.PREPARING;
    // Absent until the first phase-two request; retained even after cancellation/expiry.
    private Long sentAt;

    public Proposer(State state, Collection<String> peers) {
        this.state = state;
        this.peers = new HashSet<>(peers);
        if (this.peers.isEmpty()) throw new IllegalArgumentException("Empty membership");
        quorum = this.peers.size() / 2 + 1;
        ballot = state.nextBallotNumber();
    }

    public Request propose() {
        return request(Type.PrepareRequest);
    }

    private Request request(Type type) {
        Request r = new Request();
        r.setType(type);
        r.setBallotNumber(ballot);
        return r;
    }

    public synchronized boolean receivePrepare(String peer, Response response) {
        if (phase != Phase.PREPARING
                || !valid(peer, response)
                || response.getType() != Type.PrepareResponse) return false;
        Proposal proposal = response.getAccpetedProposal();
        if (proposal != null && !state.getNodeId().equals(proposal.getMachineId())) return false;
        prepared.add(peer);
        if (prepared.size() < quorum) return false;
        phase = Phase.PREPARED;
        return true;
    }

    public synchronized Request prepareRequestOne() {
        if (phase == Phase.PREPARED) {
            sentAt = state.now();
            phase = Phase.ACCEPTING;
        } else if (phase != Phase.ACCEPTING) {
            throw new IllegalStateException("Active prepare quorum required");
        }
        // Retransmission never changes the phase-two timestamp or ballot.
        Request r = request(Type.ProposeRequest);
        Proposal proposal = new Proposal();
        proposal.setMachineId(state.getNodeId());
        proposal.setProposerId(Long.toString(ballot));
        proposal.setTimeout(State.MAX_LEASE_MILLIS);
        r.setProposal(proposal);
        return r;
    }

    public synchronized boolean receiveAccept(String peer, Response response) {
        if (phase != Phase.ACCEPTING
                || !valid(peer, response)
                || response.getType() != Type.ProposeResponse) return false;
        Proposal proposal = response.getAccpetedProposal();
        if (proposal == null
                || !state.getNodeId().equals(proposal.getMachineId())
                || !Long.toString(ballot).equals(proposal.getProposerId())) return false;
        if (state.now() - sentAt >= State.OWNER_LEASE_MILLIS * 1000000L) {
            phase = Phase.EXPIRED;
            return false;
        }
        accepted.add(peer);
        if (accepted.size() < quorum) return false;
        phase = Phase.SUCCEEDED;
        state.grantOwnership(sentAt, ballot);
        return true;
    }

    private boolean valid(String peer, Response response) {
        return peers.contains(peer) && response != null && response.getBallotNumber() == ballot;
    }

    public synchronized void cancel() {
        // A successful attempt remains historical success; cancellation cannot revoke a
        // later round's lease. The service owns explicit State.revokeOwnership().
        if (phase == Phase.PREPARING || phase == Phase.PREPARED || phase == Phase.ACCEPTING)
            phase = Phase.CANCELLED;
    }

    public synchronized Phase getPhase() {
        return phase;
    }

    public synchronized boolean isFinished() {
        return phase == Phase.SUCCEEDED;
    }

    public synchronized long getSentAt() {
        if (sentAt == null) throw new IllegalStateException("Phase two has not been sent");
        return sentAt;
    }

    public long getBallot() {
        return ballot;
    }
}
