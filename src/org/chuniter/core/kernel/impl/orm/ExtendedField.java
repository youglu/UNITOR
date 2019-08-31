package org.chuniter.core.kernel.impl.orm;

import java.io.Serializable;
import java.lang.reflect.Field;

import org.chuniter.core.kernel.annotation.EXColumn;
import org.chuniter.core.kernel.annotation.EXIDColumn;
import org.chuniter.core.kernel.annotation.ReflectField;
import org.chuniter.core.kernel.kernelunit.AnnotationParser;
import org.chuniter.core.kernel.kernelunit.StringUtil;


public class ExtendedField implements Serializable ,Cloneable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 6307615630039704527L;
	public Field field;
	//表中对应的名称
	public String columnName;
	
	public boolean needORM = true;
	
	private Boolean showInTable = null;// true;
	private Boolean searchMust = false;
	
	private Integer lengthd;
	
	private String type;// = "varchar";
	
	public EXColumn exc = null;
	
	public EXIDColumn exid = null;
	
	private String style = "";
	
	private String titleStyle = "";
	
	private String title;
	
	private String dateFormat;
	
	private Integer index;
	
	public transient ReflectField reff;

	public ExtendedField() { 
		
	}
	public boolean getSearchMust() {
		if(searchMust)
			return searchMust;
		if(null != exc)
			return exc.searchMust();
		return searchMust;
	}
	public void setSearchMust(boolean b) {
		searchMust = b;
	}
	public ExtendedField(Field f,EXColumn exc,boolean needORM) { 
		this.exc = exc;
		this.field = f;  
		this.needORM = needORM;
		if(null != exc)
			this.setShowInTable(exc.showtable());
		this.setLengthd(AnnotationParser.parseFieldLength(f));
		this.columnName = AnnotationParser.getNameFormColumn(f); 
		//缓存起类扩展属性
		EntityMapping.putClassCacheInfo(f.getDeclaringClass(),this,null); 
	}
	public Integer getLengthd() {
		if(null == lengthd){
			if(field.getType().getSimpleName().equalsIgnoreCase("string"))
				lengthd = 50;
			else if(field.getType().getSimpleName().equalsIgnoreCase("integer"))
				lengthd = 4;
			else
				lengthd = 4;
		}
		return lengthd;
	}

	public void setLengthd(Integer lengthd) {
		this.lengthd = lengthd;
	}

	public String getColumnName() {
		return columnName;
	}

	public void setColumnName(String columnName) {
		this.columnName = columnName;
	}

	public boolean getIsKeySearch() {
		if(null != exc){
			return exc.keysearch();
		}
		return false;
	}

	public void setExc(EXColumn exc) {
		this.exc = exc;
	}
	public EXColumn getExc() {
		return this.exc;
	}
	public EXIDColumn getExid() {
		return exid;
	}

	public void setExid(EXIDColumn exid) {
		this.exid = exid;
	}
	public String getTitle(){
		if(StringUtil.isValid(title))
			return title;
		if(null != exc&&StringUtil.isValid(exc.title()))
			title = exc.title();
		return this.title;
	}
	public void setTitle(String title){
		this.title = title;
	}
	public void setStyle(String style){
		this.style = style;
	}
	public String getStyle(){
		if(null != exc&&!StringUtil.isValid(style)){
			style = exc.style();
		}
		return style;
	}

	public String getTitleStyle() {
		if(null != exc){
			if(!StringUtil.isValid(style))
				style = exc.style();
			titleStyle+=exc.titleStyle();
		}
		return titleStyle;
	}

	public void setTitleStyle(String titleStyle) {
		this.titleStyle = titleStyle;
	}

	public String getType() { 
		if(null != exc&&StringUtil.isValid(exc.utype()))
			return exc.utype(); 
		if(StringUtil.isValid(type))
			return type;
		if(null != field){
			type = field.getType().getSimpleName();
		}
		return type;
	}
	public void setType(String type) { 
		this.type = type;
	}
	public Integer getIndex(){
		if(null != index)
			return index;
		return (exc==null)?0:exc.index();
	}

	public boolean isShowInTable() {
		/*if(null != showInTable)
			return showInTable;*/
		if(exc != null)
			showInTable = exc.showtable();
		return showInTable==null?false:showInTable;
	}
	public String getSortName() {
		if(exc != null&&exc.showtable())
			return exc.sortName();
		return columnName;
	}
	public void setShowInTable(boolean showInTable){
		this.showInTable = showInTable;
	}

	public String getDateFormat() {
		if(StringUtil.isValid(dateFormat))
			return dateFormat;
		if(null != exc&&StringUtil.isValid(exc.dateFormat()))
			return exc.dateFormat();
		return dateFormat;
	}

	public void setDateFormat(String dateFormat) {
		this.dateFormat = dateFormat;
	}

	public void setIndex(Integer index) {
		this.index = index;
	}

	public boolean isNeedORM() {
		return needORM;
	}

    @Override
    public Object clone() throws CloneNotSupportedException {
		ExtendedField exf = (ExtendedField) super.clone();
		exf.exc = exc;
        return exf;
    }
    public ExtendedField cloneme() throws CloneNotSupportedException {
        return (ExtendedField) this.clone();
    }
}
