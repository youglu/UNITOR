package org.chuniter.core.kernel.kernelunit;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.chuniter.core.kernel.impl.GeneralServiceAdapter;
import org.chuniter.core.kernel.impl.orm.EntityMapping;

public class ParamLD extends Param {
	private static final long serialVersionUID = 189606200981870660L;
	protected Log log = LogFactory.getLog(ParamLD.class);
	private Map<String, Object> param;
	private String badSql = "drop|database|exec|insert|update|delete|count|*|chr|mid|master|truncate|char|declare|-|$|^|#|&|run|exe|delay|tolower|procedure|‘|{|}|[|]|codec|user|xp_cmdshell|net|asc|drop|table";
	private String worstsql = "drop|database|exec|insert|update|delete|chr|mid|master|truncate|char|declare|$|^|#|&|run|exe|delay|tolower|procedure|{|}|[|]|codec|user|xp_cmdshell|net";
	private String[] BasSqlAry = badSql.split("\\|");
	private String[] worstBasSqlAry = worstsql.split("\\|");
	private List<String> orConditoinCriterionLis;
	private Map<Class<?>,Object[]> joinConditionMap;
	private Map<String,Object[]> joinConditionMapAlias;
	public ParamLD() {
		param = new LinkedHashMap<String, Object>();
		orConditoinCriterionLis = new ArrayList<String>();
	}



	public void addParam(String propertyName, Object value) {
		addParam(propertyName, ((String) (null)), value);
	}

	public Map<String, Object[]> getJoinConditionMapAlias() {
		return joinConditionMapAlias;
	}


	public void addParam(String propertyName, String condition, Object value) {
		if (propertyName == null)
			return;
		if ("".equals(propertyName.trim()))
			return;
		//目前存在一个问题，即同一个属性且同一个条件符不能出现一次以上，如：empname==a,empname==b,其中empname==这个是做为map的KEY，所以才会有此情况。
		//现在加个序号做为处理，在解析时，去掉加的序号. youg 2017-11-30 12:31
		if (!StringUtil.isValid(condition))
			condition = "==";
		condition = condition.trim();
		if ("like".equals(condition) || "orlike".equals(condition)) {
			if(value instanceof String[]) {
				String[] vs = (String[])value;
				for(int i=0;i<vs.length;i++) {
					vs[i] = (new StringBuilder("%")).append(vs[i]).append("%").toString();
				}
			}else
				value = (new StringBuilder("%")).append(value).append("%").toString();
		}
		String key = propertyName+"_"+condition;
		if(param.containsKey(key)) {
			if(null != value&&param.get(key).toString().equals(value.toString()))
				return;
			key+="&"+param.size();
		}
		param.put(key, value);
	}

	public Map<String, Object> getParam() {
		return param;
	}

	public boolean isEmpty() {
		return param.isEmpty();
	}
	public String toString() {
		String result = " ";
		Set<String> keys = param.keySet();
		if (!keys.isEmpty()) {
			for (Iterator<String> iterator = keys.iterator(); iterator
					.hasNext();) {
				String key =  iterator.next();
				int sameindex = key.indexOf("&");
				if(sameindex != -1)
					key = key.substring(0,sameindex);
				result = (new StringBuilder(String.valueOf(result)))
						.append(key).append(" ").append(param.get(key))
						.append(",").toString();
			}

			result = result.substring(0, result.length() - 1);
		}
		return result;
	}

	public Object getCriteria() throws Exception{
		return getCriteria(param);
	}

