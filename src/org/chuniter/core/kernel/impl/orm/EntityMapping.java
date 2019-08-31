package org.chuniter.core.kernel.impl.orm;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.chuniter.core.kernel.annotation.ReflectField;
import org.chuniter.core.kernel.api.orm.ISQLProvider;
import org.chuniter.core.kernel.api.unit.Unit;
import org.chuniter.core.kernel.impl.GeneralServiceAdapter;
import org.chuniter.core.kernel.kernelunit.AnnotationParser;
import org.chuniter.core.kernel.kernelunit.Param;
import org.chuniter.core.kernel.kernelunit.ReflectUtil;
import org.chuniter.core.kernel.kernelunit.StringUtil;
import org.chuniter.core.kernel.model.BaseEntity;


public final class EntityMapping{

	private Class<?> entityClass;
	//private Class<?> originalEntityClass;
	private String entityName;
	private ExtendedField[] fields;
	private ExtendedField[] origiFields;
	private String[] fieldNames;
	private ReflectUtil refu = ReflectUtil.getInstance();
	protected Log log = LogFactory.getLog(super.getClass());
	private ISQLProvider sqlprovider;
	public final static String SQL = "sql";
	public final static String SQLPARAM = "sqlParam";
	public final static String SQLPARAMMAP = "sqlParammap";
	public final static String SQLCOUNTPARAM = "sqlCountParam";
	public final static String SQLORDER = "sqlOrder";
	public final static String SQLGROUP = "sqlGroup";
	public final static String ENTITYID = "id";
	private String entityId = ENTITYID;
	public final static String AUTONOSYB = "a?t";
	public final static String AUTONOSYBRESULTNAME = " _ca_ts_";
	private String entityAlias = "";
	private Map<Class<?>,List<ReflectField>> reflectClassMap = new HashMap<Class<?>,List<ReflectField>>();
	private Map<String,String> reflectClassFieldMap = new HashMap<String,String>();
	private Map<String,ReflectField> reflectFieldClassMap = new HashMap<String,ReflectField>();
	private Map<String,ReflectField> reflectSelectFieldMap = new HashMap<String,ReflectField>();
	private Map<String,ExtendedField> exFieldMap = new HashMap<String,ExtendedField>();
	private ThreadLocal<Param> paramLC = new ThreadLocal<Param>();
	public final static String FCOUNTEXTNAME = "_count";
	private boolean isShowTable = false;
	//缓存类相关信息。
	public static final String[] noupfields = new String[]{"createDate","createMan","createrId","estate","dataOwner","ecode","meIndex","batchNo"};
	private final static Map<String,ExtendedField> classCacheInfo = new ConcurrentHashMap<String,ExtendedField>();
	private final static Map<String,Method> classMethodCache = new ConcurrentHashMap<String,Method>();
	private final static Map<String, ExtendedField[]> classExfCacheInfo = new ConcurrentHashMap<String, ExtendedField[]>();


