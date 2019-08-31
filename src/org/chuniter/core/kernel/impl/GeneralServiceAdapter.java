package org.chuniter.core.kernel.impl;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.chuniter.core.kernel.KernelActivitor;
import org.chuniter.core.kernel.api.IGeneralService;
import org.chuniter.core.kernel.api.IMessageListenerContianer;
import org.chuniter.core.kernel.api.authorization.IAuthorization;
import org.chuniter.core.kernel.api.dao.GenericDao;
import org.chuniter.core.kernel.api.exception.CannotEditException;
import org.chuniter.core.kernel.api.exception.LockedEditException;
import org.chuniter.core.kernel.api.orm.ISimpORM;
import org.chuniter.core.kernel.api.service.IGService;
import org.chuniter.core.kernel.api.uflow.UFlowService;
import org.chuniter.core.kernel.api.unit.IDoSQL;
import org.chuniter.core.kernel.extimpl.service.GService;
import org.chuniter.core.kernel.impl.authorization.AbstractBaseAuthorization;
import org.chuniter.core.kernel.impl.orm.EntityMapping;
import org.chuniter.core.kernel.impl.orm.ExtendedField;
import org.chuniter.core.kernel.impl.orm.ORMControler;
import org.chuniter.core.kernel.impl.unit.UMap;
import org.chuniter.core.kernel.kernelunit.*;
import org.chuniter.core.kernel.model.BaseEntity;
import org.chuniter.core.kernel.model.BaseUserEntity;
import org.chuniter.core.kernel.model.Extendable;
import org.chuniter.core.kernel.model.NOCreator;

import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.lang.reflect.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
 


/**
 * 通用接口服务实现抽象适配器类。
 * @author Administrator
 *
 * @param <T>
 */
public abstract class GeneralServiceAdapter<T extends Extendable> implements IGeneralService<T>,IMessageListenerContianer {
	
	protected Log log = LogFactory.getLog(super.getClass());
	protected final Integer SAVE = 1;
	protected final Integer DELETE = 2;
	protected final Integer UPDATE = 3;
	protected final Integer CREATE = 4; 
	protected final Integer SAVEORUPDATE = 5;

	//用于组织范围控制
	/*GeneralEmpInfo*/
	private static Class<?> empClass = null;
	private static Class<?> orgClass = null;
	/*EmployeeEntity*/
	private static Class<?> empeClass = null;


	/*模板方法模式方法*/
	protected abstract GenericDao<T> getDaoInstance();
	protected abstract Param fetchParam(T t);
	private GenericDao<T> d;
	private ReflectUtil refu = ReflectUtil.getInstance();
	
