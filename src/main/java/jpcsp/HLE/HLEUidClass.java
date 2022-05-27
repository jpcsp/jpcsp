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
