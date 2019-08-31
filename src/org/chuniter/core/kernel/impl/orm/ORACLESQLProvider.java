package org.chuniter.core.kernel.impl.orm;

import java.sql.SQLException;
import java.util.Date;
import java.util.Map;

import org.chuniter.core.kernel.api.orm.ISQLProvider;
import org.chuniter.core.kernel.kernelunit.Param;
import org.chuniter.core.kernel.kernelunit.StringUtil;



public class ORACLESQLProvider extends BaseSQLProvider{
	//oracle
	//select * from (select rownum rn,tm.* from TestFModel tm) t where t.rn between 2 and 4 and fname like '%样%'
	//分页公式:ps=10,f(i)={si=(i*ps+1,ei=(i+1)*ps}
	
	protected String getSpecifeldPageSql(EntityMapping emp, Param param,int startIndex,int pageSize) throws Exception{
		/*startIndex+=1;r
		pageSize+=startIndex;
		Map<String,Object> sqlAndParamMap = null;
		if(null != param)
			sqlAndParamMap = (Map<String,Object>)param.getCriteria(null);
		emp.setParam(param);
		StringBuilder sb = new StringBuilder();		
		sb.append("  select * from (select rownum rn,")
		.append(" ets.* from ")
		.append("(").append(emp.getFindWithJoinSqlFormParam());
		if(sb.indexOf("where") == -1)
			sb.append(" where 1=1 ");
		if(null != param){
			sb.append(sqlAndParamMap.get(EntityMapping.SQLCOUNTPARAM));
		}
		sb.append(")  ets ");
		//if(null != sqlAndParamMap)
			//sb.append(sqlAndParamMap.get(EntityMapping.SQL));
		sb.append(" ) t ")
		.append(" where t.rn >=").append(startIndex).append(" and t.rn< ").append(pageSize);*/		
		return this.getSpecifeldPageSql(emp,emp.getFindWithJoinSqlFormParam(), startIndex, pageSize);//sb.toString();
	}

	@Override
	protected String getSpecifeldPageSql(EntityMapping emp,
			int startIndex, int pageSize) throws Exception {
		return this.getSpecifeldPageSql(emp, emp.getParam(),startIndex, pageSize);
	}
	public int parseSqlExceptionErrorCode(SQLException sqle){
		int errorCode = sqle.getErrorCode();
		if(errorCode == 942)
			return ISQLProvider.TABLENOEXIST;
		return errorCode;
	}
	@Override
	public String appendPreparedValue(Object value) {
		if(!(value instanceof ExtendedField))
			return "?";
		ExtendedField extfield = (ExtendedField)value;
		if(extfield.field.getType() == Date.class){
			return "TO_TIMESTAMP(?,'SYYYY-MM-DD HH24:MI:SS:FF6')";
		}
		return "?";
	}
	protected String getSpecifeldPageSql(EntityMapping emp,String selectSql,int startIndex,int pageSize) throws Exception{
		startIndex+=1;
		pageSize+=startIndex;
		Map<String,Object> sqlAndParamMap = null;
		Param param = emp.getParam();
		if(null != param)
			sqlAndParamMap = (Map<String,Object>)param.getCriteria(null);
		emp.setParam(param);
		StringBuilder sb = new StringBuilder();		
		sb.append("  select * from (select rownum rn,")
		.append(" ets.* from ")
		.append("(").append(selectSql);
		
		String wherestr = " where 1=1  ";
		if(sb.indexOf("where") != -1||sb.indexOf("WHERE") != -1)
			wherestr = " ";
		sb.append(wherestr);
		if(null != param){
			String paramsql = sqlAndParamMap.get(EntityMapping.SQLCOUNTPARAM)==null?"":sqlAndParamMap.get(EntityMapping.SQLCOUNTPARAM).toString();
			String groupandordersql = sqlAndParamMap.get(EntityMapping.SQLGROUP)==null?"":sqlAndParamMap.get(EntityMapping.SQLGROUP).toString();		
			sb.append(paramsql).append(" ").append(groupandordersql);
		}
		
		String orderParams = ((null != sqlAndParamMap)?sqlAndParamMap.get(EntityMapping.SQLORDER).toString():null);
		if(!StringUtil.isValid(orderParams)){
			orderParams = " order by "+emp.getMastAliasFormJsoin()+".createDate desc ";
		}
		sb.append(" ").append(orderParams).append(" ");
		sb.append(")  ets ");
		//if(null != sqlAndParamMap)
			//sb.append(sqlAndParamMap.get(EntityMapping.SQL));
		sb.append(" ) t ")
		.append(" where t.rn >=").append(startIndex).append(" and t.rn< ").append(pageSize);		
		return sb.toString();
	}
 
	public String getModifyColumnSql(String tableName,String cname,String ctype,Integer clength) { 
		//alter table test modify(name varchar(255));
		StringBuilder sb = new StringBuilder("alter table ").append(tableName);
		sb.append(" modify(");  
		sb.append(cname).append(" ").append(ctype).append("(").append(clength).append("))");
		return sb.toString(); 
	}
}
