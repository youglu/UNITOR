package org.chuniter.core.kernel.api.unit;

import java.sql.Connection;

public interface IDoSQL<T>  {

	T doSql(Connection con);
}
