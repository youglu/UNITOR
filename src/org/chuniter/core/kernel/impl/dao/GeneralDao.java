package org.chuniter.core.kernel.impl.dao;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.chuniter.core.kernel.api.dao.GenericDao;
import org.chuniter.core.kernel.api.dao.IConnectionHandler;
 

public abstract class GeneralDao<T> implements GenericDao<T>{
	protected Log log = LogFactory.getLog(super.getClass());
	protected IConnectionHandler  getProxConnectionHandler(IConnectionHandler ch,boolean needDefault) throws Exception{ 
		return ch; 
	}  
}
