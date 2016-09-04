package com.luairan.service.http.context.action;

import com.luairan.client.http.HTTPClient;
import com.luairan.client.http.HTTPClient.Action;
import com.luairan.conf.Configration;

import java.io.*;

/**
 * 触发 Paxos主节点的选举
 * @author Mr.lu
 *
 */
public class PaxosTriger{
	
	private static String proposeContext = Configration.getValue("/client/paxos-server/com.luairan.service.http-com.luairan.service.context/proposer");
	
	
	public static String triger(String httpUrl){
		
		return HTTPClient.sendPost(httpUrl+proposeContext, showAction);
	}

	private static Action<String> showAction =new Action<String>() {
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
