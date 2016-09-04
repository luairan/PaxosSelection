package com.luairan.service.context;


import java.io.Serializable;

public class Proposal implements Serializable {

    private static final long serialVersionUID = -3534689343324695561L;
    private String proposerId = null;
    private long timeout = 0L;

    private SingleNode singleNode;

    public String getProposerId() {
        return proposerId;
    }

    public void setProposerId(String proposerId) {
        this.proposerId = proposerId;
    }

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    public SingleNode getSingleNode() {
        return singleNode;
    }

    public void setSingleNode(SingleNode singleNode) {
        this.singleNode = singleNode;
    }

    @Override
    public String toString() {
        return "Proposal [proposerId=" + proposerId + ", timeout=" + timeout
                + ", singleNode=" + singleNode + "]";
    }
}
