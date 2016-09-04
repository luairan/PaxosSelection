package com.luairan.service.http.context;

import com.luairan.node.ContextInfo;
import com.luairan.node.ResourceBlockMap;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.*;

public class WriteToBlockMapOne implements HttpHandler {

	private ResourceBlockMap mem = null;

	public WriteToBlockMapOne(ResourceBlockMap mem) {
		this.mem = mem;
	}

	public void handle(HttpExchange exchange) throws IOException {
		String requestMethod = exchange.getRequestMethod();
		if (requestMethod.equalsIgnoreCase("POST")) {
			Headers responseHeaders = exchange.getResponseHeaders();
			responseHeaders.set("Content-Type", "application/octet-stream");
			responseHeaders.set("Accept-Ranges", "bytes");
			responseHeaders.set("Connection", "Keep-Alive");
			exchange.sendResponseHeaders(200, 0);
			OutputStream responseBody = exchange.getResponseBody();

			ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(exchange.getRequestBody()));
			ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(responseBody));
			String keys = ois.readUTF();		
			
			
			
			ContextInfo contextInfo  = null ; 
			try {
				contextInfo = (ContextInfo) ois.readObject();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
			
		    boolean flag = mem.tryPutToMem(keys, contextInfo);
			oos.writeBoolean(flag);
			oos.flush();
			oos.close();
			responseBody.close();
		}
	}
	
	public  Object toObject(byte []  bytes){
		Object obj =null;
		try {
		ByteArrayInputStream bais =new ByteArrayInputStream(bytes);
		
		ObjectInputStream ois =new ObjectInputStream(bais);
			obj = ois.readObject();
			ois.close();
			bais.close();
		} catch (ClassNotFoundException | IOException e) {
			e.printStackTrace();
		}finally{
			
		}
		return obj;
	}
}
