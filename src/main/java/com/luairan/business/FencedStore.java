package com.luairan.business;

import java.util.Optional;

/** Every implementation must linearize activation and writes at the shared resource. */
public interface FencedStore {
    WritePermit activate(long winningBallot, String ownerId);
    void write(WritePermit permit, String key, String value);
    Optional<String> read(String key);
}
