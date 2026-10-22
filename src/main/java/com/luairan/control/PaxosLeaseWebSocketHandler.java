package com.luairan.control;

import com.luairan.service.paxoslease.AcceptorRequestHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;

/** Adapts incoming WebSocket payloads to the acceptor protocol handler. */
public class PaxosLeaseWebSocketHandler extends TextWebSocketHandler {
    private static final Logger log = LoggerFactory.getLogger(PaxosLeaseWebSocketHandler.class);
    private final AcceptorRequestHandler acceptorRequestHandler;

    public PaxosLeaseWebSocketHandler(AcceptorRequestHandler acceptorRequestHandler) {
        this.acceptorRequestHandler = acceptorRequestHandler;
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            acceptorRequestHandler.handleMessage(session, message.getPayload());
        } catch (IOException e) {
            log.warn("Cannot send acceptor response for session {}", session.getId(), e);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception)
            throws IOException {
        log.debug("Transport failure for session {}", session.getId(), exception);
        if (session.isOpen()) {
            session.close();
        }
    }
}
