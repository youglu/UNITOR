package org.chuniter.core.kernel.api.authorization;


public interface IPowerAuthorization extends IAuthorization{

	/**
	 * 验证指定的角色是否有指定功能id与功能名的操作权限
	 * @param pwIdentiry
	 * @param pw
	 * @param roleName
	 * @return
	 */
	boolean isAuthoritedPower(String pwIdentity,String pw,String roleName);
	/**
	 * 验证当前已登录的用户是否有指定功能id与功能名的操作权限，
	 * 其中用户信息是从当前的线程中获得，已由用户处理单元加入
	 * @author youg
	 * @param pwIdentiry
	 * @param pw
	 * @return
	 */
	boolean isAuthoritedPower(String pwIdentity,String pname);
	/**
	 * 为指定的角色授予指定的功能
	 * @author youg
	 * @param pwIdentity
	 * @param pw
	 * @param roleName
	 */
	void permissionPower(String pwIdentity,String pw,String roleName);
	/**
	 * 把指定角色从指定功能中取消授权
	 * @author youg
	 * @param pwIdentity
	 * @param pw
	 * @param roleName
	 */
	void denialPower(String pwIdentity,String pw,String roleName);
}
