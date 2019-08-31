package org.chuniter.core.kernel.model;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Blob;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.persistence.MappedSuperclass;
import javax.sql.rowset.serial.SerialBlob;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.chuniter.core.kernel.KernelActivitor;
import org.chuniter.core.kernel.annotation.EXColumn;
import org.chuniter.core.kernel.annotation.EXIDColumn;
import org.chuniter.core.kernel.annotation.ReflectField;
import org.chuniter.core.kernel.api.IGeneralService;
import org.chuniter.core.kernel.api.cache.IUCache;
import org.chuniter.core.kernel.api.exception.CannotEditException;
import org.chuniter.core.kernel.impl.orm.EntityMapping;
import org.chuniter.core.kernel.impl.orm.ExtendedField;
import org.chuniter.core.kernel.kernelunit.AnnotationParser;
import org.chuniter.core.kernel.kernelunit.DateUtil;
import org.chuniter.core.kernel.kernelunit.ReflectUtil;
import org.chuniter.core.kernel.kernelunit.StringUtil;


/**
 * 实体类基类
 * @author Administrator
 */
@MappedSuperclass
public class BaseEntity extends Extendable implements Serializable{
	public final transient Log log = LogFactory.getLog(getClass());
	public transient String PN = "";
	private transient static ThreadLocal<String> tname = new ThreadLocal<String>();
	private final transient ReflectUtil refu = ReflectUtil.getInstance();
	private transient IUCache ucache = null;
	public BaseEntity(){ }
	/**
	 *
	 */
	private transient static final long serialVersionUID = -1170890658075845290L;
	/**
	 * 创建人
	 */
	@EXColumn(title="创建人",showtable=true,showedit=false,searchMust=true,impable=false)
	protected String createMan;
	/**
	 * 创建日期
	 */
	@EXColumn(title="创建日期",showtable=true,showedit=false,searchMust=true,impable=false)
	protected Date createDate;
	/**
	 * 最后修改人
	 */
	@EXColumn(title="最后修改人",showtable=true,showedit=false,searchMust=true,impable=false)
	protected String lastModifyMan;
	protected String lasterModifyerId;
	/**
	 * 最后修改日期
	 */
	@EXColumn(title="最后修日期",showtable=true,showedit=false,searchMust=true,impable=false,dateFormat="yyyy-MM-dd HH:mm:ss")
	protected Date lastModifyDate;
	/**
	 * 最后所进行的操作
	 */
	protected String lastOperation;

	/**优化APP标识*/
	public transient int optimizApp;

	/**实体状态*/
	@EXColumn(title="状态",showtable=true,showedit=false,searchMust=true,impable=false)
	protected Integer estate = ENABLE;

	/**创建者id*/
	protected String createrId;

	/**
	 * 实体编码
	 */
	@EXColumn(title="编码",showedit=false,searchMust=true,impable=false,serverValidateType=EXColumn.UNIQUE)
	protected String ecode;

	/** 停用日期 */
	protected Date disableDate;

	/** 停用原因  */
	protected String disableReason;
	/**
	 * 申请日期
	 */
	@EXColumn(title = "审核日期",showedit=false,impable=false)
	protected Date checkDate;
	/**
	 * 审核人
	 */
	@EXColumn(title = "审核人",showedit=false,impable=false)
	protected String checkMan;

	@EXColumn(title = "备注",length=2000,showtable=false,utype = "text")
	protected String remark;

	/**数据所有者*/
	@EXColumn(title="企业号",showtable=false,showedit=false,searchMust=true,impable=false)
	protected String dataOwner;

	/**数据内部编号，用于获得最大值的序号*/
	@EXColumn(showtable=false,showedit=false,searchMust=true,impable=false)
	@EXIDColumn(autoCreate=true)
	protected Long meIndex;

	//前缀,用于编码前缀使用，每个实体最好固定设置
	protected String preFix = "E";



