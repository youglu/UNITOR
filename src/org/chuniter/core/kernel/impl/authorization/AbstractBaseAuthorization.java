package org.chuniter.core.kernel.impl.authorization;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.chuniter.core.kernel.api.IGeneralService;
import org.chuniter.core.kernel.api.authorization.IAuthorization;
import org.chuniter.core.kernel.api.dao.GenericDao;
import org.chuniter.core.kernel.impl.unit.UMap;
import org.chuniter.core.kernel.impl.web.BaseServlet;
import org.chuniter.core.kernel.kernelunit.DateUtil;
import org.chuniter.core.kernel.kernelunit.Param;
import org.chuniter.core.kernel.kernelunit.StringUtil;
import org.chuniter.core.kernel.model.BaseEntity;
import org.chuniter.core.kernel.model.BaseUserEntity;

import com.alibaba.fastjson.JSONObject;

public abstract class AbstractBaseAuthorization implements IAuthorization{
	protected Log log = LogFactory.getLog(super.getClass());
	private final static ThreadLocal<HttpServletRequest> req = new ThreadLocal<HttpServletRequest>();
	private final static ThreadLocal<HttpSession> sess = new ThreadLocal<HttpSession>();
	private final static Map<String,Object> tokenmap = new HashMap<String,Object>();
	private final static ThreadLocal<String> dw = new ThreadLocal<String>();
	public final static String TEMPATEDW = "NGUfY12";
	public static void setSession(HttpSession req_){
		sess.set(req_);
	}
	public static HttpSession getSession(){
		try {
			if(null != req.get()&&null != req.get().getSession(false))
				return req.get().getSession();
		}catch(Exception e) {}
		return sess.get();
	}
	public static void removeSession(){
		sess.remove();
	}
	public static void set(HttpServletRequest req_){
		req.set(req_);
	}
	public static HttpServletRequest get(){
		return req.get();
	}
	public static Object getFromSession(String k){
		HttpSession se = getSession();
		if(null == se)
			return null;
		return se.getAttribute(k);
	}
	public static void putToSession(String k,Object v){
		HttpSession se = getSession();
		if(null == se)
			return;
		se.setAttribute(k,v);
	}
	public static void delFromSession(String k){
		HttpSession se = getSession();
		if(null == se)
			return;
		se.removeAttribute(k);
	}
	public static void removeReq(){
		req.remove();
	}
	public static void setDW(String dataowner) {
		dw.set(dataowner);
	}
	public static String getDW() {
		return dw.get();
	}
	public static void delDW() {
		dw.remove();
	}
	public static void destory() {
		delDW();
		removeReq();
		removeSession();
	}
	@Override
	public BaseUserEntity getGeneralUserEntity(String token) throws Exception {
		if(tokenmap.containsKey(token))
			return (BaseUserEntity)tokenmap.get(token);
		return null;
	}
	@Override
	public void setGeneralUserEntity(String token, BaseUserEntity user) throws Exception {
		tokenmap.put(token, user);
	}

