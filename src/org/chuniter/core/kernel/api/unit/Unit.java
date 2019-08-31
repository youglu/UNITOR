package org.chuniter.core.kernel.api.unit;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import javax.persistence.Entity;

import org.chuniter.core.kernel.annotation.EXColumn;
import org.chuniter.core.kernel.annotation.EXID;
import org.chuniter.core.kernel.api.IGeneralService;
import org.chuniter.core.kernel.api.exception.CannotEditException;
import org.chuniter.core.kernel.impl.unit.UniterActivatorAdaptor;
import org.chuniter.core.kernel.kernelunit.Param;
import org.chuniter.core.kernel.kernelunit.StringUtil;
import org.chuniter.core.kernel.model.BaseEntity;
import org.osgi.framework.Bundle;
 
/**
 * 
 * @author Administrator
 * 
 */
@Entity
public class Unit extends BaseEntity {

	/**
	 * 
	 */
	private transient static final long serialVersionUID = -5644825954636104080L;

	@EXID
	private String id;
	
	@EXColumn(length=500,title="模块标识")
	private String unitId;
	
	@EXColumn(length=500,title="实体名")
	private String entityClass;
	
	@EXColumn(length=1000,title="描述")
	private String description;
	@EXColumn(length=200,title="类别")
	private String catalog;
	@EXColumn(length=200,title="入口")
	private String webEnterUrl = "javascript:void(0)";
	private String state;
	private String level;
	public transient Unit parentLevel;
	@EXColumn(length=200,title="级别")
	private String plevel;
	@EXColumn(length=200,title="显示顺序")
	private Integer showOrder;
	@EXColumn(length=200,title="标题")
	private String title; 
	@EXColumn(length=1500)
	private String unitIcon;
	private String comefrom;
	private Integer subUnits;
	@EXColumn(length=200,title="是否是父级")
	private Boolean isParent;
	private String typeID, moduleID, modulename, orderNo;
	@EXColumn(length=1000,title="路径")
	private String unitPath; 
	private String pathNo;
	@EXColumn(length=1000)
	private String unitIdPath; 
	
	public transient final static  String DEFAULTCOMEFROM="UNITOR单元";
	
	public Unit() {
		
	}
	
	public String getUnitIdPath() {
		return unitIdPath;
	}

	public void setUnitIdPath(String unitIdPath) {
		this.unitIdPath = unitIdPath;
	}

	public Unit(String path,String entityClass,int index) throws ClassNotFoundException { 
		this(path, Class.forName(entityClass),index); 
	}
	public Unit(String path,Class<?> c,int index) {  
		String title = BaseEntity.fetchTitle(c);
		if(!StringUtil.isValid(title)) {
			System.err.println(this.getClass().getName()+":请设置标题");
		}
		setTitle(title);
		setLevel(title);
		setEntityClass(c.getName());
		setDescription(title);  
		setWebEnterUrl(path + "/" + BaseEntity.fethPath(c) + "?mname=todest");
		setShowOrder(index);
		//unitId = c.getName()+"."+title;
	}
	public Unit(String path,Class<?> c,int index,UniterActivatorAdaptor uact) {  
		String title = BaseEntity.fetchTitle(c);
		if(!StringUtil.isValid(title)) {
			System.err.println(this.getClass().getName()+":请设置标题");
		}
		setTitle(title);
		setLevel(title);
		setEntityClass(c.getName());
		setDescription(title);  
		setWebEnterUrl(path + "/" + BaseEntity.fethPath(c) + "?mname=todest");
		setShowOrder(index);
		//unitId = c.getName()+"."+title;
		uact.regPower(title);
	}
	
	public Integer getSubUnits() {
		return subUnits;
	}
	public Boolean getIsParent() {
		return isParent;
	}

	public void setIsParent(Boolean isParent) {
		this.isParent = isParent;
	}

	public void setSubUnits(Integer subUnits) {
		this.subUnits = subUnits;
	}

	public String getState() {
		return state;
	}
	
