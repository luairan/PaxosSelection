package com.luairan.service.http.context.action;

import com.luairan.client.http.HTTPClient;
import com.luairan.client.http.HTTPClient.Action;
import com.luairan.conf.Configration;
import com.luairan.master.SingleNode;
import com.luairan.master.SlaveNode;

import java.io.*;


/**
 * 从主节点的一致性hash算法中查找对应key 的 节点
 * @author Mr.lu
 *
 */
public class GetNodeFromConsistentHash{
	
	
	private static String consistenthashContext = Configration.getValue("/client/paxos-server/com.luairan.service.http-com.luairan.service.context/consistenthash");
	
	public static SlaveNode getHashKey(String  httpUrl,String key){
//		 return HTTPClient.sendPost("http://192.168.0.107:8080/consistenthash/", action,key);
		 return HTTPClient.sendPost(httpUrl+consistenthashContext, action,key);
	}
	public static SlaveNode getHashKey(SingleNode  httpUrl,String key){
//		 return HTTPClient.sendPost("http://192.168.0.107:8080/consistenthash/", action,key);
		System.out.println(httpUrl);
		 return HTTPClient.sendPost(httpUrl.getAddress()+consistenthashContext, action,key);
	}
	
	private static Action<SlaveNode> action =new Action<SlaveNode>() {
		@Override
		public void sendInfo(OutputStream os,Object ... args) throws IOException {
			ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(os));
			oos.writeUTF((String)args[0]);			
			oos.flush();
		}
		
		@Override
		public SlaveNode reciveInfo(InputStream is) throws IOException, ClassNotFoundException {
			ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(is));
			return  (SlaveNode) ois.readObject();
		}
	};

}
