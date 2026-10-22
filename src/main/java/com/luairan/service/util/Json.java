package com.luairan.service.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Explicit DTO serialization; polymorphic/default typing is never enabled. */
public final class Json {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private Json() { }
    public static String toJSONString(Object value) {
        try { return MAPPER.writeValueAsString(value); }
        catch (JsonProcessingException e) { throw new IllegalArgumentException("Invalid protocol payload", e); }
    }
    public static <T> T parseObject(String text, Class<T> type) {
        try { return MAPPER.readValue(text, type); }
        catch (JsonProcessingException e) { throw new IllegalArgumentException("Invalid protocol payload", e); }
    }
}
