package org.chuniter.core.kernel.model;

import java.util.Date;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class NOCreator extends BaseEntity{

	/**
	 * 
	 */
	private static final long serialVersionUID = 3993903124951194418L;

	private String id;
	private String utype;
	private Integer uno;
	private Boolean useed = false;
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getUtype() {
		return utype;
	}
	public void setUtype(String utype) {
		this.utype = utype;
	}
	public Integer getUno() {
		return uno;
	}
	public void setUno(Integer uno) {
		this.uno = uno;
	}
	
	public Boolean getUseed() {
		return useed;
	}
	public void setUseed(Boolean useed) {
		this.useed = useed;
	}
	/** 产生一个随机的字符串，适用于JDK 1.7 */  
	public static String random(int length) {  
	    StringBuilder builder = new StringBuilder(length);  
	    for (int i = 0; i < length; i++) {  
	        builder.append((char) (ThreadLocalRandom.current().nextInt(33, 128)));  
	    }  
	    return builder.toString();  
	}  
	/** 产生一个随机的字符串*/ 
	public static String RandomString(int length) {  
	    String str = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"+(new Date().getTime());  
	    Random random = new Random();  
	    int sl = str.length();
	    StringBuffer buf = new StringBuffer();  
	    for (int i = 0; i < length; i++) {  
	        int num = random.nextInt(sl);  
	        buf.append(str.charAt(num));  
	    }  
	    return buf.toString();  
	}
	
}
