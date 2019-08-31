package org.chuniter.core.kernel.api.web;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.Id;

import org.chuniter.core.kernel.model.BaseEntity;

public class Session extends BaseEntity{

	/**
	 * 
	 */
	private static final long serialVersionUID = -8802213193086234433L; 
	@Id
	private String id;
	private Boolean isActive;
	private Integer maxAliveTime = 30;//分钟
	
	private Map<String,Object> attribMap = new HashMap<String,Object>();
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public Boolean getIsActive() {
		if(null == this.getLastModifyDate())
			isActive = false;
		Long ct = new Date().getTime();
		if(ct - this.getLastModifyDate().getTime()>maxAliveTime*60*1000)
			isActive = false;
		else
			isActive = true;
		return isActive;
	}
	public void setIsActive(Boolean isActive) {
		this.isActive = isActive;
	} 
	
	public <T> Object getAttrib(String attrib){
		return attribMap.get(attrib);
	}
	
	public <T> void setAttrib(String attrib,T object){
		attribMap.put(attrib,object);
	}
	public void invalid(){
		this.attribMap.clear();
		this.attribMap = null;
	}
	public Integer getMaxAliveTime() {
		return maxAliveTime;
	}
	public void setMaxAliveTime(Integer maxAliveTime) {
		this.maxAliveTime = maxAliveTime;
	} 

}
