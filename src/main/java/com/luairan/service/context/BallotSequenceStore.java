package com.luairan.service.context;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.Path;
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
    private final int nodeId;
    private long sequence;
    private boolean closed;

    private BallotSequenceStore(Path path, RandomAccessFile file, int nodeId, long sequence) {
        this.path = path;
        this.file = file;
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
            BallotSequenceStore store = new BallotSequenceStore(path, file, nodeId, sequence);
            OPEN_FILES.add(path);
            return store;
        } catch (IOException | RuntimeException | Error failure) {
            // Closing the channel also releases its lock, including after validation fails.
            try {
                file.close();
            } catch (IOException cleanup) {
                failure.addSuppressed(cleanup);
            }
            throw failure;
        }
    }

    /** Returns only after the next counter is synced; observed ballots can only raise it. */
    public synchronized long nextBallot(long observedBallot) throws IOException {
        if (closed) throw new IllegalStateException("Ballot store is closed");
        long next = Math.max(sequence, observedBallot >>> 16) + 1;
        if (next > MAX_SEQUENCE) throw new IllegalStateException("Ballot sequence exhausted");
        file.seek(0);
        file.writeLong(next);
        file.getFD().sync();
        sequence = next;
        return (sequence << 16) | nodeId;
    }

    @Override
    public synchronized void close() throws IOException {
        if (closed) return;
        synchronized (BallotSequenceStore.class) {
            file.close();
            OPEN_FILES.remove(path);
            closed = true;
        }
    }
}
