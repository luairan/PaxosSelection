package com.luairan;

import com.luairan.service.context.State;
import com.luairan.service.paxoslease.ProposerServiceImpl;
import com.luairan.service.websocket.WebSocketServiceImpl;
import org.junit.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.embedded.EmbeddedWebApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;
import java.util.function.LongSupplier;
import static org.junit.Assert.*;

/** Three real embedded servers and WebSocket connections, with controllable lease time. */
public class WebSocketClusterTest {
    @Test public void renewsOverRealSocketsAndReconnectsAfterDisconnect() throws Exception {
        List<ConfigurableApplicationContext> contexts = new ArrayList<>();
        Path directory = Files.createTempDirectory("paxos-websocket-test");
        AtomicLong now = new AtomicLong();
        try {
            List<State> states = new ArrayList<>(); List<ProposerServiceImpl> services = new ArrayList<>();
            List<String> peers = new ArrayList<>();
            for (int id = 1; id <= 3; id++) {
                SpringApplication app = new SpringApplication(Application.class);
                app.setRegisterShutdownHook(false);
                ConfigurableApplicationContext context = app.run("--server.port=0", "--paxos.node-id=" + id,
                        "--paxos.data-dir=" + directory, "--logging.level.root=ERROR", "--spring.main.banner-mode=off");
                contexts.add(context);
                State state = context.getBean(State.class);
                ReflectionTestUtils.setField(state, "clock", (LongSupplier) now::get);
                ReflectionTestUtils.setField(state, "startedAt", 0L);
                states.add(state); services.add(context.getBean(ProposerServiceImpl.class));
                int port = ((EmbeddedWebApplicationContext) context).getEmbeddedServletContainer().getPort();
                peers.add("ws://localhost:" + port + "/paxos");
            }
            for (ProposerServiceImpl service : services) {
                ReflectionTestUtils.setField(service, "configuredPeers", String.join(",", peers)); service.initialize();
            }
            now.set(TimeUnit.SECONDS.toNanos(30));
            // Deterministic contender; protocol tests cover competing candidates and partitions.
            services.get(0).proposer();
            await(() -> states.get(0).isLeaseOwner());
            for (int round = 0; round < 4; round++) {
                long oldBallot = states.get(0).getBallotNumber();
                // Force all cached sockets closed; next election must reconnect them.
                contexts.get(0).getBean(WebSocketServiceImpl.class).close();
                now.addAndGet(TimeUnit.SECONDS.toNanos(10));
                await(() -> states.get(0).getBallotNumber() > oldBallot && services.get(0).getCurrentProposer().isFinished());
                assertTrue(states.get(0).isLeaseOwner());
                assertFalse(states.get(1).isLeaseOwner()); assertFalse(states.get(2).isLeaseOwner());
            }
            // All candidates contend over live sockets; check the safety invariant
            // throughout retries and several more lease intervals.
            services.get(1).proposer(); services.get(2).proposer();
            for (int tick = 0; tick < 70; tick++) {
                now.addAndGet(TimeUnit.SECONDS.toNanos(1)); Thread.sleep(30);
                assertTrue("Two owners during contention", states.stream().filter(State::isLeaseOwner).count() <= 1);
            }
            for (ProposerServiceImpl service : services) service.cancle();
            assertFalse(states.get(0).isLeaseOwner());
            contexts.get(2).close(); // An unreachable member must not abort majority progress.
            services.get(1).proposer(); now.addAndGet(TimeUnit.SECONDS.toNanos(31));
            await(() -> states.get(1).isLeaseOwner()); assertFalse(states.get(0).isLeaseOwner());
        } finally {
            Collections.reverse(contexts); for (ConfigurableApplicationContext context : contexts) context.close();
            try (java.util.stream.Stream<Path> files = Files.walk(directory)) {
                for (Path p : (Iterable<Path>) files.sorted(Comparator.reverseOrder())::iterator) Files.delete(p);
            }
        }
    }
    private void await(BooleanSupplier condition) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(15);
        while (!condition.getAsBoolean() && System.nanoTime() < deadline) Thread.sleep(20);
        assertTrue("Cluster condition timed out", condition.getAsBoolean());
    }
}
