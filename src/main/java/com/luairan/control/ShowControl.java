package com.luairan.control;


import com.alibaba.fastjson.JSON;
import com.luairan.service.context.State;
import com.luairan.service.paxoslease.ProposerService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@RequestMapping("/show")
public class ShowControl {
    @Resource
    private State state;

    @Resource
    private ProposerService proposerService;

    @RequestMapping(value = "/showMater", method = RequestMethod.GET)
    public String getNewForm() {
        return JSON.toJSONString(state);
    }


    @RequestMapping(value = "/start", method = RequestMethod.GET)
    public String start() throws InterruptedException {
        proposerService.proposerStart();
        return "ok";
    }


}
