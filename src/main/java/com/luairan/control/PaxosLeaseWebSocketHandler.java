package com.luairan.control;

import com.luairan.service.paxoslease.AcceptorRequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;


public class PaxosLeaseWebSocketHandler extends TextWebSocketHandler {


    private static final Map<String, WebSocketSession> users = new TreeMap<>();
    private AcceptorRequestHandler acceptorRequestHandler;
    private Logger log = LoggerFactory.getLogger(PaxosLeaseWebSocketHandler.class);

    public PaxosLeaseWebSocketHandler(AcceptorRequestHandler acceptorRequestHandler) {
        this.acceptorRequestHandler = acceptorRequestHandler;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        System.out.println("ConnectionEstablished");
        log.debug("ConnectionEstablished");
        users.put(session.getId(), session);
//        session.sendMessage(new TextMessage("connect"));
//        session.sendMessage(new TextMessage("new_msg"));
    }

//    @Override
//    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
//        System.out.println("handleMessage" + message.toString());
//        log.debug("handleMessage" + message.toString());
//        acceptorRequestHandler.handleMessage(session,message.);
////        sendMessageToUsers(new TextMessage(new Date() + "1"));
//        session.sendMessage(new TextMessage(new Date() + ""));
//    }


    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) {
        System.out.println("paxos acceptor recive :\t" + new String(message.asBytes()));
        try {
            acceptorRequestHandler.handleMessage(session, new String(message.asBytes()));
        } catch (IOException e) {
            e.printStackTrace();
        }
//        sendMessageToUsers(new TextMessage(new Date() + "1"));
//        session.sendMessage(new TextMessage(new Date() + ""));
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        if (session.isOpen()) {
            session.close();
        }
        users.remove(session.getId());
        log.debug("handleTransportError" + exception.getMessage());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        users.remove(session.getId());
        log.debug("afterConnectionClosed" + closeStatus.getReason());

    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    /**
     * 给所有在线用户发送消息
     *
     * @param message
     */
    public void sendMessageToUsers(TextMessage message) {
        for (WebSocketSession user : users.values()) {
            try {
                if (user.isOpen()) {
                    user.sendMessage(message);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}