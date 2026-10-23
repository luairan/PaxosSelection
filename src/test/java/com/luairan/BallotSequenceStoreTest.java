package com.luairan;

import static org.junit.jupiter.api.Assertions.*;

import com.luairan.service.context.BallotSequenceStore;
import com.luairan.service.context.State;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

class BallotSequenceStoreTest {
    @TempDir Path directory;

    private Path file() {
        return directory.resolve("node-1.ballot");
    }

    private void counter(long value) throws IOException {
        Files.write(file(), ByteBuffer.allocate(Long.BYTES).putLong(value).array());
    }

    private State uninitialized(AtomicLong now) {
        State state = new State();
        ReflectionTestUtils.setField(state, "dataDir", directory.toString());
        ReflectionTestUtils.setField(state, "clock", (java.util.function.LongSupplier) now::get);
        return state;
    }

    @Test
    void corruptLengthsAndValuesReleaseLockWithoutChangingFile() throws Exception {
        byte[][] corrupt = {
            new byte[1],
            new byte[7],
            new byte[9],
            ByteBuffer.allocate(8).putLong(-1).array(),
            ByteBuffer.allocate(8).putLong((Long.MAX_VALUE >>> 16) + 1).array()
        };
        for (byte[] bytes : corrupt) {
            Files.write(file(), bytes);
            for (int attempt = 0; attempt < 2; attempt++) {
                IOException failure =
                        assertThrows(
                                IOException.class, () -> BallotSequenceStore.open(1, directory));
                assertEquals("Corrupt ballot sequence", failure.getMessage());
                assertArrayEquals(bytes, Files.readAllBytes(file()));
                try (FileChannel channel = FileChannel.open(file(), StandardOpenOption.WRITE);
                        var lock = channel.tryLock()) {
                    assertNotNull(lock, "A failed open must release its node identity lock");
                }
            }
        }
    }

    @Test
    void sameJvmConflictDoesNotDisruptOwnerAndAllowsRetryAfterClose() throws Exception {
        try (var owner = BallotSequenceStore.open(1, directory)) {
            assertEquals(65537, owner.nextBallot(0));
            for (int attempt = 0; attempt < 2; attempt++) {
                IOException failure =
                        assertThrows(
                                IOException.class, () -> BallotSequenceStore.open(1, directory));
                assertEquals("Node identity already in use", failure.getMessage());
            }
            assertEquals(131073, owner.nextBallot(0));
        }
        try (var retry = BallotSequenceStore.open(1, directory)) {
            assertEquals(196609, retry.nextBallot(0));
        }
    }

    private Process probe(String mode) throws IOException {
        return new ProcessBuilder(
                        Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                        "-cp",
                        System.getProperty("java.class.path"),
                        LockProcess.class.getName(),
                        file().toString(),
                        mode)
                .redirectErrorStream(true)
                .start();
    }

    @Test
    void competingProcessCannotAcquireLockAfterRejectedSameJvmOpen() throws Exception {
        try (var owner = BallotSequenceStore.open(1, directory)) {
            assertThrows(IOException.class, () -> BallotSequenceStore.open(1, directory));
            Process probe = probe("probe");
            try {
                assertTrue(probe.waitFor(10, TimeUnit.SECONDS));
                assertEquals(
                        2, probe.exitValue(), new String(probe.getInputStream().readAllBytes()));
                owner.nextBallot(0);
            } finally {
                probe.destroyForcibly();
            }
        }
    }

    @Test
    void externalLockConflictCanBeRetriedAfterOwnerExits() throws Exception {
        Process holder = probe("hold");
        try {
            var output =
                    new java.io.BufferedReader(
                            new java.io.InputStreamReader(holder.getInputStream()));
            var ready =
                    java.util.concurrent.CompletableFuture.supplyAsync(
                            () -> {
                                try {
                                    return output.readLine();
                                } catch (IOException e) {
                                    throw new java.io.UncheckedIOException(e);
                                }
                            });
            assertEquals("locked", ready.get(10, TimeUnit.SECONDS));
            assertThrows(IOException.class, () -> BallotSequenceStore.open(1, directory));
            holder.getOutputStream().close();
            assertTrue(holder.waitFor(10, TimeUnit.SECONDS));
            assertEquals(0, holder.exitValue());
            try (var retry = BallotSequenceStore.open(1, directory)) {
                assertEquals(65537, retry.nextBallot(0));
            }
        } finally {
            holder.destroyForcibly();
        }
    }

    public static class LockProcess {
        public static void main(String[] args) throws Exception {
            try (FileChannel channel =
                            FileChannel.open(
                                    Path.of(args[0]),
                                    StandardOpenOption.CREATE,
                                    StandardOpenOption.WRITE);
                    var lock = channel.tryLock()) {
                if (lock == null) {
                    System.exit(2);
                }
                System.out.println("locked");
                System.out.flush();
                if ("hold".equals(args[1])) System.in.read();
            }
        }
    }

