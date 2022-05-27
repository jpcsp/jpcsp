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
 * Annotation for the TPointer type, giving indications on the length
 * of the buffer and if the buffer is used as input and/or input.
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface BufferInfo {
	public static final Usage defaultUsage = Usage.unknown;
	public static final LengthInfo defaultLengthInfo = LengthInfo.unknown;
	public static final int defaultLength = -1;
	public static final int defaultMaxDumpLength = -1;

	public static enum LengthInfo {
		unknown,
		nextParameter,
		nextNextParameter,
		previousParameter,
		variableLength,
		fixedLength,
		returnValue
	}
	public static enum Usage {
		unknown,
		in,
		out,
		inout
	}

	public LengthInfo lengthInfo() default LengthInfo.unknown;

	public int length() default defaultLength;

	public Usage usage() default Usage.unknown;

	public int maxDumpLength() default defaultMaxDumpLength;
}