	private Object getCriteria(Map<String,Object> param) throws Exception{
		StringBuilder sqlSb = new StringBuilder();
		Map<String,Object> sqlAndParamMap = new HashMap<String,Object>();
		List<Object> paramvLis = new ArrayList<Object>();
		orConditoinCriterionLis.clear();
		if (param.isEmpty()){
			sqlAndParamMap.put(EntityMapping.SQLCOUNTPARAM,"");
			sqlAndParamMap.put(EntityMapping.SQL,"");
			sqlAndParamMap.put(EntityMapping.SQLORDER,"");
			sqlAndParamMap.put(EntityMapping.SQLPARAM,paramvLis);
			return sqlAndParamMap;
		}
		try {
			String dataOwnerv = GeneralServiceAdapter.getCurrentDataOwner();
			for (Entry<String,Object> entry:param.entrySet()) {
				String key = entry.getKey();
				int sameindex = key.indexOf("&");
				if(sameindex != -1)
					key = key.substring(0,sameindex);
				boolean isor = false;
				String andor = " and ";
				if(key.startsWith("or_")) {
					key = key.substring(3);
					isor = true;
					andor = " or (";
				}
				Object value = entry.getValue();
				int index = key.lastIndexOf("_");

				if (index < 0) {
					sqlSb.append(andor).append(key).append(" =? ");
					paramvLis.add(value);
				} else {
					String keys[] = proMultSymbKey(key);
					keys[1] = keys[1].toLowerCase().trim();
					if ("==".equals(keys[1]) || "eq".equals(keys[1])){
						sqlSb.append(andor).append(keys[0]).append(" =? ");
						paramvLis.add(value);
					}
					else if ("!=".equals(keys[1]) || "ne".equals(keys[1])){
						sqlSb.append(andor).append(keys[0]).append(" <>? ");
						paramvLis.add(value);
					}
					else if (">".equals(keys[1]) || "gt".equals(keys[1])){
						sqlSb.append(andor).append(keys[0]).append(" >? ");
						paramvLis.add(value);
					}
					else if ("<".equals(keys[1]) || "lt".equals(keys[1])){
						sqlSb.append(andor).append(keys[0]).append(" <? ");
						paramvLis.add(value);
					}
					else if ("<=".equals(keys[1]) || "le".equals(keys[1])){
						sqlSb.append(andor).append(keys[0]).append(" <=? ");
						paramvLis.add(value);
					}
					else if (">=".equals(keys[1]) || "ge".equals(keys[1])){
						sqlSb.append(andor).append(keys[0]).append(" >=? ");
						paramvLis.add(value);
					}
					else if ("like".equals(keys[1])){
						if (value instanceof String[]) {
							sqlSb.append(andor).append(appendLikeCollectionSql((String[])value,keys[0],"like"));
							for(String s: keys[0].split(","))
								paramvLis.add((String[])value);
						}else {
							sqlSb.append(andor).append(keys[0]).append(" like ? ");
							for(String s: keys[0].split(","))
								paramvLis.add(value);
						}
					}
					else if ("in".equals(keys[1])) {
						sqlSb.append(andor).append(keys[0]).append(" in ");
						if (value instanceof Collection<?>) {
							sqlSb.append(appendCollectionSql((Collection<?>)value));
							paramvLis.add((Collection<?>)value);
						} else {
							Object[] args = (Object[]) value;
							sqlSb.append(appendCollectionSql(args));
							paramvLis.add(args);
						}
					} else if ("not in".equals(keys[1])) {
						sqlSb.append(andor).append(keys[0]).append(" not in ? ");
						if (value instanceof Collection<?>) {
							paramvLis.add((Collection<?>)value);
						} else {
							Object args[] = (Object[]) value;
							paramvLis.add(args);
						}
					}else if ("isnull".equals(keys[1]) && value != null) {
						String sqlv = " is not null ";
						if(value instanceof Boolean) {
							if ((Boolean)value)
								sqlv = " is null ";
						}else if(value instanceof Integer) {
							if (((Integer)value)>0)
								sqlv = " is null ";
						}else{
							String v = value.toString();
							if("1".equals(v)||"true".equals(v))
								sqlv = " is null ";
						}
						sqlSb.append(andor).append(keys[0]).append(sqlv);
					} else if ("or".equals(keys[1]) || "||".equals(keys[1])||"andor".equals(keys[1])) {
						Object values[] = null;
						if (value instanceof List) {
							List<?> list = (List<?>) value;
							values = list.toArray();

						}if (value instanceof Object[]) {
							values = (Object[]) value;
						} else {
							sqlSb.append(andor).append(keys[0]).append(" = ? ");
							paramvLis.add(value);
						}
						if(null != values)	{
							if (values.length == 1){
								sqlSb.append(andor).append(keys[0]).append(" =? ");
								paramvLis.add(values[0]);
							}else if (values.length > 1) {
								sqlSb.append(andor).append(" ( ");

								for (int i = 0; i < values.length; i++){
									sqlSb.append(keys[0]).append(" =? ");
									if(i<values.length-1)
										sqlSb.append(" or ");
									paramvLis.add(values[i]);
								}
								sqlSb.append(") ");
							}
						}
					}else if ("orlike".equals(keys[1])) {
						Object[] values = null;
						if (value instanceof List) {
							List<?> list = (List<?>) value;
							if (null == list || list.size() <= 0)
								continue;
							values = list.toArray();
						} else {
							if (value instanceof String)
								values = new Object[] { value };
							else
								values = (Object[]) value;
						}
						if (null == values || values.length <= 0)
							continue;
						sqlSb.append(andor).append(keys[0]).append(" like ? ");
						paramvLis.add(values[0]);
					}else if("sql".equals(keys[1])||"sql-server".equals(keys[1])) {
						if ((hasWorstSQLInjection(value.toString())&&(!"sql-server".equals(keys[1])))||((!"sql-server".equals(keys[1]))&&hasSQLInjection(value.toString()))) {
							String badsql = sqlSb.toString();
							sqlSb = new StringBuilder(" and 1='存在SQL注入攻击' ");
							//sqlSb.append(" and 1='存在SQL注入攻击' "+value);  
							throw new Exception("存在SQL注入攻击！sql内容为："+badsql);
						}
						if (value != null){
							String sqlstr = value.toString();
							if(sqlstr.trim().startsWith("and")||sqlstr.trim().startsWith("or"))
								sqlSb.append(value.toString());
							else
								sqlSb.append(" and ("+value.toString()).append(") ");
						}
						sqlSb.append(" ");
					}
					if(isor) {
						String dataOwner = "dataOwner";
						int dot = key.indexOf(".");
						if(dot != -1)
							dataOwner=key.subSequence(0, dot+1)+dataOwner;
						else if(dataOwnerName != null)
							dataOwner=dataOwnerName;
						sqlSb.append(" and ").append(dataOwner).append(" = ? ) ");
						paramvLis.add(dataOwnerv);
					}
				}
			}
			if((null == needAddDataOwner||needAddDataOwner)&&StringUtil.isValid(dataOwnerv)) {
				//最后再增加dataowner
				String dataOwner = "dataOwner";
				if(dataOwnerName != null)
					dataOwner=dataOwnerName;
				sqlSb = new StringBuilder(" and (1=1 ").append(sqlSb.toString()).append(") and ").append(dataOwner).append("=? ");
				paramvLis.add(dataOwnerv);
			}
			//由于msql的统计sql后面不能直接接order by，故在这里保存一份没有order by 的参数
			sqlAndParamMap.put(EntityMapping.SQLCOUNTPARAM,sqlSb.toString());
			//由于order 与group需要放在sql的后面,且order 也需要放在group后面，所以迭代map两次
			//StringBuilder ordersb = new StringBuilder();
			String groupstr = "";
			StringBuilder orderstr = new StringBuilder();
			for (Entry<String,Object> entry:param.entrySet()) {
				String key = entry.getKey();
				Object value = entry.getValue();
				if (key.lastIndexOf("_") > 0) {
					String keys[] = proMultSymbKey(key);
					keys[1] = keys[1].toLowerCase().trim();
					if ("order".equals(keys[1])) {
						if(orderstr.length()<=0) {
							orderstr.append(" order by ").append(keys[0]).append(("asc".equals(value))?" asc":" desc");
						}else {
							String lowstr = orderstr.toString().toLowerCase();
							if(lowstr.indexOf(new String(keys[0]).toLowerCase()) != -1)
								continue;
							orderstr.append(",").append(keys[0]).append(("asc".equals(value))?" asc":" desc");
						}
					}else if("group".equals(keys[1]))
						groupstr = " group by "+keys[0];
				}
			}
			//ordersb.append(groupstr).append(" ").append(orderstr);
			//ordersb.append(orderstr);
			//sqlSb.append(orderstr);
			sqlAndParamMap.put(EntityMapping.SQLORDER,orderstr);
			sqlAndParamMap.put(EntityMapping.SQLGROUP,groupstr);
			sqlAndParamMap.put(EntityMapping.SQL,sqlSb);
			sqlAndParamMap.put(EntityMapping.SQLPARAM,paramvLis);
			return sqlAndParamMap;
		} catch (Exception ex) {
			System.out.println(ex);
			ex.printStackTrace();
		}
		String sql = sqlSb.toString();
		if(null != this.sqlprovider)
			sql = this.sqlprovider.proSql(sqlSb.toString());
		sqlAndParamMap.put(EntityMapping.SQL,sql);
		sqlAndParamMap.put(EntityMapping.SQLPARAM,paramvLis);
		return sqlAndParamMap;
	}

