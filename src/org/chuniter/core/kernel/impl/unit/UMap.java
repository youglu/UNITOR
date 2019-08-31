package org.chuniter.core.kernel.impl.unit;

import java.util.Date;
import java.util.HashMap;

import org.chuniter.core.kernel.api.unit.IUMap;
import org.chuniter.core.kernel.kernelunit.DateUtil;
import org.chuniter.core.kernel.kernelunit.StringUtil;

public class UMap extends HashMap<String,Object> implements IUMap {
	private static final long serialVersionUID = 1922076666757597084L;

	public UMap(int num) {
		super(num);
	}

	@Override
	public String getString(String key) {
		if(!this.containsKey(key))
			return null;
		try{
			return this.get(key).toString();
		}catch(Exception e){}
		return null;
	}
	@Override
	public Date getDate(String key) {
		if(!this.containsKey(key))
			return null;
		try{
			Object o = this.get(key);
			if(o instanceof Date) {
				return (Date)o;
			}
			return DateUtil.stringToDate(o.toString());
		}catch(Exception e){}
		return null;
	}
	@Override
	public Float getFloat(String key) {
		if(!this.containsKey(key))
			return null;
		try{
			return Float.valueOf(this.get(key).toString());
		}catch(Exception e){}
		return null;
	}

	@Override
	public Double getDouble(String key) {
		if(!this.containsKey(key))
			return null;
		try{
			return Double.valueOf(this.get(key).toString());
		}catch(Exception e){}
		return null;
	}

	@Override
	public Integer getInteger(String key) {
		if(!this.containsKey(key))
			return null;
		try{
			return Integer.valueOf(this.get(key).toString());
		}catch(Exception e){}
		return null;
	}
	@Override
	public Boolean getBoolean(String key) {
		if(!this.containsKey(key))
			return null;
		try{
			Object b = this.get(key);
			if(null == b||!StringUtil.isValid(b.toString()))
				return false;
			if("1".equals(b.toString()))
				return true;
			if("0".equals(b.toString()))
				return false;
			return Boolean.valueOf(b.toString());
		}catch(Exception e){}
		return null;
	}
	@Override
	public <S> S g(String key) {
		if(!this.containsKey(key))
			return null;
		try{
			Object b = this.get(key);
			if(null == b)
				return null;
			return (S) b;
		}catch(Exception e){}
		return null;
	}

}
