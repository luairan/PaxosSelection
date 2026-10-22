package com.luairan.service.websocket;

import com.luairan.service.paxoslease.ProposeResponseHandler;
import com.luairan.service.util.websocket.WebSocketClientEndpoint;
import org.springframework.stereotype.Service;
import jakarta.annotation.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class WebSocketServiceImpl implements WebSocketService {
    private final Map<String, WebSocketClientEndpoint> connections = new ConcurrentHashMap<>();
    private final Map<String, Object> peerLocks = new ConcurrentHashMap<>();
    @Resource private com.luairan.security.SecurityTokens tokens;
    @Resource private ProposeResponseHandler proposeResponseHandler;
    public void sendMessage(String url, String message) throws URISyntaxException {
        synchronized (peerLocks.computeIfAbsent(url, key -> new Object())) {
            WebSocketClientEndpoint endpoint = connections.get(url);
            if (endpoint == null || !endpoint.isOpen()) {
                if (endpoint != null) endpoint.close();
                endpoint = new WebSocketClientEndpoint(new URI(url), tokens.nodeAuthorization());
                endpoint.addMessageHandler((session, response) -> proposeResponseHandler.handleMessage(url, response));
                connections.put(url, endpoint);
            }
            try { endpoint.sendMessage(message); }
            catch (RuntimeException e) { connections.remove(url, endpoint); endpoint.close(); throw e; }
        }
    }
    @PreDestroy public void close() {
        for (String peer : peerLocks.keySet()) {
            synchronized (peerLocks.get(peer)) {
                WebSocketClientEndpoint endpoint = connections.remove(peer);
                if (endpoint != null) endpoint.close();
            }
        }
    }
}
