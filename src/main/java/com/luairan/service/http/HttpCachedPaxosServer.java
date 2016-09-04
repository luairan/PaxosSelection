package com.luairan.service.http;//package com.luairan.server.com.luairan.service.http;
//
//import java.io.IOException;
//import java.net.InetSocketAddress;
//import java.com.luairan.service.util.concurrent.Executors;
//
//import com.luairan.leader.com.luairan.service.context.State;
//import com.luairan.com.luairan.service.paxoslease.Acceptor;
//import com.luairan.server.com.luairan.service.http.com.luairan.service.paxoslease.HTTPAcceptor;
//import com.luairan.server.com.luairan.service.http.com.luairan.service.paxoslease.HTTPProposer;
//import com.luairan.server.com.luairan.service.http.com.luairan.service.paxoslease.HTTPShower;
//import com.sun.net.httpserver.HttpServer;
//
//public class HttpCachedPaxosServer{
//	
//	
//	public static void main(String[] args) throws IOException {
//		for(int i=0;i<5;i++){
//			createServer(8080+i);
//		}
//
//	}
//
//	public static void createServer(int port){
//		InetSocketAddress addr = new InetSocketAddress(port);
//		HttpServer server = null;
//		
//		try {
//			server = HttpServer.create(addr, 10);
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//		State state =new State();
//		server.createContext("/paxosacceptor/", new HTTPAcceptor(new Acceptor(state, Executors.newScheduledThreadPool(1),addr)));
//		server.createContext("/paxospropose/", new HTTPProposer(state, Executors.newScheduledThreadPool(1)));
//		server.createContext("/paxosshower/", new HTTPShower(state));
//		server.setExecutor(Executors.newCachedThreadPool());
//		server.start();
//		System.out.println("Server is listening on port :"+port);
//	}
//}
//
