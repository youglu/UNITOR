package org.chuniter.core.kernel.impl.orm;

import java.sql.Connection;
import java.sql.SQLException;

import org.chuniter.core.kernel.api.orm.ISQLProvider;
import org.chuniter.core.kernel.kernelunit.Param;
import org.chuniter.core.kernel.kernelunit.StringUtil;


public abstract class BaseSQLProvider implements ISQLProvider{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -4433733367540139662L;
	public String getPageSql(EntityMapping emp,int startIndex,int pageSize) throws Exception{
		if(startIndex<=0)
			startIndex = 1;
		startIndex=(startIndex-1)*pageSize;
		//pageSize*=startIndex;
		return this.getPageSql(emp,emp.getParam(),startIndex,pageSize);
	}
	public String getPageSql(EntityMapping emp,String sql,int startIndex,int pageSize) throws Exception{
		if(startIndex<=0)
			startIndex = 1;
		startIndex=(startIndex-1)*pageSize;
		return getSpecifeldPageSql(emp,sql,startIndex,pageSize);
	}
	public String getPageSql(EntityMapping emp,Param param,int startIndex,int pageSize) throws Exception{
		if(startIndex <=0)
			startIndex = 1;
		startIndex=(startIndex-1)*pageSize;
		//pageSize*=startIndex;
		emp.setParam(param);
		return getSpecifeldPageSql(emp,param,startIndex,pageSize);
	}	
	@Override
	public String appendPreparedValue(Object value) {
		return "?";
	}
	public static int[] fetchPageInfo(Connection c,int startIndex,int pageSize){
		try {
			if(startIndex <=0)
				startIndex = 1;
			startIndex=(startIndex-1)*pageSize;
			String dataBaseType = c.getMetaData().getDatabaseProductName();
			if(!StringUtil.isValid(dataBaseType))
				return new int[]{startIndex,pageSize};
			if("Microsoft SQL Server".equalsIgnoreCase(dataBaseType)){
				startIndex+=1;
				pageSize+=startIndex;
				return new int[]{startIndex,pageSize};
			}else if("MySQL".equalsIgnoreCase(dataBaseType)){
				return new int[]{startIndex,pageSize};
			} else if("Oracle".equalsIgnoreCase(dataBaseType)){
				startIndex+=1;
				pageSize+=startIndex;
				return new int[]{startIndex,pageSize};
			}
		} catch (SQLException e) { 
			e.printStackTrace();
		}
		
		return new int[]{startIndex,pageSize};
	}
	 
	public String getPageSql(EntityMapping emp, int startIndex, int pageSize,
			String[] excludeFields) throws Exception {
		throw new Exception("请实现");  
	}
	public String getDefModifyColumnSql(String tableName,String cname,String ctype,Integer clength) {
		String m = getModifyColumnSql(tableName,cname,ctype,clength);
		if(null == m){ }
		return m;
	}
	public String proSql(String sql) {
		return sql;
	}
	public abstract String getModifyColumnSql(String tableName,String cname,String ctype,Integer clength) ;
	protected abstract String getSpecifeldPageSql(EntityMapping emp,Param param,int startIndex,int pageSize) throws Exception;	
	protected abstract String getSpecifeldPageSql(EntityMapping emp,int startIndex,int pageSize) throws Exception;
	protected abstract String getSpecifeldPageSql(EntityMapping emp,String sql,int startIndex,int pageSize) throws Exception;	
}