    @Test
    void aliasesCannotBypassInProcessLockOwnership() throws Exception {
        Path alias = directory.resolve("alias");
        Files.createDirectory(alias);
        try (var owner = BallotSequenceStore.open(1, directory)) {
            Files.createLink(alias.resolve("node-1.ballot"), file());
            assertThrows(IOException.class, () -> BallotSequenceStore.open(1, alias));
            Process probe = probe("probe");
            try {
                assertTrue(probe.waitFor(10, TimeUnit.SECONDS));
                assertEquals(2, probe.exitValue());
                owner.nextBallot(0);
            } finally {
                probe.destroyForcibly();
            }
        }
        try (var retry = BallotSequenceStore.open(1, alias)) {
            assertEquals(131073, retry.nextBallot(0));
        }
    }

    @Test
    void differentNodeFilesCanBeOpenTogether() throws Exception {
        try (var first = BallotSequenceStore.open(1, directory);
                var second = BallotSequenceStore.open(2, directory)) {
            assertEquals(65537, first.nextBallot(0));
            assertEquals(65538, second.nextBallot(0));
        }
    }

    @Test
    void stateCanRetryCorruptInitializationAndRestartsRecoveryWait() throws Exception {
        AtomicLong now = new AtomicLong(TimeUnit.SECONDS.toNanos(60));
        State state = uninitialized(now);
        try {
            counter(-1);
            assertThrows(IOException.class, state::initialize);
            assertThrows(IOException.class, state::initialize);
            assertFalse(state.isReady());
            assertThrows(IllegalStateException.class, state::nextBallotNumber);
            counter(
                    7); // Simulate explicit operator repair; the store never resets a corrupt
                        // counter.
            state.initialize();
            assertFalse(state.isReady());
            assertEquals((8L << 16) | 1, state.nextBallotNumber());
            now.addAndGet(TimeUnit.SECONDS.toNanos(29));
            state.initialize(); // Repeated successful initialization must not restart the wait.
            assertFalse(state.isReady());
            now.addAndGet(TimeUnit.SECONDS.toNanos(1));
            assertTrue(state.isReady());
        } finally {
            state.close();
        }
    }

    @Test
    void stateCanRetryLockConflictWithoutPublishingPartialInitialization() throws Exception {
        State contender = uninitialized(new AtomicLong());
        try {
            try (var owner = BallotSequenceStore.open(1, directory)) {
                owner.nextBallot(0);
                assertThrows(IOException.class, contender::initialize);
                assertThrows(IOException.class, contender::initialize);
                assertFalse(contender.isReady());
            }
            contender.initialize();
            assertEquals(131073, contender.nextBallotNumber());
        } finally {
            contender.close();
        }
    }

    @Test
    void restoresCounterAndJumpsAboveObservedBallot() throws Exception {
        long ballot;
        try (var store = BallotSequenceStore.open(1, directory)) {
            ballot = store.nextBallot((100L << 16) | 65535);
            assertEquals((101L << 16) | 1, ballot);
        }
        try (var reopened = BallotSequenceStore.open(1, directory)) {
            assertEquals(ballot + (1L << 16), reopened.nextBallot(0));
        }
    }

    @Test
    void exhaustionFailsWithoutChangingDurableCounter() throws Exception {
        counter(Long.MAX_VALUE >>> 16);
        byte[] before = Files.readAllBytes(file());
        try (var store = BallotSequenceStore.open(1, directory)) {
            assertThrows(IllegalStateException.class, () -> store.nextBallot(0));
            assertArrayEquals(before, Files.readAllBytes(file()));
        }
    }

    @Test
    void closeIsIdempotentAndClosedStoreCannotIssueBallots() throws Exception {
        var store = BallotSequenceStore.open(1, directory);
        store.nextBallot(0);
        store.close();
        store.close();
        assertThrows(IllegalStateException.class, () -> store.nextBallot(0));
        try (var reopened = BallotSequenceStore.open(1, directory)) {
            assertEquals(131073, reopened.nextBallot(0));
        }
    }

    @Test
    void stateClockFailureAlsoReleasesOpenedStoreBeforeRetry() throws Exception {
        State state = uninitialized(new AtomicLong());
        ReflectionTestUtils.setField(
                state,
                "clock",
                (java.util.function.LongSupplier)
                        () -> {
                            throw new IllegalStateException("clock unavailable");
                        });
        try {
            assertThrows(IllegalStateException.class, state::initialize);
            try (var other = BallotSequenceStore.open(1, directory)) {
                other.nextBallot(0);
            }
            ReflectionTestUtils.setField(
                    state, "clock", (java.util.function.LongSupplier) () -> 0L);
            state.initialize();
            assertEquals(131073, state.nextBallotNumber());
        } finally {
            state.close();
        }
    }
}
