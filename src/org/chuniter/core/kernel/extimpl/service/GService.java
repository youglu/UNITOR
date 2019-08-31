package org.chuniter.core.kernel.extimpl.service;

import java.lang.ref.SoftReference;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.chuniter.core.kernel.KernelActivitor;
import org.chuniter.core.kernel.api.service.IGService;
import org.chuniter.core.kernel.kernelunit.ReflectUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

/**
 * 通用服务调用实现类
* @Title: GService.java 
* @Package org.chuniter.core.kernel.impl.service 
* @Description: TODO
* @author youg continentlu@sina.com
* @date 2017年11月20日 下午4:09:51 
* @version V1.0
 */
public class GService implements IGService{ 
	private String serviceFullName = null; 
	private Object serviceInstance;
	private ReflectUtil refutil = ReflectUtil.getInstance();
	private Map<String, SoftReference<Method>> map=new HashMap<String, SoftReference<Method>>(); 
	private SoftReference<Method> reference;
	private Map<String, Long> tm=new HashMap<String, Long>();  
	public GService(String serviceFullName) {
		 this.serviceFullName = serviceFullName;
	}
	public Object call(String methodName,Object[] args)throws Exception{
		Bundle b = FrameworkUtil.getBundle(KernelActivitor.class); 
		return this.call(b.getBundleContext(), methodName,args);
	}
	public Object call(BundleContext ctx,String methodName,Object[] args)throws Exception{
		if(null == serviceInstance) {
			serviceInstance = KernelActivitor.getService(ctx, serviceFullName); 
		}
		if(null == serviceInstance) {
			System.out.print("无法根据"+serviceFullName+"获得服务实例");
			return null;
		}
		String tk = "t_"+serviceFullName+"."+methodName;
		long ct = new Date().getTime();
		Method m = null;
		if(tm.containsKey(tk)) {
			long precalltime = tm.get(tk); 
			String mk = serviceFullName+"."+methodName;
			if(ct-precalltime<10000) { 
				if(map.containsKey(mk)) {
					reference = map.get(mk);
					m = reference.get();
				} 
			}else {
				map.remove(tk);
				tm.remove(tk);
			}
		}
		if(null == m)
			m = refutil.getMethod(serviceInstance.getClass(), methodName);
		if(null == m) {
			System.out.print("本服务找不到方法："+methodName);
			return null;
		}
		Object o = m.invoke(serviceInstance, args);  
		reference=new SoftReference<Method>(m);
		map.put(serviceFullName+"."+methodName,reference); 
		tm.put("t_"+serviceFullName+"."+methodName,ct);  
		return o;
	}
}
