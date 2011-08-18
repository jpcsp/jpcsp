package jpcsp.HLE;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface HLEUidClass {
	public String moduleMethodUidGenerator() default "";
	public int returnValueOnNotFound() default 0;
}
