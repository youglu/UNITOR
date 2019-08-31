package org.chuniter.core.kernel.api.security;

public interface USecurityService {
	/**
	 * 操作日志记录
	 * @author youg 2016-11-14 18:27
	 * @param userID
	 * @param modelName
	 * @param funcName
	 * @param clientInfo
	 * @param remartk
	 */
	void log(String userID,String modelName,String funcName,String clientInfo,String remartk);
	void log(String userID,String modelName,String funcName,String clientInfo,String remartk,String prodatas);
	/**
	 * 处理控制台显示
	 * @author youglu
	 * @time 2017-07-06 16:56
	 * @param s
	 */
	void console(String s);
}
