package org.chuniter.core.kernel.impl.unit;

import java.io.File;
import java.lang.ref.SoftReference;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.chuniter.core.kernel.KernelActivitor;
import org.chuniter.core.kernel.api.IGeneralService;
import org.chuniter.core.kernel.api.authorization.IPowerRegiditor;
import org.chuniter.core.kernel.api.authorization.PwoerEM;
import org.chuniter.core.kernel.api.orm.ISimpORM;
import org.chuniter.core.kernel.api.service.IGService;
import org.chuniter.core.kernel.api.unit.IUniterActivator;
import org.chuniter.core.kernel.api.unit.Unit;
import org.chuniter.core.kernel.extimpl.service.GService;
import org.chuniter.core.kernel.impl.authorization.PowerCatch;
import org.chuniter.core.kernel.impl.orm.ORMControler;
import org.chuniter.core.kernel.impl.service.UBaseService;
import org.chuniter.core.kernel.kernelunit.Param;
import org.chuniter.core.kernel.kernelunit.ReflectUtil;
import org.chuniter.core.kernel.kernelunit.StringUtil;
import org.chuniter.core.kernel.model.BaseEntity;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.http.HttpService;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import com.alibaba.fastjson.JSONObject;

public abstract class UniterActivatorAdaptor implements IUniterActivator{ 
	
	protected List<Unit> unitLis = new ArrayList<Unit>();
	protected Log log = LogFactory.getLog(super.getClass()); 
	private HttpServiceTracker httpservicetracker = null;
	private ComparatorUnit unitsort = new ComparatorUnit(); 

