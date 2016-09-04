package com.luairan.service.http.paxoslease;

import com.alibaba.fastjson.JSON;
import com.luairan.service.context.State;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.*;

public class HTTPShower implements HttpHandler {

	private State state = null;
	public HTTPShower(State state) {
		this.state = state;
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
			@SuppressWarnings("unused")
			String keys = ois.readUTF();
			oos.writeObject(JSON.toJSONString(state.getAccpetedProposal()));
			oos.flush();
			oos.close();
		}
		if (requestMethod.equalsIgnoreCase("GET")) {
            Headers responseHeaders = exchange.getResponseHeaders();
            responseHeaders.set("Content-Type", "text/plain");
            exchange.sendResponseHeaders(200, 0);
            OutputStream responseBody = exchange.getResponseBody();
            PrintWriter pr =new PrintWriter(responseBody);
            pr.println(JSON.toJSONString(state.getAccpetedProposal()));
            pr.flush();
            pr.close();
            responseBody.close();
        }
	}

}









