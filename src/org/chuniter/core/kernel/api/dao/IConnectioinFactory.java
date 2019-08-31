package org.chuniter.core.kernel.api.dao;


public interface IConnectioinFactory {

	String DEFAULTFT= "(default=yes)";
	String DUFAULTKEY = "default";
	IConnectionHandler createConnection();
	IConnectionHandler createConnection(String serviceFilterStr);
}
