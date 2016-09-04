package com.luairan.service.master;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


/**
 * 连接节点
 *
 * @author luairan
 */
public class SlaveNode implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = -789642694542122740L;

    private List<SingleNode> nodeSet = new ArrayList<SingleNode>();

    private String hashkey;

    private SlaveNode(List<SingleNode> nodeSet, String hashkey) {
        this.nodeSet = nodeSet;
        this.hashkey = hashkey;
    }

    public static List<SlaveNode> getDistributeList(List<SingleNode> singleNodeList, int copySize) {
        int realNodeNum = singleNodeList.size();
        RandomIntNotSame randomUtil = new RandomIntNotSame(realNodeNum - 1);
        List<SlaveNode> list = new ArrayList<SlaveNode>(realNodeNum);
        for (int i = 0; i < realNodeNum; i++) {
            List<SingleNode> nodeSet = new ArrayList<SingleNode>();
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < copySize; j++) {
                SingleNode singleNode = singleNodeList.get(randomUtil.nextInt());
                sb.append(singleNode.getAddress());
                nodeSet.add(singleNode);
            }
            randomUtil.resetRadom();
            list.add(new SlaveNode(nodeSet, sb.toString()));
        }
        return list;
    }


    public List<SingleNode> getSingleNode() {
        return this.nodeSet;
    }

    public String getHashKey() {
        return this.hashkey;
    }

    @Override
    public String toString() {
        return "SlaveNode [nodeSet=" + nodeSet + ", hashkey=" + hashkey + "]";
    }


}
