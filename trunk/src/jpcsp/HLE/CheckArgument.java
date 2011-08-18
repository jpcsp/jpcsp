package jpcsp.HLE;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
//@Target(ElementType.PARAMETER)
public @interface CheckArgument {
	String value() default "";
}
