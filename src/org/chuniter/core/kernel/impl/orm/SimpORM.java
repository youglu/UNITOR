package org.chuniter.core.kernel.impl.orm;

import static org.chuniter.core.kernel.impl.orm.JavaAndJDBCTransformer.resultGetValueWithType;
import static org.chuniter.core.kernel.impl.orm.JavaAndJDBCTransformer.resultSetValueWithTypeProArray;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.chuniter.core.kernel.KernelActivitor;
import org.chuniter.core.kernel.annotation.ReflectField;
import org.chuniter.core.kernel.api.authorization.IAuthorization;
import org.chuniter.core.kernel.api.dao.IConnectioinFactory;
import org.chuniter.core.kernel.api.dao.IConnectionExecutor;
import org.chuniter.core.kernel.api.dao.IConnectionHandler;
import org.chuniter.core.kernel.api.dao.IDaoConfiguer;
import org.chuniter.core.kernel.api.orm.ISQLProvider;
import org.chuniter.core.kernel.api.orm.ISimpORM;
import org.chuniter.core.kernel.api.unit.IDoSQL;
import org.chuniter.core.kernel.impl.authorization.AbstractBaseAuthorization;
import org.chuniter.core.kernel.impl.dao.DaoImplAdaptor;
import org.chuniter.core.kernel.impl.dao.GeneralDao;
import org.chuniter.core.kernel.kernelunit.*;
import org.chuniter.core.kernel.model.BaseEntity;
import org.chuniter.core.kernel.model.BaseUserEntity;
import org.chuniter.core.kernel.model.CustomField;
import org.chuniter.core.kernel.model.Extendable;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

public abstract class SimpORM<T extends Extendable> extends GeneralDao<T> implements ISimpORM<T>{ 
	
	//protected Class<T> clazz = null;
	private Class<T> _clazz = null;
	private ThreadLocal<Class<T>> clazz = new ThreadLocal<Class<T>>();
	private Class<T> originalClazz = null;
	protected IConnectionHandler connectionHandler = null;
	private ReflectUtil refu =ReflectUtil.getInstance();
	
	public SimpORM(){ 
		log.info("\n实例化" + super.getClass().getName() + "...\n");
		fetchGClass();
	}
	private Class<T> fetchGClass(){
		this._clazz = (Class<T>)getSuperClassGenricType(super.getClass());
		clazz.set(this._clazz);
		originalClazz = (Class<T>)getSuperClassGenricType(super.getClass());
		if(null != _clazz&&_clazz.getSimpleName().equals(Object.class.getSimpleName())){
			this._clazz = (Class<T>)getSuperClassGenricType(this.getClass().getSuperclass());
			clazz.set(this._clazz);
			originalClazz = (Class<T>)getSuperClassGenricType(this.getClass().getSuperclass());
		}	
		return originalClazz;
	}
	protected Class<?> getSuperClassGenricType(Class<?> clazz) { 
		Type genType = clazz.getGenericSuperclass();
		if (!(genType instanceof ParameterizedType)) {
			log.info(clazz.getName() + "的父类没有设置泛型参数！");
			return Object.class;
		}
		Type[] params = ((ParameterizedType) genType).getActualTypeArguments();
		if (params.length <= 0) {
			log.debug(clazz.getName() + "的泛型参数个数为： " + params.length);
			return Object.class;
		}
		if (!(params[0] instanceof Class)) {
			log.debug(clazz.getName() + " 没有设置可用的泛型参数！");
			return Object.class;
		}
		return ((Class<?>) params[0]);
	}
	
