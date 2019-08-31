package org.chuniter.core.kernel.api.orm;

import org.chuniter.core.kernel.api.dao.IConnectionHandler;
import org.chuniter.core.kernel.impl.orm.EntityMapping;



public interface IORMControler {
	
	String RefreshEntity(Object entity) throws Exception ;
	String RefreshEntity(Class<?> clazz) throws Exception;
	EntityMapping regeditEntity(Class<?> clazz) throws Exception;
	EntityMapping regeditEntity(final Class<?> t,IConnectionHandler conHandler) throws Exception;
	EntityMapping regeditEntity(final Class<?> t,final ISimpORM<?> simpORM) throws Exception;
	void unRegeditEntity(Class<?> clazz) throws Exception;
	/**
	 * 更新实体映射
	 * @param c
	 * @throws Exception
	 */
	void refreshMapping(Class<?> c)throws Exception;
	EntityMapping fetchMappingInfo(ISimpORM<?> isimpORM) throws Exception;
	EntityMapping fetchMappingInfo(ISimpORM<?> simpORM, Class<?> clazz)
			throws Exception;
	
}
