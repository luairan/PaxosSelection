package com.luairan.service.http.paxoslease;

import com.luairan.leader.context.Request;
import com.luairan.leader.context.Request.Type;
import com.luairan.leader.context.Response;
import com.luairan.paxoslease.Acceptor;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.*;
import java.util.Date;

public class HTTPAcceptor implements HttpHandler {
	
	private Acceptor acceptor =null;
	
	public HTTPAcceptor(Acceptor acceptor){
		this.acceptor = acceptor;
	}
	public  void handle(HttpExchange exchange) throws IOException {
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
			try {
				
				Request request = (Request) ois.readObject();
				Response response = null;
				if (request.getType()==Type.PrepareRequest) {
					System.out.println(new Date()+"\t"+exchange.getLocalAddress()+"\tPrepareRequest");
					response =acceptor.prepareRequest(request);
				}else if(request.getType()==Type.ProposeRequest){
					System.out.println(new Date()+"\t"+exchange.getLocalAddress()+"\tProposeRequest");
					response =acceptor.proposeReuqest(request);
				}
				oos.writeObject(response);
				oos.reset();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
			
			oos.flush();
			oos.close();
		}
	}
}
