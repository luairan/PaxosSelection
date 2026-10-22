package com.luairan.service.paxoslease;

import com.luairan.service.context.Request;
import com.luairan.service.context.Response;
import com.luairan.service.context.Type;
import com.luairan.service.util.Json;

import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;

/** 服务端Acceptor处理Proposer请求的流程 */
@Service("acceptorRequestHandler")
public class AcceptorRequestHandler {

    private final AcceptorService acceptorService;

    public AcceptorRequestHandler(AcceptorService acceptorService) {
        this.acceptorService = acceptorService;
    }

    public void handleMessage(WebSocketSession session, String message) throws IOException {
        if (message == null || message.isBlank()) {
            return;
        }
        Request request = Json.parseObject(message, Request.class);
        Response response;
        if (request.getType() == Type.PrepareRequest) {
            response = acceptorService.prepareRequest(request);
        } else if (request.getType() == Type.ProposeRequest) {
            response = acceptorService.proposeRequest(request);
        } else {
            return;
        }
        session.sendMessage(new TextMessage(Json.toJSONString(response)));
    }
}
