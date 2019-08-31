package org.chuniter.core.kernel.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.chuniter.core.kernel.annotation.EXColumn;
import org.chuniter.core.kernel.annotation.UEntity;
import org.chuniter.core.kernel.api.IGeneralService;
import org.chuniter.core.kernel.api.unit.IUMap;
import org.chuniter.core.kernel.impl.web.BaseAction;
import org.chuniter.core.kernel.kernelunit.Param;
import org.chuniter.core.kernel.kernelunit.StringUtil;


@UEntity(title="系统用户",uniokey= {"loginId"})
public class BaseUserEntity extends BaseEntity{

	/**
	 * 
	 */
	private static final long serialVersionUID = -5603623765331210559L;
	
	//@Id
	//@GeneratedValue(generator = "ud")
	//@GenericGenerator(name = "ud", strategy = "uuid")
	private String id;
	
	@EXColumn(title="用户名",index=1)  
	protected String name; 
	@EXColumn(title = "性别", utype = "sex")
	protected Integer sex;
	
	protected Integer age;
	@EXColumn(title = "身份证号")
	protected String idcard; // 身份证号 
	 
	protected String passw;	
	protected Boolean isAdmin;
	protected transient String[] roleIds;
	protected transient String[] roleNames;
	protected String roleIdsStr;
	
	protected String email;
	protected String work;
	@EXColumn(title="地址")
	protected String address;
	
	/**
	 * 状态
	 */
	protected Integer state;
	protected Integer orgType;
	
	/**
	 * 用户类型
	 */
	protected Integer userType = GEERALUSER;
	
	/**
	 * 登录账号
	 */
	@EXColumn(title="登录账号",index=1)
	protected String loginId;
	public transient List<RolePersons> roles;
	
	@EXColumn(title="用户相片")
	private String headimgPath;
	
	@EXColumn(title="手机号码")
	protected String mobile;
	
	private Date lastLoginTime;
	private Date lastLogoutTime;
	
	@EXColumn(title="积分")
	private Integer moneys;
	
	protected String userid; 
	//平台管理员
	public final static Integer SYSUSER = 1;
	//企业管理员用户
	public final static Integer ENTUSER = 2;
	//普通用户
	public final static Integer GEERALUSER = 3;
 
	public String getUserid() {
		return userid;
	}

