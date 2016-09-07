package com.luairan.service.context;


public class Proposal {
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
