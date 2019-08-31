package org.chuniter.core.kernel.impl.web;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

 
public class CookieUtil {

    private final static String COOKIE_DOMAIN = ".ehr-cloud.com";
    public final static String COOKIE_NAME = "logintoken";
	protected static Log log  = LogFactory.getLog(CookieUtil.class);
	public static final int COOKIE_MAX_AGE = 7 * 24 * 3600;
    public static final int COOKIE_HALF_HOUR = 30 * 60;
       
    /**
     * 根据Cookie名称得到Cookie对象，不存在该对象则返回Null
     * 
     * @param request
     * @param name
     * @return
     */ 
    public static Cookie getCookie(HttpServletRequest request, String name) { 
        Cookie[] cookies = request.getCookies(); 
        if (null == cookies||cookies.length<=0) { 
            return null; 
        } 
        Cookie cookie = null; 
        for (Cookie c : cookies) {
            if (name.equals(c.getName())) { 
                cookie = c; 
                break; 
            } 
        } 
        return cookie; 
    }
     
    /**
     * 根据Cookie名称直接得到Cookie值
     * 
     * @param request
     * @param name
     * @return
     */ 
    public static String getCookieValue(HttpServletRequest request, String name) { 
        Cookie cookie = getCookie(request, name); 
        if(cookie != null){
            return cookie.getValue();
        }
        return null; 
    }
     
    /**
     * 移除cookie
     * @param request
     * @param response
     * @param name 这个是名称，不是值
     */
    public static void removeCookie(HttpServletRequest request, 
            HttpServletResponse response, String name) { 
        if (null == name) { 
            return; 
        } 
        Cookie cookie = getCookie(request, name); 
        if(null != cookie){ 
            cookie.setPath("/"); 
            cookie.setValue(""); 
            cookie.setMaxAge(0); 
            response.addCookie(cookie);
        } 
    }
     
    /**
     * 添加一条新的Cookie，可以指定过期时间(单位：秒)
     * 
     * @param response
     * @param name
     * @param value
     * @param maxValue
     */ 
    public static void setCookie(HttpServletResponse response, String name, 
            String value, int maxValue) { 
        if (null == name||"".equals(name)) { 
            return; 
        } 
        if (null == value) { 
            value = ""; 
        } 
        Cookie cookie = new Cookie(name, value); 
        cookie.setPath("/"); 
        if (maxValue != 0) { 
            cookie.setMaxAge(maxValue); 
        } else { 
            cookie.setMaxAge(COOKIE_HALF_HOUR); 
        } 
        response.addCookie(cookie);
        try {
            response.flushBuffer();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
     
    /**
     * 添加一条新的Cookie，默认30分钟过期时间
     * 
     * @param response
     * @param name
     * @param value
     */ 
    public static void setCookie(HttpServletResponse response, String name, 
            String value) { 
        setCookie(response, name, value, COOKIE_HALF_HOUR); 
    } 
  
    /**
     * 将cookie封装到Map里面
     * @param request
     * @return
     */
    public static Map<String,Cookie> getCookieMap(HttpServletRequest request){ 
        Map<String,Cookie> cookieMap = new HashMap<String,Cookie>();
        Cookie[] cookies = request.getCookies();
        if(null != cookies&&cookies.length>0){
            for(Cookie cookie : cookies){
                cookieMap.put(cookie.getName(), cookie);
            }
        }
        return cookieMap;
    }
    public static String readLoginToken(HttpServletRequest request){
        Cookie[] cks = request.getCookies();
        if(cks != null){
            for(Cookie ck : cks){ 
                if(StringUtils.equals(ck.getName(),COOKIE_NAME)){ 
                    return ck.getValue();
                }
            }
        }
        return null;
    }


    public static void writeLoginToken(HttpServletResponse response,String token){
        Cookie ck = new Cookie(COOKIE_NAME,token);
        ck.setDomain(COOKIE_DOMAIN);
        ck.setPath("/");//代表设置在根目录
        ck.setHttpOnly(true);
        //单位是秒。
        //如果这个maxage不设置的话，cookie就不会写入硬盘，而是写在内存。只在当前页面有效。
        ck.setMaxAge(60 * 60 * 24 * 365);//如果是-1，代表永久 
        response.addCookie(ck);
    }


    public static void delLoginToken(HttpServletRequest request,HttpServletResponse response){
        Cookie[] cks = request.getCookies();
        if(cks != null){
            for(Cookie ck : cks){
                if(StringUtils.equals(ck.getName(),COOKIE_NAME)){
                    ck.setDomain(COOKIE_DOMAIN);
                    ck.setPath("/");
                    ck.setMaxAge(0);//设置成0，代表删除此cookie。 
                    response.addCookie(ck);
                    return;
                }
            }
        }
    }







}
