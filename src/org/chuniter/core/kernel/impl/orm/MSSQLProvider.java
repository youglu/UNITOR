package org.chuniter.core.kernel.impl.orm;

import java.sql.SQLException;
import java.util.Map;

import org.chuniter.core.kernel.api.orm.ISQLProvider;
import org.chuniter.core.kernel.kernelunit.Param;
import org.chuniter.core.kernel.kernelunit.StringUtil;


/**
 * 只提供2005版本的join查询功能
 * @author youg
 *
 */
public class MSSQLProvider extends BaseSQLProvider{
	//mssql 2005 select *   from(select   OrderId,   Freight,   ROW_NUMBER()   OVER(order   by   Freight)   as   row   from   Orders)   a where   row   between   20   and   30   
	
	//msql2000 SELECT TOP 页大小 * FROM table1 WHERE id NOT IN       (SELECT TOP 页大小*(页数-1) id FROM table1 ORDER BY id      )	ORDER BY id 
	public String getSpecifeldPageSql(EntityMapping emp,int startIndex,int pageSize) throws Exception{
		return this.getSpecifeldPageSql(emp,emp.getParam(), startIndex, pageSize);
	}

	@Override
	protected String getSpecifeldPageSql(EntityMapping emp, Param param,
			int startIndex, int pageSize) throws Exception {
		/*StringBuilder sb = new StringBuilder();
		Map<String,Object> sqlAndParamMap = null;
		if(null != param){
			sqlAndParamMap = (Map<String,Object>)param.getCriteria(sb);
			emp.setParam(param);
		}
		startIndex+=1;
		pageSize+=startIndex;
		String orderParams = ((null != sqlAndParamMap)?sqlAndParamMap.get(EntityMapping.SQLORDER).toString():null);
		if(!StringUtil.isValid(orderParams)){
			orderParams = " order by createDate desc ";
		} 
		sb.append("select * from(")
		  .append("select ROW_NUMBER() OVER(")
		  .append(orderParams)//order by ").append(emp.getMastAliasFormJsoin()).append(".createDate")/*.append(emp.getEntityId())*/
		  /*.append(") as row,* from ")
		  .append("(").append(emp.getFindWithJoinSqlFormParam());
		if(sb.indexOf("where") == -1)
			sb.append(" where 1=1 ");
		if(null != param){
			sb.append(sqlAndParamMap.get(EntityMapping.SQLCOUNTPARAM));
		}
		
		sb.append(")").append(emp.getEntityAlias())
		  .append(") a where row >=").append(startIndex).append(" and row<").append(pageSize)
		  .append(" ");
		String params = ((null != sqlAndParamMap)?sqlAndParamMap.get(EntityMapping.SQLORDER).toString():"").toLowerCase();
		String mastEntityName = emp.getEntityAlias().toLowerCase()+".";
		if(StringUtil.isValid(params)&&params.indexOf(mastEntityName) != -1){
			params = params.replaceAll(mastEntityName, "");
		}
		sb.append(params);*/
		return this.getSpecifeldPageSql(emp, emp.getFindWithJoinSqlFormParam(),startIndex, pageSize);//sb.toString();
	}
	public int parseSqlExceptionErrorCode(SQLException sqle){
		int errorCode = sqle.getErrorCode();//1146
		if(sqle.getMessage().toLowerCase().indexOf("invalid object name") != -1 )
			return ISQLProvider.TABLENOEXIST;
		if(errorCode == 208)
			return ISQLProvider.TABLENOEXIST;		
		return errorCode;
	}
	//sql 2000的分页，目前不支持join查询，只能单表查询
	public static String getSpecifeldPageSql(String tableName,String id, Param param,
			int startIndex, int pageSize) throws Exception {
		String paramsql = "";
		if(null != param){
			Map<String,Object> sqlAndParamMap = (Map<String,Object>)param.getCriteria(null);
			paramsql = sqlAndParamMap.get(EntityMapping.SQL).toString();
		}
		StringBuilder sb = new StringBuilder();
		if(startIndex<=0)
			startIndex = 1;
		else
			startIndex = startIndex/pageSize;
		sb.append("select top "+pageSize+" * from "+tableName+" where "+id+" not in (select top "+
				  ((startIndex-1)*pageSize)+" "+id+" from  "+tableName+" where 1=1 "+paramsql+") ");
		sb.append(paramsql);
		return sb.toString();
	}
	@Override
	protected String getSpecifeldPageSql(EntityMapping emp,String selectsql, int startIndex,
			int pageSize) throws Exception {
		StringBuilder sb = new StringBuilder();
		Map<String,Object> sqlAndParamMap = null;
		Param param = emp.getParam();
		if(null != param){
			sqlAndParamMap = (Map<String,Object>)param.getCriteria(sb);
			emp.setParam(param);
		}
		startIndex+=1;
		pageSize+=startIndex;
		String orderParams = ((null != sqlAndParamMap)?sqlAndParamMap.get(EntityMapping.SQLORDER).toString():null);
		if(!StringUtil.isValid(orderParams)){
			orderParams = " order by createDate desc ";
		} 
		sb.append("select * from(")
		  .append("select ROW_NUMBER() OVER(")
		  .append(orderParams)//order by ").append(emp.getMastAliasFormJsoin()).append(".createDate")/*.append(emp.getEntityId())*/
		  .append(") as row,* from ")
		  .append("(").append(selectsql);
		String wherestr = " where 1=1  ";
		if(sb.indexOf("where") != -1||sb.indexOf("WHERE") != -1)
			wherestr = " ";
		sb.append(wherestr);
		if(null != param){
			String paramsql = sqlAndParamMap.get(EntityMapping.SQLCOUNTPARAM)==null?"":sqlAndParamMap.get(EntityMapping.SQLCOUNTPARAM).toString();
			String groupandordersql = sqlAndParamMap.get(EntityMapping.SQLGROUP)==null?"":sqlAndParamMap.get(EntityMapping.SQLGROUP).toString();		
			sb.append(paramsql).append(" ").append(groupandordersql);
		}
		
		sb.append(")").append(emp.getEntityAlias())
		  .append(") a where row >=").append(startIndex).append(" and row<").append(pageSize)
		  .append(" ");
		String params = ((null != sqlAndParamMap)?sqlAndParamMap.get(EntityMapping.SQLORDER).toString():"").toLowerCase();
		String mastEntityName = emp.getEntityAlias().toLowerCase()+".";
		if(StringUtil.isValid(params)&&params.indexOf(mastEntityName) != -1){
			params = params.replaceAll(mastEntityName, "");
		}
		sb.append(params);
		return sb.toString();
	}

	@Override
	public String getModifyColumnSql(String tableName,String cname,String ctype,Integer clength) { 
		//alter table employeeentity alter  column  depId varchar(100); 
		StringBuilder sb = new StringBuilder("alter table ").append(tableName);
		sb.append(" alter column ");  
		sb.append(cname).append(" ").append(ctype).append("(").append(clength).append(")");
		return sb.toString();
	}
	@Override
	public String proSql(String sql) {
		return sql;
	}
	 
}