	public static void removeClassCacheInfo(Class<?> c) {
		String k = c.getName();
		if(classCacheInfo.isEmpty())
			return;
		synchronized(classCacheInfo){
			Iterator<Map.Entry<String, ExtendedField>> it = classCacheInfo.entrySet().iterator();
			while(it.hasNext()){
				Map.Entry<String, ExtendedField> entry = it.next();
				if(entry.getKey().startsWith(k))
					it.remove();//使用迭代器的remove()方法删除元素
			}
		}
	}
	public static void putClassCacheInfo(Class<?> c,ExtendedField ex,String mk) {
		String k = c.getName()+(null != ex?ex.field.getName():"");
		if(null != mk)
			k = mk;
		classCacheInfo.put(k, ex);
	}
	public static ExtendedField getClassCacheInfo(Class<?> c,String fn) {
		if(c == null)
			return null;
		String k = c.getName()+""+fn;
		if(!classCacheInfo.containsKey(k))
			return null;
		ExtendedField f = classCacheInfo.get(k);
		if(f!=null&&f.field.getDeclaringClass() != c)
			return null;
		return f;
	}
	public static ExtendedField[] getClassExfs(String classFullName) {
		if(classFullName == null)
			return null;
		if(!classExfCacheInfo.containsKey(classFullName))
			return null;
		//System.out.println("从ENTITYMAPPING获取实体"+classFullName+"的扩展属性集合");
		return classExfCacheInfo.get(classFullName);
	}
	public static void removeClassMethodCache(Class<?> c) {
		String k = c.getName();
		if(classMethodCache.isEmpty())
			return;
		synchronized(classMethodCache){
			Iterator<Map.Entry<String, Method>> it = classMethodCache.entrySet().iterator();
			while(it.hasNext()){
				Map.Entry<String, Method> entry = it.next();
				if(entry.getKey().startsWith(k))
					it.remove();//使用迭代器的remove()方法删除元素
			}
		}
	}
	public static void putClassMethodCache(Class<?> c,Method m,String mk) {
		String k = c.getName()+(null != m?m.getName():"");
		if(null != mk)
			k = mk;
		if(null == k||m == null)
			return;
		classMethodCache.put(k, m);
	}
	public static Method getClassMethodCache(Class<?> c,String mn) {
		String k = c.getName()+""+mn;
		if(!classCacheInfo.containsKey(k))
			return null;
		return classMethodCache.get(k);
	}
	public EntityMapping(Class<?> entityClass){
		//originalEntityClass = entityClass;
		updateEntityClass(entityClass);
	}
	public static void removeAllClassCache() {
		System.out.println("清除所有类缓存");
		classMethodCache.clear();
		classCacheInfo.clear();
		classExfCacheInfo.clear();
	}
	public void destroy(Class<?> c,boolean isall) {
		if(isall) {
			log.debug("清除所有类缓存");
			removeAllClassCache();
			return;
		}
		log.debug("清除类缓存");
		if(c == null)
			c = this.entityClass;
		removeClassMethodCache(c);
		removeClassCacheInfo(c);
		classExfCacheInfo.remove(c.getName());
	}
	public void updateEntityClass(Class<?> updateEntityClass){
		this.entityClass = updateEntityClass;
		this.entityName = AnnotationParser.getNameFormEntity(this.entityClass);
		this.entityAlias = entityName+"_";
		setFields(refu.getAllField(entityClass,true));
		classExfCacheInfo.put(this.entityClass.getName(), this.fields );
		entityId = AnnotationParser.getNameFormId(updateEntityClass,fields);
		if(!StringUtil.isValid(entityId))
			entityId=ENTITYID;
		if(log.isDebugEnabled())
			log.debug("输出解析实体注解后的名称:"+this.getEntityName()+" id为:"+entityId);

		sqlprovider = new MYSQLProvider();
	}
	/*	private void resetOriginalEntityClass(){
            if(null != originalEntityClass&&originalEntityClass.getName().equals(this.entityClass.getName()))
                this.updateEntityClass(originalEntityClass);
        }*/
	public String getCreateSQL(){
		String tbName = this.entityName;
		StringBuilder sb = new StringBuilder("insert into ");
		sb.append(tbName).append("(");
		int paramVIndexs = 0;
		for(int i=0;i<fields.length;i++){
			if(!fields[i].needORM)
				continue;
			sb.append(fields[i].columnName);
			if(i<fields.length-1)
				sb.append(",");
			paramVIndexs++;
		}
		if(sb.toString().endsWith(","))
			sb.replace(sb.length()-1,sb.length(),"");
		sb.append(") values(");
		for(int i=0;i<paramVIndexs;i++){
			sb.append("?");
			if(i<paramVIndexs-1)
				sb.append(",");
		}
		sb.append(")");
		return sb.toString();
	}
	public Map<String,Object> getCreateSQL(Object t,String tableName) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException{
		setIdValueIfIdIsNull(t);
		Map<String,Object> sqlAndParamMap = new HashMap<String,Object>();
		List<Object> paramvLis = new ArrayList<Object>();
		Map<Integer,ExtendedField> paramExfMap = new LinkedHashMap<Integer,ExtendedField>();
		String tbName = tableName;
		StringBuilder sb = new StringBuilder("insert into ");
		sb.append(tbName).append("(");

		ExtendedField f = null;
		Object fv = null;
		for(int i=0,l=fields.length;i<l;i++){
			f = fields[i];
			if(!f.needORM)
				continue;
			if(null != f.exid&&!f.exid.cansetvalue())
				continue;
			sb.append(f.columnName);
			fv = refu.getFieldValue(t, f.field);
			if(null != f.exid&&f.exid.autoCreate()){
				fv = AUTONOSYB;
			}
			paramExfMap.put(paramvLis.size(), f);
			paramvLis.add(fv);
			if(i<l-1)
				sb.append(",");
		}
		if(sb.toString().endsWith(","))
			sb.replace(sb.length()-1,sb.length(),"");
		sb.append(") values(");
		for(Entry<Integer,ExtendedField> etry:paramExfMap.entrySet()){
			//这里可以根据不同数据库，在特定的数据格式时的特别处理，如针对oracle的日期处理
			sb.append(sqlprovider.appendPreparedValue(etry.getValue()));
			if(etry.getKey()<paramExfMap.size()-1)
				sb.append(",");
		}
		sb.append(")");
		sqlAndParamMap.put(SQL,sb);
		sqlAndParamMap.put(SQLPARAM,paramvLis);
		sqlAndParamMap.put(SQLPARAMMAP,paramExfMap);
		return sqlAndParamMap;
	}
	public Map<String,Object> getCreateSQL(Object t) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException{
		return getCreateSQL(t,this.entityName);
	}
	public Map<String,Object> getUpdateSQL(Map<String,Object> upmap) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException{
		Map<String,Object> sqlAndParamMap = new HashMap<String,Object>();
		List<Object> paramvLis = new ArrayList<Object>();
		StringBuilder sb = new StringBuilder("update ");
		sb.append(this.entityName).append(" set ");
		ExtendedField ef = null;
		for(Entry<String,Object> entry:upmap.entrySet()){
			if(null != exFieldMap&&exFieldMap.containsKey(entry.getKey())){
				ef = exFieldMap.get(entry.getKey());
				if(null == ef||!ef.needORM)
					continue;
			}
			sb.append(entry.getKey()).append("=?,");
			paramvLis.add(entry.getValue());
		}
		if(sb.toString().endsWith(","))
			sb.replace(sb.length()-1,sb.length(),"");
		sb.append(" where 1=1 ");
		sqlAndParamMap.put(SQL,sb);
		sqlAndParamMap.put(SQLPARAM,paramvLis);
		return sqlAndParamMap;
	}
	public Map<String,Object> getUpdateSQL(Object t) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException{
		//setIdValueIfIdIsNull(t);
		return this.getUpdateSQL(t, noupfields);
	}
	public Map<String,Object> getUpdateSQL(Object t,String[] excludeFieldNames) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException{
		if(null == excludeFieldNames||excludeFieldNames.length<=0)
			excludeFieldNames = noupfields;

		setIdValueIfIdIsNull(t);
		Map<String,Object> sqlAndParamMap = new HashMap<String,Object>();
		List<Object> paramvLis = new ArrayList<Object>();
		String tbName = AnnotationParser.getNameFormEntity(t.getClass());//+" "+entityAlias;
		StringBuilder sb = new StringBuilder("update ");
		sb.append(tbName).append(" set ");
		String idv = null;
		String checkFName = "";
		boolean noup = false;
		ExtendedField tempF = null;
		int l = fields.length;
		for(int i=0;i<l;i++){
			tempF = fields[i];
			if(!tempF.needORM)
				continue;
			checkFName = tempF.columnName;
			if((null != tempF.exid&&tempF.exid.autoCreate())&&!entityId.equalsIgnoreCase(checkFName))
				continue;
			Object fv = refu.getFieldValue(t, tempF.field);
			if(entityId.equalsIgnoreCase(checkFName)){
				idv = fv.toString();
				continue;
			}
			noup = false;
			for(String excName:excludeFieldNames)
				if(excName.equalsIgnoreCase(checkFName)){
					noup = true;
					break;
				}
			if(noup)
				continue;
			sb/*.append(entityAlias).append(".")*/.append(tempF.columnName).append("=").append(sqlprovider.appendPreparedValue(fv)).append(",");
			//这里可以根据不同数据库，在特定的数据格式时的特别处理，如针对oracle的日期处理
			paramvLis.add(fv);
		}
		if(sb.toString().endsWith(","))
			sb.replace(sb.length()-1,sb.length(),"");

		sb.append(" where ")/*.append(entityAlias).append(".")*/.append(entityId).append("=").append("?");
		paramvLis.add(idv);

		sqlAndParamMap.put(SQL,sb);
		sqlAndParamMap.put(SQLPARAM,paramvLis);
		return sqlAndParamMap;
	}
	public String getSelectSQL(String[] fieldNames) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException{
		if(null == fieldNames||fieldNames.length<=0)
			return this.getFindSql();

		StringBuilder sb = new StringBuilder("select ");
		String checkFName = "";
		boolean nos = false;
		ExtendedField tempF = null;
		int l = fields.length;
		for(int i=0;i<l;i++){
			tempF = fields[i];
			if(!tempF.needORM)
				continue;
			checkFName = tempF.columnName;
			nos = false;
			for(String sf:fieldNames){
				if(sf.equals(checkFName)){
					nos = true;
					break;
				}
			}
			if(!nos)
				continue;
			sb.append(" ").append(checkFName).append(",");
		}
		if(sb.toString().endsWith(","))
			sb.replace(sb.length()-1,sb.length(),"");
		sb.append(" from ").append(this.entityName).append(" where 1=1 ");
		return sb.toString();
	}
	public ExtendedField[] getFields() {
		return fields;
	}

