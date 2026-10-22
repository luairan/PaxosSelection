package com.luairan;

import com.luairan.business.*;
import com.luairan.service.context.*;
import com.luairan.service.paxoslease.*;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import javax.sql.DataSource;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;

class FencingTest {
    @TempDir Path directory;
    private final AtomicLong clock = new AtomicLong();
    private final List<State> nodes = new ArrayList<>();
    private DataSource database;
    @BeforeEach void database() {
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        database = ds;
        new ResourceDatabasePopulator(new ClassPathResource("db/fencing.sql")).execute(ds);
    }
    @AfterEach void close() throws Exception { for (State state : nodes) state.close(); }
    private State node(int id) throws Exception {
        State state = new State(id, directory, clock::get); nodes.add(state); return state;
    }
    private long elect(State candidate, State... voters) {
        Proposer round = new Proposer(candidate, List.of("a", "b", "c"));
        Request prepare = round.propose(); Request accept = null;
        for (int i = 0; i < voters.length; i++)
            if (round.receivePrepare(List.of("a", "b", "c").get(i), new AcceptorServiceImpl(voters[i]).prepareRequest(prepare)))
                accept = round.prepareRequestOne();
        assertNotNull(accept);
        for (int i = 0; i < voters.length; i++)
            round.receiveAccept(List.of("a", "b", "c").get(i), new AcceptorServiceImpl(voters[i]).proposeRequest(accept));
        assertTrue(round.isFinished()); return round.getBallot();
    }
    @Test void resumedFormerLeaderCannotOverwriteSuccessorAcrossIndependentStores() throws Exception {
        State a = node(1), b = node(2), c = node(3); clock.set(TimeUnit.SECONDS.toNanos(30));
        elect(a, a, b, c);
        JdbcFencedStore oldStore = new JdbcFencedStore(database, "cluster");
        JdbcFencedStore newStore = new JdbcFencedStore(database, "cluster");
        WritePermit oldPermit = new FencedWriteService(a, oldStore).prepareWrite();
        oldStore.write(oldPermit, "order", "old-value");
        CountDownLatch paused = new CountDownLatch(1), resume = new CountDownLatch(1);
        ExecutorService worker = Executors.newSingleThreadExecutor();
        try {
            Future<?> pending = worker.submit(() -> {
                paused.countDown();
                try { assertTrue(resume.await(5, TimeUnit.SECONDS)); }
                catch (InterruptedException e) { throw new RuntimeException(e); }
                // No local leadership check: storage alone must reject the stale operation.
                assertThrows(FencedOutException.class, () -> oldStore.write(oldPermit, "order", "stale-overwrite"));
            });
            assertTrue(paused.await(5, TimeUnit.SECONDS));
            clock.addAndGet(TimeUnit.SECONDS.toNanos(31)); elect(b, a, b, c);
            new FencedWriteService(b, newStore).write("order", "new-value");
            resume.countDown(); pending.get(5, TimeUnit.SECONDS);
            assertEquals("new-value", newStore.read("order").orElseThrow());
        } finally { resume.countDown(); worker.shutdownNow(); }
    }
    @Test void usesConfirmedLeaseRatherThanUnsuccessfulNextBallot() throws Exception {
        State a = node(1), b = node(2), c = node(3); clock.set(TimeUnit.SECONDS.toNanos(30));
        long confirmed = elect(a, a, b, c);
        new Proposer(a, List.of("a", "b", "c")); // No quorum, but advances local proposal counter.
        assertTrue(a.getBallotNumber() > confirmed);
        WritePermit permit = new FencedWriteService(a, new JdbcFencedStore(database, "cluster")).prepareWrite();
        assertEquals(confirmed, permit.token());
    }
    @Test void highWaterMarkSurvivesApplicationRestartAndRejectsOtherOwnerOrNamespace() {
        JdbcFencedStore store = new JdbcFencedStore(database, "cluster");
        WritePermit old = store.activate(100, "old"); store.write(old, "key", "old");
        WritePermit current = store.activate(200, "new"); store.write(current, "key", "new");
        JdbcFencedStore restarted = new JdbcFencedStore(database, "cluster");
        assertThrows(FencedOutException.class, () -> restarted.activate(100, "old"));
        assertThrows(FencedOutException.class, () -> restarted.write(old, "key", "bad"));
        assertThrows(FencedOutException.class, () -> restarted.activate(200, "other-owner"));
        assertThrows(FencedOutException.class, () -> restarted.write(new WritePermit("other", 200, "new"), "key", "bad"));
        assertEquals("new", restarted.read("key").orElseThrow());
    }
    @Test void durableFileDatabaseRetainsFenceAfterAllConnectionsClose() {
        JdbcDataSource ds = new JdbcDataSource(); ds.setURL("jdbc:h2:file:" + directory.resolve("business"));
        new ResourceDatabasePopulator(new ClassPathResource("db/fencing.sql")).execute(ds);
        JdbcFencedStore first = new JdbcFencedStore(ds, "cluster");
        WritePermit old = first.activate(100, "old"); first.activate(200, "new");
        JdbcDataSource reopened = new JdbcDataSource(); reopened.setURL(ds.getURL());
        assertThrows(FencedOutException.class, () -> new JdbcFencedStore(reopened, "cluster").write(old, "key", "bad"));
    }
    @Test void expiredLeaseCannotExposePermitAfterSlowStorageActivation() throws Exception {
        State a = node(1), b = node(2), c = node(3); clock.set(TimeUnit.SECONDS.toNanos(30)); elect(a, a, b, c);
        JdbcFencedStore real = new JdbcFencedStore(database, "cluster");
        FencedStore slow = new FencedStore() {
            public WritePermit activate(long token, String owner) {
                WritePermit permit = real.activate(token, owner); clock.addAndGet(TimeUnit.SECONDS.toNanos(31)); return permit;
            }
            public void write(WritePermit permit, String key, String value) { fail("Expired preparation must not write"); }
            public Optional<String> read(String key) { return real.read(key); }
        };
        assertThrows(IllegalStateException.class, () -> new FencedWriteService(a, slow).write("key", "bad"));
        assertTrue(real.read("key").isEmpty());
    }
    @Test void storageAheadOfRecoveredAcceptorsForcesNewHigherElection() throws Exception {
        State a = node(1), b = node(2), c = node(3); clock.set(TimeUnit.SECONDS.toNanos(30)); elect(a, a, b, c);
        JdbcFencedStore store = new JdbcFencedStore(database, "cluster"); store.activate(9000000, "former");
        FencedWriteService service = new FencedWriteService(a, store);
        assertThrows(FencedOutException.class, () -> service.write("key", "bad"));
        long newBallot = elect(a, a, b, c); assertTrue(newBallot > 9000000);
        service.write("key", "good"); assertEquals("good", store.read("key").orElseThrow());
    }
    @Test void noLeaseOrExpiredLeaseNeverWrites() throws Exception {
        State a = node(1), b = node(2), c = node(3);
        JdbcFencedStore store = new JdbcFencedStore(database, "cluster");
        FencedWriteService service = new FencedWriteService(a, store);
        assertThrows(IllegalStateException.class, () -> service.write("key", "bad"));
        clock.set(TimeUnit.SECONDS.toNanos(30)); elect(a, a, b, c);
        clock.addAndGet(TimeUnit.SECONDS.toNanos(31));
        assertThrows(IllegalStateException.class, () -> service.write("key", "bad"));
        assertTrue(store.read("key").isEmpty());
    }
    @Test void fenceCheckAndMutationShareOneDatabaseTransaction() throws Exception { StorageConcurrencyContract.verify(database); }
    @Test void sameGenerationCanWriteMultipleKeysAndRenewalFencesPreviousGeneration() {
        JdbcFencedStore store = new JdbcFencedStore(database, "cluster");
        WritePermit old = store.activate(10, "node"); assertEquals(old, store.activate(10, "node"));
        store.write(old, "a", "one"); store.write(old, "b", "two"); store.write(old, "a", "updated");
        WritePermit renewed = store.activate(20, "node"); store.write(renewed, "a", "renewed");
        assertThrows(FencedOutException.class, () -> store.write(old, "b", "bad"));
        assertEquals("two", store.read("b").orElseThrow());
    }
    @Test void issuedFenceIsNotUndoneByAnUnrelatedCallerTransactionRollback() {
        JdbcFencedStore store = new JdbcFencedStore(database, "cluster");
        var caller = new org.springframework.transaction.support.TransactionTemplate(
                new org.springframework.jdbc.datasource.DataSourceTransactionManager(database));
        caller.executeWithoutResult(status -> { store.activate(100, "owner"); status.setRollbackOnly(); });
        assertThrows(FencedOutException.class, () -> store.activate(50, "old"));
    }
    @Test void failedMutationRollsBackBusinessDataAndReleasesGuard() {
        JdbcFencedStore store = new JdbcFencedStore(database, "cluster"); WritePermit permit = store.activate(10, "node");
        store.write(permit, "key", "good");
        new JdbcTemplate(database).execute("ALTER TABLE business_entry ADD CONSTRAINT no_bad CHECK (entry_value <> 'bad')");
        assertThrows(org.springframework.dao.DataAccessException.class, () -> store.write(permit, "key", "bad"));
        assertEquals("good", store.read("key").orElseThrow());
        store.activate(20, "next"); assertThrows(FencedOutException.class, () -> store.write(permit, "key", "bad"));
    }
}
