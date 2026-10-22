package com.luairan.service.util.websocket;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.TimeUnit;
import jakarta.websocket.*;

/** Bounded connection/send operations. A failed endpoint is discarded by its owner. */
public class WebSocketClientEndpoint extends Endpoint {
    private volatile Session userSession;
    private volatile MessageHandler messageHandler;
    public WebSocketClientEndpoint(URI endpointURI) { this(endpointURI, null); }
    public WebSocketClientEndpoint(URI endpointURI, String authorization) {
        try {
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            container.setAsyncSendTimeout(3000);
            ClientEndpointConfig config = ClientEndpointConfig.Builder.create().configurator(new ClientEndpointConfig.Configurator() {
                @Override public void beforeRequest(java.util.Map<String, java.util.List<String>> headers) {
                    if (authorization != null) headers.put("Authorization", java.util.List.of(authorization));
                }
            }).build();
            config.getUserProperties().put("org.apache.tomcat.websocket.IO_TIMEOUT_MS", "3000");
            container.connectToServer(this, config, endpointURI);
        } catch (Exception e) { throw new IllegalStateException("WebSocket connect failed"); }
    }
    @Override public void onOpen(Session session, EndpointConfig config) {
        userSession = session;
        session.addMessageHandler(new jakarta.websocket.MessageHandler.Whole<String>() {
            @Override public void onMessage(String message) {
                MessageHandler handler = messageHandler;
                if (handler != null) handler.handleMessage(session, message);
            }
        });
    }
    @Override public void onClose(Session session, CloseReason reason) { userSession = null; }
    @Override public void onError(Session session, Throwable failure) { close(); }
    public void addMessageHandler(MessageHandler handler) { messageHandler = handler; }
    public void sendMessage(String message) {
        Session session = userSession;
        if (session == null || !session.isOpen()) throw new IllegalStateException("WebSocket disconnected");
        try { session.getAsyncRemote().sendText(message).get(3, TimeUnit.SECONDS); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); close(); throw new IllegalStateException("Send interrupted", e); }
        catch (Exception e) { close(); throw new IllegalStateException("WebSocket send failed", e); }
    }
    public boolean isOpen() { Session s = userSession; return s != null && s.isOpen(); }
    public void close() {
        Session s = userSession; userSession = null;
        if (s != null) try { s.close(); } catch (IOException ignored) { }
    }
    public interface MessageHandler { void handleMessage(Session session, String message); }
}
