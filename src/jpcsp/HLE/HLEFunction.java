package jpcsp.HLE;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

import jpcsp.HLE.kernel.types.SceKernelErrors;

/**
 * This annotation marks a function as a kernel function from a module.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface HLEFunction {
	/**
	 * Unique 32-bit identifier of the function for that module.
	 * Initially was the 32 last bits of the SHA1's function's name.
	 */
	public int nid();
	
	/**
	 * Specify if this is a special HLE function without NID. 
	 */
	public boolean syscall() default false;
	
	/**
	 * Checks if the cpu is inside an interrupt and if so, 
	 * raises SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT. 
	 */
	public boolean checkInsideInterrupt() default false;
	
	/**
	 * The minimum kernel version where this module function is found. 
	 */
	public int version() default 150;
	
	/**
	 * Name of the module. The default is the name of the class.
	 */
	public String moduleName() default "";

	/**
	 * Name of the function. The default is the name of the method.
	 */
	public String functionName() default "";
}
