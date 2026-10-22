package com.luairan;

import com.luairan.business.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

/** Executed against a real PostgreSQL service in CI; opt in locally with the same environment. */
@EnabledIfEnvironmentVariable(named = "FENCING_TEST_JDBC_URL", matches = ".+")
class PostgresFencingTest {
    private DriverManagerDataSource database() {
        DriverManagerDataSource database = new DriverManagerDataSource(System.getenv("FENCING_TEST_JDBC_URL"),
                System.getenv("FENCING_TEST_DB_USER"), System.getenv("FENCING_TEST_DB_PASSWORD"));
        new ResourceDatabasePopulator(new ClassPathResource("db/fencing.sql")).execute(database); return database;
    }
    @Test void transactionFenceIsAtomicOnPostgres() throws Exception { StorageConcurrencyContract.verify(database()); }
    @Test void restartedRepositoryDoesNotResetPostgresHighWaterMark() {
        var ds = database(); String namespace = "pg-" + UUID.randomUUID();
        JdbcFencedStore a = new JdbcFencedStore(ds, namespace), b = new JdbcFencedStore(ds, namespace);
        WritePermit old = a.activate(10, "old"), next = b.activate(20, "new");
        b.write(next, "key", "new");
        JdbcFencedStore restarted = new JdbcFencedStore(ds, namespace);
        assertThrows(FencedOutException.class, () -> restarted.activate(10, "old"));
        assertThrows(FencedOutException.class, () -> restarted.write(old, "key", "bad"));
        assertEquals("new", restarted.read("key").orElseThrow());
    }
}
