package org.chuniter.core.kernel.api.dao;

import java.sql.Connection;

public interface IConnectionExecutor<T> {

	T doConnection(Connection con) throws Exception;
}
