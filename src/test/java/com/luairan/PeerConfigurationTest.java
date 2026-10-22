package com.luairan;

import static org.junit.jupiter.api.Assertions.*;

import com.luairan.service.paxoslease.PeerConfiguration;

import org.junit.jupiter.api.Test;

import java.util.List;

class PeerConfigurationTest {
    @Test
    void preservesConfiguredOrderAndExposesImmutableMembership() {
        var configuration =
                new PeerConfiguration(
                        " ws://localhost:8001/paxos , wss://peer.example:8443/paxos ");
        assertEquals(
                List.of("ws://localhost:8001/paxos", "wss://peer.example:8443/paxos"),
                configuration.peers());
        assertThrows(UnsupportedOperationException.class, () -> configuration.peers().clear());
    }

    @Test
    void rejectsDuplicateAndInvalidEndpoints() {
        for (String peers :
                List.of(
                        "",
                        "ws://localhost:8001/paxos,ws://localhost:8001/paxos",
                        "http://localhost:8001/paxos",
                        "ws://localhost/paxos",
                        "ws://localhost:8001/other",
                        "wss://peer.example:8443/paxos?token=secret",
                        "wss://user@peer.example:8443/paxos",
                        "wss://peer.example:8443/paxos#fragment",
                        "ws://peer.example:8001/paxos")) {
            assertThrows(IllegalArgumentException.class, () -> new PeerConfiguration(peers), peers);
        }
    }

    @Test
    void allowsPlaintextOnlyForExplicitLoopbackHosts() {
        assertEquals(
                3,
                new PeerConfiguration(
                                "ws://localhost:8001/paxos,ws://127.0.0.1:8002/paxos,ws://[::1]:8003/paxos")
                        .peers()
                        .size());
        assertThrows(
                IllegalArgumentException.class,
                () -> new PeerConfiguration("ws://localhost.attacker.example:8001/paxos"));
    }
}
