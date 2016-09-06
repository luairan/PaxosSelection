package com.luairan.service.context;


/**
 *
 */
public class Response {
    private long ballotNumber;
    private Type type;
    private Proposal accpetedProposal;
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

    public Proposal getAccpetedProposal() {
        return accpetedProposal;
    }

    public void setAccpetedProposal(Proposal accpetedProposal) {
        this.accpetedProposal = accpetedProposal;
    }
}