	@Override
	public void removeToken(String token) throws Exception {
		if(tokenmap.containsKey(token))
			tokenmap.remove(token);
	}
	/**
	 * 空实现，需要用户验证的子类去处理
	 * @author youg
	 * @time 2013-02-14
	 */
	@Override
	public String getUserName() {
		return sgetUserName(this);
	}
	public static String sgetUserName(AbstractBaseAuthorization ins) {
		if(null == req.get()||null == req.get().getSession(false))
			return null;
		if(null != ins)
			ins.log.debug("输出session id："+req.get().getSession().getId());
		BaseUserEntity bu;
		try {
			bu = sgetGeneralUserEntity(ins);
			if(null != bu)
				return bu.getName();
		} catch (Exception e) {
			e.printStackTrace();
		}

		Object currentUserName = getFromSession(IAuthorization.SESSION_USER);
		return (null==currentUserName?null:currentUserName.toString());
	}
	/**
	 * 空实现，需要用户验证的子类去处理
	 * @author youg
	 * @time 2013-02-14
	 */
	@Override
	public BaseUserEntity getGeneralUserEntity() throws Exception{
		return sgetGeneralUserEntity(this);
	}
	public static BaseUserEntity sgetGeneralUserEntity(AbstractBaseAuthorization ins) throws Exception{
		/*if(null == req.get()||null == req.get().getSession(false)){
			System.out.println(AbstractBaseAuthorization.class.getName()+"无法获得请求或会话");
			return null;
		}*/
		Object baseUserEntity = getFromSession(IAuthorization.SESSION_USERENTITY_JSON);
		if(null != baseUserEntity){
			//System.out.println("从会话获得USER："+baseUserEntity);
			if(baseUserEntity instanceof String){
				net.sf.json.JSONObject ujson = net.sf.json.JSONObject.fromObject(baseUserEntity);
				if(ujson.containsKey(IAuthorization.USER))
					ujson = ujson.getJSONObject(IAuthorization.USER);
				if(ujson.containsKey("memotions"))
					ujson.remove("memotions");//由于报baseentity没此属性，且找不到此属性的方法异常
				BaseUserEntity user = (BaseUserEntity)net.sf.json.JSONObject.toBean(ujson, BaseUserEntity.class);
				return user;
			}else if(baseUserEntity instanceof JSONObject){
				JSONObject ujson = (JSONObject)baseUserEntity;
				if(ujson.containsKey(IAuthorization.USER))
					ujson = ujson.getJSONObject(IAuthorization.USER);
				BaseUserEntity user = (BaseUserEntity)JSONObject.toJavaObject(ujson, BaseUserEntity.class);
				return user;
			}else if( baseUserEntity instanceof JSONObject){
				JSONObject userjson = ((JSONObject)baseUserEntity);
				if(userjson.containsKey(IAuthorization.USER))
					userjson = userjson.getJSONObject(IAuthorization.USER);
				proTMDJsonDate("createDate",userjson);
				proTMDJsonDate("lastModifyDate",userjson);
				proTMDJsonDate("lastLoginTime",userjson);
				proTMDJsonDate("lastLogoutTime",userjson);
				BaseUserEntity user = (BaseUserEntity)JSONObject.toJavaObject(userjson, BaseUserEntity.class);
				return user;
			}else if(baseUserEntity instanceof LinkedHashMap){
				Object userJsonStr = ((LinkedHashMap<?,?>)baseUserEntity);
				if(((LinkedHashMap<?,?>)baseUserEntity).containsKey(IAuthorization.USER))
					userJsonStr = ((LinkedHashMap<?,?>)baseUserEntity).get(USER);
				JSONObject json = JSONObject.parseObject(JSONObject.toJSONString(userJsonStr));
				BaseUserEntity user = (BaseUserEntity)JSONObject.toJavaObject(json, BaseUserEntity.class);
				return user;
			}
		}
		baseUserEntity = getFromSession(IAuthorization.SESSION_USERINFOMAP);
		if(null != baseUserEntity){
			BaseUserEntity bu = new BaseUserEntity();
			BaseServlet.copProperties(bu, baseUserEntity);
			return bu;
		}
		return null;
	}
	private static void proTMDJsonDate(String dk,JSONObject j) {
		if(j.containsKey(dk)) {
			JSONObject cd = j.getJSONObject(dk);
			if(cd.containsKey("time")) {
				j.put(dk, DateUtil.dateToString(new Date(cd.getLong("time"))));
			}
		}
	}

	public static String orgLimit(GenericDao<?> dao) {
		StringBuilder fs = new StringBuilder();
		try {
			BaseUserEntity u = sgetGeneralUserEntity(null);
			if(null == u)
				return "";
			String userid = u.getId();
			if(null != u.getIsAdmin()&&u.getIsAdmin()){ return ""; }
			StringBuilder sb = new StringBuilder();
			//检查是否有设置数据范围，如无则不进行组织范围过滤
			Param p = Param.getInstance();
			p.addParam(BaseEntity.DATAOWNER, u.getDataOwner());
			String sql = "select orgid from RoleOrgs ro where ro.roleId in(select id from roleentity  r where exists(select roleid from rolepersons rp where r.id = rp.roleid and rp.personid='"+userid+"'))";
			List<Map<String,Object>> ms = dao.hisql(sql, p);
			if(null != ms&&!ms.isEmpty()){
				for(Map<String,Object> m:ms) {
					String orgid = ((UMap)m).getString("orgid");
					if(StringUtil.isValid(orgid)){
						if(orgid.endsWith(","))
							orgid = orgid.substring(0,orgid.length()-1);
						sb.append(orgid).append(",");
					}
				}
			}
			//获得用户组织绑定.youg 2018-07-05 11:47
			p.clear();
			p.addParam("userid", userid);
			p.addParam(BaseEntity.DATAOWNER, u.getDataOwner());
			ms = dao.hisql("select orgid from UserOrgBind  where isBatch=1", p);
			if(null != ms&&!ms.isEmpty()){
				for(Map<String,Object> m:ms) {
					String orgid = ((UMap)m).getString("orgid");
					if(StringUtil.isValid(orgid)){
						if(orgid.endsWith(","))
							orgid = orgid.substring(0,orgid.length()-1);
						sb.append(orgid).append(",");
					}
				}
			}
			String[] orgids = sb.toString().split(",");
			for(int i=0,j=orgids.length;i<j;i++) {
				String s = orgids[i].trim();
				if(!StringUtil.isValid(s)||s.length()<=0)
					continue;
				fs.append(s);
				if(i<j-1)
					fs.append(",");
			}
			//end
		}catch(Exception e) {
			e.printStackTrace();
			//发生异常则返回不可用的ID
			if(fs.length()>0)
				fs.delete(0, fs.length());
			fs.append("查询组织范围发生异常");
		}
		return fs.toString();
	}
	public static String fetchDefDw(IGeneralService<?> service){
		try {
			//固定查询模板企业ID
			UMap dws = service.hisqlOne("select id from GroupCompanyEntity where ecode='"+TEMPATEDW+"'", Param.getInstance(),false);
			if(null == dws||dws.isEmpty())
				return null;
			return dws.getString("id");
		}catch(Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}
