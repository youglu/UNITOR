package org.chuniter.core.kernel.model;

import java.io.Serializable;

public class CustomField implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4472997051019900608L;
	
	private String name;
	private Object value;
	private String styles;
	private Float xof;
	private Float yof;
	private String color;
	private String remark;
	private String ftype;
	private String domain;
	private String displayName;
	
	public CustomField() { 
	}
	
	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public CustomField(String name, Object value) { 
		this.name =name ;
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getStyles() {
		return styles;
	}

	public void setStyles(String styles) {
		this.styles = styles;
	}

	public Float getXof() {
		return xof;
	}

	public void setXof(Float xof) {
		this.xof = xof;
	}

	public Float getYof() {
		return yof;
	}

	public void setYof(Float yof) {
		this.yof = yof;
	}

	public String getColor() {
		return color;
	}

	public void setColor(String color) {
		this.color = color;
	}

	public String getRemark() {
		return remark;
	}

	public void setRemark(String remark) {
		this.remark = remark;
	}

	public String getFtype() {
		return ftype;
	}

	public void setFtype(String ftype) {
		this.ftype = ftype;
	}

	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}

	public Object getValue() {
		return value;
	}

	public void setValue(Object value) {
		this.value = value;
	}
	
	

}