	private String[] proMultSymbKey(String origiKey){
		if(!StringUtil.isValid(origiKey))
			return new String[]{origiKey};
		String keys[] = origiKey.split("_");
		if(keys.length>2){
			String panem = origiKey.substring(0, origiKey.lastIndexOf("_"));
			String condition = keys[keys.length-1];
			keys = new String[]{panem,condition};
		}
		return keys;
	}

	public Object getCountCriteria(Object criteria) throws Exception{
		Set<String> keys = param.keySet();
		Map<String, Object> p = new LinkedHashMap<String, Object>();
		for (Iterator<String> iterator = keys.iterator(); iterator.hasNext();) {
			String keyStr = iterator.next();
			int sameindex = keyStr.indexOf("&");
			if(sameindex != -1)
				keyStr = keyStr.substring(0,sameindex);
			// 在里需要考虑到如果属性名称包括order或group的情况。在统计总数时，不加上order与group条件。
			//所以这里需重新声明一个map参数变量，并加入除了order与group要件
			int index = keyStr.indexOf("_");
			if (index != -1) {
				String keyStrs[] = keyStr.split("_");
				String conditionStr = keyStrs[1].toLowerCase().trim();

				if (!"order".equals(conditionStr)
						&& !"group".equals(conditionStr))
					p.put(keyStr, param.get(keyStr));
			}
		}
		return getCriteria(p);
	}

