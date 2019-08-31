package org.chuniter.core.kernel.impl.orm;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.chuniter.core.kernel.api.dao.IConnectionExecutor;
import org.chuniter.core.kernel.api.dao.IConnectionHandler;
import org.chuniter.core.kernel.api.orm.IORMControler;
import org.chuniter.core.kernel.api.orm.ISQLProvider;
import org.chuniter.core.kernel.api.orm.ISimpORM;
import org.chuniter.core.kernel.kernelunit.AnnotationParser;
import org.chuniter.core.kernel.kernelunit.ReflectUtil;
import org.chuniter.core.kernel.kernelunit.StringUtil;

public class ORMControler implements IORMControler{
	
	 
	private ThreadLocal<ISimpORM<?>> simpOrmThread = new ThreadLocal<ISimpORM<?>>();
	private Map<String,EntityMapping> empm = new HashMap<String,EntityMapping>();	
	protected Log log = LogFactory.getLog(super.getClass());
	private ReflectUtil refu =ReflectUtil.getInstance();
	private static ORMControler ormControler;
	private IConnectionHandler connectionHandler = null;
	
	//private connectionThreadChecker ctc = new connectionThreadChecker();
	//private Thread checkThread = null;
	private static int MAXTRYTIMES = 5; 
	
	private ORMControler(){
		this(null);
	}
	private ORMControler(ISimpORM<?> simpORM){
		simpOrmThread.set(simpORM);
	}
	public synchronized void upSimpORM(ISimpORM<?> simpORM){
		simpOrmThread.set(simpORM);
	}
	public synchronized static ORMControler getInstance(ISimpORM<?> simpORM){
		if(null == ormControler)
			ormControler = new ORMControler(simpORM);
		else
			ormControler.upSimpORM(simpORM);
		return ormControler;
	}
	@Override
	public EntityMapping fetchMappingInfo(final ISimpORM<?> simpORM) throws Exception{
		simpOrmThread.set(simpORM);
		try{
			connectionHandler = simpORM.getConnection();
			if(null == connectionHandler)
				connectionHandler = simpORM.getDefultConnection();
		}catch(Exception e){
			connectionHandler = simpORM.getDefultConnection();
		}
		if(null == connectionHandler)
			return null;
		return regeditEntity(simpORM.getClazz(),connectionHandler);

	}
	@Override
	public EntityMapping fetchMappingInfo(final ISimpORM<?> simpORM,Class<?> clazz) throws Exception{
		simpOrmThread.set(simpORM);
		try{
			connectionHandler = simpOrmThread.get().getConnection();
			if(null == connectionHandler)
				connectionHandler = simpOrmThread.get().getDefultConnection();
		}catch(Exception e){
			connectionHandler = simpOrmThread.get().getDefultConnection();
		}
		return regeditEntity(clazz,connectionHandler);

	}
	public EntityMapping regeditEntity(final Class<?> t,final ISimpORM<?> simpORM) throws Exception{
		simpOrmThread.set(simpORM);
		try{
			connectionHandler = simpORM.getConnection();
		}catch(Exception e){
			//connectionHandler = simpORM.getDefultConnection();
			e.printStackTrace();
			throw e;
		}
		return regeditEntity(t,connectionHandler);

	}	
	public EntityMapping regeditEntity(final Class<?> t,IConnectionHandler conHandler) throws Exception{	
		 	if(null == conHandler){
		 		log.error("conHandler为空，无法处理实体映射!");
		 		return null;
		 	}
			Connection con = conHandler.getCon(); 
			EntityMapping newEmp = null;
			String tableName =  AnnotationParser.getNameFormEntity(t);
			try{ 
					/*while(null == con||con.isClosed()){
						if(MAXTRYTIMES>=5){
							System.err.println("经过最大次数的尝试，还是无法获得连接，将忽略数据库连接，应用继续运行!");
							break;
						}
						MAXTRYTIMES++;
						System.err.println("无法获得可用连接! 正进行第"+MAXTRYTIMES+"次尝试等待5秒后重新获得");
						Thread.currentThread().sleep(5000);
						con = conHandler.getCon(); 
					}
				if(con != null)
					MAXTRYTIMES = 0;
				 */
				if(null == con||con.isClosed()){
					System.err.println("无法获得连接，将忽略数据库连接，应用继续运行!"); 
					return null;
				}
				//System.out.println(tableName+"  ");
				//System.out.println(tableName+"=="+ ((EntityMapping)empm.get((tableName))).getEntityName()+"  "+(tableName == ((EntityMapping)empm.get((tableName))).getEntityName()));
				//System.out.println(((EntityMapping)empm.get((tableName))).getEntityClass().hashCode()+"   ==  "+t.hashCode()+"  "+(((EntityMapping)empm.get((tableName))).getEntityClass().hashCode()==t.hashCode()));
				if(empm.containsKey(tableName)&&((EntityMapping)empm.get((tableName))).getEntityClass().hashCode()==t.hashCode()){//empm.containsKey((t.getName()))&&((EntityMapping)empm.get((t.getName()))).getEntityClass().getClassLoader()==t.getClassLoader()&&t == ((EntityMapping)empm.get((t.getName()))).getEntityClass()){
					//看是否需要更新
					newEmp = (EntityMapping)empm.get((tableName));
					newEmp.changeSqlprovider(con.getMetaData().getDatabaseProductName());
					//检查实体的表是否存在数据库中。
					Statement st = con.createStatement();
					log.debug("是否存在检测sql："+newEmp.getEntityIsExistInDBSql());
					try{
						st.executeQuery(newEmp.getEntityIsExistInDBSql());
					}catch(SQLException e){
						//如发生异常，可判断是不是不存在表的异常，目前没有判断，直接认为是不存在表，在接下来就始创建。
						if(newEmp.getSqlprovider().parseSqlExceptionErrorCode(e)==ISQLProvider.TABLENOEXIST){
							throw e;
						}else
							//如果发生异常，有可能是因为不支持max(id)，换成count再次检测
							st.executeQuery(newEmp.getEntityIsExistInDBSqlWithCount());
					}
					//由于对于很多属性时，此刷新实体会很慢，故不对已映射好的实体进行刷新处理，这样实体的属性与长度就不能实时的同步到数据库了,但为了加速查询，得牺牲实时性了。 youg 2016-05-17 19:22
					//refreshMapping(t); 
					return newEmp;
				}
				newEmp = new EntityMapping(t);  
				newEmp.changeSqlprovider(con.getMetaData().getDatabaseProductName());
				empm.put(tableName, newEmp);
				//检查实体的表是否存在数据库中。
				Statement st = con.createStatement();
				ResultSet res = null;
				try{
					res = st.executeQuery(newEmp.getEntityIsExistInDBSql());
				}catch(SQLException rese){
					if(newEmp.getSqlprovider().parseSqlExceptionErrorCode(rese)==ISQLProvider.UNKNOWNCOLUMN){
						 throw rese;
					}
					res = st.executeQuery(newEmp.getEntityIsExistInDBSqlWithCount());
					//ignore this exception then check fileds and ceate field that is not exist;
				}catch(Exception rese){ 
					res = st.executeQuery(newEmp.getEntityIsExistInDBSqlWithCount());
					//ignore this exception then check fileds and ceate field that is not exist;
				}
				//如果存在则检查是否需要增加属性
				ResultSetMetaData md  = res.getMetaData();
				ExtendedField[] currentFields = Arrays.copyOf(newEmp.getFields(), newEmp.getFields().length);//refu.getAllField(t);
				//不对长度进行检测，可以通过 ResultSetMetaData getColumnDisplaySize 方法获得所取字段的长度
				Map<String,Integer> indbColumns = new HashMap<String,Integer>();
				for(int j=1;j<=md.getColumnCount();j++){ 
					indbColumns.put(md.getColumnLabel(j).toLowerCase(),0x1);
				} 
				for(int i=0;i<currentFields.length;i++){
					if(currentFields[i]!=null&&currentFields[i].needORM){
						if(!indbColumns.containsKey(currentFields[i].columnName.toLowerCase())){
							String addFieldSql = newEmp.getAddFieldForMappedClassSql(currentFields[i]); 
							log.debug("为实体添加新属性 "+currentFields[i].field.getName()+" \n"+addFieldSql);
							st.execute(addFieldSql);
						} 
					} 
				}
				currentFields = null;
				indbColumns.clear();
				indbColumns = null;  
			}catch(SQLException sqle){
				sqle.printStackTrace();
				log.debug(sqle.getMessage());
				//如发生异常，可判断是不是不存在表的异常，目前没有判断，直接认为是不存在表，在接下来就始创建。
				if(newEmp.getSqlprovider().parseSqlExceptionErrorCode(sqle)==ISQLProvider.TABLENOEXIST){
					creatEntityToDB(newEmp,t,con);
				}
			}catch(Exception e){
				e.printStackTrace();
			}finally{
				if(null != con){
					con.close();
					con = null;
				}
			} 
			return newEmp;
			 
	}
	private void creatEntityToDB(EntityMapping newEmp,final Class<?> t,Connection con){
		//如发生异常，可判断是不是不存在表的异常，目前没有判断，直接认为是不存在表，在接下来就始创建。
		String createSqls = newEmp.createTableWithClassSql();	
		try {
				log.debug(con.getAutoCommit()+"  "+"实体"+t+"在数据库中不存在表，开始为实体"+t.getClass()+" 创建数据库表.");
				Statement st = con.createStatement(); 
				if(StringUtil.isValid(createSqls)){
					for(String str:createSqls.split(";"))
						if(StringUtil.isValid(str))
							st.execute(str);
				}
				log.debug("为实体"+t+" 创建数据库表成功！");
				log.debug("实体"+t.getName()+" 已成功在 SimpORM 中进行映射。");
			} catch (SQLException e) {
				log.error("为实体"+t+" 创建数据库表失败！"+createSqls);
				e.printStackTrace();
			}
		
	}
	public void creatEntityToDB(EntityMapping newEmp,final String tableName,final Class<?> t,Connection con) throws Exception{
		//如发生异常，可判断是不是不存在表的异常，目前没有判断，直接认为是不存在表，在接下来就始创建。
			try {
				log.debug(con.getAutoCommit()+"  "+"实体"+t+"在数据库中不存在表，开始为实体"+tableName+" 创建数据库表.");
				Statement st = con.createStatement();
				String createSqls = newEmp.createTableWithClassSql(tableName, t);
				
				if(StringUtil.isValid(createSqls)){
					for(String str:createSqls.split(";"))
						if(StringUtil.isValid(str))
							st.execute(str);
				}
				log.debug("为实体"+t+" 创建数据库表成功！");
			} catch (SQLException e) {
				log.debug("为实体"+t+" 创建数据库表失败！");
				e.printStackTrace();
				throw e;
			}
		
	}	
	public EntityMapping regeditEntity(final Class<?> t) throws Exception{
		if(null == connectionHandler||null == simpOrmThread.get())
			connectionHandler = SimpORM.getDefultCon();
		else
			connectionHandler = simpOrmThread.get().getConnection();
		if(null == connectionHandler)
			return null;
		return regeditEntity(t,connectionHandler);
	}
	/**
	 * 更新实体映射
	 * @param c
	 * @throws Exception
	 */
	@Override
	public void refreshMapping(final Class<?> c)throws Exception{
		
		final EntityMapping newEmp = (EntityMapping)empm.get((AnnotationParser.getNameFormEntity(c)));
		if(null == newEmp){
			log.error("在refreshMapping中无法获得实体映射");
			throw new NullPointerException("在refreshMapping中无法获得实体映射");
		}
		try{
			connectionHandler = simpOrmThread.get().getConnection();
			if(null == connectionHandler)
				connectionHandler = simpOrmThread.get().getDefultConnection();
		}catch(Exception e){
			connectionHandler = simpOrmThread.get().getDefultConnection();
		}
		connectionHandler.proConnection(new IConnectionExecutor<Object>(){

			@Override
			public Object doConnection(Connection con)
					throws Exception {

				try{
					ExtendedField[] newFields = refu.getAllField(c);
					//不对长度进行检测，可以通过 ResultSetMetaData getColumnDisplaySize 方法获得所取字段的长度
					Map<String,Integer> indbColumns = new HashMap<String,Integer>();
					for(int j=0;j<newFields.length;j++){ 
						indbColumns.put(newFields[j].columnName,0x1);
					}  
					ExtendedField[] currentFields = Arrays.copyOf(newEmp.getFields(), newEmp.getFields().length);
					for(int i=0;i<currentFields.length;i++){
						if(currentFields[i]!=null&&currentFields[i].needORM){
							if(!indbColumns.containsKey(currentFields[i].columnName)){
								String addFieldSql = newEmp.getAddFieldForMappedClassSql(currentFields[i]);
								log.debug("为实体"+c+"添加新属性 "+newFields[i].field.getName()+" \n"+addFieldSql); 
								Statement st = con.createStatement();  					
								st.execute(addFieldSql);				
								log.debug("实体"+c+" 映射新增的属性完毕。");
							} 
						} 
					}
					currentFields = null;
					newFields = null;
					indbColumns.clear();
					indbColumns = null;  
					if(c != newEmp.getEntityClass())
						newEmp.updateEntityClass(c);
				}catch(Exception e){
					e.printStackTrace();
				}				
				return null;
				
			}}); 

	}
	@Override
	public String RefreshEntity(Object entity) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public String RefreshEntity(Class<?> clazz) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public void unRegeditEntity(Class<?> clazz) throws Exception {
		this.empm.remove(clazz.getName());
		this.empm.remove(clazz.getSimpleName());
	}
	/**
	 * 检测数据库连接
	 * @author youg
	 *
	 
	private class connectionThreadChecker implements Runnable{ 

		public Connection con = null;
		public IConnectionHandler conHandler;
		@Override
		public void run() { 
			try {
				while(null == con||con.isClosed()){
					log.warn("无法获得可用连接! 正尝试等待3秒后正获得");
					checkThread.sleep(3000);
					con = conHandler.getCon();
					if(con != null){
						try{con.close();con =null;}catch(Exception e){e.printStackTrace();}
						break;
					}
				}
			} catch ( Exception e) {
				e.printStackTrace();
			}
		} 
	}*/
	
	public static Class<?> getEntityByTableName(String tableName) {
		if(ormControler.empm.containsKey(tableName)) {
			EntityMapping empm = ormControler.empm.get(tableName);
			return empm.getEntityClass();
		}
		return null;
	}
	public static void main(String[] ar){
		ClassLoader cl = Object.class.getClassLoader(); 
		System.out.println(ORMControler.class.getClassLoader()==cl);
	}
}