	public void setFields(ExtendedField[] fields) {
		this.fields = fields;
		refreshFieldNames();
	}

	public void refreshFieldNames(){
		if(null == fields)
			return;
		int l = fields.length;
		ExtendedField[] tempAar = new ExtendedField[l];
		int notMapNO = 0;
		List<ReflectField> refs = null;
		origiFields = new ExtendedField[l];

		for(int i=0;i<l;i++){
			origiFields[i] = fields[i];
			if(null == fields[i])
				continue;
			if(!fields[i].needORM)
				notMapNO++;
			tempAar[i] = fields[i];
			exFieldMap.put(fields[i].columnName.toLowerCase(), fields[i]);
			//增加 youg
			putClassCacheInfo(this.entityClass,fields[i],null);
			ReflectField rf = AnnotationParser.parseReflectField(fields[i].field);
			if(null != rf){
				refs = reflectClassMap.get(rf.entity());
				if(null == refs)
					refs = new ArrayList<ReflectField>();
				refs.add(rf);
				reflectClassMap.put(rf.entity(),refs);//rf);
				reflectClassFieldMap.put(rf.entity()+rf.alias(),fields[i].columnName);
				reflectFieldClassMap.put(fields[i].columnName, rf);
			}
		}
		if (null != tempAar) {
			fieldNames = new String[tempAar.length - notMapNO];
			int j = 0;
			for (int i = 0; i < tempAar.length; i++) {
				if (!tempAar[i].needORM)
					continue;
				fieldNames[j] = tempAar[i].columnName;
				j++;
			}
		}
	}
	String getEntityIsExistInDBSql(){
		return " select * from "+getEntityName()+"   where "+entityId+"=(select max("+entityId+") from "+getEntityName()+" ) ";
		//return existSql;
	}
	String getEntityIsExistInDBSqlWithCount(){
		return " select MAX(1) as c_ from  "+getEntityName()+" where "+entityId+"='1' ";//" select COUNT(1) as c_ from  "+getEntityName()+" having COUNT(1)=0 ";
	}

