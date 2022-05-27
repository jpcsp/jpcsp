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
import java.util.Stack;

public class VSMXCallState {
	private VSMXBaseObject thisObject;
	private VSMXBaseObject[] localVariables;
	private Stack<VSMXBaseObject> stack;
	private int returnPc;
	private boolean returnThis;
	private boolean exitAfterCall;

	public VSMXCallState(VSMXBaseObject thisObject, int numberOfLocalVariables, int returnPc, boolean returnThis, boolean exitAfterCall) {
		this.thisObject = thisObject;
		localVariables = new VSMXBaseObject[numberOfLocalVariables];
		Arrays.fill(localVariables, VSMXUndefined.singleton);
		stack = new Stack<VSMXBaseObject>();
		this.returnPc = returnPc;
		this.returnThis = returnThis;
		this.exitAfterCall = exitAfterCall;
	}

	public int getReturnPc() {
		return returnPc;
	}

	public VSMXBaseObject getThisObject() {
		return thisObject;
	}

	public VSMXBaseObject getLocalVar(int i) {
		if (i <= 0 || i > localVariables.length) {
			return VSMXUndefined.singleton;
		}

		return localVariables[i - 1];
	}

	public void setLocalVar(int i, VSMXBaseObject value) {
		if (i > 0 && i <= localVariables.length) {
			localVariables[i - 1] = value;
		}
	}

	public Stack<VSMXBaseObject> getStack() {
		return stack;
	}

	public boolean getReturnThis() {
		return returnThis;
	}

	public boolean getExitAfterCall() {
		return exitAfterCall;
	}

	@Override
	public String toString() {
		StringBuilder s = new StringBuilder();

		s.append(String.format("CallState[returnPc=%d, this=%s", returnPc, thisObject));
		for (int i = 1; i <= localVariables.length; i++) {
			s.append(String.format(", var%d=%s", i, getLocalVar(i)));
		}
		s.append("]");

		return s.toString();
	}
}
