package org.chuniter.core.kernel.impl.web;

import java.awt.image.BufferedImage;
import java.beans.PropertyDescriptor;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.sql.Blob;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import javax.imageio.ImageIO;
import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.rowset.serial.SerialBlob;

import org.apache.commons.beanutils.BeanUtilsBean;
import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.beanutils.converters.BigDecimalConverter;
import org.apache.commons.beanutils.converters.DoubleConverter;
import org.apache.commons.beanutils.converters.IntegerConverter;
import org.apache.commons.beanutils.converters.LongConverter;
import org.apache.commons.beanutils.converters.ShortConverter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.chuniter.core.kernel.KernelActivitor;
import org.chuniter.core.kernel.annotation.EXColumn;
import org.chuniter.core.kernel.annotation.UService;
import org.chuniter.core.kernel.api.IGeneralService;
import org.chuniter.core.kernel.api.authorization.IAuthorization;
import org.chuniter.core.kernel.api.authorization.IPowerAuthorization;
import org.chuniter.core.kernel.api.authorization.PwoerEM;
import org.chuniter.core.kernel.api.cache.IUCache;
import org.chuniter.core.kernel.api.exception.CannotEditException;
import org.chuniter.core.kernel.api.web.IAction;
import org.chuniter.core.kernel.impl.GeneralServiceAdapter;
import org.chuniter.core.kernel.impl.authorization.AbstractBaseAuthorization;
import org.chuniter.core.kernel.impl.dao.ConnectionHandler;
import org.chuniter.core.kernel.impl.orm.ExtendedField;
import org.chuniter.core.kernel.impl.orm.ORMControler;
import org.chuniter.core.kernel.impl.unit.UMap;
import org.chuniter.core.kernel.kernelunit.AnnotationParser;
import org.chuniter.core.kernel.kernelunit.CusDateTimeConverter;
import org.chuniter.core.kernel.kernelunit.DateUtil;
import org.chuniter.core.kernel.kernelunit.JsonHelper;
import org.chuniter.core.kernel.kernelunit.PageIterator;
import org.chuniter.core.kernel.kernelunit.Param;
import org.chuniter.core.kernel.kernelunit.ReflectUtil;
import org.chuniter.core.kernel.kernelunit.StateCode;
import org.chuniter.core.kernel.kernelunit.StringUtil;
import org.chuniter.core.kernel.model.BaseEntity;
import org.chuniter.core.kernel.model.BaseUserEntity;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.webapp.WebAppContext;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import publib.Base64;

public abstract class BaseServlet extends HttpServlet implements IAction {

	private static final long serialVersionUID = -4351756960408825759L;
	protected Log log  = LogFactory.getLog(getClass());
	protected final String JSON = "json";
	protected String returnResultType = null;
	protected List<String> deleteIds;
	protected final static String SUCCESS = "success";
	protected String searchType = null;
	protected String keyWords;
	public final static CusDateTimeConverter covertor = new CusDateTimeConverter();
	// 分页属性
	protected int startIndex = 1;
	protected int limitSize = 80;

	//考虑使用弱引用 WeakReference
	private final static ThreadLocal<HttpServletRequest> reqthread = new ThreadLocal<HttpServletRequest>();
	private final static ThreadLocal<HttpServletResponse> respthread = new ThreadLocal<HttpServletResponse>();

	protected HttpServletRequest req;
	protected HttpServletResponse resp;
	protected String proResult;
	// 日期查询属性
	protected String startDate;
	protected String endDate;

	protected final String PROMSG = "promsg";
	public static String indexpage = "un";
	public static String UDATA = "ud";
	public static String PAGE = "pg";

	//查询参数类型
	/*protected final int FIND = 1;
	protected final int CREATEPARAM = 2;
	protected final int UPDATEPARAM = 3;
	protected final int DELETEPARAM = 4;  */

	public final static int CREATE = 1;
	public final static int UPDATE = 2;
	public final static int DELETE = 3;
	public final static int FIND = 4;

	//下载属性
	protected String dlFilePath;
	protected boolean isOnLine;
	protected String downloadMime = "application/x-msdownload";
	protected String nopermissionIndex = "/FIX/pbr/page/permissionDenied.html";

	public static String SERVERURL = "http://www.isunitor.com:8088";//"http://112.74.23.203";//"http://www.dgdshl.com";//
	public static String PHOTOURL = SERVERURL+"/FIX/member/lzh?mname=findphoto&headimgPath=";
	public final String[] excludeFields = new String[] { "lastModifyMan", "lastModifyDate", "createMan", "createDate", "lastOperation","optimizApp", "estate", "createrId" };

	private ReflectUtil refutil = ReflectUtil.getInstance();
	private Date limitDate = DateUtil.stringToDate("2027-01-30 01:00:00");
	protected IGeneralService<?> gservice;
	@UService
	protected IUCache ucache = null;

