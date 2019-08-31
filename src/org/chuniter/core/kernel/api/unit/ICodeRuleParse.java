package org.chuniter.core.kernel.api.unit;

import org.chuniter.core.kernel.api.IGeneralService;

public interface ICodeRuleParse {
	
	String parseCodeRule(IGeneralService<?> service, String entityClass, String orgId);

}
