// Decompiled by Jad v1.5.8e2. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://kpdus.tripod.com/jad.html
// Decompiler options: packimports(3) fieldsfirst ansi space 
// Source File Name:   StringUtil.java

package org.chuniter.core.kernel.kernelunit;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class StringUtil {

	private StringUtil() {
	}

	public static boolean isValid(String string) {
		return string != null && !"".equals(string.trim());
	}

	public static String decodeURI(String url) {
		String result = null;
		try {
			result = URLDecoder.decode(url, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return result;
	}

	public static boolean isDate(String string) {
		boolean result = false;
		if (isValid(string)) {
			String regex = "^(\\d{4})(-|/){0,1}(\\d{2})(-|/){0,1}(\\d{2})$";
			Pattern p = Pattern.compile(regex);
			Matcher m = p.matcher(string);
			if (m.find())
				result = true;
		}
		return result;
	}

	public static boolean isNumber(String string) {
		if (isValid(string)) {
			Pattern pattern = Pattern.compile("(^(-?\\d+)(\\.\\d+)?$)");
			Matcher isNum = pattern.matcher(string);
			if(isNum.matches() ){
				return true;
			}
			return false;
		}
		return false;
	}

	public static String getDownloadFileName(String fileName) {
		if (fileName != null)
			try {
				fileName = new String(fileName.getBytes(), "ISO8859-1");
			} catch (UnsupportedEncodingException unsupportedencodingexception) {
			}
		return fileName;
	}
	public static int countStr(String str) {
		if (!StringUtil.isValid(str))
			return 0 ;
		return str.toCharArray().length;
	}
	public static void main(String args[]) {
		System.out.println(isDate("000010"));
		System.out.println(isNumber("000010"));
	}
	public static String htmlRemoveTag(String inputString) {
		if (inputString == null)
			return null;
		String htmlStr = inputString; // 含html标签的字符串
		String textStr = "";
		java.util.regex.Pattern p_script;
		java.util.regex.Matcher m_script;
		java.util.regex.Pattern p_style;
		java.util.regex.Matcher m_style;
		java.util.regex.Pattern p_html;
		java.util.regex.Matcher m_html;
		try {
			//定义script的正则表达式{或<script[^>]*?>[\\s\\S]*?<\\/script>
			String regEx_script = "<[\\s]*?script[^>]*?>[\\s\\S]*?<[\\s]*?\\/[\\s]*?script[\\s]*?>";
			//定义style的正则表达式{或]*?>[\\s\\S]*?<\\/style>
			String regEx_style = "<[\\s]*?style[^>]*?>[\\s\\S]*?<[\\s]*?\\/[\\s]*?style[\\s]*?>";
			String regEx_html = "<[^>]+>"; // 定义HTML标签的正则表达式
			p_script = Pattern.compile(regEx_script, Pattern.CASE_INSENSITIVE);
			m_script = p_script.matcher(htmlStr);
			htmlStr = m_script.replaceAll(""); // 过滤script标签
			p_style = Pattern.compile(regEx_style, Pattern.CASE_INSENSITIVE);
			m_style = p_style.matcher(htmlStr);
			htmlStr = m_style.replaceAll(""); // 过滤style标签
			p_html = Pattern.compile(regEx_html, Pattern.CASE_INSENSITIVE);
			m_html = p_html.matcher(htmlStr);
			htmlStr = m_html.replaceAll(""); // 过滤html标签
			textStr = htmlStr;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return textStr;// 返回文本字符串
	}
	/**
	 * 纬度lat 经度lon
	 * @param longt1
	 * @param lat1
	 * @param longt2
	 * @param lat2
	 * @return
	 */
	private final static double PI = 3.14159265358979323;// 圆周率
	private final static double R = 6371229;  // 地球的半径
	public static String getDistance(Double longt1, Double lat1, Double longt2, Double lat2) {
		String result = "";
		if (longt1 == null || lat1 == null || longt2 == null || lat2 == null) {
			return "0";
		}
		double x, y, distance;
		x = (longt2 - longt1) * PI * R * Math.cos(((lat1 + lat2) / 2) * PI / 180) / 180;
		y = (lat2 - lat1) * PI * R / 180;
		distance = Math.hypot(x, y);
		BigDecimal din = new BigDecimal(distance/1000);
		din = din.setScale(1, BigDecimal.ROUND_HALF_UP);
		result = din.toString()+"公里";
		return result;
	}
	/**
	 * @param str 原字符串
	 * @param sToFind 需要查找的字符串
	 * @return 返回在原字符串中sToFind出现的次数
	 */
	public static int countStr(String str,String sToFind) {
		int num = 0;
		while (str.contains(sToFind)) {
			str = str.substring(str.indexOf(sToFind) + sToFind.length());
			num ++;
		}
		return num;
	}
}
