//package com.luairan.service.http;
//
//import com.luairan.client.http.context.action.PaxosTriger;
//import com.luairan.conf.Configration;
//import com.luairan.leader.context.State;
//import com.luairan.master.ConsistentHashLockMuti;
//import com.luairan.master.HashAlgorithm;
//import com.luairan.master.SingleNode;
//import com.luairan.master.SlaveNode;
//import com.luairan.node.ResourceBlockMap;
//import com.luairan.paxoslease.Acceptor;
//import com.luairan.paxoslease.ProposerUtil;
//import com.luairan.server.http.context.ConsistentHashNode;
//import com.luairan.server.http.context.GetFromCache;
//import com.luairan.server.http.context.WriteToBlockMapOne;
//import com.luairan.server.http.context.WriteToBlockMapTwo;
//import com.luairan.server.http.paxoslease.HTTPAcceptor;
//import com.luairan.server.http.paxoslease.HTTPProposeControlor;
//import com.luairan.server.http.paxoslease.HTTPProposer;
//import com.luairan.server.http.paxoslease.HTTPShower;
//import com.sun.net.httpserver.HttpServer;
//
//import java.io.IOException;
//import java.net.InetSocketAddress;
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.Map.Entry;
//import java.util.concurrent.Executors;
//import java.util.concurrent.ScheduledExecutorService;
//import java.util.concurrent.TimeUnit;
//
//public class HttpCachedMutilServer{
//	private static Map<Integer,Map<String,Boolean>> localStartUp(){
//		String localIp = Configration.getValue("/client/local");
//		Map<Integer,Map<String,Boolean>> map = new HashMap<Integer, Map<String,Boolean>>();
//		List<Map<String, String>> li = Configration.getMaps("/client/paxos-server/proposer-server/url");
//		for(Map<String, String> ma:li){
//			String httpUrl = ma.get("value");
//			if(httpUrl.indexOf(localIp)>0){
//				Integer port = Integer.parseInt(httpUrl.substring(httpUrl.lastIndexOf(":")+1));
//				putToMap(port,"poposer",map);
//			}
//		}
//		li = Configration.getMaps("/client/paxos-server/accepter-server/url");
//		for(Map<String, String> ma:li){
//			String httpUrl = ma.get("value");
//			if(httpUrl.indexOf(localIp)>0){
//				Integer port = Integer.parseInt(httpUrl.substring(httpUrl.lastIndexOf(":")+1));
//				putToMap(port,"accepter",map);
//			}
//		}
//		li = Configration.getMaps("/client/cache-server/url");
//		for(Map<String, String> ma:li){
//			String httpUrl = ma.get("value");
//			if(httpUrl.indexOf(localIp)>0){
//				Integer port = Integer.parseInt(httpUrl.substring(httpUrl.lastIndexOf(":")+1));
//				putToMap(port,"cache",map);
//			}
//		}
//		return map;
//	}
//
//	private static void putToMap(Integer key,String value,Map<Integer,Map<String,Boolean>> map){
//		if(!map.containsKey(key)) map.put(key, new HashMap<String, Boolean>());
//		map.get(key).put(value, true);
//	}
//
//	private static List<SingleNode> getSingleNodeList(){
//
//		List<SingleNode> list =new ArrayList<SingleNode>();
//		List<Map<String, String>> li = Configration.getMaps("/client/cache-server/url");
//		for(Map<String,String> ma:li){
//			String address = ma.get("value");
//			Integer port = Integer.parseInt(address.substring(address.lastIndexOf(":")+1));
//			SingleNode singleNode = new SingleNode(ma.get("name"), address, false, port);
//			list.add(singleNode);
//		}
//		return list;
//	}
//	public static void main(String[] args) throws IOException {
//		Map<Integer,Map<String,Boolean>> map  =  localStartUp();
//		for(Entry<Integer,Map<String,Boolean>> en:map.entrySet()){
//			createServer(en.getKey().intValue(),en.getValue());
//		}
//	}
//	public static void createServer(int port,Map<String,Boolean> map){
//		 ScheduledExecutorService scheduler =   Executors.newScheduledThreadPool(1);
//		InetSocketAddress addr = new InetSocketAddress(port);
//		HttpServer server = null;
//		try {
//			server = HttpServer.create(addr, 10);
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//		server.setExecutor(Executors.newCachedThreadPool());
//			if(map.get("cache")){
//				ResourceBlockMap mem = new ResourceBlockMap();
//				server.createContext(Configration.getValue("/client/cache-server/com.luairan.service.http-com.luairan.service.context/write-try"), new WriteToBlockMapOne(mem));
//				server.createContext(Configration.getValue("/client/cache-server/com.luairan.service.http-com.luairan.service.context/write-real"), new WriteToBlockMapTwo(mem));
//				server.createContext(Configration.getValue("/client/cache-server/com.luairan.service.http-com.luairan.service.context/get-cache"), new GetFromCache(mem));
//			}
//			State state =new State();
//
//			if(map.get("accepter")){
//				server.createContext(Configration.getValue("/client/paxos-server/com.luairan.service.http-com.luairan.service.context/acceptor"), new HTTPAcceptor(new Acceptor(state, Executors.newScheduledThreadPool(1),addr)));
//				server.createContext(Configration.getValue("/client/paxos-server/com.luairan.service.http-com.luairan.service.context/shower"), new HTTPShower(state));
//			}
//			if(map.get("poposer")){
//				List<SingleNode> singleList =  getSingleNodeList();
//				List<SlaveNode> node = SlaveNode.getDistributeList(singleList, Integer.parseInt(Configration.getValueByName("/client/cache-server", "copyNode")));
//				ConsistentHashLockMuti hs=new ConsistentHashLockMuti(new HashAlgorithm(), Boolean.parseBoolean(Configration.getValueByName("/client/cache-server","visualnodeMode")),node);
//				hs.buildMap();
//				ConsistentHashNode chn = 	new ConsistentHashNode(hs);
//				server.createContext(Configration.getValue("/client/paxos-server/com.luairan.service.http-com.luairan.service.context/consistenthash"), chn);
//				ProposerUtil p =new ProposerUtil(Executors.newCachedThreadPool(), state, Executors.newScheduledThreadPool(1),addr, chn);
//				server.createContext(Configration.getValue("/client/paxos-server/com.luairan.service.http-com.luairan.service.context/proposer"), new HTTPProposer(p));
//				server.createContext(Configration.getValue("/client/paxos-server/com.luairan.service.http-com.luairan.service.context/proposercancle"), new HTTPProposeControlor(p));
//				String httpUrl = "com.luairan.service.http://"+Configration.getValue("/client/local")+":"+port;
//				scheduler.schedule(new PaxosStart(httpUrl), 10L, TimeUnit.SECONDS);
//			}
//		server.start();
//		System.out.println("Server is listening on port :"+port);
//	}
//}
//
//class PaxosStart implements Runnable {
//
//private String url  ;
//	PaxosStart(String url) {
//		this.url=url;
//	}
//	@Override
//	public void run() {
//		PaxosTriger.triger(url);
//	}
//}
//
