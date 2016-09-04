package com.luairan.service.paxoslease;

import com.luairan.conf.Configration;
import com.luairan.leader.context.Proposal;
import com.luairan.leader.context.Request;
import com.luairan.leader.context.Response;
import com.luairan.leader.context.State;
import com.luairan.master.ConsistentHashLockMuti;
import com.luairan.master.SingleNode;
import com.luairan.master.SlaveNode;

import java.net.InetSocketAddress;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class Proposer {
	private  static  long  peroidTime =Long.parseLong(Configration.getValueByName("/client/paxos-server", "realProposeSeconds"));
	private  SingleNode  singleNode;
	private State state;
	private final ScheduledExecutorService scheduler ;
	private  CountDownLatch proposerOne;
	private  CountDownLatch proposerTwo;
	private InetSocketAddress inetSocketAddress;
	private ScheduledFuture<?> scheduledFuture;
	private volatile Proposal proposal ;
	private ConsistentHashLockMuti  consistentHashLockMuti;
	private volatile List<SlaveNode> reciveNode;
	public Proposer(State state,ScheduledExecutorService scheduler ,CountDownLatch proposerOne,CountDownLatch proposerTwo,InetSocketAddress inetSocketAddress,SingleNode  singleNode,ConsistentHashLockMuti consistentHashLockMuti) {
		this.state = state;
		this.scheduler = scheduler;
		this.proposerOne =proposerOne;
		this.proposerTwo = proposerTwo;
		this.inetSocketAddress = inetSocketAddress;
		this.singleNode =singleNode;
		this.consistentHashLockMuti=consistentHashLockMuti;
	}

	public Request propose() {
		Request request = new Request();
		request.setType(Request.Type.PrepareRequest);
		request.setBallotNumber(state.getNextBallotNumber());
		return request;
	}

	public  void reciveResponseOne(Response response){
		if (response.getBallotNumber() != state.getBallotNumber())
			return ;
		 setProposalOne(response.getAccpetedProposal());
		if (response.getAccpetedProposal() == null||proposal.getSingleNode().getAddress().equals(singleNode.getAddress()))
			proposerOne.countDown();
	}
	public Request prepareRequestOne() {
		scheduledFuture =scheduler.schedule(new TimeOut(), peroidTime,TimeUnit.SECONDS);
		Request request = new Request();
		request.setType(Request.Type.ProposeRequest);
		request.setBallotNumber(state.getBallotNumber());
		Proposal proposal = new Proposal();
		proposal.setProposerId(state.getBallotNumber()+"");
		proposal.setSingleNode(singleNode);
		proposal.setTimeout(10000);
		request.setProposal(proposal);
		if(this.proposal== null||!singleNode.getAddress().equals(this.proposal.getSingleNode().getAddress()))
		request.setList(consistentHashLockMuti.getNode());
		return request;
	}
	
	public ScheduledFuture<?> getScheduledFuture(){
		return this.scheduledFuture;
	}


	public void reciveResponseTwo(Response response){
		if (response.getBallotNumber() != state.getBallotNumber())
			return ;
		setReciveNode(response.getList());
		proposerTwo.countDown();
	}
	public void proposeResonseTwo() {
		System.out.println(new Date()+"\t"+inetSocketAddress+"成为主节点");
		if(reciveNode!=null){
			consistentHashLockMuti.buildMap(reciveNode);
		}
		state.setLeaseOwner(true);
	}
	
	public  Proposal getProposalOne(){
		return proposal;
	}
	
	public synchronized void setProposalOne( Proposal  proposal){
		System.out.println(new Date()+"\t"+inetSocketAddress+"\t   获取"+proposal);
		this.proposal=proposal;
	}
	
	public synchronized void setReciveNode (List<SlaveNode> reciveNode){
		this.reciveNode=reciveNode;
	}
	private class TimeOut implements Runnable {

		@Override
		public void run() {
			state.setLeaseOwner(false);
			System.out.println(new Date()+"\t"+inetSocketAddress+"放弃主节点");
		}
	}
}




