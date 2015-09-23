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

import java.util.Arrays;

public class VSMXCallState {
	private VSMXBaseObject thisObject;
	private VSMXBaseObject[] localVariables;
	private int returnPc;

	public VSMXCallState(VSMXBaseObject thisObject, int numberOfLocalVariables, int returnPc) {
		this.thisObject = thisObject;
		localVariables = new VSMXBaseObject[numberOfLocalVariables];
		Arrays.fill(localVariables, VSMXUndefined.singleton);
		this.returnPc = returnPc;
	}

	public int getReturnPc() {
		return returnPc;
	}

	public VSMXBaseObject getThisObject() {
		return thisObject;
	}

	public VSMXBaseObject getLocalVar(int i) {
		if (i < 0 || i >= localVariables.length) {
			return VSMXUndefined.singleton;
		}

		return localVariables[i];
	}

	public void setLocalVar(int i, VSMXBaseObject value) {
		if (i >= 0 && i < localVariables.length) {
			localVariables[i] = value;
		}
	}
}
