package org.chuniter.core.kernel.impl.filter;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.JSONSerializer;
import com.alibaba.fastjson.serializer.PropertyPreFilter;
import com.alibaba.fastjson.serializer.SerializerFeature;

public class JsonPropertyFilter implements PropertyPreFilter {
	private String[] excludes;

	static {
		JSON.DEFAULT_GENERATE_FEATURE |= SerializerFeature.DisableCircularReferenceDetect.getMask();
	}

	public JsonPropertyFilter() {}

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
