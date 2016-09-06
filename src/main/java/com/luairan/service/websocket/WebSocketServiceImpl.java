package com.luairan.service.websocket;

import com.luairan.service.util.websocket.WebSocketClientEndpoint;
import com.luairan.service.paxoslease.*;
import org.springframework.stereotype.Service;

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


    public ProposeResponseHandler proposeResponseHandler;


    public void startUpSingle(String url) throws URISyntaxException {
        WebSocketClientEndpoint clientEndPoint = new WebSocketClientEndpoint(new URI(url));
        // add listener
        clientEndPoint.addMessageHandler(new WebSocketClientEndpoint.MessageHandler() {
            public void handleMessage(Session userSession,String message) {
//                System.out.println(message);
                proposeResponseHandler.handleMessage(userSession,message);
            }
        });

        // send message to websocket
//        clientEndPoint.sendMessage("{'event':'addChannel','channel':'ok_btccny_ticker'}");
        holdwebSocket.put(url, clientEndPoint);
    }

    public void sendMessage(String url, String message) throws URISyntaxException {
        if (!holdwebSocket.containsKey(url)) {
            startUpSingle(url);
        }
        WebSocketClientEndpoint webSocketClientEndpoint = holdwebSocket.get(url);
        webSocketClientEndpoint.sendMessage(message);
    }

}