	public static final transient String DATAOWNER = "dataOwner";
	public static final transient String DEFDB = "def";
	public static final transient String MEINDEX = "meIndex";
	public static final transient String ECODE = "ecode";
	public static final transient String ESTATE = "estate";
	public static final transient String BATCHNO = "batchNo";
	public static final transient int  DEFNOLENGTH = 8;

	/**停用或无效*/
	public transient final static Integer DISABLED = -100; private transient final int DISABLED_=-100;//停用/无效
	/**启用或有效*/
	public transient final static Integer ENABLE = 0;private transient final int ENABLE_=0;//启用/有效
	/**未审核*/
	public transient final static Integer NOREVIEW = 1;private transient final int NOREVIEW_=1;//未审核/等待审批
	/**审核中*/
	public transient final static Integer REVIEWING = 2;private transient final int REVIEWING_=2;//审核中
	/**通过审核*/
	public transient final static Integer PASSREVIEW = 3;private transient final int PASSREVIEW_=3;//通过审核
	/**审核不通过*/
	public transient final static Integer REVIEWREJECT = 4;private transient final int REVIEWREJECT_=4;//审核不通过
	/**未确认*/
	public transient final static Integer NOCONFIRM = 5;private transient final int NOCONFIRM_=5;//未确认
	/**已确认*/
	public transient final static Integer CONFIRMED = 6;private transient final int CONFIRMED_=6;//已确认
	/**已锁定*/
	public transient final static Integer LOCKED = 7;private transient final int LOCKED_=7;//已锁定，锁定的数据不允许任何操作，除了查看。

	public transient final static String CANEDIT = "canEdit";
	public transient final static String CANCREATE = "canCreate";
	public transient final static String CANDEL = "canDel";
	public transient final static String NOPERATION = "noperation";
	public transient final static String CANBATCHCREATE = "canBatchCreate";
	public transient final static String CANWORKFLOWCREATE = "canWorkFlowCreate";

	/***************ecode预设值****************/
	public transient final static String INBUILT = "0x000001";//内置

	//是否锁定，锁定不能做任何操作
	protected Integer ulocked;

	/**全拼*/
	private String fullpinying;

	/**批号*/
	@EXColumn(length=120,title="批号",showedit=false)
	private String batchNo;

