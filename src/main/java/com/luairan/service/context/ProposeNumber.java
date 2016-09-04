package com.luairan.service.context;

public class ProposeNumber {
    private long tryTime;
    private String nodeKey;
    private long rebootTime;

    public static boolean little(ProposeNumber n1, ProposeNumber n2) {
        return false;
    }

    public long getTryTime() {
        return tryTime;
    }

    public void setTryTime(long tryTime) {
        this.tryTime = tryTime;
    }

    public String getNodeKey() {
        return nodeKey;
    }

    public void setNodeKey(String nodeKey) {
        this.nodeKey = nodeKey;
    }

    public long getRebootTime() {
        return rebootTime;
    }

    public void setRebootTime(long rebootTime) {
        this.rebootTime = rebootTime;
    }
}
