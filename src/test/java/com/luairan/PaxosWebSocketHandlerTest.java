package com.luairan;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.luairan.control.PaxosLeaseWebSocketHandler;
import com.luairan.service.paxoslease.AcceptorRequestHandler;

import org.junit.jupiter.api.Test;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;

class PaxosWebSocketHandlerTest {
    @Test
    void forwardsTextPayloadWithoutReencoding() throws Exception {
        var acceptor = mock(AcceptorRequestHandler.class);
        var session = mock(WebSocketSession.class);
        var handler = new PaxosLeaseWebSocketHandler(acceptor);
        String payload = "{\"machineId\":\"节点一\"}";
        handler.handleTextMessage(session, new TextMessage(payload));
        verify(acceptor).handleMessage(session, payload);
        assertFalse(handler.supportsPartialMessages());
    }

    @Test
    void responseFailureDoesNotEscapeAndTransportFailureClosesOpenSession() throws Exception {
        var acceptor = mock(AcceptorRequestHandler.class);
        var session = mock(WebSocketSession.class);
        var handler = new PaxosLeaseWebSocketHandler(acceptor);
        doThrow(new IOException("connection closed"))
                .when(acceptor)
                .handleMessage(session, "request");
        assertDoesNotThrow(() -> handler.handleTextMessage(session, new TextMessage("request")));
        when(session.isOpen()).thenReturn(true);
        handler.handleTransportError(session, new IOException("transport"));
        verify(session).close();
    }
}
