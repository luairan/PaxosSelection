package com.luairan.service.context;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.*;
import java.nio.file.*;
import java.util.function.LongSupplier;

/** Per-process lease state. All deadlines use elapsed, monotonic time. */
@Component
public class State {
    public static final long MAX_LEASE_MILLIS = 30000;
    public static final long OWNER_LEASE_MILLIS = 29000;
    @Value("${paxos.node-id:1}") private int nodeId = 1;
    @Value("${paxos.data-dir:./data}") private String dataDir = "./data";
    private final LongSupplier clock;
    private BallotSequenceStore sequenceStore;
    private long ballotNumber, highestPromissed;
    private Proposal accpetedProposal;
    private long acceptedAt, acceptedMillis, startedAt;
    private LeaseGrant ownerGrant;

    public State() { this.clock = System::nanoTime; }
    public State(int nodeId, Path directory, LongSupplier clock) throws IOException {
        this.clock = clock; this.nodeId = nodeId; this.dataDir = directory.toString(); initialize();
    }
    @PostConstruct
    public synchronized void initialize() throws IOException {
        if (sequenceStore != null) return;
        BallotSequenceStore opened = BallotSequenceStore.open(nodeId, Paths.get(dataDir));
        try {
            startedAt = now();
            sequenceStore = opened;
        } catch (RuntimeException | Error failure) {
            try { opened.close(); }
            catch (IOException cleanup) { failure.addSuppressed(cleanup); }
            throw failure;
        }
    }
    public long now() { return clock.getAsLong(); }
    public synchronized boolean isReady() { return sequenceStore != null && elapsed(startedAt, MAX_LEASE_MILLIS); }
    private boolean elapsed(long start, long millis) { return now() - start >= millis * 1000000L; }
    public String getNodeId() { return Integer.toString(nodeId); }
    public synchronized long nextBallotNumber() {
        if (sequenceStore == null) throw new IllegalStateException("Ballot store is not initialized");
        try {
            ballotNumber = sequenceStore.nextBallot(highestPromissed);
        } catch (IOException e) { throw new IllegalStateException("Cannot persist ballot", e); }
        return ballotNumber;
    }
    public synchronized void observeBallot(long ballot) { highestPromissed = Math.max(highestPromissed, ballot); }
    public synchronized long getBallotNumber() { return ballotNumber; }
    public synchronized long getHighestPromissed() { return highestPromissed; }
    public synchronized void setHighestPromissed(long value) { highestPromissed = Math.max(highestPromissed, value); }
    public synchronized Proposal getAccpetedProposal() {
        if (accpetedProposal != null && elapsed(acceptedAt, acceptedMillis)) accpetedProposal = null;
        return copy(accpetedProposal);
    }
    public synchronized void accept(Proposal proposal) {
        accpetedProposal = copy(proposal); acceptedAt = now(); acceptedMillis = proposal.getTimeout();
    }
    public synchronized boolean isLeaseOwner() {
        if (ownerGrant != null && elapsed(ownerGrant.startedAtNanos(), OWNER_LEASE_MILLIS)) ownerGrant = null;
        return ownerGrant != null;
    }
    public synchronized void grantOwnership(long sentAt, long winningBallot) {
        if (now() - sentAt < OWNER_LEASE_MILLIS * 1000000L) {
            ownerGrant = new LeaseGrant(winningBallot, getNodeId(), sentAt);
        }
    }
    public synchronized LeaseGrant currentLease() {
        if (!isLeaseOwner()) throw new LeaseUnavailableException();
        return ownerGrant;
    }
    public synchronized boolean isCurrentLease(LeaseGrant grant) {
        return isLeaseOwner() && ownerGrant.equals(grant);
    }
    public synchronized void revokeOwnership() { ownerGrant = null; }
    public static Proposal copy(Proposal p) {
        if (p == null) return null;
        Proposal c = new Proposal(); c.setProposerId(p.getProposerId()); c.setMachineId(p.getMachineId()); c.setTimeout(p.getTimeout()); return c;
    }
    @PreDestroy
    public synchronized void close() throws IOException {
        revokeOwnership();
        if (sequenceStore != null) {
            BallotSequenceStore opened = sequenceStore;
            sequenceStore = null;
            opened.close();
        }
    }
}
