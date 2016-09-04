package com.luairan.service.paxoslease;

import com.luairan.conf.Configration;
import com.luairan.leader.context.Request;
import com.luairan.leader.context.Response;
import com.luairan.leader.context.Response.Type;
import com.luairan.leader.context.State;
import com.luairan.master.SlaveNode;

import java.net.InetSocketAddress;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class Acceptor {

    private static long peroidTime = Long.parseLong(Configration.getValueByName("/client/paxos-server", "realProposeSeconds"));
    private final ScheduledExecutorService scheduler;
    private State state;
    private InetSocketAddress inetSocketAddress;

    private volatile List<SlaveNode> list;

    private ScheduledFuture<?> scheduledFuture;

    public Acceptor(State state, ScheduledExecutorService scheduler, InetSocketAddress inetSocketAddress) {
        this.state = state;
        this.scheduler = scheduler;
        this.inetSocketAddress = inetSocketAddress;
    }

    private synchronized void setList(List<SlaveNode> list) {
        this.list = list;
    }


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

    public Response proposeReuqest(Request request) {
        Response response = new Response();
        if (request.getBallotNumber() < state.getHighestPromissed()) {
            response.setType(Type.Closed);
            return response;
        }
        state.setAccpetedProposal(request.getProposal());
        if (list == null) {
            setList(request.getList());
        } else {
            response.setList(list);
        }
        if (scheduledFuture != null) scheduledFuture.cancel(false);
        scheduledFuture = scheduler.schedule(new TimeOut(), peroidTime, TimeUnit.SECONDS);
        response.setType(Type.ProposeResponse);
        response.setBallotNumber(request.getBallotNumber());
        return response;

    }

    private class TimeOut implements Runnable {

        @Override
        public void run() {
            state.setAccpetedProposal(null);
            System.out.println(new Date() + "\t" + inetSocketAddress + "acceptor - set - null");
        }
    }

}
