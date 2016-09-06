package com.luairan.service.paxoslease;

import com.alibaba.fastjson.JSON;
import com.luairan.service.context.Response;
import com.luairan.service.context.Type;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.websocket.Session;


/**
 * 客户端Proposer接收到的Acceptor的回复
 */


@Service
public class ProposeResponseHandler {


    @Resource
    private ProposerService proposerService;


    public void handleMessage(Session userSession, String message) {
        if (StringUtils.isNotBlank(message)) {
            Response response = JSON.parseObject(message, Response.class);

            //收到Acceptor的第一阶段
            if (response.getType() == Type.PrepareResponse) {

                Proposer proposer = proposerService.getCurrentProposer();
                proposer.reciveResponseOne(response);
                int number = proposer.getProposerOne();

                boolean oneStepCanGo =false;
                if(number<=0){
                    oneStepCanGo = proposer.compareAndSetOne(false,true);
                }
                if(oneStepCanGo){
                    proposerService.proposerTwoBefore();
                }else{

                }

                //收到Acceptor回复的第二阶段
            } else if (response.getType() == Type.ProposeResponse) {

                Proposer proposer = proposerService.getCurrentProposer();

                proposer.reciveResponseTwo(response);


                int number = proposer.getProposerTwo();

                boolean twoStepCanGo =false;
                if(number<=0){
                    twoStepCanGo = proposer.compareAndSetTwo(false,true);
                }
                if(twoStepCanGo){
                    proposerService.proposerTwoAfter();
                }else{

                }
            }
        }
    }
}