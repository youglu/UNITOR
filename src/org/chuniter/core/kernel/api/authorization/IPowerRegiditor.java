package org.chuniter.core.kernel.api.authorization;

import java.util.Map;

public interface IPowerRegiditor {

	Byte ACTIVE=1;//是否激活(授予与拒绝都需要状态为该值作为前提)
	Byte DENIED = 0;//是否拒绝
	Byte PERMISSION = 2;//是否授予
	
	/**
	 * 以键值形式获得功能信息，该方法用于获得动态改变的功能信息
	 */
	Map<String,Object> getPowerData(String powerId);
	/**
	 * 注册功能
	 * @param pid
	 * @param pname
	 */
	void registerPower(String pid,String pname,String category,String moduleName);	
	void registerPower(String pid, String pname, String category,String moduleName, String iconurl);
	void registerPower(String pid, String pname, String category,String moduleName, String iconurl,String unitId);
	void registerPower(String pid, String pname, String category,String moduleName, String iconurl,Boolean isActive);
	/**
	 * 按功能编号注销功能
	 * @param pid
	 */
	void unRegisterPowerById(String pid);
	/**
	 * 按功能名注销功能
	 * @param pname
	 */
	void unRegisterPowerByName(String pname);

}
