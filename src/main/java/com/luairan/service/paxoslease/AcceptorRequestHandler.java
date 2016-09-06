package com.luairan.service.paxoslease;

import com.alibaba.fastjson.JSON;
import com.luairan.service.context.Request;
import com.luairan.service.context.Response;
import com.luairan.service.context.Type;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import javax.annotation.Resource;
import java.io.IOException;


/**
 * 服务端Acceptor处理Proposer请求的流程
 */
@Service("acceptorRequestHandler")
public class AcceptorRequestHandler {

    @Resource
    private AcceptorService acceptorService;
    public void handleMessage(WebSocketSession session, String message) throws IOException {
        if (StringUtils.isNotBlank(message)) {
            Request request = JSON.parseObject(message, Request.class);
            //一阶段
            if (request.getType() == Type.PrepareRequest) {
                Response response = acceptorService.prepareRequest(request);
                session.sendMessage(new TextMessage(JSON.toJSONString(response)));
                //二阶段
            } else if (request.getType() == Type.ProposeRequest) {
                Response response = acceptorService.proposeRequest(request);
                session.sendMessage(new TextMessage(JSON.toJSONString(response)));
            }
        }
    }


}