package com.luairan.service.paxoslease;

import com.luairan.service.context.Request;
import com.luairan.service.context.Response;
import com.luairan.service.context.State;
import com.luairan.service.context.Type;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Created by luairan on 16/9/5.
 */

@Service
public class AcceptorServiceImpl implements AcceptorService {

    private static long peroidTime = Long.parseLong("30");

    @Resource
    private State state;

    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private ScheduledFuture<?> scheduledFuture;

    @Override
    public Response prepareRequest(Request request) {
        Response response = new Response();
        if (request.getBallotNumber() <= state.getHighestPromissed()) {
            response.setType(Type.Closed);
            return response;
        }
        state.setHighestPromissed(request.getBallotNumber());
        response.setType(Type.PrepareResponse);
        response.setBallotNumber(request.getBallotNumber());
        response.setAccpetedProposal(state.getAccpetedProposal());
        return response;
    }

    @Override
    public Response proposeRequest(Request request) {
        Response response = new Response();
        if (request.getBallotNumber() < state.getHighestPromissed()) {
            response.setType(Type.Closed);
            return response;
        }
        state.setAccpetedProposal(request.getProposal());
        if (scheduledFuture != null) scheduledFuture.cancel(false);
        scheduledFuture = scheduler.schedule(new TimeOut(), peroidTime, TimeUnit.SECONDS);
        response.setType(Type.ProposeResponse);
        response.setBallotNumber(request.getBallotNumber());
        response.setAccpetedProposal(request.getProposal());
        return response;
    }


    private class TimeOut implements Runnable {
        @Override
        public void run() {
            state.setAccpetedProposal(null);
            System.out.println(new Date() + "\t" + "acceptor - 释放锁");
        }
    }
}