	protected BundleContext context;
	protected String path = ""; 
	protected String location = "/";
	protected long maxFileSize = 104857600;
	protected long maxRequestSize = 104857600;
	protected int fileSizeThreshold = 2621440;
	//private static final SoftReference<Map<String, Object>> serviceMap = new SoftReference<Map<String,Object>>(new HashMap<String,Object>());
	private ReflectUtil refutil = ReflectUtil.getInstance();
	protected IGeneralService<?> gservice = null;
	protected ServiceTracker<Object, Object> userviceTracker;
	protected Dictionary<String,String> dic = null;
	protected String powerCategory = "";
	protected String fpath = null; 
	
	
	private static Map<String, SoftReference<ClassLoader>> unitClassLaderCache = new HashMap<String, SoftReference<ClassLoader>>();
	public static void puttoclcache(String key,ClassLoader o) { 
		unitClassLaderCache.put(key, new SoftReference<ClassLoader>(o));
	}
	public static ClassLoader getclcache(String key) { 
		SoftReference<ClassLoader> so = unitClassLaderCache.get(key);
		if(null == so||so.get() == null)
			return null;
		return so.get();
	}
	public static void rmclcache(String key) { 
		unitClassLaderCache.remove(key);
	}
	public UniterActivatorAdaptor(){
		
	}
	public static String getSaveFilePath(Class<?> c,BundleContext...bc) {
		try{ 
			BundleContext b = null;
			if(c == null)
				c = UniterActivatorAdaptor.class;
			if(null != bc&&bc.length>0) {
				b = bc[0];
			}else {
				Bundle bb = FrameworkUtil.getBundle(c);
				b = bb.getBundleContext();
			}
			Dictionary<String,String> dic  = b.getBundle().getHeaders();
			Object fp = dic.get("fpath");
			if(null != fp)
				return fp.toString();
		}catch(Exception  e){
			e.printStackTrace();
		}
		return null;
	}
	@Override
	public final void start(BundleContext context_) throws Exception {
		context = context_; 
		dic = context.getBundle().getHeaders();
		Object fp = dic.get("fpath1");
		if(null != fp)
			fpath = fp.toString(); 
		else {
			fp = dic.get("fpath");
			if(null != fp)
				fpath = fp.toString(); 
		}
		path = context.getBundle().getHeaders().get("Web-ContextPath");
		httpservicetracker = new HttpServiceTracker(context);
		httpservicetracker.open(); 
		//这个最好在注册功能前调用
		//unRegisterPowerById(context_, this.getClass().getName());
		//end
		this.startup(context); 
		
		//发布自动任务事件订阅,用于在任务中心重启动，重新注册任务到任务中心。2018-10-21 12:13
		try {
			JobEventSubscriber subscriber = new JobEventSubscriber();
			Dictionary<String,Object> dict = new Hashtable<String,Object>();
			dict.put(EventConstants.EVENT_TOPIC,"busi/jobevent");
			context.registerService(EventHandler.class.getName(),subscriber, dict); 
			regeJob(null,null);
		}catch(Exception e) {
			e.printStackTrace();
		}
		try {
			Filter filter = context.createFilter("(|("+ Constants.OBJECTCLASS+"=org.eclipse.jetty.server.handler.ContextHandler))");// ("+Constants.OBJECTCLASS+"=org.fix.bbp.*) ("+Constants.OBJECTCLASS+"=org.unitor.bbp.*) ("+Constants.OBJECTCLASS+"=org.fix.sbp.*) ("+Constants.OBJECTCLASS+"=org.unitor.sbp.*) ("+Constants.OBJECTCLASS+"=org.chuniter.core.kernel.api.*))");
			userviceTracker = new ServiceTracker<Object, Object>(context, filter,serviceCusTracker);//"org.eclipse.jetty.server.handler.ContextHandler"
			userviceTracker.open();
		} catch (Exception e) {
			e.printStackTrace();
		}
		unitLis = this.getInteractors();  
		ORMControler ormc = ORMControler.getInstance((ISimpORM<?>) UBaseService.getInstance().getDaoInstance());
		ormc.RefreshEntity(Unit.class);
		if(null != unitLis&&!unitLis.isEmpty()){ 
			if(null == gservice)
				gservice = getGService(); 
			if(null == gservice)
				gservice = UBaseService.getInstance();
			Param p = Param.getInstance();  
			Map<String,String> dbuni = null;
			Map<String,Integer> needDisabledUnitId = new HashMap<String,Integer>();
			if(null != gservice){
				dbuni = new HashMap<String,String>(); 
				//p.addParam("unitId", "like",this.getClass().getName());
				List<Map<String,Object>> l = gservice.hisql("select unitId,id from Unit where  unitId is not null ", p,false);//(Unit.class, p);
				if(null != l&&!l.isEmpty())
					for(Map<String,Object> m:l) {
						dbuni.put(m.get("unitId").toString(), m.get("id").toString());
					}
			}
			Map<String,List<Unit>> allBundlesInfo = KernelActivitor.getUnitMap();
			Set<Entry<String,List<Unit>>> s = allBundlesInfo.entrySet();
			for(Unit unit:unitLis){
				if(!StringUtil.isValid(unit.getPlevel())&&!StringUtil.isValid(unit.getLevel()))
					unit.setLevel(this.getClass().getName()+"."+unit.getTitle());
				//设置为启用，因为在更新或卸载时设置其状态为停用 
				unit.setEstate(BaseEntity.ENABLE);
				//强制设置为当前启动类为前缀的UNITID， 
				unit.setUnitId(this.getClass().getName()+"."+unit.getTitle());  
				unit.setCatalog(this.getClass().getName());
				//处理不同单元设置另一单元为父菜单情况  . youg 2016-05-11 21:04
				if(null == unit.getParentLevel()&&StringUtil.isValid(unit.getPlevel())){ 
					for(Entry<String,List<Unit>> e:s){
						List<Unit> us = e.getValue();
						if(null == us||us.isEmpty())
							continue;  
						for(Unit u:us){
							if(null == u.getLevel())
								continue;
							if(u.getLevel().equals(unit.getPlevel())){
								unit.setParentLevel(u);
								break;
							}
							if(null == u.getPlevel())
								continue;
							if(u.getPlevel().equals(unit.getLevel())){
								u.setParentLevel(unit);
								break;
							} 
						}
					}
				}
				//end youg
				if(null != gservice){  
					if(dbuni.containsKey(unit.getUnitId())){
						unit.setId(dbuni.get(unit.getUnitId())); 
						p.clear();
						p.addParam("id", dbuni.get(unit.getUnitId()));//.addParam("unitid", unit.getUnitId());
						//处理单元的层级
						unit.fetchUnitPath(gservice);
						try{gservice.updateEntity(unit);}catch(Exception e) {log.error(e.getMessage());} 
					}else {
						//处理单元的层级
						unit.fetchUnitPath(gservice);
						gservice.createEntity(unit);
						//加入到不禁用集合
						dbuni.put(unit.getUnitId(), unit.getId());
					}
				}else if(allBundlesInfo.containsKey(this.getClass().getName())){ 
					for(Unit existunit:allBundlesInfo.get(this.getClass().getName())) 
						if(existunit.equals(unit))
							unit.setId(existunit.getId());
				}
				if(null != dbuni&&!dbuni.containsKey(unit.getUnitId()))
					needDisabledUnitId.put(unit.getUnitId(),1);
				else {
					p.clear();
					gservice.hisql("update Unit set estate="+unit.getEstate()+" where unitId='"+unit.getUnitId()+"'", p,false); 
				}
			}
			//清理DB中无效的单元配置
			if(null != needDisabledUnitId&&!needDisabledUnitId.isEmpty()){
				for(Entry<String,Integer> e:needDisabledUnitId.entrySet()){
					p.clear();
					p.addParam("unitId", e.getKey()); 
					//gservice.deleteEntity(Unit.class, p);  
					gservice.hisql("update Unit set estate="+BaseEntity.DISABLED+" where unitId='"+e.getKey()+"'", p); 
				}
			}
			allBundlesInfo.put(this.getClass().getName(),unitLis);
			if(null != dbuni)
				dbuni.clear();
			dbuni = null;
			if(null != needDisabledUnitId)
				needDisabledUnitId.clear();
			needDisabledUnitId = null; 
		}
		puttoclcache(this.getClass().getName(), this.getClass().getClassLoader());
	}
	
