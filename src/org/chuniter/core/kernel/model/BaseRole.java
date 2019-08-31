package org.chuniter.core.kernel.model;


public class BaseRole extends BaseEntity{

	/**
	 * 
	 */
	private static final long serialVersionUID = -7760849566114761865L;

	private String id;
	
	private String roleId;
	  
	private transient String roleName;
	
	private transient Boolean  isAdmin; 
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getRoleId() {
		return roleId;
	}

	public void setRoleId(String roleId) {
		this.roleId = roleId;
	}
	
	public String getRoleName() {
		return roleName;
	}

	public void setRoleName(String roleName) {
		this.roleName = roleName;
	}

	public Boolean getIsAdmin() {
		return isAdmin;
	}

	public void setIsAdmin(Boolean isAdmin) {
		this.isAdmin = isAdmin;
	}
 
}
