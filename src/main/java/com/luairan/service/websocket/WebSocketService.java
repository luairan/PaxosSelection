package com.luairan.service.websocket;

import java.net.URISyntaxException;

/**
 * Created by luairan on 16/9/5.
 */
public interface WebSocketService {


    void sendMessage(String url, String message) throws URISyntaxException;
}
