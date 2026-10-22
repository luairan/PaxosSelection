package com.luairan;

import com.luairan.security.SecurityTokens;
import com.luairan.service.context.Request;
import com.luairan.service.util.Json;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import java.net.URI;
import java.net.http.*;
import java.util.concurrent.CompletionException;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "paxos.security.node-token=security-test-node-01234567890123456789",
    "paxos.security.admin-token=security-test-admin-01234567890123456789",
    "paxos.data-dir=${java.io.tmpdir}/paxos-security-${random.uuid}", "logging.level.root=ERROR"
})
class ApiSecurityTest {
    private static final String NODE = "security-test-node-01234567890123456789";
    private static final String ADMIN = "security-test-admin-01234567890123456789";
    @LocalServerPort int port;
    private final HttpClient client = HttpClient.newHttpClient();
    private HttpResponse<String> call(String path, String method, String token) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path));
        if (token != null) b.header("Authorization", "Bearer " + token);
        return client.send(b.method(method, HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());
    }
    @Test void rejectsMissingInvalidAndQueryStringCredentials() throws Exception {
        assertEquals(401, call("/show/showMater", "GET", null).statusCode());
        assertEquals(401, call("/show/start", "POST", "wrong").statusCode());
        assertEquals(401, call("/show/start?token=" + ADMIN, "POST", null).statusCode());
    }
    @Test void separatesNodeAndAdminRoles() throws Exception {
        assertEquals(403, call("/show/showMater", "GET", NODE).statusCode());
        assertEquals(403, call("/show/start", "POST", NODE).statusCode());
        HttpResponse<String> status = call("/show/showMater", "GET", ADMIN);
        assertTrue(status.headers().allValues("Set-Cookie").isEmpty());
        assertEquals(200, status.statusCode()); assertTrue(status.body().contains("leaseOwner"));
        assertFalse(status.body().contains(ADMIN)); assertFalse(status.body().contains(NODE));
        assertFalse(status.body().contains("dataDir"));
    }
    @Test void requiresPostForMutationsAndDoesNotAuthenticateViaCookie() throws Exception {
        assertEquals(405, call("/show/start", "GET", ADMIN).statusCode());
        assertEquals(200, call("/show/start", "POST", ADMIN).statusCode());
        assertEquals(200, call("/show/stop", "POST", ADMIN).statusCode());
        HttpRequest cookie = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/show/showMater"))
            .header("Cookie", "Authorization=Bearer%20" + ADMIN).GET().build();
        assertEquals(401, client.send(cookie, HttpResponse.BodyHandlers.discarding()).statusCode());
    }
    private int deniedHandshake(String token, String origin) {
        WebSocket.Builder b = client.newWebSocketBuilder();
        if (token != null) b.header("Authorization", "Bearer " + token);
        if (origin != null) b.header("Origin", origin);
        CompletionException error = assertThrows(CompletionException.class,
            () -> b.buildAsync(URI.create("ws://localhost:" + port + "/paxos"), new WebSocket.Listener() { }).join());
        assertInstanceOf(WebSocketHandshakeException.class, error.getCause());
        return ((WebSocketHandshakeException) error.getCause()).getResponse().statusCode();
    }
    @Test void authenticatesWebSocketUpgradeAndRejectsCrossOrigin() {
        assertEquals(401, deniedHandshake(null, null));
        assertEquals(403, deniedHandshake(ADMIN, null));
        assertEquals(403, deniedHandshake(NODE, "https://untrusted.example"));
        WebSocket socket = client.newWebSocketBuilder().header("Authorization", "Bearer " + NODE)
            .buildAsync(URI.create("ws://localhost:" + port + "/paxos"), new WebSocket.Listener() { }).join();
        socket.abort();
    }
    @Test void businessApiIsAbsentByDefault() throws Exception {
        assertEquals(404, call("/business/entries/key", "GET", ADMIN).statusCode());
    }
    @Test void rejectsAbsentWeakOrSharedSecrets() {
        assertThrows(IllegalArgumentException.class, () -> new SecurityTokens("", ""));
        assertThrows(IllegalArgumentException.class, () -> new SecurityTokens("short", ADMIN));
        assertThrows(IllegalArgumentException.class, () -> new SecurityTokens(NODE, NODE));
        assertThrows(IllegalArgumentException.class, () -> new SecurityTokens(NODE + "\r\n", ADMIN));
    }
    @Test void applicationContextFailsClosedWithoutCredentials() {
        new org.springframework.boot.test.context.runner.ApplicationContextRunner()
            .withUserConfiguration(SecurityTokens.class).run(context -> {
                assertNotNull(context.getStartupFailure());
                assertTrue(context.getStartupFailure().getMessage().contains("securityTokens"));
            });
    }
    @Test void rejectsPlaintextRemotePeers() {
        com.luairan.service.paxoslease.ProposerServiceImpl service = new com.luairan.service.paxoslease.ProposerServiceImpl();
        org.springframework.test.util.ReflectionTestUtils.setField(service, "state", org.mockito.Mockito.mock(com.luairan.service.context.State.class));
        try {
            org.springframework.test.util.ReflectionTestUtils.setField(service, "configuredPeers", "ws://remote.example:8080/paxos");
            assertThrows(IllegalArgumentException.class, service::initialize);
            org.springframework.test.util.ReflectionTestUtils.setField(service, "configuredPeers", "wss://remote.example:8443/paxos");
            assertDoesNotThrow(service::initialize);
        } finally { service.close(); }
    }
    @Test void rejectsPolymorphicAndUnknownProtocolFields() {
        assertThrows(IllegalArgumentException.class,
            () -> Json.parseObject("{\"@type\":\"java.lang.Runtime\",\"ballotNumber\":1}", Request.class));
    }
}
