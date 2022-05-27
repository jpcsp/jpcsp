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

public class VSMXFunction extends VSMXBaseObject {
	private int args;
	private int localVars;
	private int startLine;
	private VSMXObject prototype;

	public VSMXFunction(VSMXInterpreter interpreter, int args, int localVars, int startLine) {
		super(interpreter);
		this.args = args;
		this.localVars = localVars;
		this.startLine = startLine;
		prototype = new VSMXObject(interpreter, "Function");
	}

	public int getArgs() {
		return args;
	}

	public int getLocalVars() {
		return localVars;
	}

	public int getStartLine() {
		return startLine;
	}

	public void call(VSMXCallState callState) {
	}

	public VSMXBaseObject getReturnValue() {
		return null;
	}

	@Override
	public String typeOf() {
		return "function";
	}

	@Override
	public String getClassName() {
		return "Function";
	}

	@Override
	protected VSMXObject getPrototype() {
		return prototype;
	}

	@Override
	public String toString() {
		return String.format("Function(args=%d, localVars=%d, startLine=%d)", args, localVars, startLine);
	}
}
