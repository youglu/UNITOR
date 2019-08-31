package org.chuniter.core.kernel.annotation;

@java.lang.annotation.Target(value={java.lang.annotation.ElementType.TYPE})
@java.lang.annotation.Retention(value=java.lang.annotation.RetentionPolicy.RUNTIME)
public @interface UEntity{

	String PS = "人事";
	String AT = "考勤";
	String SA = "薪酬";
	String SYS = "系统";

	public String name() default "";
	public String path() default "";
	public String entityTemplate() default "";
	public String title() default "";
	public String moduleName() default "";
	public String descript() default "";
	/**联合主键*/
	public String[] uniokey() default "";
}