	@Override
	public final void stop(BundleContext context) throws Exception {
		this.shutdown(context); 
		KernelActivitor.getUnitMap().remove(this.getClass().getName());  
		if(null != unitLis&&!(unitLis.isEmpty())){
			//清理DB中无效的单元配置
			if(null != this.gservice||null !=(gservice = this.getGService())){
				Param p = Param.getInstance();
				for(Unit u:unitLis){
					p.clear();
					p.addParam("unitId", u.getUnitId());
					//gservice.deleteEntity(Unit.class, p);
					gservice.hisql("update Unit set estate="+BaseEntity.DISABLED+" where unitId='"+u.getUnitId()+"'", p,false); 
				}
			}
			unitLis.clear();
			unitLis = null;
			unRegisterPowerById(context,this.getClass().getName());
		}
		if(null != httpservicetracker)
			httpservicetracker.close();
		if(null != userviceTracker)
			userviceTracker.close();
		rmclcache(this.getClass().getName());
	}
	public static void cleanOrmEntity(Class<?> c) { 
		try {
			// 获得ORM控制类
			ORMControler ormc = ORMControler.getInstance(null);
			// 清理ORM映射
			ormc.unRegeditEntity(c);
		} catch (Exception e) { 
			e.printStackTrace();
		}
	}
	public void regeEntity(Class<?> c) { 
		try {
			if(null == gservice)
				gservice = getGService(); 
			if(null == gservice)
				gservice = UBaseService.getInstance();
			ORMControler ormc = ORMControler.getInstance((ISimpORM<?>) gservice.getDao());
			ormc.regeditEntity(c);
		} catch (Exception e) { 
			e.printStackTrace();
		}
	}
	protected ServiceTrackerCustomizer<Object, Object> serviceCusTracker = new ServiceTrackerCustomizer<Object, Object>() {
		@Override
		public Object addingService(ServiceReference<Object> reference) { 
			Object service = context.getService(reference);   
			try {if (service.getClass().getName().equals("org.eclipse.jetty.webapp.WebAppContext")){proWebContextInfo(reference);}} catch (Exception e1) {e1.printStackTrace();}  		 			 
			return service;
		}
		@Override
		public void modifiedService(ServiceReference<Object> reference,Object service) {}
		@Override
		public void removedService(ServiceReference<Object> reference,Object service) {}
	};
	@Override
	public String enterUnit(String unitId) {
		return null;
	}
	@Override
	public List<Unit> getInteractors() {
		return null;
	}
	
