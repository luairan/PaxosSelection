package com.luairan.service.paxoslease;

import com.luairan.service.context.Response;
import com.luairan.service.util.Json;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Service
public class ProposeResponseHandler {

    // Resolve lazily: the proposer sends through a transport that routes responses here.
    private final ObjectProvider<ProposerServiceImpl> proposerService;

    public ProposeResponseHandler(ObjectProvider<ProposerServiceImpl> proposerService) {
        this.proposerService = proposerService;
    }

    public void handleMessage(String peer, String message) {
        if (peer == null || message == null || message.trim().isEmpty()) return;
        Response response = Json.parseObject(message, Response.class);
        if (response != null) proposerService.getObject().receive(peer, response);
    }
}
