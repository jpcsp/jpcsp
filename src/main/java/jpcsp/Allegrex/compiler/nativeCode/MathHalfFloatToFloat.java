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
package jpcsp.Allegrex.compiler.nativeCode;

/**
 * @author gid15
 *
 */
public class MathHalfFloatToFloat extends AbstractNativeCodeSequence {
	static public void call() {
		int halfFloat = getGprA0();
		int e = (halfFloat >> 10) & 0x1F;

		if ((halfFloat & 0x7FFF) != 0) {
			e += 112;
		}

		setFprF0(Float.intBitsToFloat(((halfFloat & 0x8000) << 16) |
		                              (e << 23) |
		                              ((halfFloat & 0x03FF) << 13)
		                             ));
	}
}
