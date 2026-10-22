package com.luairan.business;

public final class FencedOutException extends RuntimeException {
    private final long currentToken;
    public FencedOutException(long currentToken) {
        super("Write generation has been superseded"); this.currentToken = currentToken;
    }
    public long currentToken() { return currentToken; }
}
