package org.chuniter.core.kernel.api.dao;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.chuniter.core.kernel.api.unit.IDoSQL;
import org.chuniter.core.kernel.impl.orm.EntityMapping;
import org.chuniter.core.kernel.impl.orm.ORMControler;
import org.chuniter.core.kernel.kernelunit.PageIterator;
import org.chuniter.core.kernel.kernelunit.Param;
import org.chuniter.core.kernel.model.Extendable;

public abstract interface GenericDao<T> {
	
	public abstract T find(Serializable paramSerializable) throws Exception;

	public abstract List<T> find()throws Exception;
	
	public abstract void delete(T entity) throws Exception;

	public abstract void delete(Serializable paramSerializable) throws Exception;
	public abstract void delete(Param p) throws Exception;
	public abstract void deleteEntity(Class<?> claz,Param p) throws Exception;
	
	public abstract List<T> find(String paramString) throws Exception;

	public abstract PageIterator<T> find(int paramInt1, int paramInt2) throws Exception;

	public abstract List<T> find(Param paramParam) throws Exception;

	public abstract PageIterator<T> find(Param paramParam, int paramInt1, int paramInt2) throws Exception;

	public abstract Serializable save(T entity) throws Exception;

	public abstract Serializable update(T entity) throws Exception;
	
	/**
	 * 更新方法，可以进行批量更新
	 * @author yonglu
	 * @time 2015-07-15 10:52
	 * @param p
	 * @param upmap
	 * @throws Exception
	 */
	public abstract void update(Param p,Map<String,Object> upmap,Class<?>...clz) throws Exception;
	
	//public abstract Serializable updateProperty(Serializable id,Map<String,Object>) throws Exception;
	/**
	 * 更新属性方法
	 * @author yonglu
	 * @time 2015-08-09 22:49
	 * @param properMap 要更新的属性名与值
	 * @param param 更新条件
	 * @throws Exception
	 */
	void updatePropertie(Map<String,Object> properMap,Param param) throws Exception;
	/**
	 * 更新属性方法
	 * @author yonglu
	 * @time 2015-08-09 22:49
	 * @param clax 要更新的类
	 * @param properMap 要更新的属性名与值
	 * @param param 更新条件
	 * @throws Exception
	 */
	<S> void updatePropertie(Class<S> clax,Map<String,Object> properMap,Param param) throws Exception;
	/**
	 * 更新属性方法
	 * @author yonglu
	 * @time 2017-09-03 11:49
	 * @param clax 要更新的类
	 * @param properMap 要更新的属性名与值
	 * @param id 更新id
	 * @throws Exception
	 */ 
	public  void updatePropertie(Class<?> s,Map<String,Object> properMap,String id) throws Exception;
	/**
	 * 更新属性方法
	 * @author yonglu
	 * @time 2017-09-03 11:49 
	 * @param properMap 要更新的属性名与值
	 * @param id 更新id
	 * @throws Exception
	 */ 
	public <S> void updatePropertie(Map<String,Object> properMap,String id) throws Exception ;
	public abstract void saveOrUpdate(T entity) throws Exception;

	public abstract Map<String, List<T>> findProperty(Param paramParam,
			String[] paramArrayOfString)throws Exception;
	/**
	 * 执行sql方法，传入一下执行接口，该接口有一个以connection为参数的方法
	 * 具体的处理在由具体实现
	 * @author youg
	 * @time 2012-09-13
	 * @param idoSql
	 * @return
	 */
	<S> S executerSQL(IDoSQL<S> idoSql) throws Exception;
	public <S> S executerSQL(IDoSQL<S> idoSql,Class<?> clz) throws Exception;
	
	/*************具有管理类的方法*************/
	/**
	 * 提供注册实体的功能
	 * @author youg
	 * @time 2012-01-13
	 * @param entity 要注册的实体
	 */
	public abstract <S> String registerEntity(Class<S> entityClass,IDaoConfiguer daoConfig)throws Exception;
	public abstract <S> String registerEntity(List<Class<S>> entityClassLis)throws Exception;
	/**
	 * 解除实体注册
	 * @author youglu
	 * @time 2016-12-23 19:04
	 * @param entityClass
	 * @throws Exception
	 */
	public abstract <S> void unRegeditEntity(Class<S> entityClass)throws Exception;
	/**
	 * 刷新实体，也可用于重新注册实体
	 * @param entity
	 * @return
	 */
	public abstract String RefreshEntity(T entity)throws Exception;
	public abstract String RefreshEntity(Class<?> clazz)throws Exception;
	/**
	 * when use hibernate and use this method set sessionFactory
	 * for every business dao.
	 * @param sessionFactory
	 */
	public abstract void setSessionFactory1(Object sessionFactory);
	
	/**
	 * 排除指定的属性进行查询
	 * @author youg
	 * @time 2013-09-28
	 * @vertion v3
	 * @param clazz
	 * @param param
	 * @param startIndex
	 * @param pageSize
	 * @param excludeFields
	 * @return
	 * @throws Exception
	 */
	PageIterator<T> find(Param param,int startIndex,int pageSize,String[] excludeFields) throws Exception;
	public Serializable createEntity(Object entity) throws Exception;
	public void deleteEntity(Class<?> claz,Serializable delId) throws Exception;
	<S> List<S> findEntitys(Class<S> clazz,Param param) throws Exception;
	<S> S findEntity(Class<S> clazz,Param param) throws Exception;
	PageIterator<?> findEntitys(Class<?> clazz,Param param,int startIndex,int pageSize) throws Exception;
	public Serializable updateEntity(Object entity) throws Exception ; 
	public <S> S updateExcludeField(final S t,final String[] excludeFileNames) throws Exception;

	List<T> findWithProperty(Param param, String[] paramArrayOfString) throws Exception;
	public List<Map<String,Object>> hisql(String sql,Param p) throws Exception;
	public List<Map<String, Object>> hisqlOrig(final String sql_,final Param p)throws Exception ;
	public PageIterator<?> hisql(final String sql,final Param p,final int startIndex,final int pageSize) throws Exception;
	public <S> PageIterator<S> hisql(final String sql,final Param p,final int startIndex,final int pageSize,boolean needFilterDataOwner) throws Exception;
	public PageIterator<T> findPageAsJoin(Param p, int paramInt1, int paramInt2)  throws Exception; 
	/**
	 * 组合查询泛型版
	 * @author yonglu
	 * @time 2015-12-25 07:29
	 * @param claz 要查询的实体，必须继承可扩展基类
	 * @param param
	 * @param startIndex
	 * @param pageSize
	 * @return
	 * @throws Exception
	 */
	public <S extends Extendable> PageIterator<S> findPageAsJoin(Class<S> claz,Param param, int startIndex,int pageSize) throws Exception;
	public <S extends Extendable> PageIterator<S> findPageAsJoin(Class<S> claz,Param param, int startIndex, int pageSize,boolean applyEntitySet) throws Exception;
	
}