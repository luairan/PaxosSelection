package com.luairan.service.context;

/** A successful lease, never an unconfirmed next-election ballot. */
public record LeaseGrant(long ballot, String nodeId, long startedAtNanos) { }
