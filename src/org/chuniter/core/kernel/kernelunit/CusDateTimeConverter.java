package org.chuniter.core.kernel.kernelunit;

 

import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.apache.commons.beanutils.ConvertUtilsBean;
import org.apache.commons.beanutils.Converter;

public class CusDateTimeConverter extends  ConvertUtilsBean implements Converter{

	public CusDateTimeConverter(){
		this.register(new DateTimeConverter(), Date.class);
	}
	public Object convert(Class type, Object value) {
		return super.convert(value,type);
	}
 
	public static Object convertdatev(String value,Class type) {
		if(type.getName().equals(Date.class.getName())){
			if(value.length()<=DateTimeConverter.YM.length()){
				return DateUtil.stringToDate(value,DateTimeConverter.YM);
			}
			if(value.length()<=DateTimeConverter.DATE.length()){
				return DateUtil.stringToDate(value,DateTimeConverter.DATE);
			}
			if(value.length()<=DateTimeConverter.YMDHM.length()){
				return DateUtil.stringToDate(value,DateTimeConverter.YMDHM);
			}
			return DateUtil.stringToDate(value,"yyyy-MM-dd HH:mm:ss");
		}else
			return value;
	}	
	@Override
	public Object convert(String value,Class type) {
		if(type.getName().equals(Date.class.getName())){ 
			return convertdatev(value,type);
		}else
			return super.convert(value,type);
	}
	class DateTimeConverter implements Converter {
		public static final String YM      = "yyyy-MM";
	    public static final String DATE      = "yyyy-MM-dd";
	    public static final String YMDHM     = "yyyy-MM-dd HH:mm";
	    public static final String DATETIME  = "yyyy-MM-dd HH:mm:ss";
	    public static final String TIMESTAMP = "yyyy-MM-dd HH:mm:ss.SSS";
	 
	    @Override
	    public Object convert(Class type, Object value) {
	        return toDate(type, value);
	    }
	 
	    public  Object toDate(Class type, Object value) {
	        if (value == null || "".equals(value))
	            return null;
	        if (value instanceof String) {
	            String dateValue = value.toString().trim();
	            int length = dateValue.length();
	            if (type.equals(java.util.Date.class)) {
	                try {
	                    DateFormat formatter = null;
	                    if (length <= 8) {
	                        formatter = new SimpleDateFormat(YM, new DateFormatSymbols(Locale.CHINA));
	                        return formatter.parse(dateValue);
	                    }
	                    if (length <= 10) {
	                        formatter = new SimpleDateFormat(DATE, new DateFormatSymbols(Locale.CHINA));
	                        return formatter.parse(dateValue);
	                    }
	                    if (length <= 19) {
	                        formatter = new SimpleDateFormat(DATETIME, new DateFormatSymbols(Locale.CHINA));
	                        return formatter.parse(dateValue);
	                    }
	                    if (length <= 23) {
	                        formatter = new SimpleDateFormat(TIMESTAMP, new DateFormatSymbols(Locale.CHINA));
	                        return formatter.parse(dateValue);
	                    }
	                } catch (Exception e) {
	                    e.printStackTrace();
	                }
	            }
	        }
	        return value;
	    }
	}
}