	public final static String KEYWORDSQL="kwsql";
	static {
		ConvertUtils.register(new LongConverter(null), Long.class);
		ConvertUtils.register(new ShortConverter(null), Short.class);
		ConvertUtils.register(new IntegerConverter(null), Integer.class);
		ConvertUtils.register(new DoubleConverter(null), Double.class);
		ConvertUtils.register(new BigDecimalConverter(null), BigDecimal.class);
	}
	public BaseServlet() {
		if(null == bb)
			bb = new uBeanUtilsBean(covertor);
	}
	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp)throws ServletException, IOException {
		try {
			resp.setCharacterEncoding("utf-8");
			reqthread.set(req);
			respthread.set(resp);

			try {
				AbstractBaseAuthorization.delDW();
				AbstractBaseAuthorization.set(req);
				this.req = req;
				this.resp = resp;
				if(new Date().after(limitDate)){
					resp.getWriter().write("您的授权已过期!请联系大数互联信息技术提供授权。");
					return;
				}
				try {
					// 判断是否有实现授权服务存在，不存在则不允许使用。
					IAuthorization authoritor = (IAuthorization) KernelActivitor.getService(IAuthorization.class.getName());
				} catch (NullPointerException e) {
					// throw new NullPointerException("无法获得用户验证单元，系统无法为您提供服务！");
					log.error("无法获得用户验证单元！");
				}
				try {
					fetchParamModel(req, resp,getModel());
					fetchGeneralParams(req, resp);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}catch(Exception e) {
				throw e;
			}
			super.service(req, resp);
		}catch(Exception e) {
			throw e;
		}finally {
			//保证每次都清除设置的优先DW值
			reqthread.set(req);
			putToSession(BaseEntity.DATAOWNER,null);
			AbstractBaseAuthorization.destory();
		}
	}

	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		this.doPost(req, resp);
	}
	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		String mname = "";
		try {
			this.getReq().setCharacterEncoding("utf-8");
			if(new Date().after(limitDate)){
				resp.getWriter().write("您的授权已过期!请联系大数互联信息技术提供授权。");
				return;
			}
			mname = this.getReq().getParameter("mname");
			if(!StringUtil.isValid(mname)){
				log.debug(this.getClass() + "没有指定要调用的方法");
				return;
			}
			if(null == ucache){
				try{
					Object c = KernelActivitor.getService(IUCache.class.getName());
					if(null != c) {
						ucache = (IUCache) c;
						BaseUserEntity cu = getCurrentLoginUserEntity();
						if(null != cu)
							ucache.setDataOwner(cu.getDataOwner());
					}
				}catch(Exception e){log.error("无法获得缓存服务："+e.getMessage());}
			}
			Method mm = refutil.getMethod(this.getClass(),mname);
			if(null==mm||!(Modifier.isPublic( mm.getModifiers()))) {
				resp.sendRedirect("/FIX/pbr/page/505.html");
				return;
			}
			/*目前只支持一个参数*/
			Object[] mparam = new Object[] {};
			Class<?>[] mpt = mm.getParameterTypes();
			if(mpt != null&&mpt.length>0) {
				Class<?> mc = mpt[0];
				//识别可变参数
				if(mc.isArray())
					mc = null;
				Object pobj = fetchParamModel(req, resp,null == mc?null:mc.newInstance());
				mparam = new Object[] {pobj};
			}
			proResult = (String) mm.invoke(this,mparam);
			if (JSON.equals(proResult)) {
				resp.setContentType("text/plain; charset=UTF-8");
			}
		}catch (Exception e) {
			log.error("调用方法"+this.getClass().getName()+"."+mname+" 异常"+e.getMessage());
			e.printStackTrace();
			Field f = refutil.getField(e.getClass(), "target");
			Object et = null;
			if(null != f)
				et = refutil.getFieldValue(e, f);
			if(!this.isAjaxRequestInternal(req, resp)){
				if(e instanceof CannotEditException||et instanceof CannotEditException){
					this.redirect("/FIX/pbr/page/cannotedit.html");
					return;
				}
				resp.sendRedirect("/FIX/pbr/page/505.html");
				return;
			}
			log.error(e.getMessage());
			if(e instanceof CannotEditException||et instanceof CannotEditException){
				if(!resp.isCommitted()){
					JSONObject j = new JSONObject();
					j.put(IAction.STATECODE, "数据已处理非操作状态");
					resp.setContentType("text/plain; charset=UTF-8");
					this.writeResult(j.toString());
				}
			}else if(!resp.isCommitted()){
				JSONObject j = new JSONObject();
				j.put(IAction.STATECODE, "系统繁忙，请稍侯再试.");
				resp.setContentType("text/plain; charset=UTF-8");
				this.writeResult(j.toString());
			}
		}
	}

	protected String getCurrentLoginUserName() {
		Object userName = this.getReq().getSession().getAttribute("userName");
		if (null != userName)
			return userName.toString();
		BaseUserEntity bu = this.getCurrentLoginUserEntity();
		if(null != bu)
			return bu.getName();
		JSONObject j = this.getCurrentLoginJsonUser();
		if (null == j)
			return null;
		if(j.containsKey("userName"))
			return j.get("userName").toString();
		return j.getString("name");
	}

	protected String getCurrentLoginUserId() {
		BaseUserEntity bu = this.getCurrentLoginUserEntity();
		if(null != bu)
			return bu.getId();
		JSONObject j = this.getCurrentLoginJsonUser();
		if (null == j)
			return null;
		return j.getString("id");
	}
	protected void proTemplateUserToSession(String dataOwner) {
		//设置一个临时用户到SESSION
		BaseUserEntity bu = new BaseUserEntity();
		bu.setId("temp_user");
		bu.setName("临时用户");
		bu.setDataOwner(dataOwner);
		JSONObject json = JSONObject.parseObject(net.sf.json.JSONObject.fromObject(bu).toString());
		storeMemberToSerssioin(json);
	}
	/*protected Map<String, Object> getCurrentLoginUserInfoMap() {
		if(null == this.getReq()||null == this.getReq().getSession(false))
			return null;
		Object mapObj = this.getReq().getSession().getAttribute(IAuthorization.SESSION_USERINFOMAP);
		if (null == mapObj)
			return null;
		Map<String, Object> userInfoMap = (Map<String, Object>) mapObj;
		return userInfoMap;
	}*/
	protected JSONObject getCurrentLoginJsonUser() {
		if(null == this.getReq()||null == this.getReq().getSession())
			return null;
		Object obj = this.getReq().getSession().getAttribute(IAuthorization.SESSION_USERENTITY_JSON);
		if (null != obj) {
			if(obj instanceof String){
				JSONObject userjson = JSONObject.parseObject(obj.toString());
				if(userjson.containsKey(IAuthorization.USER))
					userjson = userjson.getJSONObject(IAuthorization.USER);
				return userjson;
			}
			if(obj instanceof LinkedHashMap){
				Object userJsonStr = ((LinkedHashMap<?,?>)obj);
				obj = JSONObject.toJSON(userJsonStr);
			}
			JSONObject jsonuser = (JSONObject) obj;
			if(jsonuser.containsKey(IAuthorization.USER))
				return jsonuser.getJSONObject(IAuthorization.USER);
		}
		Object mapObj = this.getReq().getSession().getAttribute(IAuthorization.LOGIN_USER);
		if(null == mapObj)
			mapObj = GeneralServiceAdapter.fetchJSONUser();
		if(null == mapObj)
			return null;
		if(mapObj instanceof net.sf.json.JSONObject){
			JSONObject j = com.alibaba.fastjson.JSON.parseObject(((net.sf.json.JSONObject)mapObj).toString());
			AbstractBaseAuthorization.putToSession(IAuthorization.LOGIN_USER,j);
			return j;
		}
		if(mapObj instanceof String){
			JSONObject j = com.alibaba.fastjson.JSON.parseObject(mapObj.toString());
			AbstractBaseAuthorization.putToSession(IAuthorization.LOGIN_USER,j);
			return j;
		}
		JSONObject jsonuser = (JSONObject)mapObj;
		return jsonuser;


	}
	protected BaseUserEntity getCurrentLoginUserEntity() {
		if(null == this.getReq()||null == this.getReq().getSession(false))
			return null;
		Object userEntity = this.getReq().getSession(false).getAttribute(IAuthorization.SESSION_USERENTITY_JSON);
		if(null == userEntity)
			return null;

		if(userEntity instanceof String){
			//JSONObject userjson = JSONObject.parseObject(userEntity.toString()).getJSONObject(IAuthorization.USER);
			//为什么这个自义字段容器会报错
			//userjson.getJSONObject("customProperty").put("sss", 11);
			//userjson.remove("customProperty");
			net.sf.json.JSONObject ujson = net.sf.json.JSONObject.fromObject(userEntity);
			if(ujson.containsKey(IAuthorization.USER))
				ujson = ujson.getJSONObject(IAuthorization.USER);
			if(ujson.containsKey("memotions"))
				ujson.remove("memotions");//由于报baseentity没此属性，且找不到此属性的方法异常
			BaseUserEntity user = (BaseUserEntity)net.sf.json.JSONObject.toBean(ujson, BaseUserEntity.class);//(userjson, BaseUserEntity.class);
			putToSession(IAuthorization.SESSION_USERENTITY,user);
			return user;
		}
		//这里有可能当登录为会员时，而又刷新了会员单元，导致这里面保存的member与刷新后的member的clasloader不一致而发生ClassCastException异常,暂时只作清除已有的session用户信息作为处理
		if(!(userEntity instanceof BaseUserEntity)){
			BaseUserEntity user = new BaseUserEntity();
			JSONObject jsonuser= this.getCurrentLoginJsonUser();
			if(jsonuser.containsKey("usreId"))
				user.setId(jsonuser.get("userId").toString());
			if(jsonuser.containsKey("userName"))
				user.setName(jsonuser.get("userName").toString());
			if(jsonuser.containsKey("name")){
				net.sf.json.JSONObject ujson = net.sf.json.JSONObject.fromObject(userEntity);
				if(ujson.containsKey(IAuthorization.USER))
					ujson = ujson.getJSONObject(IAuthorization.USER);
				if(null == ujson||ujson.isEmpty())
					ujson = net.sf.json.JSONObject.fromObject(userEntity);
				user = (BaseUserEntity)net.sf.json.JSONObject.toBean(ujson, BaseUserEntity.class);//(userjson, BaseUserEntity.class);
			}
			if(jsonuser.containsKey("isAdmin")&&null != jsonuser.getBoolean("isAdmin"))
				user.setIsAdmin(jsonuser.getBoolean("isAdmin"));
			if(jsonuser.containsKey("roleIds")){
				Object roleIds = jsonuser.get("roleIds");
				if(roleIds instanceof String[])
					user.setRoleIds((String[])jsonuser.get("roleIds"));
				else if(roleIds instanceof ArrayList){
					String[] roleids = new String[((ArrayList<?>)roleIds).size()];
					((ArrayList<?>)roleIds).toArray(roleids);
					user.setRoleIds(roleids);
				} else if(roleIds instanceof JSONArray){
					String[] roleids = new String[((JSONArray)roleIds).size()];
					((JSONArray)roleIds).toArray(roleids);
					user.setRoleIds(roleids);
				}
			}
			if(jsonuser.containsKey("roleNames")){
				Object roleIds = jsonuser.get("roleNames");
				if(roleIds instanceof String[])
					user.setRoleIds((String[])jsonuser.get("roleNames"));
				else if(roleIds instanceof ArrayList){
					String[] roleids = new String[((ArrayList)roleIds).size()];
					((ArrayList)roleIds).toArray(roleids);
					user.setRoleIds(roleids);
				} else if(roleIds instanceof JSONArray){
					String[] roleids = new String[((JSONArray)roleIds).size()];
					((JSONArray)roleIds).toArray(roleids);
					//
				}
			}

			return user;
		}
		return (BaseUserEntity) userEntity;
	}

	public int getStartIndex() {
		return startIndex;
	}

	public void setStartIndex(int startIndex) {
		this.startIndex = startIndex;
	}

	public int getLimitSize() {
		return limitSize;
	}

	public void setLimitSize(int limitSize) {
		this.limitSize = limitSize;
	}

	private void fetchGeneralParams(HttpServletRequest req,HttpServletResponse resp) {
		try {
			this.setKeyWords(this.getReq().getParameter("keyWords"));
			this.setSearchType(this.getReq().getParameter("searchType"));
			if (null != this.getReq().getParameter("limitSize"))
				this.setLimitSize(Integer.parseInt(req
						.getParameter("limitSize")));
			if (null != this.getReq().getParameter("startIndex"))
				this.setStartIndex(Integer.parseInt(req
						.getParameter("startIndex")));
			//兼容jquery flexgrid插件
			if (null != this.getReq().getParameter("rp"))
				this.setLimitSize(Integer.parseInt(req
						.getParameter("rp")));
			if (null != this.getReq().getParameter("page")){
				int page = Integer.parseInt(this.getReq().getParameter("page"));
				if(page >0)
					page = page-1;
				this.setStartIndex(page*this.limitSize);
			}
			String[] delIds = this.getReq().getParameterValues("deleteIds");
			if (null == delIds || delIds.length <= 0) {
				delIds = this.getReq().getParameterValues("deleteIds[]");
			}
			if (null != delIds && delIds.length > 0) {
				this.deleteIds = Arrays.asList(delIds);
			}
			startDate = this.getReq().getParameter("startDate");
			endDate = this.getReq().getParameter("endDate");
			if(StringUtil.isValid(startDate))
				startDate = URLDecoder.decode(startDate,"UTF-8");
			if(StringUtil.isValid(endDate))
				endDate = URLDecoder.decode(endDate,"UTF-8");
			BaseEntity b = this.getModel();
			if(null != b){
				String id = b.getId();
				if(StringUtil.isValid(id)&&"null".equals(id)){
					b.setId("");
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public String execute() throws Exception {
		return execute();
	}

	public String find() throws Exception {
		return "find";
	}

	public String create() throws Exception {
		return "create";
	}

	public String update() throws Exception {
		return "update";
	}

	public String delete() throws Exception {
		return "delete";
	}
	public String print() throws Exception {
		return "print";
	}

	public String inputs() throws Exception {
		return "inputs";
	}

	public String export() throws Exception {
		return "export";
	}
	public String audit() throws Exception {
		return this.audit(true);
	}
	public String deAudit() throws Exception {
		return this.deAudit(true);
	}

	/**
	 * 通用审核操作
	 * @author youg 2016-10-12 09:17
	 * @return
	 * @throws Exception
	 */
	protected String afterAudit(String id) { return null; }
	protected String beforAudit(String id) { return null; }
	public String audit(boolean...needwriteresp) throws Exception {
		JSONObject j = new JSONObject();
		//判断权限
		String auditpid = fetchAuditPID();
		if(!auditpid.endsWith(PwoerEM.AUDIT.getDescription())&&!auditpid.endsWith(PwoerEM.AUDIT.name()))
			auditpid+="."+PwoerEM.AUDIT.getDescription();
		if (!hasPermission(auditpid,false)){
			log.error("您没有该操作的权限!");
			//this.redirect("/FIX/pbr/page/permissionDeniedJSON.html");
			j.put(IAction.STATECODE,"对不起，你没有该操作的权限！");
			this.writeResult(j.toJSONString());
			return JSON;
		}

		//Field idf = refutil.getField(this.getModel().getClass(), "id");
		String[] ids = this.getParams("ids[]");
		if(null == ids||ids.length<=0){
			String idstrs = this.getParameter("id");
			if(StringUtil.isValid(idstrs)){
				if(idstrs.indexOf(",") != -1)
					ids = idstrs.split(",");
				else
					ids = new String[]{idstrs};
			}
			if(null == ids||ids.length<=0){
				j.put(IAction.STATECODE,StateCode.NODATA);
				this.writeResult(j.toString());
				return JSON;
			}
		}
		for(String id:ids){
			if(!StringUtil.isValid(id))
				continue;
			String msg = beforAudit(id);
			if(StringUtil.isValid(msg)) {
				j.put(IAction.STATECODE,msg);
				if(null == needwriteresp||needwriteresp.length>0&&needwriteresp[0]){
					this.writeResult(j.toString());
					return JSON;
				}
				return j.toString();
			}
			String estateStr = this.getParameter("estate");
			int estate = BaseEntity.REVIEWREJECT;
			String proType = this.getParameter("proType");
			if(!StringUtil.isValid(estateStr)&&StringUtil.isValid(proType)){
				estate = "1".equals(proType)?BaseEntity.PASSREVIEW:BaseEntity.REVIEWREJECT;
			}//else 考虑安全问题这里注掉. 2017-06-16 16:36 youg
			//estate = Integer.valueOf(estateStr);
			//上次有注掉这个，但注掉后,estate的值不是根界面来设值，且protype为null时，界面就算审核通过这里也是未通过,所以还要处理下. 2017-10-14 10：47 youg
			else{
				if(!StringUtil.isValid(proType)){
					try{estate = Integer.valueOf(estateStr);}catch(Exception e){};
				}
			}
			//因为DEAUDIT也改为调此方法，那里设置的状态在REQ ATTRIBUTE中，所以这里如果有的话，就优先此参数.youg 2018-09-14 11:05
			Object deauditEstate = this.getReq().getAttribute("deauditEstate");
			if(null != deauditEstate)
				try{estate = Integer.valueOf(deauditEstate.toString());}catch(Exception e){};;
			//end
			//end youg
			Param p = Param.getInstance();
			//如果选择了按批号批量审核，则需要更新与此单批号一样的数据的状态为本次的审核状态
			String batchCheck = this.getParameter("batchCheck");
			if(null != batchCheck&&"1".equals(batchCheck)) {
				this.gservice.audit(this.getModel().getClass(), id, estate, p);//,true);
			}else
				this.gservice.audit(this.getModel().getClass(), id, estate, p);
			msg = afterAudit(id);
			if(StringUtil.isValid(msg)) {
				j.put(IAction.STATECODE,msg);
				if(null == needwriteresp||needwriteresp.length>0&&needwriteresp[0]){
					this.writeResult(j.toString());
					return JSON;
				}
				return j.toString();
			}
		}
		j.put(IAction.STATECODE,StateCode.SUCCESS);
		if(null == needwriteresp||needwriteresp.length>0&&needwriteresp[0]){
			this.writeResult(j.toString());
			return JSON;
		}
		return j.toJSONString();
	}

	/**
	 * 通用取消审核操作
	 * @author youg 2016-10-12 09:17
	 * @return
	 * @throws Exception
	 */
	protected String afterDeAudit(String id) { return null; }
	public String deAudit(boolean...needwriteresp) throws Exception {
		//JSONObject j = new JSONObject();
		this.getReq().setAttribute("deauditEstate", BaseEntity.REVIEWREJECT);
		return this.audit(needwriteresp);
		//Field idf = refutil.getField(this.getModel().getClass(), "id");
		/*改为调用统一的审核方法
		String[] ids = this.getParams("ids[]");
		if(null == ids||ids.length<=0){
			String idstrs = this.getParameter("id");
			if(StringUtil.isValid(idstrs)){
				if(idstrs.indexOf(",") != -1)
					ids = idstrs.split(",");
				else
					ids = new String[]{idstrs};
			}
			if(null == ids||ids.length<=0){
				j.put(IAction.STATECODE,StateCode.NODATA);
				this.writeResult(j.toString());
				return JSON;
			}
		}
		for(String id:ids){
			if(!StringUtil.isValid(id)){
				continue;
			}
			Param p = Param.getInstance();
			this.gservice.deAudit(this.getModel().getClass(), id, p);
			afterDeAudit(id);
		}
		j.put(IAction.STATECODE,StateCode.SUCCESS);
		if(null != needwriteresp&&needwriteresp.length>0&&needwriteresp[0]){
			this.writeResult(j.toString());
			return JSON;
		}
		return j.toJSONString();
		*/
	}

	public String manage() throws Exception {
		return "manage";
	}

	protected boolean needSuperReturn() {
		if (null == returnResultType || "".equals(returnResultType))
			return false;
		return true;
	}

	public String getReturnResultType() {
		return returnResultType;
	}

	public void setReturnResultType(String returnResultType) {
	}

	public List<String> getDeleteIds() {
		return deleteIds;
	}

	public void setDeleteIds(List<String> deleteIds) {
		this.deleteIds = deleteIds;
	}

	public String getSearchType() {
		return searchType;
	}

	public void setSearchType(String searchType) {
		this.searchType = searchType;
	}

	public String getKeyWords() {
		return getKeyWords(false);
	}
	public String getKeyWords(boolean dec) {
		if(!StringUtil.isValid(keyWords))
			return "";
		try{
			if(dec)
				keyWords = URLDecoder.decode(keyWords,"utf8");
		}catch(Exception e){
			return keyWords;
		}
		if(StringUtil.isValid(keyWords))
			keyWords = keyWords.replaceAll("，", ",");
		return keyWords;
	}

	public void setKeyWords(String keyWords) {
		this.keyWords = keyWords;
	}

	protected IPowerAuthorization getAuthorizationService()
			throws NullPointerException {
		try {
			return (IPowerAuthorization) KernelActivitor .getService(IPowerAuthorization.class.getName());
		} catch (NullPointerException e) {
			throw new NullPointerException("无法获得权限验证单元，系统暂时无法提供权限验证服务！");
		}
	}

	protected boolean hasPermission(String powerId) {
		return hasPermission(powerId,false);
	}
	protected boolean hasPermission(String powerId,Boolean defper) {
		try {
			return hasPermission(powerId, "");
		} catch (NullPointerException ne) {
			log.error("发生异常：返回默认:"+defper);
			return defper;
		}
	}
	protected boolean hasPermission(String modelId,String powerName) {
		try {
			log.info("验证权限:"+modelId+"  "+powerName);
			IPowerAuthorization authorizationService = getAuthorizationService();
			if(null == authorizationService) {
				log.error("authorizationService为空：返回默认:"+false);
				return false;
			}
			if(!StringUtil.isValid(powerName)) {
				log.info("功能名为空:"+modelId+" "+powerName);
			}
			int dotindex = powerName.lastIndexOf(".");
			if(dotindex != -1)
				powerName = powerName.substring(dotindex+1);
			boolean canpro = authorizationService.isAuthoritedPower(modelId, powerName);
			log.info("验证结果:"+canpro);
			return canpro;
		} catch (NullPointerException ne) {
			ne.printStackTrace();
		}
		return false;
	}
	public BaseServlet createAC() {
		log.info("类" + getClass() + "创建新的实例");
		BaseServlet ac = this.createAction();
		if(null == ac||ac == this){
			try {
				ac = this.getClass().newInstance();
			}  catch (Exception e) {
				e.printStackTrace();
			}
		}
		if(null != ac)
			ac.setService(this.gservice);
		return ac;
	}

	public HttpServletRequest getReq() {
		return reqthread.get();
	}
	public void writeResult(String result) throws IOException {
		this.getReq().setAttribute("submited", 1);
		this.getResp().getWriter().write(result);
	}
	public void nodataView(String msg) throws IOException {
		this.writeResult("<div style=\"text-align:center;margin-top: 3rem;\"><img src=\"/FIX/pbr/weixin/img/no-data.svg\" style=\"width: 30%;\"><div style='text-align:center;font-size: 30px;font-family: Microsoft YaHei,\\\\5FAE\\8F6F\\96C5\\9ED1,\\5FAE\\8F6F\\96C5\\9ED1,Tahoma,\\\\5B8B\\4F53,helvetica,Hiragino Sans GB;'>"+msg+"</div></div>");
	}
	public String getParameter(String name) {
		return reqthread.get().getParameter(name);
	}
	public String[] getParams(String name) {
		return reqthread.get().getParameterValues(name);
	}
	public HttpServletResponse getResp() {
		return respthread.get();
	}

	protected abstract BaseServlet createAction();

	protected String getUserAccessInfo() {
		StringBuilder sb = new StringBuilder();
		String agent = this.getReq().getHeader("user-agent");
		sb.append(agent).append(",");
		sb.append(this.getReq().getMethod()).append(",");// ：获得客户端向服务器端传送数据的方法有get、post、put等类型
		sb.append(this.getReq().getRequestURI()).append(",");// 获得发出请求字符串的客户端地址
		sb.append(this.getReq().getServletPath()).append(",");// 获得客户端所请求的脚本文件的文件路径
		sb.append(this.getReq().getServerName()).append(",");// 获得服务器的名字
		sb.append(this.getReq().getServerPort()).append(",");// 获得服务器的端口号
		sb.append(this.getReq().getRemoteAddr()).append(",");// 获得客户端的ip地址
		sb.append(this.getReq().getRemoteHost()).append(",");// 获得客户端电脑的名字，若失败，则返回客户端电脑的ip地址
		sb.append(this.getReq().getProtocol()).append(",");//
		return sb.toString();
	}

	protected abstract <S> S getModel();
	/**
	 * 根据T的类型 ，从reqeust中提取数据并填充
	 * @author youg
	 * @time 2012-07-30
	 * @param request
	 * @param response
	 * @return
	 * @throws Exception
	 */
	private static BeanUtilsBean bb;
	protected <S> S fetchParamModel(HttpServletRequest request, HttpServletResponse response,S sm)throws Exception{
		//S model = getModel();
		if(null == sm){
			//throw new Exception("如果您需要自动设置页面数据到实体中，请实现getModel方面，并通过该方法可以获得设置参数据对象。当前获取不到设置参数的对象！");
			return sm;
		}
		try{
			Map<String,String[]> paramMap = request.getParameterMap();
			if(null == paramMap||paramMap.isEmpty())
				return null;
			//对于JSON实体的处理 2015-07-06 18:53 youg
			if(sm.getClass().getName().equalsIgnoreCase("com.alibaba.fastjson.JSONObject")){
				com.alibaba.fastjson.JSONObject json = new com.alibaba.fastjson.JSONObject();
				json.putAll(paramMap);
				return (S)json;
			}
			if(sm instanceof String){
				for(Entry<String,String[]> entry:paramMap.entrySet()){
					if(!IAction.MNAME.equals(entry.getKey())){
						sm = (S) entry.getValue()[0];
						return sm;
					}
				}
			}
			bb.populate(sm , paramMap);
		}catch(IllegalArgumentException ie){
			ie.printStackTrace();
			log.error(ie.fillInStackTrace().toString());
			System.err.println(this.getClass().getName()+" : "+ie.getMessage());
		}catch(Exception e){e.printStackTrace();}

		return sm;
	}
	/**
	 * 参数拷贝
	 * @param dest 目标
	 * @param orig 数据源
	 * @author
	 * @throws InvocationTargetException
	 * @throws IllegalAccessException
	 * */
	public static void copProperties(Object dest, Object orig) throws IllegalAccessException, InvocationTargetException {
		bb.copyProperties(dest, orig);
	}

	@Override
	public String downloadFile() throws Exception {
		BufferedOutputStream bo = null;
		FileInputStream in = null;
		try{
			this.getResp().reset();
			bo = new BufferedOutputStream(this.getResp().getOutputStream());
			byte[] b = new byte[1024];
			File file = new File(dlFilePath);
			String fname = "";
			if(file.exists())
				fname = file.getName();//URLEncoder.encode(file.getName(),"utf-8");
		    /*String agent = req.getHeader("USER-AGENT");
            if(agent != null && agent.toLowerCase().indexOf("firefox") > 0) {
            	//fname =  (new String(Base64Encoder.en(fname.getBytes("UTF-8")))) + "?=";//"=?UTF-8?B?" ;
            	fname = java.net.URLEncoder.encode(fname, "UTF-8");
            }else*/
			fname =  java.net.URLEncoder.encode(fname, "UTF-8");
			if (isOnLine) {
				// 在线打开方式
				this.getResp().setContentType(downloadMime);
				this.getResp().setHeader("Content-Disposition", "inline;filename=" +fname);
				// 文件名应该编码成UTF-8
			} else {
				// 下载方式
				this.getResp().setContentType(downloadMime);
				this.getResp().setHeader("Content-Disposition", "attachment;filename*=utf-8'zh_cn'" + fname);
			}
			this.getResp().addHeader("Content-Length",String.valueOf(file.length()));
			in = new FileInputStream(file);
			int n = 0;
			while((n = in.read(b)) != -1)
				bo.write(b, 0, n);
			bo.flush();
			bo.close();
			bo = null;
			in.close();
			in = null;
		}catch(Exception e){
			throw e;
		}finally{
			try{
				if(bo != null){
					bo.close();
					bo = null;
				}
				if(null != in){
					in.close();
					in = null;
				}
			}catch(Exception e){
				throw e;
			}
		}
		return null;
	}

	@Override
	protected Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

	protected void forward(String url) throws ServletException, IOException{
		reqthread.get().getRequestDispatcher(url).forward(getReq(), getResp());
	}
	protected void redirect(String url) throws IOException{
		url = url.trim();
		if(url.indexOf("jsessionid") == -1){
			int i = url.indexOf("?");
			if(i != -1){
				String u1 = url.substring(0,i);
				url=u1+";jsessionid="+this.getReq().getSession().getId()+url.substring(i, url.length());
			}else
				url+=";jsessionid="+this.getReq().getSession().getId();
		}
		this.getReq().setAttribute("submited",1);
		respthread.get().sendRedirect(url);
	}

	public static String getUserPlatform(HttpServletRequest reqs) {
		String agent = reqs.getHeader("User-Agent");
		String[] keywords = { ANDROID,IPAD,IPHONE,IPOD,MQQ,WINP };
		if (null != agent&&(agent.indexOf("Windows NT") == -1||
				(agent.indexOf("Windows NT") == -1 &&
						agent.indexOf("compatible; MSIE 9.0;") == -1)))
			if (agent.indexOf("Windows NT") == -1 &&
					agent.indexOf("Macintosh") == -1)
				for (String item : keywords)
					if (agent.indexOf(item) != -1)
						return item;
		return null;
	}
	/**
	 * 获取用户真实IP地址，不使用request.getRemoteAddr();的原因是有可能用户使用了代理软件方式避免真实IP地址,
	 * 可是，如果通过了多级反向代理的话，X-Forwarded-For的值并不止一个，而是一串IP值，究竟哪个才是真正的用户端的真实IP呢？
	 * 答案是取X-Forwarded-For中第一个非unknown的有效IP字符串。
	 * 如：X-Forwarded-For：192.168.1.110, 192.168.1.120, 192.168.1.130,
	 * 192.168.1.100
	 * 用户真实IP为： 192.168.1.110
	 * @param request
	 * @return
	 */
	public static String getIpAddress(HttpServletRequest request) {
		String ip = request.getHeader("X-forwarded-for");
		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("X-Real-IP");
		}
		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("Proxy-Client-IP");
		}
		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("WL-Proxy-Client-IP");
		}
		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("HTTP_CLIENT_IP");
		}
		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("HTTP_X_FORWARDED_FOR");
		}
		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getRemoteAddr();
		}
		return ip;
	}
	public static void showCookieAndHeaderInfo(HttpServletRequest hreq){
		Cookie[] cookies =  hreq.getCookies();
		if (null != cookies) {
			Cookie myCookie = null;
			for (int i = 0; i < cookies.length; i++) {
				myCookie = cookies[i];
				myCookie.getDomain();
				System.out.println("myCookie.getDomain() = "+myCookie.getDomain());
				System.out.println("cookie = "+myCookie.getValue());
				System.out.println("name = "+myCookie.getName());
			}
		}

		Enumeration<String> hns = hreq.getHeaderNames();
		while(hns.hasMoreElements()){
			String hn = hns.nextElement();
			System.out.println(hn+"  "+hreq.getHeader(hn));
		}
	}

	/*public Object getFromSession(String key){
		return reqthread.get().getSession().getAttribute(key);//this.getReq().getSession().getAttribute(key);
	}*/
	public <S> S getFromSession(String key){
		Object o = reqthread.get().getSession().getAttribute(key);
		if(null == o)
			return null;
		return (S)o;
	}
	public static void putToSession(String key, Object obj){
		if(null == reqthread.get())
			return;
		reqthread.get().getSession().setAttribute(key, obj);
	}

	/**
	 * 将列表、数组转换成JSON数组
	 * @param obj
	 * @return
	 */
	public <T> JSONArray toJSONArray(Object obj){
		return JsonHelper.toJSONArray(obj);
	}

	/**
	 * 将实体类转换成JSON
	 * @param obj 实体类
	 * @param excludeMap 不转换指定
	 * @return
	 */
	public JSONObject toJSON(Object obj) {
		return JsonHelper.toJSON(obj, null);
	}

	/**
	 * 将实体类转换成JSON
	 * 时间格式：yyyy-MM-dd HH:mi:ss
	 * @param obj 实体类
	 * @return
	 */
	public static JSONObject toJSONDateStr(Object obj) {
		return JsonHelper.toJSONDateStr(obj);
	}

	public Map<String,Object> beanOrmPropertiesToMap(BaseEntity e)throws Exception {
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
	/**
	 * 拷贝属性
	 * @param target
	 * @param source
	 * @throws Exception
	 */
	public static void copyProperties(Object target, Object source)throws Exception {
		if(target == null || source==null) {
			throw new Exception("Source and Target must not be null");
		}
		PropertyDescriptor[] targetPds = PropertyUtils.getPropertyDescriptors(target.getClass());
		for (PropertyDescriptor targetPd : targetPds) {
			if (targetPd.getWriteMethod() != null) {
				PropertyDescriptor sourcePd = PropertyUtils.getPropertyDescriptor(source, targetPd.getName());
				if (sourcePd != null && sourcePd.getReadMethod() != null) {
					try {
						Method readMethod = sourcePd.getReadMethod();
						if (!Modifier.isPublic(readMethod.getDeclaringClass().getModifiers())) {
							readMethod.setAccessible(true);
						}
						Object value = readMethod.invoke(source);
						// 这里判断以下value是否为空 当然这里也能进行一些特殊要求的处理 例如绑定时格式转换等等
						if (value != null) {
							Method writeMethod = targetPd.getWriteMethod();
							if (!Modifier.isPublic(writeMethod.getDeclaringClass().getModifiers())) {
								writeMethod.setAccessible(true);
							}
							writeMethod.invoke(target, value);
						}
					} catch (Throwable ex) {
						throw new Exception("Could not copy properties from source to target",ex);
					}
				}
			}
		}
	}
	public static void storeMemberToSerssioin(HttpServletRequest _req,HttpServletResponse _resp,JSONObject mem){
		reqthread.set(_req);
		respthread.set(_resp);
		storeMemberToSerssioin(mem);
	}

	public static void storeMemberToSerssioin(JSONObject mem){
		//登陆成功，将用户信息存到session
		putToSession(IAuthorization.LOGIN_USER, mem);
		//由于现有的用户登录检测过滤器检查session的属性是：sessionUser，故在此把该属性也加入到session值中。 2015-07-13 00:07
		Map<String,Object> userInfoMap = mem;
		userInfoMap.put("userId",mem.getString("id"));
		userInfoMap.put("userName",mem.getString("name"));
		String dw = mem.getString(BaseEntity.DATAOWNER);
		userInfoMap.put("dataOwner",dw);
		try {
			//登录成功后，将用户数据放入session，再清空企业db配置的缓存，保证如果对企业db有更改能生效。youg 2018-06-18 00:22
			//if(StringUtil.isValid(dw))
				//ConnectionHandler.removeDBName(dw);
			//end
		}catch(Exception e) {
			System.err.println(e.getMessage());
		}
		//JSONObject jsonuser = new JSONObject();
		if(mem.containsKey("account")&&StringUtil.isValid(mem.getString("account")))
			putToSession(IAuthorization.SESSION_USER,mem.getString("account"));
		else
			putToSession(IAuthorization.SESSION_USER,mem.getString("id"));
		putToSession(IAuthorization.SESSION_USERINFOMAP,userInfoMap);
		//jsonuser.put(IAuthorization.USER, mem);
		putToSession(IAuthorization.SESSION_USERENTITY_JSON, mem.toJSONString());
	}

	protected void simg(String filePath,OutputStream outstram){
		File f = new File(filePath);
		if(!f.exists()||f.isDirectory())
			return;
		InputStream inp = null;
		ByteArrayOutputStream bf = null;
		byte[] tempByte = null;
		try{
			inp = new FileInputStream(f);
			bf = new ByteArrayOutputStream();
			tempByte = new byte[1024];
			int readIndex = 0;
			while((readIndex=inp.read(tempByte)) != -1){
				bf.write(tempByte,0,readIndex);
			}
			outstram.write(bf.toByteArray());
			bf.flush();
			bf.close();
			inp.close();
		}catch(Exception e){
			e.printStackTrace();
			System.out.println(this.getClass()+" "+e.getMessage());
		}finally{
			try{
				if(null != inp)
					inp.close();
				if(null != bf)
					bf.close();

			}catch(Exception e){}
		}
	}
	@Override
	public IGeneralService<?> getGService() {
		return gservice;
	}
	/**
	 * 通用编号检测
	 * @author youg 2016-07-24 17:25
	 * @param cname
	 * @param cvalue
	 * @return
	 * @throws Exception
	 */
	protected boolean chechNORepeat(String cname,String cvalue) throws Exception{
		Param p = Param.getInstance();
		Object idv = refutil.getFieldValue(this.getModel(),refutil.getField(this.getModel().getClass(),"id"));
		if(null != idv)
			p.addParam("id","!=",idv);
		p.addParam(cname,cvalue);
		int c = this.gservice.findCount(this.getModel().getClass(), p);
		if(c<=0)
			return false;
		return true;
	}
	class uBeanUtilsBean extends BeanUtilsBean{
		public uBeanUtilsBean(CusDateTimeConverter covertor) {
			super(covertor);
			ConvertUtils.register(new LongConverter(null), Date.class);
			ConvertUtils.register(new LongConverter(null), Long.class);
			ConvertUtils.register(new ShortConverter(null), Short.class);
			ConvertUtils.register(new IntegerConverter(null), Integer.class);
			ConvertUtils.register(new DoubleConverter(null), Double.class);
			ConvertUtils.register(new BigDecimalConverter(null), BigDecimal.class);
		}

		@Override
		public void copyProperties(Object arg0, Object arg1) throws IllegalAccessException, InvocationTargetException {
			super.copyProperties(arg0, arg1);
		}

		public void populate(Object bean, Map properties) throws IllegalAccessException, InvocationTargetException {
			if ((bean == null) || (properties == null)) {  return; }
			if (log.isDebugEnabled()) {  log.debug("BeanUtils.populate(" + bean + ", " + properties + ")");}
			Iterator<?> entries = properties.entrySet().iterator();
			while (entries.hasNext()) {
				Entry<String,?> entry = (Entry)entries.next();
				String name = (String) entry.getKey();
				Object v = entry.getValue();
				if(null == v||!StringUtil.isValid(v.toString())) {
					continue;
				}
				if (name == null||name.endsWith("[]")) { continue; }
				Field f = refutil.getField(bean.getClass(), name);
				if(null != f&&f.getType().getSimpleName().equals("Blob")){
					try { Blob b = new SerialBlob(entry.getValue().toString().getBytes("utf-8")); refutil.setFieldValues(bean, f, b);}catch (Exception e) { e.printStackTrace();}
					continue;
				}
				//System.out.println(name+" : "+v);
				//处理界面传来是时：分格式，但实体里保存的是整数. youg 2018-01-17 22:06
				if(v !=null&&v instanceof String[]) {
					String[] vs = ((String[])v);
					if(vs.length>0) {
						if(vs.length==1&&!StringUtil.isValid(vs[0]))
							v = null;
						else {
							String vv = ((String[])v)[0];
							if(null != vv&&vv.indexOf(":") != -1&&null != f&&(f.getType()==Integer.class||f.getType().getSimpleName().equals("int"))) {
								v = new Integer[] {proHMinfo(vv)};
							}
						}
					}else
						v = null;
				}
				if(null == v)
					continue;
				//end
				setProperty(bean, name, v);
			}
		}
	}
	public int proHMinfo(String hmstr){
		if(StringUtil.isValid(hmstr) && hmstr.indexOf(":") != -1){
			String[] hm = hmstr.split(":");
			return Integer.parseInt(hm[0])*60+Integer.parseInt(hm[1]);
		}
		return 0;
	}
	/**
	 * 通用图片显示
	 * @author yonglu
	 * @time 2016-09-20 12:26
	 * @throws IOException
	 */
	public String showimg(String filepath) throws IOException {
		try {
			File f = new File(filepath);
			System.out.println("输出头像图片地址："+f.getAbsolutePath()+"  headimg:"+filepath);
			if(!f.exists()||f.isDirectory()){
				this.redirect("/FIX/pbr/img/defheadimg.png");
				return "imge";
			}
			InputStream inp = new FileInputStream(f);
			ByteArrayOutputStream bf = new ByteArrayOutputStream();
			byte[] tempByte = new byte[1024];
			int readIndex = 0;
			while((readIndex=inp.read(tempByte)) != -1){
				bf.write(tempByte,0,readIndex);
			}
			resp.getOutputStream().write(bf.toByteArray());
			bf.flush();
			bf.close();
			inp.close();
			//resp.setStatus(HttpServletResponse.SC_OK);
			resp.flushBuffer();
			return "imge";
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	/**
	 * 通用base64编码图片显示
	 * @author yonglu
	 * @time 2018-09-11 14:46
	 * @throws IOException
	 */
	public String showimg(String base64code,String imgType) throws IOException {
		try {
			if(!StringUtil.isValid(base64code))
				return null;
			if(base64code.indexOf("base64,") != -1)
				base64code = base64code.substring(base64code.indexOf("base64,")+7);
			if(!StringUtil.isValid(imgType))
				imgType = "jpg";
			this.getResp().setContentType("image/jpeg");
			this.getResp().setDateHeader("expries", -1);
			this.getResp().setHeader("Cache-Control", "no-cache");
			this.getResp().setHeader("Pragma", "no-cache");
			byte[] bytes1 = Base64.decode(base64code);
			ByteArrayInputStream bais = new ByteArrayInputStream(bytes1);
			BufferedImage bi1 = ImageIO.read(bais);
			ImageIO.write(bi1, imgType, this.getResp().getOutputStream());
			return "imge";
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	/**
	 * 默认排序字性设置
	 * @param p
	 */
	protected Param defaultSortParam(Param p){
		String sortField = this.getParameter("sortField");
		if (StringUtil.isValid(sortField)) {
			try{
				Field f = ReflectUtil.getInstance().getField(this.getModel().getClass(),sortField);
				if(null == f)
					throw new Exception(this.getModel().getClass().getName()+" no field "+sortField);
				EXColumn ex = AnnotationParser.getExtColumn(f);
				if(null != ex&&StringUtil.isValid(ex.sortName())){
					sortField = ex.sortName();
				}
				String sortType = this.getParameter("sortType");
				p.addParam(sortField, "order", sortType);
			}catch(Exception e){e.printStackTrace();}
		}else {
			//获得表格设置的排序属
			Param pp = Param.getInstance();
			List<Map<String, Object>> sortfields = fetchEntitySetInfo(pp);
			if(null != sortfields&&!sortfields.isEmpty()) {
				UMap um = null;
				for(Map<String,Object> m:sortfields) {
					um = (UMap) m;
					String sortName = um.getString("sortName");
					String orderType = um.getString("orderType");
					if(!StringUtil.isValid(sortName)) {
						sortName = um.getString("fieldName");
					}
					if(!StringUtil.isValid(orderType)) {
						orderType = "asc";
					}
					p.addParam(sortName, "order",orderType);
				}
				return p;
			}
		}
		if(!p.hasCondition("order",""))
			p.addParam(this.getModel().getClass().getSimpleName()+"_.createDate", "order", "desc");

		return p;
	}
	protected List<Map<String, Object>> fetchEntitySetInfo(Param p)  {
		try {
			Class<?> eclas = getModel().getClass();
			String ename = eclas.getName();
			p.addParam("createrId", this.getCurrentLoginUserId());
			p.addParam("entityClass", ename);
			int c = gservice.findCount("EntitySetEntity", p);
			if(c>0){
				StringBuilder sb = new StringBuilder();
				p.clear();
				p.addParam("showIndex", "order","asc");
				PageIterator<?> ml = gservice.hisql("select id,showIndex from EntitySetSolution where entityclass='"+ename+"'  and createrid='"+getCurrentLoginUserId()+"' ", p,0,1);
				p.clear();

				sb.append("select fieldName,sortName,orderType,showIndex from EntitySetEntity where orderType is not null ")
						.append(" and createrId='").append(getCurrentLoginUserId()).append("' ")
						.append(" and entityClass='").append(ename).append("' ");
				if(null != ml&&null != ml.getItems()&&!ml.getItems().isEmpty()) {
					sb.append(" and solutionId='" + (((UMap)ml.getItems().get(0)).getString("id")) + "' ");
				}
				p.addParam("showIndex", "order","asc");
				List<Map<String, Object>>  eses = gservice.hisql(sb.toString(), p);
				return eses;
			}
		}catch(Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	protected String beforDisableEnable(int estate,String id) {return null;}
	protected String afterDisableEnable(int estate,String id) {return null;}
	/**
	 * 通用禁用与启用
	 * @author youg 2016-10-19 16:05
	 * @return
	 * @throws Exception
	 */
	public String disableEnable(String...needWriteResult) throws Exception {
		JSONObject json = new JSONObject();
		//判断权限
		String pid = fetchUpdatePID();
		if(!pid.endsWith(PwoerEM.UPDATE.getDescription())&&!pid.endsWith(PwoerEM.UPDATE.name()))
			pid+="."+PwoerEM.UPDATE.getDescription();
		if (!hasPermission(pid,false)){
			log.error("您没有该操作的权限!");
			json.put(IAction.STATECODE,"对不起，你没有该操作的权限！");
			this.writeResult(json.toJSONString());
			return JSON;
		}
		String proType = this.getParameter("proType");
		String[] ids = this.getParams("ids[]");
		if(null == ids||ids.length<=0){
			BaseEntity b = this.getModel();
			if(StringUtil.isValid(b.getId())){
				ids = b.getId().split(",");
				if(null != ids&&ids.length==1)
					b.setId(ids[0]);
			}else{
				json.put(IAction.STATECODE,StateCode.NODATA);
				if(null == needWriteResult||needWriteResult.length<=0||(needWriteResult.length>0&&"1".equals(needWriteResult[0])))
					this.writeResult(json.toString());
				else
					return StateCode.NODATA;
				return JSON;
			}
		}
		StringBuilder emsg = new StringBuilder();
		for(String id:ids){
			BaseEntity b = this.getModel();
			Map<String,Object> m = new HashMap<String,Object>();
			int estate = BaseEntity.ENABLE;
			if ("1".equals(proType)) {
				/*启用*/
				if(null != b)
					b.setEstate(BaseEntity.ENABLE);
				m.put("estate",BaseEntity.ENABLE);
				m.put("disableReason","");
				m.put("disableDate",null);
			} else {
				/*停用 以上检查通过后，可正常进行停用*/
				if(null != b) {
					b.setEstate(BaseEntity.DISABLED);
					b.setDisableDate(new Date());
				}
				estate = BaseEntity.DISABLED;
				m.put("disableDate", new Date());
				m.put("estate",BaseEntity.DISABLED);
				m.put("disableReason",this.getParameter("disableReason"));
			}
			String msg = beforDisableEnable(estate,id);
			if(null != msg) {
				emsg.append(msg);
				continue;
			}
			Param p = Param.getInstance();
			p.addParam("id", id);
			this.gservice.updatePropertie(this.getModel().getClass(),m, p,false);//(model);
			afterDisableEnable(estate,id);
		}
		if(emsg.length()>0)
			json.put(IAction.STATECODE, emsg);
		else
			json.put(IAction.STATECODE, StateCode.SUCCESS);
		if(null == needWriteResult||needWriteResult.length<=0||(needWriteResult.length>0&&"1".equals(needWriteResult[0])))
			this.writeResult(json.toString());
		else
			return StateCode.SUCCESS;
		return JSON;
	}
	public Param getLasterFindParam(){
		return getLasterFindParam(getModel().getClass().getName());
	}
	public Param getLasterFindParam(String entityFullName){
		Object o = getFromSession(BaseEntity.fetchParamFindKey(entityFullName));
		if(o == null)
			return null;
		if(o instanceof Param)
			return ((Param)o);
		return null;
	}
	private static final String AJAX_ACCEPT_CONTENT_TYPE = "text/html;type=ajax";
	private static final String AJAX_SOURCE_PARAM = "ajaxSource";
	protected boolean isAjaxRequestInternal(HttpServletRequest request, HttpServletResponse response) {
		String acceptHeader = request.getHeader("Accept");
		String ajaxParam = request.getParameter(AJAX_SOURCE_PARAM);
		if (AJAX_ACCEPT_CONTENT_TYPE.equals(acceptHeader) || StringUtil.isValid(ajaxParam)) {
			return true;
		} else {
			return isAjaxRequest(request);
		}
	}
	protected boolean isAjaxRequest(HttpServletRequest request){
		String header = request.getHeader("X-Requested-With");
		return  ("XMLHttpRequest".equals(header))?true:false;
	}
	/**
	 * 获得审核完整的功能id，注意，这是功能id，不是modelid
	 * @return
	 */
	public String fetchAuditPID(){
		return "fetchAuditPID:未设置功能验证ID，请设置";
	}
	public String fetchCreatePID(){
		return "fetchCreatePID:未设置功能验证ID，请设置";
	}
	public String fetchDelPID(){
		return "fetchDelPID:未设置功能验证ID，请设置";
	}
	public String fetchUpdatePID(){
		return "fetchUpdatePID:未设置功能验证ID，请设置";
	}
	public String fetchfindPID(){
		return this.getClass().getName()+".fetchfindPID:未设置功能验证ID，请设置";
	}
	public Param fetchParam() {
		//因为考虑以前没有另一个有参的方法，所在固定不识别参数类型。
		return fetchParam(-1);
	}
	/**
	 * @param paramType:1:查询，2：创建，3：修改，4：删除
	 * @return
	 * @throws Exception
	 */
	public Param fetchParam(int paramType)  {
		return null;
	}
	/**
	 * 获得禁用/启用功能ID，注意，这是功能全id，不是modelid。
	 * @return
	 */
	public String fetchEnableDisablePID(){
		return "fetchEnableDisablePID:未设置功能验证ID，请设置";
	}
	public String getCurrentUserToEmp() throws Exception {
		Object o = this.getFromSession("wxUserData");
		if(null != o&&o instanceof JSONObject) {
			JSONObject  jemp = (JSONObject)o;
			if(jemp.containsKey("employeeId")&&StringUtil.isValid(jemp.getString("employeeId")))
				return jemp.getString("employeeId");
		}
		if(null == this.gservice) {
			this.log.error("获得当前登录人员对应的EMPLOYEE时，SERVICE为空，无法处理");
			return null;
		}
		String mobile = this.getCurrentLoginUserEntity().getMobile();
		if(!StringUtil.isValid(mobile)) {
			this.log.debug("根据当前登录人员获得EMPLOYEE时，当前人员的MOBILE为空或不可用。");
			return null;
		}
		Param p = Param.getInstance();
		p.addParam("mobile", mobile);
		p.addParam("dataOwner",this.getCurrentLoginUserEntity().getDataOwner());
		List<Map<String,Object>> mls = this.gservice.getDao().hisql("select id from EmployeeEntity EmployeeEntity where mobile='"+mobile+"'", p);
		if(null == mls||mls.isEmpty())
			return null;
		return mls.get(0).get("id").toString();
	}
	public JSONObject getCurrentUserENTName() throws Exception {
		if(null == this.gservice) {
			this.log.error("获得当前登录人员对应的企业时，SERVICE为空，无法处理");
			return null;
		}
		String dw = this.getCurrentLoginUserEntity().getDataOwner();
		if(!StringUtil.isValid(dw)) {
			this.log.debug("根据当前登录人员获得企业号时，当前人员的企业标识为空或不可用。");
			return null;
		}

		Param p = Param.getInstance();
		p.addParam("id", dw);
		List<Map<String,Object>> mls = this.gservice.hisql("select name,ecode from GroupCompanyEntity where id='"+dw+"'", p,false);
		if(null == mls||mls.isEmpty())
			return null;
		UMap um = (UMap) mls.get(0);
		JSONObject j = new JSONObject();
		j.put("id", dw);
		j.put("name", um.getString("name"));
		j.put("ecode",um.getString("ecode"));
		return j;
	}

	public String auditform() throws Exception {
		this.writeResult("此模板没有配置表单显示视图。");
		return SUCCESS;
	}
	protected String getModelNameFiledName() {
		return  "name";
	}
	protected String validate() {
		return validate(0);
	}
	/**
	 * 统一数据验证方法，0：不指定操作，1：创建，2：修改，3：删除,4:查询
	 * @param proType
	 * @return
	 */
	protected String validate(int proType) {
		Object o = this.getModel();
		if(!(o instanceof BaseEntity)) {
			return null;
		}
		String msg = null;
		Param p = Param.getInstance();
		BaseEntity b = (BaseEntity)o;
		try {
			Object v = null;
			String name = getModelNameFiledName();
			//如果为空则约定为不检查名称重复性
			if(StringUtil.isValid(name)) {
				v = refutil.gettFieldReadMethodValue(b, name);
				if(null != v){
					if(StringUtil.isValid(b.getId()))
						p.addParam("id","!=", b.getId());
					p.addParam(name, v.toString().trim());
					int c = gservice.findCount(b.getClass(), p);
					if(c > 0)
						return "名称重复，请更换";
				}
			}
			//检查统一编码
			v = refutil.gettFieldReadMethodValue(b, "ecode");
			if(null != v) {
				p.clear();
				if(StringUtil.isValid(b.getId()))
					p.addParam("id","!=", b.getId());
				p.addParam("ecode", v.toString().trim());
				int c = gservice.findCount(b.getClass(), p);
				if(c > 0)
					return "编码重复，请更换";
			}
			//统一唯一属性校验。2018-07-25 21:19.youg
			if(proType == UPDATE||proType==CREATE) {
				p.clear();
				ExtendedField[] exfs = refutil.getAllEXPField(b.getClass());
				if(null != exfs&&exfs.length>0) {
					for(ExtendedField e:exfs) {
						if(e.exc != null&& EXColumn.UNIQUE == e.exc.serverValidateType()) {
							v = refutil.getFieldValue(b, e.field);
							if(null == v)
								continue;
							p.clear();
							if(StringUtil.isValid(b.getId()))
								p.addParam("id","!=", b.getId());
							p.addParam(e.columnName, v.toString().trim());
							int c = gservice.findCount(b.getClass(), p);
							if(c > 0)
								return e.getTitle()+"重复，请更换";
						}
					}
				}
			}
			//end
		} catch (Exception e) {
			e.printStackTrace();
			msg = "验证发生错误!"+e.getMessage();
		}
		return msg;
	}
	@Override
	public void destroy() {
		super.destroy();
		//称除保存用户的线程安全变量中保存此线和的数据
		AbstractBaseAuthorization.destory();
		//解除实体的ORM注册
		ORMControler ormc = ORMControler.getInstance(null);
		try {
			Object o = this.getModel();
			if(null != o)
				ormc.unRegeditEntity(o.getClass());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
	}
	protected String exitSys(String loginPath) throws ServletException, IOException{
		Param p = Param.getInstance();
		p.addParam("id",this.getCurrentLoginUserId());
		try{if(null != gservice) {this.gservice.hisql("update BaseUserEntity set lastLogoutTime='"+DateUtil.dateToString(new Date(),"yyyy-MM-dd HH:mm:ss")+"' where id='"+this.getCurrentLoginUserId()+"' ", p);}}catch(Exception e){e.printStackTrace();}
		HttpSession s = getReq().getSession();
		s.removeAttribute(IAuthorization.SESSION_USER);
		s.removeAttribute(IAuthorization.SESSION_USERENTITY_JSON);
		s.removeAttribute(IAuthorization.SESSION_USERINFOMAP);
		s.invalidate();
		String exitPage = getReq().getParameter("exitPage");
		if(StringUtil.isValid(exitPage)){
			if(!exitPage.startsWith("/")){
				this.forward("/tell_cracker.html");
				return SUCCESS;
			}
			this.redirect(exitPage);
		}else
			redirect(loginPath);
		IPowerAuthorization uauthori = (IPowerAuthorization) KernelActivitor.getService(IPowerAuthorization.class.getName());
		try {
			Cookie c = CookieUtil.getCookie(req, CookieUtil.COOKIE_NAME);
			if(null != c)
				uauthori.removeToken(c.getValue());
		} catch (Exception e) {
			e.printStackTrace();
		}finally{
			AbstractBaseAuthorization.removeReq();
		}
		return SUCCESS;
	}
	protected String beforCopy(String id) {return null;}
	protected String afterCopy(String fromId,String newId) {return null;}
	/**
	 * 通用复制功能
	 * @author youglu 2018-09-15 10:39
	 * @return JSON
	 * @throws Exception
	 */
	public String copyme() throws Exception {
		JSONObject j = new JSONObject();
		//判断权限,复制使用创建权限
		String pid = fetchCreatePID();
		if(!pid.endsWith(PwoerEM.CREATE.getDescription())&&!pid.endsWith(PwoerEM.CREATE.name()))
			pid+="."+PwoerEM.CREATE.getDescription();
		if (!hasPermission(pid,false)){
			log.error("您没有该操作的权限!");
			j.put(IAction.STATECODE,"对不起，你没有该操作的权限！");
			this.writeResult(j.toJSONString());
			return JSON;
		}
		BaseEntity b = this.getModel();
		String[] ids = this.getParams("ids");
		if(null == ids||ids.length<=0){
			if(StringUtil.isValid(b.getId())){
				ids = b.getId().split(",");
				if(null != ids&&ids.length==1)
					b.setId(ids[0]);
			}
		}
		if(null == ids||ids.length<=0) {
			j.put(IAction.STATECODE,StateCode.NODATA);
			this.writeResult(j.toString());
			return JSON;
		}
		String newId = "";
		for(String id:ids){
			if(!StringUtil.isValid(id))
				continue;
			String msg = beforCopy(id);
			if(StringUtil.isValid(msg)) {
				j.put(IAction.STATECODE,msg);
				this.writeResult(j.toString());
				return JSON;
			}
			BaseEntity nb = this.gservice.findEntityById(b.getClass(), id);
			if(nb == null)
				continue;

			nb.setId(null);
			nb.setRemark("复制源ID:"+id);
			nb.setEstate(BaseEntity.ENABLE);
			nb.setCreateDate(new Date());
			nb.setCreateMan(null);
			nb.setCreaterId(this.getCurrentLoginUserId());
			nb.setEcode(this.gservice.fetchNo(nb.getPreFix(),nb.getClass()));
			this.gservice.createEntity(nb);

			newId = nb.getId();
			msg = afterCopy(id,nb.getId());
			if(StringUtil.isValid(msg)) {
				j.put(IAction.STATECODE,msg);
				this.writeResult(j.toString());
				return JSON;
			}
		}
		j.put("newid",newId);
		j.put(IAction.STATECODE,StateCode.SUCCESS);
		this.writeResult(j.toString());
		return JSON;
	}
	/**
	 * 修改企业DB时，需要调用此方法来实时更新企业所属DB
	 * @author youglu 2018-06-19 10:02
	 * @param cmpanyId
	 * @throws Exception
	 */
	protected final void changeEntDB(String cmpanyId) throws Exception{
		//获得当前的登录用户信息
		BaseUserEntity user = this.getCurrentLoginUserEntity();
		if(null == user){
			log.error("获得用户为空");
			return;
		}
		//如果是超管，则直接通过
		if(null == user.getIsAdmin()||!user.getIsAdmin()||user.getUserType() != BaseUserEntity.SYSUSER)
			return;
		//FIXME 这个地方需要考虑安全问题，不能随便更改.
		//ConnectionHandler.removeDBName(cmpanyId);
	}
	protected List<Map<String,Object>> fetchAllModules(){
		//获得所有的模块
		Param p = Param.getInstance();
		p.addParam("title", "order","desc");
		try {
			List<Map<String,Object>> mls = this.gservice.hisql("select id,unitid,title,entityClass from Unit where webEnterUrl is not null and estate =0 and entityClass is not null and entityClass !='' ", p,false);
			return mls;
		}catch(Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	public String startBatchWorkFlow() throws Exception{
		JSONObject j = new JSONObject();
		String[] ids = this.getParams("ids[]");
		if(null == ids||ids.length<=0){
			ids = this.getParams("ids");
		}
		if(null == ids||ids.length<=0){
			String idstrs = this.getParameter("id");
			if(StringUtil.isValid(idstrs)){
				if(idstrs.indexOf(",") != -1)
					ids = idstrs.split(",");
				else
					ids = new String[]{idstrs};
			}
			if(null == ids||ids.length<=0){
				j.put(IAction.STATECODE,StateCode.NODATA);
				this.writeResult(j.toString());
				return JSON;
			}
		}
		//生成一个批号
		String batchNo = UUID.randomUUID().toString();//"p_"+new Date().getTime();
		String entityName = this.getModel().getClass().getSimpleName();
		Param p = Param.getInstance();
		for(String id:ids){
			if(!StringUtil.isValid(id))
				continue;
			p.clear();
			p.addParam("id", id);
			p.addParam(BaseEntity.ESTATE,BaseEntity.ENABLE);
			this.getGService().hisql("update "+entityName+" set "+BaseEntity.BATCHNO+" ='"+batchNo+"' where id='"+id+"' ", p);
		}
		p.clear();
		p.addParam("id", ids[0]);
		String msg = this.getGService().doWorkFlow(this.getGService().findEntitys(this.getModel().getClass(), p), 3);//在SERVICE中3为update
		System.out.println(this.getClass().getName()+"  批量启动工作流结果:"+msg);
		if(null == msg||msg.equals("0"))
			j.put(IAction.STATECODE, StateCode.SUCCESS);
		else
			j.put(IAction.STATECODE, msg);
		this.writeResult(j.toString());
		return JSON;
	}
	public static <S extends BaseServlet> S initme(Class<S> ac,IGeneralService<?> gservice,WebAppContext webappcontext) throws InstantiationException, IllegalAccessException{
		return initme(ac,gservice,webappcontext,null,null);
	}
	public static <S extends BaseServlet> S initme(Class<S> ac,IGeneralService<?> gservice,WebAppContext webappcontext,String mpath,String filePath) throws InstantiationException, IllegalAccessException{
		S aci = ac.newInstance();
		if(null == filePath)
			filePath = "/";
		if(!StringUtil.isValid(mpath))
			mpath = BaseEntity.fethPath(aci.getModel().getClass());
		aci.setService(gservice);

		ServletHolder acholder = new ServletHolder(new MultipleInstanceServlet(aci));
		// 设置上传文件用时的配置，第一个参数是保存路径，第二个与第三个是文件大小设置，第四个未知。
		acholder.getRegistration().setMultipartConfig(new MultipartConfigElement(filePath, 104857600, 104857600, 262144));
		webappcontext.addServlet(acholder, "/" + mpath);
		return aci;
	}
	public void setService(IGeneralService<?> gservice) {
		this.gservice = gservice;
	}
}