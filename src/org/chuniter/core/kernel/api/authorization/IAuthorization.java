package org.chuniter.core.kernel.api.authorization;

import org.chuniter.core.kernel.model.BaseUserEntity;

public interface IAuthorization {

	final String SESSION_USER="sessionUser";
	final String SESSION_USERINFOMAP="sessionUserInfoMap";
	final String SESSION_USERENTITY="sessionUserEntity";
	final String SESSION_USERENTITY_JSON="sessionuserasjson";
	final String USER = "user";
	final String LOGIN_USER = "LOGIN_USER";
	
	String getUserName();	
	BaseUserEntity getGeneralUserEntity() throws Exception;
	BaseUserEntity getGeneralUserEntity(String token) throws Exception;
	void setGeneralUserEntity(String token,BaseUserEntity user) throws Exception;
	void removeToken(String token) throws Exception;
}
