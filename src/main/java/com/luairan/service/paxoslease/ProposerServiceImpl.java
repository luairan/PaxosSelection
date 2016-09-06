package com.luairan.service.paxoslease;

import com.luairan.service.context.Proposal;
import com.luairan.service.context.Request;
import com.luairan.service.context.State;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;
import java.util.concurrent.*;

/**
 * Created by luairan on 16/9/5.
 */

@Service
public class ProposerServiceImpl implements ProposerService{
    private static long realProposeSeconds = Long.parseLong("30");
    private static int oneFailRetrySeconds = Integer.parseInt("6");
    private static long waitSeconds = Long.parseLong("3");
    private static long successReProposeSeconds = Long.parseLong("20");
    private static String paxosAcceptor = "/paxosacceptor/";
    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private Executor executor = Executors.newCachedThreadPool();
    private List<String> urlList;
    private State state;

    private Proposer proposer;

    private ScheduledFuture<?> scheduledFuture;
    private ScheduledFuture<?> scheduledFutureProposer;
    private Random random = new Random();

//    private SingleNode singleNode;
//    private ConsistentHashNode consistentHashNode;

//    public ProposerUtil(Executor executor, State state,
//                        ScheduledExecutorService scheduler, InetSocketAddress inetSocketAddress, ConsistentHashNode consistentHashNode) {
//        this.executor = executor;
//        this.urlList = new ArrayList<String>();
//        List<Map<String, String>> lis = Configration.getMaps("/client/paxos-server/accepter-server/url");
//        for (Map<String, String> map : lis) {
//            urlList.add(map.get("value"));
//        }
//        this.scheduler = scheduler;
//        this.state = state;
//        this.inetSocketAddress = inetSocketAddress;
//        String httpUrl = "com.luairan.service.http://" + Configration.getValue("/client/local") + ":" + inetSocketAddress.getPort();
//        lis = Configration.getMaps("/client/paxos-server/proposer-server/url");
//        String name = null;
//        for (Map<String, String> map : lis) {
//            String url = map.get("value");
//            if (url.equals(httpUrl)) name = map.get("name");
//        }
//        this.singleNode = new SingleNode(name, httpUrl, false, inetSocketAddress.getPort());
//        this.consistentHashNode = consistentHashNode;
//    }


    public void proposerOneAfter(){
        if (!onestep) {
            Proposal proposal = proposer.getProposalOne();
//            System.out.println(new Date() + "\t" + singleNode.getAddress() + "\t" + proposal);
//            if (proposal != null && !proposal.getSingleNode().getAddress().equals(singleNode.getAddress())) {
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


    public void proposerTwoBefore(){
        if (scheduledFutureProposer != null) scheduledFutureProposer.cancel(false);
        Request   request = proposer.prepareRequestOne();
        scheduledFutureProposer = proposer.getScheduledFuture();
        // 执行paxoslease 第二个阶段
        for (String url : urlList) {
            ProposerTwo proposerTwo = new ProposerTwo(url + paxosAcceptor, proposer, request);
            executor.execute(proposerTwo);
        }
    }


    public void proposerTwoAfter(){
        if (twostep) {
            scheduledFuture.cancel(false);
            scheduledFuture = scheduler.scheduleAtFixedRate(new TimePropose(), successReProposeSeconds, realProposeSeconds, TimeUnit.SECONDS);
            proposer.proposeResonseTwo();
        } else {
            scheduledFuture.cancel(false);
            scheduledFuture = scheduler.scheduleAtFixedRate(new TimePropose(), 0L, realProposeSeconds, TimeUnit.SECONDS);
        }

    }



    public void proposerOnce() throws InterruptedException {
        CountDownLatch proposeOne = new CountDownLatch(urlList.size() / 2 + 1);
        CountDownLatch proposeTwo = new CountDownLatch(urlList.size() / 2 + 1);
        Proposer proposer = new Proposer(state);
        // 执行paxoslease 第一个阶段
        Request request = proposer.propose();
        for (String url : urlList) {
            ProposerOne popser = new ProposerOne(url + paxosAcceptor, proposer, request);
            executor.execute(popser);
        }
        boolean onestep = proposeOne.await(waitSeconds, TimeUnit.SECONDS);

//        if (!onestep) {
//            Proposal proposal = proposer.getProposalOne();
////            System.out.println(new Date() + "\t" + singleNode.getAddress() + "\t" + proposal);
////            if (proposal != null && !proposal.getSingleNode().getAddress().equals(singleNode.getAddress())) {
//            if (proposal != null) {
//                scheduledFuture.cancel(false);
//                scheduledFuture = scheduler.schedule(new TimePropose(), realProposeSeconds, TimeUnit.SECONDS);
//                return;
//            }
//            scheduledFuture.cancel(false);
//            scheduledFuture = scheduler.schedule(new TimePropose(), random.nextInt(oneFailRetrySeconds), TimeUnit.SECONDS);
//            return;
//        }
//


        if (scheduledFutureProposer != null) scheduledFutureProposer.cancel(false);
        request = proposer.prepareRequestOne();
        scheduledFutureProposer = proposer.getScheduledFuture();
        // 执行paxoslease 第二个阶段
        for (String url : urlList) {
            ProposerTwo proposerTwo = new ProposerTwo(url + paxosAcceptor, proposer, request);
            executor.execute(proposerTwo);
        }
        boolean twostep = proposeTwo.await(waitSeconds, TimeUnit.SECONDS);
        if (twostep) {
            scheduledFuture.cancel(false);
            scheduledFuture = scheduler.scheduleAtFixedRate(new TimePropose(), successReProposeSeconds, realProposeSeconds, TimeUnit.SECONDS);
            proposer.proposeResonseTwo();
        } else {
            scheduledFuture.cancel(false);
            scheduledFuture = scheduler.scheduleAtFixedRate(new TimePropose(), 0L, realProposeSeconds, TimeUnit.SECONDS);
        }
//		System.out.println(new Date());
    }

    public synchronized void proposer() throws InterruptedException {
        if (scheduledFuture != null) scheduledFuture.cancel(false);
        scheduledFuture = scheduler.scheduleAtFixedRate(new TimePropose(), 0L, realProposeSeconds, TimeUnit.SECONDS);
    }

    public synchronized void cancle() {
        if (scheduledFuture != null) scheduledFuture.cancel(false);
    }

    private class TimePropose implements Runnable {
        @Override
        public void run() {
            try {
                proposerOnce();
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
}

