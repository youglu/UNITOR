package org.chuniter.core.kernel.impl.web;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.chuniter.core.kernel.api.web.ISessionManager;
import org.chuniter.core.kernel.api.web.Session;

import com.alibaba.fastjson.JSONObject;


public class SessionManager implements ISessionManager{

	{
		checkSession cs = new checkSession();
		Thread t = new Thread(cs);
		t.start(); 
	}
	
	private static Map<String,Session> sessionMap = new HashMap<String,Session>(); 
	public JSONObject getSession(String sessionId){
		Session s =  findSession(sessionId);
		JSONObject j = new JSONObject();
		j.put("s", s);
		return j.getJSONObject("s");
	}
	public static Session findSession(String sessionId){
		Session s = sessionMap.get(sessionId); 
		if(null == s){
			s = new Session();
			s.setId(sessionId);
			sessionMap.put(sessionId, s);
		}
		s.setLastModifyDate(new Date());
		return s;
	}
	public JSONObject userisInline(String loginid) {
		return null;
	}
	class checkSession implements Runnable{
		private List<String> needRemoveSids = new ArrayList<String>();
		@Override
		public void run() { 
			while(true){
				try{
					if(!sessionMap.isEmpty()){
						this.wait();
						needRemoveSids.clear();
						for(Entry<String,Session> sentry:sessionMap.entrySet()){
							if(!sentry.getValue().getIsActive()){
								sentry.getValue().invalid();
								needRemoveSids.add(sentry.getKey());
							}
						}
						for(String rsid:needRemoveSids){
							sessionMap.remove(rsid);
						}
						this.notify();
					}
					Thread.currentThread().sleep(30*60*1000);
				}catch(Exception e){e.printStackTrace();}
				
			}
		}
		
	}

	@Override
	public Integer getActiveSessions() { 
		return null;
	}
	@Override
	public List<JSONObject> getAtiveSessions() { 
		return null;
	}
	@Override
	public String forcelogout(String sessionId) {
		// TODO Auto-generated method stub
		return null;
	}
}
