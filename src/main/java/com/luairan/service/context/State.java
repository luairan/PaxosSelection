package com.luairan.service.context;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.*;
import java.nio.channels.FileLock;
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
    private RandomAccessFile sequenceFile;
    private FileLock fileLock;
    private long sequence, ballotNumber, highestPromissed;
    private Proposal accpetedProposal;
    private long acceptedAt, acceptedMillis, ownerAt, ownerMillis, startedAt;
    private boolean leaseOwner;

    public State() { this.clock = System::nanoTime; }
    public State(int nodeId, Path directory, LongSupplier clock) throws IOException {
        this.clock = clock; this.nodeId = nodeId; this.dataDir = directory.toString(); initialize();
    }
    @PostConstruct
    public synchronized void initialize() throws IOException {
        if (sequenceFile != null) return;
        if (nodeId < 1 || nodeId > 65535) throw new IllegalArgumentException("node-id must be 1..65535 and unique in the cluster");
        Files.createDirectories(Paths.get(dataDir));
        sequenceFile = new RandomAccessFile(Paths.get(dataDir, "node-" + nodeId + ".ballot").toFile(), "rw");
        fileLock = sequenceFile.getChannel().tryLock();
        if (fileLock == null) throw new IOException("Node identity already in use");
        if (sequenceFile.length() != 0 && sequenceFile.length() != 8) throw new IOException("Corrupt ballot sequence");
        sequence = sequenceFile.length() == 8 ? sequenceFile.readLong() : 0;
        if (sequence < 0 || sequence > (Long.MAX_VALUE >>> 16)) throw new IOException("Corrupt ballot sequence");
        startedAt = now();
    }
    public long now() { return clock.getAsLong(); }
    public synchronized boolean isReady() { return elapsed(startedAt, MAX_LEASE_MILLIS); }
    private boolean elapsed(long start, long millis) { return now() - start >= millis * 1000000L; }
    public String getNodeId() { return Integer.toString(nodeId); }
    public synchronized long nextBallotNumber() {
        long next = Math.max(sequence, highestPromissed >>> 16) + 1;
        if (next > (Long.MAX_VALUE >>> 16)) throw new IllegalStateException("Ballot sequence exhausted");
        try {
            sequenceFile.seek(0); sequenceFile.writeLong(next); sequenceFile.getFD().sync();
        } catch (IOException e) { throw new IllegalStateException("Cannot persist ballot", e); }
        sequence = next;
        ballotNumber = (sequence << 16) | nodeId;
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
        if (leaseOwner && elapsed(ownerAt, ownerMillis)) leaseOwner = false;
        return leaseOwner;
    }
    public synchronized void grantOwnership(long sentAt) {
        if (now() - sentAt < OWNER_LEASE_MILLIS * 1000000L) {
            ownerAt = sentAt; ownerMillis = OWNER_LEASE_MILLIS; leaseOwner = true;
        }
    }
    public synchronized void revokeOwnership() { leaseOwner = false; }
    public static Proposal copy(Proposal p) {
        if (p == null) return null;
        Proposal c = new Proposal(); c.setProposerId(p.getProposerId()); c.setMachineId(p.getMachineId()); c.setTimeout(p.getTimeout()); return c;
    }
    @PreDestroy
    public synchronized void close() throws IOException {
        revokeOwnership();
        if (fileLock != null) fileLock.release();
        if (sequenceFile != null) sequenceFile.close();
        fileLock = null; sequenceFile = null;
    }
}
