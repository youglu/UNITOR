package org.chuniter.core.kernel.api.dao;

import java.sql.Connection;

public interface IConnectionHandler{ 
	public <T> Object proConnection(IConnectionExecutor<T> ic)throws Exception;
	public void setCc(IConnectionCreator cc_);
	public Connection getCon() throws Exception;
}
