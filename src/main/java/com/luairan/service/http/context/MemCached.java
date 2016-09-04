//package com.luairan.service.http.context;
//
//import com.luairan.client.http.context.action.GetFromMemcache;
//import com.luairan.client.http.context.action.GetNodeFromConsistentHash;
//import com.luairan.client.http.context.action.PutToMemcache;
//import com.luairan.client.http.context.action.ShowMaster;
//import com.luairan.conf.Configration;
//import com.luairan.master.SingleNode;
//import com.luairan.master.SlaveNode;
//import com.luairan.node.ContextInfo;
//import com.luairan.util.RandomIntNotSame;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Map;
//
//public class MemCached {
//
//	private static List<String> accpterUrl ;
//
//	private static RandomIntNotSame randomUtil ;
//
//	static{
//		accpterUrl = new ArrayList<String>();
//		List<Map<String,String>> list = Configration.getMaps("/client/paxos-server/accepter-server/url");
//		randomUtil =new RandomIntNotSame(list.size()-1);
//		for(Map<String,String> map:list){
//			accpterUrl.add(map.get("value"));
//		}
//	}
//
//	public  static ContextInfo getValue(List<String> httpList ,String keys){
//		return GetFromMemcache.get(httpList,keys);
//	}
//	public static  <T> void putValue(List<String> httpList,String keys ,T obj){
//		PutToMemcache.twoCommitParseSet(httpList,keys, obj);
//	}
//
//	public static ContextInfo getValue(SlaveNode slaveNode,String keys){
//		return GetFromMemcache.get(slaveNode,keys);
//	}
//	public static  <T> void putValue(SlaveNode slaveNode,String keys ,T obj){
//		PutToMemcache.twoCommitParseSet(slaveNode,keys, obj);
//	}
//
//
//	public   static ContextInfo getValue(String keys){
//		SlaveNode slaveNode =  GetNodeFromConsistentHash.getHashKey(getMasterNode(), keys);
//		return getValue(slaveNode,keys);
//	}
//	public static  <T> void putValue(String keys ,T obj){
//		SlaveNode slaveNode =  GetNodeFromConsistentHash.getHashKey(getMasterNode(), keys);
//		putValue(slaveNode,keys, obj);
//	}
//
//
//	private static SingleNode getMasterNode(){
//		String showUrl  =accpterUrl.get(randomUtil.nextInt());
//		System.out.println(showUrl);
//		return ShowMaster.getMaster(showUrl);
//	}
//	public static void main(String[] args) {
//		MemCached.putValue("key", "111111");
//
//		System.out.println((String)MemCached.getValue("key").getObject());
//	}
//}
