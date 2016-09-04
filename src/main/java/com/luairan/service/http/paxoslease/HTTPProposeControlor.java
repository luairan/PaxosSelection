package com.luairan.service.http.paxoslease;

import com.luairan.paxoslease.ProposerUtil;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

public class HTTPProposeControlor implements HttpHandler {


	
	
	private ProposerUtil p;
	public HTTPProposeControlor(ProposerUtil p) {
		this.p =p;
	}

	public void handle(HttpExchange exchange) throws IOException {
		
		String requestMethod = exchange.getRequestMethod();
		if (requestMethod.equalsIgnoreCase("GET")) {
			Headers responseHeaders = exchange.getResponseHeaders();
			responseHeaders.set("Content-Type", "text/plain");
			exchange.sendResponseHeaders(200, 0);
			OutputStream responseBody = exchange.getResponseBody();
				p.cancle();
			PrintWriter pr = new PrintWriter(responseBody);
			pr.print("paxos lease is cancleing");
			pr.flush();
			pr.close();
			responseBody.close();
		}
	
	}

}









