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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import jpcsp.format.rco.vsmx.objects.BaseNativeObject;

public class VSMXNativeObject extends VSMXObject {
	private BaseNativeObject object;

	public VSMXNativeObject(VSMXInterpreter interpreter, BaseNativeObject object) {
		super(interpreter, null);
		this.object = object;
	}

	public BaseNativeObject getObject() {
		return object;
	}

	@Override
	public void setPropertyValue(String name, VSMXBaseObject value) {
		if (object != null && name != null && name.length() >= 1 && value instanceof VSMXNativeObject) {
			String setterName = "set" + Character.toUpperCase(name.charAt(0)) + name.substring(1);
			try {
				Method setterMethod = object.getClass().getMethod(setterName, BaseNativeObject.class);
				setterMethod.invoke(object, ((VSMXNativeObject) value).getObject());
				return;
			} catch (NoSuchMethodException e) {
				// Ignore exception
			} catch (SecurityException e) {
				// Ignore exception
			} catch (IllegalAccessException e) {
				// Ignore exception
			} catch (IllegalArgumentException e) {
				// Ignore exception
			} catch (InvocationTargetException e) {
				// Ignore exception
			}
		}

		super.setPropertyValue(name, value);
	}

	@Override
	protected void toString(StringBuilder s) {
		s.append(object.toString());
		super.toString(s);
	}
}
