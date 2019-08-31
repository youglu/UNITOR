package org.chuniter.core.kernel.impl.orm;

import java.sql.SQLException;
import java.util.Map;

import org.chuniter.core.kernel.api.orm.ISQLProvider;
import org.chuniter.core.kernel.kernelunit.Param;
import org.chuniter.core.kernel.kernelunit.StringUtil;



public class MYSQLProvider extends BaseSQLProvider{
	//mysql
	//select * from t_order limit 5,10; #返回第6-15行数据
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -5087488730428080601L;
	public String getSpecifeldPageSql(EntityMapping emp,int startIndex,int pageSize) throws Exception{		
		return this.getPageSql(emp,emp.getParam(), startIndex, pageSize);
	}

	@Override
	public String getSpecifeldPageSql(EntityMapping emp, Param param, int startIndex,
			int pageSize) throws Exception {
		/*StringBuilder sb = new StringBuilder();
		sb.append(emp.getFindWithJoinSqlFormParam());
		if(sb.indexOf("where") == -1)
			sb.append(" where 1=1 ");
		if(null != param){
			Map<String,Object> sqlAndParamMap = (Map<String,Object>)param.getCriteria(sb);
			sb.append(sqlAndParamMap.get(EntityMapping.SQL));
		}
		sb.append(" limit ").append(startIndex).append(",").append(pageSize);
		emp.setParam(param);*/
		return this.getSpecifeldPageSql(emp, emp.getFindWithJoinSqlFormParam(),startIndex, pageSize);//sb.toString();
	}
	@Override
	public int parseSqlExceptionErrorCode(SQLException sqle){
		int errorCode = sqle.getErrorCode();
		if(sqle.getMessage().indexOf("doesn't exist") != -1)		
			return ISQLProvider.TABLENOEXIST;
		if(errorCode == 1146)		
			return ISQLProvider.TABLENOEXIST;	
		if(errorCode == 1054)
			return ISQLProvider.UNKNOWNCOLUMN;
		if(sqle.getMessage().indexOf("Unknown column") != -1)
			return ISQLProvider.UNKNOWNCOLUMN;
		return errorCode;
	}
	@Override
	public String getSpecifeldPageSql(EntityMapping emp,String selectsql, int startIndex,
			int pageSize) throws Exception {
		StringBuilder sb = new StringBuilder();
		sb.append(selectsql);
		String wherestr = " where 1=1 ";
		if(sb.indexOf("where") != -1||sb.indexOf("WHERE") != -1)
			wherestr = "";
		sb.append(wherestr);
		Param param = emp.getParam();
		if(null != param){
			Map<String,Object> sqlAndParamMap = (Map<String,Object>)param.getCriteria(sb);
			sb.append(sqlAndParamMap.get(EntityMapping.SQL)); 
			String groupsql = sqlAndParamMap.get(EntityMapping.SQLGROUP)==null?"":sqlAndParamMap.get(EntityMapping.SQLGROUP).toString();		
			sb.append(" ").append(groupsql);
			String ordersql = ((null != sqlAndParamMap)?sqlAndParamMap.get(EntityMapping.SQLORDER).toString():"").toLowerCase();
			String mastEntityName = emp.getEntityAlias().toLowerCase()+".";
			if(StringUtil.isValid(ordersql)&&ordersql.indexOf(mastEntityName) != -1){
				ordersql = ordersql.replaceAll(mastEntityName, "");
			}
			sb.append(" ").append(ordersql);
		}
		sb.append(" limit ").append(startIndex).append(",").append(pageSize);
		return sb.toString();
	}
	@Override
	public String getModifyColumnSql(String tableName,String cname,String ctype,Integer clength) { 
		//alter table undblzh.appointmentplan modify column id varchar(120); 
		StringBuilder sb = new StringBuilder("alter table ").append(tableName);
		sb.append(" modify column ");  
		sb.append(cname).append(" ").append(ctype).append("(").append(clength).append(") ");
		return sb.toString();
	}
	@Override
	public String proSql(String sql) {
		sql = sql.replaceAll("ISNULL", "IFNULL");
		return sql;
	}
}
