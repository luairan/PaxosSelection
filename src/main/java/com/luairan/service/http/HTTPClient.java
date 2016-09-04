package com.luairan.service.http;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

public class HTTPClient {

	public interface Action <T>{
		public void sendInfo(OutputStream os, Object... args) throws Exception;
		public T reciveInfo(InputStream is) throws Exception;
	}
	
	public static<T> T sendPost(String url,Action<T> action) {
		ObjectOutputStream oos = null;
		ObjectInputStream ois = null;
		try {
			URL realUrl = new URL(url);
			HttpURLConnection conn = (HttpURLConnection) realUrl.openConnection();
			conn.setRequestProperty("accept", "*/*");
			conn.setRequestProperty("connection", "Keep-Alive");
//			conn.setRequestProperty("user-agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
			conn.setDoOutput(true);
			conn.setDoInput(true);
			conn.setRequestMethod("POST");
			action.sendInfo(conn.getOutputStream());
			conn.getOutputStream().flush();
		  return action.reciveInfo(conn.getInputStream());
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		} finally {
			try {
				if (oos != null) {
					oos.close();
				}
				if (ois != null) {
					ois.close();
				}
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	}

	public static<T> T sendPost(String url,Action<T> action,Object... args ) {
		ObjectOutputStream oos = null;
		ObjectInputStream ois = null;
		try {
			URL realUrl = new URL(url);
			HttpURLConnection conn = (HttpURLConnection) realUrl.openConnection();
			conn.setRequestProperty("accept", "*/*");
			conn.setRequestProperty("connection", "Keep-Alive");
//			conn.setRequestProperty("user-agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
			conn.setDoOutput(true);
			conn.setDoInput(true);
			conn.setRequestMethod("POST");
			action.sendInfo(conn.getOutputStream(),args);
			conn.getOutputStream().flush();
		  return action.reciveInfo(conn.getInputStream());
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		} finally {
			try {
				if (oos != null) {
					oos.close();
				}
				if (ois != null) {
					ois.close();
				}
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	}
	public static void main(String[] args) {

//		HTTPClient.sendPost("http://192.168.0.107:8080/writetocache/",new Action() {
//					@Override
//					public void sendInfo(OutputStream os) throws IOException {
//						ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(os));
//						oos.writeUTF("test");			
//						Integer i =new Integer(1111);
//						byte [] by =ObjectByteTranspot.toByteArray(i);
//						oos.write(by);
//						oos.flush();
//					}
//					
//					@Override
//					public void reciveInfo(InputStream is) throws IOException {
//						
//						ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(is));
//						String temp = ois.readUTF();
//						System.out.println(temp + "=======");
//					}
//				});
//		
//		HTTPClient.sendPost("http://192.168.0.107:8080/getfromcache/",new Action() {
//			@Override
//			public void sendInfo(OutputStream os) throws IOException {
//				ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(os));
//				oos.writeUTF("test");
//				oos.flush();
//			}
//			
//			@Override
//			public void reciveInfo(InputStream is) throws IOException, ClassNotFoundException {
//				ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(is));
//				String ss = ois.readUTF();
//				System.out.println(ss);
//				int len = 0;
//				int cachelen = 512;
//				byte[] cache = new byte[cachelen];
//				ByteArrayOutputStream baos = new ByteArrayOutputStream();
//				while ((len = ois.read(cache)) > 0) {
//					baos.write(cache, 0, len);
//				}
//				byte[] s = baos.toByteArray();
//				Integer i = (Integer) ObjectByteTranspot.toObject(s);
//				System.out.println(i + "=======");
//			}
//		});
		
//		
//		HTTPClient.sendPost("http://192.168.0.107:8080/consistenthash/",new Action() {
//			@Override
//			public void sendInfo(OutputStream os) throws IOException {
//				ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(os));
//				oos.writeUTF("t9");
//				oos.flush();
//			}
//			
//			@Override
//			public Object reciveInfo(InputStream is) throws IOException, ClassNotFoundException {
//				ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(is));
//				String name = ois.readUTF();
//				String address = ois.readUTF();
//				System.out.println("name:"+name);
//				System.out.println("address:"+address);
//				return null;
//			}
//		});
	}
}