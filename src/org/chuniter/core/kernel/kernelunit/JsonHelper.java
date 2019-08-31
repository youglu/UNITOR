package org.chuniter.core.kernel.kernelunit;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.JSONSerializer;
import com.alibaba.fastjson.serializer.PropertyPreFilter;
import com.alibaba.fastjson.serializer.SerializeConfig;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alibaba.fastjson.serializer.SimpleDateFormatSerializer;

public class JsonHelper {
	private static SerializeConfig sConfig = new SerializeConfig();
	private static SerializeConfig sConfig2 = new SerializeConfig();
	static {
		sConfig.put(Date.class, new SimpleDateFormatSerializer("yyyy-MM-dd"));
		sConfig2.put(Date.class, new SimpleDateFormatSerializer("yyyy-MM-dd HH:mm:ss"));
	}
	
	private static String[] excludeStr = new String[] { "lastModifyMan", "lastModifyDate",
		"lastModifyDateStr", "pN", "password", "createMan",
		"weixinKey", "qqKey", "weiboKey", "drivinglicenseNo", "hxPwd",
		"lastOperation", "optimizApp", "ecode", "receiveAddress",
		"defaultCarId", "dlValidDate", "dlInvalidDate" };
	
	/**
	 * 将实体类转换成JSON
	 * 时间格式：yyyy-MM-dd
	 * @param obj 实体类
	 * @return
	 */
	public static JSONObject toJSON(Object obj) {
		return toJSON(obj, null);
	}
	
	/**
	 * 将实体类转换成JSON
	 * 时间格式：yyyy-MM-dd HH:mi:ss
	 * @param obj 实体类
	 * @return
	 */
	public static JSONObject toJSONDateStr(Object obj) {
		return toJSONDateStr(obj, null);
	}
	
	/**
	 * 将实体类转换成JSON
	 * 时间格式：yyyy-MM-dd
	 * @param obj 实体类
	 * @param excludes 不转换指定
	 * @return
	 */
	public static JSONObject toJSON(Object obj, String[] excludes){
		return toJSON(obj, excludes, sConfig);
	}
	
	/**
	 * 将实体类转换成JSON
	 * 时间格式：yyyy-MM-dd HH:mi:ss
	 * @param obj 实体类
	 * @param excludes 不转换指定
	 * @return
	 */
	public static JSONObject toJSONDateStr(Object obj, String[] excludes){
		return toJSON(obj, excludes, sConfig2);
	}
	
	/**
	 * 将实体类转换成JSON
	 * @param obj 实体类
	 * @param excludes 不转换指定
	 * @return
	 */
	public static JSONObject toJSON(Object obj, String[] excludes, SerializeConfig config){
		if (excludes == null) {
			excludes = excludeStr;
		}
		if (obj == null)
			return new JSONObject();
		formatDate(obj);
		String jsonStr = null;
		JsonPropertyFilter filter = new JsonPropertyFilter();
		filter.setExcludes(excludes);
		jsonStr = JSONObject.toJSONString(obj, config, filter, SerializerFeature.WriteMapNullValue);
		jsonStr = jsonStr.replaceAll(":null", ":\"\"");
		JSONObject result = JSONObject.parseObject(jsonStr);
		// 转换距离
		for(Iterator<Entry<String, Object>> it = result.entrySet().iterator(); it.hasNext();) {
			Entry<String, Object> entry = (Entry<String, Object>) it.next();
			if (entry.getKey().equals("distance")) {
				result.put(entry.getKey(), getDistance(entry.getValue()));
			}
		}
		return result;
	}
	