	public void addParam(String propertyName, Object value, boolean isUrlDecode) {
		if (!StringUtil.isValid(propertyName))
			return;
		if (!(value instanceof String))
			return;
		String val = (String) value;
		if (!StringUtil.isValid(val))
			return;
		if("isnull".equals(val)) {
			addParam(propertyName,val,isUrlDecode,false);
			return;
		}
		if (isUrlDecode)
			try {
				val = URLDecoder.decode(val, "UTF-8");
				addParam(propertyName, val);
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		else
			addParam(propertyName, value);
	}

	public void addParam(String propertyName, String condition, Object value,boolean isUrlDecode) {
		if (!StringUtil.isValid(propertyName)||null == value)
			return;
		if (!(value instanceof String)) {
			addParam(propertyName, condition, value);
			return;
		}
		String val = (String) value;
		if (!StringUtil.isValid(val))
			return;
		if (isUrlDecode) {
			try {
				val = URLDecoder.decode(val, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}
		addParam(propertyName, condition, val);
	}

	public void clear() {
		param.clear();
		if(null != orConditoinCriterionLis)
			orConditoinCriterionLis.clear();
		if(null != joinConditionMap)
			joinConditionMap.clear();
		if(null != joinConditionMapAlias)
			joinConditionMapAlias.clear();
		this.dataOwnerName = null;
	}
	private boolean hasWorstSQLInjection(String sqlStr) throws Exception{
		if(!StringUtil.isValid(sqlStr))
			return false;
		try{
			Properties prop = PropertiesUtil.getProperties("system.properties");
			Object obj = prop.getProperty("worstsql");
			if(null != obj)
				if(obj.toString().length()>worstsql.length()){
					worstsql = obj.toString();
					worstBasSqlAry = worstsql.split("\\|");
				}
		}catch(Exception e){
			log.debug("在进行sql注入处理时，无法加system.properties中的badSQL属性指的定过滤sql，程序使用默认设置。");
		}
		worstsql = worstsql.toLowerCase();
		sqlStr = sqlStr.toLowerCase();
		for(String badSQ:worstBasSqlAry){
			if(sqlStr.indexOf(badSQ) != -1)
				return true;
		}
		return false;
	}
	public  boolean hasSQLInjection(String sqlStr) throws Exception{
		if(!StringUtil.isValid(sqlStr))
			return false;
		try{
			Properties prop = PropertiesUtil.getProperties("system.properties");
			if(null != prop){
				Object obj = prop.getProperty("worstsql");
				if(null != obj)
					if(obj.toString().length()>worstsql.length()){
						worstsql = obj.toString();
						BasSqlAry = worstsql.toLowerCase().split("\\|");
					}
			}else{
				BasSqlAry = worstsql.toLowerCase().split("\\|");
			}
		}catch(Exception e){
			log.debug("在进行sql注入处理时，无法加system.properties中的badSQL属性指的定过滤sql，程序使用默认设置。");
		}
		//worstsql = worstsql.toLowerCase();
		sqlStr = sqlStr.toLowerCase();
		for(String badSQ:BasSqlAry){
			if(sqlStr.indexOf(badSQ) != -1)
				return true;
		}
		return false;
	}
	public final boolean checkSQLInjection(String sqlStr) throws Exception{
		return hasSQLInjection(sqlStr);
	}
	private String appendCollectionSql(Collection<?> c){
		if(null == c || c.isEmpty())
			return "";
		StringBuilder inSql = new StringBuilder(" (");
		for(int i=0;i<c.size();i++){
			inSql.append("?");
			if(i<c.size()-1)
				inSql.append(",");
		}
		inSql.append(") ");
		return inSql.toString();
	}
	private String appendCollectionSql(Object[] c){
		if(null == c || c.length<=0)
			return "";
		StringBuilder inSql = new StringBuilder(" (");
		for(int i=0;i<c.length;i++){
			inSql.append("?");
			if(i<c.length-1)
				inSql.append(",");
		}
		inSql.append(") ");
		return inSql.toString();
	}
	private String appendLikeCollectionSql(Object[] c,String fname,String condition){
		if(null == c || c.length<=0)
			return "";
		StringBuilder inSql = new StringBuilder(" (");
		String[] fs = null;
		if(fname.indexOf(",") != -1) {
			fs = fname.split(",");
		}else
			fs = new String[] {fname};
		if(null != condition)
			condition = condition.trim();
		int l1 = fs.length;
		int l2 = c.length;
		for(int i=0;i<l1;i++){
			for(int j=0;j<l2;j++){
				inSql.append(fs[i]).append(" ").append(condition).append(" ? ");
				if(j<l2-1)
					inSql.append(" or ");
			}
			if(i<l1-1)
				inSql.append(" or ");
		}
		inSql.append(") ");
		return inSql.toString();
	}
	public static void main(String[] args) throws Exception {
		/*String badSql = "exec|insert|select|update|delete|count|*|chr|mid|master|truncate|char|declare|-|$|^|#|&|run|exe|delay|tolower|procedure|‘|{|}|[|]|codec|user|xp_cmdshell|net|asc|drop|table";
		Properties prop = PropertiesUtil.getProperties("system.properties");
		Object obj = prop.getProperty("badSQL");
		if (null != obj)
			if (obj.toString().length() > badSql.length())
				badSql = obj.toString();
		String[] BasSqlArys = badSql.split("\\|");
		System.out.println(BasSqlArys);*/

		System.out.println("sdfsdf".split(",").length);
		String [] sss = new String[] {"","",""};
		System.out.println((sss instanceof String[])+ " --- ");
		System.out.println((sss instanceof Object[])+ " --- ");

		String f = "or_emp_.empName";
		System.out.println(f.substring(3));

		String f2 = "emp_.empName_==&1";
		System.out.println(f2.substring(0,f2.indexOf("&")));
		Param p = new ParamLD();//p.hasCondition("PersonContractEntity_.createrId")
		p.addParam("PersonContractEntity_.createrId", "12312312312");
		System.out.println("has1:"+p.hasCondition("PersonContractEntity_.createrId"));
		System.out.println("has2:"+p.hasCondition("==","PersonContractEntity_.createrId"));
		p.addParam("name,age","like",new String[] {"小明","请假"});
		Map tmp = (Map)p.getCriteria(null);
		System.out.println(tmp.get("sql"));
		System.out.println(tmp.get("sqlParam"));

		p.clear();
		p.addParam("id","!=","12312");
		System.out.println(p.getCondition("id","!="));
		p.orParam("emp_.name","like","12312");
		p.addParam("sname","中心人才");
		p.addParam("ids","in",new String[]{"1","2"});
		p.addParam("sex","like", "力",true);
		p.addParam("age",">=","2012-11-12 00:00:00");
		p.addParam("age","<=","2012-12-12 00:00:00");
		tmp = (Map)p.getCriteria(null);
		System.out.println(tmp.get("sql"));
		System.out.println(tmp.get("sqlParam"));
		String sfd = "p_pass_isok_==1";
		String[] ks = sfd.split("_");
		String s1 = sfd.substring(0,sfd.lastIndexOf("_"));
		String cd = ks[ks.length-1];
		System.out.println(s1+"  "+cd);

		p.clear();
		p.addParam("empName", "s");
		p.orParam("empName","like", "2");
		p.orParam("empName","like", "3");
		System.out.println(p.getCriteria(null));
		p.delParam("empName", "like");
		System.out.println(p.getCriteria(null));
	}

	@Override
	public Object getCriteria(Object obj) throws Exception{
		return getCriteria();
	}

	public Map<Class<?>,Object[]> getJoinConditionMap() {
		return joinConditionMap;
	}

	public void setJoinConditionMap(Map<Class<?>, List<String>> joinConditionMap) {
		Map<Class<?>,Object[]> tempmap  = new HashMap<Class<?>,Object[]>(joinConditionMap.size());
		for(Entry<Class<?>,List<String>> cz:joinConditionMap.entrySet()){
			tempmap.put(cz.getKey(), cz.getValue().toArray());
		}
		this.joinConditionMap = tempmap;
	}
	@Override
	public void setJoinCondition(Class<?> clas,String[] joinfeilds,String...joinType) {
		if(joinConditionMap == null)
			joinConditionMap = new LinkedHashMap<Class<?>, Object[]>();
		if(null != joinType&&joinType.length>0&&null!= joinfeilds&&joinfeilds.length>0) {
			if("left".equals(joinType[0])||"right".equals(joinType[0])||"inner".equals(joinType[0])) {
				String[] os = new String[joinfeilds.length+1];
				os[0] = "|"+joinType[0]+"|";
				for(int i=0;i<joinfeilds.length;i++) {
					os[i+1] = joinfeilds[i];
				}
				joinfeilds = os;
			}
		}
		joinConditionMap.put(clas, joinfeilds);
	}
	@Override
	public void delJoinCondition(Class<?> clas) {
		if(joinConditionMap == null)
			joinConditionMap = new LinkedHashMap<Class<?>, Object[]>();
		if(!joinConditionMap.containsKey(clas))
			return;
		joinConditionMap.remove(clas);
	}
	@Override
	public boolean hasJoinCondition(String fullJoinClassName) {
		if(joinConditionMap == null||joinConditionMap.isEmpty())
			return false;
		for(Entry<Class<?>,Object[]> e:joinConditionMap.entrySet()) {
			if(e.getKey().getName().equals(fullJoinClassName))
				return true;
		}
		return false;
	}

	public void setJoinConditionMapArray(Map<Class<?>, Object[]> joinConditionMap) {
		this.joinConditionMap = joinConditionMap;
	}
	@Override
	public void delParam(String s) {
		delParam(s,"==");
	}
	@Override
	public void delParam(String s,String condition) {
		if (!StringUtil.isValid(s))
			return;
		String k = "";
		StringBuilder dsb = new StringBuilder();
		for(Entry<String, Object> e:param.entrySet()){
			k = e.getKey();
			int sameindex = k.indexOf("&");
			if(sameindex != -1)
				k = k.substring(0,sameindex);
			if(k.startsWith("or_")) {
				k = k.substring(3);
			}
			if(k.equals(s+"_"+condition)){
				if(dsb.length()>0)
					dsb.append(",").append(e.getKey());
				else
					dsb.append(e.getKey());
			}
		}
		if(dsb.length()<=0)
			return;
		for(String rk:dsb.toString().split(","))
			param.remove(rk);
	}

	@Override
	public void setJoinConditionMap(Class<?> c, String[] fs) {
		if(joinConditionMap == null)
			joinConditionMap = new HashMap<Class<?>, Object[]>();
		joinConditionMap.put(c, fs);
	}
	public  void setJoinConditionMap(String c, String[] fs) {
		if(true)
			try {
				throw new Exception("未实现");
			} catch (Exception e) {
				e.printStackTrace();
			}
		if(true)
			return;
		if(null == joinConditionMapAlias)
			joinConditionMapAlias = new HashMap<String,Object[]>();
		joinConditionMapAlias.put(c, fs);
	}
	public String getSqlForReport() throws Exception{
		Map<String,Object> sqlAndParamMap = (Map<String, Object>) this.getCriteria();
		Object sql = sqlAndParamMap.get(EntityMapping.SQL);
		if(null == sql){
			return "";
		}
		//替换关联表
		return sql.toString();
	}

	public String getReportSql() throws Exception{
		Map<String,Object> sqlAndParamMap = (Map<String, Object>) this.getCriteria();
		Object sql = sqlAndParamMap.get(EntityMapping.SQL);
		if(null == sql)
			return "";
		List<Object> paramvLis = (List<Object>) sqlAndParamMap.get(EntityMapping.SQLPARAM);
		if(null != paramvLis){
			String[] sqls = sql.toString().split("\\?");
			StringBuilder sb = new StringBuilder();
			for(int i=0;i<paramvLis.size();i++){
				Object po = paramvLis.get(i);
				if(null == po)
					continue;
				sqls[i]+=po.toString();
				System.out.println(po);
			}
			for(String s:sqls){
				sb.append(s).append(" ");
			}
			sql = sb.toString();
		}
		return sql.toString();
	}
	public void orParam(String propertyName, String condition, Object value) {
		if (!StringUtil.isValid(propertyName))
			return;
		if (!(value instanceof String))
			return;
		String val = (String) value;
		if (!StringUtil.isValid(val))
			return;
		if(!StringUtil.isValid(condition))
			condition = "==";
		addParam("or_"+propertyName,condition, value);
	}



	@Override
	public Object clone() throws CloneNotSupportedException {
		ParamLD newp =  (ParamLD) super.clone();
		if(null != param)
			newp.param = (Map<String, Object>)( ((HashMap<String, Object>)param).clone());
		if(null != joinConditionMap)
			newp.joinConditionMap = (Map<Class<?>,Object[]>)( ((HashMap<Class<?>,Object[]>)joinConditionMap).clone());
		if(null != joinConditionMapAlias)
			newp.joinConditionMapAlias = (Map<String,Object[]>)( ((HashMap<String,Object[]>)joinConditionMapAlias).clone());
		return newp;
	}
	@Override
	public Param cloneme() throws CloneNotSupportedException {
		return (ParamLD) this.clone();
	}

}
