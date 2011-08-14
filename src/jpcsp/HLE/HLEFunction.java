package jpcsp.HLE;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface HLEFunction {
	public int nid();
	public int implemented() default 1;
	public int syscall() default 0; 
	public int version() default 150;
}
