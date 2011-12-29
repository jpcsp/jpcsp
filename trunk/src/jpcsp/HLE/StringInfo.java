package jpcsp.HLE;

//import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
//import java.lang.annotation.Target;

/**
* Annotation for a TPointer type, that indicates that the pointer can be null and that's not an error. 
*/
@Retention(RetentionPolicy.RUNTIME)
//@Target(ElementType.PARAMETER)
public @interface StringInfo {
	/**
	 * Maximum length that the will be readed from the memory address. 
	 */
	public int maxLength() default -1;
	
	/**
	 * Encoding of the string.
	 */
	public String encoding() default "UTF-8";
}
