package org.chuniter.core.kernel.kernelunit;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class PropertiesUtil {
	private Properties pro = null;
	private static PropertiesUtil instance = new PropertiesUtil();
	private PropertiesUtil(){
		
	}
	public synchronized static PropertiesUtil getInstance(String filePath) throws Exception{
		instance.loadProp(filePath);
		return instance;
	}
	public synchronized static PropertiesUtil getInstance(InputStream ins) throws Exception{
		instance.loadProp(ins);
		return instance;
	}
	private void loadProp(InputStream ins) throws Exception{
		pro = getProperties(ins);
	}
	private void loadProp(String filePath) throws Exception{
		pro = getProperties(filePath);
	}
	public String getValue(String key){
		if(null != pro)
			return pro.getProperty(key);
		return null;
	}
	public static Properties getProperties(String fileName) throws Exception {
		InputStream in = PropertiesUtil.class.getResourceAsStream("/"+ fileName);
		if(null == in)
			return null;
		Properties prop = new Properties();
		try {
			prop.load(in);
		} catch (IOException e) {
			throw new Exception(e.getLocalizedMessage());
		} finally {
			try {
				if(null != in)
					in.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return prop;
	}
	public static Properties getProperties(InputStream ins) throws Exception { 
		Properties prop = new Properties();
		try {
			prop.load(ins);
		} catch (IOException e) {
			throw new Exception(e.getLocalizedMessage());
		} finally {
			try {
				if(null != ins)
					ins.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return prop;
	}
	
	public Properties getProp(){
		return this.pro;
	}
}