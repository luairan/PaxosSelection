package com.luairan.service.paxoslease;

import com.luairan.service.util.Json;
import com.luairan.service.context.Request;
import com.luairan.service.context.Response;
import com.luairan.service.context.Type;

import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import jakarta.annotation.Resource;
import java.io.IOException;


/**
 * 服务端Acceptor处理Proposer请求的流程
 */
@Service("acceptorRequestHandler")
public class AcceptorRequestHandler {

    @Resource
    private AcceptorService acceptorService;
    public void handleMessage(WebSocketSession session, String message) throws IOException {
        if (message != null && !message.isBlank()) {
            Request request = Json.parseObject(message, Request.class);
            //一阶段
            if (request.getType() == Type.PrepareRequest) {
                Response response = acceptorService.prepareRequest(request);
                System.out.println("acceptor send PrepareResponse :\t"+Json.toJSONString(response));
                session.sendMessage(new TextMessage(Json.toJSONString(response)));
                //二阶段
            } else if (request.getType() == Type.ProposeRequest) {
                Response response = acceptorService.proposeRequest(request);
                System.out.println("acceptor send ProposeResponse :\t"+Json.toJSONString(response));
                session.sendMessage(new TextMessage(Json.toJSONString(response)));
            }
        }
    }


}