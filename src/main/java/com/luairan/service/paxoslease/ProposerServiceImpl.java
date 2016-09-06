package com.luairan.service.paxoslease;

import com.alibaba.fastjson.JSON;
import com.luairan.service.context.Proposal;
import com.luairan.service.context.Request;
import com.luairan.service.context.State;
import com.luairan.service.websocket.WebSocketService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Created by luairan on 16/9/5.
 */

@Service
public class ProposerServiceImpl implements ProposerService {

    private static long realProposeSeconds = Long.parseLong("30");
    private static int oneFailRetrySeconds = Integer.parseInt("6");
    private static long waitSeconds = Long.parseLong("3");
    private static long successReProposeSeconds = Long.parseLong("20");
    private static List<String> urlList;

    static {
        urlList = new ArrayList<>();
        urlList.add("ws://localhost:8080/paxos");
    }
    @Resource
    private WebSocketService webSocketService;
    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    @Resource
    private State state;

    private volatile Proposer proposer;
    private ScheduledFuture<?> scheduledFuture;
    private ScheduledFuture<?> scheduledFutureProposer;
    private Random random = new Random();

    @Override
    public Proposer getCurrentProposer() {
        return proposer;
    }


    public synchronized void setProposer(Proposer proposer) {
        this.proposer = proposer;
    }


    @Override
    public void proposerTwoBefore() {

        Request request = proposer.prepareRequestOne();
        scheduledFutureProposer = proposer.getScheduledFuture();
        String jsonInfo = JSON.toJSONString(request);
        // 执行paxoslease 第二个阶段
        for (String url : urlList) {
            try {
                webSocketService.sendMessage(url, jsonInfo);
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        }
        scheduler.schedule(new Runnable() {
            @Override
            public void run() {
                boolean twoFinished = proposer.getProposerTwoFinished();
                if (!twoFinished) {
                    scheduledFuture.cancel(false);
                    scheduledFuture = scheduler.scheduleAtFixedRate(new TimePropose(), 0L, realProposeSeconds, TimeUnit.SECONDS);
                }
            }
        }, waitSeconds, TimeUnit.SECONDS);
    }

    @Override
    public void proposerTwoAfter() {
        if (scheduledFuture != null)
            scheduledFuture.cancel(false);
        scheduledFuture = scheduler.scheduleAtFixedRate(new TimePropose(), successReProposeSeconds, realProposeSeconds, TimeUnit.SECONDS);
        proposer.proposeResonseTwo();
    }

    @Override
    public void proposerStart() throws InterruptedException {
        int useNum = urlList.size() / 2 + 1;
        Proposer proposer = new Proposer(state, useNum);
        setProposer(proposer);
        // 执行paxoslease 第一个阶段
        Request request = proposer.propose();
        String jsonInfo = JSON.toJSONString(request);
        for (String url : urlList) {
            try {
                webSocketService.sendMessage(url, jsonInfo);
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        }

        scheduler.schedule(new Runnable() {
            @Override
            public void run() {
                boolean oneFinished = proposer.getProposerOneFinished();
                if (!oneFinished) {
                    Proposal proposal = proposer.getProposalOne();
                    if (proposal != null) {
                        scheduledFuture.cancel(false);
                        scheduledFuture = scheduler.schedule(new TimePropose(), realProposeSeconds, TimeUnit.SECONDS);
                        return;
                    }
                    scheduledFuture.cancel(false);
                    scheduledFuture = scheduler.schedule(new TimePropose(), random.nextInt(oneFailRetrySeconds), TimeUnit.SECONDS);
                    return;
                }
            }
        }, waitSeconds, TimeUnit.SECONDS);
    }


    @Override
    public synchronized void proposer() throws InterruptedException {
        if (scheduledFuture != null) scheduledFuture.cancel(false);
        scheduledFuture = scheduler.scheduleAtFixedRate(new TimePropose(), 0L, realProposeSeconds, TimeUnit.SECONDS);
    }


    @Override
    public synchronized void cancle() {
        if (scheduledFuture != null) scheduledFuture.cancel(false);
    }

    private class TimePropose implements Runnable {
        @Override
        public void run() {
            try {
                proposerStart();
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
}

