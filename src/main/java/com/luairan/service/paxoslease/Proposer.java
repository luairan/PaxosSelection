package com.luairan.service.paxoslease;


import com.alibaba.fastjson.JSON;
import com.luairan.service.context.*;

import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class Proposer {
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    //放弃成为主节点的时间间隔
    private long peroidTime = Long.parseLong("30");
    private State state;
    private ScheduledFuture<?> scheduledFuture;
    private volatile Proposal proposal;
    private final AtomicInteger proposerOne;
    private final AtomicInteger proposerTwo ;
    private final AtomicBoolean proposerOneFinished;
    private final AtomicBoolean proposerTwoFinished;
    public Proposer(State state,int proposerNum) {
        this.state = state;
        this.proposerOne = new AtomicInteger(proposerNum);
        this.proposerTwo = new AtomicInteger(proposerNum);
        proposerOneFinished = new AtomicBoolean(false);
        proposerTwoFinished = new AtomicBoolean(false);
    }

    /**
     * 一个新提案的提出
     *
     * @return
     */
    public Request propose() {
        Request request = new Request();
        request.setType(Type.PrepareRequest);
        request.setBallotNumber(state.nextBallotNumber());
        return request;
    }

    /**
     * 一阶段提交后 从Acceptor 获取的最大编号已经被确认的正式议案
     * @param response
     * @return
     */
    public void reciveResponseOne(Response response) {
        if (response.getBallotNumber() != state.getBallotNumber())
            return ;
        setProposalOne(response.getAccpetedProposal());
//        if (response.getAccpetedProposal() == null || proposal.getSingleNode().getAddress().equals(singleNode.getAddress()))
        if (response.getAccpetedProposal() == null)
            proposerOne.decrementAndGet();
    }


    /**
     * 准备二阶段 所使用的提案
     * @return
     */
    public Request prepareRequestOne() {
        if (scheduledFuture != null) scheduledFuture.cancel(false);
        scheduledFuture = scheduler.schedule(new TimeOut(), peroidTime, TimeUnit.SECONDS);
        Request request = new Request();
        request.setType(Type.ProposeRequest);
        request.setBallotNumber(state.getBallotNumber());
        Proposal proposal = new Proposal();
        proposal.setProposerId(state.getBallotNumber() + "");
        proposal.setTimeout(10000);
        request.setProposal(proposal);
        return request;
    }

    public ScheduledFuture<?> getScheduledFuture() {
        return this.scheduledFuture;
    }

    public void reciveResponseTwo(Response response) {
        if (response.getBallotNumber() != state.getBallotNumber())
            return;
        proposerTwo.decrementAndGet();
    }

    public void proposeResonseTwo() {

        System.out.println(new Date() + "\t" + "成为主节点");
        state.setLeaseOwner(true);
    }

    public Proposal getProposalOne() {
        return proposal;
    }

    public synchronized void setProposalOne(Proposal proposal) {
        System.out.println(new Date() + "\t" + "\t   一阶段获取" + JSON.toJSONString(proposal));
        this.proposal = proposal;
    }

    private class TimeOut implements Runnable {
        @Override
        public void run() {
            state.setLeaseOwner(false);
            System.out.println(new Date() + "\t" + "放弃主节点");
        }
    }


    public int getProposerOne() {
        return proposerOne.get();
    }

    public int getProposerTwo() {
        return proposerTwo.get();
    }

    public boolean getProposerOneFinished() {
        return proposerOneFinished.get();
    }

    public boolean  getProposerTwoFinished() {
        return proposerTwoFinished.get();
    }

    public boolean compareAndSetOne(boolean oldValue,boolean newValue){
       return proposerOneFinished.compareAndSet(oldValue,newValue);
    }


    public boolean compareAndSetTwo(boolean oldValue,boolean newValue){
        return proposerTwoFinished.compareAndSet(oldValue,newValue);
    }
}




