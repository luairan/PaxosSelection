package com.luairan.service.http;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

public class GetInfoFromJson {
	/**
	 * 鍚戞寚瀹歎RL鍙戦�丟ET鏂规硶鐨勮姹�
	 * 
	 * @param url
	 *            鍙戦�佽姹傜殑URL
	 * @param param
	 *            璇锋眰鍙傛暟锛岃姹傚弬鏁板簲璇ユ槸 name1=value1&name2=value2 鐨勫舰寮忋��
	 * @return URL 鎵�浠ｈ〃杩滅▼璧勬簮鐨勫搷搴旂粨鏋�
	 */
	public static String sendGet(String url, String param) {
		String result = "";
		BufferedReader in = null;
		try {
			String urlNameString = url + "?" + param;
			URL realUrl = new URL(urlNameString);
			// 鎵撳紑鍜孶RL涔嬮棿鐨勮繛鎺�
			URLConnection connection = realUrl.openConnection();
			// 璁剧疆閫氱敤鐨勮姹傚睘鎬�
			connection.setRequestProperty("accept", "*/*");
			connection.setRequestProperty("connection", "Keep-Alive");
			connection.setRequestProperty("user-agent",
					"Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
			// 寤虹珛瀹為檯鐨勮繛鎺�
			connection.connect();
			// 鑾峰彇鎵�鏈夊搷搴斿ご瀛楁
			Map<String, List<String>> map = connection.getHeaderFields();
			// 閬嶅巻鎵�鏈夌殑鍝嶅簲澶村瓧娈�
			for (String key : map.keySet()) {
				System.out.println(key + "--->" + map.get(key));
			}
			// 瀹氫箟 BufferedReader杈撳叆娴佹潵璇诲彇URL鐨勫搷搴�
			in = new BufferedReader(new InputStreamReader(
					connection.getInputStream()));
			String line;
			while ((line = in.readLine()) != null) {
				result += line;
			}
		} catch (Exception e) {
			System.out.println("鍙戦�丟ET璇锋眰鍑虹幇寮傚父锛�" + e);
			e.printStackTrace();
		}
		// 浣跨敤finally鍧楁潵鍏抽棴杈撳叆娴�
		finally {
			try {
				if (in != null) {
					in.close();
				}
			} catch (Exception e2) {
				e2.printStackTrace();
			}
		}
		return result;
	}

	/**
	 * 鍚戞寚瀹� URL 鍙戦�丳OST鏂规硶鐨勮姹�
	 * 
	 * @param url
	 *            鍙戦�佽姹傜殑 URL
	 * @param param
	 *            璇锋眰鍙傛暟锛岃姹傚弬鏁板簲璇ユ槸 name1=value1&name2=value2 鐨勫舰寮忋��
	 * @return 鎵�浠ｈ〃杩滅▼璧勬簮鐨勫搷搴旂粨鏋�
	 */
	public static String sendPost(String url, byte[] param) {
		PrintWriter out = null;
		BufferedReader in = null;
		String result = "";
		try {
			URL realUrl = new URL(url);
			// 鎵撳紑鍜孶RL涔嬮棿鐨勮繛鎺�
			URLConnection conn = realUrl.openConnection();

			// 璁剧疆閫氱敤鐨勮姹傚睘鎬�
			conn.setRequestProperty("accept", "*/*");
			conn.setRequestProperty("connection", "Keep-Alive");
			conn.setRequestProperty("user-agent",
					"Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
			// 鍙戦�丳OST璇锋眰蹇呴』璁剧疆濡備笅涓よ
			conn.setDoOutput(true);
			conn.setDoInput(true);
			
			
			OutputStream os = conn.getOutputStream();
			
			os.write(param);
			
			os.flush();
			
			// 瀹氫箟BufferedReader杈撳叆娴佹潵璇诲彇URL鐨勫搷搴�
			in = new BufferedReader(
					new InputStreamReader(conn.getInputStream(),Charset.forName("UTF-8")));
			String line;
			while ((line = in.readLine()) != null) {
				result += line;
			}
		} catch (Exception e) {
			System.out.println("鍙戦�� POST 璇锋眰鍑虹幇寮傚父锛�" + e);
			e.printStackTrace();
		}
		// 浣跨敤finally鍧楁潵鍏抽棴杈撳嚭娴併�佽緭鍏ユ祦
		finally {
			try {
				if (out != null) {
					out.close();
				}
				if (in != null) {
					in.close();
				}
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		return result;
	}
}