package com.luairan.business;

/** Ordering credential for this storage namespace, not an authentication secret. */
public record WritePermit(String namespace, long token, String ownerId) { }
