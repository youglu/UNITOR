package org.chuniter.core.kernel.api.dao;

import java.io.Serializable;

import org.chuniter.core.kernel.model.Extendable;


public interface IDaoConfiguer {
	/**
	 * Serializable 如为hibernate，则为org.hibernate.SessionFactory
	 * @return
	 */
	public Serializable getSessionFactory();
	public boolean regeditEntity(Class<? extends Extendable> clazz) throws Exception;
	public void unRegeditEntity(Class<? extends Extendable> clazz)throws Exception;
	
}
