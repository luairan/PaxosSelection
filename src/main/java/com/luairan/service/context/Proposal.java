package com.luairan.service.context;


import java.io.Serializable;

public class Proposal implements Serializable {

    private static final long serialVersionUID = -3534689343324695561L;
    private String proposerId;
    private String machineId;
    private long timeout;

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

    public String getMachineId() {
        return machineId;
    }

    public void setMachineId(String machineId) {
        this.machineId = machineId;
    }
}
