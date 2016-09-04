package com.luairan.service.http.context.action;


import com.luairan.service.http.HTTPClient;

import java.io.*;


/**
 * 从各个节点查找主节点
 * @author Mr.lu
 *
 */
public class ShowMaster{
	
	private static String showContext = "/paxosshower/";

	public static String  getMaster(String httpUrl){
		
		return HTTPClient.sendPost(httpUrl+showContext, showAction);
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
			return (String) ois.readObject();
		}
	};

}
