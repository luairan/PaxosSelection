package com.luairan.service.http.context;//package com.luairan.server.com.luairan.service.http.com.luairan.service.context;
//
//import java.io.BufferedInputStream;
//import java.io.BufferedOutputStream;
//import java.io.ByteArrayInputStream;
//import java.io.ByteArrayOutputStream;
//import java.io.IOException;
//import java.io.ObjectInputStream;
//import java.io.ObjectOutputStream;
//import java.io.OutputStream;
//
//import com.luairan.com.luairan.service.node.Memcached;
//import com.sun.net.httpserver.Headers;
//import com.sun.net.httpserver.HttpExchange;
//import com.sun.net.httpserver.HttpHandler;
//
//public class WriteToCache implements HttpHandler {
//
//	private Memcached mem = null;
//
//	public WriteToCache(Memcached mem) {
//		this.mem = mem;
//	}
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
//
//			ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(exchange.getRequestBody()));
//			ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(responseBody));
//			String keys = ois.readUTF();
//			byte[] s = null;
//			int len = 0;
//			int cachelen = 512;
//			byte[] cache = new byte[cachelen];
//			ByteArrayOutputStream baos = new ByteArrayOutputStream();
//			while ((len = ois.read(cache)) > 0) {
//				baos.write(cache, 0, len);
//			}
//			s = baos.toByteArray();
//			baos.flush();
//			baos.close();
//			if (s != null) {
//				mem.put(keys, s);
//			}
//			
//			
//			oos.writeUTF("finish");
//			oos.flush();
//			oos.close();
//			responseBody.close();
//		}
//	}
//	
//	public  Object toObject(byte []  bytes){
//		Object obj =null;
//		try {
//		ByteArrayInputStream bais =new ByteArrayInputStream(bytes);
//		
//		ObjectInputStream ois =new ObjectInputStream(bais);
//			obj = ois.readObject();
//			ois.close();
//			bais.close();
//		} catch (ClassNotFoundException | IOException e) {
//			e.printStackTrace();
//		}finally{
//			
//		}
//		return obj;
//	}
//}
