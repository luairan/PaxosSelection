package com.luairan.service.http.paxoslease;

import com.luairan.paxoslease.ProposerUtil;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.*;

public class HTTPProposer implements HttpHandler {


	
	
	private ProposerUtil p;
	public HTTPProposer(ProposerUtil p) {
		this.p =p;
	}

	public void handle(HttpExchange exchange) throws IOException {
		
		String requestMethod = exchange.getRequestMethod();
		if (requestMethod.equalsIgnoreCase("GET")) {
			Headers responseHeaders = exchange.getResponseHeaders();
			responseHeaders.set("Content-Type", "text/plain");
			exchange.sendResponseHeaders(200, 0);
			OutputStream responseBody = exchange.getResponseBody();
			try {
				p.proposer();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			PrintWriter pr = new PrintWriter(responseBody);
			pr.print("paxos lease is scheduling");
			pr.flush();
			pr.close();
			responseBody.close();
		}
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
				p.proposer();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			ois.readUTF();
			oos.writeUTF("paxos lease is scheduling");
			oos.flush();
			oos.close();
			responseBody.close();
		}
	}

}









