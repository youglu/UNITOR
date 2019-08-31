package org.chuniter.core.kernel.api.unit;

import java.util.Date;
import java.util.Map;

public interface IUMap  extends Map<String,Object>{

	String getString(String key);
	Float getFloat(String key);
	Double getDouble(String key);
	Integer getInteger(String key);
	Boolean getBoolean(String string);
	Date getDate(String key);
	<S> S g(String key);

}
