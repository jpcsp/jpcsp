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
package jpcsp.format.rco.vsmx.interpreter;

import jpcsp.format.rco.vsmx.INativeFunction;

public class VSMXNativeFunction extends VSMXFunction {
	private INativeFunction nativeFunction;
	private VSMXBaseObject returnValue;
	private VSMXBaseObject arguments[];

	public VSMXNativeFunction(VSMXInterpreter interpreter, INativeFunction nativeFunction) {
		super(interpreter, nativeFunction.getArgs(), 0, -1);
		this.nativeFunction = nativeFunction;
		arguments = new VSMXBaseObject[nativeFunction.getArgs() + 1];
	}

	@Override
	public void call(VSMXCallState callState) {
		arguments[0] = callState.getThisObject();
		for (int i = 1; i < arguments.length; i++) {
			arguments[i] = callState.getLocalVar(i);
		}
		returnValue = nativeFunction.call(arguments);
	}

	@Override
	public VSMXBaseObject getReturnValue() {
		return returnValue;
	}

	@Override
	public String toString() {
		return String.format("VSMXNativeFunction[%s, returnValue=%s]", nativeFunction, returnValue);
	}
}
