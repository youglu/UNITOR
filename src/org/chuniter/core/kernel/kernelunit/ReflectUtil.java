package org.chuniter.core.kernel.kernelunit;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.sql.Blob;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.sql.rowset.serial.SerialBlob;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.chuniter.core.kernel.annotation.EXColumn;
import org.chuniter.core.kernel.annotation.EXIDColumn;
import org.chuniter.core.kernel.annotation.ReflectField;
import org.chuniter.core.kernel.annotation.UService;
import org.chuniter.core.kernel.impl.orm.EntityMapping;
import org.chuniter.core.kernel.impl.orm.ExtendedField;
import org.chuniter.core.kernel.model.BaseEntity;

public class ReflectUtil{

	protected Log log = LogFactory.getLog(ReflectUtil.class);
	private final static Map<String,Method> methodCache = new HashMap<String,Method>();
	private static class SingletonHolder{
		private static final ReflectUtil instance = new ReflectUtil();
	}
	private ReflectUtil() { }

	public static ReflectUtil getInstance() {
		return SingletonHolder.instance;
	}
	public static Class<?> getFiledEntityClass(Field f) {
		if(null == f)
			return null;
		return f.getDeclaringClass();
	}
	public ExtendedField[] getAllField(String c) {
		try {
			return getAllField(Class.forName(c),false);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}
	public ExtendedField[] getAllField(Class<?> c) {
		return getAllField(c,false);
	}
	public ExtendedField[] getAllField(Class<?> c,boolean includeGetMethod) {
		//FIXME 这里使用缓存则需要单元在停止或更新时也要解注册实体，不然这里保存的是原单元的类加载器加载的类，而在处理保存时设置时却用的最新的单元类加载器，导至异常:IllegalArgumentException发生.youg 2018-07-25 21:28
		ExtendedField[] cacheexfs = EntityMapping.getClassExfs(c.getName());
		if(null != cacheexfs&&cacheexfs.length>0) {
			ExtendedField f = cacheexfs[0];
			if(f!=null&&f.field.getDeclaringClass() == c)
				return cacheexfs;
		}
		//end
		Class<?> s = c;
		boolean needORM = false;
		ExtendedField exfd = null;
		Map<String,ExtendedField> me = new LinkedHashMap<String,ExtendedField>();
		Field[] fields = null;
		int i = 0;
		while (s != null) {
			// 获取对应类中的所有属性域
			fields = s.getDeclaredFields();
			for (Field f:fields) {
				if("SerialVersionUID".equalsIgnoreCase(f.getName()))
					continue;
				if(Modifier.isTransient(f.getModifiers())&&!includeGetMethod){
					log.debug("属性"+f.getName()+"或其set方法 具有transient声明，不需要持久化.");
					continue;
				}
				if(me.containsKey(f.getName()))
					continue;
				try {
					Method rm = null;
					needORM = fieldNeedMapping(s,f,false);
					if(!needORM&&includeGetMethod){
						rm = this.gettFieldReadMethod(s,f,true);
						if(rm == null)
							rm = this.gettFieldReadMethod(s,f);
						if(null == rm&&s != c)
							rm = this.gettFieldReadMethod(c,f);
						if(null ==rm)
							continue;
					}
					EXColumn exc = AnnotationParser.getExtColumn(f);
					try {
						if(exc == null&&rm == null){
							rm = this.gettFieldReadMethod(c,f,true);
							if(null != rm)
								rm = this.gettFieldReadMethod(s,f,true);
							if(null != rm)
								rm = this.gettFieldReadMethod(s,f);
							if(null != rm){
								exc = AnnotationParser.getExtColumn(rm);
							}
							//if(null == exc&&s != c)
							//rm = this.gettFieldReadMethod(c,f);
							if(null != rm){
								EXColumn mexc = AnnotationParser.getExtColumn(rm);
								if(null != mexc)
									exc = mexc;
							}
						}else {
							rm = this.gettFieldReadMethod(c,f,true);
							if(null != rm) {
								EXColumn mexc = AnnotationParser.getExtColumn(rm);
								if(null != mexc)
									exc = mexc;
							}
						}
					} catch (Exception e1) {  e1.printStackTrace(); }
					EXIDColumn exidc = AnnotationParser.getEXIDColumn(f);
					exfd = new ExtendedField();
					exfd.exc = exc;
					exfd.field = f;
					exfd.exid = exidc;
					exfd.needORM = needORM;
					exfd.setShowInTable(null==exc?true:exc.showtable());
					exfd.setLengthd(AnnotationParser.parseFieldLength(f));
					exfd.columnName = AnnotationParser.getNameFormColumn(exfd.field);
					if(null == exc||exc.index()==0)
						exfd.setIndex(i);
					i++;
					me.put(f.getName(),exfd);
				}catch (Exception e) {
					e.printStackTrace();
				}
			}
			s = s.getSuperclass();
		}
		//Set<String> set = me.keySet();
		return  me.values().toArray(new ExtendedField[me.size()]);
	}
	public Method getHMethod(Class<?> c,Field fname) {
		Class<?> s = c;
		try{
			while(s!=null){
				Method rm = this.gettFieldReadMethod(c,fname);
				if(null != rm)
					return rm;
				s = s.getSuperclass();
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		return null;
	}
	public ExtendedField getEXTField(Class<?> c,String fname) {
		return getEXTField(c,fname,false);
	}
	public ExtendedField getEXTField(Class<?> c,Field f) {
		return getEXTField(c,f,false);
	}
	public ExtendedField getEXTField(Class<?> c,String fname,boolean includeGetMethod) {
		Class<?> s = c;
		Field f = null;
		while (s != null) {
			// 获取对应类中的所有属性域
			f = getField(s,fname);
			if(null != f)
				break;
			s = s.getSuperclass();
		}
		if(null == f)
			return null;
		return getEXTField(c,f,false);
	}
	private ExtendedField getEXTField(Class<?> c,Field f,boolean includeGetMethod) {
		ExtendedField ef = EntityMapping.getClassCacheInfo(c, f.getName());
		if(null != ef&&null != ef.field)
			return ef;
		if(null == c||f == null)
			return null;
		Class<?> s = c;
		while (s != null) {
			try {
				if(!BaseEntity.class.isAssignableFrom(c))
					return null;
				boolean needORM = fieldNeedMapping(s,f);
				Method rm = this.gettFieldReadMethod(c, f);
				if(!needORM&&includeGetMethod&&null == rm){
					s = s.getSuperclass();
					continue;
				}
				//优先使用GET方法中的注解
				EXColumn exc = null;
				//try {
				if(null != rm){
					EXColumn texc = AnnotationParser.getExtColumn(rm);
					if(null != texc)
						exc = texc;
				}
				//} catch (Exception e1) {  e1.printStackTrace(); }
				if(null == exc)
					exc = AnnotationParser.getExtColumn(f);
				ExtendedField exfd = new ExtendedField(f,exc,needORM);
				return exfd;
			} catch (Exception e) {
				e.printStackTrace();
			}
			s = s.getSuperclass();
		}
		return null;
	}
	public ExtendedField[] getAllEXPField(String c) {
		try {
			return getAllField(Class.forName(c),false);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}
	public ExtendedField[] getAllEXPField(Class<?> c) {
		//FIXME 这里使用缓存则需要单元在停止或更新时也要解注册实体，不然这里保存的是原单元的类加载器加载的类，而在处理保存时设置时却用的最新的单元类加载器，导至异常:IllegalArgumentException发生.youg 2018-07-25 21:28
		ExtendedField[] cacheexfs = EntityMapping.getClassExfs(c.getName());
		if(null != cacheexfs&&cacheexfs.length>0) {
			ExtendedField f = cacheexfs[0];
			if(f!=null&&f.field.getDeclaringClass() == c)
				return cacheexfs;
		}
		//end
		Class<?> s = c;
		boolean needEXP = false;
		Map<String,ExtendedField> me = new LinkedHashMap<String,ExtendedField>();
		while (s != null) {
			// 获取对应类中的所有属性域
			Field[] fields = s.getDeclaredFields();
			for (Field f:fields) {
				if(me.containsKey(f.getName())){
					continue;
				}
				try {
					needEXP = fieldNeedExport(s,f);
					if(!needEXP)
						continue;
					EXColumn exc = AnnotationParser.getExtColumn(f);
					Method rm;
					try {
						rm = this.gettFieldReadMethod(c, f);
						if(null != rm){
							EXColumn mexc = AnnotationParser.getExtColumn(rm);
							if(null != mexc)
								exc = mexc;
						}
					} catch (Exception e1) {  e1.printStackTrace(); }
					EXIDColumn exidc = AnnotationParser.getEXIDColumn(f);
					ExtendedField exfd = new ExtendedField();
					exfd.exc = exc;
					exfd.field = f;
					exfd.exid = exidc;

					exfd.needORM = needEXP;
					exfd.setLengthd(AnnotationParser.parseFieldLength(f));
					exfd.columnName = AnnotationParser.getNameFormColumn(exfd.field);
					//tempList.add(exfd);
					me.put(f.getName(), exfd);
				} catch (IllegalArgumentException ex) {
					ex.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					e.printStackTrace();
				}
			}
			s = s.getSuperclass();
		}
		return me.values().toArray(new ExtendedField[me.size()]);
		//return set.toArray(new ExtendedField[set.size()]);
		//ExtendedField[] fds = new ExtendedField[me.size()];
		/*int i=0;
		for(Entry<String,ExtendedField> e:me.entrySet()) {
			fds[i] = e.getValue();
			i++;
		}*/
		//me.clear();
		//me = null;
		//return fds;
	}
	public Map<String,ExtendedField> getMapEXPField(Class<?> c) {
		Map<String,ExtendedField> me = new LinkedHashMap<String,ExtendedField>();
		Class<?> s = c;
		boolean needEXP = false;
		while (s != null) {
			// 获取对应类中的所有属性域
			Field[] fields = s.getDeclaredFields();
			for (Field f:fields) {
				if(me.containsKey(f.getName())){
					continue;
				}
				try {
					needEXP = fieldNeedExport(s,f);
					if(!needEXP)
						continue;
					EXColumn exc = AnnotationParser.getExtColumn(f);
					Method rm;
					try {
						rm = this.gettFieldReadMethod(c, f);
						if(null != rm){
							EXColumn texc = AnnotationParser.getExtColumn(rm);
							if(null != texc)
								exc = texc;
						}
					} catch (Exception e1) {  e1.printStackTrace(); }
					EXIDColumn exidc = AnnotationParser.getEXIDColumn(f);
					ExtendedField exfd = new ExtendedField();
					exfd.exc = exc;
					exfd.field = f;
					exfd.exid = exidc;

					exfd.needORM = needEXP;
					exfd.setLengthd(AnnotationParser.parseFieldLength(f));
					exfd.columnName = AnnotationParser.getNameFormColumn(exfd.field);
					me.put(f.getName().toLowerCase(), exfd);
				} catch (IllegalArgumentException ex) {
					ex.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					e.printStackTrace();
				}
			}
			s = s.getSuperclass();
		}
		return me;
	}

	public Field getField(Class<?> c,String fieldName) {
		ExtendedField ef = EntityMapping.getClassCacheInfo(c, fieldName);
		if(null != ef&&null != ef.field)
			return ef.field;
		Class<?> s = c;
		while (s != null) {
			// 获取对应类中的所有属性域
			Field[] fields = s.getDeclaredFields();
			for (Field f:fields) {
				ExtendedField exf = this.getEXTField(s, f,false);
				/*if(f.getName().equals(fieldName)) {
					ExtendedField exf = this.getEXTField(s, f,false);
					EntityMapping.putClassCacheInfo(c,exf,fieldName);
					return f;
				}*/
				//ExtendedField exf = this.getEXTField(s, f,false);
				if(f.getName().equals(fieldName)||(null != exf&&null != exf.exc&&exf.exc.sortName().equals(fieldName))) {
					return f;
				}
			}
			s = s.getSuperclass();
		}
		return null;
	}
	public String[] getClassFields(Class<?> c) {
		Field[] fs = c.getDeclaredFields();
		if (fs == null || fs.length <= 0)
			return null;
		String[] fst = new String[fs.length];
		String getMethodName = "";
		int nullNV = 0;
		for (int i = 0; i < fs.length; i++) {
			getMethodName = fs[i].getName();
			int m = fs[i].getModifiers(); ;
			if((Modifier.isFinal(m)&&Modifier.isStatic(m))||(Modifier.isTransient(m)&&Modifier.isFinal(m)&&Modifier.isStatic(m))||(Modifier.isTransient(m)&&Modifier.isFinal(m)))
				continue;
			getMethodName = getMethodName.substring(0, 1).toUpperCase()+ getMethodName.substring(1);
			Method setMethod = null;
			try {
				if("CustomProperty".equalsIgnoreCase(getMethodName))
					continue;
				setMethod = c.getMethod("set" + getMethodName, fs[i].getType());
			} catch (SecurityException e) {
				log.error(this.getClass() + " 获得方法发生安全异常：\n"+ e.getMessage());
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
				log.error(this.getClass() + " 方法：set" + getMethodName+ " 不存在\n" + e.getMessage());
			}
			if (null == setMethod) {
				nullNV++;
				continue;
			}
			fst[i] = fs[i].getName();
		}
		String[] finalFn = new String[fst.length - nullNV];
		int j = 0;
		for (int i = 0; i < fst.length; i++) {
			if (null == fst[i])
				continue;
			finalFn[j] = fst[i];
			j++;
		}
		return finalFn;

	}

	public boolean fieldNeedMapping(Class<?> c,Field f,boolean...needchecktransient) throws IllegalArgumentException,
			IllegalAccessException, InvocationTargetException {
		int m = f.getModifiers();
		if((Modifier.isTransient(m)&&Modifier.isStatic(m))||(Modifier.isFinal(m)&&Modifier.isStatic(m))||(Modifier.isTransient(m)&&Modifier.isFinal(m)&&Modifier.isStatic(m))||(Modifier.isTransient(m)&&Modifier.isFinal(m)))
			return false;
		String setMethodName = f.getName();
		if("CustomProperty".equalsIgnoreCase(setMethodName))
			return false;
		setMethodName = setMethodName.substring(0, 1).toUpperCase()+ setMethodName.substring(1);
		Method setMethod = null;
		try {
			if(null == needchecktransient||needchecktransient.length<=0||!needchecktransient[0])
				if(Modifier.isTransient(f.getModifiers())){
					log.debug("属性"+f.getName()+"或其set方法 具有transient声明，不需要持久化.");
					return false;
				}
			setMethod = c.getMethod("set" + setMethodName, f.getType());
			if (null == setMethod||AnnotationParser.isTransient(setMethod))
				return false;
			return true;
		} catch (SecurityException e) {
			//log.error(c + " 获得方法发生安全异常：\n"+ e.getMessage());
			return false;
		} catch (NoSuchMethodException e) {
			//log.error(c+ " 方法：" + setMethodName+ " 不存在\n" + e.getMessage());
			return false;
		}
	}
	public boolean fieldNeedExport(Class<?> c,Field f) throws IllegalArgumentException,
			IllegalAccessException, InvocationTargetException {

		try {
			EXColumn exc = AnnotationParser.getExtColumn(f);
			if (null != exc&&(null != exc.exportName()))
				return true;
			else{
				if(null == exc||!StringUtil.isValid(exc.title()))
					return false;
				return fieldNeedMapping(c,f);
			}
		} catch (SecurityException e) {
			log.error(c + " 获得方法发生安全异常：\n"
					+ e.getMessage());
			return false;
		}
	}

	public void setFieldValues(Object t,Field fd,Object fv) throws IllegalArgumentException, IllegalAccessException{
		try {
			//log.debug("反射设值调用：被调用类："+(t==null?"t is null ":t.getClass())+"  :设被值的属性:"+(fd == null?"fd is null":fd.getName())+"  "+(fd == null?"fd is null":fd.getDeclaringClass())+"  被设值的值："+fv);
			if(null == fv||"".equals(fv)||null == fd)
				return;
			//优先使用SET方法设置，如果有的话
			Method m = this.gettFieldWriteMethod(t.getClass(), fd);
			if(null != m)
				try {m.invoke(t, fv);return;}catch(Exception e) {/*ignor exception*/}

			Class<?> ftype = fd.getType();
			String ftName = ftype.getName();
			//获取原来的访问控制权限
			boolean accessFlag = fd.isAccessible();
			//修改访问控制权限
			fd.setAccessible(true);
			//设置在对象中属性fields对应的值
			if(!ftName.equals(String.class.getName())) {
				if("是".equals(fv))
					fv = 1;
				else if("否".equals(fv))
					fv = 0;
			}
			if(ftName.equals(String.class.getName())&&!(fv instanceof String))
				fd.set(t,fv.toString());
			else if(ftName.equals(float.class.getName())&&!(fv instanceof Float)){
				try{ fd.setFloat(t, Float.valueOf(fv.toString()));}catch(Exception e){e.printStackTrace();}
			}else if(ftName.equals(Float.class.getName())&&!(fv instanceof Float)){
				try{ fd.setFloat(t, Float.valueOf(fv.toString()));}catch(Exception e){
					callSetMethod(t,fd,Float.valueOf(fv.toString()));
				}
			}else if(ftName.equals(int.class.getName())&&!(fv instanceof Integer)){
				try{ fd.setInt(t, Integer.valueOf(fv.toString()));}catch(Exception e){
					callSetMethod(t,fd,Integer.valueOf(fv.toString()));
				}
			}else if(ftName.equals(Boolean.class.getName())&&!(fv instanceof Boolean)){
				String fvstr = fv.toString();
				if("1".equals(fvstr))
					fvstr = "true";
				else if("0".equals(fvstr))
					fvstr = "false";
				if("是".equals(fv))
					fv = true;
				else if("否".equals(fv))
					fv = false;
				try{ fd.setBoolean(t, Boolean.valueOf(fvstr));}catch(Exception e){callSetMethod(t,fd,Boolean.valueOf(fvstr));}
			}else if(ftName.equals(Byte.class.getName())){
				try{
					if(fv instanceof Byte)
						fd.setByte(t, (Byte)fv);
					else if(fv instanceof Boolean&&null != fv)
						fd.setByte(t, (byte)(((Boolean)fv)?1:0));
				}catch(Exception e){
					String v = fv.toString();
					if("false".equals(v))
						v = "0";
					else if("true".equals(v))
						v = "1";
					callSetMethod(t,fd,Byte.valueOf(v));
				}
			}else if(ftName.equals(Date.class.getName())&&!(fv instanceof Date)){
				if(fv instanceof String) {
					fv = fv.toString().replaceAll("/", "-");
				}
				try{
					fd.set(t, DateUtil.stringToDate(fv.toString()));
				}catch(Exception e){
					callSetMethod(t,fd,DateUtil.stringToDate(fv.toString(),"yyyy-MM-dd HH:mm:ss"));
				}
			}else if(ftName.equals(Integer.class.getName())&&!(fv instanceof Integer)){
				if("是".equals(fv))
					fv = 1;
				else if("否".equals(fv))
					fv = 0;
				try{ fd.set(t, Integer.valueOf(fv.toString()));}catch(Exception e){callSetMethod(t,fd,Integer.valueOf(fv.toString()));}
			}else if(ftype == double.class&&!(fv instanceof Double)){
				try{ fd.set(t, Double.valueOf(fv.toString()));}catch(Exception e){callSetMethod(t,fd,Double.valueOf(fv.toString()));}
			}else if(ftype == Double.class&&!(fv instanceof Double)){
				try{ fd.set(t, Double.valueOf(fv.toString()));}catch(Exception e){callSetMethod(t,fd,Double.valueOf(fv.toString()));}
			}else if(ftName.equals(double.class.getName())&&!(fv instanceof Double)){
				try{ fd.setDouble(t, Double.valueOf(fv.toString()));}catch(Exception e){callSetMethod(t,fd,Double.valueOf(fv.toString()));}
			}else if((ftName.equals(Long.class.getName()))||ftName.equals(long.class.getName())){
				try{ fd.setLong(t, Long.valueOf(fv.toString()));}catch(Exception e){ callSetMethod(t,fd,Long.valueOf(fv.toString()));}
			}else if(ftName.equals(BigDecimal.class.getName())&&!(fv instanceof BigDecimal)){
				try{  BigDecimal b = (BigDecimal) fv; callSetMethod(t,fd,b); }catch(Exception e){
					callSetMethod(t,fd,BigDecimal.valueOf(Double.valueOf(fv.toString())));
				}
			}else if(ftype.getSimpleName().equals("Blob")){
				if(fv instanceof String){
					Blob b = new SerialBlob(fv.toString().getBytes("utf-8"));//String 转 blob
					fd.set(t,b);
				}else{
					fd.set(t,fv);
				}
			}else
				fd.set(t,fv);
			//恢复访问控制权限
			fd.setAccessible(accessFlag);
		}catch(Exception e){
			e.printStackTrace();
			log.error("设置对象"+t+"的属性"+fd+"的值为:"+fv+" 发生异常：\n"+ e.getMessage());
			System.err.println("设置对象"+t+"的属性"+fd+"的值为:"+fv+" 发生异常：\n"+ e.getMessage());
			try{
				callSetMethod(t,fd,fv);
			}catch(Exception ee){
				ee.printStackTrace();
			}
		}
	}
	public void setFieldValues(Object t,String fdName,Object fv) throws IllegalAccessException, SecurityException, NoSuchFieldException{
		Field fd = getField(t.getClass(),fdName);
		if(null == fd){
			Field[] fs = t.getClass().getDeclaredFields();
			if(null == fs||fs.length<=0)
				return;
			fdName = fdName.toLowerCase().replaceAll(t.getClass().getSimpleName().toLowerCase()+"_", "");
			for(Field f:fs){
				if(f.getName().equalsIgnoreCase(fdName)){
					fd = f;
					break;
				}
			}
		}
		this.setFieldValues(t, fd, fv);
	}
	public String[] getFieldValues(Object t) throws IllegalArgumentException, IllegalAccessException,InvocationTargetException {
		Class<?> c = (Class<?>)t.getClass();
		String[] fs = getClassFields(c);
		String[] GetFieldV = new String[fs.length];
		if (fs == null || fs.length <= 0)
			return null;
		String fname = "";
		for (int i = 0; i < fs.length; i++) {
			fname = fs[i];
			int m = this.getField(c, fname).getModifiers();
			if((Modifier.isFinal(m)&&Modifier.isStatic(m))||(Modifier.isTransient(m)&&Modifier.isFinal(m)&&Modifier.isStatic(m))||(Modifier.isTransient(m)&&Modifier.isFinal(m)))
				return null;
			fname = fname.substring(0, 1).toUpperCase()+ fname.substring(1);
			Method setMethod = null;
			try {
				setMethod = c.getMethod("get" + fname, null);
			} catch (SecurityException e) {
				log.debug(this.getClass() + " 获得方法发生安全异常：\n"
						+ e.getMessage());
			} catch (NoSuchMethodException e) {
				log.debug(this.getClass() + " 属性：" + fname+ " 不存在\n" + e.getMessage());
			}
			if (null != setMethod)
				GetFieldV[i] = ObjectToString(setMethod.invoke(t, null));
		}
		return GetFieldV;
	}
	public Object getFieldValue(Object obj,Field fd){
		try {
			// 获取原来的访问控制权限
			boolean accessFlag = fd.isAccessible();
			// 修改访问控制权限
			fd.setAccessible(true);
			// 获取在对象f中属性fields[i]对应的对象中的变量
			Object o = fd.get(obj);
			// 恢复访问控制权限
			fd.setAccessible(accessFlag);
			return o;
		} catch (Exception ex) {
			ex.printStackTrace();
			log.error("获得属性值出错:"+obj+","+(fd));
		}
		return null;
	}
	public String getFieldValueAsStr(Object obj,Field fd){
		try {
			Object o = this.getFieldValue(obj, fd);
			if(null == o)
				return null;
			return ObjectToString(o);
		} catch (IllegalArgumentException ex) {
			ex.printStackTrace();
		}
		return null;
	}
	@EXColumn(isTransient=true)
	public String ObjectToString(Object obj) {
		if (null == obj)
			return null;
		if (obj instanceof Integer)
			return String.valueOf(obj);
		if (obj instanceof Double)
			return String.valueOf(obj);
		if (obj instanceof Date) {
			return DateUtil.dateToString((Date) obj, "yyyy-MM-dd HH:mm:ss");
		}
		return obj.toString();
	}

	public void callSetMethod(Object t,Field f,Object fv) throws IntrospectionException, IllegalAccessException, IllegalArgumentException, InvocationTargetException{
		Method wM  = null;
		try{PropertyDescriptor pd = new PropertyDescriptor(f.getName(), t.getClass());wM = pd.getWriteMethod();}catch(Exception e) {
			wM = gettFieldWriteMethod(t.getClass(),f);
		}
		if(null == wM)
			return;
		wM.invoke(t, fv);
	}
	public Method gettFieldWriteMethod(Class<?> clz,Field f) throws IntrospectionException, IllegalAccessException, IllegalArgumentException, InvocationTargetException{
		//PropertyDescriptor pd = new PropertyDescriptor(f.getName(), clz);
		//Method wM = pd.getWriteMethod();//获得写方法
		//return wM;
		try{
			String s = f.getName();
			s = "set" +s.substring(0, 1).toUpperCase()+ s.substring(1);
			return getMethod(clz, s);
		}catch(Exception e){
			e.printStackTrace();
		}
		return null;
	}
	public Method gettFieldReadMethod(Class<?> clz,Field f) {
		try {
			return gettFieldReadMethod(clz,f,false);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	public Method gettFieldReadMethod(Class<?> clz,Field f,boolean onlySelfClass) throws IntrospectionException, IllegalAccessException, IllegalArgumentException, InvocationTargetException{
		try{

			String g = f.getName();
			g = "get" +g.substring(0, 1).toUpperCase()+ g.substring(1);
			return getMethod(clz, g,onlySelfClass);
		}catch(Exception e){/*忽略异常*/}
		return null;
	}
	public Method gettFieldReadMethod(Class<?> clz,String g) throws IntrospectionException, IllegalAccessException, IllegalArgumentException, InvocationTargetException{
		try{
			g = "get" +g.substring(0, 1).toUpperCase()+ g.substring(1);
			return getMethod(clz, g);
		}catch(Exception e){/*忽略异常*/}
		return null;
	}
	public Object gettFieldReadMethodValue(Object ins,String g) throws IntrospectionException, IllegalAccessException, IllegalArgumentException, InvocationTargetException{
		try{
			g = "get" +g.substring(0, 1).toUpperCase()+ g.substring(1);
			Method m =  getMethod(ins.getClass(), g);
			if(null == m)
				return null;
			return m.invoke(ins, new Object[] {});
		}catch(Exception e){/*忽略异常*/}
		return null;
	}
	public Method getMethod(Class<?> cla,String methodName) throws IntrospectionException, IllegalAccessException, IllegalArgumentException, InvocationTargetException{
		return getMethod(cla,methodName,false);
	}
	public Object getMethodValue(Object ins,String methodName) throws IntrospectionException, IllegalAccessException, IllegalArgumentException, InvocationTargetException{
		Method m = getMethod(ins.getClass(),methodName,false);
		if(null == m)
			return null;
		return m.invoke(ins, new Object[] {});
	}
	private Method getMethod(Class<?> cla,String methodName,boolean onlySelfClass) throws IntrospectionException, IllegalAccessException, IllegalArgumentException, InvocationTargetException{
		String mk = cla.getName()+cla.getClassLoader().hashCode()+methodName;
		if(methodCache.containsKey(mk)&&null != methodCache.get(mk))
			return methodCache.get(mk);
		Method[] ms  = null;
		if(onlySelfClass) {
			ms = cla.getMethods();
			for(Method m:ms){
				if(m.getName().equals(methodName)){
					methodCache.put(mk, m);
					return m;
				}
			}
			return null;
		}
		ms = cla.getDeclaredMethods();
		for(Method m:ms){
			if(m.getName().equals(methodName)){
				methodCache.put(mk, m);
				return m;
			}
		}
		ms = cla.getMethods();
		for(Method m:ms){
			if(m.getName().equals(methodName)){
				methodCache.put(mk, m);
				return m;
			}
		}
		return null;
	}
	public Method getMethod(Class<?> cla,String methodName,Class<?>[] pcs) throws IntrospectionException, IllegalAccessException, IllegalArgumentException, InvocationTargetException{
		String mk = cla.getName()+cla.getClassLoader().hashCode()+methodName;
		if(null != pcs&&pcs.length>0) {
			mk += pcs.length;
		}
		if(methodCache.containsKey(mk)&&null != methodCache.get(mk))
			return methodCache.get(mk);
		try{
			if(null == pcs||pcs.length<=0)
				return this.getMethod(cla,methodName);
		}catch(Exception e){return null;}
		try{
			Method mm = cla.getMethod(methodName, pcs);
			if(mm != null) {
				methodCache.put(mk, mm);
				return mm;
			}
			Method[] ms = cla.getMethods();
			Class<?>[] mpcs = null;
			for(Method m:ms){
				if(!m.getName().equals(methodName))
					continue;
				mpcs = m.getParameterTypes();
				if((null == mpcs||mpcs.length<=0)&&(pcs == null||pcs.length<=0)) {
					methodCache.put(mk, m);
					return m;
				}
				if((null == mpcs||mpcs.length<=0)&&(pcs != null&&pcs.length>0))
					return null;
				if((null != mpcs&&mpcs.length>0)&&(pcs == null||pcs.length<=0))
					return null;
				if(mpcs.length != pcs.length){
					continue;
				}
				methodCache.put(mk, m);
				return m;
			}
		}catch(Exception e){log.error("无法获得指定的方法:"+e.getMessage());}
		return null;
	}
	public Class<?> getMethodFirstParamtYPE(Class<?> cla,String methodName) throws IntrospectionException, IllegalAccessException, IllegalArgumentException, InvocationTargetException{
		Method[] ms = cla.getDeclaredMethods();
		for(Method m:ms){
			if(m.getName().equals(methodName)){
				Class<?>[] pcs = m.getParameterTypes();
				if(pcs == null|| pcs.length<=0)
					return null;
				return pcs[0];
			}
		}
		return null;
	}
	public void setReflectValues(Object t,ReflectField ref,String setFieldName,Object fv){
		if(null == ref||null==fv)
			return;
		try {
			if(StringUtil.isValid(ref.sourceFiledName())){
				Field f = this.getField(t.getClass(),ref.sourceFiledName());
				if(f == null){
					log.error("根据属性名获得属性为空:类型："+t.getClass()+" 属性名:"+ref.sourceFiledName());
					return;
				}
				Object obj = null;
				try{ obj = getFieldValue(t,f);}catch(Exception ee){
					log.error("获得属性 "+f+"的值失败"+ee.getMessage());
					ee.printStackTrace();
				}
				if(null == obj)
					try { obj = ref.entity().newInstance();f.set(t, obj);} catch (InstantiationException e) {  e.printStackTrace(); }
				this.setFieldValues(obj, setFieldName, fv);
			}else{
				Field[] fields = t.getClass().getDeclaredFields();
				//ExtendedField[] extFields3 = null;
				for(Field ff:fields){
					//System.out.println(ff.getType()+"  "+ref.entity());
					if(ff.getType() == ref.entity()){
						Object obj = getFieldValue(t,ff);
						if(null == obj)
							try { obj = ref.entity().newInstance();ff.set(t, obj);} catch (InstantiationException e) {  e.printStackTrace(); }
						this.setFieldValues(obj, setFieldName, fv);
						/*extFields3 = getAllField(obj.getClass());
						for(ExtendedField extf3:extFields3){
							if(extf3.columnName.toLowerCase().equals(setFieldName.toLowerCase())){
								this.setFieldValues(obj, extf3.columnName, fv);
								return;
							}
						}*/
						return;
					}
				}
			}
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
		}
	}
	public static void main(String[] args) throws SecurityException, NoSuchMethodException, ParseException{
		ReflectUtil re = ReflectUtil.getInstance();
		Method m = re.getClass().getDeclaredMethod("ObjectToString", new Class[]{Object.class});
		//System.out.println(AnnotationParser.isTransient(m));
		//System.out.println(new Integer(1).toString());
		DecimalFormat df = new DecimalFormat("##.0000");

		Number n = df.parse("4991514.601");
		String s = df.format(n.doubleValue());
		//System.out.println(s);//
		//System.out.println(Float.valueOf("4991514.601"));//

	}
	public Field[] getServiceFiled(Class<? extends Object> class1) {
		List<Field> l = new ArrayList<Field>();
		Class<?> c = class1;
		while(c != null){
			Field[] f = c.getDeclaredFields();
			if(null == f||f.length<=0){
				c = c.getSuperclass();
				continue;
			}
			for(int i=0;i< f.length;i++)
				if(null != f[i].getAnnotation(UService.class))
					l.add(f[i]);
			c = c.getSuperclass();
		}
		if(null != l&&!l.isEmpty()){
			return l.toArray(new Field[l.size()]);
		}
		return null;
	}

	/**
	 * 升序，因为以前的都采用了升序，所以在一些属性多的地方只能引用负数了。
	 * @Title: BaseVelocityServlet.java
	 * @Package velocitybundle
	 * @Description: TODO
	 * @author youg
	 * @date Mar 16, 2018 2:36:50 PM
	 * @version V1.0
	 */
	private class EXColumnComparator implements Comparator<ExtendedField>{
		@Override
		public int compare(ExtendedField c1, ExtendedField c2) {
			int i1 = (c1.getIndex()==null)?0:c1.getIndex();
			int i2 = (c2.getIndex()==null)?0:c2.getIndex();
			//System.out.println(c1.getColumnName()+":"+i1+"   "+c2.getColumnName()+":"+i2);
			return (i1-i2);
		}
	}
	public final EXColumnComparator exfsortor = new EXColumnComparator();
	public static List<ExtendedField> sortExtendedFields(ExtendedField[]  exf_){
		List<ExtendedField> exs = new ArrayList<ExtendedField>();
		for(ExtendedField ex:exf_){
			if(!ex.needORM)
				continue;
			if((null !=ex.exc&&!ex.exc.showedit())&&(null == ex.exc.title()||"".equals(ex.exc.title())))
				continue;
			exs.add(ex);
		}
		Collections.sort(exs, getInstance().exfsortor);
		return exs;
	}
}
