package com.luairan.service.context;


public class Request {
    private long ballotNumber;
    private Type type;
    private Proposal proposal;

    public long getBallotNumber() {
        return ballotNumber;
    }

    public void setBallotNumber(long ballotNumber) {
        this.ballotNumber = ballotNumber;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public Proposal getProposal() {
        return proposal;
    }

    public void setProposal(Proposal proposal) {
        this.proposal = proposal;
    }

}
