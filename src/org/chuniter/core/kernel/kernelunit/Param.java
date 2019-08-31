// Decompiled by Jad v1.5.8e2. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://kpdus.tripod.com/jad.html
// Decompiler options: packimports(3) fieldsfirst ansi space 
// Source File Name:   Param.java

package org.chuniter.core.kernel.kernelunit;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.chuniter.core.kernel.api.orm.ISQLProvider;

public abstract class Param implements Serializable,Cloneable 
{

	/**
	 * 
	 */
	private static final long serialVersionUID = -5907155820022664970L;
	private static String implementClass = "org.chuniter.core.kernel.kernelunit.ParamLD";
	protected Boolean needAddDataOwner = false;
	protected String dataOwnerName = null;
	protected ISQLProvider sqlprovider;

	public Param()
	{
	}

	@SuppressWarnings("unchecked")
	public final static Param getInstance()
	{
		Param param = null;
		try
		{
			Class clazz = Class.forName(implementClass);
			param = (Param)clazz.newInstance();
		}
		catch (ClassNotFoundException e)
		{
			e.printStackTrace();
		}
		catch (InstantiationException e)
		{
			e.printStackTrace();
		}
		catch (IllegalAccessException e)
		{
			e.printStackTrace();
		}
		return param;
	}

	public abstract void addParam(String propertyName, Object value);

	public abstract void addParam(String propertyName, String condition, Object value);
	
	public abstract void orParam(String propertyName, String condition, Object value);

	public abstract void addParam(String propertyName, Object value, boolean isUrlDecode);

	public abstract void addParam(String propertyName, String condition, Object value, boolean isUrlDecode);

	public abstract Map<String,Object> getParam();

	public abstract boolean isEmpty();

	public abstract Object getCriteria(Object obj) throws Exception;

	public abstract Object getCountCriteria(Object obj) throws Exception;

	public abstract void clear();
	public abstract Map<Class<?>, Object[]> getJoinConditionMap(); 
	public abstract void setJoinConditionMap(Map<Class<?>, List<String>> joinConditionMap) ;
	public abstract void setJoinConditionMapArray(Map<Class<?>, Object[]> joinConditionMap) ;
	public abstract void setJoinConditionMap(Class<?> c, String[] fs) ;
	public abstract void setJoinConditionMap(String c, String[] fs) ;
	public abstract void delJoinCondition(Class<?> clas) ;
	public abstract void delParam(String s);

	public abstract void delParam(String s, String condition); 

	public abstract void setJoinCondition(Class<?> clas, String[] joinfeilds,String...joinType) ;
	public void setJoinCondition(Class<?> clas, String[] joinfeilds) {
		setJoinCondition(clas,joinfeilds,"");
	}
	public abstract String getReportSql() throws Exception;

	/**
	 * 这个地方声明与使用方法会误导，第一个是条件，但实际用时第一个却是属性。
	 * 所以不得不再加一个调换的判断
	 * @param condition
	 * @param key
	 * @return
	 */
	public boolean hasCondition(String condition, String...key) {
		if (!StringUtil.isValid(condition))
			return false; 
		for(Entry<String, Object> e:getParam().entrySet()){
			if(null != key&&key.length>0&&StringUtil.isValid(key[0])){
				if(e.getKey().trim().equals(key[0]+"_"+condition.trim()))
					return true;
				if(e.getKey().trim().equals(condition.trim()+"_"+key[0]))
					return true;
			}else if(e.getKey().trim().endsWith("_"+condition.trim()))
				return true;
		} 
		return false;
	}
 
	public boolean hasCondition(String key) { 
		if (!StringUtil.isValid(key))
			return false;  
		if(key.indexOf("_") != -1&&key.indexOf("_.") == -1) { 
			return hasCondition(key.substring(key.lastIndexOf("_")+1,key.length()),
					key.substring(0,key.lastIndexOf("_")) );
		}
		return hasCondition("==",key);
	}
	public Object getCondition(String key, String...condition) {
		if (!StringUtil.isValid(key))
			return null; 
		for(Entry<String, Object> e:getParam().entrySet()){
			if(null != condition&&condition.length>0&&StringUtil.isValid(condition[0])){
				if(e.getKey().trim().equals(key.trim()+"_"+condition[0]))
					return e.getValue();
			}else if(e.getKey().trim().startsWith(key.trim()+"_"))
				return e.getValue();
		} 
		return null;
	}
	public abstract boolean checkSQLInjection(String sqlStr) throws Exception;  
	public void setNeedAddDataOwner(Boolean needAddDataOwner) {
		this.needAddDataOwner = needAddDataOwner;
	}
	public Boolean getNeedAddDataOwner() {
		return needAddDataOwner;
	}

	public void setDataOwnerName(String dataOwnerName) {
		this.dataOwnerName = dataOwnerName;
	}

	public ISQLProvider getSqlprovider() {
		return sqlprovider;
	}

	public void setSqlprovider(ISQLProvider sqlprovider) {
		this.sqlprovider = sqlprovider;
	}
	public abstract Param cloneme() throws CloneNotSupportedException ;

	public abstract boolean hasJoinCondition(String fullJoinClassName) ;
	
}
