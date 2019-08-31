package org.chuniter.core.kernel;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.chuniter.core.kernel.api.unit.AbstratorServiceListenerActivator;
import org.chuniter.core.kernel.api.unit.Unit;
import org.chuniter.core.kernel.impl.web.BaseServlet;
import org.chuniter.core.kernel.kernelunit.ReflectUtil;
import org.chuniter.core.kernel.kernelunit.StringUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceReference;

public class KernelActivitor extends AbstratorServiceListenerActivator{

	private  static BundleContext context; 
	private final static  Map<String, Object> serviceMap = new HashMap<String,Object>();
	protected static Map<String,List<Unit>> allBundlesInfo = new HashMap<String,List<Unit>>();
	private static ReflectUtil refutil = ReflectUtil.getInstance();
	
	public static BundleContext gContext(){
		return context;
	}
	public static Map<String, Object> getServiceMap(){
		return serviceMap;
	}
	public static  Map<String,List<Unit>> getUnitMap(){
		return allBundlesInfo;
	}
	public static Object getService(String serviceName) throws NullPointerException{
		if(null == KernelActivitor.context)
			return null;
		ServiceReference<?> s = KernelActivitor.context.getServiceReference(serviceName);
		if(null == s)
			return null;
		return KernelActivitor.context.getService(s);
	} 
	public void startup(BundleContext bundleContext) throws Exception {
		context = bundleContext; 
		String serviceFiltstr = "(|("+ Constants.OBJECTCLASS+"=org.eclipse.jetty.server.handler.ContextHandler) ("+Constants.OBJECTCLASS+"=org.fix.bbp.*) ("+Constants.OBJECTCLASS+"=org.unitor.bbp.*) ("+Constants.OBJECTCLASS+"=org.fix.sbp.*) ("+Constants.OBJECTCLASS+"=org.unitor.sbp.*) ("+Constants.OBJECTCLASS+"=com.unitor.sbp.*) ("+Constants.OBJECTCLASS+"=org.chuniter.core.kernel.api.*))";
		bundleContext.addBundleListener(this); 
		bundleContext.addServiceListener(this,serviceFiltstr);
		//serviceCusTracker = overserviceCusTracker;
		serviceCusTracker = null;
		String serverurl=context.getProperty("unitor.server.url");
		if(StringUtil.isValid(serverurl)){
			BaseServlet.SERVERURL = serverurl; 
			BaseServlet.PHOTOURL = BaseServlet.SERVERURL+"/FIX/member/lzh?mname=findphoto&headimgPath=";
		} 
		System.out.println("启动核心activitor完毕。"+BaseServlet.SERVERURL+"  \n"+BaseServlet.PHOTOURL);
	}
	public void shutdown(BundleContext bundleContext) throws Exception {
		bundleContext.removeBundleListener(this);
		if(null != userviceTracker)
			userviceTracker.close();
		KernelActivitor.context = null;
		System.out.println("停止核心activitor完毕。");
	}
	@Override
	public String enterUnit(String unitId) { 
		return null;
	}
	@Override
	public List<Unit> getInteractors() { 
		return null;
	}
	@Override
	protected BundleContext getBundleContext() { 
		return context;
	} 
	public static void sendUInfo(){
		 try { String p = URLEncoder.encode("param1", "UTF-8") + "=" + URLEncoder.encode("value1", "UTF-8");
	            p += "&" + URLEncoder.encode("param2", "UTF-8")  + "=" + URLEncoder.encode("value2", "UTF-8"); 
	            String h = "isunitor.com"; int port = 80; 
	            InetAddress a = InetAddress.getByName(h); Socket so = new Socket(a, port);String pa = "/myapp"; 
	            BufferedWriter wr = new BufferedWriter(new OutputStreamWriter(so.getOutputStream(), "UTF8"));
	            wr.write("POST "+pa+" HTTP/1.0\r\n");
	            wr.write("Content-Length: "+p.length()+"\r\n");
	            wr.write("Content-Type: application/x-www-form-urlencoded\r\n");
	            wr.write("\r\n"); 
	            wr.write(p); wr.flush(); 
	            BufferedReader rd = new BufferedReader(new InputStreamReader(so.getInputStream()));
	            String l; 
	            while ((l = rd.readLine()) != null) { System.out.println(l);} 
	            wr.close();
	            rd.close(); }  catch (Exception e) {e.printStackTrace();}
	}
	@Override
	public final void serviceChanged(ServiceEvent serviceEvent) { 
	  	ServiceReference<?> sref = serviceEvent.getServiceReference();//getBundleContext().getServiceReference(IUniterService.class.getName());
		if(null == sref)
			return;
		Object service = getBundleContext().getService(sref); 
		String servicename = ((String[])sref.getProperty("objectClass"))[0]; 
		switch(serviceEvent.getType()){
			case ServiceEvent.REGISTERED: 
			case ServiceEvent.MODIFIED: 
				if(!serviceMap.containsKey(servicename))
					serviceMap.put(servicename, service); 
				break;
			case ServiceEvent.UNREGISTERING:  
				serviceMap.remove(servicename);
				break;
		} 
	}
	/**
	 * 单元监听
	 * @author youg
	 */
	@Override
	public final void bundleChanged(BundleEvent be) {
		Bundle bund = be.getBundle();		
		switch(be.getType()){
		case BundleEvent.STOPPED:
			allBundlesInfo.remove(bund.getBundleId());
		}
	} 
	/**
	 * 获得自已所需要的服务,此方法通过传入一个需要获得自已需要的服务对象，并解析此对象的service注解来自动帮其设置相应的服务对象到其实体中
	 * @author yonglu
	 * @time 2015-10-14 12:11
	 * @param fetchObj
	 */
	public static void fetchService2(Object fetchObj){
		if(null == fetchObj)
			return; 
		//获得当前要获得的服务的对象的所有的属性，并从中提取有@service注解的
		Field[] fds = refutil.getServiceFiled(fetchObj.getClass());
		if(null == fds||fds.length<=0)
			return; 
		boolean isfind = false;
		for(Field f:fds){  
			//System.out.println(f.getType().getName()+"   --  "+serviceMap);
			boolean ismatch = false;
			if(serviceMap.containsKey(f.getType().getName())){
				ismatch = true;
			}
			if(!ismatch){
				Class<?> c = f.getType().getSuperclass();
				while(c!=null&&!ismatch){
					if(serviceMap.containsKey(c.getName())){
						ismatch = true;
						break;
					}
					c = c.getSuperclass();
				}
			}else{
				try {  
					ClassLoader cc = Thread.currentThread().getContextClassLoader();
					Thread.currentThread().setContextClassLoader(serviceMap.get(f.getType().getName()).getClass().getClassLoader());
					refutil.setFieldValues(fetchObj, f, serviceMap.get(f.getType().getName())); 
					isfind = true;
					Thread.currentThread().setContextClassLoader(cc);
					continue;
				} catch (Exception e) {e.printStackTrace();}
				
			}
			//迭代所有已知服务
			for(Entry<String,Object> ent:serviceMap.entrySet()){ 
				//System.out.println(f.getType().getName()+"   :  "+ent.getValue());
				if(f.getType().getName().equals(ent.getValue().getClass().getName())||f.getType().isInstance(ent.getValue())||ent.getValue().getClass().isInstance(f.getType())){
					/*List<Object> fobjs = fetchServiceMap.get().get(ent.getValue().getClass().getName());
					if(null == fobjs){
						fobjs = new ArrayList<Object>();
						fobjs.add(fetchObj);
						fetchServiceMap.get().put(ent.getValue().getClass().getName(), fobjs);
					}*/
					try { refutil.setFieldValues(fetchObj, f, ent.getValue());isfind = true;break; } catch (Exception e) {e.printStackTrace();}
				}
			}
			if(!isfind&&null != context){
				ServiceReference<?> sf = context.getServiceReference(f.getType().getName());
				if(null != sf){
					Object o = context.getService(sf);
					if(null != o){
						serviceMap.put(f.getType().getName(), o);
						try {refutil.setFieldValues(fetchObj, f, o); isfind = true;} catch (Exception e) {e.printStackTrace();}
					} 
				}
			}
		} 
	}
}
