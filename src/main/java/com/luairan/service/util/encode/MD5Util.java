package com.luairan.service.util.encode;

import java.security.MessageDigest;

public class MD5Util {
	public final static byte[] MD5(String s) {
		try {
			byte[] btInput = s.getBytes();
			// 获得MD5摘要算法的 MessageDigest 对象
			MessageDigest mdInst = MessageDigest.getInstance("MD5");
			// 使用指定的字节更新摘要
			mdInst.update(btInput);
			// 获得密文
			byte[] md = mdInst.digest();
			return md;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}