package org.chuniter.core.kernel.api.unit;

import java.util.List;
import java.util.Map;


public interface IUniterService{
	
	String enterUnit(String unitId);
	/**
	 * 获得该bundle的互动实体
	 * @author youg
	 * @time 2012-01-13
	 * @return
	 */
	List<Unit> getInteractors();
	
	Map<String,List<Unit>> getAllBundelsInfo();
}
