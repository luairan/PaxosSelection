package com.luairan.control;

import com.luairan.service.paxoslease.AcceptorRequestHandler;
import com.luairan.service.paxoslease.AcceptorService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import jakarta.annotation.Resource;

@Configuration
@EnableWebSocket
public class PaxosLeaseWebSocket implements WebSocketConfigurer {


    @Resource
    private AcceptorRequestHandler acceptorRequestHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(systemWebSocketHandler(), "/paxos");
    }

    @Bean
    public WebSocketHandler systemWebSocketHandler() {
        return new PaxosLeaseWebSocketHandler(acceptorRequestHandler);
    }

}