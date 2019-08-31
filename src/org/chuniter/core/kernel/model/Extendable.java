package org.chuniter.core.kernel.model;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public abstract class Extendable implements Serializable {

	/**
	 * 
	 */
	private transient static final long serialVersionUID = 4376672483534511709L;
	private Map<String, Object> customProperty;

	public Extendable() {

	}

	public abstract String retrieveWhereClause();

	public CustomField setProperty(String propertyName, Object value) {
		if (customProperty == null)
			customProperty = new HashMap<String, Object>();
		CustomField val = new CustomField(propertyName, value);
		customProperty.put(propertyName, val);
		return val;
	}
	public void setProperty(CustomField val) {
		if (customProperty == null)
			customProperty = new HashMap<String, Object>(); 
		customProperty.put(val.getName(), val);
	}
	public Object fetchCustomProperty(String fname) {
		if (customProperty == null)
			return null;
		else
			return customProperty.get(fname);
	}

	public Map<String, Object> fetchCustomProperty() {
		if (customProperty == null)
			return Collections.emptyMap();
		else
			return customProperty;
	}
	public Map<String, Object> getCustomProperty() {
		//if (customProperty == null)
			//return null;//Collections.emptyMap();
		//else
			return customProperty;
	}
}
