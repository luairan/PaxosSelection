package com.luairan;

import com.luairan.service.util.websocket.WebSocketClientEndpoint;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

class RedirectCredentialTest {
    @ParameterizedTest
    @ValueSource(ints = {301, 302, 303, 307})
    void neverForwardsNodeCredentialToRedirectDestination(int status) throws Exception {
        String authorization = "Bearer test-only-node-secret-01234567890123456789";
        AtomicReference<String> destinationAuthorization = new AtomicReference<>();
        AtomicInteger destinationRequests = new AtomicInteger();
        AtomicReference<String> originalAuthorization = new AtomicReference<>();
        HttpServer destination = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        destination.createContext("/paxos", exchange -> {
            destinationAuthorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            destinationRequests.incrementAndGet(); exchange.sendResponseHeaders(403, -1); exchange.close();
        });
        HttpServer original = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        original.createContext("/paxos", exchange -> {
            originalAuthorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            exchange.getResponseHeaders().add("Location", "ws://127.0.0.1:" + destination.getAddress().getPort() + "/paxos");
            exchange.sendResponseHeaders(status, -1); exchange.close();
        });
        try {
            destination.start(); original.start();
            URI uri = URI.create("ws://127.0.0.1:" + original.getAddress().getPort() + "/paxos");
            IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> new WebSocketClientEndpoint(uri, authorization));
            assertEquals(authorization, originalAuthorization.get(), "The configured peer receives its credential");
            assertFalse(authorization.equals(destinationAuthorization.get()), "Node credential must not reach redirect target");
            assertEquals(0, destinationRequests.get(), "No connection may follow the redirect, even without credentials");
            assertFalse(error.toString().contains(authorization));
        } finally { original.stop(0); destination.stop(0); }
    }
}
