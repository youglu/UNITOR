package org.chuniter.core.kernel.impl.dao;

import java.sql.Connection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.chuniter.core.kernel.api.dao.IConnectionCreator;
import org.chuniter.core.kernel.api.dao.IConnectionExecutor;
import org.chuniter.core.kernel.api.dao.IConnectionHandler;

public class ConnectionHandler implements IConnectionHandler{

	private Connection con;
	private IConnectionCreator cc;
	protected Log log = LogFactory.getLog(ConnectionHandler.class);
	
	public <T> Object proConnection(IConnectionExecutor<T> ic)throws Exception{
		try{
			con = getCon();
			T t = ic.doConnection(con);
			return t;
		}catch(Exception e){
			throw e;
		}finally{
			if(null != con){				
				con.close();
				log.info("关闭连接.");
			}
		}
	}
	public void setCc(IConnectionCreator cc_) {
		this.cc = cc_;
	}
	@Override
	public Connection getCon() throws Exception {
		if(null != cc) { 
			return cc.getRelCon();
		}
		return null;
	}

}
