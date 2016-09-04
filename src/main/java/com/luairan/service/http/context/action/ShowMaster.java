package com.luairan.service.http.context.action;

import com.luairan.client.http.HTTPClient;
import com.luairan.client.http.HTTPClient.Action;
import com.luairan.conf.Configration;
import com.luairan.master.SingleNode;

import java.io.*;


/**
 * 从各个节点查找主节点
 * @author Mr.lu
 *
 */
public class ShowMaster{
	
	private static String showContext = Configration.getValue("/client/paxos-server/com.luairan.service.http-com.luairan.service.context/shower");

	public static SingleNode getMaster(String httpUrl){
		
		return HTTPClient.sendPost(httpUrl+showContext, showAction);
	}
	
	

	private static Action<SingleNode> showAction =new Action<SingleNode>() {
		@Override
		public void sendInfo(OutputStream os,Object ... args) throws IOException {
			ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(os));
			oos.writeUTF("test");			
			oos.flush();
		}
		
		@Override
		public SingleNode reciveInfo(InputStream is) throws IOException, ClassNotFoundException {
			ObjectInputStream ois = new ObjectInputStream(is);
			return (SingleNode) ois.readObject();
		}
	};

}
