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

import jpcsp.Allegrex.compiler.Compiler;

/**
 * @author gid15
 *
 */
public class Sleep extends AbstractNativeCodeSequence {
	static public void call(int regDoubleLow, int regDoubleHigh) {
		int doubleLow = getRegisterValue(regDoubleLow);
		int doubleHigh = getRegisterValue(regDoubleHigh);
		Double sleepSeconds = Double.longBitsToDouble(getLong(doubleLow, doubleHigh));

		Compiler.log.info("Sleeping " + sleepSeconds + " s");

		try {
            Thread.sleep((long) (sleepSeconds * 1000));
        } catch (InterruptedException e) {
        	// Ignore exception
        }
	}
}