	public String getAddFieldForMappedClassSql(ExtendedField exf,boolean...isalter){
		StringBuilder sb = new StringBuilder("alter table ").append(getEntityName());
		if(null != isalter&&isalter.length>0&&isalter[0]){
			String ctype = JavaAndJDBCTransformer.javaTypeToJDBCType(exf,sqlprovider);
			int si = ctype.indexOf("(");
			if(si != -1)
				ctype = ctype.substring(0, ctype.indexOf(si));
			return this.getSqlprovider().getModifyColumnSql(getEntityName(),exf.columnName,ctype,exf.getLengthd());
		}else
			sb.append(" add ");

		sb.append(exf.columnName).append(" ").append(JavaAndJDBCTransformer.javaTypeToJDBCType(exf,sqlprovider));
		return sb.toString();
	}
	public String getFindSql(boolean...iscount){
		if(null != iscount&&iscount.length>0&&iscount[0]){
			return "select count(1) from "+getEntityName()+"  "+this.getEntityAlias()+"  where 1=1  ";
		}
		return "select "+this.getSelectFieldSql()+" from "+getEntityName()+"  "+this.getEntityAlias()+"  where 1=1  ";
	}
	public String getCountIdsSql(){
		return "select count(id) as "+AUTONOSYBRESULTNAME+" from "+getEntityName();
	}
	public String getMaxAutoNosSql(String fname){
		return "select max("+fname+") as "+AUTONOSYBRESULTNAME+" from "+getEntityName();
	}
	public String getMaxAutoNosSql(ExtendedField extf){
		if(!extf.field.getType().getName().equals(Integer.class.getName())){
			for(ExtendedField exf:fields){
				if(null != exf.exid&&exf.exid.autoCreate()&&exf.field.getType().getName().equals(Integer.class.getName())){
					return "select max("+exf.columnName+") as "+AUTONOSYBRESULTNAME+" from "+getEntityName();
				}
			}
		}
		return "select max("+extf.columnName+") as "+AUTONOSYBRESULTNAME+" from "+getEntityName();
	}
	public String getEntityCountWithJoinSQL(Map<Class<?>,List<String>> joinClassMap){
		return proJoinSql(joinClassMap,true);
	}
	/**
	 * 这里可以考虑一次性把总数统计sql与非统计sql一次性组装，以免处理两次，目前是处理两次。
	 * @param joinClassMap
	 * @param isCount
	 * @return
	 */
	private String proJoinSql(Map<Class<?>,List<String>> joinClassMap,boolean isCount){
		Map<Class<?>,Object[]> joinClassMap_ = new HashMap<Class<?>,Object[]>();
		for(Entry<Class<?>,List<String>> es:joinClassMap.entrySet())
			joinClassMap_.put(es.getKey(), es.getValue().toArray());
		return proJoinSqlArray(joinClassMap_,isCount,null,null);
	}
	private String proJoinSqlArray(Map<Class<?>,Object[]> joinClassMap,boolean isCount,String...paramsql){
		String[] proPageJoinSqls = this.proPageJoinSqlByArray(joinClassMap,isCount,paramsql);
		if(proPageJoinSqls.length>3&&StringUtil.isValid(proPageJoinSqls[3])) {
			return proPageJoinSqls[0]+proPageJoinSqls[1]+proPageJoinSqls[2]+" where 1=1 "+proPageJoinSqls[3];
		}
		return proPageJoinSqls[0]+proPageJoinSqls[1]+proPageJoinSqls[2];
	}
	private String[] proPageJoinSqlByArray(Map<Class<?>,Object[]> joinClassMap,boolean isCount,String...paramsql){
		if(null == joinClassMap||joinClassMap.isEmpty())
			return new String[]{this.getFindSql(isCount),"",""};
		String[] joinPagesql = new String[4];
		StringBuilder sb = new StringBuilder();
		if(isCount)
			sb.append("select count(1) ");
		else{
			//sb.append("select ").append(entityAlias).append(".*,");
			//由于需要加入distinct特性，故更换了获查询属性的方法 2015-06-12 08:05 youg
			sb.append(" select ").append(getSelectFieldSql()).append(",");
		}
		//ReflectField rf = null;
		String joinTableName = "";
		String aliasName = "";
		StringBuilder selectFieldsql = new StringBuilder();
		StringBuilder onSqls = new StringBuilder();
		List<ReflectField> refs = null;
		String joinType = null;
		//用于保存关于对象的dataowner条件
		StringBuilder joinDataOwnerSql = new StringBuilder();
		for(Entry<Class<?>,Object[]> cz:joinClassMap.entrySet()){
			//从本类中获得关系的类
			refs = reflectClassMap.get(cz.getKey());
			if(null == refs||refs.isEmpty()) {
				//尝试检查是否是子类
				for(Entry<Class<?>,List<ReflectField>> me:reflectClassMap.entrySet()) {
					if(me.getKey().isAssignableFrom(cz.getKey())) {
						refs = me.getValue();
						break;
					}
				}
			}
			if(null == refs||refs.isEmpty()) {
				continue;
			}
			joinTableName = AnnotationParser.getNameFormEntity(cz.getKey());
			for(ReflectField rf:refs){
				if(StringUtil.isValid(rf.entityFullName())){
					joinTableName = fetchRefColumnFinalEntityName(rf);
				}
				aliasName = joinTableName+"_";
				//rf = reflectClassMap.get(cz.getKey());
				if(StringUtil.isValid(rf.alias())){
					aliasName = rf.alias();
				}
				joinType = rf.joinType();
				Object[] vs = cz.getValue();

				if(null!=vs&&vs.length>0){
					Object v = vs[0];
					if(null == v||"".equals(v.toString()))
						continue;
					String sv = v.toString();
					if("|left|".equals(sv)||"|right|".equals(sv)||"|inner|".equals(sv)) {
						joinType = sv.replaceAll("\\|","");
					}
					if(!isCount){
						for(Object tf:vs){
							String fv = tf.toString();
							if("|left|".equals(fv)||"|right|".equals(fv)||"|inner|".equals(fv))
								continue;
							selectFieldsql.append(aliasName).append(".").append(fv).append(" as ").append(aliasName).append(fv).append(",");
							reflectSelectFieldMap.put((aliasName+fv).toLowerCase(), rf);
						}
					}
				}

				onSqls.append(" ").append(joinType).append(" join ").append(joinTableName).append(" ").append(aliasName)
						.append(" on ").append(aliasName).append(".").append(rf.fieldName()).append(" = ");
				String onselffield = rf.selffieldName().equals("")?reflectClassFieldMap.get(rf.entity()+rf.alias()):rf.selffieldName();
				if(onselffield.indexOf(".") == -1)
					onSqls.append(this.entityAlias).append(".").append(onselffield);
				else
					onSqls.append(onselffield).append(" ");
				//级联查询增加dataowner 2017-06-17 14:15 youg
				//排除应用中不需要DATAOWNER的数据，如模块 2017-07-15 19:05 youg
				//FIXME 关联查询时在这里加dataowner条件不对。需加到where后面去
				//还在加下，这样在生成临时表时的数据会少些。 2018-04-15 23：47
				if(rf.entity()!=Unit.class){
					Boolean needdw = paramLC.get().getNeedAddDataOwner();
					if(null == needdw||needdw) {
						onSqls.append(" and  ").append(GeneralServiceAdapter.getCurrentDataOwnerParamSql(aliasName));
						//这两行应该有用。
						//joinDataOwnerSql.append(" and  (").append(GeneralServiceAdapter.getCurrentDataOwnerParamSql(aliasName))
						////这行应该没用 .append(" or ").append(aliasName).append(".").append(rf.fieldName()).append(" is null");
						//joinDataOwnerSql.append(" ) ") ;
						//两行end
						//end
						//if(null != paramsql&&paramsql.length>0) {
						//this.param.addParam(aliasName+".dataOwner", "sql",GeneralServiceAdapter.getCurrentDataOwner(aliasName));
						//}
					}
				}
				//end
				//end
			}
		}

		if(!isCount){
			sb.append(selectFieldsql);
			if(sb.toString().endsWith(","))
				sb.replace(sb.length()-1,sb.length(),"");
		}
		joinPagesql[0] = sb.toString();
		sb.delete(0, sb.length());
		sb.append(" from ").append(entityName).append(" ").append(entityAlias).append(" ");
		joinPagesql[1] = sb.toString();
		joinPagesql[2] = onSqls.toString();
		joinPagesql[3] = joinDataOwnerSql.toString();
		return joinPagesql;
	}
	/*	private String[] proPageJoinSqls(Map<Class<?>,List<String>> joinClassMap,boolean isCount){
            if(null == joinClassMap||joinClassMap.isEmpty())
                return new String[]{this.getFindSql(),"",""};
            Map<Class<?>,Object[]> tempmap  = new HashMap<Class<?>,Object[]>(joinClassMap.size());
            for(Entry<Class<?>,List<String>> cz:joinClassMap.entrySet()){
                tempmap.put(cz.getKey(), cz.getValue().toArray());
            }
            return this.proPageJoinSqlByArray(tempmap,isCount);
        }*/
	private Param fetchParam() {
		Param p = getParam();
		if(p == null)
			return Param.getInstance();
		return p;
	}
	public String getEntityCountWithJoinSQLFormParam(){
		return proJoinSqlArray(fetchParam().getJoinConditionMap(),true);
	}
	public String getEntityCountWithJoinSQLFormParam(String paramsql){
		if(!StringUtil.isValid(paramsql))
			return proJoinSqlArray(fetchParam().getJoinConditionMap(),true);
		String s = proJoinSqlArray(fetchParam().getJoinConditionMap(),true,paramsql);
		if(s.indexOf("where 1=1") != -1)
			return s+" "+paramsql;
		return s+" where 1=1 "+paramsql;
	}
	String getEntityCountWithJoinSQLFormParam(String hisql,Map<String,Object> sqlAndParamMap){
		if(hisql.toLowerCase().lastIndexOf("where")== -1)
			hisql+=" where 1=1 ";
		//int fromindex = hisql.indexOf("from");//indexOf("from");
		//if(fromindex == -1)
		// fromindex = hisql.indexOf("FROM");//.indexOf("FROM");
		/*String s = hisql.substring(0,fromindex);
		if(s.trim().toLowerCase().indexOf("(select") != -1){
			fromindex = hisql.lastIndexOf("from");//indexOf("from");
			if(fromindex == -1)
				 fromindex = hisql.lastIndexOf("FROM");//.indexOf("FROM");
		}*/
		String paramsql = sqlAndParamMap.get(EntityMapping.SQLCOUNTPARAM)==null?"":sqlAndParamMap.get(EntityMapping.SQLCOUNTPARAM).toString();
		String groupandordersql = sqlAndParamMap.get(EntityMapping.SQLGROUP)==null?"":sqlAndParamMap.get(EntityMapping.SQLGROUP).toString();
		String countSql = "select count(1) from ("+hisql+"  "+paramsql+"  "+groupandordersql+") hncc ";
		return countSql;
	}
	public String getFindWithJoinSqlFormParam(){
		return proJoinSqlArray(fetchParam().getJoinConditionMap(),false);
	}
	public String getFindWithJoinSql(Map<Class<?>,List<String>> joinClassMap){
		if(null == joinClassMap||joinClassMap.isEmpty())
			return this.getFindSql();
		return proJoinSql(joinClassMap,false);
	}
	public String getSelectFieldSql(){
		if(null == fields){
			isShowTable = false;
			return null;
		}
		StringBuilder sb = new StringBuilder();
		for(int i=0;i<fields.length;i++){
			if(null == fields[i]||!fields[i].needORM)
				continue;
			if(isShowTable&&!fields[i].isShowInTable()&&null == fields[i].exc)
				continue;
			if(isShowTable&&!fields[i].isShowInTable()&&null != fields[i].exc&&!fields[i].exc.searchMust())
				continue;
			if(null != fields[i].exc&&StringUtil.isValid(fields[i].exc.cussql())){
				sb.append(" ").append(this.entityAlias+"."+fields[i].exc.cussql()).append(" ,");
			}else
				sb.append(" ").append(this.entityAlias+"."+fields[i].columnName).append(" ,");
			if(null != fields[i].exc&&fields[i].exc.distinct()){
				sb.append(" count(").append(this.entityAlias).append(".").append(fields[i].columnName).append(") ").append(fields[i].columnName).append(FCOUNTEXTNAME).append(" ,");
			}
		}
		if(sb.lastIndexOf(",")!=-1)
			sb.replace(sb.length()-1,sb.length(),"");
		isShowTable = false;
		return sb.toString();
	}
	public void proFieldByEntitySetInfo(List<Map<String,Object>> eses){
		if(null == eses || eses.isEmpty())
			return;
		StringBuilder sb = new StringBuilder();
		boolean paramhasorder = fetchParam().hasCondition("order");
		boolean ok = false;
		String fn = null;
		ExtendedField f;
		for(int i=0;i<fields.length;i++){
			f = fields[i];
			fn = f.columnName;
			if(!f.needORM)
				continue;
			if(fn.equalsIgnoreCase(entityId)||fn.equalsIgnoreCase("createDate")||fn.equalsIgnoreCase("createMan"))
				continue;
			if(f.getSearchMust()||f.isShowInTable())
				continue;
			ok = false;
			for(Map<String,Object> m:eses){
				if(m.get("fieldName").equals(f.field.getName())){
					ok = true;
					if(!paramhasorder){
						Object orderType = m.get("orderType");
						if(null != orderType&&!"".equals(orderType.toString()))
							sb.append(m.get("fieldName").toString()).append(",").append(orderType).append("|");
					}
					break;
				}
			}
			//if(!ok)
			f.setShowInTable(ok);
		}
		if(StringUtil.isValid(sb.toString())){
			String o = sb.toString();
			if(o.endsWith("\\|"))
				o = o.substring(0, o.lastIndexOf("\\|"));
			String[] orderstrs = o.split("\\|");
			sb.delete(0, sb.length());
			for(int i=0;i<orderstrs.length;i++){
				if(i+1<orderstrs.length)
					sb.append(orderstrs[i].replaceAll(","," ")).append(",");
				else{
					String[] lastorder = orderstrs[i].split(",");
					sb.append(lastorder[0]);
					fetchParam().addParam(sb.toString(), "order",lastorder[1]);
				}
			}
		}
	}
	public String createTableWithClassSql(){
		/*if(null == fields)
			return null;
		StringBuilder sb = new StringBuilder("create table ").append(getEntityName()).append(" (");
		String primaryKeyStr = "";
		for(int i=0;i<fields.length;i++){
			if(null == fields[i]||!fields[i].needORM)
				continue;
			sb.append(fields[i].columnName).append("  ").append(JavaAndJDBCTransformer.javaTypeToJDBCType(fields[i],sqlprovider));
			if(fields[i].columnName.equalsIgnoreCase(entityId)){
				sb.append(" not null ");
				primaryKeyStr = "alter table "+getEntityName()+" add PRIMARY KEY ("+entityId+");";
			}
			sb.append(",");

		}
		if(sb.lastIndexOf(",")!=-1)
			sb.replace(sb.length()-1,sb.length(),"");
		sb.append("); ").append(primaryKeyStr);*/
		return createTableWithClassSql(getEntityName(),entityClass);
	}
	public String createTableWithClassSql(String tableName,Class<?> c){
		ExtendedField[] fds = refu.getAllField(c);
		if(null == fds)
			return null;
		StringBuilder sb = new StringBuilder("create table ").append(tableName).append(" (");
		String primaryKeyStr = "";
		StringBuilder uniqueStr = new StringBuilder();
		ExtendedField exf = null;
		for(int i=0;i<fds.length;i++){
			exf = fds[i];
			if(null == exf||!exf.needORM)
				continue;
			sb.append(exf.columnName).append("  ").append(JavaAndJDBCTransformer.javaTypeToJDBCType(exf,sqlprovider));
			if(fds[i].columnName.equalsIgnoreCase(entityId)){
				sb.append(" not null ");
				primaryKeyStr = "alter table "+tableName+" add PRIMARY KEY ("+entityId+");";
			}
			/*else if(null != exf.exid&&exf.exid.autoCreate()){
				sb.append(" not null ");
				uniqueStr.append(";ALTER TABLE ").append(tableName).append(" ADD UNIQUE (").append(BaseEntity.MEINDEX).append(")");
			}*/
			sb.append(",");
		}
		if(sb.lastIndexOf(",")!=-1)
			sb.replace(sb.length()-1,sb.length(),"");
		sb.append("); ").append(primaryKeyStr).append(uniqueStr);
		return sb.toString();
	}
	public Map<String,Object> delByEntityPropertySql(Object t) throws Exception{
		StringBuilder sb = new StringBuilder();
		Map<String,Object> sqlAndParamMap = new HashMap<String,Object>();
		String mdelStr = ("delete from "+this.getEntityName()+" where ");
		List<String> paramvLis = new ArrayList<String>();
		for(int i=0;i<fields.length;i++){
			if(null == fields[i]||!fields[i].needORM)
				continue;
			String fieldValueAsStr = refu.getFieldValueAsStr(t,fields[i].field);
			if(!StringUtil.isValid(fieldValueAsStr))
				continue;
			sb.append(fields[i].columnName).append("=? and ");
			paramvLis.add(fieldValueAsStr);
		}
		if(sb.length()<=0)
			throw new Exception("要根据传入的对象参数删除失败，因为这个对象没有任何属性值。");
		sb.append(" ((ecode !='").append(BaseEntity.INBUILT).append("' and ulocked is null) or ecode is null)");
		sqlAndParamMap.put(SQLPARAM, paramvLis);
		sqlAndParamMap.put(SQL, mdelStr+sb.toString());
		return sqlAndParamMap;
	}
	public void setIdValueIfIdIsNull(Object t) throws IllegalArgumentException, IllegalAccessException{
		int l = fields.length;
		for(int i=0;i<l;i++){
			if(fields[i].columnName.equalsIgnoreCase(entityId)){
				if(null != fields[i].exid&&!fields[i].exid.cansetvalue())
					return;
				if(null != fields[i].exid&&fields[i].exid.autoCreate())
					return;
				Object fv = refu.getFieldValue(t, fields[i].field);
				if(null == fv||!(StringUtil.isValid(fv.toString())))
					refu.setFieldValues(t, fields[i].field, UUID.randomUUID().toString());
				break;
			}
		}
	}
	public String getEntityCountSQL(){
		//"+entityId+"
		return "select count(-1) c_pagecount from "+getEntityName()+"  "+this.getEntityAlias()+"  where 1=1 ";
	}
	public String getEntityCountSQL(String paramsql) {
		if(!StringUtil.isValid(paramsql)||paramsql.toLowerCase().indexOf(this.getEntityAlias().toLowerCase()) == -1)
			return this.getEntityCountSQL()+paramsql;
		return "select count("+entityId+") from "+getEntityAlias()+" where 1=1 "+paramsql;
	}
	public String getDelByIdSQL(){
		return "delete from "+getEntityName()+" where "+entityId+"=? and  (((ecode !='"+BaseEntity.INBUILT+"' and (ulocked != 1 or ulocked is null)) or ecode is null) and (estate="+BaseEntity.ENABLE+" or estate="+BaseEntity.NOREVIEW+" or estate is null) ) ";
	}
	public String getDelAllSQL(){
		return "delete  "+this.getEntityAlias()+" from "+getEntityName()+" "+this.getEntityAlias()+" where 1=1 and  ((ecode !='"+BaseEntity.INBUILT+"' and (ulocked != 1 or ulocked is null)) or ecode is null) ";
	}
	public String[] getFieldNames() {
		return fieldNames;
	}
	public Class<?> getEntityClass() {
		return entityClass;
	}
	public ISQLProvider getSqlprovider() {
		return sqlprovider;
	}
	public Serializable getEntityId(Object t){
		for(int i=0;i<fields.length;i++){
			if(null == fields[i]||!fields[i].needORM)
				continue;
			if(fields[i].columnName.equalsIgnoreCase(entityId)){
				return refu.getFieldValue(t, fields[i].field).toString();
			}
		}
		return null;
	}
	public void changeSqlprovider(String dataBaseType) {
		if(!StringUtil.isValid(dataBaseType))
			return;
		if("Microsoft SQL Server".equalsIgnoreCase(dataBaseType))
			sqlprovider = new MSSQLProvider();
		else if("MySQL".equalsIgnoreCase(dataBaseType))
			sqlprovider = new MYSQLProvider();
		else if("Oracle".equalsIgnoreCase(dataBaseType))
			sqlprovider = new ORACLESQLProvider();
	}
	public static String getSqlproviderStr(String dataBaseType) {
		if(!StringUtil.isValid(dataBaseType))
			return "";
		if("Microsoft SQL Server".equalsIgnoreCase(dataBaseType))
			return "sqlserver";
		if("MySQL".equalsIgnoreCase(dataBaseType))
			return "mysql";
		if("Oracle".equalsIgnoreCase(dataBaseType))
			return "oracle";
		return null;
	}
	public static ISQLProvider getSqlProvider(Connection con) throws SQLException {
		if(null == con)
			return null;
		String s = con.getMetaData().getDatabaseProductName();
		if(!StringUtil.isValid(s))
			return null;
		if("Microsoft SQL Server".equalsIgnoreCase(s))
			return new MSSQLProvider();
		if("MySQL".equalsIgnoreCase(s))
			return new MYSQLProvider();
		if("Oracle".equalsIgnoreCase(s))
			return new ORACLESQLProvider();
		return null;
	}
	public Map<String,Object> getUpdatePropertySqlWithMap(Map<String,Object> properMap) throws Exception{
		StringBuilder sb = new StringBuilder();
		Map<String,Object> sqlAndParamMap = new HashMap<String,Object>();
		String mdelStr = ("update "+getEntityName()+" set ");
		List<Object> paramvLis = new ArrayList<Object>();
		ExtendedField texf = null;
		for(Entry<String,Object> entry:properMap.entrySet()){
			texf = exFieldMap.get(entry.getKey().toLowerCase());
			if(null == texf||!texf.needORM||texf.columnName.equals(this.entityId))
				continue;
			sb.append(entry.getKey()).append(" = ?,");
			paramvLis.add(entry.getValue());
		}
		if(sb.lastIndexOf(",")!=-1)
			sb.replace(sb.length()-1,sb.length(),"");
		sb.append(" where 1=1 ");
		sqlAndParamMap.put(SQLPARAM, paramvLis);
		sqlAndParamMap.put(SQL, mdelStr+sb.toString());
		return sqlAndParamMap;
	}
	public Param getParam() {
		return paramLC.get();
	}
	public void setParam(Param param) {
		paramLC.set(param);
	}
	public String getEntityName() {
		if(!StringUtil.isValid(entityName)&&null != entityClass){
			entityName = AnnotationParser.getNameFormEntity(entityClass);
			//entityId = AnnotationParser.getNameFormId(updateEntityClass);
		}
		return entityName;
	}
	public String getEntityId() {
		if(!StringUtil.isValid(entityId)&&null != entityClass){
			entityId = AnnotationParser.getNameFormId(entityClass);
		}
		return entityId;
	}
	public String getEntityAlias() {
		return entityAlias;
	}
	public String getMastAliasFormJsoin(){
		return entityAlias;//+"_";
	}
	public Map<String, ReflectField> getReflectFieldClassMap() {
		return reflectFieldClassMap;
	}
	public Map<String, ExtendedField> getExFieldMap() {
		return exFieldMap;
	}
	public Map<String, ReflectField> getReflectSelectFieldMap() {
		return reflectSelectFieldMap;
	}
	public static String fetchRefColumnFinalEntityName(ReflectField refObj) {
		String c = refObj.entity().getSimpleName();
		if(StringUtil.isValid(refObj.entityFullName())) {
			c = refObj.entityFullName();
			if(c.indexOf(".") != -1)
				c = c.substring(c.lastIndexOf(".")+1,c.length());
		}
		return c;
	}
	public String proRefFieldNameFromResult(ReflectField refObj,String resColumnName){
		String c = fetchRefColumnFinalEntityName(refObj);
		if(resColumnName.indexOf(c+"_") != -1)
			return resColumnName = resColumnName.replaceAll(c+"_","");
		else if(null != refObj.alias()&&resColumnName.startsWith(refObj.alias()))
			return resColumnName = resColumnName.replaceAll(refObj.alias(),"");
		return resColumnName;
	}
	public void resetFields(){
		if(null == origiFields||origiFields.length<=0)
			return;
		for(int i=0;i<origiFields.length;i++)
			fields[i] = origiFields[i];
	}
	public String[] addexcludeUpFields(String[] origexcfieds){
		if(null == origexcfieds||origexcfieds.length<=0)
			return noupfields;
		int l = origexcfieds.length;
		String[] n = new String[l+noupfields.length];
		int i=0;
		for(;i<l;i++){
			n[i] = origexcfieds[i];
		}
		int j = 0;
		l = noupfields.length;
		for(;j<l;j++,i++){
			n[i] = noupfields[j];
		}
		return n;
	}
	public static String proSqlDataOwner(String sql,String dw) {
		if(sql.indexOf(BaseEntity.DATAOWNER) != -1)
			return sql;
		//如果是存储过程
		if(sql.indexOf("exec") != -1||sql.indexOf("select") == -1)
			return sql;
		sql = sql.replaceAll("\n", "");
		//sql = sql.replaceAll("\t", "");
		sql = sql.replaceAll("\r", "");
		String lowsql = sql.toLowerCase();
		if(lowsql.lastIndexOf("where")== -1)
			sql+=" where 1=1 ";
		String s = sql.substring(lowsql.indexOf("from")+"from".length());

		String[] table_alias = fetchTableNameAndAlias(sql);
		String etname = table_alias[1];
		String fname = BaseEntity.DATAOWNER;

		if(s.indexOf(etname+"_") != -1)
			etname = etname+"_";
		if(null != etname)
			fname = etname+"."+BaseEntity.DATAOWNER;

		int whereindex = lowsql.indexOf("where")+"where".length();
		String wheresql = sql.substring(0,whereindex);
		String beforwheresql = lowsql.substring(0,whereindex);
		String afterwheresql = lowsql.substring(whereindex);

		String tempwhere = lowsql.substring(sql.indexOf(table_alias[1]),whereindex);
		int left = StringUtil.countStr(tempwhere,"(");
		int right = StringUtil.countStr(tempwhere,")");
		while(left != right) {
			lowsql = beforwheresql.replaceAll("where", "there")+afterwheresql;
			whereindex = lowsql.indexOf("where")+"where".length();

			beforwheresql = lowsql.substring(0,whereindex);
			afterwheresql = lowsql.substring(whereindex);
			tempwhere = lowsql.substring(sql.indexOf(table_alias[1]),whereindex);

			left = StringUtil.countStr(tempwhere,"(");
			right = StringUtil.countStr(tempwhere,")");
		}
		wheresql = sql.substring(0,whereindex);
		String afterwhere = sql.substring(wheresql.length(), sql.length());

		if(null == dw)
			dw = "?";
		sql=wheresql+" "+fname+" = '"+dw+"' and " +afterwhere;
		sql = sql.replaceAll("from", "  from  ");

		return sql;
	}

