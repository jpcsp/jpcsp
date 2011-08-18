package jpcsp.HLE;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This will register the class as a class that will be serialized as UIDs. 
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface HLEUidClass {
	/**
	 * Method name of the module class, without parameter, returning an int, that will generate
	 * an ID for this annotated class.
	 */
	public String moduleMethodUidGenerator() default "";
	
	/**
	 * Error code that will be returned or stored in a TErrorPointer when UID not found for this
	 * annotated class. 
	 */
	public int errorValueOnNotFound() default 0;
}
