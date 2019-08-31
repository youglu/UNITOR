package org.chuniter.core.kernel.api.cache;

import java.util.List;

public interface IUCache { 
	
	String HRDATABASECACHENAME = "hrbasedata";// 分类编码
	String POSTCACHENAME = "postdata";
	String HRBASECODE = "PS";
	String MODELDATA = "modeldata";
	String HRCACHE = "hr";// 其他缓存数据
	
	String ATTACH = "attach";
	public Object getObject(String cacheName,String key);
	public Object getObjectNoDataOwnerKey(String cacheName,String key);
	public Object getObject(String cacheName,String key,Boolean findDBWhilNoFind);
	public Object getObjectByName(String cacheName,String name) ;
	/**
	* @Description: 根据某一缓存，指定属性,并与指定值一样时，反回其对象
	* @author youg continentlu@sina.com
	* @date 2017年11月21日 上午11:29:11 
	* @version V1.0
	 */
	public Object getObjectByFieldName(String cacheName,String fieldName,String v) ;
	//由于上面这个方法只返回ID，不够用，所以增加一个返回全部属性的方法。主要担心上面方法有地方用了，改了之后会影响，所以才增加一个方法。
	public Object getObjectByFieldName2(String cacheName, String fieldName,String fv);
	public void putObject(String cacheName,String key, Object obj);
	public void putObjectNoDataOwnerKey(String cacheName,String key, Object obj) ;
	public void putObject(String cacheName, String key, Object obj,boolean needproDataOwnerKey);
	public void removeObject(String cacheName,String key);
	public void removeObjectNoDataOwnerKey(String cacheName,String key);
	public void removeObject(String cacheName,String key,boolean needproDataOwnerKey);
	
	/**
	 * 是否有附件
	 * @author youglu
	 * @time 2017-09-13 00:22
	 * @param entityId
	 * @return
	 */
	public boolean hasAttach(String entityId);
	public List<String> getKeys(String cacheName) ;
	/**
	 * 清空指定缓存
	* @Description: TODO
	* @author youg continentlu@sina.com
	* @date 2017年11月21日 下午3:25:06 
	* @version V1.0
	 */
	public void cleanCache(String cacheName);
	
	/**
	 * 设置企业号
	 * @author youglu 2018-01-13 10:22周六
	 * @param dataOwner
	 */
	void setDataOwner(String dataOwner);
	public String getDataOwner();
}
