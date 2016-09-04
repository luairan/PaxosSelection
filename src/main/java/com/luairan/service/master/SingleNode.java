//package com.luairan.service.master;
//
//import java.io.Serializable;
//
//
///**
// * 单一实体节点
// *
// * @author luairan
// */
//public class SingleNode implements Serializable {
//    /**
//     *
//     */
//    private static final long serialVersionUID = 8706059346620589344L;
//    private String name;
//    private String address;
//    private boolean failed;
//    private int port;
//
//    public SingleNode(String name, String address, boolean failed, int port) {
//        this.name = name;
//        this.address = address;
//        this.failed = failed;
//        this.port = port;
//    }
//
//    public String getName() {
//        return name;
//    }
//
//    public void setName(String name) {
//        this.name = name;
//    }
//
//    public String getAddress() {
//        return address;
//    }
//
//    public void setAddress(String address) {
//        this.address = address;
//    }
//
//    public boolean isFailed() {
//        return failed;
//    }
//
//    public void setFailed(boolean failed) {
//        this.failed = failed;
//    }
//
//    public int getPort() {
//        return port;
//    }
//
//    public void setPort(int port) {
//        this.port = port;
//    }
//
//    @Override
//    public String toString() {
//        return "SingleNode [name=" + name + ", address=" + address
//                + ", failed=" + failed + ", port=" + port + "]";
//    }
//
//}
