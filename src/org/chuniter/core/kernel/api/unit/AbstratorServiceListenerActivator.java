package org.chuniter.core.kernel.api.unit;

import org.chuniter.core.kernel.impl.unit.UniterActivatorAdaptor;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleListener;
import org.osgi.framework.ServiceListener;

public abstract class AbstratorServiceListenerActivator extends UniterActivatorAdaptor implements BundleListener,IUniterActivator,ServiceListener{
  
	protected abstract BundleContext getBundleContext();  
}
