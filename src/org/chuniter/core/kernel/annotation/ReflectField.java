package org.chuniter.core.kernel.annotation;


@java.lang.annotation.Target(value={java.lang.annotation.ElementType.METHOD,java.lang.annotation.ElementType.FIELD})
@java.lang.annotation.Retention(value=java.lang.annotation.RetentionPolicy.RUNTIME)
public @interface ReflectField{
	public Class<?> entity();
	public String fieldName() default "id";//指定entity中的哪个属性对应关联
	public String selffieldName() default "";//本实体的关联属性,对于多个属性关联同一个实体时有用
	public String sourceFiledName() default "";//使用类是用哪个属性来关联此关联对象的，比如A类有个empid关联了employeeentity,然后在A里面声明emp属性为employeeentity类型，然后可设置此属性值为：emp .
	public String alias() default "";
	public String entityFullName() default "";//用于当一个单元引用另一个公共单时，如果实际表是实现单元的子类，则需要设置此属性为子类对应的表名
	public String targetShowFiled() default "name";
	public String joinType() default "left";//SQL连接类型，默认为left join
}
