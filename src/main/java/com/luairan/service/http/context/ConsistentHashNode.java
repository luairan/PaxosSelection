//package com.luairan.service.http.context;
//
//import com.sun.net.httpserver.Headers;
//import com.sun.net.httpserver.HttpExchange;
//import com.sun.net.httpserver.HttpHandler;
//
//import java.io.*;
//import java.util.Map.Entry;
//
//public class ConsistentHashNode implements HttpHandler {
//
//	private ConsistentHashLockMuti consistentHashLock ;
//	public ConsistentHashNode(ConsistentHashLockMuti consistentHashLock) {
//		this.consistentHashLock = consistentHashLock;
//	}
//
//
//	public ConsistentHashLockMuti getConsistentHashLock() {
//		return consistentHashLock;
//	}
//
//
//	public void handle(HttpExchange exchange) throws IOException {
//		String requestMethod = exchange.getRequestMethod();
//		if (requestMethod.equalsIgnoreCase("POST")) {
//			Headers responseHeaders = exchange.getResponseHeaders();
//			responseHeaders.set("Content-Type", "application/octet-stream");
//			responseHeaders.set("Accept-Ranges", "bytes");
//			responseHeaders.set("Connection", "Keep-Alive");
//			exchange.sendResponseHeaders(200, 0);
//			OutputStream responseBody = exchange.getResponseBody();
//			ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(exchange.getRequestBody()));
//			ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(responseBody));
//			String keys = ois.readUTF();
//			SlaveNode slaveNode = consistentHashLock.getNode(keys);
////			oos.writeUTF(slaveNode.name);
////			oos.writeUTF(slaveNode.address);
//
//			oos.writeObject(slaveNode);
//			oos.flush();
//			oos.close();
//		}
//		if (requestMethod.equalsIgnoreCase("GET")) {
//            Headers responseHeaders = exchange.getResponseHeaders();
//            responseHeaders.set("Content-Type", "text/plain");
//            exchange.sendResponseHeaders(200, 0);
//            OutputStream responseBody = exchange.getResponseBody();
//            PrintWriter pr =new PrintWriter(responseBody);
//            for(Entry<Long, SlaveNode> entry :consistentHashLock.getMap().entrySet()){
//            	pr.println(entry.getKey()+"\t:\t");
//            	SlaveNode slaveNode = entry.getValue();
//            	for(SingleNode singleNode: slaveNode.getSingleNode())
//            	pr.println("\t\t\t"+singleNode);
//            }
//            pr.flush();
//            pr.close();
//            responseBody.close();
//        }
//	}
//}
