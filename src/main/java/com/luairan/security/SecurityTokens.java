package com.luairan.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
public final class SecurityTokens {
    private final String nodeToken, adminToken;
    public SecurityTokens(@Value("${paxos.security.node-token:}") String nodeToken,
                          @Value("${paxos.security.admin-token:}") String adminToken) {
        if (nodeToken.length() < 32 || adminToken.length() < 32 || nodeToken.isBlank() || adminToken.isBlank()
                || nodeToken.chars().anyMatch(c -> c < 33 || c > 126) || adminToken.chars().anyMatch(c -> c < 33 || c > 126)
                || nodeToken.equals(adminToken))
            throw new IllegalArgumentException("Configure distinct node/admin tokens of at least 32 characters");
        this.nodeToken = nodeToken; this.adminToken = adminToken;
    }
    public String nodeAuthorization() { return "Bearer " + nodeToken; }
    public String role(String header) {
        if (header == null || !header.startsWith("Bearer ")) return null;
        byte[] supplied = header.substring(7).getBytes(StandardCharsets.UTF_8);
        if (MessageDigest.isEqual(supplied, nodeToken.getBytes(StandardCharsets.UTF_8))) return "NODE";
        if (MessageDigest.isEqual(supplied, adminToken.getBytes(StandardCharsets.UTF_8))) return "ADMIN";
        return null;
    }
}
