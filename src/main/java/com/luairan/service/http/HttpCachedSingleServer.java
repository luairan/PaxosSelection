package com.luairan.service.http;//package com.luairan.server.com.luairan.service.http;
//
//import java.io.IOException;
//import java.net.InetSocketAddress;
//import java.com.luairan.service.util.concurrent.Executors;
//
//import com.luairan.com.luairan.service.node.Memcached;
//import com.luairan.server.com.luairan.service.http.com.luairan.service.context.GetFromCache;
//import com.luairan.server.com.luairan.service.http.com.luairan.service.context.WriteToCache;
//import com.sun.net.httpserver.HttpServer;
//
//public class HttpCachedSingleServer {
//	public static void main(String[] args) throws IOException {
//		Memcached mem = new Memcached();
//		InetSocketAddress addr = new InetSocketAddress(8080);
//		HttpServer server = HttpServer.create(addr, 10);
//		server.createContext("/writetocache/", new WriteToCache(mem));
//		server.setExecutor(Executors.newCachedThreadPool());
//		server.start();
//		System.out.println("Server is listening on port 8080");
//	}
//}
//
