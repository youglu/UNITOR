package org.chuniter.core.kernel.impl.orm;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.sql.Blob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import javax.sql.rowset.serial.SerialBlob;

import org.chuniter.core.kernel.api.orm.ISQLProvider;
import org.chuniter.core.kernel.kernelunit.DateUtil;
import org.chuniter.core.kernel.kernelunit.StringUtil;

import com.alibaba.fastjson.asm.Type;
 

public class JavaAndJDBCTransformer {
	
	public synchronized static String javaTypeToJDBCType(ExtendedField etf,ISQLProvider sqlprovider) {
		Class<?> typeClass = etf.field.getType();
		String eqName = typeClass.getSimpleName();//.getName();
		//eqName = eqName.substring(eqName.lastIndexOf(".")+1);
		if(null != etf.exc&&null != etf.exc.type()&&!"".equals(etf.exc.type()))
			eqName = etf.exc.type();
		if (eqName.equals("Integer"))
			return "integer";
		if (eqName.equals("Long")){
			if(sqlprovider instanceof MSSQLProvider)
				return "bigint";
			return "long";
		}if (eqName.equals("Short"))
			return "smallint";
		if (eqName.equals("Float"))
				return "float";
		if (eqName.equals("Double"))
			if(sqlprovider instanceof ORACLESQLProvider)
				return "double Precision";
			else if(sqlprovider instanceof MSSQLProvider)
				return "decimal(18,4)";
			else
				return "double";
		if (eqName.equals("BigDecimal"))
			return "numeric";
		if (eqName.equals("String")||eqName.equals("varchar")){
			int fl = 50;
			if(null != etf.getLengthd()&&etf.getLengthd()>0)
				fl = etf.getLengthd();
			if(sqlprovider instanceof ORACLESQLProvider)
				return "varchar2("+fl+")";
			return "varchar("+fl+")";
		}
		if (eqName.equals("Byte")){
			if(sqlprovider instanceof ORACLESQLProvider)
				return "char(1)";			
			return "bit";
		}
		if (eqName.equals("Boolean")){
			if(sqlprovider instanceof ORACLESQLProvider)
				return "char(1)";			
			return "bit";
		}
		if (eqName.equals("Boolean"))
			return "char(1)('Y'/'N')";
		if (eqName.equals("Date")){
			if(sqlprovider instanceof ORACLESQLProvider)
				return "TIMESTAMP";
			return "datetime";
		}
		if (eqName.equals("Time"))
			return "time";
		if (eqName.equals("Calendar")){
			if(sqlprovider instanceof ORACLESQLProvider)
				return "TIMESTAMP";
			return "datetime";
		}
		if (eqName.equals("Clob"))
			return "clob";
		//mssql没有blob类型，所以在这里得根据不同的数据库类型处理一下
		if (eqName.equals("Blob")){
			if(sqlprovider instanceof MSSQLProvider)
				return "text";
			else
				return "blob";
		}
		if (eqName.equals("TimeZone"))
			return "varchar(50)";
		
		if(sqlprovider instanceof ORACLESQLProvider)
			return "varchar2(50)";
		return "varchar(50)";
	}
	private  synchronized static Object resultGetValueWithType(ExtendedField fd,String eqName,ResultSet sqlResult,ISQLProvider sqlprovider) throws SQLException {
		String fname = fd.columnName; 
		Object obj = sqlResult.getObject(fname);
		try {
		if(null == obj||"".equals(obj.toString().trim()))
			return null;
		if (eqName.equals("Integer"))
			return sqlResult.getInt(fname);
		if (eqName.equals("Long"))
			return sqlResult.getLong(fname);
		if (eqName.equals("Short"))
			return sqlResult.getShort(fname);
		if (eqName.equals("Float"))
			return sqlResult.getFloat(fname);
		if (eqName.equals("Double"))
			return sqlResult.getDouble(fname);		
		if (eqName.equals("BigDecimal"))
			return sqlResult.getBigDecimal(fname);
		if(null != fd.exc&&null != obj){ 
			if("Blob".equals(eqName)||(null != fd.exc&&"Blob".equals(fd.exc.type()))){
				eqName = "Blob";
			}
		} 
		if (eqName.equals("String")){ 
			return sqlResult.getString(fname); 
		}
		if (eqName.equals("Byte"))
			return sqlResult.getByte(fname);
		if (eqName.equals("Boolean"))
			return sqlResult.getBoolean(fname);
		if (eqName.equals("Date")){
			try{ return sqlResult.getTimestamp(fname); }catch(Exception e){ 
				String tss = sqlResult.getString(fname);
				if(StringUtil.isValid(tss)){
					if(tss.length()>=10&&tss.length()<19)
						return DateUtil.stringToDate(tss,"yyyy-MM-dd HH:mm:ss");
					if(tss.length()>19)
						return DateUtil.stringToDate(tss,"yyyy-MM-dd HH:mm:ss:SSS");
				}
				throw e;
			}
		}
		if (eqName.equals("Time"))
			return sqlResult.getTime(fname);
		if (eqName.equals("Calendar")){
			return sqlResult.getTimestamp(fname);
		}
		if (eqName.equals("Clob"))
			return sqlResult.getClob(fname);
		//mssql没有blob类型，所以在这里得根据不同的数据库类型处理一下
		if (eqName.equals("Blob")){
			if(sqlprovider instanceof MSSQLProvider)
				try {return new String(obj.toString().getBytes());/*,"utf-8");*/} catch (Exception e) { e.printStackTrace();return sqlResult.getString(fname);}
			else if(sqlprovider instanceof MYSQLProvider)
				try {return new String(sqlResult.getBytes(fname),"utf-8");} catch (UnsupportedEncodingException e) { e.printStackTrace();return sqlResult.getString(fname);}
			else 
				return sqlResult.getBlob(fname);
		} 
		if (eqName.equals("TimeZone"))
			return sqlResult.getString(fname);
		}catch(Exception e){
			System.err.println(e.getMessage());
			e.printStackTrace();
		}
		return sqlResult.getString(fname);
	}
	public static Object resultGetValueWithType(ExtendedField fd,ResultSet sqlResult,ISQLProvider sqlprovider) throws SQLException {
		String eqName = fd.field.getType().getSimpleName();
		//String fname = fd.columnName;
		if(null != fd.exc){
			if("Date".equals(fd.exc.type())&&!"String".equals(fd.field.getType().getSimpleName()))
					eqName = fd.exc.type();		
		}
		Object obj = null;
		try{obj = resultGetValueWithType(fd,eqName,sqlResult,sqlprovider);}catch(SQLException sqle){System.out.println(eqName+" "+fd);sqle.printStackTrace();}
		try {
			//由于mssql的blob类型是以text保存，故在获得的值时就已是字符，所以直接返回
			if(null != obj&&fd.field.getType().getSimpleName().equals("Blob")&&sqlprovider instanceof MSSQLProvider){
				if(obj instanceof String){
					Blob b = new SerialBlob(obj.toString().getBytes());//.getBytes("utf-8"));//String 转 blob
					return b;
				}
			}
		} catch (Exception e) { e.printStackTrace(); }
		if(null != fd.exc&&null != obj){
			try { 
				//fd.field.getType().getSimpleName().equals("String")&&
				//System.out.println("\n\n **************************** "+eqName+"  "+(null != fd.exc?fd.exc.type():"null"));
				if((("Blob".equals(eqName))||(null != fd.exc&&"Blob".equals(fd.exc.type())))){
					//由于mssql的blob类型是以text保存，故在获得的值时就已是字符，所以直接返回
					if(sqlprovider instanceof MSSQLProvider){
						return obj;
					}
					if(obj instanceof Blob){
						Blob blob = (Blob)obj;
						InputStream ins = blob.getBinaryStream();
						ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
						byte[] bytes = new byte[1024];
						int readIndex = 0;
						while((readIndex = ins.read(bytes)) != -1){
							byteOut.write(bytes,0,readIndex);
						} 
						ins.close();
						byteOut.close();
						byteOut.flush(); 
						return new String(byteOut.toByteArray(),"utf-8");
					}
					return  new String(obj.toString().getBytes(),"utf-8");
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return obj;
	}	
	public synchronized static int resultSetValueWithTypeProArray(Object vobj,PreparedStatement ps,int pindex,ISQLProvider sqlprovider,Class<?> dataType) throws SQLException {
		if(vobj instanceof Collection){
			Collection<?> c = (Collection<?>)vobj;
			if(null != c&&c.size()>0){
				for(Object obj:c){
					pindex+=1;
					ps.setObject(pindex,obj);
				}
			}
			return pindex;
		}
		if(vobj instanceof Object[]){
			Object[] c = (Object[])vobj;
			if(null != c&&c.length>0){
				for(Object obj:c){
					pindex+=1;
					ps.setObject(pindex,obj);
				}
			}
			return pindex;
		} 
		pindex+=1;
		resultSetValueWithType(vobj,ps,pindex,sqlprovider,dataType);
		return pindex; 
	}	
	public synchronized static int resultSetValueWithTypeProArray(Object vobj,PreparedStatement ps,int pindex,ISQLProvider sqlprovider) throws SQLException {
		pindex = resultSetValueWithTypeProArray(vobj,ps,pindex,sqlprovider,null);
		return pindex;
		/*String eqName = vobj.getClass().getSimpleName(); 
		if (eqName.equals("Integer"))
			 ps.setInt(pindex, (Integer)vobj);
		if (eqName.equals("Long"))
			 ps.setLong(pindex,vobj);
		if (eqName.equals("Short"))
			 ps.setShort(pindex,vobj);
		if (eqName.equals("Float"))
			 ps.setFloat(pindex,vobj);
		if (eqName.equals("BigDecimal"))
			 ps.setBigDecimal(pindex,vobj);
		if (eqName.equals("String")){			
			 ps.setString(pindex,vobj);
		}
		if (eqName.equals("Byte"))
			 ps.setByte(pindex,vobj);
		if (eqName.equals("Boolean"))
			 ps.setBoolean(pindex,vobj);
		if (eqName.equals("Date")){
			 ps.setDate(pindex,vobj);
		}
		if (eqName.equals("Time"))
			 ps.setTime(pindex,vobj);
		if (eqName.equals("Calendar")){
			 ps.setDate(pindex,vobj);
		}
		if (eqName.equals("Clob"))
			 ps.setClob(pindex,vobj);
		if (eqName.equals("Blob"))
			 ps.setBlob(pindex,vobj);
		if (eqName.equals("TimeZone"))
			 ps.setString(pindex,vobj);
		*/
	}	
	public synchronized static void resultSetValueWithType(Object vobj,PreparedStatement ps,int pindex,ISQLProvider sqlprovider,Class<?> dataType) throws SQLException { 
		String eqName = "";
		if(null == vobj||"null".equals(vobj)){
			if(null != dataType){ 
				eqName = dataType.getSimpleName();
				if(eqName.equals("Integer")){
					//vobj = 0;
					ps.setNull(pindex,Type.INT);
				}else  if (eqName.equals("Long")){
					 ps.setNull(pindex,Types.FLOAT);
				}else if (eqName.equals("Short")){
					 ps.setNull(pindex,Types.INTEGER);
				}else if (eqName.equals("Float")){
					 ps.setNull(pindex,Types.FLOAT);
				}else if (eqName.equals(Double.class.getSimpleName())){
					 ps.setNull(pindex,Types.DOUBLE);
				}else if (eqName.equals("BigDecimal")){
					 ps.setNull(pindex,Types.BIGINT);
				}else if (eqName.equals("String")){			
					 ps.setNull(pindex,Types.VARCHAR);
				}
				else if (eqName.equals("Byte")){
					 ps.setNull(pindex,Types.BIT);
				}else if (eqName.equals("Boolean")){
							 ps.setNull(pindex,Types.BOOLEAN);
				}else if (eqName.equals("Date")){
					ps.setNull(pindex,Types.DATE);
				}
				else if (eqName.equals("Time")){
					ps.setNull(pindex,Types.TIME); 
				}else if (eqName.equals("Clob")){
					ps.setNull(pindex,Types.CLOB);
				}else if (eqName.equals("Blob")){
					if(sqlprovider instanceof MSSQLProvider){
						ps.setNull(pindex,Types.VARCHAR);
					}else
						ps.setNull(pindex,Types.BLOB);
				}else if (eqName.equals("TimeZone")){
					ps.setNull(pindex,Types.TIMESTAMP);
				}
			}else
				ps.setNull(pindex, Types.VARCHAR);
			return;
		}
		eqName = vobj.getClass().getSimpleName();
		if(eqName.equals("String")){
			if(sqlprovider instanceof  ORACLESQLProvider){
				if(null != vobj&&vobj.toString().length()>4000)
					eqName = "Blob";
			}
			if(sqlprovider instanceof  MYSQLProvider){
				if(null != vobj&&vobj.toString().length()>4000)
					eqName = "Blob";
			}
		}
		if (eqName.equals("Integer"))
			 ps.setInt(pindex, (Integer)vobj);
		/*if (eqName.equals("Long"))
			 ps.setLong(pindex,vobj);
		if (eqName.equals("Short"))
			 ps.setShort(pindex,vobj);
		if (eqName.equals("Float"))
			 ps.setFloat(pindex,vobj);
		if (eqName.equals("BigDecimal"))
			 ps.setBigDecimal(pindex,vobj);
		if (eqName.equals("String")){			
			 ps.setString(pindex,vobj);
		}
		if (eqName.equals("Byte"))
			 ps.setByte(pindex,vobj);
		if (eqName.equals("Boolean"))
			 ps.setBoolean(pindex,vobj);
		if (eqName.equals("Date")){
			 ps.setDate(pindex,vobj);
		}
		if (eqName.equals("Time"))
			 ps.setTime(pindex,vobj);
		if (eqName.equals("Calendar")){
			 ps.setDate(pindex,vobj);
		}
		if (eqName.equals("Clob"))
			 ps.setClob(pindex,vobj);
		if (eqName.equals("Blob"))
			 ps.setBlob(pindex,vobj);
		if (eqName.equals("TimeZone"))
			 ps.setString(pindex,vobj);
		*/
		
		else if (eqName.equals("Boolean"))
			 ps.setBoolean(pindex,(Boolean) vobj);
		else if (eqName.equals("byte[]")||eqName.equals("Blob")||vobj instanceof Blob){
			InputStream ins = null;
			if(vobj instanceof String){
				ins =  new ByteArrayInputStream(vobj.toString().getBytes());
			}else if(vobj instanceof byte[]){
				byte[] bytes = (byte[])vobj;
				ins =  new ByteArrayInputStream(bytes);
			}else if(vobj instanceof Blob){
				Blob b = (Blob)vobj; 
				//由于破"Microsoft SQL Server 2005 中引入了 max 说明符。此说明符增强了 varchar、nvarchar 和 varbinary 
				//数据类型的存储能力。varchar(max)、nvarchar(max) 和 varbinary(max) 统称为大值数据类型。
				//您可以使用大值数据类型来存储最大为 2^31-1 个字节的数据" 所以前面类型设置了为text,所以这里也只能设置为string
				//否则会报:"操作数类型冲突: varbinary(max) 与 text 不兼容"异常
				if(sqlprovider instanceof MSSQLProvider){
					if(b.length()>0)
						ps.setObject(pindex, new String(b.getBytes(1,(int)b.length())));
					else
						ps.setObject(pindex, null);
					return;
				}
				ins =  b.getBinaryStream(); 
				//ps.setBlob(pindex, (Blob)vobj);
				//return;
			} 
			try { ps.setBinaryStream(pindex,ins,ins.available()); } catch (IOException e) {  e.printStackTrace(); }
		}else if(vobj instanceof Date){
			//Calendar c = Calendar.getInstance();
			//c.setTime((Date) vobj);
			//Timestamp st = new Timestamp(c.getTimeInMillis());
			//ps.setTimestamp(pindex, st);
			//由于上述方式在oracle中会有问题，所以换成转成字符串方式进行 2015－06-01 youg.
			//判断不同数据库处理,没在oracle测试，2017-10-20 09:20 youg
			if(sqlprovider instanceof ORACLESQLProvider){
				ps.setString(pindex,DateUtil.dateToString((Date) vobj,"yyyy-MM-dd HH:mm:ss:SSS"));
			}else{
				Calendar c = Calendar.getInstance();
				c.setTime((Date) vobj);
				Timestamp st = new Timestamp(c.getTimeInMillis());
				ps.setTimestamp(pindex, st);
			}
			//end
			//end 06-01
			return;
		}else
			ps.setObject(pindex,vobj);
	}	
	public synchronized static void resultSetValueWithType(Object vobj,PreparedStatement ps,int pindex,ISQLProvider sqlprovider) throws SQLException {
		resultSetValueWithType(vobj,ps,pindex,sqlprovider,null);
	}	
	
	protected final GregorianCalendar m_cal = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
	protected int prepareStatement(PreparedStatement pstmt, Object[] fields,ISQLProvider sqlprovider) throws SQLException
	{ 
		// put in all the fields
		int pos = 1;
		if ((fields != null) && (fields.length > 0))
		{
			for (int i = 0; i < fields.length; i++)
			{
				if (fields[i] == null || (fields[i] instanceof String && ((String) fields[i]).length() == 0))
				{  
					 pstmt.setObject(pos, null); 
					pos++;
				}
				else if (fields[i] instanceof Time)
				{
					Time t = (Time) fields[i]; 
					pstmt.setTimestamp(pos, new Timestamp(t.getTime()), m_cal);
					pos++;
				}
				//KNL-558 an obvious one
				else if (fields[i] instanceof java.util.Date)
				{
					java.util.Date d = (java.util.Date) fields[i];
					pstmt.setTimestamp(pos, new Timestamp(d.getTime()), m_cal);
					pos++;
				}
				else if (fields[i] instanceof Long)
				{
					long l = ((Long) fields[i]).longValue();
					pstmt.setLong(pos, l);
					pos++;
				}
				else if (fields[i] instanceof Integer)
				{
					int n = ((Integer) fields[i]).intValue();
					pstmt.setInt(pos, n);
					pos++;
				}
				else if (fields[i] instanceof Float)
				{
					float f = ((Float) fields[i]).floatValue();
					pstmt.setFloat(pos, f);
					pos++;
				}
				else if (fields[i] instanceof Boolean)
				{
					pstmt.setBoolean(pos, ((Boolean) fields[i]).booleanValue());
					pos++;
				}
				else if ( fields[i] instanceof byte[] ) 
				{
					pstmt.setBytes(pos, (byte[])fields[i] );
					pos++;
				}

				// %%% support any other types specially?
				else
				{
					String value = fields[i].toString();  
					if(sqlprovider instanceof  MYSQLProvider){
						try
						{
							pstmt.setBytes(pos, value.getBytes("UTF-8"));
						}
						catch (UnsupportedEncodingException ex)
						{ 
							throw new SQLException(ex.getMessage());
						}
					}else{
						pstmt.setCharacterStream(pos, new StringReader(value), value.length());
					}
					pos++;
				}
			}
		}

		return pos;
	}
	
}
