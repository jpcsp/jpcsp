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

import java.util.LinkedList;
import java.util.List;

import jpcsp.format.rco.vsmx.VSMX;

import org.apache.log4j.Logger;

public abstract class VSMXBaseObject {
	public static final Logger log = VSMX.log;
	protected static final String lengthName = "length";
	protected static final String prototypeName = "prototype";
	protected static final String callName = "call";
	protected VSMXInterpreter interpreter;

	public VSMXBaseObject(VSMXInterpreter interpreter) {
		setInterpreter(interpreter);
	}

	protected void setInterpreter(VSMXInterpreter interpreter) {
		this.interpreter = interpreter;
	}

	public VSMXInterpreter getInterpreter() {
		return interpreter;
	}

	public VSMXBaseObject getValue() {
		return this;
	}

	public VSMXBaseObject getValueWithArguments(int numberOfArguments) {
		return getValue();
	}

	public float getFloatValue() {
		return 0f;
	}

	public int getIntValue() {
		return (int) getFloatValue();
	}

	public boolean getBooleanValue() {
		return getFloatValue() != 0f;
	}

	public String getStringValue() {
		return toString();
	}

	public boolean equals(VSMXBaseObject value) {
		return getFloatValue() == value.getFloatValue();
	}

	public boolean identity(VSMXBaseObject value) {
		return this == value;
	}

	public VSMXBaseObject getPropertyValue(String name) {
		if (prototypeName.equals(name)) {
			VSMXObject prototype = getPrototype();
			if (prototype != null) {
				return prototype;
			}
		}
		return VSMXUndefined.singleton;
	}

	public VSMXBaseObject getPropertyValue(int index) {
		return getPropertyValue(Integer.toString(index));
	}

	public void setPropertyValue(String name, VSMXBaseObject value) {
	}

	public void setPropertyValue(int index, VSMXBaseObject value) {
		setPropertyValue(Integer.toString(index), value);
	}

	public void deletePropertyValue(String name) {
		setPropertyValue(name, VSMXUndefined.singleton);
	}

	public void deletePropertyValue(int index) {
		deletePropertyValue(Integer.toString(index));
	}

	public boolean hasPropertyValue(String name) {
		return !VSMXUndefined.singleton.equals(getPropertyValue(name));
	}

	public List<String> getPropertyNames() {
		return new LinkedList<String>();
	}

	public void setFloatValue(float value) {
	}

	public abstract String typeOf();

	public abstract String getClassName();

	protected VSMXObject getPrototype() {
		String className = getClassName();
		if (className == null) {
			return null;
		}

		if (!interpreter.getGlobalVariables().hasPropertyValue(className)) {
			return null;
		}

		VSMXBaseObject classObject = interpreter.getGlobalVariables().getPropertyValue(className).getPrototype();
		if (!(classObject instanceof VSMXObject)) {
			classObject = new VSMXObject(interpreter, className);
			interpreter.getGlobalVariables().setPropertyValue(className, classObject);
		}

		return (VSMXObject) classObject;
	}

	public VSMXBaseObject toString(VSMXBaseObject object) {
		return new VSMXString(getInterpreter(), getStringValue());
	}

	public VSMXBaseObject toString(VSMXBaseObject object, VSMXBaseObject radix) {
		String s = Integer.toString(getIntValue(), radix.getIntValue());
		return new VSMXString(getInterpreter(), s);
	}

	@Override
	public String toString() {
		if (getFloatValue() == (float) getIntValue()) {
			return Integer.toString(getIntValue());
		}
		return Float.toString(getFloatValue());
	}
}
