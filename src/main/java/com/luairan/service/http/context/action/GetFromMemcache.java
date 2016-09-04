//package com.luairan.service.http.context.action;
//
//import com.luairan.client.http.HTTPClient;
//import com.luairan.client.http.HTTPClient.Action;
//import com.luairan.conf.Configration;
//import com.luairan.master.SlaveNode;
//import com.luairan.node.ContextInfo;
//
//import java.io.*;
//import java.util.List;
//import java.util.Random;
//
///**
// *
// * @author Mr.lu
// * 得到从内存得到的内容
// */
//public class GetFromMemcache{
//
//	//private static String getContext = "getfromcache/";
//	private static String getContext = Configration.getValue("/client/cache-server/com.luairan.service.http-com.luairan.service.context/get-cache");
//	private static Random random =new Random();
//
//	public static ContextInfo get(List<String> httpUrlList,String key){
//		int rand = random.nextInt(httpUrlList.size()-1);
//		String httpUrl  = httpUrlList.get(rand);
//		return HTTPClient.sendPost(httpUrl+getContext, action,key);
//	}
//	public static ContextInfo get(SlaveNode slaveNode,String key){
//		int rand = random.nextInt(slaveNode.getSingleNode().size()-1);
//		String httpUrl  = slaveNode.getSingleNode().get(rand).getAddress();
//		return HTTPClient.sendPost(httpUrl+getContext, action,key);
//	}
//
//	private static Action<ContextInfo> action =new Action<ContextInfo>() {
//		@Override
//		public void sendInfo(OutputStream os,Object ... args) throws IOException {
//			ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(os));
//			oos.writeUTF((String)args[0]);
//			oos.flush();
//		}
//
//		@Override
//		public ContextInfo reciveInfo(InputStream is) throws IOException, ClassNotFoundException {
//			ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(is));
//			ContextInfo contextInfo = (ContextInfo) ois.readObject();
////			int len = 0;
////			int cachelen = 512;
////			byte[] cache = new byte[cachelen];
////			ByteArrayOutputStream baos = new ByteArrayOutputStream();
////			while ((len = ois.read(cache)) > 0) {
////				baos.write(cache, 0, len);
////			}
//			//byte[] s = baos.toByteArray();
//			return contextInfo;
//		}
//
//	};
//
//}
