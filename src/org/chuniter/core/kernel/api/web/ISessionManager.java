package org.chuniter.core.kernel.api.web;

import java.util.List;

import com.alibaba.fastjson.JSONObject;

public abstract interface ISessionManager {

	JSONObject getSession(String sessionId);
	List<JSONObject> getAtiveSessions();
	
	Integer getActiveSessions();
	/**
	 * 查询指定用户是否在线
	 * @author youglu 2018-08-09 20:48
	 * @param loginid
	 * @return null:无法处理或不在线
	 */
	JSONObject userisInline(String loginid);
	/**
	 * 强制退出
	 * @author youglu 2018-08-13 23:44
	 * @param sessionId
	 * @return
	 */
	String forcelogout(String sessionId) ;
	
}
