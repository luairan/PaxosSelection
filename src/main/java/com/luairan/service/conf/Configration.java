package com.luairan.service.conf;

import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.Element;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;


/**
 * 事件类型
 * 
 * @author luairan
 *
 */
public final class Configration {
	private static Map<String,List<Map<String,String>>> map = new HashMap<String,List<Map<String,String>>>();
	
	static {
		Document doc=XMLReader.readXML("client.xml");
		Element root=doc.getRootElement();
		putOneToMap(map, root, "");
		for(Entry<String,List<Map<String,String>>> en:map.entrySet()){
			System.out.println(en.getKey());
			for(Map<String,String> value:en.getValue()){
				for(Entry<String, String> e:value.entrySet()){
					System.out.println("\t\t"+e.getKey()+":"+e.getValue());
				}
			}
			
		}
	}


	public static void main(String[] args) {
//		Integer port = Integer.parseInt("http://127.0.0.1:8080".substring("http://127.0.0.1:8080".lastIndexOf(":")+1));
//		System.out.println(port);
	}
	
	
	
	private static void putTheMap(Map<String,List<Map<String,String>>> map ,Element root,String before){
		if(root==null) return ;
		@SuppressWarnings("unchecked")
		List<Element> l =root.elements();
		if(l.size()>0)
		for(Element e  :l){
			putOneToMap(map, e, before);
		}
	}
	
	
	
	
	private static void putOneToMap(Map<String,List<Map<String,String>>> map ,Element root,String before){
		String key =before+"/"+root.getName();
		Map<String,String> value= new HashMap<String, String>();
		String text= root.getTextTrim();
		if(!text.equals(""))
		value.put("value", root.getTextTrim());
		@SuppressWarnings("unchecked")
		List<Attribute> list =	root.attributes();
		if(list.size()>0)
		for(Attribute a :list){
			value.put(a.getName(), a.getText());
		}
		if(value.size()>0)
		putIfNotNull(map, key, value);
		putTheMap(map,root, key);
	}
	private static void putIfNotNull(Map<String,List<Map<String,String>>> map,String key ,Map<String,String> value){
		if(!map.containsKey(key)) map.put(key, new ArrayList<Map<String,String>>());
		map.get(key).add(value);
	}
	
	

	public static List<Map<String, String>>  getMaps(String key){
		return map.get(key);
	}
	public static String  getValue(String key){
		return map.get(key).get(0).get("value");
	}
	public static String  getValueByName(String key,String name){
		return map.get(key).get(0).get(name);
	}
}
