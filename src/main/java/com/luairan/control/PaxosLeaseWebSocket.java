package com.luairan.control;

import com.luairan.service.paxoslease.AcceptorRequestHandler;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class PaxosLeaseWebSocket implements WebSocketConfigurer {

    private final AcceptorRequestHandler acceptorRequestHandler;

    public PaxosLeaseWebSocket(AcceptorRequestHandler acceptorRequestHandler) {
        this.acceptorRequestHandler = acceptorRequestHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(systemWebSocketHandler(), "/paxos");
    }

    @Bean
    public WebSocketHandler systemWebSocketHandler() {
        return new PaxosLeaseWebSocketHandler(acceptorRequestHandler);
    }
}
