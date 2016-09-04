//package com.luairan.service.http.context.action;
//
//import com.luairan.client.http.HTTPClient;
//import com.luairan.client.http.HTTPClient.Action;
//import com.luairan.conf.Configration;
//import com.luairan.master.SingleNode;
//import com.luairan.master.SlaveNode;
//import com.luairan.node.ContextInfo;
//import com.luairan.util.ObjectByteTranspot;
//
//import java.io.*;
//import java.util.List;
//
//
///**
// * 通过两阶段提交算法来控制  内容的提交正确问题
// * @author Mr.lu
// *
// */
//public class PutToMemcache{
//
//	//private static String oneCommit = "writetoblockcacheone/";
//	//private static String twoCommit = "writetoblockcachetwo/";
//
//	private static String oneCommit = Configration.getValue("/client/cache-server/com.luairan.service.http-com.luairan.service.context/write-try");
//	private static String twoCommit = Configration.getValue("/client/cache-server/com.luairan.service.http-com.luairan.service.context/write-real");
//
//	public static<T> ContextInfo getContextInfo(T value){
//		byte [] bytes = ObjectByteTranspot.toByteArray(value);
//		ContextInfo contextInfo =new ContextInfo();
//		contextInfo.setClassName(value.getClass().getName());
//		contextInfo.setInfo(bytes);
//		contextInfo.setSize(Long.valueOf(bytes.length));
//		return contextInfo;
//	}
//
//
//
//
//	public static <T> boolean  twoCommitParseSet(List<String> httpUrlList,String key,T value){
//		if(prepareSet(httpUrlList, key, value)){
//			return realSet(httpUrlList, key, value);
//		}
//		else{
//			return false;
//		}
//	}
//
//
//	public static <T> boolean  twoCommitParseSet(SlaveNode slaveNode,String key,T value){
//		if(prepareSet(slaveNode, key, value)){
//			return realSet(slaveNode, key, value);
//		}
//		else{
//			return false;
//		}
//	}
//	/**
//	 *  需要用多线程重写
//	 * @param httpUrl
//	 * @param key
//	 * @param value
//	 * @return
//	 */
//	private static <T>  boolean prepareSet(List<String> httpUrlList,String key,T value){
//		int count = 0;
//		ContextInfo contextInfo = getContextInfo(value);
//		for(String httpUrl: httpUrlList){
//			if(HTTPClient.sendPost(httpUrl+oneCommit, actionOne,key,contextInfo)) count++;
//		}
//		return count==httpUrlList.size()?true:false;
//	}
//
//
//	private static <T>  boolean prepareSet(SlaveNode slaveNode,String key,T value){
//		int count = 0;
//		ContextInfo contextInfo = getContextInfo(value);
//		for(SingleNode httpUrl: slaveNode.getSingleNode()){
//			if(HTTPClient.sendPost(httpUrl.getAddress()+oneCommit, actionOne,key,contextInfo)) count++;
//		}
//		return count==slaveNode.getSingleNode().size()?true:false;
//	}
//
//	private static <T>  boolean realSet(List<String> httpUrlList,String key,T value){
//		int count = 0;
//		ContextInfo contextInfo = getContextInfo(value);
//		for(String httpUrl: httpUrlList){
//			if(HTTPClient.sendPost(httpUrl+twoCommit, actionTwo,key,contextInfo)) count++;
//		}
//		return count==httpUrlList.size()?true:false;
//	}
//
//
//	private static <T>  boolean realSet(SlaveNode slaveNode,String key,T value){
//		int count = 0;
//		ContextInfo contextInfo = getContextInfo(value);
//		for(SingleNode httpUrl: slaveNode.getSingleNode()){
//			if(HTTPClient.sendPost(httpUrl.getAddress()+twoCommit, actionTwo,key,contextInfo)) count++;
//		}
//		return count==slaveNode.getSingleNode().size()?true:false;
//	}
//
//	private static Action<Boolean> actionOne =new Action<Boolean>() {
//		@Override
//		public void sendInfo(OutputStream os,Object ... args) throws IOException {
//			ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(os));
//			oos.writeUTF((String)args[0]);
//			oos.writeObject((ContextInfo)args[1]);
//			oos.flush();
//		}
//
//		@Override
//		public Boolean reciveInfo(InputStream is) throws IOException {
//			ObjectInputStream ois = new ObjectInputStream(is);
//			boolean temp = ois.readBoolean();
//			return temp;
//		}
//	};
//	private static Action<Boolean> actionTwo =new Action<Boolean>() {
//		@Override
//		public void sendInfo(OutputStream os,Object ... args) throws IOException {
//			ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(os));
//			oos.writeUTF((String)args[0]);
//			oos.writeObject((ContextInfo)args[1]);
//			oos.flush();
//		}
//
//		@Override
//		public Boolean reciveInfo(InputStream is) throws IOException {
//			ObjectInputStream ois = new ObjectInputStream(is);
//			boolean temp = ois.readBoolean();
//
//			return temp;
//		}
//	};
//	public static void main(String[] args) {
//		//PutToMemcache.set("http://127.0.0.1:8080/writetoblockcacheone/","test", new String("11111111"));
//	}
//}
