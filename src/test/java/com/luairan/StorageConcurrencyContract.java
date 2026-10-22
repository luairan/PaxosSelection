package com.luairan;

import com.luairan.business.*;
import org.springframework.jdbc.datasource.AbstractDataSource;
import javax.sql.DataSource;
import java.lang.reflect.*;
import java.sql.*;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.*;

final class StorageConcurrencyContract {
    static void verify(DataSource database) throws Exception {
        CountDownLatch checked = new CountDownLatch(1), resume = new CountDownLatch(1), activating = new CountDownLatch(1);
        AtomicBoolean pauseOnce = new AtomicBoolean(true);
        // Pause precisely after the repository validates its fence but before its SQL mutation.
        DataSource paused = new AbstractDataSource() {
            public Connection getConnection() throws SQLException { return wrap(database.getConnection()); }
            public Connection getConnection(String u, String p) throws SQLException { return wrap(database.getConnection(u, p)); }
            private Connection wrap(Connection connection) {
                return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(), new Class<?>[]{Connection.class}, (proxy, method, args) -> {
                    Object result = invoke(method, connection, args);
                    if (method.getName().equals("prepareStatement") && args[0].toString().startsWith("UPDATE business_entry")) {
                        PreparedStatement statement = (PreparedStatement) result;
                        return Proxy.newProxyInstance(PreparedStatement.class.getClassLoader(), new Class<?>[]{PreparedStatement.class}, (p, m, a) -> {
                            if (m.getName().equals("executeUpdate") && pauseOnce.compareAndSet(true, false)) {
                                checked.countDown(); assertTrue(resume.await(5, TimeUnit.SECONDS));
                            }
                            return invoke(m, statement, a);
                        });
                    }
                    return result;
                });
            }
        };
        String namespace = "race-" + UUID.randomUUID();
        JdbcFencedStore oldStore = new JdbcFencedStore(paused, namespace), nextStore = new JdbcFencedStore(database, namespace);
        WritePermit old = oldStore.activate(10, "old");
        ExecutorService threads = Executors.newFixedThreadPool(2);
        try {
            Future<?> oldWrite = threads.submit(() -> oldStore.write(old, "key", "old"));
            assertTrue(checked.await(5, TimeUnit.SECONDS));
            Future<WritePermit> takeover = threads.submit(() -> { activating.countDown(); return nextStore.activate(20, "new"); });
            assertTrue(activating.await(5, TimeUnit.SECONDS));
            assertThrows(TimeoutException.class, () -> takeover.get(200, TimeUnit.MILLISECONDS), "Activation must wait for in-flight mutation to commit");
            resume.countDown(); oldWrite.get(5, TimeUnit.SECONDS);
            WritePermit next = takeover.get(5, TimeUnit.SECONDS);
            nextStore.write(next, "key", "new");
            assertThrows(FencedOutException.class, () -> oldStore.write(old, "key", "stale"));
            assertEquals("new", nextStore.read("key").orElseThrow());
        } finally { resume.countDown(); threads.shutdownNow(); }
    }
    private static Object invoke(Method method, Object target, Object[] args) throws Throwable {
        try { return method.invoke(target, args); }
        catch (InvocationTargetException e) { throw e.getCause(); }
    }
}
