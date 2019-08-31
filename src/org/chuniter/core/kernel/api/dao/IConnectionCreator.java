package org.chuniter.core.kernel.api.dao;

import java.sql.Connection;


public interface IConnectionCreator {
	IConnectionHandler getConnection();
	Connection getRelCon() throws Exception;
}
