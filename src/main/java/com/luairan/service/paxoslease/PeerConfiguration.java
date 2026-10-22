package com.luairan.service.paxoslease;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.*;

/** Validated, immutable membership used by the election service. */
@Component
public final class PeerConfiguration {
    private final List<String> peers;

    public PeerConfiguration(
            @Value("${paxos.peers:ws://localhost:8080/paxos}") String configuredPeers) {
        Set<String> unique = new LinkedHashSet<>();
        for (String entry : configuredPeers.split(",")) {
            URI uri = URI.create(entry.trim()).normalize();
            if (!("ws".equals(uri.getScheme()) || "wss".equals(uri.getScheme()))
                    || uri.getHost() == null
                    || uri.getPort() < 1
                    || !"/paxos".equals(uri.getPath())
                    || uri.getQuery() != null
                    || uri.getFragment() != null
                    || uri.getUserInfo() != null)
                throw new IllegalArgumentException("Peers must be ws(s)://host:port/paxos");
            if ("ws".equals(uri.getScheme())
                    && !Set.of("localhost", "127.0.0.1", "[::1]", "::1")
                            .contains(uri.getHost().toLowerCase(Locale.ROOT)))
                throw new IllegalArgumentException(
                        "Remote peers require wss to protect node credentials");
            if (!unique.add(uri.toString())) throw new IllegalArgumentException("Duplicate peer");
        }
        peers = List.copyOf(unique);
    }

    public List<String> peers() {
        return peers;
    }
}