	public static String getSqlTableName(String sql) {
		String[] tablename_alias = fetchTableNameAndAlias(sql);
		return null != tablename_alias&&tablename_alias.length>0?tablename_alias[1]:null;
	}
	public static String[] fetchTableNameAndAlias(String sql) {
		//2019-08-06 13:59 youg
		sql = sql.replaceAll("\n", "");
		sql = sql.replaceAll("\r", "");
		//end

		sql = sql.replaceAll("	", "");
		sql = leftTrim(sql);
		sql = sql.replaceAll("\n", "\n ");
		sql = sql.replaceAll("\r", "\r ");
		sql = sql.replaceAll("\t", "\t ");
		sql = sql.replaceAll("\n", "");
		sql = sql.replaceAll("\r", "");
		sql = sql.replaceAll("\t", "");
		sql = sql.replaceAll("  ", " ");
		sql=sql.replaceAll(", ", ",").replaceAll(" ,", ",");

		String lowsql = sql.toLowerCase();
		if(lowsql.equals("update")||lowsql.equals("insert")||lowsql.equals("delete"))
			return null;
		if(lowsql.lastIndexOf("where")== -1)
			sql+=" where 1=1 ";

		String indexstr = " from ";
		String spsymb = " ";
		if(lowsql.trim().startsWith("update")) {
			if(lowsql.indexOf(",from") == -1) {
				sql = sql.replaceAll(" SET "," set ");
				String[] setplit = sql.split(" set ");
				String tableName = setplit[0].split(" ")[1];
				return new String[] {tableName,tableName};
			}
			indexstr = "update ";
			//if(lowsql.indexOf("where") != -1) {
			if(lowsql.indexOf("from") != -1) {
				//String where = lowsql.substring(0,lowsql.indexOf("where"));
				//if(null != where&&where.indexOf("from") != -1&&where.indexOf("(") == -1&&where.indexOf("(")<where.lastIndexOf("from"))
				indexstr = "from ";
			}
		}
		else if(lowsql.trim().startsWith("delete")) {
			indexstr = "delete ";
			String s = sql.substring(lowsql.indexOf(indexstr)+indexstr.length());
			s = leftTrim(s);
			if(s.startsWith("from "))
				indexstr = "from ";
		}else if(lowsql.trim().startsWith("insert")) {
			String[] sqls = sql.split("values");
			String tname = "";
			tname = sqls[0];
			if(sqls[0].indexOf("(") == -1) {
				tname = tname.substring(tname.indexOf("into")+4, tname.length());
			}else {
				tname = sqls[0];
				tname = tname.substring(tname.indexOf("into")+4, tname.indexOf("("));
			}
			tname = tname.trim();
			return new String[] {tname,tname};
		}

		String s = sql.substring(lowsql.indexOf(indexstr)+indexstr.length());
		//处理动态的FROM，如 SELECT XXX FROM （SELECT XXX，XXX FROM TABLENAME） FTABLE ... 2019-01-02 10:05 youg
		if(s.startsWith("(")) {
			int rightsymindex = s.indexOf(")");
			String s2 = s.substring(s.indexOf("(")+1,rightsymindex);
			while(s2.indexOf("(") != -1) {
				s = s.substring(rightsymindex+1,s.length());
				rightsymindex = s.indexOf(")");
				s2 = s.substring(0,rightsymindex);
			}
			s = s.substring(rightsymindex+1,s.length());
		}
		//end

		String[] sqls = s.split(spsymb);
		String entityName = null;
		String alias = null;
		if(null != sqls&&sqls.length>0){
			int i=0;
			for(String n:sqls){
				if(!StringUtil.isValid(n.trim()))
					continue;
				if("left".equals(n.toLowerCase())
						||"right".equals(n.toLowerCase())
						||"inner".equals(n.toLowerCase())
						||"where".equals(n.toLowerCase())
						||"group".equals(n.toLowerCase())
						||"order".equals(n.toLowerCase())
						||"set".equals(n.toLowerCase())
						||"(".equals(n.toLowerCase())
						||",".equals(n.toLowerCase())
						||",(select".equals(n.toLowerCase()))
					break;

				//if(StringUtil.isValid(n.trim())) {
				n = n.replaceAll("\\)", "");
				if(n.indexOf(",") != -1) {
					n = n.split(",")[0];
					if(null == entityName)
						entityName = n;
					alias = n;
					if(i>0&&i+1<sqls.length&&"as".equals(sqls[i+1])) {
						alias = sqls[i+1];
					}
					break;
				}
				if(null == entityName)
					entityName = n;
				alias = n;
				//}
				i++;
			}
		}
		return new String[] {entityName,alias};
	}
	/*去左空格*/
	public static String leftTrim(String str) {
		if (str == null || str.equals("")) return str;  return str.replaceAll("^[ ]+", "");
	}
	/*去右空格*/
	public static String rightTrim(String str) {
		if (str == null || str.equals("")) return str; return str.replaceAll("[ ]+$", "");
	}
	public static void main(String[] args) throws NoSuchFieldException, SecurityException {
		BaseEntity a = new BaseEntity();
		BaseEntity b = new BaseEntity();
		Field ff = b.getClass().getDeclaredField("estate");
		System.out.println(a.getClass().getClassLoader() == b.getClass().getClassLoader());
		System.out.println(ff.getDeclaringClass() == a.getClass());
		String sql = "select count(e.name) c,e.name orgName from ATDayData a \n" +
				" inner join EmployeeEntity e on e.id = a.employeeid ";
		//sql = "insert into RoleOrgs(id,roleId,orgId) values('077e9b5e-8f8b-4066-9424-56d082e2eb4d','d56b2f08-758b-4462-b9ae-3a3574518f3a','cced3c3c-161b-465f-a6a3-0157b16ee1dc')  ";
		//sql = "select b.name,quotaId,a.target,a.weight,a.remark from QuotaSolutionDetail a,Quota b where a.dataOwner=b.dataOwner ";
		sql = "		select e.* ,b.name comeFromNoName ,c.name marryStateNoName,o.name orgName  " +
				",j.jobName from EmployeeEntity e  " +
				"left join hrbasedataentity b on b.id=e.comeFromNo " +
				"left join  hrbasedataentity c  on c.id=e.marryStateNo " +
				"left join orgEntity o on o.id = e.partId " +
				"left join JobEntity j on j.id=e.quartersNo " +
				"where .dataOwner=$P{dataOwner}  and  e.id=  $P{employeeid}";

		sql = "update a set a.partId = b.id from EmployeeEntity a inner join OrgEntity b  on b.depCode = a.partId and b.dataOwner=a.dataOwner where b.depCode = a.partId";
		sql = "update NOCreator set uno = (select max(uno)+1 from NOCreator) where utype='EmployeeEntity' ";
		sql = "update orgEntity set name=(select c.name from GroupCompanyEntity ca where  c.id = '1' where id='2'";
		sql = "update orgEntity  set parentId=p.id from orgEntity sub ,\n" +
				"                                             (select id,depCode,dataOwner dw from orgEntity where dataOwner='dc57cd64-6fd1-4dfb-8cc3-e6a9b8b1679e') p\n" +
				"    where p.depCode = sub.parentNo and p.dw = sub.dataOwner  and ((sub.ecode !='0x000001' or sub.ulocked is null))  and dataOwner ='2d5234a3-a4ac-4ed2-8595-1397953ebc63'";
		sql = "select num,fromUid, empName,b64imgcode,emp.wimg wimg,orgName,emp.mobile mobile "
				+ " from ( select COUNT(1) num,fromUid    from MsgInfo   where toUid='13632546725' and isRead='0' group by fromUid ) msgNum "
				+ "left join  (select empName,b64imgcode,avatar_mediaid wimg,org.name orgName,e.mobile from EmployeeEntity e left join OrgEntity org on e.partId=org.id ) emp on fromUid=emp.mobile where 1=1";

		sql = "SELECT top 1 a.id FROM EmployeeEntity A,(select partId from EmployeeEntity  where id='18910735-f8c6-4f9c-8a0a-d25068d7c4f4') B\n" +
				" where  A.DutyNo in (select top 1 id from DutiesEntity  where dutiesName='课长') and A.PartID=B.PartID";

		sql = "select *\n" +
				"from (select empName,\n" +
				"             empNo,\n" +
				"             CAST(DATEPART(YEAR, submitDate) as varchar(4)) + '-' +\n" +
				"             right('00' + cast(DATEPART(MONTH, submitDate) as varchar(2)), 2) 月份,\n" +
				"             isnull(sum(leaveLength), 0)                                      请假时长,\n" +
				"             b.typeName\n" +
				"      from ATHolidayMaintenance a\n" +
				"             left join ATHolidayType b on b.id = a.holidayTypeId\n" +
				"             left join EmployeeEntity c on c.id = a.employeeid\n" +
				"      where  a.submitDate between ? and ?\n" +
				"      group by empName, empNo, CAST(DATEPART(YEAR, submitDate) as varchar(4)) + '-' +\n" +
				"                               right('00' + cast(DATEPART(MONTH, submitDate) as varchar(2)), 2), typeName) t pivot (sum(\n" +
				"    请假时长) for t.typeName in ([事假], [年假], [调休], [病假], [停工待料], [出差], [产假], [陪产假], [工伤假])) as ourpivot\n" +
				"order by 月份 desc, empName desc";

		sql = "update emp set empTypeNo=j.id,setki=0, from EmployeeEntity as emp ,(select id,name from HRBaseDataEntity where parentItemId='PS132') j where emp.empTypeNo=j.name and emp.empTypeNo is not null";
		sql = "update PositionChildEntity set resumeNum=(select count(1) from ResumeEntity res where PositionChildEntity.id = res.positionId) where 1=1";
		sql = "update SYSReportCentre set reportFile = ?,lastModifyMan = ?,ReportTemplateType = ?,lastModifyDate = ?,dataOwner = ? where 1=1 ";
		String[] set_split = sql.split(" set ");

		sql = sql.replaceAll("	", "");
		sql = leftTrim(sql.replaceAll("  ", " "));

		String[] et = fetchTableNameAndAlias(sql);
		System.out.println("tablename:"+et[0]+" : "+et[1]);
		System.out.println(proSqlDataOwner(sql,"123"));
	}
}

