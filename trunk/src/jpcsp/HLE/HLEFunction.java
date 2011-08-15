package jpcsp.HLE;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface HLEFunction {
	public int nid();
	public int implemented() default 1;
	public int syscall() default 0;
	public boolean checkInsideInterrupt() default false; 
	public int version() default 150;
	public String moduleName() default "";
	public String functionName() default "";
}