	private final ProxyFactory proxfactory = new ProxyFactory(); 
	private GenericDao<T> getProxDao(){
		d = getDaoInstance();
		if(null == d)
			return null;
		JdkHandler handler = new JdkHandler(d);   
        GenericDao<T> pdao = proxfactory.newProxyInstance(GenericDao.class,handler);  
		return pdao;
	} 
	public GenericDao<T> getDao(){
		return getDaoInstance();
	};
	class ProxyFactory{
		public <I> I newProxyInstance(Class<I> inferfaceClass, InvocationHandler handler) {  
			return inferfaceClass.cast(Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class[] {inferfaceClass},handler));  
		}  
	}
	private class JdkHandler implements InvocationHandler {
		private Object delegate = null;
		JdkHandler(Object delegate) {
			this.delegate = delegate;
		}
		public Object invoke(Object object, Method method, Object[] objects) throws Throwable {
			String mname = method.getName().toLowerCase();
			//处理组织范围限制.youg 2018-07-05 17:44
			proOrgLimit(objects,mname);
			//end
			//如果是delete,save,create
			if(mname.startsWith("save")||mname.startsWith("create")&&objects.length>0||mname.startsWith("update")) {
				checkCanPro(objects);
			}
			Object o = method.invoke(delegate, objects);
			//System.out.println("调用后:");
			return o;
		}

		//与业务在关的代码
		private final void proOrgLimit(Object[] objects,String mname) throws ClassNotFoundException {
			Class<?> ent = null;
			Class<?> clz = null;
			try{
				if(null == empClass) {
					empClass = ent.getClassLoader().loadClass("org.unitor.bbp.hrbaseapi.model.GeneralEmpInfo");
					orgClass = ent.getClassLoader().loadClass("org.unitor.bbp.hrbaseapi.model.GeneralOrgInfo");
					empeClass = ent.getClassLoader().loadClass("org.unitor.bbp.hrbaseapi.model.EmployeeEntity");
				}
			}catch(Exception ee){
				//log.error("无法加载人员与组织相关类，可能此运行环境不需要此功能."+ee.getMessage());
				return;
			}

			if(null != objects&&objects.length>0&&objects[0] instanceof Class) {ent = (Class<?>) objects[0];}
			if(null == ent&&delegate instanceof ISimpORM) { ent = ((ISimpORM<?>) delegate).getClazz();}
			String alias = null;
			String tn = null != ent?ent.getSimpleName():null;
			if (log.isDebugEnabled())
				log.debug(Thread.currentThread().getName()+"\n 实体类:"+ent+",方法:"+mname);
			if(mname.startsWith("hisql")) {
				String sql = objects[0].toString();
				String[] e_a = EntityMapping.fetchTableNameAndAlias(sql);
				tn = e_a[0];
				alias = e_a[1];
				if(null == e_a[0]) {
					log.error("获得SQL表名为空:"+e_a[0]+" \n "+sql);
					return;
				}
				if(e_a[0].equalsIgnoreCase("Unit"))
					return;
				clz = ORMControler.getEntityByTableName(e_a[0]);
			}
			if(tn.equals("UCacheEntity")||tn.equals("Unit")||tn.equals("OrgEntity"))
				return;
			if(null == clz&&objects!=null&&objects.length>0&&(objects[0] instanceof Class)&&BaseEntity.class.isAssignableFrom(((Class<?>)(objects[0]))))
				clz = (Class<?>)(objects[0]);

			//判断此实体类是否是人员或组织，用于过处理业务数据范围,这种是业务数据处理，放在框架中可能不是最好的选择，得想办法移出去.youg 2018-07-05 :27
			if((null != ent&&(empeClass.isAssignableFrom(ent)||orgClass.isAssignableFrom(ent)||empClass.isAssignableFrom(ent)||ent.getSimpleName().equals("EmployeeEntity")))||
					(null != clz&&(empeClass.isAssignableFrom(clz)||orgClass.isAssignableFrom(clz)||empClass.isAssignableFrom(clz)||clz.getSimpleName().equals("EmployeeEntity")))
			){
				// 获得当前人员授权的组织
				String orgids = orgLimit(getDaoInstance());
				if(!StringUtil.isValid(orgids))
					return;
				//有出现SERVICE是有关人员实体的SERVICE，但HISQL中却不是与之有关的，需要以HISQL中的实体为主
				if(null == alias)
					alias = ent.getSimpleName()+"_";
				if (log.isDebugEnabled())
					log.debug(Thread.currentThread().getName()+"\n 找到:"+ent+"别名:"+alias);
				//获得参数对象
				Param p = null;
				for(Object o:objects) {if(o instanceof Param){p = (Param) o;break;}}
				if(null == p)
					return;
				if(log.isDebugEnabled())
					log.debug(Thread.currentThread().getName()+"-----\n alias:"+alias+"  \n table:"+tn+"  \n ent:"+ent.getSimpleName()+" \n clz:"+(clz==null?"null":clz.getSimpleName())+"\n");
				if(orgids.endsWith(","))
					orgids = orgids.substring(0,orgids.length()-1);
				if(tn.equals("EmployeeEntity")||(null != clz&&empeClass.isAssignableFrom(clz))) {
					orgids = " partid in(select o.id from orgEntity o where (orgPath like '%"+orgids.replaceAll(",", "%' or orgPath like '%")+"%') and partId=o.id) ";
					p.addParam("orgidsql","sql-server",orgids);//"and '"+orgids+"' like '%'+partId+'%'");
					return;
				}
				if(p.hasJoinCondition("org.unitor.bbp.hrbaseapi.model.EmployeeEntity")) {
					orgids = " partid in(select o.id from orgEntity o where (orgPath like '%"+orgids.replaceAll(",", "%' or orgPath like '%")+"%') and EmployeeEntity_.partId=o.id) ";
					p.addParam("orgidsql","sql-server",orgids);//"('"+orgids+"' like '%'+EmployeeEntity_.partId+'%')");
					return;
				}
				if(null != clz&&empClass.isAssignableFrom(clz)) {
					orgids = " partid in(select o.id from orgEntity o where (orgPath like '%"+orgids.replaceAll(",", "%' or orgPath like '%")+"%') and emp.partId=o.id) ";
					p.addParam("orgidsql","sql-server","and exists(select 1 from EmployeeEntity emp where "+alias+".employeeid=emp.id and "+orgids+")");
					return;
				}
				if(orgClass.isAssignableFrom(ent)||(null != clz&&orgClass.isAssignableFrom(clz))) {
					orgids = "and orgId in(select o.id from OrgEntity o where "+alias+".orgId=o.id and (orgPath like '%"+orgids.replaceAll(",", "%' or orgPath like '%")+"%') )";
					p.addParam("orgidsql","sql-server",orgids);//"and '"+orgids+"' like '%'+orgId+'%'");
					return;
				}
			}
		}
	}
	public static String orgLimit(GenericDao<?> dao) {
		return AbstractBaseAuthorization.orgLimit(dao);
	}
    private void checkCanPro(Object[] objects) throws Exception{
		//3.2暂时忽略不处理
		if(true)
			return;
    	if(null == objects||objects.length<=0)
    		return;
    	//结算检查，检查类，不同的业务期时间不一样
		Object t = objects[0];
		if(t instanceof BaseEntity) {
			Object empid = null;
	    	Object yymm = null;
			String cname = t.getClass().getSimpleName();
    		//检查
    		if(cname.equals("ATHolidayMaintenance")||
    				cname.equals("ATOvertimeApply")||
    				cname.equals("ATCheckCard")||
    				cname.equals("SAChange")||
    				cname.equals("SAInput")||
    				cname.equals("AffairsChangeEntity")) {
    			//获得开始时间,作为检查的时间条件，这里固定使用STARTITME，没有的实体请加之
    			yymm = refu.gettFieldReadMethodValue(t, "startTime");
    			if(null != yymm)
    				yymm = DateUtil.dateToString(((Date)yymm),"yyyy-MM");
        		//获得人员id
    			empid = refu.gettFieldReadMethodValue(t, "employeeid");
    		}
    		if(null != empid&&null != yymm) {
    			try {
					//检查结账
					Param p = Param.getInstance();
					p.addParam("YYMM", yymm);
					p.addParam("employeeid", empid);
					List<Map<String, Object>> ml = d.hisql("select count(1) c from SAMonthCount where estate in (9,10,11) ", p);
					if (null != ml && !ml.isEmpty() && (Integer.valueOf(ml.get(0).get("c").toString()) > 0)) {
						throw new LockedEditException("此人在该时间已结账，无法操作");
					}
				}catch(Exception ee){
    				log.error("检查操作数据关联人员是否已经账发生异常:"+ee.getMessage());
				}
    		}
		}
    }
	public void delete(T entity) throws Exception {
		if(!caneditable(entity))
			return;
		finalDoWorkFlow(entity,DELETE);
		getProxDao().delete(entity);
	}
 
	public void delete(Serializable entity) throws Exception{
		if(!caneditable(entity))
			return;
		finalDoWorkFlow(entity,DELETE);
		getProxDao().delete(entity); 
	} 
	public Serializable save(T entity) throws Exception {
		boolean iscreate = false;
		if(entity instanceof BaseEntity){
			if(!StringUtil.isValid(((BaseEntity) entity).getId()))
					iscreate=true;	
		}
		generalSetBaseValue(entity,iscreate);
		proDataIsolation(entity);
		Serializable saveResult = getProxDao().save(entity);
		finalDoWorkFlow(entity,SAVE);
		return saveResult;
	}
 
	public Serializable create(T entity) throws Exception {
		generalSetBaseValue(entity,true);
		proDataIsolation(entity);
		Serializable saveResult = getProxDao().save(entity); 
		sendMsg(saveResult);
		finalDoWorkFlow(entity,CREATE);
		return saveResult;
	}
	public Serializable create(Class<? extends BaseEntity> c,JSONObject json) throws Exception {
		if(null == c||json == null)
			return null;
		if(!BaseEntity.class.isAssignableFrom(c))
			return null;
		BaseEntity entity = JSONObject.toJavaObject(json, c); 
		generalSetBaseEntityValue(entity,true);
		proDataIsolation(entity);
		Serializable saveResult = getProxDao().createEntity(entity);
		sendMsg(saveResult);
		finalDoWorkFlow(entity,CREATE);
		return saveResult;
	}
	public Serializable update(Class<? extends BaseEntity> c, JSONObject json) throws Exception {
		if (null == c || json == null)
			return null;
		if (!BaseEntity.class.isAssignableFrom(c))
			return null;
		BaseEntity entity = JSONObject.toJavaObject(json, c);

		proDataIsolation(entity);
		Serializable saveResult = null;
		if (!StringUtil.isValid(entity.getId())) {
			generalSetBaseEntityValue(entity, true);
			saveResult = getProxDao().createEntity(entity);
		}else {
			generalSetBaseEntityValue(entity, false);
			saveResult = getProxDao().updateEntity(entity);
		}
		sendMsg(saveResult);
		finalDoWorkFlow(entity, CREATE);
		return saveResult;
	}
	private boolean isSubBaseEntity(Class<?> c) { 
		if(!c.isAssignableFrom(BaseEntity.class)) {
			Class<?> s = c;
			s = s.getSuperclass();
			while (s != null) {
				if(!s.isAssignableFrom(BaseEntity.class)) 
					return true; 
			}
		}
		return false;
	}
	public Serializable create(String classFullName, JSONObject json) throws Exception {
		if (!StringUtil.isValid(classFullName))
			return null;
		Class<? extends BaseEntity> c = (Class<? extends BaseEntity>) Class.forName(classFullName);
		if (null == c || json == null)
			return null;

		if (!isSubBaseEntity(c))
			return null;
		return this.create(c, json);
	}
	public Serializable update(String classFullName, JSONObject json) throws Exception {
		if (!StringUtil.isValid(classFullName))
			return null;
		Class<? extends BaseEntity> c = (Class<? extends BaseEntity>) Class.forName(classFullName);
		if (null == c || json == null)
			return null;
		return this.update(c, json);
	}
	public void saveOrUpdate(T entity) throws Exception {
		if(!caneditable(entity))
			return;
		generalSetBaseValue(entity,false);
		getProxDao().saveOrUpdate(entity);
		this.finalDoWorkFlow(entity,SAVEORUPDATE);
	}
 
	public Serializable update(T entity) throws Exception {
		if(!caneditable(entity))
			return entity;
		generalSetBaseValue(entity,false);
		Serializable s = getProxDao().update(entity);
		this.finalDoWorkFlow(s,UPDATE);
		return s;
	}	
	protected boolean caneditable(Object entity) throws Exception{
		BaseEntity b = null;
		if(entity instanceof String) {
			b = (BaseEntity) this.fetchClazz().newInstance();
			b.setId(entity.toString());
			//b = this.findEntityById(this.fetchClazz(), entity.toString());
		}else if(entity instanceof BaseEntity){ 
			b = (BaseEntity)entity;
		}
		if(null == b||!StringUtil.isValid(b.getId()))
			return true;
		//这里考虑从UI会人为的设值其ESTATE，所以就算有EASTATE，还是从DB查出进行判断。
		Param p = Param.getInstance();
		p.addParam("id", b.getId()); 
		try{
			List<Map<String,Object>> ms = this.hisql("select estate,ecode from "+b.getClass().getSimpleName()+" where id='"+b.getId()+"'", p,false);
			if(null != ms&&!ms.isEmpty()){
				UMap m = (UMap)ms.get(0); 
				String inbuild = m.get("ecode")==null?null: m.get("ecode").toString();
				if(null !=inbuild&&BaseEntity.INBUILT.equals(inbuild))
					return false;
				if(m.containsKey("estate")&&null != m.get("estate")&&b.getEstate()!= m.getInteger("estate")){
					BaseEntity e = b.getClass().newInstance();
					e.setEstate(m.getInteger("estate"));
					return e.canedit();//BaseEntity.canedit(m.getInteger("estate"));
				}else
					return true;
			}
		}catch(Exception e){e.printStackTrace();throw new CannotEditException("cannotedit");} 
		
		return false;
	}
	protected boolean caneditable(Param p) throws Exception{
		if(p == null)
			return true; 
		try{
			List<Map<String,Object>> ms = this.getProxDao().hisql("select estate,ecode from "+this.fetchClazz().getSimpleName()+" where  1=1 ", p);
			if(null != ms&&!ms.isEmpty()){
				Map<String,Object> m = ms.get(0); 
				String inbuild = m.get("ecode")==null?null: m.get("ecode").toString();
				if(null !=inbuild&&BaseEntity.INBUILT.equals(inbuild))
					return false;
				if(m.containsKey("estate")&&null != m.get("estate")){
					BaseEntity b = (BaseEntity) this.fetchClazz().newInstance();
					return b.canedit();//Integer.parseInt(m.get("estate").toString())
				}else
					return true;
			}
		}catch(Exception e){throw new CannotEditException("cannotedit");}  
		return false;
	}
	/*@RemotingInclude
	public  T find(Serializable paramSerializable) throws Exception{
		return getProxDao().find(paramSerializable);
	}*/
 
	public  List<T> find() throws Exception{
		Param p = Param.getInstance();
		findParamtoSession(null,p);
		proDataIsolation(null,p);
		return getProxDao().find(p);
	}
 
	public  List<T> find(String paramString) throws Exception {
		return getProxDao().find(paramString);
	} 
	public T findById(Serializable entityId) throws Exception{
		return getProxDao().find(entityId);
	}
	public  PageIterator<T> find(int paramInt1, int paramInt2) throws Exception {
		Param param = Param.getInstance();
		Class<?> c = null;
		findParamtoSession(c,param);
		proDataIsolation(c,param);
		return getProxDao().find(paramInt1,paramInt2);
	} 
	public  List<T> find(Param param) throws Exception {
		Class<?> c = null;
		findParamtoSession(c,param);
		proDataIsolation(c,param);
		return getProxDao().find(param);
	} 
	public  PageIterator<T> find(Param param, int paramInt1, int paramInt2)  throws Exception{
		Class<?> c = null;
		findParamtoSession(c,param);
		proDataIsolation(c,param);
		return getProxDao().find(param,paramInt1,paramInt2);
	} 
	public  Map<String, List<T>> findProperty(Param param,String[] paramArrayOfString) throws Exception{
		return getProxDao().findProperty(param, paramArrayOfString);
	} 
	public  List<T> findByEntity(T t) throws Exception{
		Param p = this.fetchParam(t);
		return this.find(p);
	} 
	public List<T> find(List<ParamCarrier> paramCarriers) throws Exception{		
		Param p = fetchParam(paramCarriers);
		return this.find(p);
	} 
	public PageIterator<T> findPage(List<ParamCarrier> paramCarriers, int paramInt1, int paramInt2)  throws Exception{
		Param p = fetchParam(paramCarriers);
		return getProxDao().find(p,paramInt1,paramInt2);
	} 
	public PageIterator<T> findPageAsJoin(Param p, int paramInt1, int paramInt2)  throws Exception{ 
		findParamtoSession(null,p);
		proDataIsolation(null,p);
		return this.getProxDao().findPageAsJoin(p, paramInt1, paramInt2); 
	}
	public void deleteByLis(List<T> dels) throws Exception {
		if(null == dels)
			return;
		for(T cs:dels){  
			this.delete(cs);  
		}
	} 
	public void deleteByIdLis(List<Serializable> ids) throws Exception {
		if(null == ids)
			return;
		for(Serializable id:ids){  
			this.delete(id);  
		}
	}
	@Override
	public Serializable createEntity(Object entity) throws Exception{
		if(entity instanceof BaseEntity)
			generalSetBaseEntityValue((BaseEntity)entity,true);
		proDataIsolation(entity); 
		Serializable s = getProxDao().createEntity(entity);
		this.finalDoWorkFlow(entity,CREATE);
		return s;
	}
	@Override
	public void deleteEntity(Class<?> claz,Serializable delId) throws Exception{
		BaseEntity o = (BaseEntity) claz.newInstance();
		o.setId(delId.toString());
		if(this.caneditable(o)) {
			this.finalDoWorkFlow(o,DELETE);
			getProxDao().deleteEntity(claz,delId);
		}else {
			throw new CannotEditException("cannotedit");
		}
	}
	@Override
	public Serializable updateEntity(Object entity) throws Exception {
		if(caneditable(entity)){ 
			if(entity instanceof BaseEntity) {
				//UEntity ue = AnnotationParser.getUEntity(entity.getClass());
				//String tableName = ue.name();
				//if(!StringUtil.isValid(tableName))
				String tableName = entity.getClass().getSimpleName();
				Param p = Param.getInstance();
				p.addParam("id",((BaseEntity) entity).getId());
				p.addParam(BaseEntity.DATAOWNER,((BaseEntity) entity).getDataOwner());
				UMap um = this.hisqlOne("select meindex from "+tableName+" where id='"+((BaseEntity) entity).getId()+"'", p);
				if(null != um&&!um.isEmpty()) {
					Integer mindex = um.getInteger("meindex"); 
					if(null == mindex) {
						um = this.hisqlOne("select MAX(meindex) m from "+tableName+" where id='"+((BaseEntity) entity).getId()+"'", p);
						mindex = um.getInteger("m"); 
						if(mindex == null)
							mindex = 1;
						this.hisqlOne("update "+tableName+" set meindex="+mindex+" where id='"+((BaseEntity) entity).getId()+"'", p);
					}
				}
			}
			generalSetBaseEntityValue(entity,false);
			Serializable s =  this.getProxDao().updateEntity(entity);
			this.finalDoWorkFlow(entity,UPDATE);
			return s;
		}else{
			throw new CannotEditException("cannotedit");
		}
	}
	protected final void generalSetBaseEntityValue(Object entity,boolean isCreate){
		if(entity instanceof Map) { 
			Map<String,Object> um = (Map<String,Object>)entity;
			JSONObject ju = fetchJSONUser();
			if(null != ju) 
				um.put("lastModifyMan",ju.getString("id")); 
			um.put("lastModifyDate",new Date());
		}else if(entity instanceof BaseEntity){ 
			BaseEntity te = (BaseEntity)entity; 
			if(null == te.getCreateDate()&&isCreate)
				te.setCreateDate(new Date());
			te.setLastModifyDate(new Date());
			try{
				HttpServletRequest o = AbstractBaseAuthorization.get();
				if(null != o){
					if(null != o.getSession(false)){  
						BaseUserEntity bu = AbstractBaseAuthorization.sgetGeneralUserEntity(null);
						if(null != bu){
							te.setCreaterId(bu.getId());	
							te.setCreateMan(bu.getName());	
							return;
						}
						Object mapObj = AbstractBaseAuthorization.get().getSession().getAttribute(IAuthorization.SESSION_USERINFOMAP);
						if(null != mapObj){
							Map buser = (Map)mapObj; 
							String userid = "";
							if(buser.containsKey("userId"))
								userid = buser.get("userId").toString();
							else if(buser.containsKey("id"))
								userid = buser.get("id").toString();
							if(!StringUtil.isValid(te.getCreaterId())){//&&isCreate){
								te.setCreaterId(userid);	
								te.setCreateMan(buser.get("userName").toString());	
							}
							te.setLastModifyMan(buser.get("userName").toString());
						}else{
							Object userName= AbstractBaseAuthorization.get().getSession().getAttribute(IAuthorization.SESSION_USER);
							if(null != userName){
								if(!StringUtil.isValid(te.getCreateMan())&&isCreate)
									te.setCreateMan(userName.toString());						
								te.setLastModifyMan(userName.toString());
							}
						}
					}
				}
			}catch(Exception e){}
		}
	}	
	public String fetchUserId(){ 
		if(null != AbstractBaseAuthorization.get()&&null != AbstractBaseAuthorization.get().getSession(false)){ 
			Object mapObj = AbstractBaseAuthorization.get().getSession().getAttribute(IAuthorization.SESSION_USERINFOMAP);
			if(null == mapObj)
				return null;
			Map buser = (Map)mapObj;  
			if(buser.containsKey("userId"))
				return buser.get("userId").toString();
			else if(buser.containsKey("id"))
				return  buser.get("id").toString(); 
		} 
		return null;
	}
	public static JSONObject fetchJSONUser(){ 
		if(null != AbstractBaseAuthorization.get()&&null != AbstractBaseAuthorization.get().getSession(false)){ 
			Object mapObj = AbstractBaseAuthorization.get().getSession().getAttribute(IAuthorization.LOGIN_USER);
			if(null == mapObj)
				return null;
			if(mapObj instanceof net.sf.json.JSONObject){
				JSONObject j = JSON.parseObject(((net.sf.json.JSONObject)mapObj).toString()); 
				AbstractBaseAuthorization.get().getSession().setAttribute(IAuthorization.LOGIN_USER,j);
				return j; 
			}
			if(mapObj instanceof String){
				JSONObject j = JSON.parseObject(mapObj.toString()); 
				AbstractBaseAuthorization.get().getSession().setAttribute(IAuthorization.LOGIN_USER,j);
				return j; 
			}
			JSONObject jsonuser = (JSONObject)mapObj;  
			return jsonuser;
		} 
		return null;
	}
	private void generalSetBaseValue(T entity,boolean isCreate){
		generalSetBaseEntityValue(entity,isCreate);
	}
	/**-------空实现方法，用于子类去重写-------**/
	//public void onMessage(Message msg){}
	public void sendMsg(Serializable sendObj){}
	public void regeditToMessageListenerContainer(){}
	
	protected Param fetchParam(List<ParamCarrier> paramCarriers){
		if(null == paramCarriers||paramCarriers.size()<=0)
			return null;
		Param p = Param.getInstance();
		for(ParamCarrier pc:paramCarriers){
			pc.fetchParam(p);
		}
		return p;
	}
	/**
	 * 执行sql方法，传入一下执行接口，该接口有一个以connection为参数的方法
	 * 具体的处理在由具体实现
	 * @author youg
	 * @time 2012-09-13
	 * @param idoSql
	 * @return
	 */
	public <S> S executerSQL(IDoSQL<S> idoSql) throws Exception {
		return this.getProxDao().executerSQL(idoSql);
	}
	@Override
	public <S> S executerSQL(IDoSQL<S> idoSql,Class<?> claz) throws Exception {
		return this.getProxDao().executerSQL(idoSql,claz);
	}
	/**
	 * 排除指定的属性进行查询
	 * @author youg
	 * @time 2013-09-28
	 * @vertion v3
	 * @param clazz
	 * @param param
	 * @param startIndex
	 * @param pageSize
	 * @param excludeFields
	 * @return
	 * @throws Exception
	 */
	public PageIterator<T> find(Param param,int startIndex,int pageSize,String[] excludeFields) throws Exception{
		if (startIndex < 0)
			startIndex = 0;
		findParamtoSession(null,param);
		proDataIsolation(null,param);
		return this.getProxDao().find(param, startIndex, pageSize, excludeFields);
	}	
	public static void main(String[] args) throws IllegalArgumentException, SecurityException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, InstantiationException{

	}
	public <S> List<S> findEntitys(Class<S> clazz,Param param) throws Exception{
		return findEntitys(clazz,param,true);
	}
	@Override
	public <S> List<S> findEntitys(Class<S> clazz,Param p,boolean needDataOwner) throws Exception { 
		if(needDataOwner)
			p.setDataOwnerName(clazz.getSimpleName()+"_.dataOwner"); 
		proDataIsolation(clazz,p);
		return this.getProxDao().findEntitys(clazz, p);
	}
	public PageIterator<?> findEntitys(Class<?> claz,Param p,int startIndex,int pageSize) throws Exception{
		findParamtoSession(claz,p);
		p.setDataOwnerName(claz.getSimpleName()+"_.dataOwner");
		proDataIsolation(claz,p);
		return this.getProxDao().findEntitys(claz, p, startIndex, pageSize);
	}
	 
	@Override
	public <S> S findEntity(Class<S> claz, Param p) throws Exception {  
		return findEntity(claz, p,true); 
	}
	@Override
	public <S> S findEntity(Class<S> claz, Param p,boolean needDataOwner) throws Exception { 
		if(needDataOwner) {
			p.setDataOwnerName(claz.getSimpleName()+"_.dataOwner");
			proDataIsolation(claz,p);
		}
		return this.getProxDao().findEntity(claz, p); 
	}
	@Override
	public <S> S findEntityById(Class<S> claz, String id) throws Exception { 
		Param p = Param.getInstance();
		p.addParam("id", id);
		//FIXME 这个地方因考虑以前的代码，所以才设置FALSE，但会不会有安全问题，比如知道不属自已企业的数据ID
		return findEntity(claz, p,false); 
	}
	@Override
	public void delete(Param p) throws Exception {
		proDataIsolation(null,p);
		if(this.caneditable(p)) {
			this.getProxDao().delete(p); 
		}
	}
	@Override
	public void deleteEntity(Class<?> claz, Param p) throws Exception {
		//FIXME 这里获得的表名遇到与实体用不一样的表时，就会有问题,日后要完善.youg 2017-10-14 11:50
		//List<Map<String,Object>> mls = this.hisql("select estate from "+claz.getSimpleName(), p);
		//用对象查吧，就没上述问题，但性能没有上述的好
		Class c = claz;
		List<T> l = this.findEntitys(c, p);
		if(null == l||l.isEmpty())
			return;
		for(T t:l){
			if((t instanceof BaseEntity) && (this.caneditable((BaseEntity)t))){
				proDataIsolation(claz,p);
				this.finalDoWorkFlow(t, DELETE);
				this.getProxDao().deleteEntity(claz, p);    
			}
		}
	}
	
	/**
	 * 更新属性方法
	 * @author yonglu
	 * @time 2015-08-09 22:49
	 * @param properMap 要更新的属性名与值
	 * @param param 更新条件
	 * @throws Exception
	 */
	public void updatePropertie(Map<String,Object> properMap,Param param) throws Exception{
		//检查是否可以修改
		checkcanedit(this.fetchClazz(),param);
		proDataIsolation(null,param,"update");
		generalSetBaseEntityValue(properMap,false);
		this.getProxDao().updatePropertie(properMap, param);
	}
	private <S> boolean checkcanedit(Class<S> cc,Param p) throws Exception {
		//检查是否可以修改
		List<S> ts = this.getProxDao().findEntitys(cc,p);
		if(null == ts||ts.isEmpty())
			return true;
		for(S t:ts) {
			if(!((BaseEntity)t).canedit()) {
				throw new CannotEditException("cannotedit");
			}
		}
		return true;
	}
	/**
	 * 更新属性方法
	 * @author yonglu
	 * @time 2015-08-09 22:49
	 * @param clax 要更新的类
	 * @param properMap 要更新的属性名与值
	 * @param param 更新条件
	 * @throws Exception
	 */
	public <S> void updatePropertie(Class<S> clax,Map<String,Object> properMap,Param param) throws Exception{
		updatePropertie(clax,properMap,param,true);
	}
	/**
	 * 更新属性方法
	 * @author yonglu
	 * @time 2018-04-26 17:18
	 * @param clax 要更新的类
	 * @param properMap 要更新的属性名与值
	 * @param param 更新条件
	 * @param checkEstate 是否要检查可编辑性
	 * @throws Exception
	 */
	public <S> void updatePropertie(Class<S> clax,Map<String,Object> properMap,Param param,boolean checkEstate) throws Exception{
		if(checkEstate) {
			//检查是否可以修改
			checkcanedit(clax,param);
		}
		proDataIsolation(clax,param,"update");
		generalSetBaseEntityValue(properMap,false);
		this.getProxDao().updatePropertie(clax,properMap, param);
	}
	/**
	 * 更新属性方法
	 * @author yonglu
	 * @time 2017-09-03 11:49
	 * @param clax 要更新的类
	 * @param properMap 要更新的属性名与值
	 * @param id 更新id
	 * @throws Exception
	 */ 
	public  void updatePropertie(Class<?> s,Map<String,Object> properMap,String id) throws Exception {
		//检查是否可以修改
		Param p = Param.getInstance();
		p.addParam("id", id);
		checkcanedit(s,p);
		generalSetBaseEntityValue(properMap,false);
		this.getProxDao().updatePropertie(s, properMap, id);
	}
	/**
	 * 更新属性方法
	 * @author yonglu
	 * @time 2017-09-03 11:49 
	 * @param properMap 要更新的属性名与值
	 * @param id 更新id
	 * @throws Exception
	 */ 
	public <S> void updatePropertie(Map<String,Object> properMap,String id) throws Exception {
		//检查是否可以修改
		Param p = Param.getInstance();
		p.addParam("id", id);
		checkcanedit(this.fetchClazz(),p);
		generalSetBaseEntityValue(properMap,false);
		this.getProxDao().updatePropertie(properMap, id);
	} 
	public <S> S updateExcludeField(final S t,String[] excludeFileNames) throws Exception{ 
		String[] exp = excludeFileNames;
		if(null != excludeFileNames){
			//excludeFileNames
			exp = new String[excludeFileNames.length+EntityMapping.noupfields.length];
			int i=0;
			for(;i<excludeFileNames.length;i++){
				exp[i]=excludeFileNames[i];
			}
			int j=0;
			for(;j<EntityMapping.noupfields.length;j++,i++){
				exp[i]=EntityMapping.noupfields[j];
			}
		}else
			exp = EntityMapping.noupfields;
		generalSetBaseEntityValue(t,false);
		return this.getProxDao().updateExcludeField(t, exp);
	}
	public void update(Param p,Map<String,Object> upmap,Class<?>...clz) throws Exception{
		proDataIsolation(((clz==null||clz.length<=0)?null:clz[0]),p,"update");
		generalSetBaseEntityValue(upmap,false);
		this.getProxDao().update(p,upmap,clz);
	}
	public List<T> findWithProperty(Param param, String[] paramArrayOfString) throws Exception{
		proDataIsolation(null,param);
		return this.getProxDao().findWithProperty(param, paramArrayOfString);
	}
	/**
	 * 通用sql查询
	 */
	@Override
	public List<Map<String, Object>> hisql(final String sql,final Param p) throws Exception { 
		proDataIsolation(null,p,sql); 
		return this.getProxDao().hisql(sql,p);
	}
	@Override
	public UMap hisqlOne(final String sql,final Param p,boolean needFilterData) throws Exception {
		List<Map<String,Object>> mls = this.hisql(sql,p,needFilterData);
		if(null == mls||mls.isEmpty())
			return null;
		if(mls.size()>1)
			throw new Exception("查询结果多于一条");
		return (UMap)mls.get(0);
	}
	@Override
	public UMap hisqlOne(final String sql,final Param p) throws Exception { 
		return hisqlOne(sql,p,true);
	} 
	public List<Map<String, Object>> hisql(final String sql,final Param pp,boolean needFilterData) throws Exception {
		Param p = pp.cloneme();
		p.setNeedAddDataOwner(needFilterData);
		if(needFilterData)
			proDataIsolation(null,p,sql); 
		return this.getProxDao().hisql(sql,p);
	}
	public <S> PageIterator<S> hisql(final String sql, final Param p, final int startIndex, final int pageSize)
			throws Exception {
		return this.hisql(sql, p.cloneme(), startIndex, pageSize, true);
	}

	public <S> PageIterator<S> hisql(final String sql, final Param p, final int startIndex, final int pageSize,
									 boolean needFilterDataOwner) throws Exception {
		if (needFilterDataOwner) {
			proDataIsolation(null, p, sql);
		}
		return this.getProxDao().hisql(sql, p, startIndex, pageSize, needFilterDataOwner);
	}
	/**
	 * 获得一个自定义前缀的序号
	 * @param prefix
	 * @return
	 * @throws Exception
	 */
	@Override
	public String fetchNo(String prefix,IGeneralService<?> service) throws Exception{
		Class<T> clz = fetchClazz();
		return fetchNo(prefix,clz);
	}
	@Override
	public String fetchNo(String prefix,Class<?> clz) throws Exception{
		/*try{ 
			//默认采用count数据总数来获得编号.2017-11-30 00:11 youg.
			Param p = Param.getInstance();
			List<Map<String,Object>> ml = hisql("select count(1) m from "+clz.getSimpleName()+" where 1=1 " , p);  
			if(null != ml&&!ml.isEmpty()) {
				Object m = ml.get(0).get("m");
				if(m == null)
					return "1";
				m = cleanNoDigital(m.toString());
				if(!StringUtil.isValid(m.toString()))
					return "1";
				return String.valueOf(Integer.valueOf(m.toString())+1);
			}
		}catch(Exception e) {
			e.printStackTrace();
		}
		return String.valueOf(new Date().getTime());*/
		return fetchNo(prefix,clz,"meIndex");
	}
	@Override
	public String fetchNo(String prefix,Class<?> clz,String maxFieldName) throws Exception{
		return fetchNo(prefix,clz,maxFieldName,6);
	}
	@Override
	public String fetchNo(String prefix,Class<?> clz,String maxFieldName,Integer noLength) throws Exception{ 
		return fetchNo(prefix,clz,maxFieldName,noLength,null);
	}
	@Override
	public String fetchNo(String prefix,Class<?> clz,String maxFieldName,Integer noLength,Param pp) throws Exception{
		try{  
			Param p = (pp==null?null:pp.cloneme());
			if(p==null)
				p = Param.getInstance(); 
			//FIXME 这个地方多数据库注意， LEN目前只适用于MSSQL。2018-05-03 18：13 youg.
			if(StringUtil.isValid(prefix))
				p.addParam("lensql","sql-server","LEN("+maxFieldName+")="+(prefix.length()+noLength)); 
			//end
			List<Map<String,Object>> ml = hisql("select max("+maxFieldName+") m from "+clz.getSimpleName()+" where 1=1 " , p);
			if(null == ml||ml.isEmpty()||ml.get(0).isEmpty()||null == ml.get(0).get("m")) {
				p = (pp==null?null:pp.cloneme());
				if(p==null)
					p = Param.getInstance();
				 ml = hisql("select max("+maxFieldName+") m from "+clz.getSimpleName()+" where 1=1 " , p);
			}
			if(null == ml||ml.isEmpty()||ml.get(0).isEmpty()||null == ml.get(0).get("m")) {
				p = (pp==null?null:pp.cloneme());
				if(p==null)
					p = Param.getInstance();
				 ml = hisql("select max(meIndex) m from "+clz.getSimpleName()+" where 1=1 " , p);
			}
			if(null != ml&&!ml.isEmpty()) {
				Object m = ml.get(0).get("m");
				StringBuilder s = new StringBuilder(prefix);
				for(int i=0;i<noLength;i++)
					s.append("0");
				if(m == null) {
					return s.substring(0,s.length()-1)+"1";
				}
				m = cleanNoDigital(m.toString());
				if(!StringUtil.isValid(m.toString())) {
					String ms = ml.get(0).get("m").toString()+"1";
					return s.substring(0,s.length()-ms.length())+ms;
				}
				//FIXME 如果存在字符，则获得的最大值有可能就会不对，所以为了准确性，只能牺牲性能了。暂没处理
				//END
				String no = String.valueOf(Integer.valueOf(m.toString())+1);
				if(no.length()>=s.length())
					return no;
				return s.substring(0,s.length()-no.length())+no;
			}
		}catch(Exception e) {
			e.printStackTrace();
		}
		return String.valueOf(new Date().getTime());
	}
	private String cleanNoDigital(String s) {
		if(!StringUtil.isValid(s))
			return s;
		try {if(Integer.valueOf(s) != null){return s;}}catch(Exception e) { /*ignore exception*/}
		StringBuilder sb = new StringBuilder();
		//正则表达式清除非数字
		/*Pattern pat = Pattern.compile("\\d");
		for(int i=s.length();i>0;i--) {
			Matcher m = pat.matcher(s.substring(i-1,i));
			if(m.find())
				sb.append(s.substring(i-1,i)); 
		}
		System.out.println(sb); 
		sb.delete(0, sb.length());
		*/
		//利用ASCII码清除非数字
		for(char c:s.toCharArray()) {
			int i = Integer.valueOf(c-'0'); 
			if(i>10) {
				continue;
			}
			sb.append(i);
		}
		return sb.toString();
	}
	public int findCount(Class<?> clz,Param pp,boolean needFilterData) throws Exception{
		return findCount(clz.getSimpleName(),pp,needFilterData);
	}
	public int findCount(Class<?> clz,Param pp) throws Exception{
		try{  
			Param p = pp.cloneme();
			List<Map<String,Object>> ml = hisql("select count(1) m from "+clz.getSimpleName()+" where 1=1 " , p); 		
			if(null != ml&&!ml.isEmpty()) {
				Object m = ml.get(0).get("m");
				if(m == null)
					m = "0";
				return Integer.valueOf(m.toString());
			}
		}catch(Exception e) {
			e.printStackTrace();
		}
		return 0;
	}
	public int findCount(String tableName,Param pp) throws Exception{
		return findCount(tableName,pp,true);
	}
	public int findCount(String tableName,Param pp,boolean needFilterData) throws Exception{
		try{
			Param p = pp.cloneme();
			List<Map<String,Object>> ml = hisql("select count(1) m from "+tableName+" where 1=1 " , p,needFilterData);
			if(null != ml&&!ml.isEmpty()) {
				Object m = ml.get(0).get("m");
				if(m == null)
					m = "0";
				return Integer.valueOf(m.toString());
			}
		}catch(Exception e) {
			e.printStackTrace();
		}
		return 0;
	}
	public void noInUse(String prefix,String nov) throws Exception{
		Param p = Param.getInstance();
		p.addParam("uno", nov);
		p.addParam("utype", prefix);
		this.hisql("update NOCreator set useed = 1 where uno="+nov, p);
	}
	private Class<T> fetchClazz(){
		Class<T> _clazz = (Class<T>)getSuperClassGenricType(super.getClass()); 
		if(null == _clazz||_clazz.getSimpleName().equals("Object"))
			_clazz = (Class<T>)getSuperClassGenricType(this.getClass()); 
		if(null == _clazz||_clazz.getSimpleName().equals("Object"))
			_clazz = (Class<T>)getSuperClassGenricType(this.getClass().getSuperclass()); 
		return _clazz;
	}
	@Override
	public String fetchNo() throws Exception{
		try{
			Class<T> clz = fetchClazz();
			return fetchNo("NO_",clz);
		}catch(Exception e) {
			e.printStackTrace();
		}
		return String.valueOf(new Date().getTime());
	}
	/**
	 * 获得一个自定义前缀的序号,提前生成PRENO指定大小
	 * @param prefix
	 * @param preNo 指定更新到的大小
	 * @return
	 * @throws Exception
	 */
	@Override
	public String fetchNo(String prefix,Integer preNo) throws Exception{
		try{
			Class<T> clz = fetchClazz();
			return fetchNo(prefix,clz);
			//end
		}catch(Exception e){
			e.printStackTrace();
		}
		return NOCreator.random(6);
	}
	
	/**
	 * 组合查询泛型版
	 * @author yonglu
	 * @time 2015-12-25 07:29
	 * @param claz 要查询的实体，必须继承可扩展基类
	 * @param param
	 * @param startIndex
	 * @param pageSize
	 * @return
	 * @throws Exception
	 */
	public <S extends Extendable> PageIterator<S> findPageAsJoin(Class<S> claz,Param p, int startIndex,int pageSize) throws Exception{
		Param param = p.cloneme();
		findParamtoSession(claz,param);
		proDataIsolation(claz,param);
		return this.getProxDao().findPageAsJoin(claz, param, startIndex, pageSize);
	}
	public <S extends Extendable> PageIterator<S> findPageAsJoin(Class<S> claz,Param p, int startIndex,int pageSize,boolean applyEntitySet) throws Exception{
		Param param = p.cloneme();
		findParamtoSession(claz,param);
		proDataIsolation(claz,param);
		return this.getProxDao().findPageAsJoin(claz, param, startIndex, pageSize,applyEntitySet);
	}



	protected Class<?> getSuperClassGenricType(Class<?> clazz) {
		Type genType = clazz.getGenericSuperclass();
		if (!(genType instanceof ParameterizedType)) {
			log.info(clazz.getName() + "的父类没有设置泛型参数！");
			return Object.class;
		}
		Type[] params = ((ParameterizedType) genType).getActualTypeArguments();
		if (params.length <= 0) {
			log.debug(clazz.getName() + "的泛型参数个数为： " + params.length);
			return Object.class;
		}
		if (!(params[0] instanceof Class)) {
			log.debug(clazz.getName() + " 没有设置可用的泛型参数！");
			return Object.class;
		}
		return ((Class<?>) params[0]);
	}

	protected <S> void findParamtoSession(Class<S> _clazz,Param p){
		if(null == _clazz)
			_clazz = (Class<S>)getSuperClassGenricType(super.getClass()); 
		if(null == _clazz||_clazz.getSimpleName().equals("Object"))
			_clazz = (Class<S>)getSuperClassGenricType(this.getClass()); 
		if(null == _clazz||_clazz.getSimpleName().equals("Object"))
			_clazz = (Class<S>)getSuperClassGenricType(this.getClass().getSuperclass()); 
		String k = "find_" + _clazz.getName().replaceAll("\\.", "_"); 
		if(null == AbstractBaseAuthorization.get()||null == AbstractBaseAuthorization.get().getSession())
			return;
		AbstractBaseAuthorization.putToSession(BaseEntity.fetchParamFindKey(_clazz.getName()), p);
	}
	protected <S> void proDataIsolation(Class<S> _clazz,Param p,String...sqlstr){  

	}
	public String getCurrentUserDataOwner(){
		JSONObject jsonuser = fetchJSONUser();
		if(null == jsonuser||!jsonuser.containsKey(BaseEntity.DATAOWNER)){
			return null;
		}
		return jsonuser.getString(BaseEntity.DATAOWNER); 
	}
	public String getCurrentEntECode(){
		JSONObject jsonuser = fetchJSONUser();
		if(null == jsonuser||!jsonuser.containsKey(BaseEntity.DATAOWNER)){
			return null;
		}
		String id = jsonuser.getString(BaseEntity.DATAOWNER);
		Param p = Param.getInstance();
		p.addParam("id", id);
		try {
			List<Map<String,Object>> g = this.hisql("select ecode from GroupCompanyEntity where id='"+id+"'", p,false);
			if(null == g||g.isEmpty())
				return null;
			Map<String,Object> m = g.get(0);
			if(!m.containsKey("ecode")||null == m.get("ecode"))
				return null;
			return m.get("ecode").toString();
		} catch (Exception e) { 
			e.printStackTrace();
		}
		return null;
	}
	public static String getCurrentDataOwner(){
		JSONObject jsonuser = fetchJSONUser();
		if(null == jsonuser||!jsonuser.containsKey(BaseEntity.DATAOWNER)){
			return "";
		} 
		Integer userType = jsonuser.getInteger("userType");
		if(BaseUserEntity.SYSUSER.intValue() == userType){ 
			return "";
		}
		String entid = jsonuser.getString(BaseEntity.DATAOWNER);
		if(!StringUtil.isValid(entid))
			return "";
		return entid;
	}
	public static String getCurrentDataOwnerParamSql(String prefix){
		String p = BaseEntity.DATAOWNER;
		if(StringUtil.isValid(prefix))
			p = prefix+"."+BaseEntity.DATAOWNER;;
		JSONObject j = fetchJSONUser();
		if(null == j||!j.containsKey(BaseEntity.DATAOWNER)){
			return p+" = 'ndataowber' ";
		} 
		Integer userType = j.getInteger("userType");
		Boolean isadmin = j.getBoolean("isAdmin");
		if((null != userType&&BaseUserEntity.SYSUSER.intValue() == userType)||(null != isadmin&&isadmin)){ 
			return " ("+p+" is not null or "+p+" is null) ";
		}
		String entid = j.getString(BaseEntity.DATAOWNER);
		if(!StringUtil.isValid(entid))
			return p+" = 'ndataowber' ";
		return p+"='"+entid+"'";
	}
	public static String getCurrentDataOwner(String prefix){
		String p = BaseEntity.DATAOWNER;;
		if(StringUtil.isValid(prefix))
			p = prefix+"."+BaseEntity.DATAOWNER;;
		JSONObject j = fetchJSONUser();
		if(null == j||!j.containsKey(BaseEntity.DATAOWNER)){
			return "("+p+"='ndataowber')";
		} 
		Integer userType = j.getInteger("userType");
		Boolean isadmin = j.getBoolean("isAdmin");
		if((null != userType&&BaseUserEntity.SYSUSER.intValue() == userType)||(null != isadmin&&isadmin)){ 
			return " ("+p+" is not null or "+p+" is null) ";
		}
		String entid = j.getString(BaseEntity.DATAOWNER);
		if(!StringUtil.isValid(entid))
			return "("+p+" = 'ndataowber' )";
		return "("+p+"='"+entid+"')";
	}
	protected <S> void proDataIsolation(S s){ 
		JSONObject jsonuser = fetchJSONUser();
		if(null == jsonuser||!jsonuser.containsKey(BaseEntity.DATAOWNER)){ 
			return;
		} 
		String entid = jsonuser.getString(BaseEntity.DATAOWNER);
		if(StringUtil.isValid(entid))
			if(s instanceof BaseEntity){
				if(StringUtil.isValid(((BaseEntity)s).getDataOwner()))
					return;
				((BaseEntity)s).setDataOwner(entid);
			}
	}
	@Override
	public <S> String doWorkFlow(S s,Integer proType) throws Exception{if(null ==s) {return null;} else { return null;}}
	private <S> String finalDoWorkFlow(S s,Integer proType) throws Exception{
		if(null == s) {return null;}
		try{
			this.getClass().getDeclaredMethod("doWorkFlow", new Class[] {Object.class,Integer.class});
		}catch(Exception e) {/*System.out.println(this.getClass()+"没有重写审批方法。");*/return null;}
		//处理异常，不影响业务执行
		try {
			//统一调用业务单元的工作流执行方法。
			//如果是删除，则检查状态是否没有进入工作流并且没有在审批，如为初始状态，则终止相关工作流
			if(proType == DELETE) {
				Param p = Param.getInstance();
				String eid = "";
				String sql = "";
				String entityName = "";
				String batchSql = "";
				//获得业务对应的状态
				if(s instanceof BaseEntity) {
					eid = ((BaseEntity) s).getId();
					entityName = s.getClass().getSimpleName();
					p.addParam("id", eid);
					sql = "select estate,"+BaseEntity.BATCHNO+" from "+entityName+" where id='"+eid+"' ";
				}else if(s instanceof String) {//如果是stirng,则为实体的ID,类为当前操作的泛型类
					Class<T> c = this.fetchClazz();
					eid = s.toString();
					entityName = c.getSimpleName();
					p.addParam("id", eid);
					sql = "select estate,"+BaseEntity.BATCHNO+" from "+entityName+" where id='"+eid+"' ";
				}
				List<Map<String,Object>> mlis = this.hisql(sql, p);
				if(null == mlis||mlis.isEmpty())
					return null;
				Integer estatev = ((UMap)mlis.get(0)).getInteger("estate");
				//如果为有效，则处理，然返回
				if(null == estatev)
					return null;
				if(BaseEntity.ENABLE == estatev||BaseEntity.NOREVIEW == estatev) {
					//检查是否是批量审批（数据有批号），如果是，则检查除本次要删除的数据外，是否还存在相同批量的数据。如存在，则不停此流程.youg 2018-11-01 08:53
					String batchNo =  ((UMap)mlis.get(0)).getString(BaseEntity.BATCHNO);
					if(StringUtil.isValid(batchNo)) {
						p.clear();
						p.addParam(BaseEntity.BATCHNO, batchNo);
						p.addParam("id", "!=",eid);
						if(this.findCount(entityName, p)>0) {
							System.out.println(this.getClass().getName()+" 存在相同批号的数据，不终止流程");
							return null;
						}
						batchSql = " or busiBatchNo='"+batchNo+"' ";
					}
					//end
					//更新流程实例
					p.clear();
					//p.addParam("businessId", "like",eid);
					String u = this.fetchUserId();
					StringBuilder sb = new StringBuilder();
					sb.append("update AcInstance set  estate=").append(BaseEntity.DISABLED).
							append(",lastModifyMan='").append(u).append("'").
							append(",lastModifyDate='").append(DateUtil.dateToString(new Date(),"yyyy-MM-dd")).append("' ").
							append(" where (businessId like '%").append(eid).append("' ").append(batchSql).append(") ");
					//将产品中的流程实例置为相应的状态
					this.hisql(sb.toString(), p);
					//同时结束流程
					IGService gservice = new GService("org.unitor.ssb.uactiviserviceapi.service.IAcBaseService");
					if(null != gservice) {
						if(null == batchNo)
							batchNo = "";
						Object res = gservice.call("completeWF", new Object[] {eid,batchNo});
						System.out.println("调用工作流结果:"+res);
					}
					return "-2";
				}
				return null;
			}
			//如果实体已已经进入审批了，则不处理新建工作流程
			boolean isok = true;
			if(s instanceof BaseEntity) {
				int est = ((BaseEntity) s).getEstate();
				if(est == BaseEntity.NOREVIEW)
					isok = false;
			}
			if(!isok)
				return "0";
			String res = doWorkFlow(s,proType);
			//完后处理一些清理事务。
			return res;
		}catch(Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public String changeSqlprovider(Connection c) throws SQLException {
		if(null == c)
			return null;
		String dataBaseType = c.getMetaData().getDatabaseProductName();
		if(!StringUtil.isValid(dataBaseType))
			return null;
		if("Microsoft SQL Server".equalsIgnoreCase(dataBaseType))
			return "sql";
		else if("MySQL".equalsIgnoreCase(dataBaseType))
			return "mysql";
		else if("Oracle".equalsIgnoreCase(dataBaseType))
			return "oracle"; 
		return null;
	}
	public <S> S findFull(Class<S> c,String id,Map<String,Byte> m) throws Exception { 
		S s = this.findEntityById(c, id); 
		if(null == s)
			return null;
		if(null == m)
			m = new HashMap<String,Byte>();
		if(m.containsKey(c.getName()))
			return s;
		m.put(c.getName(), (byte)1);
		List<ExtendedField> refs = AnnotationParser.parseReflectFields(c);
		if(null == refs||refs.isEmpty())
			return s;
		for(ExtendedField r:refs) {
			Object refidv = refu.gettFieldReadMethodValue(s, r.field.getName());
			if(null == refidv)
				continue;
			Object v = findFull(r.reff.entity(),refidv.toString(),m);
			refu.setFieldValues(s, r.reff.sourceFiledName(), v); 
		}
		return s;
	}
	public String audit(Class<?> c,String id,int estate,Param p) throws Exception { 
		p.addParam("id", id);
		//不处理相同的状态数据,这种可以查出目前的状态，然后比较如一样可以不处理后续，但这样在不一样的状态时，就多了一次检测，可以在最开始检查会好些。
		p.addParam("estate", "!=",estate);//
		String cd = DateUtil.dateToString(new Date(),"yyyy-MM-dd");
		BaseUserEntity bu = AbstractBaseAuthorization.sgetGeneralUserEntity(null);
		String u = bu.getName();
		String checku = ""; 
		String checkDate = "null"; 
		//如果是审核或不通过则有审核日期
		if(BaseEntity.PASSREVIEW == estate||BaseEntity.REVIEWREJECT==estate){
			checkDate ="'"+cd+"'"; 
			checku = u;
		} 
		StringBuilder sb = new StringBuilder();
		sb.append("update ").append(c.getSimpleName()).
		append(" set estate=").append(estate).
		append(", checkMan='").append(checku).
		append("',checkDate=").append(checkDate).
		append(", lastModifyMan='").append(u).
		append("',lastModifyDate='").append(cd).
		append("' where id='").append(id).append("' ");
		this.hisql(sb.toString(),p);
		//结束此数据的审批流程
		//UFlowService
		Object ufs = KernelActivitor.getService(UFlowService.class.getName());
		if(null != ufs){
			try{UFlowService uflow = (UFlowService)ufs;
			uflow.complete(id,"系统审批通过，流程结束");}catch(Exception e){e.printStackTrace();}
		}
		return null;
	}
	public String deAudit(Class<?> c,String id,Param p) throws Exception { 
		p.addParam("id", id);
		p.addParam("estate","sql","( estate ="+BaseEntity.PASSREVIEW+" or estate="+BaseEntity.REVIEWREJECT+" or estate is null) ");
		 
		StringBuilder sb = new StringBuilder();
		sb.append("update ").append(c.getSimpleName()).append(" set  estate=").
		append(BaseEntity.REVIEWREJECT).append(", checkMan=null, checkDate=null, lastModifyMan=null, lastModifyDate=null where id='").append(id).append("' ");
		this.hisql(sb.toString(),p);
		//结束此数据的审批流程
		//UFlowService
		Object ufs = KernelActivitor.getService(UFlowService.class.getName());
		if(null != ufs){
			try{UFlowService uflow = (UFlowService)ufs;
			uflow.complete(id,"系统取消审核，流程结束");}catch(Exception e){e.printStackTrace();}
		}
		return null;
	}

	public Map<String,Object> beanOrmPropertiesToMap(BaseEntity e)throws Exception {
		ReflectUtil refutil = ReflectUtil.getInstance();
		ExtendedField[] exfs = refutil.getAllEXPField(e.getClass());
		if(null == exfs||exfs.length<=0)
			return null;
		Map<String,Object> upm = new HashMap<String,Object>();
		for(ExtendedField f:exfs) {
			if(!f.needORM)
				continue;
			upm.put(f.getColumnName(), refutil.getFieldValue(e, f.field));
		}
		return upm;
	}
	public <S> String fetchBatchNo(Class<S> cs) {
		return DateUtil.dateToString(new Date(),"yyyyMMddHHmmssSSS");
		//return String.valueOf(new Date().getTime());
	}
	public <S> void create(List<S> lis) throws Exception {
		if(null == lis||lis.isEmpty())
			return;
		//生成一个批号
		String batchNo = fetchBatchNo(lis.get(0).getClass());//String.valueOf(new Date().getTime());//UUID.randomUUID().toString();//"p_"+new Date().getTime();
		GenericDao<T> d = getProxDao();
		for(S s:lis) {
			if(s instanceof BaseEntity) {
				generalSetBaseEntityValue((BaseEntity)s,true);
				((BaseEntity) s).setBatchNo(batchNo);
			}
			proDataIsolation(s);
			d.createEntity(s);
		}
		finalDoWorkFlow(lis,CREATE);
	}
	public <S> String doWorkFlow(List<S> lis,Integer proType) throws Exception{System.out.println("默认批处理创建流程处理");return null;}
	private <S> String finalDoWorkFlow(List<S> lis,Integer proType) throws Exception{
		if(null == lis||lis.isEmpty())
			return null;
		String res = doWorkFlow(lis,proType);
		//完后处理一些清理事务。
		return res;
	}
}
