package org.chuniter.core.kernel.annotation;

@java.lang.annotation.Target(value={java.lang.annotation.ElementType.METHOD,java.lang.annotation.ElementType.FIELD})
@java.lang.annotation.Retention(value=java.lang.annotation.RetentionPolicy.RUNTIME)
public @interface EXID{
	public String name() default "id";
	public String type() default "varchar";
	public boolean isauto() default false;
}
