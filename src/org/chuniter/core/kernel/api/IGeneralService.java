package org.chuniter.core.kernel.api;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.chuniter.core.kernel.api.dao.GenericDao;
import org.chuniter.core.kernel.api.unit.IDoSQL;
import org.chuniter.core.kernel.impl.unit.UMap;
import org.chuniter.core.kernel.kernelunit.PageIterator;
import org.chuniter.core.kernel.kernelunit.Param;
import org.chuniter.core.kernel.kernelunit.ParamCarrier;
import org.chuniter.core.kernel.model.BaseEntity;
import org.chuniter.core.kernel.model.Extendable;

import com.alibaba.fastjson.JSONObject;
 

/**
 * 通用服务接口
 * @author Administrator
 *
 * @param <T>
 */
 public abstract interface IGeneralService<T extends Extendable> {
	
	/* abstract T find(Serializable paramSerializable) throws Exception;*/
	/**
	 * 查询所有指定类数据
	 */
	 abstract List<T> find()throws Exception;
	/**
	 * 按实体进行删除
	 * @param entity
	 * @throws Exception
	 */
	 abstract void delete(T entity) throws Exception;
	/**
	 * 按实体集合批量删除
	 * @param dels
	 * @throws Exception
	 */
	 abstract void deleteByLis(List<T> dels) throws Exception;
	/**
	 * 按指定的实体ID集合批量删除
	 * @param ids
	 * @throws Exception
	 */
	 abstract void deleteByIdLis(List<Serializable> ids) throws Exception;
	 abstract void delete(Param p) throws Exception;
	 abstract void deleteEntity(Class<?> claz,Param p) throws Exception;
	/**
	 * 
	 * @param paramSerializable
	 * @throws Exception
	 */
	 abstract void delete(Serializable paramSerializable)throws Exception;
	 abstract void deleteEntity(Class<?> claz,Serializable delId) throws Exception;

	 abstract List<T> find(String paramString) throws Exception;
	 abstract T findById(Serializable entityId) throws Exception;
	 abstract PageIterator<T> find(int paramInt1, int paramInt2)throws Exception;
	 abstract List<T> find(Param paramParam) throws Exception;
	 abstract PageIterator<T> find(Param paramParam, int paramInt1,int paramInt2) throws Exception;
	 PageIterator<T> findPage(List<ParamCarrier> paramCarriers, int paramInt1, int paramInt2)  throws Exception;
	 PageIterator<T> findPageAsJoin(Param p, int startIndex, int pageSize)  throws Exception;
	 int findCount(Class<?> clz,Param p) throws Exception;
	int findCount(Class<?> clz,Param p,boolean needFilterData) throws Exception;
	int findCount(String tableName,Param pp) throws Exception;
	int findCount(String tableName,Param pp,boolean needFilterData) throws Exception;
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
	 <S extends Extendable> PageIterator<S> findPageAsJoin(Class<S> claz,Param param, int startIndex,int pageSize) throws Exception;
	 <S extends Extendable> PageIterator<S> findPageAsJoin(Class<S> claz,Param param, int startIndex, int pageSize,boolean applyEntitySet) throws Exception;
	 abstract Serializable save(T entity) throws Exception;
	 abstract Serializable create(T entity) throws Exception;
	 abstract Serializable createEntity(Object entity) throws Exception;
	 Serializable create(Class<? extends BaseEntity> c,JSONObject json) throws Exception ;
	 Serializable create(String classFullName,JSONObject json) throws Exception;
	 Serializable update(String classFullName, JSONObject json) throws Exception;
	 abstract Serializable update(T entity) throws Exception;
	 abstract Serializable updateEntity(Object entity) throws Exception;
	 
	/**
	 * 更新方法，可以进行批量更新
	 * @author yonglu
	 * @time 2015-07-15 10:52
	 * @param p
	 * @param upmap
	 * @throws Exception
	 */
	 abstract void update(Param p,Map<String,Object> upmap,Class<?>...clz) throws Exception;
	 abstract void saveOrUpdate(T entity) throws Exception;


	 abstract Map<String, List<T>> findProperty(Param paramParam,String[] paramArrayOfString)  throws Exception;
	 List<T> findWithProperty(Param param, String[] paramArrayOfString) throws Exception;
	/**
	 * 按实体属性来设置条件，进行查询
	 * @param t 用于设置查询条件的实体对象
	 * @return List<T> 查询后的结果 
	 * @throws Exception
	 */
	 abstract List<T> findByEntity(T t) throws Exception;
	/**
	 * 获得已设置好的参数携带类集合，并解析它组成一个param实现类时行查询
	 * @param List<ParamCarryer>
	 * @return
	 * @throws Exception
	 */
	 List<T> find(List<ParamCarrier> paramCarriers) throws Exception;
	
	/**
	 * 当整合了JMS消息服务时，每个服务类需要一个发送消息的方法
	 * @param sendMsg
	 */
	void sendMsg(Serializable sendObj);
	
	 abstract void setGenericDao(GenericDao<T> gd) throws Exception;
	// abstract GenericDao<T> getGenericDao() throws Exception;
	/**
	 * 执行sql方法，传入一下执行接口，该接口有一个以connection为参数的方法
	 * 具体的处理在由具体实现
	 * @author youg
	 * @time 2012-09-13 
	 * @param idoSql
	 * @return
	 */
	<S> S executerSQL(IDoSQL<S> idoSql) throws Exception;
	<S> S executerSQL(IDoSQL<S> idoSql,Class<?> claz) throws Exception;
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
	 
	<S> List<S> findEntitys(Class<S> clazz,Param param) throws Exception;
	<S> List<S> findEntitys(Class<S> clazz,Param param,boolean needDataOwner) throws Exception;
	PageIterator<?> findEntitys(Class<?> clazz,Param param,int startIndex,int pageSize) throws Exception;
	 <S> S findEntity(Class<S> claz,Param p)throws Exception;
	 <S> S findEntity(Class<S> claz, Param p,boolean needDataOwner) throws Exception;
	 <S> S findEntityById(Class<S> claz, String id)throws Exception;
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
	 * @time 2018-04-26 17:18
	 * @param clax 要更新的类
	 * @param properMap 要更新的属性名与值
	 * @param param 更新条件
	 * @param checkEstate 是否要检查可编辑性
	 * @throws Exception
	 */
	<S> void updatePropertie(Class<S> clax,Map<String,Object> properMap,Param param,boolean checkEstate) throws Exception;
	/**
	 * 更新属性方法
	 * @author yonglu
	 * @time 2017-09-03 11:49
	 * @param clax 要更新的类
	 * @param properMap 要更新的属性名与值
	 * @param id 更新id
	 * @throws Exception
	 */ 
	  void updatePropertie(Class<?> s,Map<String,Object> properMap,String id) throws Exception;
	/**
	 * 更新属性方法
	 * @author yonglu
	 * @time 2017-09-03 11:49 
	 * @param properMap 要更新的属性名与值
	 * @param id 更新id
	 * @throws Exception
	 */ 
	 <S> void updatePropertie(Map<String,Object> properMap,String id) throws Exception ;
	 <S> S updateExcludeField(final S t,final String[] excludeFileNames) throws Exception;
	 List<Map<String,Object>> hisql(String sql,Param p) throws Exception;

	<S> PageIterator<S> hisql(final String sql,final Param p,final int startIndex,final int pageSize) throws Exception;
	<S> PageIterator<S> hisql(final String sql,final Param p,final int startIndex,final int pageSize,boolean needFilterData) throws Exception;
	/**
	 * 此方法用于可以指定是否需要进行数据隔离
	 * @author yonglu
	 * @time 2017-02-28 06:53
	 * @param sql
	 * @param p
	 * @param needFilterData 是否需要进行企业数据隔离，false为不隔离，true为隔离
	 * @return
	 * @throws Exception
	 */
	 List<Map<String, Object>> hisql(final String sql,final Param p,boolean needFilterData) throws Exception;
	 UMap hisqlOne(final String sql,final Param p,boolean needFilterData) throws Exception;
	 UMap hisqlOne(final String sql,final Param p) throws Exception;
	/**
	 * 获得一个自定义前缀的序号
	 * @param prefix
	 * @return
	 * @throws Exception
	 */
	String fetchNo(String prefix,IGeneralService<?> service) throws Exception;
	 String fetchNo(String prefix,Integer preNo) throws Exception;
	 String fetchNo() throws Exception;
	String fetchNo(String prefix, Class<?> clz) throws Exception;
	/**
	 * 更新生成的编号为已用，默认生成的为未用，需保存成功后手动更新
	 * @author yonglu
	 * @time 2017-02-28 06:53
	 * @param prefix
	 * @param nov
	 * @throws Exception
	 */
	 void noInUse(String prefix,String nov) throws Exception;
	
	 String getCurrentUserDataOwner();
	 String getCurrentEntECode(); 
	/**
	 * 处理流程,在实体增，修时处理是否要触发流程。
	 * @author youglu
	 * @time 2017-04-02 18:19
	 * @param t
	 * @return
	 * @throws Exception
	 */
	 <S> String doWorkFlow(S t,Integer proType) throws Exception;
	
	/**
	 * 根据连接获得数据库类型
	* @Description: TODO
	* @author youg continentlu@sina.com
	* @date 2017年12月14日 上午1:13:09 
	* @version V1.0
	 */
	 String changeSqlprovider(Connection c) throws SQLException;
	 /**
	  * 使用MAX指定属性获得此属性最大值，
	  * 此方法会去除非数字部分，如果全部为非数字，则返回1,否则返回此属性最大值+1后的结果。
	  * @author youglu 2018-04-06 14:06
	  * @param prefix
	  * @param clz
	  * @param maxFieldName
	  * @return
	  * @throws Exception
	  */
	String fetchNo(String prefix, Class<?> clz, String maxFieldName) throws Exception; 
	String fetchNo(String prefix,Class<?> clz,String maxFieldName,Integer noLength) throws Exception;
	String fetchNo(String prefix, Class<?> clz, String maxFieldName, Integer noLength, Param p) throws Exception;
	/**
	 * 查询完整对象，如果对象中的属性还也包括关联对象会也一并查询之
	 * @author youglu 2018-05-18 14:10
	 * @param c 查询询的实体类
	 * @param id 查询的ID
	 * @param m 防止死循环MAP
	 * @return S 完整对象
	 * @throws Exception
	 */
	<S> S findFull(Class<S> c,String id,Map<String,Byte> m) throws Exception;
	/**
	 * @author youglu 2018-08-26 16:33
	 * @param c 要更新类
	 * @param id
	 * @param estate
	 * @param p
	 * @return msg，null:无异常，有值：有异常消息
	 * @throws Exception
	 */
	String audit(Class<?> c,String id,int estate,Param p) throws Exception;
	/**
	 * 取消审核
	 * @author youglu 2018-08-26 16:33
	 * @param c
	 * @param id
	 * @param p
	 * @return
	 * @throws Exception
	 */
	String deAudit(Class<?> c,String id,Param p) throws Exception ;
	GenericDao<T> getDao();
}
