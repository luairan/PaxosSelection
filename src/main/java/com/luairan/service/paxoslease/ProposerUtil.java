//package com.luairan.service.paxoslease;
//
//import com.luairan.service.context.State;
//
//import java.io.*;
//import java.net.InetSocketAddress;
//import java.util.*;
//import java.util.concurrent.*;
//
//public class ProposerUtil {
//    private static long realProposeSeconds = Long.parseLong("30");
//    private static int oneFailRetrySeconds = Integer.parseInt("6");
//    private static long waitSeconds = Long.parseLong("3");
//    private static long successReProposeSeconds = Long.parseLong("20");
//    private static String paxosAcceptor = Configration.getValue("/client/paxos-server/com.luairan.service.http-com.luairan.service.context/acceptor");
//    private final ScheduledExecutorService scheduler;
//    private Executor executor = null;
//    private List<String> urlList = null;
//    private State state;
//    private ScheduledFuture<?> scheduledFuture;
//    private ScheduledFuture<?> scheduledFutureProposer;
//    private InetSocketAddress inetSocketAddress;
//    private Random random = new Random();
//    private SingleNode singleNode;
//    private ConsistentHashNode consistentHashNode;
//
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
//
//
//    public void proposerOnce() throws InterruptedException {
//        CountDownLatch proposeOne = new CountDownLatch(urlList.size() / 2 + 1);
//        CountDownLatch proposeTwo = new CountDownLatch(urlList.size() / 2 + 1);
//        Proposer proposer = new Proposer(state, scheduler, proposeOne, proposeTwo, inetSocketAddress, singleNode, consistentHashNode.getConsistentHashLock());
//        // 执行paxoslease 第一个阶段
//        Request request = proposer.propose();
//        for (String url : urlList) {
//            ProposerOne popser = new ProposerOne(url + paxosAcceptor, proposer, request);
//            executor.execute(popser);
//        }
//        boolean onestep = proposeOne.await(waitSeconds, TimeUnit.SECONDS);
//        if (!onestep) {
//            Proposal proposal = proposer.getProposalOne();
//            System.out.println(new Date() + "\t" + singleNode.getAddress() + "\t" + proposal);
//            if (proposal != null && !proposal.getSingleNode().getAddress().equals(singleNode.getAddress())) {
//                scheduledFuture.cancel(false);
//                scheduledFuture = scheduler.schedule(new TimePropose(), realProposeSeconds, TimeUnit.SECONDS);
//                return;
//            }
//            scheduledFuture.cancel(false);
//            scheduledFuture = scheduler.schedule(new TimePropose(), random.nextInt(oneFailRetrySeconds), TimeUnit.SECONDS);
//            return;
//        }
//        if (scheduledFutureProposer != null) scheduledFutureProposer.cancel(false);
//        request = proposer.prepareRequestOne();
//        scheduledFutureProposer = proposer.getScheduledFuture();
//        // 执行paxoslease 第二个阶段
//        for (String url : urlList) {
//            ProposerTwo proposerTwo = new ProposerTwo(url + paxosAcceptor, proposer, request);
//            executor.execute(proposerTwo);
//        }
//        boolean twostep = proposeTwo.await(waitSeconds, TimeUnit.SECONDS);
//        if (twostep) {
//            scheduledFuture.cancel(false);
//            scheduledFuture = scheduler.scheduleAtFixedRate(new TimePropose(), successReProposeSeconds, realProposeSeconds, TimeUnit.SECONDS);
//            proposer.proposeResonseTwo();
//        } else {
//            scheduledFuture.cancel(false);
//            scheduledFuture = scheduler.scheduleAtFixedRate(new TimePropose(), 0L, realProposeSeconds, TimeUnit.SECONDS);
//        }
////		System.out.println(new Date());
//    }
//
//    public synchronized void proposer() throws InterruptedException {
//        if (scheduledFuture != null) scheduledFuture.cancel(false);
//        scheduledFuture = scheduler.scheduleAtFixedRate(new TimePropose(), 0L, realProposeSeconds, TimeUnit.SECONDS);
//    }
//
//    public synchronized void cancle() {
//        if (scheduledFuture != null) scheduledFuture.cancel(false);
//    }
//
//    private class TimePropose implements Runnable {
//
//        @Override
//        public void run() {
//            try {
//                proposerOnce();
//            } catch (InterruptedException e) {
//                // TODO Auto-generated catch block
//                e.printStackTrace();
//            }
//        }
//    }
//}
//
//class ProposerOne implements Runnable {
//    private String url;
//    private Proposer proposer;
//    private Request request;
//
//    public ProposerOne(String url, Proposer proposer, Request request) {
//        this.url = url;
//        this.proposer = proposer;
//        this.request = request;
//    }
//
//    @Override
//    public void run() {
//        Response response = (Response) HTTPClient.sendPost(url,
//                new ProposeAction(), request);
//        if (response.getType() == Response.Type.Closed)
//            return;
//        proposer.reciveResponseOne(response);
//    }
//
//}
//
//class ProposerTwo implements Runnable {
//    private String url;
//    private Proposer proposer;
//    private Request request;
//
//    public ProposerTwo(String url, Proposer proposer, Request request) {
//        this.url = url;
//        this.proposer = proposer;
//        this.request = request;
//    }
//
//    @Override
//    public void run() {
//        Response response = (Response) HTTPClient.sendPost(url,
//                new ProposeAction(), request);
//        if (response.getType() == Response.Type.Closed)
//            return;
//        proposer.reciveResponseTwo(response);
//    }
//
//}
//
//
//class ProposeAction implements Action<Response> {
//
//
//    @Override
//    public void sendInfo(OutputStream os, Object... args) throws Exception {
//        ObjectOutputStream oos = new ObjectOutputStream(
//                new BufferedOutputStream(os));
//        oos.writeObject(args[0]);
//        oos.flush();
//        oos.close();
//    }
//
//    @Override
//    public Response reciveInfo(InputStream is) throws Exception {
//        ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(
//                is));
//        return (Response) ois.readObject();
//
//    }
//}