	public String getPreFix() {
		return preFix;
	}
	public void setPreFix(String preFix) {
	}
	public Long getMeIndex() {
		return meIndex;
	}
	public void setMeIndex(Long meIndex) {
		this.meIndex = meIndex;
	}
	public Integer getUlocked() {
		return ulocked;
	}
	public void setUlocked(Integer ulocked) {
		this.ulocked = ulocked;
	}
	public String getDataOwner() {
		return dataOwner;
	}
	public void setDataOwner(String dataOwner) {
		this.dataOwner = dataOwner;
	}
	public Date getCheckDate() {
		return checkDate;
	}
	public void setCheckDate(Date checkDate) {
		this.checkDate = checkDate;
	}
	public String getCheckMan() {
		return checkMan;
	}
	public void setCheckMan(String checkMan) {
		this.checkMan = checkMan;
	}
	public String getRemark() {
		return remark;
	}
	public void setRemark(String remark) {
		this.remark = remark;
	}
	public String getLastModifyMan() {
		if(optimizApp>0)
			return null;
		return lastModifyMan;
	}
	public void setLastModifyMan(String lastModifyMan) {
		this.lastModifyMan = lastModifyMan;
	}
	public Date getLastModifyDate() {
		//if(optimizApp>0)
		//return null;
		return lastModifyDate;
	}
	public void setLastModifyDate(Date lastModifyDate) {
		this.lastModifyDate = lastModifyDate;
	}
	public String getCreateMan() {
		if(optimizApp>0)
			return null;
		return createMan;
	}
	public void setCreateMan(String createMan) {
		this.createMan = createMan;
	}
	public Date getCreateDate() {
		if(optimizApp>0)
			return null;
		return createDate;
	}
	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}
	@Override
	public String retrieveWhereClause() {
		return " 1=1 ";
	}
	public String getLastOperation() {
		if(optimizApp>0)
			return null;
		return lastOperation;
	}
	public void setLastOperation(String lastOperation) {
		this.lastOperation = lastOperation;
	}
	public static String ftname(){
		return tname.get();
	}
	public static void ptname(String tnamev){
		tname.set(tnamev);
	}
	public static void resetTname(){
		tname.set(null);
	}
	public String getCreateDateStr(){
		if(optimizApp>0)
			return null;
		if(null == createDate)
			return "";
		return DateUtil.dateToString(createDate, "yyyy-MM-dd HH:mm:ss");
	}
	public String getCreateDateStr(String format){
		if(optimizApp>0)
			return null;
		if(null == createDate)
			return "";
		if(!StringUtil.isValid(format))
			format = "yyyy-MM-dd";
		return DateUtil.dateToString(createDate, "yyyy-MM-dd HH:mm:ss");
	}
	public String getDateStr(String d){
		if(!StringUtil.isValid(d))
			return "";
		String dateStr = DateUtil.dateToString(DateUtil.stringToDate(d), "yyyy-MM-dd");
		return dateStr;
	}
	public String getDateStrd(Date d){
		if(null == d)
			return "";
		return DateUtil.dateToString(d);//, "yyyy-MM-dd HH:mm:ss");
	}
	public String getDateStrd(Date d,String f){
		if(null == d)
			return "";
		if(!StringUtil.isValid(f))
			f = "yyyy-MM-dd";
		return DateUtil.dateToString(d, f);
	}
	public static String proDouble(Object d,int znos){
		if(null == d||!StringUtil.isValid(d.toString()))
			return "";
		Double dd = null;
		if(d instanceof Double)
			dd = (Double)d;
		else
			dd = Double.valueOf(d.toString());
		if(null == dd||dd<=0)
			return "0";
		String f = "#.00";
		if(znos>2) {
			for(int i=0;i<znos;i++) {
				f+="0";
			}
		}
		DecimalFormat df = new DecimalFormat(f);
		return df.format(d);
	}
	//由于破cxf远程服务对每个有get的方法必须有一个set对应，故在此声明个无用的（可能是对这个东西还不是很了解）。
	public void setCreateDateStr(){
	}
	public String getLastModifyDateStr(){
		//if(optimizApp>0)
		//return null;
		if(null == lastModifyDate)
			return "";
		return DateUtil.dateToString(this.lastModifyDate, "yyyy-MM-dd HH:mm:ss");
	}
	public String getCreaterId() {
		return createrId;
	}
	public void setCreaterId(String createrId) {
		this.createrId = createrId;
	}
	public Integer getEstate() {
		return estate;
	}
	public void setEstate(Integer estate) {
		this.estate = estate;
	}
	public static String fethPath(Class<?> c){
		return AnnotationParser.getEntityPath(c);
	}
	public String getEcode() {
		return ecode;
	}
	public void setEcode(String ecode) {
		this.ecode = ecode;
	}
	public String getPN() {
		if(!StringUtil.isValid(PN))
			PN = AnnotationParser.getEntityTitle(this.getClass());
		return PN;
	}
	public static String fetchTitle(Class<?> c) {
		return AnnotationParser.getEntityTitle(c);
	}
	public Date getDisableDate() {
		return disableDate;
	}
	public void setDisableDate(Date disableDate) {
		this.disableDate = disableDate;
	}
	public String getDisableReason() {
		return disableReason;
	}
	public void setDisableReason(String disableReason) {
		this.disableReason = disableReason;
	}
	public boolean isMeField(String fieldName){
		if(null != refu.getField(this.getClass(), fieldName))
			return true;
		return false;
	}
	protected String getEstateStr(String est) {
		String en = est;
		if(null == est)
			return est;
		int e = Integer.valueOf(est);
		switch(e){
			case  DISABLED_:
				en = "停用/无效";break;
			case  ENABLE_:
				en = "启用/有效";break;
			case  NOREVIEW_:
				en = "待审核";break;
			case  REVIEWING_:
				en = "审核中";break;
			case  PASSREVIEW_:
				en = "通过审核";break;
			case  REVIEWREJECT_:
				en = "审核不通过";break;
			case  NOCONFIRM_:
				en = "未确认";break;
			case  CONFIRMED_:
				en = "已确认";break;
			case  LOCKED_:
				en = "已锁定";break;
		}
		return en;
	}
	protected String getEstateStr() {
		return getEstateStr((estate == null)?null:estate.toString());
	}
	public ExtendedField fetchField(Class<?> c,String fn) {
		if(!StringUtil.isValid(fn))
			return null;
		ExtendedField ef = EntityMapping.getClassCacheInfo(c,fn);
		if(null == ef) {
			ef = refu.getEXTField(c, fn);
			if(null != ef)
				return ef;
			Field nf = refu.getField(c,fn);
			if(null != nf) {
				ef = new ExtendedField();
				ef.field = nf;
				return ef;
			}
		}
		return ef;
	}
	public Method fetchMethod(Class<?> c,String mk) {
		if(!StringUtil.isValid(mk))
			return null;
		Method m = EntityMapping.getClassMethodCache(c, mk);
		if(null == m) {
			try {
				m = refu.getMethod(c, mk);
				if(null == m)
					m = refu.gettFieldReadMethod(c, mk);
				EntityMapping.putClassMethodCache(c,m,mk);
				return m;
			}catch (Exception e) {
				e.printStackTrace();
			}
		}
		return m;
	}
	public Method fetchMethod(Class<?> c,Field f) {
		if(null == f)
			return null;
		String g = f.getName();
		String mk = "get" +g.substring(0, 1).toUpperCase()+ g.substring(1);
		Method m = EntityMapping.getClassMethodCache(c, mk);
		if(null == m) {
			try {
				m = refu.getMethod(c, mk);
				EntityMapping.putClassMethodCache(c,m,mk);
				return m;
			}catch (Exception e) {
				e.printStackTrace();
			}
		}
		return m;
	}
	public static String proOrgNamePath(String orgPath) {
		if(!StringUtil.isValid(orgPath))
			return null;
		if(orgPath.indexOf("|") == -1)
			return null;
		if(orgPath.split("|").length<3)
			return orgPath;
		orgPath = orgPath.substring(orgPath.indexOf("|")+1,orgPath.length());
		return orgPath.replaceAll("\\|", ">");
	}
	/**
	 * 注意，这里只用于显示用
	 * @param fn
	 * @return
	 */
	public Object fieldNameValue(String fn){
		Class<?> c = this.getClass();
		ExtendedField ef = fetchField(c,fn);
		if(null == ef||ef.field == null)
			return null;
		Object v = null;
		//优先显示设置的显示方法
		if(null != ef.exc&&StringUtil.isValid(ef.exc.showGetMethod())){
			try {
				String mk = ef.exc.showGetMethod();
				Method m = fetchMethod(c, mk);
				if(null != m){
					Class<?>[] ptypes = m.getParameterTypes();
					if(null == ptypes||ptypes.length<=0)
						v = m.invoke(this, new Object[]{});
					else {
						if(ptypes[0] ==  ef.field.getType())
							v = m.invoke(this, new Object[]{refu.getFieldValue(this,  ef.field)});
					}
				}
				if(null != v)
					return v;
			}catch (Exception e) {e.printStackTrace();}
		}
		if(v == null)
			v = refu.getFieldValue(this,ef.field);
		if(v == null){
			try {
				Method m = fetchMethod(c, ef.field);
				if(null != m){v = m.invoke(this, new Object[]{});}
			}catch (Exception e) {e.printStackTrace();}
		}
		//如果是一些可以直接返回的类型就直接返回了
		if(null == v)
			return null;
		String ftype = ef.field.getType().getSimpleName();
		if(v instanceof Date) {
			return DateUtil.dateToString((Date)v,(ef.exc != null?ef.exc.dateFormat():"yyyy-MM-dd"));
		}else if("Long".equals(ftype)||"Float".equals(ftype)||"Double".equals(ftype)) {
			return v;
		}else if("Byte".equals(ftype)) {
			return this.getFieldStr(ef.field,v);
		}else if("Blob".equals(ftype)) {
			return this.getB64imgcodeStr(fn);
		}
		String utype = ef.getType();
		if(StringUtil.isValid(utype)&&"list".equals(utype)) {
			String[] vlis = ef.exc.vlis();
			if(null != vlis&&vlis.length>1) {
				for(String v_:vlis){
					if(v_.indexOf(v.toString()) != -1&&v_.indexOf(":") != -1)
						return v_.split(":")[1];
				}
			}
		}
		//检查缓存是否有数据,由于这些key是在hr基本单元定义，所以引用不到相应的常量.
		EXColumn exc = ef.exc;
		if(this.ucache == null)
			ucache = (IUCache) KernelActivitor.getService(IUCache.class.getName());
		ReflectField rf = AnnotationParser.parseReflectField(ef.field);
		if(null != ucache&&null != exc){
			boolean needfromCache = false;
			//FIXME 获得属性所属的类名
			//由于有些属性用的MODELDATA，但又没有设置相关联类的类属性，所以这里对modeldata进行判断。比如人事中的职位，只有类的引用，却没有类属性，所以只能从缓存取，不然就无法获得名称了。youg 2018-08-01 11:36
			if(utype.equals("hrbasecode")||utype.equals("orgdata")||
					utype.equals("companydata")||
					(utype.equals("modeldata")&&(rf==null||"".equals(rf.sourceFiledName())))||
					utype.equals("postdata")) {
				//end
				needfromCache = true;
			}
			if(needfromCache) {
				ucache.setDataOwner(this.dataOwner);
				Object v2 = null;
				if("postdata".equals(utype)) {
					v2 = ucache.getObject(IUCache.POSTCACHENAME, v.toString());
				}else if("hrbasecode".equals(utype)) {
					v2 = ucache.getObject(IUCache.HRDATABASECACHENAME, v.toString());
				}else
					v2 = ucache.getObject(IUCache.HRCACHE, v.toString());
				if(null == v2)
					return null;
				if(!(v2 instanceof Map)){
					String mk = "name";
					if(utype.equals("orgdata"))
						mk = "orgPath";
					ExtendedField exsf = fetchField(v2.getClass(), mk);
					if(null != exsf) {
						Object o = refu.getFieldValueAsStr(v2, exsf.field);
						if(null == o) {
							Method m = fetchMethod(v2.getClass(), exsf.field);
							try {if(null != m){o = m.invoke(v2, new Object[]{});}}catch (Exception e) {e.printStackTrace();}
						}
						if(null != o)
							return proOrgNamePath(o.toString());
					}
				}else{
					if(utype.equals("orgdata")&&!this.getClass().getSimpleName().equals("OrgEntity")) {
						Object n = ((Map)v2).get("namePath");
						if(null != n&&StringUtil.isValid(n.toString())) {
							return proOrgNamePath(n.toString());
						}
					}
					String n = ((Map)v2).get("name").toString();
					return n;
				}
			}
		}
		//end
		if(null == rf)
			return getFieldStr( ef.field,v);
		//处理引用属性的显示值
		String mk =  ef.field.getName().replaceAll("Id", "");
		if(StringUtil.isValid(rf.sourceFiledName()))
			mk = rf.sourceFiledName();
		ExtendedField exref = fetchField(c, mk);
		if(null == exref)
			return getFieldStr(ef.field,v);
		Field reff = exref.field;
		//默认为name
		Field n = refu.getField(reff.getType(), rf.targetShowFiled());
		Object refEntity = refu.getFieldValue(this, reff);
		if(null == refEntity||rf.entity() != refEntity.getClass())
			return null;
		if(null == n){
			try {
				Method m = fetchMethod(reff.getType(), rf.targetShowFiled());
				if(null != m){return m.invoke(refEntity, new Object[]{});}
			}catch(Exception e){System.err.println(e.getMessage());}
		}else{
			return refu.getFieldValue(refEntity,n);
		}
		//end
		return getFieldStr(ef.field,v);
	}
	private Object getFieldStr(Field f,Object...v){
		if(null == f)
			return (null == v||v.length<=0)?"":v[0];

		Object fv = null;
		try {
			if(null == v||v.length<=0)
				return null;
			fv = v[0];
			if (fv instanceof Boolean) {
				if ((Boolean) fv) {
					return "是";
				} else {
					return "否";
				}
			}
			if (fv instanceof Byte) {
				if ((Byte) fv == 1) {
					return "是";
				} else {
					return "否";
				}
			}
			// 性别判断
			if ("sex".equals(f.getName())) {
				if ("1".equals(fv.toString())||"true".equals(fv.toString())) {
					return "男";
				} else if ("0".equals(fv.toString())||"false".equals(fv.toString()))  {
					return "女";
				}else{
					return "保密";
				}
			}
			// 性别estate
			if ("estate".equals(f.getName())&&null != fv) {
				return this.getEstateStr();
			}
			Class<?> c = this.getClass();
			EXColumn ec = AnnotationParser.getExtColumn(f);
			if(null == ec){
				try {
					Method m = fetchMethod(c, f);
					if(null != m)
						ec = AnnotationParser.getExtColumn(m);
				}catch (Exception e) {e.printStackTrace();}
			}
			if(null != ec&&StringUtil.isValid(ec.showGetMethod())){
				Method m = fetchMethod(c, ec.showGetMethod());
				Class<?>[] pc = m.getParameterTypes();
				if(null != pc&&pc.length>0)
					fv = m.invoke(this,fv);
				else
					fv = m.invoke(this);
				if(null != fv)
					return fv;
				if(null != ec.vlis()&&ec.vlis().length>1)
					for(String v_:ec.vlis()){
						if(v_.indexOf(fv.toString()) != -1&&v_.indexOf(":") != -1)
							return v_.split(":")[1];
					}
			}
			if(f.getType().getName().equals(Date.class.getName())){
				return DateUtil.dateToString((Date) fv, ec.dateFormat());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return fv;
	}
	/**
	 * 注意，此方法是编辑界面与创建界面直接调用。列表只有在调fieldNameValue拿不到
	 * 数据时才会调此方法
	 * @param fieldName
	 * @return
	 */
	public Object fieldValue(String fieldName){
		if("estate".equals(fieldName)){
			return this.getEstateStr();
		}
		Object v = null;
		Method m = null;
		ExtendedField ef = this.fetchField(this.getClass(), fieldName);
		if(null == ef||ef.field == null)
			return null;
		Field f = ef.field;
		try {
			m = refu.gettFieldReadMethod(this.getClass(), fieldName);
			if(null != m){v = m.invoke(this, new Object[]{});}
		}catch (Exception e) {e.printStackTrace();}

		if(null == v){
			v = refu.getFieldValue(this, f);
			if(null == v)
				return null;
		}
		//这里是创建与修改界面设，所以直接返回EMPID，不再处理名称相关
		if("employeeid".equals(fieldName)) {
			return v;
			/*ReflectField rf = AnnotationParser.parseReflectField(ef.field);
			if(null != rf) {
				String mk =  fieldName.replaceAll("Id", "");
				mk =  fieldName.replaceAll("id", "");
				if(StringUtil.isValid(rf.sourceFiledName()))
					mk = rf.sourceFiledName();
				ExtendedField exref = fetchField(this.getClass(), mk);
				//默认为name
				Field n = refu.getField(rf.entity(), rf.targetShowFiled());
				Object refEntity = refu.getFieldValue(this, exref.field);
				if(null != n)
					return refu.getFieldValue(refEntity,n);
				try {
					m = fetchMethod(rf.entity(), rf.targetShowFiled());
					if(null != m){return m.invoke(refEntity, new Object[]{});}
				}catch(Exception e){System.err.println(e.getMessage());}
			}*/
		}

		return getFieldStr(f,v);
	}
	public Object getFieldStr(String fieldName){
		if(null == fieldName)
			return "";
		try {
			ExtendedField f = fetchField(this.getClass(), fieldName);
			if(null == f)
				return null;
			return getFieldStr(f.field);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}
	public String getId(){return null;};
	public void setId(String id){};
	/*public static boolean canedit(int est) throws CannotEditException{
		if(est == ENABLE||est == NOREVIEW)//)||est == CONFIRMED)
			return true;
		throw new CannotEditException("cannotedit");
		//return false;
	}*/
	public int[] fetchCanEditEstate() {
		return new int[] {ENABLE,NOREVIEW};
	}
	public boolean canedit() throws CannotEditException{
		for(int est:fetchCanEditEstate()) {
			if(estate == est)
				return true;
		}
		throw new CannotEditException("cannotedit");
	}
	public static String getInBuilt(){
		return INBUILT;
	}
	public String getLasterModifyerId() {
		return lasterModifyerId;
	}
	public void setLasterModifyerId(String lasterModifyerId) {
		this.lasterModifyerId = lasterModifyerId;
	}
	public String getFullpinying() {
		return fullpinying;
	}
	public void setFullpinying(String fullpinying) {
		this.fullpinying = fullpinying;
	}
	public void setBase64Img(String b64imgcode,String fName) {
		try {
			if(!StringUtil.isValid(b64imgcode))
				return;
			refu.setFieldValues(this, fName, new SerialBlob(b64imgcode.toString().getBytes("UTF-8")));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public void fillBlob(String v,String fName) {
		try {
			if(!StringUtil.isValid(v))
				return;
			refu.setFieldValues(this, fName, new SerialBlob(v.toString().getBytes("UTF-8")));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public String fetchBlobStr(String fName) {
		try {
			if(!StringUtil.isValid(fName))
				return null;
			Object o = refu.getFieldValue(this, refu.getField(this.getClass(),fName));
			if(o instanceof Blob){
				Blob bo = (Blob)o;
				return new String(bo.getBytes(1, (int) bo.length()));
			}
				
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	public String getB64imgcodeStr(String fName) {
		try {
			ExtendedField f = fetchField(this.getClass(), fName);
			if(null == f)
				return null;
			Object o = refu.getFieldValue(this, f.field);
			if(o == null)
				return null;
			if(!(o instanceof Blob))
				return null;
			Blob bo = (Blob)o;
			if (bo.length() <= 0)
				return null;
			return new String(bo.getBytes(1, (int) bo.length()));
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return "";
	}
	public static String fetchParamFindKey(String entityFullName) {
		return "find_" + entityFullName.replaceAll("\\.", "_");
	}

	public String getBatchNo() {
		return batchNo;
	}
	public void setBatchNo(String batchNo) {
		this.batchNo = batchNo;
	}
	public String fetchNameFileName() {
		return "name";
	}

	/**
	 * 用于列表处理为MAP时显示前调用
	 * @author youglu 2019-06-21 10:17
	 * @param ov 值对象
	 * @param fn 显示的属性
	 * @return
	 */
	public Object proFieldShow(Object ov,String fn){ return null; }
	public String importBeforSave(IGeneralService<?> service) {
		return null;
	}
	public String importAfterSave(IGeneralService<?> service) {
		return null;
	}
	public <S> void importOver(IGeneralService<?> service,List<S> s) { 
	}
}
