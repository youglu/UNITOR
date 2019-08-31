package org.chuniter.core.kernel.api.service;

import org.osgi.framework.BundleContext;

/**
 * 
* @Title: IGService.java 
* @Package org.chuniter.core.kernel.api.service 
* @Description: 通用服务调用接口
* @author youg continentlu@sina.com
* @date 2017年11月20日 下午4:04:09 
* @version V1.0
 */
public interface IGService {

/**
 * 
* @Description: 统一调用方法
* @author youg continentlu@sina.com
* @date 2017年11月20日 下午4:06:28 
* @version V1.0
 */
	public Object call(BundleContext ctx,String methodName,Object[] args)throws Exception;
	public Object call(String methodName,Object[] args)throws Exception;
}