	public Serializable create(final T t) throws Exception {	
		final EntityMapping emp = ORMControler.getInstance(this).fetchMappingInfo(this);			
		return create(emp,t);
	}
	public Serializable create(final EntityMapping emp,final T t) throws Exception {	
		connectionHandler = getDBCon();
		try{
		return (Serializable) connectionHandler.proConnection(new IConnectionExecutor<Serializable>(){ 
			@Override
			public Serializable doConnection(Connection con)
					throws Exception {
				Map<String,Object> sqlAndParamMap = emp.getCreateSQL(t);
				List<Object> sqlParam = (List<Object>)sqlAndParamMap.get(EntityMapping.SQLPARAM);
				PreparedStatement pst = con.prepareStatement(sqlAndParamMap.get(EntityMapping.SQL).toString());
				
				PreparedStatement maxsta = null;
				ResultSet maxrs = null;
				Map<Integer,Object> paramvMap = null;
				int mj = sqlParam.size();
				for(int j=0,i=0;i<mj;i++){ 
					Object oi = sqlParam.get(i) ;
					if(oi != null && oi.toString().equals(EntityMapping.AUTONOSYB)){
						if(null == paramvMap)
							paramvMap = (Map<Integer, Object>) sqlAndParamMap.get(EntityMapping.SQLPARAMMAP);
						ExtendedField extf = (ExtendedField) paramvMap.get(new Integer(i));
						String maxcreatesql = emp.getMaxAutoNosSql(extf);
						String dw = null;
						if(t instanceof BaseEntity&&StringUtil.isValid(((BaseEntity)t).getDataOwner())){
							dw = ((BaseEntity)t).getDataOwner();
							if(StringUtil.isValid(dw))
								maxcreatesql += " where dataOwner=?";
						}
						if(null == dw) {
							dw = fetchCurrentDataOwner();
							if(StringUtil.isValid(dw))
								maxcreatesql += " where dataOwner=?";
						}
						if(null == maxsta)
							maxsta = con.prepareStatement(maxcreatesql);
						if(StringUtil.isValid(dw))
							maxsta.setString(1, dw);
						maxrs = maxsta.executeQuery();
						maxrs.next();
						int idc = maxrs.getInt(1);
						maxrs.close();
						try{
							ReflectUtil.getInstance().setFieldValues(t,extf.columnName, (idc+1));
						}catch(Exception fex){
							log.error("设置自动编号属性发生异常:"+fex.getMessage());
						}
						pst.setInt(j+1, (idc+1));
						j++;
					}else						
						j=resultSetValueWithTypeProArray(sqlParam.get(i),pst,j,emp.getSqlprovider());
				}
				if(null != maxsta){
					maxsta.close();
					maxsta = null;
				}
				pst.execute();
				return (Serializable) t;
			}});
		}catch(Exception e){
			log.error("创建实体"+t.getClass()+"失败:"+e.getMessage());
			throw e;
		}
	}
	@Override
	public Serializable createEntity(final Object t) throws Exception{ 
			//changeTempClass(t.getClass()); 
		try{ 
			final EntityMapping emp = ORMControler.getInstance(this).fetchMappingInfo(this,t.getClass());			
			connectionHandler = getDBCon();
			return (Serializable) connectionHandler.proConnection(new IConnectionExecutor<Serializable>(){
	
				@Override
				public Serializable doConnection(Connection con) throws Exception {
					String sql = "";
					try {
						Map<String,Object> sqlAndParamMap = emp.getCreateSQL(t);
						List<Object> sqlParam = (List<Object>)sqlAndParamMap.get(EntityMapping.SQLPARAM);
						Map<Integer,Object> paramvMap = (Map<Integer, Object>) sqlAndParamMap.get(EntityMapping.SQLPARAMMAP);
						sql = sqlAndParamMap.get(EntityMapping.SQL).toString();
						log.debug("sql:"+sql);
						PreparedStatement pst = con.prepareStatement(sql);
						
						PreparedStatement maxsta = null;
						ResultSet maxrs = null;
						ExtendedField extf = null;
						int pl = sqlParam.size();
						for(int j=0,i=0;i<pl;i++){ 
							extf = (ExtendedField) paramvMap.get(new Integer(i)); 
							Object o1 = sqlParam.get(i);
							if( o1 != null && o1.toString().equals(EntityMapping.AUTONOSYB)){ 
								String dw = null;
								String maxcreatesql = emp.getMaxAutoNosSql(extf);
								if(t instanceof BaseEntity){
									dw = ((BaseEntity)t).getDataOwner();
									if(StringUtil.isValid(dw))
										maxcreatesql += " where dataOwner=? ";
								}
								if(null == maxsta){maxsta = con.prepareStatement(maxcreatesql);}
								if(StringUtil.isValid(dw))
									maxsta.setString(1, dw);
								
								maxrs = maxsta.executeQuery();
								maxrs.next();
								int idc = maxrs.getInt(1);
								maxrs.close();
								try{ReflectUtil.getInstance().setFieldValues(t,extf.columnName, (idc+1));}catch(Exception fex){
									log.error("设置自动编号属性发生异常:"+fex.getMessage());}
								j++;
								pst.setInt(j, (idc+1));
							}else						
								try{  
									j=resultSetValueWithTypeProArray(sqlParam.get(i),pst,j,emp.getSqlprovider(),extf.field.getType()); 
								}catch(SQLException fex){fex.printStackTrace();}
						}	
						pst.execute();
						return (Serializable) t;
					}catch(Exception e) {
						System.err.println("执行SQL异常:"+e.getMessage()+"\n"+sql);
						throw e;
					}
				}});
		}catch(Exception e){
			log.error("创建实体:"+t.getClass()+"失败:"+e.getMessage());
			throw e;
		}finally{
			this.resetEntityClass();
		}		
	}	
	@SuppressWarnings("unchecked")
	@Override
	public T find(final Serializable id) throws Exception {
		final EntityMapping emp = ORMControler.getInstance(this).fetchMappingInfo(this);	
		connectionHandler = getDBCon();
		return (T) connectionHandler.proConnection(new IConnectionExecutor<T>(){

			@Override
			public T doConnection(Connection con)
					throws Exception {
				T t = null;						
				String sql = emp.getFindSql();
				sql+=" and id=?";
				PreparedStatement st = con.prepareStatement(sql);
				st.setString(1,id.toString());
				ResultSet result = st.executeQuery();
				t = clazz.get().newInstance();
				ExtendedField[] cfields = emp.getFields();
				ISQLProvider sqlp = emp.getSqlprovider();
				int haveData = 0;
				while(result.next()){
					haveData = 1;
					for(ExtendedField efd:cfields){
						if(!efd.needORM)
							continue;
						refu.setFieldValues(t,efd.field,JavaAndJDBCTransformer.resultGetValueWithType(efd,result,sqlp));
					}
				}
				if(haveData <=0)
					t = null;
				return t;
			}});
	}
	@SuppressWarnings("unchecked")
	@Override
	public List<T> find() throws Exception {		
		
		final EntityMapping emp = ORMControler.getInstance(this).fetchMappingInfo(this);	
		connectionHandler = getDBCon();
		if(null == connectionHandler){
			log.error("无法获得 connectionHandler 对象");
			return null;
		}
		return (List<T>) connectionHandler.proConnection(new IConnectionExecutor<List<T>>(){

			@Override
			public List<T> doConnection(Connection con)
					throws Exception {
				List<T> tempLis = new ArrayList<T>();
				String sql = emp.getFindSql();
				log.debug("输出查询sql"+sql);
				PreparedStatement st = con.prepareStatement(sql);
				ResultSet result = st.executeQuery();
				T t = null;
				ExtendedField[] cfields = emp.getFields();
				ISQLProvider sqlp = emp.getSqlprovider(); 
				Map<String,Byte> cmap = getEntityTableColumnMap(result);
				while(result.next()){
					t = clazz.get().newInstance();
					for(ExtendedField efd:cfields){
						if(!efd.needORM||!cmap.containsKey(efd.columnName.toLowerCase()))
							continue;
						try{ 
							refu.setFieldValues(t,efd.field,resultGetValueWithType(efd,result,sqlp));
						}catch(SQLException columnex){System.err.println(this.getClass()+"设置属性值异常:"+columnex.getMessage());}
					}
					tempLis.add(t);
				}
				return tempLis;
			}}); 
	}
	protected Map<String,Byte> getEntityTableColumnMap(ResultSet result) throws SQLException{
		ResultSetMetaData metaDate = result.getMetaData();
		int columnCount = metaDate.getColumnCount();
		Map<String,Byte> cmap = new HashMap<String,Byte>();
		for(int i=1;i<=columnCount;i++)
			cmap.put(metaDate.getColumnName(i).toLowerCase(), Byte.valueOf(((byte) 0x1)));
		return cmap;
	} 
	@Override
	public <S> List<S> findEntitys(final Class<S> clz,final Param param) throws Exception{
		//changeTempClass(clz);
		Connection con = null;	
		String sql = null;

		try{ 
			final EntityMapping emp = ORMControler.getInstance(this).fetchMappingInfo(this,clz);	
			connectionHandler = getDBCon();
			con = connectionHandler.getCon();
			List<S> tempLis = new ArrayList<S>();
			sql = emp.getFindSql();
			Map<String,Object> m = (Map<String,Object>)param.getCriteria(sql);
			sql+= m.get(EntityMapping.SQL)==null?"":m.get(EntityMapping.SQL).toString();
			String groupsql = m.get(EntityMapping.SQLGROUP)==null?"":m.get(EntityMapping.SQLGROUP).toString();
			String ordersql = m.get(EntityMapping.SQLORDER)==null?"":m.get(EntityMapping.SQLORDER).toString();
			sql+=ordersql+groupsql;
			//log.debug("输出查询sql："+sql);
			PreparedStatement pst = con.prepareStatement(sql);
			List<Object> sqlParam = (List<Object>)m.get(EntityMapping.SQLPARAM);
			if(null != sqlParam&&sqlParam.size()>0)
				for(int j=0,i=0;i<sqlParam.size();i++){
					j=resultSetValueWithTypeProArray(sqlParam.get(i),pst,j,emp.getSqlprovider());
				}
			ResultSet result = pst.executeQuery();
			S t = null;
			ExtendedField[] cfields = emp.getFields();
			/*log.debug("tmd class"+clz);
			for(ExtendedField exf:cfields){
				log.debug("tmd felds"+exf.field.getType().getClass()+"  "+exf.field.getName());
			}*/
			ISQLProvider sqlp = emp.getSqlprovider();
			Map<String,Byte> cmap = getEntityTableColumnMap(result);
			while(result.next()){
				t = clz.newInstance();
				for(ExtendedField efd:cfields){
					if(!efd.needORM||!cmap.containsKey(efd.columnName.toLowerCase()))
						continue;
					try{refu.setFieldValues(t,efd.field,resultGetValueWithType(efd,result,sqlp));}catch(SQLException csqle){System.err.println(this.getClass()+" 从结果集获得值出错："+csqle.getMessage());}
				}
				tempLis.add(t);
			}				
			return tempLis;
		}catch(Exception e){
			log.error("查询实体发生异常:"+sql);
			throw e;
		}finally{
			this.resetEntityClass();
			if(null != con)
				con.close();
		}
	}
	@Override
	public <S> S findEntity(Class<S> clz, Param param) throws Exception {
		/*changeTempClass(clz);
		Connection con = null;		
		try{ 
			final EntityMapping emp = ORMControler.getInstance(this).fetchMappingInfo(this,clz);	
			connectionHandler = getDBCon();
			con = connectionHandler.getCon(); 
			List<Object> tempLis = new ArrayList<Object>();
			String sql = emp.getFindSql();			
			Map<String,Object> sqlAndParamMap = (Map<String,Object>)param.getCriteria(sql);
			sql+= sqlAndParamMap.get(EntityMapping.SQL)==null?"":sqlAndParamMap.get(EntityMapping.SQL).toString();
			log.debug("输出查询sql："+sql);
			PreparedStatement pst = con.prepareStatement(sql);
			List<Object> sqlParam = (List<Object>)sqlAndParamMap.get(EntityMapping.SQLPARAM);
			if(null != sqlParam&&sqlParam.size()>0)
				for(int j=0,i=0;i<sqlParam.size();i++){
					j=resultSetValueWithTypeProArray(sqlParam.get(i),pst,j,emp.getSqlprovider());
				}
			ResultSet result = pst.executeQuery();
			Object t = null;
			ExtendedField[] cfields = emp.getFields();
			log.debug("tmd class"+clz);
			for(ExtendedField exf:cfields){
				log.debug("tmd felds"+exf.field.getClass());
			}
			ISQLProvider sqlp = emp.getSqlprovider();
			Map<String,Byte> cmap = getEntityTableColumnMap(result);
			while(result.next()){
				t = clz.newInstance();
				for(ExtendedField efd:cfields){
					if(!efd.needORM||!cmap.containsKey(efd.columnName.toLowerCase()))
						continue;
					try{refu.setFieldValues(t,efd.field,resultGetValueWithType(efd,result,sqlp));}catch(SQLException sqle){sqle.printStackTrace();}
				}
				return (T)t;
			}	 			
		}catch(Exception e){
			throw e;
		}finally{
			this.resetEntityClass();
			if(null != con)
				con.close();
		}
		return null;*/
		 List<S> entitys = this.findEntitys(clz, param);
		 if(null == entitys||entitys.isEmpty())
			 return null;
		 S t =  entitys.get(0);
		 entitys.clear();
		 entitys = null;
		 return t;
	}
	public PageIterator<?> findEntitys(Class<?> clz,final Param param,final int startIndex,final int pageSize) throws Exception{
		//changeTempClass(clz);
		try{			 
			 //final EntityMapping emp = ORMControler.getInstance(this).fetchMappingInfo(this); 	 
			 //return this.find(emp, param, startIndex, pageSize);
			 return findEntitys(clz,param,startIndex,pageSize,null);
			/*connectionHandler = getDBCon();
			return (PageIterator<?>)connectionHandler.proConnection(new IConnectionExecutor<PageIterator<?>>(){
				
				@Override
				public PageIterator<?> doConnection(Connection con)
						throws Exception {
					String countSql = emp.getEntityCountSQL();
					Map<String,Object> sqlAndParamMap = (Map<String,Object>)param.getCriteria(countSql);		
					
					countSql+= sqlAndParamMap.get(EntityMapping.SQL)==null?"":sqlAndParamMap.get(EntityMapping.SQL).toString();				
					PreparedStatement pst = con.prepareStatement(countSql);
					log.debug("输出分页前的统计sql:"+countSql);
					List<String> sqlParam = (List<String>)sqlAndParamMap.get(EntityMapping.SQLPARAM);
					if(null != sqlParam&&sqlParam.size()>0)
						for(int j=0,i=0;i<sqlParam.size();i++){
							j=resultSetValueWithTypeProArray(sqlParam.get(i),pst,j,emp.getSqlprovider());
						}
					ResultSet result = pst.executeQuery();
					Long totalCount = 0l;
					while(result.next()){
						totalCount = result.getLong(1);
					}
					if(totalCount <= 0)
						return PageIteratorUtil.NULL;
					
					//有数据
					String pageSql = emp.getSqlprovider().getPageSql(emp,param, startIndex, pageSize);	
					log.debug("输出分页sql:"+pageSql);
					pst = con.prepareStatement(pageSql);		 
					if(null != sqlParam&&sqlParam.size()>0)
						for(int j=0,i=0;i<sqlParam.size();i++){ 
							j=resultSetValueWithTypeProArray(sqlParam.get(i),pst,j,emp.getSqlprovider());
						}
					result = pst.executeQuery();
					
					Object t = null;
					ExtendedField[] cfields = emp.getFields();		
					List<Object> tempLis = new ArrayList<Object>();
					ISQLProvider sqlp = emp.getSqlprovider();
					while(result.next()){
						t = clazz.newInstance();
						for(ExtendedField efd:cfields){
							if(!efd.needORM)
								continue;
							refu.setFieldValues(t,efd.field,resultGetValueWithType(efd,result,sqlp));
						}
						tempLis.add(t);
					}
					PageIterator<?> page = new PageIterator<Object>(tempLis,totalCount.intValue(),pageSize,startIndex);
					return page;
				}});
			return this.find(param, startIndex, pageSize);*/
		}catch(Exception e){
			throw e;
		}finally{
			this.resetEntityClass();
		}
	}
	public PageIterator<?> findEntitys(final Class<?> clz,final Param param,final int startIndex,final int pageSize,Class<?>[] joinClass) throws Exception{
		//changeTempClass(clz);
		try{		 
			final EntityMapping emp = ORMControler.getInstance(this).fetchMappingInfo(this,clz);
			connectionHandler = getDBCon();
			return (PageIterator<?>)connectionHandler.proConnection(new IConnectionExecutor<PageIterator<?>>(){
				
				@Override
				public PageIterator<?> doConnection(Connection con)
						throws Exception {
					String pageSql = "";
					String countSql = "";
					try {
						countSql = emp.getEntityCountSQL();
						Map<String,Object> sqlAndParamMap = (Map<String,Object>)param.getCriteria(countSql);		
						
						countSql+= sqlAndParamMap.get(EntityMapping.SQLCOUNTPARAM)==null?"":sqlAndParamMap.get(EntityMapping.SQLCOUNTPARAM).toString();				
						if(!countSql.startsWith("select count(-1)"))
							countSql = "select count(-1) from("+countSql+") c_count";
						PreparedStatement pst = con.prepareStatement(countSql);
						log.debug("输出分页前的统计sql:"+countSql);
						//System.out.println("输出分页前的统计sql:"+countSql);
						List<String> sqlParam = (List<String>)sqlAndParamMap.get(EntityMapping.SQLPARAM);
						if(null != sqlParam&&sqlParam.size()>0)
							for(int j=0,i=0;i<sqlParam.size();i++){
								j=resultSetValueWithTypeProArray(sqlParam.get(i),pst,j,emp.getSqlprovider());
							}
						ResultSet r = pst.executeQuery();
						Long totalCount = 0l;
						while(r.next())
							totalCount = r.getLong(1);
						if(totalCount <= 0)
							return new PageIteratorUtil().getNullPage();
						//有数据
						pageSql = emp.getSqlprovider().getPageSql(emp,param, startIndex, pageSize);	
						log.debug("输出分页sql:"+pageSql);
						//System.out.println("输出分页sql:"+pageSql);
						pst = con.prepareStatement(pageSql);		 
						if(null != sqlParam&&sqlParam.size()>0)
							for(int j=0,i=0;i<sqlParam.size();i++){ 
								j=resultSetValueWithTypeProArray(sqlParam.get(i),pst,j,emp.getSqlprovider());
							}
						r = pst.executeQuery();
						
						Object t = null;
						ExtendedField[] cfields = emp.getFields();		
						List<Object> tempLis = new ArrayList<Object>();
						ISQLProvider sqlp = emp.getSqlprovider();
						Map<String,Byte> cmap = getEntityTableColumnMap(r);
						while(r.next()){
							t = clz.newInstance();
							for(ExtendedField efd:cfields){
								if(!efd.needORM||!cmap.containsKey(efd.columnName.toLowerCase()))
									continue;
								refu.setFieldValues(t,efd.field,resultGetValueWithType(efd,r,sqlp));
							}
							tempLis.add(t);
						}
						PageIterator<?> page = new PageIterator<Object>(tempLis,totalCount.intValue(),pageSize,startIndex);
						return page;
					}catch(Exception e) {
						System.out.println("查询异常:"+countSql+"\n\n"+pageSql);
						throw e;
					}finally {
						
					}
					
				}});			
		}catch(Exception e){
			throw e;
		}finally{
			this.resetEntityClass();
		}
	}	
	@SuppressWarnings("unchecked")
	@Override
	public void delete(final T paramT) throws Exception {
		final EntityMapping emp = ORMControler.getInstance(this).fetchMappingInfo(this);	
		connectionHandler = getDBCon();
		connectionHandler.proConnection(new IConnectionExecutor(){

			@Override
			public Object doConnection(Connection con)
					throws Exception {
				Map<String,Object> sqlAndParamMap = emp.delByEntityPropertySql(paramT);
				String delSql = sqlAndParamMap.get(EntityMapping.SQL).toString();
				List<String> sqlParam = (List<String>)sqlAndParamMap.get(EntityMapping.SQLPARAM);
				PreparedStatement pst = con.prepareStatement(delSql);
				for(int j=0,i=0;i<sqlParam.size();i++){ 
					j=resultSetValueWithTypeProArray(sqlParam.get(i),pst,j,emp.getSqlprovider());
				}			
				pst.executeUpdate();
				return null;
			}});
	}
	public void delete(final Param param) throws Exception { 
		//this.changeTempClass(this.originalClazz);
		EntityMapping emp = ORMControler.getInstance(this).fetchMappingInfo(this);	
		delete(emp,param);
	}	
	private void delete(final EntityMapping emp ,final Param param) throws Exception { 
		connectionHandler = getDBCon();
		connectionHandler.proConnection(new IConnectionExecutor<Object>(){
			@Override
			public Object doConnection(Connection con)
					throws Exception {
				String sql = emp.getDelAllSQL();
				Map<String,Object> sqlAndParamMap = (Map<String,Object>)param.getCriteria(sql);
				sql+= sqlAndParamMap.get(EntityMapping.SQL)==null?"":sqlAndParamMap.get(EntityMapping.SQL).toString();
				log.debug("输出删除sql："+sql);
				
				List<Object> sqlParam = (List<Object>)sqlAndParamMap.get(EntityMapping.SQLPARAM);
				PreparedStatement pst = con.prepareStatement(sql);
				for(int i=0,j=0;i<sqlParam.size();i++){
					j=resultSetValueWithTypeProArray(sqlParam.get(i),pst,j,emp.getSqlprovider());
				}			
				pst.executeUpdate();
				return null;
			}});
	}	
	@SuppressWarnings("unchecked")
	@Override
	public void delete(final Serializable delId) throws Exception {
		//this.changeTempClass(this.originalClazz);
		final EntityMapping emp = ORMControler.getInstance(this).fetchMappingInfo(this);	
		delete(emp,delId);
	}
	private void delete(final EntityMapping emp,final Serializable delId) throws Exception { 
		connectionHandler = getDBCon();
		connectionHandler.proConnection(new IConnectionExecutor(){ 
			@Override
			public Object doConnection(Connection con)
					throws Exception {
				String delSql = emp.getDelByIdSQL(); 
				PreparedStatement pst = con.prepareStatement(delSql);			 
				pst.setObject(1,delId);			 			
				pst.execute();
				return null;
			}});
	}
	@Override
	public List<T> find(String paramString) throws Exception {
		return null;
	}
	@Override
	public PageIterator<T> find(int paramInt1, int paramInt2) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}
	@SuppressWarnings("unchecked")
	@Override
	public List<T> find(final Param param) throws Exception { 
		final EntityMapping emp = ORMControler.getInstance(this).fetchMappingInfo(this);
		log.debug(emp.getEntityClass()+"  "+this.clazz.get()+" "+this.originalClazz);
		connectionHandler = getDBCon();
		return (List<T>)connectionHandler.proConnection(new IConnectionExecutor<List<T>>(){

			@Override
			public List<T> doConnection(Connection con)throws Exception {
				List<T> tempLis = new ArrayList<T>();
				String sql = null;
				try { 
					sql = emp.getFindSql();	
					Map<String,Object> sqlAndParamMap = (Map<String,Object>)param.getCriteria(sql);
					sql+= sqlAndParamMap.get(EntityMapping.SQL)==null?"":sqlAndParamMap.get(EntityMapping.SQL).toString();
					log.debug("输出查询sql："+sql);
					PreparedStatement pst = con.prepareStatement(sql);
					List<String> sqlParam = (List<String>)sqlAndParamMap.get(EntityMapping.SQLPARAM);
					if(null != sqlParam&&sqlParam.size()>0)
						for(int j=0,i=0;i<sqlParam.size();i++){
							j = resultSetValueWithTypeProArray(sqlParam.get(i),pst,j,emp.getSqlprovider());
						}
					ResultSet result = pst.executeQuery();
					T t = null;
					ExtendedField[] cfields = emp.getFields();
					ISQLProvider sqlp = emp.getSqlprovider();
					Map<String,Byte> cmap = getEntityTableColumnMap(result);
					while(result.next()){
						t = clazz.get().newInstance();
						for(ExtendedField efd:cfields){
							if(!efd.needORM||!cmap.containsKey(efd.columnName.toLowerCase()))
								continue;
							refu.setFieldValues(t,efd.field,resultGetValueWithType(efd,result,sqlp));
						}
						tempLis.add(t);
					}	
				}catch(Exception e) {
					log.error("发生异常，sql："+sql);
					throw e;
				}
				return tempLis;
			}});
	}
	@SuppressWarnings("unchecked")
	public synchronized PageIterator<T> find(final EntityMapping emp,final Param param,final int startIndex,final int pageSize)
			throws Exception {
		return this.find(emp, param, startIndex, pageSize,null);
	}
	@SuppressWarnings("unchecked")
	private synchronized PageIterator<T> find(final EntityMapping emp,final Param param,final int startIndex,final int pageSize,String[] excludeFields)
			throws Exception {
		Connection con = null;
		try{
			emp.setParam(param);
			connectionHandler = getDBCon();
			con = connectionHandler.getCon();
			String countSql = emp.getEntityCountSQL();
			Map<String,Object> sqlAndParamMap = (Map<String,Object>)emp.getParam().getCriteria(countSql);
			countSql+= sqlAndParamMap.get(EntityMapping.SQLCOUNTPARAM)==null?"":sqlAndParamMap.get(EntityMapping.SQLCOUNTPARAM).toString();				
			PreparedStatement pst = con.prepareStatement(countSql);
			log.debug("输出分页前的统计sql:"+countSql);
			List<String> sqlParam = (List<String>)sqlAndParamMap.get(EntityMapping.SQLPARAM);
			if(null != sqlParam&&sqlParam.size()>0)
				for(int j=0,i=0;i<sqlParam.size();i++){
					j=resultSetValueWithTypeProArray(sqlParam.get(i),pst,j,emp.getSqlprovider());
				}
			ResultSet result = pst.executeQuery();
			Long totalCount = 0l;
			while(result.next()){
				totalCount = result.getLong(1);
			}
			if(totalCount <= 0)
				return new PageIteratorUtil().getNullPage(); 
			
			//加入处理要查询的属性，从设置中获得属性,这里主要是当前用户的ID获得不是很好，需改进 youg 2016-05-17 17:37
			proEntityMapFieldInfo(emp);
			//end youg
			
			//有数据
			String pageSql = emp.getSqlprovider().getPageSql(emp,emp.getParam(), startIndex, pageSize);
			log.debug("输出分页sql:"+pageSql);
			pst = con.prepareStatement(pageSql);		 
			if(null != sqlParam&&sqlParam.size()>0)
				for(int j=0,i=0;i<sqlParam.size();i++){ 
					j=resultSetValueWithTypeProArray(sqlParam.get(i),pst,j,emp.getSqlprovider());
				}
			result = pst.executeQuery();
			T t = null;
			ExtendedField[] cfields = emp.getFields();		
			List<T> tempLis = new ArrayList<T>();
			ISQLProvider sqlp = emp.getSqlprovider();
			boolean skipField = false;
			Map<String,Byte> cmap = getEntityTableColumnMap(result);
			while(result.next()){
				t = clazz.get().newInstance(); 
				for(ExtendedField efd:cfields){
					skipField = false;
					if(null != excludeFields&&excludeFields.length>0){
						for(String exf:excludeFields){
							if(efd.field.getName().equalsIgnoreCase(exf)||efd.field.getName().equalsIgnoreCase(exf)){
								skipField = true;
								break;
							}
						}
					}
					if(skipField){
						log.debug("不加载属性值:"+efd.field.getName());
						continue;
					}	
					if(null != efd.exc&&efd.exc.distinct()){
						CustomField cf = new CustomField(efd.columnName+EntityMapping.FCOUNTEXTNAME,result.getInt(efd.columnName+EntityMapping.FCOUNTEXTNAME)); 
						cf.setFtype("int");
						t.setProperty(cf); 
					}
					if(!efd.needORM||!cmap.containsKey(efd.columnName.toLowerCase())){ 
						continue;
					}
					refu.setFieldValues(t,efd.field,resultGetValueWithType(efd,result,sqlp));
				}
				tempLis.add(t);
			}
			PageIterator<T> page = new PageIterator<T>(tempLis,totalCount.intValue(),pageSize,startIndex);
			return page;
		}catch(Exception e){
			e.printStackTrace();			
		}finally{
			if(null != con){
				con.close();
			}
		}
		return null;
	}
	@SuppressWarnings("unchecked")
	@Override
	public synchronized PageIterator<T> find(final Param param,final int startIndex,final int pageSize)
			throws Exception {
		EntityMapping emp = ORMControler.getInstance(this).fetchMappingInfo(this);			 
		return this.find(emp, param, startIndex, pageSize);
	}
	@Override
	public PageIterator<T> find(Param param,int startIndex,int pageSize,String[] excludeFields) throws Exception{
		//this.changeTempClass(originalClazz);
		final EntityMapping emp = ORMControler.getInstance(this).fetchMappingInfo(this);			 
		return this.find(emp, param, startIndex, pageSize,excludeFields);
	}	
	@Override
	public PageIterator<T> findPageAsJoin(Param param, int startIndex,
			int pageSize)
			throws Exception {
		return findPageAsJoin(this._clazz,param,startIndex,pageSize);
	}
	@Override
	public <S extends Extendable> PageIterator<S> findPageAsJoin(Class<S> claz,Param param, int startIndex,
			int pageSize) throws Exception { 
		return findPageAsJoin(claz,param,startIndex,pageSize,true);
	}
	@Override
	public <S extends Extendable> PageIterator<S> findPageAsJoin(Class<S> claz,Param param, int startIndex, int pageSize,boolean applyEntitySet) throws Exception {
		Connection con = null;
		String pageSql = "";
		String countSql = "";
		try{
			//this.changeTempClass_(claz);
			final EntityMapping emp = ORMControler.getInstance(this).fetchMappingInfo(this,claz);
			emp.setParam(param);
			con = getDBCon().getCon();
			countSql = emp.getEntityCountWithJoinSQLFormParam();
			Map<String,Object> sqlAndParamMap = (Map<String,Object>)emp.getParam().getCriteria(null);
			String paramsql = sqlAndParamMap.get(EntityMapping.SQLCOUNTPARAM)==null?"":sqlAndParamMap.get(EntityMapping.SQLCOUNTPARAM).toString();
			if(countSql.indexOf("where 1=1") != -1)
				countSql = countSql+" "+paramsql;
			else
				countSql = countSql+" where 1=1 "+paramsql;
			PreparedStatement pst = con.prepareStatement(countSql);
			log.debug("输出分页前的统计sql:"+countSql);
			List<String> sqlParam = (List<String>)sqlAndParamMap.get(EntityMapping.SQLPARAM);
			if(null != sqlParam&&sqlParam.size()>0)
				for(int j=0,i=0;i<sqlParam.size();i++){
					j=resultSetValueWithTypeProArray(sqlParam.get(i),pst,j,emp.getSqlprovider());
				}
			ResultSet result = pst.executeQuery();
			Long totalCount = 0l;
			while(result.next())
				totalCount = result.getLong(1);
			if(totalCount <= 0)
				return new PageIteratorUtil().getNullPage();
			//加入处理要查询的属性，从设置中获得属性,这里主要是当前用户的ID获得不是很好，需改进 youg 2016-05-17 17:37
			//加入控制，因为有的查单个时，也用了这个方法 2016-06-07 15:35 youg
			if(applyEntitySet)
				proEntityMapFieldInfo(emp);
			//else
			//emp.resetFields();
			//end youg
			//end youg
			//有数据
			pageSql = emp.getSqlprovider().getPageSql(emp,emp.getParam(), startIndex, pageSize);
			if(log.isDebugEnabled())
				log.debug("输出分页sql:"+pageSql);
			pst = con.prepareStatement(pageSql);
			if(null != sqlParam&&sqlParam.size()>0)
				for(int j=0,i=0;i<sqlParam.size();i++){
					j=resultSetValueWithTypeProArray(sqlParam.get(i),pst,j,emp.getSqlprovider());
				}
			result = pst.executeQuery();
			S t = null;
			List<S> tempLis = new ArrayList<S>();
			ISQLProvider sqlp = emp.getSqlprovider();
			Map<String, ExtendedField> exfMap = emp.getExFieldMap();
			Map<String, ReflectField> reflectSelectFieldMap = emp.getReflectSelectFieldMap();
			ResultSetMetaData md = result.getMetaData();
			int maxColumns = md.getColumnCount();
			String cname = "";
			String columnLabel = "";//就是as后面的名称
			ExtendedField efd = null;
			String columnkey = "";
			String columnLablekey = "";
			ReflectField reffield = null;
			while(result.next()){
				t = claz.newInstance();
				for(int i=1;i<=maxColumns;i++){
					cname = md.getColumnName(i);//.toLowerCase();
					columnkey = cname.toLowerCase();
					if("sa12".equals(columnkey)){
						System.out.println(result.getDouble(columnkey));
					}
					columnLabel = md.getColumnLabel(i);//.toLowerCase();
					columnLablekey = columnLabel.toLowerCase();
					if(exfMap.containsKey(columnkey)&&cname.equals(columnLabel)){
						efd = exfMap.get(columnkey);
						refu.setFieldValues(t,efd.field,resultGetValueWithType(efd,result,sqlp));
						if(null != efd.exc&&efd.exc.distinct()){
							CustomField cf = new CustomField(efd.columnName+EntityMapping.FCOUNTEXTNAME,result.getInt(efd.columnName+EntityMapping.FCOUNTEXTNAME));
							cf.setFtype("int");
							t.setProperty(cf);
						}
					}else if(reflectSelectFieldMap.containsKey(columnLablekey)){
						reffield = reflectSelectFieldMap.get(columnLablekey);
						refu.setReflectValues(t,reffield,emp.proRefFieldNameFromResult(reffield,cname),result.getObject(i));
					}
				}
				tempLis.add(t);
			}
			PageIterator<S> page = new PageIterator<S>(tempLis,totalCount.intValue(),pageSize,startIndex);
			return page;
		}catch(Exception e){
			e.printStackTrace();
			log.error(e.getMessage()+"\n执行分页错误:"+pageSql+" \n统计SQL:\n"+countSql);
			System.err.println("执行分页错误:"+pageSql);
			throw e;
		}finally{
			this.resetEntityClass();;
			if(null != con){
				con.close();
			}
		}
	}
	@Override
	public Serializable save(T entity) throws Exception {		 
		return create(entity);
	}
	@Override
	public Serializable update(final T t) throws Exception {
		//this.changeTempClass(this.originalClazz);
		/*if(!t.getClass().getName().equals(this.originalClazz.getName())){
			this.originalClazz = (Class<T>) t.getClass();
			this._clazz =  (Class<T>) t.getClass();
			clazz.set(this._clazz);
		}*/
		final EntityMapping emp = ORMControler.getInstance(this).fetchMappingInfo(this,t.getClass());	
		return update(emp,t);
	}
	 
	private Serializable update(final EntityMapping emp ,final T t) throws Exception {	 
		connectionHandler = getDBCon();
		return (Serializable) connectionHandler.proConnection(new IConnectionExecutor<Serializable>(){

			@Override
			public Serializable doConnection(Connection con)
					throws Exception {
				Map<String,Object> sqlAndParamMap = emp.getUpdateSQL(t);
				List<Object> sqlParam = (List<Object>)sqlAndParamMap.get(EntityMapping.SQLPARAM);
				log.debug("update sql:"+sqlAndParamMap.get(EntityMapping.SQL).toString());
				PreparedStatement pst = con.prepareStatement(sqlAndParamMap.get(EntityMapping.SQL).toString());
				for(int j=0,i=0;i<sqlParam.size();i++){ 
					j=resultSetValueWithTypeProArray(sqlParam.get(i),pst,j,emp.getSqlprovider());
				}	
				int i = pst.executeUpdate();
				return i==1?(Serializable)t:null;
			}});
	}
	@Override
	public <S> S updateExcludeField(final S s,final String[] excludeFileNames_) throws Exception {	
		//this.changeTempClass(s.getClass());
		try{
		final EntityMapping emp = ORMControler.getInstance(this).fetchMappingInfo(this,s.getClass());	
		final String[]  excludeFileNames = emp.addexcludeUpFields(excludeFileNames_);
		connectionHandler = getDBCon();
		@SuppressWarnings("unchecked")
		S rs = (S) connectionHandler.proConnection(new IConnectionExecutor<S>(){

			@Override
			public S doConnection(Connection con)
					throws Exception {
				Map<String,Object> sqlAndParamMap = emp.getUpdateSQL(s,excludeFileNames);
				List<Object> sqlParam = (List<Object>)sqlAndParamMap.get(EntityMapping.SQLPARAM);
				PreparedStatement pst = con.prepareStatement(sqlAndParamMap.get(EntityMapping.SQL).toString());
				for(int j=0,i=0;i<sqlParam.size();i++){ 
					j=resultSetValueWithTypeProArray(sqlParam.get(i),pst,j,emp.getSqlprovider());
				}	
				int i = pst.executeUpdate();
				return i==1?(S)s:null;
			}});
		return rs;
		}catch(Exception e){
			throw e;
		}finally{
			this.resetEntityClass();
		}
	}	
	@Override
	public void updatePropertie(final Map<String,Object> properMap,final Param param) throws Exception {
		if(null == properMap || properMap.isEmpty())
			return ;
		//this.changeTempClass(this.originalClazz);
		final EntityMapping emp = ORMControler.getInstance(this).fetchMappingInfo(this);	
		this.updatePropertie(emp,properMap,param);
	}
	
	@Override
	public <S> void updatePropertie(Class<S> claz,Map<String,Object> properMap,Param param) throws Exception {
		if(null == properMap || properMap.isEmpty())
			return ;
		//changeTempClass(claz);
		try{
			final EntityMapping emp = ORMControler.getInstance(this).regeditEntity(claz);//ORMControler.getInstance(this).fetchMappingInfo(this);	
			this.updatePropertie(emp,properMap,param);
		}catch(Exception e){
			throw e;
		}finally{
			this.resetEntityClass();
		}
	} 
	private void updatePropertie(final EntityMapping emp,final Map<String,Object> properMap,final Param param) throws Exception {
		if(null == properMap || properMap.isEmpty())
			return ; 
		connectionHandler = getDBCon();
		connectionHandler.proConnection(new IConnectionExecutor<Serializable>(){ 
			@Override
			public Serializable doConnection(Connection con)
					throws Exception {
				String sql = null;
				try {
				Map<String,Object> sqlAndParamMap = emp.getUpdatePropertySqlWithMap(properMap);
				List<Object> upArgument = (List<Object>)sqlAndParamMap.get(EntityMapping.SQLPARAM);
				
				Map<String,Object> paramMap = (Map<String,Object>)param.getCountCriteria(sqlAndParamMap.get(EntityMapping.SQL));
				List<Object> paramArgument = (List<Object>)paramMap.get(EntityMapping.SQLPARAM);
				
				sql = sqlAndParamMap.get(EntityMapping.SQL).toString()+" "+paramMap.get(EntityMapping.SQL).toString();
				PreparedStatement pst = con.prepareStatement(sql);
				log.debug("更新属性sql：\n"+sql);
				int j=0;
				for(int i=0;i<upArgument.size();i++){
					j=resultSetValueWithTypeProArray(upArgument.get(i),pst,j,emp.getSqlprovider());
				}
				for(int i=0;i<paramArgument.size();i++){
					j=resultSetValueWithTypeProArray(paramArgument.get(i),pst,j,emp.getSqlprovider());
				}	
				pst.executeUpdate();
				}catch(Exception e) {
					e.printStackTrace();
					System.err.println("更新属性发生异常:\n "+sql);
					throw e;
				}
				return null;
			}});
	}
	
	public  void updatePropertie(Class<?> s,Map<String,Object> properMap,String id) throws Exception {
		this.changeTempClass_(s);
		try{final EntityMapping emp = ORMControler.getInstance(this).regeditEntity(s);
		this.updatePropertie(emp, properMap, id);
		}catch(Exception e){
			throw e;
		}finally{
			this.resetEntityClass();
		} 
	} 
	public <S> void updatePropertie(Map<String,Object> properMap,String id) throws Exception {
		final EntityMapping emp = ORMControler.getInstance(this).regeditEntity(this.getClazz());
		this.updatePropertie(emp, properMap, id);
	} 
	public void updatePropertie(final EntityMapping emp,final Map<String,Object> properMap,final String id) throws Exception {
		if(null == properMap || properMap.isEmpty())
			return ; 
		connectionHandler = getDBCon();
		connectionHandler.proConnection(new IConnectionExecutor<Serializable>(){ 
			@Override
			public Serializable doConnection(Connection con)
					throws Exception {
				Map<String,Object> sqlAndParamMap = emp.getUpdatePropertySqlWithMap(properMap);
				List<Object> upArgument = (List<Object>)sqlAndParamMap.get(EntityMapping.SQLPARAM); 
				String sql = sqlAndParamMap.get(EntityMapping.SQL).toString()+" and id='"+id+"'";
				PreparedStatement pst = con.prepareStatement(sql);
				log.debug("更新属性sql：\n"+sql);
				int j=0;
				for(int i=0;i<upArgument.size();i++){
					j=resultSetValueWithTypeProArray(upArgument.get(i),pst,j,emp.getSqlprovider());
				}	
				pst.executeUpdate();
				return null;
			}});
	}
	
	@Override
	public void saveOrUpdate(T t) throws Exception {
		EntityMapping emp = ORMControler.getInstance(this).fetchMappingInfo(this);
		if(null != emp.getEntityId(t))
			this.update(t);
		else
			this.create(t);
	}
	@Override
	public Map<String,List<T>> findProperty(Param param,
			String[] paramArrayOfString) throws Exception {
		Map<String,List<T>> map = new HashMap<String,List<T>>();
		map.put("presult", this.findWithProperty(param, paramArrayOfString));
		return map;
		
	}
	 
	@Override
	public List<T> findWithProperty(Param param,
			String[] paramArrayOfString) throws Exception {
		
		final EntityMapping emp = ORMControler.getInstance(this).fetchMappingInfo(this);	
		connectionHandler = getDBCon();
		Connection con = connectionHandler.getCon(); 
		List<T> tempLis = new ArrayList<T>();
		String sql = emp.getSelectSQL(paramArrayOfString);			
		Map<String,Object> sqlAndParamMap = (Map<String,Object>)param.getCriteria(sql);
		sql+= sqlAndParamMap.get(EntityMapping.SQL)==null?"":sqlAndParamMap.get(EntityMapping.SQL).toString();
		log.debug("输出查询sql："+sql);
		PreparedStatement pst = con.prepareStatement(sql);
		List<Object> sqlParam = (List<Object>)sqlAndParamMap.get(EntityMapping.SQLPARAM);
		if(null != sqlParam&&sqlParam.size()>0)
			for(int j=0,i=0;i<sqlParam.size();i++){
				j=resultSetValueWithTypeProArray(sqlParam.get(i),pst,j,emp.getSqlprovider());
			}
		ResultSet result = pst.executeQuery();
		T t = null;
		ExtendedField[] cfields = emp.getFields(); 
		ISQLProvider sqlp = emp.getSqlprovider();
		Map<String,Byte> cmap = getEntityTableColumnMap(result);
		while(result.next()){
			t = clazz.get().newInstance();
			for(ExtendedField efd:cfields){
				if(!efd.needORM||!cmap.containsKey(efd.columnName.toLowerCase()))
					continue;
				try{refu.setFieldValues(t,efd.field,resultGetValueWithType(efd,result,sqlp));}catch(SQLException csqle){System.err.println(this.getClass()+" 从结果集获得值出错："+csqle.getMessage());}
			}
			tempLis.add(t);
		}
		
		return tempLis;
	}
	@Override
	public <S> S executerSQL(IDoSQL<S> idoSql) throws Exception{
		return executerSQL(idoSql,"");
	}
	public <S> S executerSQL(IDoSQL<S> idoSql,Class<?> clz) throws Exception{
		return executerSQL(idoSql, AnnotationParser.getNameFormEntity(clz));
	}
	private <S> S executerSQL(IDoSQL<S> idoSql,String entityName) throws Exception{
		IConnectionHandler connectionHandler  = getDBConWithName(entityName);
		if(null == connectionHandler){
			log.error("数据库连接单元尚未启动，无法进行db操作!");
			return null;
		}
		Connection con = null;
		try{
			con = connectionHandler.getCon();
			if(null == con) {
				log.error("无法获得的DB连接");
				return null;
			}
			return idoSql.doSql(con);
		}catch(Exception e){
			e.printStackTrace();
			throw e;
		}finally{if(null != con){con.close();con = null;}}
	}
	private final IConnectionHandler getDBConWithName(String tableName) throws Exception{
		IConnectionHandler ic = getConnection();
		return ic;
	}
	@Override
	public void deleteEntity(Class<?> claz,Serializable delId) throws Exception {
		//changeTempClass(claz);
		try{ 
			final EntityMapping emp = ORMControler.getInstance(this).regeditEntity(claz);	
			delete(emp,delId); 
		}catch(Exception e){
			throw e;
		}finally{
			this.resetEntityClass();
		}		
	}
	@Override
	public void deleteEntity(Class<?> claz,Param param) throws Exception {
		//changeTempClass(claz);
		try{
			final EntityMapping emp = ORMControler.getInstance(this).regeditEntity(claz);//ORMControler.getInstance(this).fetchMappingInfo(this);	
			this.delete(emp,param);
		}catch(Exception e){
			throw e;
		}finally{
			this.resetEntityClass();
		}		
	}	
	@Override
	public Serializable updateEntity(final Object t) throws Exception {
		//changeTempClass(t.getClass());
		try{
			final EntityMapping emp = ORMControler.getInstance(this).regeditEntity(t.getClass());//ORMControler.getInstance(this).fetchMappingInfo(this);
			this.update(emp,(T) t);
		}catch(Exception e){
			throw e;
		}finally{
			this.resetEntityClass();
		}
		return (Serializable) t;
	}	
	@Override
	public <S> String registerEntity(Class<S>  entityClass, IDaoConfiguer daoConfig)
			throws Exception { 
		ORMControler.getInstance(this).regeditEntity(entityClass);
		return "success";
	}
	@Override
	public <S> String registerEntity(List<Class<S>> entityClassLis)
			throws Exception {
		if(null == entityClassLis||entityClassLis.isEmpty())
			return null;
		for(Class  _c:entityClassLis)
			ORMControler.getInstance(this).regeditEntity(_c);
		return null;
	}
	@Override
	public String RefreshEntity(T entity) throws Exception {		
		RefreshEntity(entity.getClass());
		return "success";
	}
	@Override
	public String RefreshEntity(Class<?> claz) throws Exception {
		ORMControler.getInstance(this).unRegeditEntity(claz);
		ORMControler.getInstance(this).fetchMappingInfo(this);
		ORMControler.getInstance(this).refreshMapping(claz);
		return "success";
	}
	
	@Override
	public void update(final Param p,final Map<String,Object> upmap,Class<?>...clz) throws Exception { 
		try{	
			Class<?> c = this.getClazz();
			if(null != clz&&clz.length>0){
				//this.changeTempClass(clz[0]);
				c = clz[0];
			}
			final EntityMapping emp = ORMControler.getInstance(this).fetchMappingInfo(this,c);			
			connectionHandler = getDBCon();
			connectionHandler.proConnection(new IConnectionExecutor<Void>(){ 
				@Override
				public Void doConnection(Connection con) throws Exception {
					String upsql = "";
					try{	
						Map<String,Object> sqlAndParamMap = emp.getUpdateSQL(upmap); 
						upsql = sqlAndParamMap.get(EntityMapping.SQL)==null?"":sqlAndParamMap.get(EntityMapping.SQL).toString();
						
						Map<String,Object> upsqlandparam = (Map<String,Object>)p.getCriteria(null);
						upsql+= upsqlandparam.get(EntityMapping.SQL)==null?"":upsqlandparam.get(EntityMapping.SQL).toString();
						
						List<Object> sqlParam = (List<Object>)sqlAndParamMap.get(EntityMapping.SQLPARAM); 
						List<Object> paramsqlandparamv = (List<Object>)upsqlandparam.get(EntityMapping.SQLPARAM); 
						if(null != paramsqlandparamv&&!paramsqlandparamv.isEmpty())
							sqlParam.addAll(paramsqlandparamv); 
						PreparedStatement pst = con.prepareStatement(upsql);
						for(int j=0,i=0;i<sqlParam.size();i++){ 
							j=resultSetValueWithTypeProArray(sqlParam.get(i),pst,j,emp.getSqlprovider());
						}	
						pst.executeUpdate(); 
						}catch(Exception e){
							System.err.println("执行出错:"+e.getMessage()+"\n "+upsql);
							throw e;
						}
					return null;
				}});
		}catch(Exception e){
			throw e;
		}finally{ 
			this.resetEntityClass();
		}
	}
	public IConnectionHandler getConnection() throws Exception{
		return getDefultConnection();
	}
	private final IConnectionHandler getDBCon() throws Exception{ 
		IConnectionHandler ic = getConnection();
		return ic;
	}  
	public IConnectionHandler getDefultConnection() throws Exception{
		try{ 
			String dataBaseFT=IConnectioinFactory.DEFAULTFT;
			Bundle b = FrameworkUtil.getBundle(KernelActivitor.class);
			BundleContext c = b.getBundleContext();//KernelActivitor.gContext();//FrameworkUtil.getBundle(this.getClass().forName("org.chuniter.core.kernel.KernelActivitor")).getBundleContext();
			ServiceReference<?> srf = c.getServiceReference("org.chuniter.core.kernel.api.dao.IConnectioinFactory");
			if(null == srf){
				log.error("无法获得IConnectioinFactory 服务");
				return null;
			}
			IConnectioinFactory icf = (IConnectioinFactory)c.getService(srf);
			return icf.createConnection(dataBaseFT);
		}catch(Exception  e){
			log.error(e.getMessage());
		}
		return null;
	}	
	static IConnectionHandler getDefultCon() throws Exception{
		try{
			BundleContext c = FrameworkUtil.getBundle(Class.forName("org.chuniter.core.kernel.KernelActivitor")).getBundleContext();
			ServiceReference<?> srf = null == c?null:c.getServiceReference(IConnectioinFactory.class.getName());
			if(null == srf){
				System.err.println("无法获得具有IConnectioinFactory类型的服务.");
				return null;
			}
			IConnectioinFactory icf = (IConnectioinFactory)c.getService(srf);
			return icf.createConnection();
		}catch(Exception  e){
			e.printStackTrace();
		}
		return null;
	}	
	
	public void updatePerperties(Map<String,Object> uppropmap,Param p) throws Exception{
		this.updatePropertie(uppropmap, p);
	}
	public <S> void updatePerperties(Class<S> clz,Map<String,Object> uppropmap) throws Exception{
		throw new Exception("不好意思，暂未实现");
	}
	
	@Override
	public void setSessionFactory1(Object sessionFactory) {
		// TODO Auto-generated method stub		
	}
	public Class<T> getClazz() {
		if(null == clazz.get()){
			clazz.set(this.originalClazz);
		}
		return clazz.get();
	}
	public Class<T> getOrigClazz() {
		return originalClazz;
	} 
	private void changeTempClass_(Class<?> c){
		if(null == clazz.get())
			clazz.set(this.originalClazz);
		if(c.getName().equals(clazz.get().getName()))
			return;
		this._clazz = (Class) c;
		clazz.set((Class)c);
	}
	private void resetEntityClass(){
		fetchGClass();
	} 
	@Override
	public List<Map<String, Object>> hisqlOrig(final String sql_,final Param p)throws Exception {  
		return finalhisql(sql_,p,false);
	}
	@Override
	public List<Map<String, Object>> hisql(final String sql_,final Param p)throws Exception {   
		return finalhisql(sql_,p,true);
	} 
	private List<Map<String, Object>> finalhisql(final String sql_,final Param p,final boolean isControledEntity)throws Exception {  
		Object o = this.executerSQL(new IDoSQL<Object>(){
			@Override
			public Object doSql(Connection con) {  
				String sql2 = "";
				String er = null;
				try {
					String sql = sql_;
					String tmsql = sql.toLowerCase().trim(); 
					if(tmsql.toLowerCase().startsWith("update")||tmsql.toLowerCase().startsWith("delete")){
						String tablealias = EntityMapping.getSqlTableName(sql);
						if(!StringUtil.isValid(tablealias))
							tablealias = "";
						else
							tablealias+=".";
						boolean iscontroled = isControledEntity;
						if(!iscontroled) {
							//FIXME HISQL这里务必保持表名与实体名一致，不然将无法设置禁用及锁定条件。
							Class<?> c = ORMControler.getEntityByTableName(tablealias);
							if(null != c)
								iscontroled = true;
						}
						if(iscontroled) {
							//p.addParam("lockedsql", "sql-server"," (("+tablealias+"ecode !='"+BaseEntity.INBUILT+"' and "+tablealias+"ulocked is null) or "+tablealias+"ecode is null) ");
							sql+=" and (("+tablealias+"ecode !='"+BaseEntity.INBUILT+"' and "+tablealias+"ulocked is null) or "+tablealias+"ecode is null) ";
						}
					}
					ISQLProvider dbner = EntityMapping.getSqlProvider(con);
					p.setSqlprovider(dbner);
					Map<String,Object> m = (Map<String,Object>)p.getCriteria(sql);
					String groupsql = m.get(EntityMapping.SQLGROUP)==null?"":m.get(EntityMapping.SQLGROUP).toString();
					String ordersql = m.get(EntityMapping.SQLORDER)==null?"":m.get(EntityMapping.SQLORDER).toString();
					sql2 = sql + (m.get(EntityMapping.SQL)==null?"":m.get(EntityMapping.SQL).toString())+ordersql+groupsql;
					if(null != dbner)
						sql2 = dbner.proSql(sql2);
					PreparedStatement pst = con.prepareStatement(sql2);
					//将条件设置到预处理器中
					fillParamToStatement(p,m,sql,pst,dbner);

					if(tmsql.toLowerCase().startsWith("update")||tmsql.toLowerCase().startsWith("insert")||tmsql.toLowerCase().startsWith("delete")){
						boolean res = pst.execute();
						log.debug("更新结果:"+res);
						return null;
					}
					return DaoImplAdaptor.extractData(pst.executeQuery());
				} catch (Exception e) { 
					e.printStackTrace();
					log.error(e.getMessage()+"  \nsql:"+sql2+"  \n param:"+p); 
					er = "exception:"+e.getMessage();
				} 
				return er;
			} 
		});
		if(null == o)
			return null;
		if(!(o instanceof List)) {
			throw new Exception(o.toString());
		}
		return (List<Map<String, Object>>)o;
	}
	private void fillParamToStatement(Param p,Map<String,Object> m,String appendParamSql,PreparedStatement st,ISQLProvider dbner) throws Exception {
		if(null == p)
			return;
		if(null == m)
			m = (Map<String,Object>)p.getCriteria(appendParamSql);
		List<Object> sqlParam = (List<Object>)m.get(EntityMapping.SQLPARAM);
		if(null == sqlParam||sqlParam.isEmpty())
			return;
		for(int j=0,l=sqlParam.size(),i=0;i<l;i++){
			j=resultSetValueWithTypeProArray(sqlParam.get(i),st,j,dbner);
		}
		//sqlParam.clear();
		//sqlParam = null;
	}

	public PageIterator<?> hisql(final String sql,final Param p,final int startIndex,final int pageSize) throws Exception{
		return hisql(sql,p,startIndex,pageSize,true);
	}
	public <S> PageIterator<S> hisql(final String sql_,final Param p,final int startIndex,final int pageSize,boolean needFilterDataOwner) throws Exception{
		String[] et = EntityMapping.fetchTableNameAndAlias(sql_);
		final Class<?> c = ORMControler.getEntityByTableName(et[0]);
		EntityMapping emp_ = ORMControler.getInstance(this).regeditEntity(c);
		if(null == emp_)
			emp_ = ORMControler.getInstance(this).fetchMappingInfo(this);
		final EntityMapping emp = emp_;
		return this.executerSQL(new IDoSQL<PageIterator<S>>(){
			@Override
			public PageIterator<S> doSql(Connection con) {
				String pageSql = null;
				String countSql = null;
				try{
					String sql = sql_;
					emp.setParam(p);
					Map<String,Object> sqlAndParamMap = (Map<String,Object>)emp.getParam().getCriteria(sql);
					//String paramsql = sqlAndParamMap.get(EntityMapping.SQLCOUNTPARAM)==null?"":sqlAndParamMap.get(EntityMapping.SQLCOUNTPARAM).toString();
					countSql = emp.getEntityCountWithJoinSQLFormParam(sql, sqlAndParamMap);

					PreparedStatement pst = con.prepareStatement(countSql);
					log.debug("输出分页前的统计sql:"+countSql);
					List<String> sqlParam = (List<String>)sqlAndParamMap.get(EntityMapping.SQLPARAM);
					if(null != sqlParam&&sqlParam.size()>0)
						for(int j=0,i=0;i<sqlParam.size();i++){
							j=resultSetValueWithTypeProArray(sqlParam.get(i),pst,j,emp.getSqlprovider());
						}
					ResultSet result = pst.executeQuery();
					Long totalCount = 0l;
					while(result.next()){
						totalCount = result.getLong(1);
					}
					if(totalCount <= 0)
						return new PageIteratorUtil<S>().getNullPage();
					//有数据
					pageSql = emp.getSqlprovider().getPageSql(emp,sql, startIndex, pageSize);
					log.debug("输出分页sql:"+pageSql);
					pst = con.prepareStatement(pageSql);
					if(null != sqlParam&&sqlParam.size()>0)
						for(int j=0,i=0;i<sqlParam.size();i++){
							j=resultSetValueWithTypeProArray(sqlParam.get(i),pst,j,emp.getSqlprovider());
						}
					List<Map<String,Object>> resmap = DaoImplAdaptor.extractData(pst.executeQuery());
					PageIterator<S> page = new PageIterator(resmap,totalCount.intValue(),pageSize,startIndex);
					return page;
				}catch(Exception e){
					e.printStackTrace();
					log.error("hisql查询分页异常,sql:统计:"+countSql+" \n分页:"+pageSql);
				}
				return null;
			}},et[0]);
	}
	private String fetchUserId(){ 
		HttpServletRequest o = AbstractBaseAuthorization.get();
		if(null != o){
			if(null != o.getSession(false)){  
				BaseUserEntity bu;
				try {
					bu = AbstractBaseAuthorization.sgetGeneralUserEntity(null);
					if(null != bu)
						return bu.getId();
				} catch (Exception e) { 
					e.printStackTrace();
				}
				
			}
		}
		if(null != AbstractBaseAuthorization.get()&&null != AbstractBaseAuthorization.get().getSession(false)){ 
			Object mapObj = AbstractBaseAuthorization.get().getSession().getAttribute(IAuthorization.SESSION_USERINFOMAP);
			if(null == mapObj)
				return null;
			Map buser = (Map)mapObj;  
			if(buser.containsKey("userId"))
				return buser.get("userId").toString();
			else if(buser.containsKey("id"))
				return  buser.get("id").toString(); 
		} 
		return null;
	}
	private String fetchCurrentDataOwner(){ 
		HttpServletRequest o = AbstractBaseAuthorization.get();
		if(null != o){
			if(null != o.getSession(false)){  
				BaseUserEntity bu;
				try {
					bu = AbstractBaseAuthorization.sgetGeneralUserEntity(null);
					if(null != bu)
						return bu.getDataOwner();
				} catch (Exception e) { 
					e.printStackTrace();
				}
				
			}
		}
		if(null != AbstractBaseAuthorization.get()&&null != AbstractBaseAuthorization.get().getSession(false)){ 
			Object mapObj = AbstractBaseAuthorization.get().getSession().getAttribute(IAuthorization.SESSION_USERINFOMAP);
			if(null == mapObj)
				return null;
			Map buser = (Map)mapObj;  
			if(buser.containsKey(BaseEntity.DATAOWNER))
				return buser.get(BaseEntity.DATAOWNER).toString(); 
		} 
		return null;
	}
	/**
	 * 此方法会存在性能问是，望后续优化
	 * @author youg 2016-05-17 19:17
	 * @param emp
	 */
	private void proEntityMapFieldInfo(EntityMapping emp){
		Param p = Param.getInstance(); 
		StringBuilder sb = new StringBuilder();
		try {
			List<Map<String,Object>> ml = hisql("select id from EntitySetSolution es where es.entityclass='"+emp.getEntityClass().getName()+"'  and createrid='"+fetchUserId()+"'  order by es.showIndex asc", p);			 
			String defSolutionId = "";
			if(null != ml&&!ml.isEmpty())
				defSolutionId = ml.get(0).get("id").toString(); 
			sb.append("select fieldName,title,alias,fillNoticeText,orderType,fieldType from EntitySetEntity EntitySetEntity_ where ")
			  .append(" createrId='").append(fetchUserId()).append("' ")
			  .append(" and entityClass='").append(emp.getEntityClass().getName()).append("' "); 
			if(StringUtil.isValid(defSolutionId))  
				sb.append(" and solutionId='"+defSolutionId+"' ");
			sb.append(" order by showIndex asc "); 
			List<Map<String,Object>> eses = hisql(sb.toString(), Param.getInstance()); 
			if(null == eses||eses.isEmpty())
				return;
			emp.proFieldByEntitySetInfo(eses);
		} catch (Exception e) {  e.printStackTrace();System.err.print("根据实体设置 处理 属性发生异常，有可能是没有安装basedatautil单元或是其没有正常启动."+e.getMessage());log.error("根据实体设置 处理 属性发生异常，有可能是没有安装basedatautil单元或是其没有正常启动."+e.getMessage()); }
	}
	public static void main(String[] arg){
		String sdf = "select * from bbb and 1=1 ";
		String tmsql = sdf.toLowerCase();
		if(tmsql.indexOf("and") != -1){
			String tssql = tmsql.substring(0, tmsql.indexOf("and")).toLowerCase();
			//log.debug(tssql);
			if(tssql.indexOf("where") == -1){
				tmsql = tssql+" where 1=1 "+tmsql.substring(tmsql.indexOf("and"));
			}
		}
		//log.debug(tmsql);
	}
	@Override
	public <S> void unRegeditEntity(Class<S> c) throws Exception {
		ORMControler.getInstance(null).unRegeditEntity(c);
	}
	
}