	/**
	 * 获取距离
	 * @param value
	 * @return
	 */
	private static String getDistance(Object value) {
		String result = "";
		if (value == null || value.equals(""))
			return result;
		try {
			double distance = Double.parseDouble(value.toString());
			if (distance >= 1000) {
				BigDecimal din = new BigDecimal(distance/1000);
				din = din.setScale(1, BigDecimal.ROUND_HALF_UP);
				result = din.toString()+"公里";
			}
			else {
				BigDecimal din = new BigDecimal(distance);
				din = din.setScale(0, BigDecimal.ROUND_HALF_UP);
				result = din.toString()+"米";
			}
		} catch (NumberFormatException e) {
			e.printStackTrace();
		}
		
		return result;
	}
	
	/**
	 * 将列表、数组转换成JSON数组
	 * 时间格式：yyyy-MM-dd
	 * @param obj
	 * @return
	 */
	public static JSONArray toJSONArray(Object obj){
		JSONArray result = new JSONArray();
		if (obj == null)
			return result;
		if (obj instanceof List) {
			List<?> list = (List<?>)obj;
			for (Object child : list) {
				result.add(toJSON(child));
			}
		} else {
			JSONArray jsonArr = (JSONArray) JSONArray.toJSON(obj);
			for (Iterator<Object> it = jsonArr.iterator();it.hasNext();) {
				result.add(toJSON(it.next()));
			}
		}
		return result;
	}
	
	/**
	 * 将列表、数组转换成JSON数组
	 * 时间格式：yyyy-MM-dd HH:mi:ss
	 * @param obj
	 * @return
	 */
	public static JSONArray toJSONArrayDateStr(Object obj){
		JSONArray result = new JSONArray();
		if (obj == null)
			return result;
		if (obj instanceof List) {
			List<?> list = (List<?>)obj;
			for (Object child : list) {
				result.add(toJSONDateStr(child));
			}
		} else {
			JSONArray jsonArr = (JSONArray) JSONArray.toJSON(obj);
			for (Iterator<Object> it = jsonArr.iterator();it.hasNext();) {
				result.add(toJSONDateStr(it.next()));
			}
		}
		return result;
	}
	
	private static void formatDate(Object model) {
		Field[] field = model.getClass().getDeclaredFields(); // 获取实体类的所有属性，返回Field数组
        try {
            for (int j = 0; j < field.length; j++) { // 遍历所有属性
                String name = field[j].getName(); // 获取属性的名字
                name = name.substring(0, 1).toUpperCase() + name.substring(1); // 将属性的首字符大写，方便构造get，set方法
                String type = field[j].getGenericType().toString(); // 获取属性的类型
                if (type.equals("class java.util.Date")) {
                    Method m = model.getClass().getMethod("get" + name);
                    Date value = (Date) m.invoke(model);
                    if (value != null) {
                        m = model.getClass().getMethod("set"+name,Date.class);
                        m.invoke(model, new Date(value.getTime()));
                    }
                }
            }
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

	}
	
}

class JsonPropertyFilter implements PropertyPreFilter {
	private String[] excludes;
	static {
		JSON.DEFAULT_GENERATE_FEATURE |= SerializerFeature.DisableCircularReferenceDetect.getMask();
	}
	public JsonPropertyFilter() {
	}
	public JsonPropertyFilter(String[] excludeStr) {
		this.excludes = excludeStr;
	}
	public boolean apply(JSONSerializer serializer, Object source, String name) {
		// 对象为空。直接放行
		if (source == null || excludes == null) {
			return true;
		}
		if (isHave(excludes, name)) {
			return false;
		}
		return true;
	}
	/*
	 * 此方法有两个参数，第一个是要查找的字符串数组，第二个是要查找的字符或字符串
	 */
	public static boolean isHave(String[] strs, String s) {
		for (int i = 0; i < strs.length; i++) {
			// 循环查找字符串数组中的每个字符串中是否包含所有查找的内容
			if (strs[i].equals(s)) {
				// 查找到了就返回真，不在继续查询
				return true;
			}
		}
		// 没找到返回false
		return false;
	}
	public String[] getExcludes() {
		return excludes;
	}
	public void setExcludes(String[] excludes) {
		this.excludes = excludes;
	}
}
