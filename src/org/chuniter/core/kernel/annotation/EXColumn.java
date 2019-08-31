package org.chuniter.core.kernel.annotation;


@java.lang.annotation.Target(value={java.lang.annotation.ElementType.METHOD,java.lang.annotation.ElementType.FIELD})
@java.lang.annotation.Retention(value=java.lang.annotation.RetentionPolicy.RUNTIME)
public @interface EXColumn{
	
	public final int UNIQUE = 1;
	
	public boolean isTransient() default false;
	public String type() default "";
	public String utype() default "";
	public int length() default 100;
	public boolean distinct() default false;
	public String desc() default "";
	public String title() default "";
	public String style() default "";
	public String titleStyle() default "";
	public boolean keysearch() default false;
	public int index() default 0;
	public String[] vlis() default ""; 
	public boolean showtable() default true;//是否显示在表格，不同的表格设置方案不一样
	public boolean searchMust() default false;//是否是必须查询的,如果为true，则生成的查询SQL中必须包括此属性
	public boolean showedit() default true;
	public boolean updateable() default true;//是否可以修改
	public String validateType() default ""; 
	public int serverValidateType() default 0; //服务端检测类型
	public boolean isrequire() default false; 
	public String dateFormat() default "yyyy-MM-dd";
	public String validateErrorMsg() default "此项为必输或格式错误";
	public String defaultValue() default "";
	public int min() default 0;
	public int max() default 1;
	public String aufillId() default "";
	public boolean frozen() default false;
	public String cussql() default "";//自定义属性sql,默认是属性名，如select name from user;如果name的cussql注解为 xxx(name),其中xxx是某一函数，则最终的sql为select xxx(name) from user;2015-10-20 16:15 youg

	public String exportName() default "";
	//分类编码属性
	public String parentItemId() default "";
	public String group() default "";
	//通用模块数据选择
	public String dataUrl() default "";
	//用于执行JS代码
	public String script() default "";
	//是否可编辑
	public boolean editable() default true;
	//是否禁用
	public boolean disable() default false;
	//排序名
	public String sortName() default "";
	//排序方式
	public String sortType() default "desc";
	//UI展示的GET方法，用于一些属性在显示时要通过计算得出的情况
	public String showGetMethod() default "";
	//单位符号
	public String usymbol() default "";
	//是否可以导入
	public boolean impable() default true;
	//字段名，用于属性名与数据库名不一致时设置。
	public String name() default "";
	//在数据类型为选择时，是否可以多选
	public boolean multipleChoose() default false;
}
