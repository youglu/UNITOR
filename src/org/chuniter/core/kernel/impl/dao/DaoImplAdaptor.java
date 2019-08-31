package org.chuniter.core.kernel.impl.dao;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.chuniter.core.kernel.api.dao.GenericDao;
import org.chuniter.core.kernel.api.dao.IDaoConfiguer;
import org.chuniter.core.kernel.api.unit.IDoSQL;
import org.chuniter.core.kernel.api.unit.IUMap;
import org.chuniter.core.kernel.impl.unit.UMap;
import org.chuniter.core.kernel.kernelunit.PageIterator;
import org.chuniter.core.kernel.kernelunit.Param;
import org.chuniter.core.kernel.model.Extendable;
/**
 * DAO接口适配类
 * @author youg
 *
 */
public  class DaoImplAdaptor<T extends Extendable> extends GeneralDao<T> implements GenericDao<T> {

	@Override
	public String RefreshEntity(T arg0) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public String RefreshEntity(Class<?> clazz) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public void delete(T arg0) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void delete(Serializable arg0) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public List<T> find() throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public T find(Serializable arg0) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<T> find(String arg0) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<T> find(Param arg0) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PageIterator<T> find(int arg0, int arg1) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PageIterator<T> find(Param arg0, int arg1, int arg2)
			throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, List<T>> findProperty(Param arg0, String[] arg1)
			throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <S> String registerEntity(List<Class<S>> arg0) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <S> String registerEntity(Class<S> arg0, IDaoConfiguer arg1)
			throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Serializable save(T arg0) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void saveOrUpdate(T arg0) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public Serializable update(T arg0) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setSessionFactory1(Object sessionFactory) {
		// TODO Auto-generated method stub

	}

	@Override
	public <S> S executerSQL(IDoSQL<S> idoSql) throws Exception{
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public <S> S executerSQL(IDoSQL<S> idoSql,Class<?> clz) throws Exception{
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public PageIterator<T> find(Param param,
								int startIndex, int pageSize, String[] excludeFields)
			throws Exception {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public Serializable createEntity(Object entity) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public void deleteEntity(Class<?> claz, Serializable delId)
			throws Exception {
		// TODO Auto-generated method stub

	}
	@Override
	public <S> List<S> findEntitys(Class<S> clazz, Param param)
			throws Exception {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public PageIterator<?> findEntitys(Class<?> clazz, Param param,
									   int startIndex, int pageSize) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public Serializable updateEntity(Object entity) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public <S> S findEntity(Class<S> clazz, Param param) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public void delete(Param p) throws Exception {
		// TODO Auto-generated method stub

	}
	@Override
	public void deleteEntity(Class<?> claz, Param p) throws Exception {
		// TODO Auto-generated method stub

	}
	public void updatePerperties(Map<String,Object> uppropmap,Param p) throws Exception{};
	public <S> void updatePerperties(Class<S> clz,Map<String,Object> uppropmap) throws Exception{}
	public <S> S updateExcludeField(final S t,final String[] excludeFileNames) throws Exception{return null;}
	public void update(Param p,Map<String,Object> upmap,Class<?>...clz) throws Exception{ }
	@Override
	public List<T> findWithProperty(Param param, String[] paramArrayOfString)
			throws Exception {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public void updatePropertie(Map<String, Object> properMap, Param param)
			throws Exception {
		// TODO Auto-generated method stub

	}
	@Override
	public <S> void updatePropertie(Class<S> clax, Map<String, Object> properMap,
									Param param) throws Exception {
		// TODO Auto-generated method stub

	}
	/**
	 * 更新属性方法
	 * @author yonglu
	 * @time 2017-09-03 11:49
	 * @param clax 要更新的类
	 * @param properMap 要更新的属性名与值
	 * @param id 更新id
	 * @throws Exception
	 */
	public  void updatePropertie(Class<?> s,Map<String,Object> properMap,String id) throws Exception {

		//
	}
	/**
	 * 更新属性方法
	 * @author yonglu
	 * @time 2017-09-03 11:49 
	 * @param properMap 要更新的属性名与值
	 * @param id 更新id
	 * @throws Exception
	 */
	public <S> void updatePropertie(Map<String,Object> properMap,String id) throws Exception {

	}
	/**
	 * 通用取结果方案,返回list
	 *
	 * @param rs
	 * @return
	 * @throws SQLException
	 */
	public static List<Map<String,Object>> extractData(ResultSet rs) throws SQLException {
		ResultSetMetaData md = rs.getMetaData();
		int num = md.getColumnCount();
		List<Map<String,Object>> listOfRows = new ArrayList<Map<String,Object>>();
		while (rs.next()) {
			IUMap mapOfColValues = new UMap(num);
			for (int i = 1; i <= num; i++) {
				mapOfColValues.put(md.getColumnLabel(i), rs.getObject(i));
			}
			listOfRows.add(mapOfColValues);
		}
		return listOfRows;
	}
	@Override
	public List<Map<String, Object>> hisql(final String sql,final Param p)
			throws Exception {
		return null;
	}
	public List<Map<String, Object>> hisqlOrig(final String sql_,final Param p)throws Exception {
		return null;
	}
	@Override
	public PageIterator<T> findPageAsJoin(Param p, int paramInt1, int paramInt2)
			throws Exception {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public PageIterator<?> hisql(String sql, Param p, int startIndex, int pageSize) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public PageIterator<?> hisql(String sql, Param p, int startIndex, int pageSize,boolean needFilterDataOwner) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public <S extends Extendable> PageIterator<S> findPageAsJoin(Class<S> claz,
																 Param param, int startIndex, int pageSize) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}
	public <S extends Extendable> PageIterator<S> findPageAsJoin(Class<S> claz,Param param, int startIndex, int pageSize,boolean applyEntitySet) throws Exception{
		// TODO Auto-generated method stub
		return null;
	}
	/*
        /**
        * 通用取结果方案,返回JSONArray
        *
        * @param rs
        * @return
        * @throws SQLException
        */
	/*
	public static JSONArray extractJSONArray(ResultSet rs) throws SQLException {
		ResultSetMetaData md = rs.getMetaData();
		int num = md.getColumnCount();
		JSONArray array = new JSONArray();
		while (rs.next()) {
		JSONObject mapOfColValues = new JSONObject();
		for (int i = 1; i <= num; i++) {
		mapOfColValues.put(md.getColumnName(i), rs.getObject(i));
		}
		array.add(mapOfColValues);
		}
		return array;
	}
*/
	@Override
	public <S> void unRegeditEntity(Class<S> entityClass) throws Exception {
		// TODO Auto-generated method stub

	}
}
