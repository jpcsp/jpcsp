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

public class VSMXReference extends VSMXBaseObject {
	private VSMXObject refObject;
	private String refProperty;
	private int refIndex;

	public VSMXReference(VSMXInterpreter interpreter, VSMXObject refObject, String refProperty) {
		super(interpreter);
		this.refObject = refObject;
		this.refProperty = refProperty;
	}

	public VSMXReference(VSMXInterpreter interpreter, VSMXObject refObject, int refIndex) {
		super(interpreter);
		this.refObject = refObject;
		this.refIndex = refIndex;
	}

	public void assign(VSMXBaseObject value) {
		if (refProperty == null) {
			refObject.setPropertyValue(refIndex, value.getValue());
		} else {
			refObject.setPropertyValue(refProperty, value.getValue());
		}
	}

	public String getRefProperty() {
		if (refProperty == null) {
			return Integer.toString(refIndex);
		}

		return refProperty;
	}

	protected VSMXBaseObject getRef() {
		return getRef(0);
	}

	protected VSMXBaseObject getRef(int numberOfArguments) {
		if (refProperty == null) {
			return refObject.getPropertyValue(refIndex);
		}

		if (!refObject.hasPropertyValue(refProperty) && refObject instanceof VSMXNativeObject) {
			VSMXNativeObject nativeObject = (VSMXNativeObject) refObject;
			INativeFunction nativeFunction = NativeFunctionFactory.getInstance().getNativeFunction(nativeObject, refProperty, numberOfArguments);
			if (nativeFunction != null) {
				return new VSMXNativeFunction(interpreter, nativeFunction);
			}
		}

		return refObject.getPropertyValue(refProperty);
	}

	@Override
	public VSMXBaseObject getValue() {
		return getRef();
	}

	@Override
	public VSMXBaseObject getValueWithArguments(int numberOfArguments) {
		return getRef(numberOfArguments);
	}

	@Override
	public float getFloatValue() {
		return getRef().getFloatValue();
	}

	@Override
	public int getIntValue() {
		return getRef().getIntValue();
	}

	@Override
	public boolean getBooleanValue() {
		return getRef().getBooleanValue();
	}

	@Override
	public String getStringValue() {
		return getRef().getStringValue();
	}

	@Override
	public boolean equals(VSMXBaseObject value) {
		return getRef().equals(value);
	}

	@Override
	public VSMXBaseObject getPropertyValue(String name) {
		return getRef().getPropertyValue(name);
	}

	@Override
	public void setPropertyValue(String name, VSMXBaseObject value) {
		getRef().setPropertyValue(name, value);
	}

	@Override
	public void deletePropertyValue(String name) {
		getRef().deletePropertyValue(name);
	}

	@Override
	public void setFloatValue(float value) {
		getRef().setFloatValue(value);
	}

	@Override
	public String typeOf() {
		return getRef().typeOf();
	}

	@Override
	public String getClassName() {
		return getRef().getClassName();
	}

	@Override
	public String toString() {
		if (refProperty == null) {
			return String.format("@OBJ[%d]=%s", refIndex, getRef());
		}
		return String.format("@OBJ.%s=%s", refProperty, getRef());
	}
}
