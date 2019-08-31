package org.chuniter.core.kernel.api.service;

import java.util.List;

import org.chuniter.core.kernel.api.IGeneralService;
import org.chuniter.core.kernel.model.BaseUserEntity;
import org.chuniter.core.kernel.model.Extendable;
import org.chuniter.core.kernel.model.RolePersons;

import com.alibaba.fastjson.JSONObject;

public interface IUserManagerService<T extends Extendable> extends IGeneralService<T>{
	public List<RolePersons> findUserRoles(BaseUserEntity user) throws Exception;
	/**
	 * 创建普通用户,并且绑定普能用户角色，同时也会创建一个临时组织
	 * @author youglu 2018-11-10 09:13
	 * @param user
	 * @return
	 * @throws Exception
	 */
	String createNormalUser(JSONObject user)  throws Exception;
}
