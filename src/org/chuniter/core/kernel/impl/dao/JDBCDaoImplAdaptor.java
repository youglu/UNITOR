package org.chuniter.core.kernel.impl.dao;

import java.io.Serializable;
import java.sql.Connection;
import java.util.List;
import java.util.Map;

import org.chuniter.core.kernel.api.dao.GenericDao;
import org.chuniter.core.kernel.api.dao.IDaoConfiguer;
import org.chuniter.core.kernel.api.unit.IDoSQL;
import org.chuniter.core.kernel.kernelunit.PageIterator;
import org.chuniter.core.kernel.kernelunit.Param;
import org.chuniter.core.kernel.model.Extendable;

public abstract class JDBCDaoImplAdaptor <T extends Extendable> extends GeneralDao<T> implements GenericDao<T> {

	public abstract Connection getConnection() throws Exception;
	@Override
	public T find(Serializable paramSerializable) throws Exception {
		return null;
	}

	@Override
	public List<T> find() throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void delete(T paramT) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void delete(Serializable paramSerializable) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public List<T> find(String paramString) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PageIterator<T> find(int paramInt1, int paramInt2) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<T> find(Param paramParam) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PageIterator<T> find(Param paramParam, int paramInt1, int paramInt2)
			throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Serializable save(T paramT) throws Exception {
		return null;
	}

	@Override
	public Serializable update(T paramT) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void saveOrUpdate(T paramT) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Map<String, List<T>> findProperty(Param paramParam,
			String[] paramArrayOfString) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <S> S executerSQL(IDoSQL<S> idoSql) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <S> String registerEntity(Class<S> entityClass, IDaoConfiguer daoConfig)
			throws Exception { 
		return null;
	}

	@Override
	public <S> String registerEntity(List<Class<S>> entityClassLis)
			throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String RefreshEntity(T entity) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public String RefreshEntity(Class<?> clazz) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public void setSessionFactory1(Object sessionFactory) {
		// TODO Auto-generated method stub
		
	}
	public List<Map<String, Object>> hisqlOrig(final String sql_,final Param p)throws Exception {
		return null;
	}

}
