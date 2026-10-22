package com.luairan.service.context;

/** No confirmed lease is currently usable for starting a business operation. */
public final class LeaseUnavailableException extends IllegalStateException {
    public LeaseUnavailableException() { super("No current leadership lease"); }
}