	public Map<String,List<Unit>> getAllBundelsInfo(){ 
		return  KernelActivitor.getUnitMap();
	}
	public Map<String,List<Unit>> getAllBundelsInfoByLevel(String level){
		Map<String,List<Unit>> leveUnitMap = null;
		if(!StringUtil.isValid(level))
			return null;
		leveUnitMap = new LinkedHashMap<String,List<Unit>>();
		List<Unit> leveUnits = new ArrayList<Unit>();
		for(Entry<String,List<Unit>> ent: KernelActivitor.getUnitMap().entrySet())
			for(Unit unit:ent.getValue())
				if(level.equals(unit.getLevel()))
					leveUnits.add(unit);
		
		leveUnitMap.put(level, leveUnits);
		return leveUnitMap;
	}	
	class ComparatorUnit implements Comparator<Unit>{
		 public int compare(Unit arg0, Unit arg1) {
			 Unit unit1=(Unit)arg0;
			 Unit unit2=(Unit)arg1; 
			 if(null == unit1.getShowOrder())
				 return 1;
			 if(null == unit2.getShowOrder())
				 return 0;
			 int flag=unit1.getShowOrder().compareTo(unit2.getShowOrder());
			 try{
			 if(flag==0)
				 flag = unit1.getTitle().compareTo(unit2.getTitle());
			 }catch(Exception e){log.error(e.getMessage());}
			 return flag;
		 }
	}
	public Map<String,List<Unit>> geSubBundelsInfoByLevel(String plevel){
		Map<String,List<Unit>> allBundlesInfo = KernelActivitor.getUnitMap();
		if(null == allBundlesInfo||allBundlesInfo.isEmpty())
			return null;
		Map<String,List<Unit>> leveUnitMap = new LinkedHashMap<String,List<Unit>>();
		List<Unit> leveUnits = new ArrayList<Unit>();
		if(!StringUtil.isValid(plevel)){
			for(Entry<String,List<Unit>> ent:allBundlesInfo.entrySet())
				for(Unit unit:ent.getValue())
					if(null == unit.getParentLevel())
						leveUnits.add(unit);
		}else
			for(Entry<String,List<Unit>> ent:allBundlesInfo.entrySet())
				for(Unit unit:ent.getValue())
					if(plevel.equals(unit.getPlevel()))
						leveUnits.add(unit);
		Collections.sort(leveUnits,unitsort);
		leveUnitMap.put(plevel, leveUnits);
		return leveUnitMap;
	}	
	/**
	 * @deprecated 请使用 无BundleContext参数方法。
	 * @param bc
	 * @param serviceName
	 * @return
	 */
	public static <S>  S getService(BundleContext bc, String serviceName){
		/*try{			 
			ServiceReference<?> sf = bc.getServiceReference(serviceName);
			if(null == sf)
				return null;
			return bc.getService(sf);
		}catch(Exception e){
			e.printStackTrace();
			System.err.println("根据服务名获得服务出错"+e.getMessage());
		}*/
		return getService(serviceName);
	}
	public static <S>  S getService(String serviceName){
		try{			 
			if(!StringUtil.isValid(serviceName))
				return null;
			Bundle b = FrameworkUtil.getBundle(UniterActivatorAdaptor.class);
			if(null == b) {
				System.err.println(UniterActivatorAdaptor.class.getName()+".getService("+serviceName+") 单元上下文为空");
				return null;
			}
			BundleContext bc = b.getBundleContext();
			if(null == bc) {
				System.err.println("无法获得获得服务的BUNDLECONTEXT"+bc);
				return null;
			}
			ServiceReference<?> sf = bc.getServiceReference(serviceName);
			if(null == sf) {
				//System.err.println(UniterActivatorAdaptor.class.getName()+".getService("+serviceName+") 服务引用为空");
				return null;
			}
			return (S) bc.getService(sf);
		}catch(Exception e){
			e.printStackTrace();
			System.err.println("根据服务名获得服务出错"+e.getMessage());
		}
		return null;
	}
	protected void registerPower(IPowerRegiditor pwRegitor,String pid,String pname,String category,String moduleName){
		//注册功能
		if(null != pwRegitor){
			pwRegitor.registerPower(pid,pname,category,moduleName);
		}else{
			PowerCatch.catchPower(pid,pname,category,moduleName);
		}
	}
	/**
	 * 以完整的功能路径进行注册，其中pid包括了功能名称，放在最后
	 * @param pwRegitor
	 * @param pid
	 * @param category
	 * @param moduleName
	 */
	protected void registerPowerByFullId(IPowerRegiditor pwRegitor,String pid,String category,String moduleName){
		//注册功能
		this.registerPowerByFullId(pwRegitor, pid, category, moduleName,null);
	}	
	protected void registerPowerByFullId(IPowerRegiditor pwRegitor,String pid,String category,String moduleName,String iconurl){
		//注册功能
		String pname = pid;
		String unitId = this.getClass().getName()+"."+moduleName;
		if(pid.indexOf(".") != -1){
			pname = pid.substring(pid.lastIndexOf(".")+1);
			pid = pid.substring(0,pid.lastIndexOf("."));
		}else{
			pid = unitId;
		}
		//检查分类是否有，如无分类，则在菜单界面上会显示不了
		if(!StringUtil.isValid(category)){
			List<Unit> units = KernelActivitor.getUnitMap().get(this.getClass().getName());
			if(null != units&&!units.isEmpty()){
				for(Unit u:units){
					if((u.getTitle().equals(moduleName))||(null != u.getUnitId() && u.getUnitId().equals(pid))){
						category = u.getPlevel();
						if(!StringUtil.isValid(category))
							category = u.getTitle();
						break;
					}
				}
			}
		}
		
		if(null != pwRegitor){
			pwRegitor.registerPower(pid,pname,category,moduleName,iconurl,unitId);
			return;
		} 
		if(null == gservice)
			gservice = getGService(); 
		if(null != gservice){
			Param p = Param.getInstance();  
			p.addParam("modelId",pid);
			p.addParam("name",pname);
			try { 
				StringBuilder sb = new StringBuilder();
				UMap m = gservice.hisqlOne("select count(1) c from PowerEntity where 1=1",p,false); 
				if(null == m||m.isEmpty()||m.getInteger("c") == null||m.getInteger("c")<=0){ 
					p.clear();
					sb.append("insert  into PowerEntity(id,modelId,name,unitId,modelName) values('").append(UUID.randomUUID().toString()).append(".").append(pname).append("','").append(pid).append("','").append(pname).append("','").append(unitId).append("','").append(moduleName).append("')");
				}else {
					sb.append("update PowerEntity set modelId='").append(pid).append("',modelName='").append(moduleName).append("',name='").append(pname).append("',unitId='").append(unitId).append("' where modelId='").append(pid).append("'");
				}
				gservice.hisql(sb.toString(),p);
			}catch(Exception e){e.printStackTrace();}
		}
		PowerCatch.catchPower(pid,pname,category,moduleName,iconurl);
		 
	}
	/**
	 *  
	 * @param bc
	 * @param pid 完整的功能路径，包括unitid,title,功能名
	 * @param unitTitle
	 */
	protected void regeditPower(BundleContext bc, String pid,
			String unitTitle) {
		this.regeditPower(bc, pid, powerCategory, unitTitle);
	}
	protected void regeditPower(BundleContext bc, String pid) {
		String[] ps = pid.split("\\."); 
		this.regeditPower(bc, pid, powerCategory, ps[ps.length-1]);
	}
	protected void unRegisterPowerById(IPowerRegiditor pwRegitor,String pid){
		//注册功能
		if(null != pwRegitor){
			pwRegitor.unRegisterPowerById(pid);
		}else{
			PowerCatch.removePowerFormCatch(pid);
		}
	}
	
