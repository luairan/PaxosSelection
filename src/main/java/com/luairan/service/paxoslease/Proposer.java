package com.luairan.service.paxoslease;

import com.luairan.service.context.*;
import java.util.*;

/** One election attempt. Responses are counted once per configured peer and phase. */
public class Proposer {
    private final State state;
    private final Set<String> peers, prepared = new HashSet<>(), accepted = new HashSet<>();
    private final long ballot;
    private final int quorum;
    private long sentAt;
    private boolean requestIssued;
    private boolean phaseTwo, finished, cancelled;
    public Proposer(State state, Collection<String> peers) {
        this.state = state; this.peers = new HashSet<>(peers);
        if (this.peers.isEmpty()) throw new IllegalArgumentException("Empty membership");
        quorum = this.peers.size() / 2 + 1; ballot = state.nextBallotNumber();
    }
    public Request propose() { return request(Type.PrepareRequest); }
    private Request request(Type type) { Request r = new Request(); r.setType(type); r.setBallotNumber(ballot); return r; }
    public synchronized boolean receivePrepare(String peer, Response r) {
        if (!valid(peer, r) || phaseTwo || r.getType() != Type.PrepareResponse) return false;
        Proposal p = r.getAccpetedProposal();
        if (p != null && !state.getNodeId().equals(p.getMachineId())) return false;
        prepared.add(peer);
        if (prepared.size() < quorum) return false;
        phaseTwo = true; return true;
    }
    public synchronized Request prepareRequestOne() {
        if (!phaseTwo || cancelled) throw new IllegalStateException("Prepare quorum required");
        if (!requestIssued) { sentAt = state.now(); requestIssued = true; }
        Request r = request(Type.ProposeRequest);
        Proposal p = new Proposal(); p.setMachineId(state.getNodeId()); p.setProposerId(Long.toString(ballot));
        p.setTimeout(State.MAX_LEASE_MILLIS); r.setProposal(p); return r;
    }
    public synchronized boolean receiveAccept(String peer, Response r) {
        if (!valid(peer, r) || !phaseTwo || !requestIssued || finished || r.getType() != Type.ProposeResponse) return false;
        Proposal p = r.getAccpetedProposal();
        if (p == null || !state.getNodeId().equals(p.getMachineId()) || !Long.toString(ballot).equals(p.getProposerId())) return false;
        if (state.now() - sentAt >= State.OWNER_LEASE_MILLIS * 1000000L) { cancelled = true; return false; }
        accepted.add(peer);
        if (accepted.size() < quorum) return false;
        finished = true; state.grantOwnership(sentAt); return true;
    }
    private boolean valid(String peer, Response r) {
        return !cancelled && peers.contains(peer) && r != null && r.getBallotNumber() == ballot;
    }
    public synchronized void cancel() { cancelled = true; }
    public synchronized boolean isFinished() { return finished; }
    public synchronized long getSentAt() {
        if (!requestIssued) throw new IllegalStateException("Phase two has not been sent");
        return sentAt;
    }
    public long getBallot() { return ballot; }
}
