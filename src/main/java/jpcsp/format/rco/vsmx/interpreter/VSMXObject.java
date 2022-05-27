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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class VSMXObject extends VSMXBaseObject {
	protected final Map<String, VSMXBaseObject> properties;
	private String className;
	private final List<String> sortedPropertyNames;

	public VSMXObject(VSMXInterpreter interpreter, String className) {
		super(interpreter);
		this.className = className;
		properties = new HashMap<String, VSMXBaseObject>();
		sortedPropertyNames = new LinkedList<String>();
	}

	private void addProperty(String name, VSMXBaseObject value) {
		properties.put(name, value);
		sortedPropertyNames.add(name);
	}

	protected static int getIndex(String name) {
		int index;
		try {
			index = Integer.parseInt(name);
		} catch (NumberFormatException e) {
			index = -1;
		}

		return index;
	}

	@Override
	public VSMXBaseObject getPropertyValue(String name) {
		if (lengthName.equals(name)) {
			return new VSMXNumber(interpreter, properties.size());
		}

		if (prototypeName.equals(name)) {
			VSMXObject prototype = getPrototype();
			if (prototype != null) {
				return prototype;
			}
			return VSMXUndefined.singleton;
		}

		VSMXBaseObject value = properties.get(name);
		if (value == null) {
			int index = getIndex(name);
			if (index >= 0) {
				if (index < properties.size()) {
					value = properties.get(sortedPropertyNames.get(index));
				} else {
					value = VSMXUndefined.singleton;
				}
			} else {
				VSMXObject prototype = getPrototype();
				if (prototype != null && prototype.properties.containsKey(name)) {
					value = prototype.getPropertyValue(name);
				} else {
					value = VSMXUndefined.singleton;
					addProperty(name, value);
				}
			}
		}

		return value;
	}

	@Override
	public List<String> getPropertyNames() {
		return sortedPropertyNames;
	}

	@Override
	public void setPropertyValue(String name, VSMXBaseObject value) {
		if (properties.containsKey(name)) {
			properties.put(name, value);
		} else {
			addProperty(name, value);
		}
	}

	@Override
	public void deletePropertyValue(String name) {
		properties.remove(name);
		sortedPropertyNames.remove(name);
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
		return false;
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
