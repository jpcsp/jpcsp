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
package jpcsp.format.rco.object;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Comparator;

import jpcsp.format.RCO.RCOEntry;
import jpcsp.format.rco.ObjectField;
import jpcsp.format.rco.type.BaseType;
import jpcsp.format.rco.vsmx.interpreter.VSMXBaseObject;
import jpcsp.format.rco.vsmx.interpreter.VSMXObject;

public abstract class BaseObject {
	private static class FieldComparator implements Comparator<Field> {
		@Override
		public int compare(Field f1, Field f2) {
			ObjectField o1 = f1.getAnnotation(ObjectField.class);
			ObjectField o2 = f2.getAnnotation(ObjectField.class);
			if (o1 == null || o2 == null) {
				return 0;
			}

			return o1.order() - o2.order();
		}
		
	}

	private Field[] getSortedFields() {
		Field fields[] = getClass().getFields();

		// According the definition of getFields():
		//   The elements in the array returned are not sorted and are not in any particular order.
		// So now, we need to sort the fields according to the "ObjectField" annotation.
		Arrays.sort(fields, new FieldComparator());

		return fields;
	}

	public int read(byte[] buffer, int offset) {
		Field[] fields = getSortedFields();
		for (Field field : fields) {
			if (BaseType.class.isAssignableFrom(field.getType())) {
				try {
					BaseType baseType = (BaseType) field.get(this);
					if (baseType == null) {
						baseType = (BaseType) field.getType().newInstance();
						field.set(this, baseType);
					}
					offset = baseType.read(buffer, offset);
				} catch (InstantiationException e) {
					// Ignore error
				} catch (IllegalAccessException e) {
					// Ignore error
				}
			}
		}

		return offset;
	}

	public int size() {
		int size = 0;
		Field[] fields = getSortedFields();
		for (Field field : fields) {
			if (BaseType.class.isAssignableFrom(field.getType())) {
				try {
					BaseType baseType = (BaseType) field.get(this);
					if (baseType == null) {
						baseType = (BaseType) field.getType().newInstance();
					}
					size += baseType.size();
				} catch (IllegalAccessException e) {
					// Ignore error
				} catch (InstantiationException e) {
					// Ignore error
				}
			}
		}

		return size;
	}

	public VSMXBaseObject createVSMXObject(VSMXBaseObject parent, RCOEntry entry) {
		VSMXObject object = new VSMXObject();
		parent.setPropertyValue(entry.label, object);

		return object;
	}

	@Override
	public String toString() {
		StringBuilder s = new StringBuilder();

		Field[] fields = getSortedFields();
		s.append(String.format("%s[", getClass().getSimpleName()));
		boolean firstField = true;
		for (Field field : fields) {
			if (BaseType.class.isAssignableFrom(field.getType())) {
				try {
					BaseType baseType = (BaseType) field.get(this);
					if (firstField) {
						firstField = false;
					} else {
						s.append(", ");
					}
					s.append(String.format("%s=(%s)", field.getName(), baseType));
				} catch (IllegalAccessException e) {
					// Ignore error
				}
			}
		}
		s.append("]");

		return s.toString();
	}
}
