package org.chuniter.core.kernel.kernelunit;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.chuniter.core.kernel.annotation.EXColumn;
import org.chuniter.core.kernel.annotation.EXID;
import org.chuniter.core.kernel.annotation.EXIDColumn;
import org.chuniter.core.kernel.annotation.ReflectField;
import org.chuniter.core.kernel.annotation.UEntity;
import org.chuniter.core.kernel.impl.orm.ExtendedField;
import org.chuniter.core.kernel.model.BaseEntity;

/**
 * 注解分析器
 * @author youg
 * @time 2013-03-22
 */
public class AnnotationParser implements Serializable {
	private static final long serialVersionUID = -2267475972296676373L;
	protected static Log log = LogFactory.getLog(AnnotationParser.class);
	private static  ReflectUtil ref = ReflectUtil.getInstance();
	public static String getNameFormEntity(Class<?> cz){
		//Entity
		String tableName = cz.getSimpleName();
		Annotation[] acs = cz.getDeclaredAnnotations(); 
		if(null == acs)
			return tableName;
		Entity et = getEntityAnnocation(cz);
		UEntity uet = getUEntityAnnocation(cz);
		if(null != et)
			tableName = et.name();
		if(null != uet){
			tableName = uet.name();	
			if(BaseEntity.class.isAssignableFrom(cz)){
				tableName = BaseEntity.ftname();
				if(!StringUtil.isValid(tableName))
					tableName = uet.name();
			}
		}
		
		if(!StringUtil.isValid(tableName))
			tableName = cz.getSimpleName();	
		return tableName;
	}
	private static Entity getEntityAnnocation(Class<?> cz){
		Annotation[] acs = cz.getDeclaredAnnotations();
		if(null == acs||acs.length<=0)
			return null;
		for(Annotation ac:acs){
			if(ac instanceof Entity){ 
				return (Entity)ac;
			}
		}
		return null;
	}
	public static String getEntityPath(Class<?> cz){
		UEntity uec = getUEntityAnnocation(cz);
		if(null == uec)
			return null; 
		return uec.path();
	}
	public static UEntity getUEntityAnnocation(Class<?> cz){
		Annotation[] acs = cz.getDeclaredAnnotations();
		if(null == acs||acs.length<=0)
			return null;
		for(Annotation ac:acs){
			if(ac instanceof UEntity){ 
				return (UEntity)ac;
				
			}
		}
		return null;
	}
	public static String getNameFormId(Class<?> cz){
		//Entity
		Field f = ref.getField(cz, "id");
		if(null != f)
			return f.getName(); 
		f = ref.getField(cz, "Id");
		if(null != f)
			return f.getName();
		ExtendedField[] efs = ref.getAllField(cz);
		if(null == efs||efs.length<=0)
			return null;
		for(ExtendedField ef:efs){
			Annotation[] acs = ef.field.getDeclaredAnnotations();
			if(null == acs||acs.length<=0)
				continue;
			for(Annotation ac:acs){
				if(ac instanceof Id){
					return getNameFormColumn(ef.field);
				}else if(ac instanceof EXID){
					return ((EXID)ac).name();
				}
			}
		}		
		return null;
	}
	public static String getNameFormId(Class<?> cz,ExtendedField[] efs){ 
		Field f = ref.getField(cz, "id");
		if(null != f)
			return f.getName(); 
		f = ref.getField(cz, "Id");
		if(null != f)
			return f.getName();
		if(null == efs||efs.length<=0)
			return null;
		for(ExtendedField ef:efs){
			Annotation[] acs = ef.field.getDeclaredAnnotations();
			if(null == acs||acs.length<=0)
				continue;
			for(Annotation ac:acs){
				if(ac instanceof Id){
					return getNameFormColumn(ef.field);
				}else if(ac instanceof EXID){
					return ((EXID)ac).name();
				}
			}
		}		
		return null;
	}
	public static String getNameFormColumn(Field f){
		Annotation[] acs = f.getDeclaredAnnotations();
		String cname = f.getName();		
		if(null == acs||acs.length<=0)
			return cname;
		for(Annotation ac:acs){
			if(ac instanceof Column){
				cname = ((Column)ac).name();
				if(!StringUtil.isValid(cname))
					return f.getName();
				return cname;
			}
		}
		return cname;
	}
	public static String getNameFormColumn(Class<?> f){
		Annotation[] acs = f.getDeclaredAnnotations();
		String cname = f.getName();	
		if(null == acs||acs.length<=0)
			return cname;
		for(Annotation ac:acs){
			if(ac instanceof Column){
				cname = ((Column)ac).name();
				if(!StringUtil.isValid(cname))
					cname = f.getName();
				return cname;
			}
		}
		return cname;
	}	
	public static boolean isTransient(Field f){
		Annotation[] acs = f.getDeclaredAnnotations();
		if(null == acs||acs.length<=0)
			return false;
		for(Annotation ac:acs){
			if(ac instanceof EXColumn){
				return ((EXColumn)ac).isTransient();
			}
		}
		return false;
	}
	public static EXColumn getExtColumn(Field f){
		Annotation[] acs = f.getDeclaredAnnotations();
		if(null == acs||acs.length<=0)
			return null;
		for(Annotation ac:acs)
			if(ac instanceof EXColumn)
				return ((EXColumn)ac);
		return null;
	}	
	public static EXColumn getExtColumn(Method m){
		Annotation[] acs = m.getDeclaredAnnotations();
		if(null == acs||acs.length<=0)
			return null;
		for(Annotation ac:acs)
			if(ac instanceof EXColumn)
				return ((EXColumn)ac);
		return null;
	}
	public static EXIDColumn getEXIDColumn(Field f){
		Annotation[] acs = f.getDeclaredAnnotations();
		if(null == acs||acs.length<=0)
			return null;
		for(Annotation ac:acs)
			if(ac instanceof EXIDColumn)
				return ((EXIDColumn)ac);
		return null;
	}	
	public static boolean isTransient(Method m){
		Annotation[] acs = m.getDeclaredAnnotations();
		if(null == acs||acs.length<=0)
			return false;
		for(Annotation ac:acs){
			if(ac instanceof EXColumn){
				return ((EXColumn)ac).isTransient();
			}
		}
		return false;
	}	
	public static Integer parseFieldLength(Field f){
		Annotation[] ats = f.getDeclaredAnnotations(); 
		if(null == ats||ats.length<=0)
			return 0;
		for(Annotation at:ats){
			log.debug("注解类型为:"+at.annotationType());
			if(at instanceof javax.persistence.Column){
				return ((javax.persistence.Column)at).length();
			}
			if(at instanceof EXColumn){
				return ((EXColumn)at).length();
			}			
		}
		return 0;
	}
	public static ReflectField parseReflectField(Field f){
		Annotation[] ats = f.getDeclaredAnnotations();
		if(null == ats||ats.length<=0)
			return null;
		for(Annotation at:ats){
			log.debug("注解类型为:"+at.annotationType());
			if(at instanceof ReflectField){
				return (ReflectField)at;
			}
		}
		return null;
	}	
	public static List<ExtendedField> parseReflectFields(Class<?> c){
		ExtendedField[] exfs = ref.getAllEXPField(c);
		if(null == exfs||exfs.length<=0)
			return null;
		List<ExtendedField> rs = new ArrayList<ExtendedField>();
		for(ExtendedField exf:exfs) {
			ReflectField e = parseReflectField(exf.field);
			if(null == e)
				continue; 
			exf.reff = e;
			rs.add(exf);
		}
		return rs;
	}	
	public static void main(String[] args) {
	}
	public static String getEntityTitle(Class<?> cz){  
		UEntity uet = getUEntity(cz); 
		if(null == uet)
			return cz.getSimpleName();
		return uet.title();
	}
	public static UEntity getUEntity(Class<?> cz){  
		UEntity uet = getUEntityAnnocation(cz); 
		return uet;
	}
}