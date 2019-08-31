package org.chuniter.core.kernel.kernelunit;

import java.io.Serializable;

/**
 * 用于携带Param 实现类所需的 属性名，查询类型，值
 * @author Administrator
 *
 */
public class ParamCarrier implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4228465764041929506L;
	/**
	 * 查询属性名
	 */
	private String propertyName;
	/**
	 * 查询类型
	 */
	private String condition;
	/**
	 * 查询值
	 */
	private Object value;
	
	/**
	 * 查询值数据类型
	 * @return
	 */
	private String valueType;
	
	public String getValueType() {
		return valueType;
	}
	public void setValueType(String valueType) {
		this.valueType = valueType;
	}
	public String getPropertyName() {
		return propertyName;
	}
	public void setPropertyName(String propertyName) {
		this.propertyName = propertyName;
	}
	public String getCondition() {
		return condition;
	}
	public void setCondition(String condition) {
		this.condition = condition;
	}
	public Object getValue() {
		return value;
	}
	public void setValue(Object value) {
		this.value = value;
	}
	public Param fetchParam(Param p){
		proValueByTargetType();
		if(null==this.value||!StringUtil.isValid(this.value.toString())){			
			return p;
		}
		p.addParam(this.propertyName,this.condition,this.value);
		return p;
	}
	private Object proValueByTargetType(){
		if(null == this.valueType)
			return value;
		if(valueType.toLowerCase().equals("double")){
			value = new Double(value.toString());
		}
		if(valueType.toLowerCase().equals("float")){
			value = new Float(value.toString());
		}		
		return value;
	}

}
