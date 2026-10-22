package com.luairan.service.paxoslease;

import com.alibaba.fastjson.JSON;
import com.luairan.service.context.Response;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;

@Service
public class ProposeResponseHandler {
    @Resource private ProposerServiceImpl proposerService;
    public void handleMessage(String peer, String message) {
        if (peer == null || message == null || message.trim().isEmpty()) return;
        Response response = JSON.parseObject(message, Response.class);
        if (response != null) proposerService.receive(peer, response);
    }
}
