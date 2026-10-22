package com.luairan.business;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import javax.sql.DataSource;
import java.util.Objects;
import java.util.Optional;

/** The guard row lock is held until the business mutation commits or rolls back. */
public final class JdbcFencedStore implements FencedStore {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transactions;
    private final String namespace;

    public JdbcFencedStore(DataSource dataSource, String namespace) {
        if (namespace == null || !namespace.matches("[A-Za-z0-9._-]{1,64}"))
            throw new IllegalArgumentException("Invalid business namespace");
        this.namespace = namespace;
        jdbc = new JdbcTemplate(dataSource);
        transactions = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        transactions.setTimeout(5);
        transactions.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        // Bootstrap outside the guarded transaction: duplicate INSERT aborts a PostgreSQL transaction.
        try {
            jdbc.update("INSERT INTO business_fence(resource_namespace,fence_token,owner_id) VALUES (?,0,'')", namespace);
        } catch (DuplicateKeyException alreadyInitialized) { /* Preserve the durable high-water mark. */ }
    }
    @Override public WritePermit activate(long winningBallot, String ownerId) {
        if (winningBallot <= 0 || ownerId == null || ownerId.isBlank() || ownerId.length() > 64)
            throw new IllegalArgumentException("Invalid lease identity");
        return transactions.execute(status -> {
            WritePermit current = lockFence();
            if (winningBallot < current.token() || (winningBallot == current.token() && !ownerId.equals(current.ownerId())))
                throw new FencedOutException(current.token());
            if (winningBallot > current.token())
                jdbc.update("UPDATE business_fence SET fence_token=?,owner_id=? WHERE resource_namespace=?", winningBallot, ownerId, namespace);
            return new WritePermit(namespace, winningBallot, ownerId);
        });
    }
    @Override public void write(WritePermit permit, String key, String value) {
        validateEntry(key, value);
        Objects.requireNonNull(permit, "permit");
        transactions.executeWithoutResult(status -> {
            WritePermit current = lockFence();
            if (!current.equals(permit) || permit.token() <= 0) throw new FencedOutException(current.token());
            int changed = jdbc.update("UPDATE business_entry SET entry_value=?,fence_token=? WHERE resource_namespace=? AND entry_key=?",
                    value, permit.token(), namespace, key);
            if (changed == 0)
                jdbc.update("INSERT INTO business_entry(resource_namespace,entry_key,entry_value,fence_token) VALUES (?,?,?,?)",
                        namespace, key, value, permit.token());
        });
    }
    @Override public Optional<String> read(String key) {
        validateEntry(key, "");
        return jdbc.query("SELECT entry_value FROM business_entry WHERE resource_namespace=? AND entry_key=?",
                (rs, row) -> rs.getString(1), namespace, key).stream().findFirst();
    }
    private WritePermit lockFence() {
        return jdbc.queryForObject("SELECT fence_token,owner_id FROM business_fence WHERE resource_namespace=? FOR UPDATE",
                (rs, row) -> new WritePermit(namespace, rs.getLong(1), rs.getString(2)), namespace);
    }
    private static void validateEntry(String key, String value) {
        if (key == null || key.isBlank() || key.length() > 128 || value == null || value.length() > 4096)
            throw new IllegalArgumentException("Key must be 1..128 characters and value at most 4096 characters");
    }
}
