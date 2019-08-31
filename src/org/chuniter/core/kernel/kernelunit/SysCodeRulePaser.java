package org.chuniter.core.kernel.kernelunit;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.chuniter.core.kernel.api.IGeneralService;
import org.chuniter.core.kernel.api.unit.ICodeRuleParse;
import org.chuniter.core.kernel.impl.unit.UMap;
import org.chuniter.core.kernel.model.BaseEntity;
 

/**
 * 
* @Title: CodeRulePaser.java 
* @Package org.chuniter.core.kernel.kernelunit 
* @Description: 解析编码规则，并产生新的编码
* @author youg  
* @date Aug 2, 2018 5:51:04 PM 
* @version V1.0
 */
public class SysCodeRulePaser implements ICodeRuleParse{
	/**
	 * 根据参数所获得的编码规则产生编码
	 * @param service
	 * @param entityClass
	 * @param orgId
	 * @return 
	 */
	public  String parseCodeRule(IGeneralService<?> service,String entityClass,String orgId) {
		return sysPparseCodeRule(service,entityClass,orgId);
	}
	public  static String sysPparseCodeRule(IGeneralService<?> service,String entityClass,String orgId) {
		Param p = Param.getInstance();
		p.addParam("modelId", entityClass);
		if(StringUtil.isValid(orgId))
			p.addParam("orgids","like",orgId);
		else
			p.addParam("orgids","isnull",true);
		p.addParam("estate", "!=",BaseEntity.DISABLED);
		try {
			//List<Map<String,Object>> ms = service.hisql("select * from SYSCodeRuleDetail a where "
					//+ "exists(select 1 from SYSCodeRule b where a.parentid = b.id and orgids like '%"+orgId+"%' and entityClass='"+entityClass+"' and b.estate =0) "
					//+ "", p); 
			List<Map<String,Object>> ms = service.hisql("select * from SYSCodeRule  where 1=1",p);
			if(null == ms||ms.isEmpty())
				return null;//service.fetchNo("E", Class.forName(entityClass));
			//如果有多个，取第一个
			Map<String,Object> m = ms.get(0);
			p.clear();
			p.addParam("parentId", m.get("id").toString());
			p.addParam("createDate", "order","asc");
			ms = service.hisql("select * from SYSCodeRuleDetail  where 1=1",p);
			if(null == ms||ms.isEmpty())
				return null;//service.fetchNo("E", Class.forName(entityClass));
			StringBuilder code = new StringBuilder();
			String tableName = entityClass.substring(entityClass.lastIndexOf(".")+1, entityClass.length());
			String orgEcode = fetchOrgEcodeById(service,orgId);
			UMap um = null; 
			String dw = ms.get(0).get(BaseEntity.DATAOWNER).toString();
			String fromfdname = BaseEntity.ECODE;
			if(tableName.equalsIgnoreCase("employeeentity"))
				fromfdname = "empno";
			for(Map<String,Object> sm:ms) {
				um = (UMap)sm;
				String codeAttribute = um.getString("codeAttribute");
				String hformat = um.getString("hformat");
				Integer hlength = um.getInteger("hlength");
				 
				int ctype = Integer.valueOf(codeAttribute);
				//"1:固定值","2:系统日期","3:顺序号","4:组织编号"
				switch(ctype) {
					case 1:
						code.append(hformat);
						break;
					case 2:
						if(!StringUtil.isValid(hformat))
							hformat = "yyyyMMdd";
						code.append(DateUtil.dateToString(new Date(),hformat));
						break;
					case 3:
						p.clear();
						UMap maxm = service.hisqlOne("select max("+BaseEntity.MEINDEX+") maxindex from "+tableName+" where 1=1 ", p);
						Integer maxindex = maxm.getInteger("maxindex");
						if(null == maxindex)
							maxindex = 0;
						else {
							p.clear();
							//FIXME 这个地方使用MSSQL的LEN函数，跨数据库需处理。根据当前位数检查是否已有此位置的序号
							//先处理MEINDEX有重复的，可能是以前的旧数据或是导入产生的
							int cnolengh = code.length()+hlength;
							p.addParam(BaseEntity.DATAOWNER, dw);
							List<Map<String,Object>> ms2 = service.hisql("select id from "+tableName+" where  meIndex=(select max(meIndex) from "+tableName+"  where len("+fromfdname+")="+cnolengh+" and dataOwner='"+dw+"') and  len("+fromfdname+")= "+cnolengh, p);
							if(null != ms2&&ms2.size()>1) {
								for(Map<String,Object> m2:ms2) {
									service.hisql("update "+tableName+" set meindex=(select max(meindex) from "+tableName+" where dataOwner='"+dw+"')+1  where id='"+m2.get("id")+"'",p);
								}
							}
							String ec = null;
							p.addParam(BaseEntity.DATAOWNER, dw);
							maxm = service.hisqlOne("select "+fromfdname+" from "+tableName+" where  meIndex=(select max(meIndex) from "+tableName+" where len("+fromfdname+")="+cnolengh+" and dataOwner='"+dw+"') and  len("+fromfdname+")= "+cnolengh, p);
							if(null != maxm&&!maxm.isEmpty()) {
								ec = maxm.getString(fromfdname);
							}
							//System.out.println("tmd:"+ec);
							if(!StringUtil.isValid(ec)) {
								p.clear();
								//根据已生成的编号与当前规则，检查是否需要重置编码
								maxm = service.hisqlOne("select "+fromfdname+" from "+tableName+" where "+BaseEntity.MEINDEX+"= "+maxindex, p);
								ec = maxm.getString(fromfdname);
							}
							//System.out.println("tmd2:"+ec);
							if(StringUtil.isValid(ec)) {
								//获得当前顺序号
								String clstr = ec;
								if(ec.length()>=code.length()+hlength)
									clstr = clstr.substring(code.length(), code.length()+hlength);
								else
									clstr = clstr.substring(code.length(), ec.length());
								//检查是否需要重置序号
								if(needResetNo(ms,ec,orgEcode))
									maxindex = 0;
								else {
									maxindex = Integer.valueOf(clstr);
								}
							}
						}
						maxindex+=1;
						if(null == hlength||hlength<=0)
							hlength = 3;
						for(int i=0,j=hlength-String.valueOf(maxindex).length();i<j;i++)
							code.append("0");
						code.append(maxindex);
						break;	
					case 4:
						if(!StringUtil.isValid(orgId))
							break; 
						if(StringUtil.isValid(orgEcode))
							code.append(orgEcode); 
						break;	
				}
			}
			return code.toString();
		}catch(Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	private static String fetchOrgEcodeById(IGeneralService<?> service,String orgId) throws Exception {
		if(!StringUtil.isValid(orgId))
			return null;
		Param p = Param.getInstance();
		p.addParam("id", orgId);
		UMap orgum = service.hisqlOne("select ecode,depCode,"+BaseEntity.MEINDEX+" from OrgEntity where 1=1 ", p);
		String orgEcode = orgum.getString("ecode");
		boolean isok = true;
		if(!StringUtil.isValid(orgEcode)) {
			orgEcode = orgum.getString("depCode");
			isok = false;
		}
		if(!StringUtil.isValid(orgEcode))
			orgEcode = orgum.getString(BaseEntity.MEINDEX);
		if(!StringUtil.isValid(orgEcode)) 
			return null;
		if(!isok) {
			p.clear();
			p.addParam("id", orgId);
			service.hisql("update OrgEntity set ecode='"+orgEcode+"' where id='"+orgId+"'", p);
		} 
		return orgEcode;
	}
	private static boolean needResetNo(List<Map<String,Object>> ms,String checkstr,String orgEcode) {
		//组装此编码格式的正则表达式
		StringBuilder regularstr = new StringBuilder();
		String datestr = "((\\d{4})|(\\d{2})|(\\d{4}\\d{2})|(\\d{4}\\d{2}\\d{2}))"; 
		String orgreg = "(\\w*)";
		String noreg = "(\\d{6})";
		
		StringBuilder checkv = new StringBuilder();  
		String clstr = checkstr;
		
		UMap um = null;
		for(Map<String,Object> sm:ms) {  
			um = (UMap)sm;
			String codeAttribute = um.getString("codeAttribute");
			String hformat = um.getString("hformat");
			Integer hlength = um.getInteger("hlength");
			int ctype = Integer.valueOf(codeAttribute);
			//"1:固定值","2:系统日期","3:顺序号","4:组织编号"
			switch(ctype) {
				case 1:
					regularstr.append(hformat);
					checkv.append(hformat);
					break;
				case 2: 
					regularstr.append(datestr);
					checkv.append(DateUtil.dateToString(new Date(),hformat));
					break;
				case 3:  
					noreg = "(\\d{"+hlength+"})"; 
					regularstr.append(noreg); 
					if(clstr.length()>=checkv.length()+hlength)
						clstr = clstr.substring(0,checkv.length())+clstr.substring(checkv.length()+hlength,clstr.length());
					break;	
				case 4: 
					if(!StringUtil.isValid(orgreg))
						break;
					regularstr.append(orgreg); 
					checkv.append(orgEcode);
					break;	
			}
		}
		//以下规则为:固定值+日期+序号+固定值+序号+日期+组织
		Pattern p = Pattern.compile(regularstr.toString()); 
		boolean ismach = p.matcher(checkstr).matches(); 
		//首选检查格式是否匹配
		if(!ismach)
			return true;
		//再检查是在数据上是否需要重置
		if(!clstr.equals(checkv.toString()))
			return true;
		System.out.println(clstr+"\n"+checkv+"\n 解析后是否匹配:"+(clstr.equals(checkv.toString()))+"\n----- \n"+checkstr+"\n"+regularstr.toString()+"\n是否匹配:"+ismach);
		return false;
	}
	public static void main(String[] args) {
		String checkstr = "hn201808063123gw11542018soe";
		
		List<Map<String,Object>> ms = new ArrayList<Map<String,Object>>();
		UMap u = new UMap(3);
		u.put("codeAttribute", 1);
		u.put("hformat", "hn");
		ms.add(u);
		
		u = new UMap(3);
		u.put("codeAttribute", 2);
		u.put("hformat", "yyyyMMdd");
		ms.add(u);
		
		u = new UMap(3);
		u.put("codeAttribute",3);
		u.put("hlength", 4);
		ms.add(u);
		
		u = new UMap(3);
		u.put("codeAttribute",1);
		u.put("hformat", "gw");
		ms.add(u);
		
		u = new UMap(3);
		u.put("codeAttribute",3);
		u.put("hlength", 4);
		ms.add(u);
		
		u = new UMap(3);
		u.put("codeAttribute", 2);
		u.put("hformat", "yyyy");
		ms.add(u);
		
		u = new UMap(3);
		u.put("codeAttribute",4); 
		ms.add(u);
		
		needResetNo(ms,checkstr,"soe");
		if(true)
			return;
		
		StringBuilder checkv = new StringBuilder();
		StringBuilder regularstr = new StringBuilder();
		
		String clstr = checkstr;
		
		String datestr = "((\\d{4})|(\\d{4}\\d{2})|(\\d{4}\\d{2}\\d{2}))";
		String noreg = "(\\d{4})";
		String orgreg = "(\\w*)";
		 //固定值
		 regularstr.append("hn");
		 checkv.append("hn");
		 //日期
		 regularstr.append(datestr); 
		 checkv.append(DateUtil.dateToString(new Date(),"yyyyMMdd"));
		 //序号
		 regularstr.append(noreg); 
		 clstr = clstr.substring(0,checkv.length())+clstr.substring(checkv.length()+4,clstr.length());
		 //固定值
		 regularstr.append("gw"); 
		 checkv.append("gw");
		 //序号
		regularstr.append(noreg); 
		clstr = clstr.substring(0,checkv.length())+clstr.substring(checkv.length()+4,clstr.length());
		 //日期
		regularstr.append(datestr); 
		checkv.append(DateUtil.dateToString(new Date(),"yyyy"));
		 //组织
		 regularstr.append(orgreg);
		 checkv.append("soe");
		 //以下规则为:固定值+日期+序号+固定值+序号+日期+组织
		Pattern p = Pattern.compile(regularstr.toString()); 
		boolean isdate = p.matcher(checkstr).matches(); 
		System.out.println(clstr+"\n"+checkv+"\n 解析后是否匹配:"+(clstr.equals(checkv.toString()))+"\n----- \n"+checkstr+"\n"+regularstr.toString()+"\n是否匹配:"+isdate);
			 
	}
}
