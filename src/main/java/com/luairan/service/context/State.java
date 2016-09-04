package com.luairan.service.context;

import java.io.Serializable;

public class State implements Serializable {
    /**
     *
     */
    private static final long serialVersionUID = 4252840912458974311L;
    private volatile long ballotNumber;
    private long timeperoid;
    private volatile long highestPromissed;
    private volatile Proposal accpetedProposal;
    private volatile boolean leaseOwner;


    public synchronized long getNextBallotNumber() {
        ballotNumber++;
        return ballotNumber;
    }

    public long getTimeperoid() {
        return timeperoid;
    }

    public void setTimeperoid(long timeperoid) {
        this.timeperoid = timeperoid;
    }

    public long getHighestPromissed() {
        return highestPromissed;
    }

    public synchronized void setHighestPromissed(long highestPromissed) {
        this.highestPromissed = highestPromissed;
    }

    public Proposal getAccpetedProposal() {
        return accpetedProposal;
    }

    public synchronized void setAccpetedProposal(Proposal accpetedProposal) {
        this.accpetedProposal = accpetedProposal;
    }

    public boolean isLeaseOwner() {
        return leaseOwner;
    }

    public synchronized void setLeaseOwner(boolean leaseOwner) {
        this.leaseOwner = leaseOwner;
    }

    public long getBallotNumber() {
        return ballotNumber;
    }

    public synchronized void setBallotNumber(long ballotNumber) {
        this.ballotNumber = ballotNumber;
    }


}
