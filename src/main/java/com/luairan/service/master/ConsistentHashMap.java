package com.luairan.service.master;


import com.luairan.service.util.encode.MD5Util;

import java.util.List;
import java.util.TreeMap;

/**
 * 多节点的一致性hash算法
 *
 * @author luairan
 */
public class ConsistentHashMap<T> {
    private final static int VISUAL_MIX = Integer.parseInt("160");
    public int size;
    public boolean visualMixMode;
    private transient volatile TreeMap<Long, T> map = new TreeMap<Long, T>();
    private volatile List<T> node;

    public ConsistentHashMap(List<T> node) {
        map = new TreeMap<Long, T>();
        visualMixMode = true;
        this.node = node;
    }

    public ConsistentHashMap(boolean visualMixMode, List<T> node) {
        map = new TreeMap<Long, T>();
        this.visualMixMode = visualMixMode;
        this.node = node;
    }

    public List<T> getNode() {
        return this.node;
    }

    public synchronized void setNode(List<T> node) {
        this.node = node;
    }

    public void buildMap() {
        buildMap(node);
    }

    public void buildMap(List<T> slaveNodeList) {
        TreeMap<Long, T> map = new TreeMap<Long, T>();
        int nodeSize = 0;
        for (T node : slaveNodeList) {
            if (visualMixMode) {
                for (int i = 0; i < VISUAL_MIX; i++) {
                    long hash = hash(node.hashCode() + "-" + i);
                    map.put(hash, node);
                }
                nodeSize += VISUAL_MIX;
            } else {
                long hash = hash(node.hashCode() + "");
                map.put(hash, node);
                nodeSize++;
            }
        }
        this.map = map;
        this.size = nodeSize;
        setNode(node);
    }

    public T getNode(String key) {
        Long hash = hash(key);
        T slaveNode = getSlaveNodeByKey(hash);
//		int retrySize = 0;
//		while (this.retry && slaveNode.failed && retrySize++ < this.size) {
//			hash = hashAlgorithm.hash(retrySize + key);
//			slaveNode = getSlaveNodeByKey(hash);
//		}
        return slaveNode;
    }

    private T getSlaveNodeByKey(Long key) {
        Long ke = key;
        if (!map.containsKey(key)) {
            ke = map.ceilingKey(key);
            if (ke == null) {
                ke = map.firstKey();
            }
        }
        T slaveNode = map.get(ke);
        return slaveNode;
    }

    public TreeMap<Long, T> getMap() {
        return map;
    }


    public long hash(String str) {
        byte[] arry = MD5Util.MD5(str);
        return byteToLong(arry);
    }

    public long byteToLong(byte[] bytes) {
        long num1 = bytes[0];
        long num2 = bytes[8];

        for (int i = 1; i < 8; i++) {
            num1 = num1 << 8 | bytes[i];
        }
        for (int i = 9; i < 16; i++) {
            num2 = num2 << 8 | bytes[i];
        }
        return num1 ^ num2;
    }
}