	public String getPlevel() {
		if(null == plevel&&null != this.parentLevel)
			this.plevel = this.parentLevel.getLevel();
		return plevel;
	}

	public void setPlevel(String plevel) {
		this.plevel = plevel;
	}

	public void setState(String state) {
		this.state = state;
	}

	public void setState(int state) {
		switch(state){
		case Bundle.INSTALLED:
			this.state = "成功安装 ";break;
		case Bundle.RESOLVED:
			this.state = "启动就绪/已经停止";break;
		case Bundle.STARTING:
			this.state = "正在启动";break;
		case Bundle.ACTIVE:
			this.state = "运行中";break;
		case Bundle.STOPPING:
			this.state = "正在停止";break;
		case Bundle.UNINSTALLED:
			this.state = "卸载完毕";break;			
		}
	}

	public String getWebEnterUrl() {
		return webEnterUrl;
	}

	public void setWebEnterUrl(String webEnterUrl) {
		this.webEnterUrl = webEnterUrl;
	}

	public String getCatalog() {
		return catalog==null?"":catalog;
	}

	public void setCatalog(String catalog) {
		this.catalog = catalog;
	}

	public String getUnitId() {
		if(!StringUtil.isValid(unitId)&&StringUtil.isValid(entityClass))
			return entityClass+"."+title;
		return unitId;
	}

