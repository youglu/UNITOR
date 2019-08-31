package org.chuniter.core.kernel.impl.web;

import java.util.Map;

import org.apache.commons.logging.LogFactory;

public abstract class BaseAction extends BaseServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4351756960408825759L;

	protected Map<String, Object> session;
	//public static String SERVERURL = "http://www.isunitor.com:8081";//"http://113.105.152.188:8081";//"http://192.168.1.109:8080";//"http://120.84.201.18:8080";//

	public void setSession(Map<String, Object> session) {
		this.session = session;
	}

	public BaseAction() {
		log = LogFactory.getLog(getClass());
		log.info((new StringBuilder("\n\n====>正在实例化 action --> "))
				.append(getClass()).append("...\n").toString());
	}
	public BaseServlet createAction(){
		return this;
	}
	public abstract Object getModel();

	public abstract void clearModelValue();
}
