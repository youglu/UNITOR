package org.chuniter.core.kernel.kernelunit;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import org.chuniter.core.kernel.api.IGeneralService;
import org.chuniter.core.kernel.model.BaseEntity;

public class ServiceDynamicProxyer implements InvocationHandler {

	//IGeneralService<BaseEntity> igernalService;
	public static ServiceDynamicProxyer instance = new ServiceDynamicProxyer();
	public static ServiceDynamicProxyer getInstance(){
		return instance;
	} 
	private ServiceDynamicProxyer(){}
	@Override
	public Object invoke(Object proxy, Method method, Object[] args)
			throws Throwable {
		// TODO Auto-generated method stub
		return null;
	}
	
	public Object invocaService(IGeneralService<? extends BaseEntity> igernalService,String methodName,Object[] args) throws SecurityException, NoSuchMethodException, Throwable{
		//this.igernalService = igernalService;
		return this.invoke(igernalService, igernalService.getClass().getMethod(methodName), args);
	}
}
