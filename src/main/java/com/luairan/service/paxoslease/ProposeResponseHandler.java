package com.luairan.service.paxoslease;

import com.luairan.service.util.Json;
import com.luairan.service.context.Response;
import org.springframework.stereotype.Service;
import jakarta.annotation.Resource;

@Service
public class ProposeResponseHandler {
    @Resource private org.springframework.beans.factory.ObjectProvider<ProposerServiceImpl> proposerService;
    public void handleMessage(String peer, String message) {
        if (peer == null || message == null || message.trim().isEmpty()) return;
        Response response = Json.parseObject(message, Response.class);
        if (response != null) proposerService.getObject().receive(peer, response);
    }
}