	protected void regeditHttpInfo(ServiceReference<?>  reference){}
	protected void removeHttpInfo(ServiceReference<?> reference,Object service){} 
	protected <T> void  proWebContextInfo(ServiceReference<T>  reference){ 
		Object contextv = reference.getProperty("osgi.web.contextpath");
		if(null == contextv ||null == path || !path.equals(contextv))
			return;   
		Object service = context.getService(reference);
		if(service.getClass().getSimpleName().equals("WebAppContext")){  
			try {
				ReflectUtil.getInstance().setFieldValues(this, "webappconent", service);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} 
		regeditHttpInfo(reference);
	}
	
	public class HttpServiceTracker extends ServiceTracker<Object,Object> {
		public HttpServiceTracker(BundleContext context) {
			super(context, HttpService.class.getName(), null);
		}
		public Object addingService(ServiceReference<Object>  reference) {
			Object result = super.addingService(reference);
			if (!(result instanceof HttpService))
				return result;
			final HttpService httpService = (HttpService) context.getService(reference);
			try {regeditHttpInfo(reference);} catch (Exception e1) {e1.printStackTrace();}
			return httpService;
		}
		public void removedService(ServiceReference<Object> reference,Object service) { 
			removeHttpInfo(reference, service);
		}
	}
	protected void unRegisterPowerById(BundleContext bc,String pid){
		IPowerRegiditor pwRegitor = null;
		try{
			//获得功能注册服务
			pwRegitor = (IPowerRegiditor)getService(bc,IPowerRegiditor.class.getName());
			if(null == pwRegitor)
				System.err.println(" 功能管理服务尚未注册发布，暂无法获得。");
		}catch(Exception e){
			e.printStackTrace();
			System.err.println("无法获得功能管理服务，将无法注册功能");
		}
		this.unRegisterPowerById(pwRegitor, pid);
	}
	protected void regeditPower(BundleContext bc,String powerName,String powerType,String unitTitle){
		this.regeditPower(bc, powerName, powerType, unitTitle,null);
	}
	protected void regeditPower(BundleContext bc,PwoerEM powerName,String powerType,String unitTitle){
		this.regeditPower(bc, powerName.getDescription(), powerType, unitTitle,null);
	}
	protected void regeditPower(BundleContext bc,PwoerEM powerName,String unitTitle){
		//String powerType = this.powerCategory;
		if("".equals(powerCategory)){
			
			if(this.getClass().getName().indexOf("sbp") != -1)
				powerCategory = "系统";
		}
		
		this.regeditPower(bc, powerName.getDescription(),powerCategory , unitTitle,null);
	}
	protected void regeditPower(BundleContext bc,String powerName,String powerType,String powerUtil,String iconurl){
		IPowerRegiditor pwRegitor = null;
		try{
			//获得功能注册服务
			pwRegitor = (IPowerRegiditor)getService(bc,IPowerRegiditor.class.getName());
		}catch(Exception e){
			e.printStackTrace();
			System.err.println("无法获得功能管理服务，将无法注册功能");
		}
		this.registerPowerByFullId(pwRegitor,powerName,powerType,powerUtil,iconurl);
	}

	
	/**
	 * 获得自已所需要的服务,此方法通过传入一个需要获得自已需要的服务对象，并解析此对象的service注解来自动帮其设置相应的服务对象到其实体中
	 * @author yonglu
	 * @time 2015-10-14 12:11
	 * @param fetchObj
	 */
	public void fetchService(Object fetchObj){
		KernelActivitor.fetchService2(fetchObj); 
	}
	
	public void fetchService(Object fetchObj,Object service){
		if(null == fetchObj)
			return;  
		//获得当前要获得的服务的对象的所有的属性，并从中提取有@service注解的
		Field[] fds = ReflectUtil.getInstance().getServiceFiled(fetchObj.getClass());
		if(null == fds||fds.length<=0)
			return;
		for(Field f:fds)
			if(service.getClass().getName().equals(f.getType().getName()))
				try { refutil.setFieldValues(fetchObj, f, service); } catch (Exception e) {e.printStackTrace();}
	} 
	protected <S extends BaseEntity> IGeneralService<?> getGService(){
		return gservice;
	}
	protected String fetchFilePath(String bfPath){ 
	    return getFilePath(context,bfPath); 
	}
	public static String getFilePath(BundleContext c,String bfPath){
		if(!bfPath.startsWith("/"))
			bfPath ="/"+bfPath;
		String blocation = c.getBundle().getLocation().replaceAll("reference:file:", "");
	    File f = new File(blocation+bfPath);  
	    return f.getAbsolutePath(); 
	} 
	public String getConfigValue(String key){
		return dic.get(key);
	}
	public void regPower(String t){
		regPower(t,this.powerCategory);
	}
	public void regPower(String powerTitle,String pCategory){
		if(!StringUtil.isValid(pCategory))
			pCategory = powerCategory;
		if(!StringUtil.isValid(pCategory)){
			log.error("注册功能发现没有功能分类!!");
			pCategory = powerCategory = "其它";
		}
		this.regeditPower(context, PwoerEM.FIND, pCategory, powerTitle);// 查询
		this.regeditPower(context, PwoerEM.DELETE, pCategory,powerTitle);// 删除
		this.regeditPower(context, PwoerEM.UPDATE, pCategory, powerTitle);// 更新
		this.regeditPower(context, PwoerEM.CREATE, pCategory, powerTitle);// 新建
		this.regeditPower(context, PwoerEM.AUDIT, pCategory, powerTitle);// 审核
		this.regeditPower(context, PwoerEM.EXPORT, pCategory, powerTitle);// 导出
		this.regeditPower(context, PwoerEM.IMPORT, pCategory, powerTitle);// 导入
		this.regeditPower(context, PwoerEM.ENABLE, pCategory, powerTitle);// 启用
		this.regeditPower(context, PwoerEM.DISABLE, pCategory, powerTitle);// 停用
	}
	
	//事件绑定
	protected void regeJob(String eventTopic,JSONObject j) {
		if(null == j)
			return;
		if(!j.containsKey("jobclass")) {
			log.error("请设置任务执行类");
			return;
		}
		IGService gservice = new GService("org.unitor.sbp.autojobapi.service.IJobService");
		//重新注册事件 
		try {  
			gservice.call("registeJob",new Object[] {j});
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
	protected void removeJob(String jobName) { 
		IGService gservice = new GService("org.unitor.sbp.autojobapi.service.IJobService"); 
		try {  
			gservice.call("removeJob",new Object[] {jobName});
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
	private final class JobEventSubscriber implements EventHandler { 
		protected Log log = LogFactory.getLog(super.getClass());
		public void handleEvent(Event event) {
			log.error(" 收到事件,主题为:" + event.getTopic());
			for (String propertyName : event.getPropertyNames()) {
				System.out.println("\t" + propertyName + " = " + event.getProperty(propertyName));
			}
			regeJob(event.getTopic(),null);
		}
	}
	
}
