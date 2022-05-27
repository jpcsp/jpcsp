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

public class VSMXBoolean extends VSMXBaseObject {
	public static final VSMXBoolean singletonTrue = new VSMXBoolean(true);
	public static final VSMXBoolean singletonFalse = new VSMXBoolean(false);
	private boolean value;

	private VSMXBoolean(boolean value) {
		super(null);
		this.value = value;
	}

	public static void init(VSMXInterpreter interpreter) {
		singletonTrue.setInterpreter(interpreter);
		singletonFalse.setInterpreter(interpreter);
	}

	public static VSMXBoolean getValue(boolean value) {
		return value ? singletonTrue : singletonFalse;
	}

	public static VSMXBoolean getValue(int value) {
		return getValue(value != 0);
	}

	@Override
	public float getFloatValue() {
		return value ? 1f : 0f;
	}

	@Override
	public int getIntValue() {
		return value ? 1 : 0;
	}

	@Override
	public boolean getBooleanValue() {
		return value;
	}

	@Override
	public String typeOf() {
		return "boolean";
	}

	@Override
	public String getClassName() {
		return "Boolean";
	}

	@Override
	public String toString() {
		return value ? "true" : "false";
	}
}
