package com.luairan.service.context;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.Set;

/** Owns the exclusive node identity lock and the durable, eight-byte ballot counter. */
public final class BallotSequenceStore implements AutoCloseable {
    private static final long MAX_SEQUENCE = Long.MAX_VALUE >>> 16;
    // Some systems release all process locks on a file when any descriptor closes.
    // Reject our own duplicates before opening another descriptor for the same file.
    private static final Set<Path> OPEN_FILES = new HashSet<>();
    private final Path path;
    private final RandomAccessFile file;
    private final RandomAccessFile journal;
    private final int nodeId;
    private long sequence;
    private boolean closed;
    private boolean failed;

    private BallotSequenceStore(
            Path path, RandomAccessFile file, RandomAccessFile journal, int nodeId, long sequence) {
        this.path = path;
        this.file = file;
        this.journal = journal;
        this.nodeId = nodeId;
        this.sequence = sequence;
    }

    public static synchronized BallotSequenceStore open(int nodeId, Path directory)
            throws IOException {
        if (nodeId < 1 || nodeId > 65535)
            throw new IllegalArgumentException(
                    "node-id must be 1..65535 and unique in the cluster");
        Files.createDirectories(directory);
        Path path = directory.toRealPath().resolve("node-" + nodeId + ".ballot");
        if (Files.exists(path)) path = path.toRealPath();
        for (Path owned : OPEN_FILES) {
            if (owned.equals(path) || (Files.exists(path) && Files.isSameFile(owned, path)))
                throw new IOException("Node identity already in use");
        }
        RandomAccessFile file = new RandomAccessFile(path.toFile(), "rw");
        RandomAccessFile journal = null;
        try {
            try {
                if (file.getChannel().tryLock() == null)
                    throw new IOException("Node identity already in use");
            } catch (OverlappingFileLockException conflict) {
                throw new IOException("Node identity already in use", conflict);
            }
            if (file.length() != 0 && file.length() != Long.BYTES)
                throw new IOException("Corrupt ballot sequence");
            long sequence = file.length() == Long.BYTES ? file.readLong() : 0;
            if (sequence < 0 || sequence > MAX_SEQUENCE)
                throw new IOException("Corrupt ballot sequence");
            Path journalPath = path.resolveSibling(path.getFileName() + ".journal");
            if (Files.isSymbolicLink(journalPath))
                throw new IOException("Invalid ballot journal path");
            if (Files.exists(journalPath)) {
                if (Files.isSameFile(path, journalPath))
                    throw new IOException("Invalid ballot journal path");
                for (Path owned : OPEN_FILES) {
                    if (Files.isSameFile(owned, journalPath))
                        throw new IOException("Ballot journal already in use");
                }
            }
            journal = new RandomAccessFile(journalPath.toFile(), "rw");
            sequence = Math.max(sequence, readJournal(journal));
            // Persist the journal directory entry before any counter overwrite can begin.
            journal.getFD().sync();
            try (FileChannel directoryChannel =
                    FileChannel.open(path.getParent(), StandardOpenOption.READ)) {
                directoryChannel.force(true);
            }
            BallotSequenceStore store =
                    new BallotSequenceStore(path, file, journal, nodeId, sequence);
            OPEN_FILES.add(path);
            return store;
        } catch (IOException | RuntimeException | Error failure) {
            // Closing the channel also releases its lock, including after validation fails.
            if (journal != null) {
                try {
                    journal.close();
                } catch (IOException cleanup) {
                    failure.addSuppressed(cleanup);
                }
            }
            try {
                file.close();
            } catch (IOException cleanup) {
                failure.addSuppressed(cleanup);
            }
            throw failure;
        }
    }

    private static long readJournal(RandomAccessFile journal) throws IOException {
        if (journal.length() % (2 * Long.BYTES) != 0)
            throw new IOException("Incomplete ballot journal");
        long highest = 0;
        journal.seek(0);
        while (journal.getFilePointer() < journal.length()) {
            long value = journal.readLong();
            long complement = journal.readLong();
            if (value <= highest || value > MAX_SEQUENCE || complement != ~value)
                throw new IOException("Corrupt ballot journal");
            highest = value;
        }
        return highest;
    }

    public synchronized boolean isHealthy() {
        return !closed && !failed;
    }

    /** Returns only after the next counter is synced; observed ballots can only raise it. */
    public synchronized long nextBallot(long observedBallot) throws IOException {
        if (!isHealthy()) throw new IllegalStateException("Ballot store is closed or failed");
        long next = Math.max(sequence, observedBallot >>> 16) + 1;
        if (next > MAX_SEQUENCE) throw new IllegalStateException("Ballot sequence exhausted");
        try {
            journal.seek(journal.length());
            journal.writeLong(next);
            journal.writeLong(~next);
            journal.getFD().sync();
            file.seek(0);
            file.writeLong(next);
            file.getFD().sync();
        } catch (IOException failure) {
            // A failed write may have left a partial record. Keep ownership but stop issuing
            // ballots until an explicit close/reopen validates both durable files.
            failed = true;
            throw failure;
        }
        sequence = next;
        return (sequence << 16) | nodeId;
    }

    @Override
    public synchronized void close() throws IOException {
        if (closed) return;
        synchronized (BallotSequenceStore.class) {
            IOException failure = null;
            try {
                journal.close();
            } catch (IOException cleanup) {
                failure = cleanup;
            }
            try {
                file.close();
            } catch (IOException cleanup) {
                if (failure == null) failure = cleanup;
                else failure.addSuppressed(cleanup);
            }
            if (failure != null) {
                failed = true;
                throw failure;
            }
            OPEN_FILES.remove(path);
            closed = true;
        }
    }
}
