package org.chuniter.core.kernel.model;

import java.util.Map;

import org.chuniter.core.kernel.annotation.EXColumn;
import org.chuniter.core.kernel.kernelunit.StringUtil;
 

/**
 * 角色与功能绑定类，用于把角色绑定到一个或多个功能上
 * @author youg
 * @time 2013-01-26
 *
 */
public class RolePowers extends BaseEntity {
 

	/**
	 * 
	 */
	private static final long serialVersionUID = 2181657570664304750L;

	private String id;
	
	@EXColumn(length=500)
	private String roleId;
	
	@EXColumn(length=500)
	private String powerId;
	
	@EXColumn(length=500)
	private String powerName;
	
	@EXColumn(length=1000)
	private String modelId;
	
	private transient String[] powerIds;
	
	private transient BaseEntity roleEntity;
	
	public transient Map<String,Object> powerEntityMap;

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

	public String[] getPowerIds() {
		return powerIds;
	}

	public void setPowerIds(String[] powerIds) {
		this.powerIds = powerIds;
	}

	public BaseEntity getRoleEntity() {
		return roleEntity;
	}

	public void setRoleEntity(BaseEntity roleEntity) {
		this.roleEntity = roleEntity;
	}

	public Map<String, Object> getPowerEntityMap() {
		return powerEntityMap;
	}

	public void setPowerEntityMap(Map<String, Object> powerEntityMap) {
		this.powerEntityMap = powerEntityMap;
	}

	public String getPowerId() {
		return powerId;
	}

	public void setPowerId(String powerId) {
		this.powerId = powerId;
		
		if(StringUtil.isValid(this.powerId))
		{	
			int dotindex = powerId.lastIndexOf(".") ;
			if(dotindex == -1)
				return;
			powerName = powerId.substring(dotindex+1);
			modelId = powerId.substring(0,dotindex);
		}
	}

	public String getPowerName() {
		if(!StringUtil.isValid(powerName)&&StringUtil.isValid(this.powerId)&&powerId.indexOf(".") != -1)
			powerName = powerId.substring(powerId.lastIndexOf(".")+1);
		return powerName;
	}

	public void setPowerName(String powerName) {
		this.powerName = powerName;
	}

	public String getModelId() {
		if(!StringUtil.isValid(modelId)&&StringUtil.isValid(this.powerId)&&powerId.indexOf(".") != -1)
			modelId = powerId.substring(0,powerId.lastIndexOf("."));
		return modelId;
	}

	public void setModelId(String modelId) {
		this.modelId = modelId;
	}
	
	
	
}
