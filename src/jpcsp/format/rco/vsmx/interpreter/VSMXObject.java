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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class VSMXObject extends VSMXBaseObject {
	protected final Map<String, VSMXBaseObject> properties;
	private String className;

	public VSMXObject(VSMXInterpreter interpreter, String className) {
		super(interpreter);
		this.className = className;
		properties = new HashMap<String, VSMXBaseObject>();
	}

	@Override
	public VSMXBaseObject getPropertyValue(String name) {
		if (prototypeName.equals(name)) {
			VSMXObject prototype = getPrototype();
			if (prototype != null) {
				return prototype;
			}
			return VSMXUndefined.singleton;
		}

		VSMXBaseObject value = properties.get(name);
		if (value == null) {
			VSMXObject prototype = getPrototype();
			if (prototype != null && prototype.properties.containsKey(name)) {
				value = prototype.getPropertyValue(name);
			} else {
				value = VSMXUndefined.singleton;
				properties.put(name, value);
			}
		}
		return value;
	}

	@Override
	public Set<String> getPropertyNames() {
		return properties.keySet();
	}

	@Override
	public void setPropertyValue(String name, VSMXBaseObject value) {
		properties.put(name, value);
	}

	@Override
	public void deletePropertyValue(String name) {
		properties.remove(name);
	}

	@Override
	public boolean hasPropertyValue(String name) {
		if (prototypeName.equals(name)) {
			return true;
		}

		if (properties.containsKey(name)) {
			return true;
		}

		VSMXObject prototype = getPrototype();
		if (prototype != null) {
			return prototype.hasPropertyValue(name);
		}

		return false;
	}

	@Override
	public boolean equals(VSMXBaseObject value) {
		if (value instanceof VSMXObject) {
			// Return true if both values refer to the same object
			return this == value;
		}
		return super.equals(value);
	}

	@Override
	public String typeOf() {
		return "object";
	}

	@Override
	public String getClassName() {
		return className;
	}

	@Override
	public String getStringValue() {
		if (hasPropertyValue("toString")) {
			log.warn(String.format("getStringValue on VSMXObject should be calling existing toString: %s", getPropertyValue("toString")));
		}
		return super.getStringValue();
	}

	protected void toString(StringBuilder s) {
		String[] keys = properties.keySet().toArray(new String[0]);
		Arrays.sort(keys);
		for (String key : keys) {
			VSMXBaseObject value = properties.get(key);
			if (s.length() > 1) {
				s.append(",\n");
			}
			s.append(String.format("%s=%s", key, value));
		}
	}

	@Override
	public String toString() {
		StringBuilder s = new StringBuilder();
		s.append("[");
		toString(s);
		s.append("]");

		return s.toString();
	}
}
