package org.chuniter.core.kernel.api.orm;

import java.io.Serializable;
import java.util.Map;

import org.chuniter.core.kernel.api.dao.GenericDao;
import org.chuniter.core.kernel.api.dao.IConnectionHandler;
import org.chuniter.core.kernel.kernelunit.PageIterator;
import org.chuniter.core.kernel.kernelunit.Param;

public interface ISimpORM<T> extends GenericDao<T>{
	
	/*public Serializable create(T t) throws Exception;
	public void delete(Serializable delParam) throws Exception;
	public Serializable upate(T t) throws Exception;
	public List<T> find(Param p) throws Exception;*/
	IConnectionHandler getConnection() throws Exception;
	IConnectionHandler getDefultConnection() throws Exception;
	Class<T> getClazz();
	Class<T> getOrigClazz();

	Serializable createEntity(Object obj) throws Exception;
//	public Serializable create(final EntityMapping emp,final T t) throws Exception;
		
	public PageIterator<T> findPageAsJoin(Param p, int paramInt1, int paramInt2)  throws Exception; 
	void deleteEntity(Class<?> claz,Serializable delId) throws Exception; 
	void delete(final Param param) throws Exception;  
}
