package com.luairan.service.util;

import org.dom4j.Document;
import org.dom4j.io.SAXReader;

import java.io.InputStream;

public class XMLReader {
    public static Document readXML(String filePath) {
        SAXReader saxReader = new SAXReader();
        String stripped = filePath.startsWith("/") ? filePath.substring(1) : filePath;
        InputStream stream = null;
        Document doc = null;
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader != null) {
            stream = classLoader.getResourceAsStream(stripped);
        }
        if (stream == null) {
            stream = XMLReader.class.getResourceAsStream(filePath);
        }
        if (stream == null) {
            stream = XMLReader.class.getClassLoader().getResourceAsStream(stripped);
        }
        try {
            doc = saxReader.read(stream);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return doc;
    }
}
