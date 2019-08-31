package org.chuniter.core.kernel.impl.cache;

import org.chuniter.core.kernel.api.cache.IUCache;

import org.chuniter.core.kernel.api.cache.UCacheAdaptor; 

public class UCache extends UCacheAdaptor{  
	private static IUCache instance;
	public static synchronized IUCache getCacheInstance() { 
		if(null == instance)
			instance = new UCache();
		return instance;
	}
}
