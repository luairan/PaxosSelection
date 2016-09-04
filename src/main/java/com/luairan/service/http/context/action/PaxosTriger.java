package com.luairan.service.http.context.action;


import com.luairan.service.http.HTTPClient;

import java.io.*;

/**
 * 触发 Paxos主节点的选举
 * @author Mr.lu
 *
 */
public class PaxosTriger{
	
	private static String proposeContext = "/paxospropose/";
	
	
	public static String triger(String httpUrl){
		
		return HTTPClient.sendPost(httpUrl+proposeContext, showAction);
	}

	private static HTTPClient.Action<String> showAction =new HTTPClient.Action<String>() {
		@Override
		public void sendInfo(OutputStream os,Object ... args) throws IOException {
			ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(os));
			oos.writeUTF("test");			
			oos.flush();
		}

		@Override
		public String reciveInfo(InputStream is) throws IOException, ClassNotFoundException {
			ObjectInputStream ois = new ObjectInputStream(is);
			return ois.readUTF();
		}
	};

}
