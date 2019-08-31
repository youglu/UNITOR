package org.chuniter.core.kernel.impl.service;

import org.chuniter.core.kernel.api.dao.GenericDao;
import org.chuniter.core.kernel.impl.GeneralServiceAdapter;
import org.chuniter.core.kernel.impl.orm.SimpORM;
import org.chuniter.core.kernel.kernelunit.Param;
import org.chuniter.core.kernel.model.BaseEntity;

public class UBaseService extends GeneralServiceAdapter<BaseEntity>{
	private UBaseService(){}
	private static class SingletonHolder{
		private static UBaseService instance = new UBaseService();
	}
	public static UBaseService getInstance(){
		return SingletonHolder.instance;
	}
	private GDao gdao = new GDao();
	@Override
	public void setGenericDao(GenericDao<BaseEntity> gd) throws Exception {

	}

	@Override
	public GenericDao<BaseEntity> getDaoInstance() {
		return gdao;
	}

	@Override
	protected Param fetchParam(BaseEntity t) {
		return null;
	}
	class GDao extends SimpORM<BaseEntity> {

	}
}