	public void setUnitId(String unitId) {
		this.unitId = unitId;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getLevel() { 
		return level;
	}

	public void setLevel(String level) {
		this.level = level;
	}

	public Unit getParentLevel() {
		return parentLevel;
	}

	public void setParentLevel(Unit parentLevel) {
		this.parentLevel = parentLevel;
		this.plevel = parentLevel.getLevel();
	}

	public Integer getShowOrder() {
		return showOrder == null?0:showOrder;
	}

	public void setShowOrder(Integer showOrder) {
		this.showOrder = showOrder;
	}

	public String getTitle() {
		if(!StringUtil.isValid(title)&&StringUtil.isValid(description))
			title = this.description;
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}
	
	public String getUnitIcon() {
		return unitIcon;
	}

	public void setUnitIcon(String unitIcon) {
		this.unitIcon = unitIcon;
	}
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getComefrom() {
		return (comefrom==null||comefrom.equals(""))?DEFAULTCOMEFROM:comefrom;
	}

	public void setComefrom(String comefrom) {
		this.comefrom = comefrom;
	}
	
	@Override
	public boolean equals(Object obj) {
		if(null == obj)
			return false;
		Unit eqo = (Unit)obj;
		if(null == unitId||null==eqo.getCatalog()||null == getCatalog()||null == eqo.getUnitId()){
			return false;
		}
		return getComefrom().equals(eqo.getComefrom())&&unitId.equals(eqo.getUnitId())&&title.equals(eqo.getTitle());//&&getCatalog().equals(eqo.getCatalog())&&description.equals(eqo.getDescription());
	}
	public static class ComparatorUnit implements Comparator<Unit>{
		private static ComparatorUnit instance = null;
		private ComparatorUnit(){}
		public static ComparatorUnit getInstance(){
			if(null == instance)
				instance = new ComparatorUnit();
			return instance;
		}
		 public int compare(Unit arg0, Unit arg1) {
			 int flag = 0;
			 try{
				 if(null == arg0)
					 return 1;
				 if(null == arg1)
					 return 0;
				 Unit unit1=(Unit)arg0;
				 Unit unit2=(Unit)arg1; 
				 if(null == unit1.getShowOrder()||null == unit2.getShowOrder())
					 return unit1.getTitle().compareTo(unit2.getTitle());
				if(unit1.getShowOrder()<unit2.getShowOrder())
					return 1;
				if(unit1.getShowOrder()>unit2.getShowOrder())
					return -1; 
				 flag = unit1.getTitle().compareTo(unit2.getTitle());
			 }catch(Exception e){
				 e.printStackTrace();
			 }
			 return flag;
		 }
	}  
	public static void shortUnitLis(List<Unit> units){
		Collections.sort(units,ComparatorUnit.getInstance());
	}
	public String getTypeID() {
		return typeID;
	}
	public void setTypeID(String typeID) {
		this.typeID = typeID;
	}
	public String getModuleID() {
		return moduleID;
	}
	public void setModuleID(String moduleID) {
		this.moduleID = moduleID;
	}
	public String getModulename() {
		return modulename;
	}
	public void setModulename(String modulename) {
		this.modulename = modulename;
	}
	public String getOrderNo() {
		return orderNo;
	}
	public void setOrderNo(String orderNo) {
		this.orderNo = orderNo;
	}
	public String getUnitPath() {
		return unitPath;
	}
	public void setUnitPath(String unitPath) {
		this.unitPath = unitPath;
	} 
	
	public String fetchUnitPath(IGeneralService<?> service) throws Exception {
		if (!StringUtil.isValid(this.getPlevel())) {
			this.unitPath = "0";
			this.pathNo = "0";
			return unitPath;
		}
		// 防止死循环
		if (null != getLevel()&&this.getLevel().equals(getPlevel())){
			this.unitPath = "0";
			this.pathNo = "0";
			return unitPath;
		}
		StringBuilder sb = new StringBuilder();
		StringBuilder sb2 = new StringBuilder();
		StringBuilder sb3 = new StringBuilder();
		Param p = Param.getInstance();
		//先注掉
		//p.addParam("catalog", this.catalog);
		if(null != this.getParentLevel())
			p.addParam("unitid", this.getParentLevel().getUnitId());
		else if(null != this.getPlevel())
			p.addParam("level", this.getPlevel());
		Unit o = service.findEntity(Unit.class,p,false);
		if (null == o){
			this.unitPath = "0";
			this.pathNo = "0";
			return unitPath;
		} 
		Stack<String> l = new Stack<String>(); 
		int i=1;
		while (o != null) {
			// 防止死循环
			if (null != getLevel()&&getLevel().equals(o.getLevel())){
				this.unitPath = "0";
				this.pathNo = "0";
				return unitPath;
			} 
			l.push(i+"|"+o.getLevel()+"|"+o.getUnitId()); 
			p.clear();
			p.addParam("level", o.getPlevel()); 
			o = service.findEntity(Unit.class,p); 
			i++;
		}
		while(!l.isEmpty()){
			String[] ln = l.pop().split("\\|"); 
			sb2.append(ln[0]).append("|");
			sb.append(ln[1]).append("|"); 
			sb3.append(ln[2]).append("|"); 
		} 
		if (sb.indexOf("|") != -1){
			this.setUnitPath(sb.substring(0, sb.length() - 1));
			this.setUnitIdPath(sb3.substring(0, sb3.length() - 1));
			pathNo = (sb2.substring(0, sb2.length() - 1));
		}
		return unitPath;
	}
	public static Map<String,Object> fetchUnit(IGeneralService<?> service){
		try {
			Param p = Param.getInstance();
			List<Map<String,Object>> ms = service.hisql("select id,unitId,title,moduleID from Unit where estate != "+DISABLED+"", p,false);
			if(null == ms||ms.isEmpty())
				return null;
			Map<String,Object> m = new HashMap<String,Object>();
			for(Map<String,Object> mm:ms)
				m.put(mm.get("id").toString(), mm);
			return m;
		}catch(Exception e) {
			e.printStackTrace();
		}
		return null;
		
	}
	public String getPathNo() {
		return pathNo;
	}
	public void setPathNo(String pathNo) {
		this.pathNo = pathNo;
	}
	public String getName(){
		return title;
	}
	public boolean canedit() throws CannotEditException{ 
		if(estate == ENABLE||estate == NOREVIEW||estate == DISABLED)
			return true;
		throw new CannotEditException("cannotedit");
		//return false;
	}
	public String getEntityClass() {
		return entityClass;
	}
	public void setEntityClass(String entityClass) {
		this.entityClass = entityClass;
	}
	
}
