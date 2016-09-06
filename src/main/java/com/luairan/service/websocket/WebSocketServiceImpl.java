package com.luairan.service.websocket;

import com.luairan.service.paxoslease.ProposeResponseHandler;
import com.luairan.service.util.websocket.WebSocketClientEndpoint;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.websocket.Session;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by luairan on 16/9/5.
 */


@Service
public class WebSocketServiceImpl implements WebSocketService {


    private Map<String, WebSocketClientEndpoint> holdwebSocket = new TreeMap();


    @Resource
    private ProposeResponseHandler proposeResponseHandler;


    public void startUpSingle(String url) throws URISyntaxException {
        WebSocketClientEndpoint clientEndPoint = new WebSocketClientEndpoint(new URI(url));
        // add listener
        clientEndPoint.addMessageHandler(new WebSocketClientEndpoint.MessageHandler() {
            public void handleMessage(Session userSession, String message) {
                System.out.println("proposer recive:\t"+message);
                proposeResponseHandler.handleMessage(userSession, message);
            }
        });

        // send message to websocket
//        clientEndPoint.sendMessage("{'event':'addChannel','channel':'ok_btccny_ticker'}");
        holdwebSocket.put(url, clientEndPoint);
    }


    @Override
    public void sendMessage(String url, String message) throws URISyntaxException {
        if (!holdwebSocket.containsKey(url)) {
            startUpSingle(url);
        }
        WebSocketClientEndpoint webSocketClientEndpoint = holdwebSocket.get(url);
        webSocketClientEndpoint.sendMessage(message);
    }

}
