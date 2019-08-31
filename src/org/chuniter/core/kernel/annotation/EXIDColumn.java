package org.chuniter.core.kernel.annotation;

@java.lang.annotation.Target(value={java.lang.annotation.ElementType.METHOD,java.lang.annotation.ElementType.FIELD})
@java.lang.annotation.Retention(value=java.lang.annotation.RetentionPolicy.RUNTIME)
public @interface EXIDColumn{
	public boolean autoCreate() default false;
	public boolean cansetvalue() default true;
}
