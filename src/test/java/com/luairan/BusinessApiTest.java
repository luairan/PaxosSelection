package com.luairan;

import com.luairan.business.FencedStore;
import com.luairan.service.context.State;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import java.net.URI;
import java.net.http.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "paxos.security.node-token=business-test-node-01234567890123456789",
    "paxos.security.admin-token=business-test-admin-01234567890123456789",
    "paxos.data-dir=${java.io.tmpdir}/business-api-${random.uuid}",
    "paxos.business.enabled=true", "paxos.business.initialize-schema=true",
    "paxos.business.datasource.jdbc-url=jdbc:h2:mem:business-api-${random.uuid};MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
    "paxos.business.datasource.username=sa", "logging.level.root=ERROR"
})
class BusinessApiTest {
    @LocalServerPort int port;
    @Autowired State state;
    @Autowired FencedStore store;
    @Autowired com.zaxxer.hikari.HikariDataSource database;
    private final HttpClient client = HttpClient.newHttpClient();
    private HttpResponse<String> call(String method, String token, String body) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/business/entries/order"));
        if (token != null) request.header("Authorization", "Bearer " + token);
        request.header("Content-Type", "application/json");
        return client.send(request.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }
    @Test void businessWriteRequiresAdminAndCurrentStorageGeneration() throws Exception {
        String admin = "business-test-admin-01234567890123456789", node = "business-test-node-01234567890123456789";
        try {
            assertEquals(401, call("PUT", null, "{\"value\":\"bad\"}").statusCode());
            assertEquals(403, call("PUT", node, "{\"value\":\"bad\"}").statusCode());
            assertEquals(403, call("GET", node, null).statusCode());
            assertEquals(409, call("PUT", admin, "{\"value\":\"bad\"}").statusCode());
            // Protocol quorum behavior is tested independently; here exercise the real HTTP/JDBC boundary.
            state.grantOwnership(state.now(), 100);
            assertEquals(204, call("PUT", admin, "{\"value\":\"first\"}").statusCode());
            assertEquals("{\"value\":\"first\"}", call("GET", admin, null).body());
            store.activate(200, "successor");
            assertEquals(409, call("PUT", admin, "{\"value\":\"stale\"}").statusCode());
            assertEquals("first", store.read("order").orElseThrow());
            state.grantOwnership(state.now(), 300);
            assertEquals(204, call("PUT", admin, "{\"value\":\"second\"}").statusCode());
            assertEquals(400, call("PUT", admin, "{}").statusCode());
            assertEquals("second", store.read("order").orElseThrow());
            database.close();
            HttpResponse<String> unavailable = call("PUT", admin, "{\"value\":\"bad\"}");
            assertEquals(503, unavailable.statusCode());
            assertFalse(unavailable.body().contains("jdbc:"));
        } finally { state.revokeOwnership(); }
    }
}
