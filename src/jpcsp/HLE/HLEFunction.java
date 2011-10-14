/*
This file is part of jpcsp.

Jpcsp is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

Jpcsp is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with Jpcsp.  If not, see <http://www.gnu.org/licenses/>.
 */
package jpcsp.HLE;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

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
	 * Checks if the cpu is inside an interrupt and if so, 
	 * raises SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT. 
	 */
	public boolean checkInsideInterrupt() default false;

	/**
	 * Checks if the dispatch thread is enabled and if disabled, 
	 * raises SceKernelErrors.ERROR_KERNEL_WAIT_CAN_NOT_WAIT. 
	 */
	public boolean checkDispatchThreadEnabled() default false;

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