	public void setUserid(String userid) {
		this.userid = userid;
	}
	public String getPassw() {
		return passw;
	}
	public void setPassw(String passw) {
		this.passw = passw;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public Integer getSex() {
		return sex;
	}
	public void setSex(Integer sex) {
		this.sex = sex;
	}
	public Integer getAge() {
		return age;
	}
	public void setAge(Integer age) {
		this.age = age;
	}
	 
	public String getIdcard() {
		return idcard;
	}
	public void setIdcard(String idcard) {
		this.idcard = idcard;
	} 
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public Boolean getIsAdmin() {
		return isAdmin==null?false:isAdmin;
	}
	public void setIsAdmin(Boolean isAdmin) {
		this.isAdmin = isAdmin;
	}
	public void setroles(List<RolePersons> roles){
		this.roles = roles;
		this.getRoleIds();
		getRoleNames();
	}
	public String[] getRoleIds() {
		if(null == this.roleIds || this.roleIds.length <= 0)
			if(null != this.roles&&!this.roles.isEmpty()){
				roleIds = new String[roles.size()];
				for(int i=0;i<this.roles.size();i++){
					roleIds[i] = roles.get(i).getRoleId();
				}
			}
		if((null == roleIds||roleIds.length<=0)&&StringUtil.isValid(this.roleIdsStr))
			roleIds = roleIdsStr.split(",");
		return roleIds;
	}
	public String[] getRoleNames() {
		if(null == this.roleNames || this.roleNames.length <= 0)
			if(null != this.roles&&!this.roles.isEmpty()){
				roleNames = new String[roles.size()];
				for(int i=0;i<this.roles.size();i++){
					roleNames[i] = roles.get(i).getRoleName();
				}
			}
		return roleNames;
	}
/*	public void refRoleId() { 
		if(null != this.roles&&!this.roles.isEmpty()){
			roleIds = new String[roles.size()];
			for(int i=0;i<this.roles.size();i++){
				roleIds[i] = roles.get(i).getRoleId();
			}
		}
	}
	public void refRoleNames() { 
		if(null != this.roles&&!this.roles.isEmpty()){
			roleNames = new String[roles.size()];
			for(int i=0;i<this.roles.size();i++){
				roleNames[i] = roles.get(i).getRoleName();
			}
		}
	}*/
	public Boolean isRole(String roleName) {
		if(null == this.roleNames || this.roleNames.length <= 0)
			return false;
				for(String r:roleNames){
					if(r.equals(roleName))
						return true;
				} 
		return false;
	}
	 
	public void addRole(String roleId) {
		RolePersons newRole = new RolePersons();
		newRole.setRoleId(roleId);
		newRole.setPersonId(this.getIdcard());
		if(null != roles)
			roles.add(newRole);
		roleNames = null;
		roleIds = null;
	}
	public void removeRole(String roleId) {
		
		/*if(null == this.roleIds || this.roleIds.length <= 0){
			if(null != this.roles&&!this.roles.isEmpty()){
				roleIds = new String[roles.size()];
				for(int i=0;i<this.roles.size();i++){
					roleIds[i] = roles.get(i).getRoleId();
				}
			}
		}else{
			String[] newRoles = new String[roleIds.length-1];
			roleIds = new String[roleIds.length+1];
			for(int i=0;i<this.roleIds.length;i++){
				newRoles[i] = roleIds[i];
			}
			newRoles[newRoles.length-1] = roleId;
			roleIds = newRoles;
		}*/
		int removeIndex = -1;
		if(null == roles)
			return;
		for(int i=0;i<this.roles.size();i++){
			if(roles.get(i).getRoleId().equals(roleId)){
				removeIndex = i; 
				break;
			}
		}
		if(removeIndex != -1){
			roles.remove(removeIndex); 
			//refRoleNames();
			roleNames = null;
			roleIds = null;
		}
	}	
	public void setRoleIds(String[] roleIds) {
		this.roleIds = roleIds;
	} 
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public String getWork() {
		return work;
	}
	public void setWork(String work) {
		this.work = work;
	}
	public String getAddress() {
		return address;
	}
	public void setAddress(String address) {
		this.address = address;
	}
	
	public String getRoleIdsStr() {
		return roleIdsStr;
	}
	public void setRoleIdsStr(String roleIdsStr) {
		this.roleIdsStr = roleIdsStr;
	}
	
	public Integer getState() {
		return state;
	}
	public void setState(Integer state) {
		this.state = state;
	}
	public Integer getOrgType() {
		return orgType;
	}
	public void setOrgType(Integer orgType) {
		this.orgType = orgType;
	}
	
	public Integer getUserType() {
		return userType;
	}
	public void setUserType(Integer userType) {
		this.userType = userType;
	}
	
	public String getLoginId() {
		return loginId;
	}
	public void setLoginId(String loginId) {
		this.loginId = loginId;
	}
	/**
	 * 同步角色，从指定的角色列表与人员列表，其中人员列表中的id与当前匹配则同步，否则不处理
	 * @param roleIds
	 * @param personIds
	 * @param isAdd 是否是添加,true:添加角色，false:删除角色
	 */
	public void syncRole(String[] roleIds,String[] personIds,boolean isAdd){
		boolean hasBinded = false;
		String[] tempRoles;
		for(String personId:personIds){
			if(!(personId.equals(id)))
				continue;
			for(String roleId:roleIds){	
				hasBinded = false;
				tempRoles = getRoleIds();
				if(null == tempRoles||tempRoles.length<=0)
					continue;
				for(String hasBindRoleId:tempRoles){
					if(hasBindRoleId.equals(roleId)){
						hasBinded = true;
						break;
					}
				}
				//如果未绑定，则看是否是添加操作
				if(!hasBinded){
					if(isAdd)
						addRole(roleId);
				}else{
					if(!isAdd)
						removeRole(roleId);
				}
			}			
		}
	}
	
	public String getHeadimgPathUrl() {
	     if (StringUtil.isValid(headimgPath))
	    	 return (BaseAction.PHOTOURL + headimgPath);
	     return "";
	}
	public String getHeadimgPath() {
		return headimgPath;
	}
	public void setHeadimgPath(String headimgPath) {
		this.headimgPath = headimgPath;
	}
	public Date getLastLoginTime() {
		return lastLoginTime;
	}
	public void setLastLoginTime(Date lastLoginTime) {
		this.lastLoginTime = lastLoginTime;
	}
	public Date getLastLogoutTime() {
		return lastLogoutTime;
	}
	public void setLastLogoutTime(Date lastLogoutTime) {
		this.lastLogoutTime = lastLogoutTime;
	}
	public String getMobile() {
		return mobile;
	}
	public void setMobile(String mobile) {
		this.mobile = mobile;
	} 
 
	public List<RolePersons> findUserRoles(IGeneralService<?> service) throws Exception {
		try{
			String sql3 = " select rp.id,rp.personId,rp.roleid,re.name,re.isAdmin from RolePersons rp  "
					+ " left join roleentity re "
					+ "  on re.id = rp.roleid  "
					+ "  where personid='"+getId()+"'  "; 
			Param p = Param.getInstance();
			p.addParam("personid", getId());
			List<Map<String,Object>> mls = service.hisql(sql3, p,false); 
			if(null == mls||mls.isEmpty())
				return null;
			roles = new ArrayList<RolePersons>(mls.size());
			RolePersons roleperson = null;
			roleNames = new String[mls.size()];
			roleIds = new String[mls.size()];
			int ml = mls.size();
			roleIdsStr = "";
			for(int i=0;i<ml;i++){
				IUMap um = (IUMap)mls.get(i);
				roleperson = new RolePersons();
				roleperson.setId(um.getString("id"));
				roleperson.setRoleId(um.getString("roleid"));
				roleperson.setPersonId(um.getString("personId"));
				roleperson.setRoleName(um.getString("name"));
				roleperson.setIsAdmin(um.getBoolean("isAdmin"));
				if(roleperson.getIsAdmin())
					setIsAdmin(true);
				roles.add(roleperson);
				roleNames[i] = um.getString("name");
				roleIds[i] = um.getString("roleid");
				roleIdsStr+=roleIds[i]+",";
			}  
			if(roleIdsStr.endsWith(","))
				roleIdsStr = roleIdsStr.substring(0, roleIdsStr.length()-1);
			return roles;
		}catch(Exception e){
			e.printStackTrace();
		}
		return null;
	}
	public Integer getMoneys() {
		return moneys;
	}
	public void setMoneys(Integer moneys) {
		this.moneys = moneys;
	}  
	
}
