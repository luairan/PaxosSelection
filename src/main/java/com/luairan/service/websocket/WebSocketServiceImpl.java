package com.luairan.service.websocket;

import com.luairan.service.paxoslease.ProposeResponseHandler;
import com.luairan.service.util.websocket.WebSocketClientEndpoint;
import org.springframework.stereotype.Service;
import javax.annotation.*;
import java.net.*;
import java.util.*;

@Service
public class WebSocketServiceImpl implements WebSocketService {
    private final Map<String, WebSocketClientEndpoint> connections = new HashMap<>();
    @Resource private ProposeResponseHandler proposeResponseHandler;
    public synchronized void sendMessage(String url, String message) throws URISyntaxException {
        WebSocketClientEndpoint endpoint = connections.get(url);
        if (endpoint == null || !endpoint.isOpen()) {
            if (endpoint != null) endpoint.close();
            endpoint = new WebSocketClientEndpoint(new URI(url));
            endpoint.addMessageHandler((session, response) -> proposeResponseHandler.handleMessage(url, response));
            connections.put(url, endpoint);
        }
        try { endpoint.sendMessage(message); }
        catch (RuntimeException e) { connections.remove(url); endpoint.close(); throw e; }
    }
    @PreDestroy public synchronized void close() {
        for (WebSocketClientEndpoint endpoint : connections.values()) endpoint.close();
        connections.clear();
    }
}
