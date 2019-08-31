package org.chuniter.core.kernel.api.unit;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;


public interface IUniterActivator extends BundleActivator,IUniterService{
	void startup(BundleContext context) throws Exception;
	void shutdown(BundleContext context) throws Exception;
}
