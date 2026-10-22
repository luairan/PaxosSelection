package com.luairan.business;

import com.luairan.service.context.LeaseGrant;
import com.luairan.service.context.State;
import com.luairan.service.context.LeaseUnavailableException;
import java.util.Optional;

/** Captures a confirmed lease and registers it at storage before exposing a permit. */
public final class FencedWriteService {
    private final State state;
    private final FencedStore store;
    public FencedWriteService(State state, FencedStore store) { this.state = state; this.store = store; }
    public WritePermit prepareWrite() {
        LeaseGrant lease = state.currentLease();
        WritePermit permit;
        try { permit = store.activate(lease.ballot(), lease.nodeId()); }
        catch (FencedOutException newerGeneration) {
            // Acceptors can forget promises after recovery. Never lower the storage fence;
            // make the next election jump above the durable storage high-water mark instead.
            state.observeBallot(newerGeneration.currentToken()); throw newerGeneration;
        }
        // Activation may block or the process may pause. Never expose an expired grant.
        if (!state.isCurrentLease(lease)) throw new LeaseUnavailableException();
        return permit;
    }
    public void write(String key, String value) { store.write(prepareWrite(), key, value); }
    public Optional<String> read(String key) { return store.read(key); }
}
