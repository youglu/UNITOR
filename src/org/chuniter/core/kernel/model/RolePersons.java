package org.chuniter.core.kernel.model;


public class RolePersons extends BaseEntity{

	/**
	 * 
	 */
	private static final long serialVersionUID = -7760849566114761865L;

	private String id;
	
	private String roleId;
	
	private String personId; 
	
	private transient String roleName;
	
	private transient Boolean  isAdmin;
	
	private transient BaseEntity role;
	
	private transient BaseEntity person;

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

	public String getPersonId() {
		return personId;
	}

	public void setPersonId(String personId) {
		this.personId = personId;
	}

	public BaseEntity getRole() {
		return role;
	}

	public void setRole(BaseEntity role) {
		this.role = role;
	}

	public BaseEntity getPerson() {
		return person;
	}

	public void setPerson(BaseEntity person) {
		this.person = person;
	}

	public String getRoleName() {
		return roleName;
	}

	public void setRoleName(String roleName) {
		this.roleName = roleName;
	}

	public Boolean getIsAdmin() {
		return null == isAdmin?false:isAdmin;
	}

	public void setIsAdmin(Boolean isAdmin) {
		this.isAdmin = isAdmin;
	}
 
}
