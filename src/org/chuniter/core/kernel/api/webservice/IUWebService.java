package org.chuniter.core.kernel.api.webservice;

import com.alibaba.fastjson.JSONObject;

/**
 * 远程服务接口
 * @author youg 2016-10-08 16:29
 *
 */
public interface IUWebService {

	/**
	 * 调用远程方法
	 * @author youg 2016-10-08 16:29
	 * @param methodName
	 * @param params
	 * @return
	 */
	JSONObject callRemote(String methodName,Object[] params);
}
