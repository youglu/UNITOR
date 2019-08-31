package org.chuniter.core.kernel.api.orm;

import java.io.Serializable;
import java.sql.SQLException;

import org.chuniter.core.kernel.impl.orm.EntityMapping;
import org.chuniter.core.kernel.kernelunit.Param;

public interface ISQLProvider extends Serializable{
	
	final int TABLENOEXIST = 1;
	final int UNKNOWNCOLUMN = 2;
	
	String getPageSql(EntityMapping emp,int startIndex,int pageSize) throws Exception;
	String getPageSql(EntityMapping emp,Param param,int startIndex,int pageSize) throws Exception;
	String getPageSql(EntityMapping emp,String sql,int startIndex,int pageSize) throws Exception;
	int parseSqlExceptionErrorCode(SQLException sqle);
	Object appendPreparedValue(Object value); 
	String getPageSql(EntityMapping emp, int startIndex, int pageSize,
			String[] excludeFields) throws Exception;
	String getModifyColumnSql(String tableName,String cname,String ctype,Integer clength) ;
	
	/**
	 * 模根不同数据处理即将要执行的sql
	* @Description: TODO
	* @author youg continentlu@sina.com
	* @date 2018年1月6日 下午11:12:02 
	* @version V1.0
	 */
	String proSql(String sql);
}
