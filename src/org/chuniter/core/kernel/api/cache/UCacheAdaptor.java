package org.chuniter.core.kernel.api.cache;

import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.chuniter.core.kernel.impl.GeneralServiceAdapter;
import org.chuniter.core.kernel.impl.authorization.AbstractBaseAuthorization;
import org.chuniter.core.kernel.impl.unit.UMap;
import org.chuniter.core.kernel.kernelunit.Param;
import org.chuniter.core.kernel.kernelunit.StringUtil;
import org.chuniter.core.kernel.model.BaseEntity; 
 

public abstract class UCacheAdaptor implements IUCache{

	public ThreadLocal<Map<String,Object>> defaultCache = new ThreadLocal<Map<String,Object>>(); 
	//public ThreadLocal<String> dataOwner = new ThreadLocal<String>();
	protected Log log = LogFactory.getLog(super.getClass());
	
	public Object getObject(String cacheName,String key) {
		return this.getObject(cacheName, key,null);
	}
	@Override
	public Object getObject(String cacheName,String key,Boolean findDBWhileNoCache) {
		if(null == defaultCache.get()||!defaultCache.get().containsKey(key))
			return null;
		return defaultCache.get().get(key);
	}
	public Object getObjectNoDataOwnerKey(String cacheName,String key){
		if(null == defaultCache.get()||!defaultCache.get().containsKey(key))
			return null;
		return defaultCache.get().get(key);
	}
	@Override
	public void putObject(String cacheName,String key, Object obj) {
		defaultCache.get().put(key, obj);
	}

	@Override
	public void removeObject(String cacheName,String key) {
		defaultCache.get().remove(key);
	}
	public void removeObjectNoDataOwnerKey(String cacheName,String key) {
		
	}
	
	@Override
	public boolean hasAttach(String entityId) { 
		return false;
	}
	@Override
	public List<String> getKeys(String cacheName) { 
		return null;
	}
	@Override
	public Object getObjectByName(String cacheName, String name) {
		return null;
	}
	
	@Override
	public void putObjectNoDataOwnerKey(String cacheName, String key, Object obj) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public Object getObjectByFieldName(String cacheName, String fieldName, String v) {
		// TODO Auto-generated method stub
		return null;
	} 
	@Override
	public void cleanCache(String cacheName) {
		defaultCache.get().clear();
	}

	@Override
	public Object getObjectByFieldName2(String cacheName, String fieldName, String fv) {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public void setDataOwner(String dataOwner) {
		//this.dataOwner = dataOwner;
		//this.dataOwner.set(dataOwner);
	}
	@Override
	public String getDataOwner() {
		/*if(!StringUtil.isValid(this.dataOwner.get())) {
			String dw =  GeneralServiceAdapter.getCurrentDataOwner();
			this.dataOwner.set(null == dw?null:dw);
		}
		return dataOwner.get();*/
		return GeneralServiceAdapter.getCurrentDataOwner();
	}                                                                            
	protected Object fetchFromDB(final String cacheName,String fdName,String fdv,org.chuniter.core.kernel.api.IGeneralService<?> service) {  
		try{ 
			String dw = getDataOwner(); 
			if((!StringUtil.isValid(fdName)&&!StringUtil.isValid(fdv))||!StringUtil.isValid(fdv))
				return null;
			if(!StringUtil.isValid(fdName))
				fdName = "id"; 
			Param p = Param.getInstance();
			//p.addParam(fdName,fdv); 
			Object o = null; 
			AbstractBaseAuthorization.setDW(dw);
			if(IUCache.HRDATABASECACHENAME.equals(cacheName)) {  
				//p.addParam("idcodesql","sql-server","(id='"+fdv+"' or name='"+fdv+"' or itemId='"+fdv+"')");
				p.addParam("id",fdv);
				p.addParam(BaseEntity.DATAOWNER, dw);
				List<Map<String,Object>> ms = service.hisql("select name,id,"+BaseEntity.DATAOWNER+",itemId from HRBaseDataEntity where 1=1 ", p);
				if(null != ms&&!ms.isEmpty()) {
					UMap mo = (UMap)ms.get(0);  
					p.clear();
					p.addParam("parentItemId", fdv);
					p.addParam("isDefault","order", fdv);
					p.addParam(BaseEntity.DATAOWNER, dw);
					ms = service.hisql("select name,id,"+BaseEntity.DATAOWNER+",itemId from HRBaseDataEntity where parentItemId='"+fdv+"'", p);
					if(null != ms&&!ms.isEmpty())
						mo.put("subs", ms);
					o = mo;
				}
				//如果没有再按parentid查,先查子，再查父
				p.clear();
				p.addParam("parentItemId", fdv);
				p.addParam("isDefault","order", fdv);
				p.addParam(BaseEntity.DATAOWNER, dw);
				ms = service.hisql("select name,id,"+BaseEntity.DATAOWNER+",itemId,category from HRBaseDataEntity where parentItemId='"+fdv+"'", p);
				if(null != ms&&!ms.isEmpty()) {
					UMap mo = (UMap)ms.get(0);  
					p.clear();
					p.addParam(BaseEntity.DATAOWNER, dw);
					p.addParam("itemid", fdv);
					p.addParam("parentItemId", mo.getString("category"));
					List<Map<String,Object>> ms1 = service.hisql("select name,id,"+BaseEntity.DATAOWNER+",itemId from HRBaseDataEntity where 1=1 ", p);
					if(null != ms1&&!ms1.isEmpty()) {
						mo = (UMap)ms1.get(0);  
						mo.put("subs", ms);
						o = mo;
					}
				}
				//由于去掉了名称查询与按编码查询，不知其它地方会不会出问题。但与其提供错误的数据，还不如为空。
			}else{
				p.clear();
				p.addParam(BaseEntity.DATAOWNER,dw);
				p.addParam("idcodesql","sql-server","(id='"+fdv+"' or depCode='"+fdv+"' or name='"+fdv+"' or ecode='"+fdv+"')");
				o = service.hisqlOne("select name,depcode,id,parentid,namePath,"+BaseEntity.DATAOWNER+" from OrgEntity where 1=1 ", p); 				
				if(null == o) {
					p.clear();
					p.addParam(BaseEntity.DATAOWNER, dw);
					p.addParam("idcodesql","sql-server","(id='"+fdv+"' or jobId='"+fdv+"' or jobName='"+fdv+"' or ecode='"+fdv+"')");
					o = service.hisqlOne("select jobName name,id,"+BaseEntity.DATAOWNER+" from JobEntity where 1=1 ", p); 
				} 
			}
			//if(null == o)
				//o = null; 
			return o;
		}catch(Exception e) {
			/*ignore exception*/
			log.error(this.getClass().getName()+":获得缓存值发生异常:"+e.getMessage()+ "\n"+"cacheName:"+cacheName+" fdName:"+fdName+"  fdv:"+fdv);
		}finally {
			try{
				AbstractBaseAuthorization.delDW(); 
				//AbstractBaseAuthorization.delFromSession(BaseEntity.DATAOWNER);
			}catch(Exception e) {e.printStackTrace();System.err.println(this.getClass().getName()+"重置会话中的DW发生异常:"+e.getMessage());;}
		}
		return null;
	}
	@Override
	public void putObject(String cacheName, String key, Object obj, boolean needproDataOwnerKey) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void removeObject(String cacheName, String key, boolean needproDataOwnerKey) {
		// TODO Auto-generated method stub
		
	}
	
	
}
