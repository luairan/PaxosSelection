package com.luairan.service.util;

import java.io.*;

public class ObjectByteTranspot {

	public static byte[] toByteArray(Object obj) {
		byte[] bytes = null;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = null;
		try {
			oos = new ObjectOutputStream(baos);
			oos.writeObject(obj);
			oos.flush();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {

			try {
				oos.close();
				baos.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		bytes = baos.toByteArray();
		return bytes;
	}
	
	public static Object toObject(byte [] bytes){
		Object obj =null;
		ByteArrayInputStream bais =new ByteArrayInputStream(bytes);
		ObjectInputStream ois=null;
		try {
			ois = new ObjectInputStream(bais);
			obj = ois.readObject();
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}finally{
			try {
				ois.close();
				bais.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}
		return obj;
	}
	
}
