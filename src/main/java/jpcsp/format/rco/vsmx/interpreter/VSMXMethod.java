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
import jpcsp.format.rco.vsmx.objects.NativeFunctionFactory;

public class VSMXMethod extends VSMXBaseObject {
	private VSMXBaseObject object;
	private String name;
	private VSMXBaseObject thisObject;
	private int numberOfArguments;
	private VSMXBaseObject[] arguments;

	public VSMXMethod(VSMXInterpreter interpreter, VSMXBaseObject object, String name) {
		super(interpreter);
		this.object = object;
		this.name = name;
	}

	public VSMXBaseObject getObject() {
		return object;
	}

	public String getName() {
		return name;
	}

	public VSMXBaseObject getThisObject() {
		return thisObject;
	}

	public VSMXBaseObject[] getArguments() {
		return arguments;
	}

	public int getNumberOfArguments() {
		return numberOfArguments;
	}

	public VSMXFunction getFunction(int numberOfArguments, VSMXBaseObject[] arguments) {
		this.numberOfArguments = numberOfArguments;
		this.arguments = arguments;
		thisObject = object.getValue();

		if (object.hasPropertyValue(name)) {
			VSMXBaseObject function = object.getPropertyValue(name).getValue();
			if (function != null && function instanceof VSMXFunction) {
				return (VSMXFunction) function;
			}
		}

		if (object instanceof VSMXFunction && callName.equals(name)) {
			// The first argument of the "call()" function call is the "this" object.
			if (numberOfArguments > 0) {
				this.numberOfArguments--;
				if (arguments.length > 0) {
					thisObject = arguments[0];
					this.arguments = new VSMXBaseObject[this.numberOfArguments];
					System.arraycopy(arguments, 1, this.arguments, 0, this.numberOfArguments);
				}
			}

			return (VSMXFunction) object;
		}

		INativeFunction nativeFunction = null;
		if (object instanceof VSMXNativeObject) {
			VSMXNativeObject nativeObject = (VSMXNativeObject) object;
			nativeFunction = NativeFunctionFactory.getInstance().getNativeFunction(nativeObject, name, numberOfArguments);
		} else if (object instanceof VSMXBaseObject) {
			nativeFunction = NativeFunctionFactory.getInstance().getNativeFunction(object, name, numberOfArguments);
		}

		if (nativeFunction != null) {
			return new VSMXNativeFunction(interpreter, nativeFunction);
		}

		return null;
	}

	public void call(VSMXBaseObject arguments[]) {
	}

	@Override
	public String typeOf() {
		return "function";
	}

	@Override
	public String getClassName() {
		return name;
	}

	@Override
	public String toString() {
		return String.format("%s.%s()", object, name);
	}
}
