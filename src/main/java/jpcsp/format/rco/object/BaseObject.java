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

import org.apache.log4j.Logger;

import jpcsp.Emulator;
import jpcsp.format.RCO.RCOEntry;
import jpcsp.format.rco.ObjectField;
import jpcsp.format.rco.RCOContext;
import jpcsp.format.rco.Display;
import jpcsp.format.rco.type.BaseType;
import jpcsp.format.rco.type.EventType;
import jpcsp.format.rco.vsmx.VSMX;
import jpcsp.format.rco.vsmx.interpreter.VSMXBaseObject;
import jpcsp.format.rco.vsmx.interpreter.VSMXInterpreter;
import jpcsp.format.rco.vsmx.interpreter.VSMXNativeObject;
import jpcsp.format.rco.vsmx.interpreter.VSMXString;
import jpcsp.format.rco.vsmx.objects.BaseNativeObject;
import jpcsp.format.rco.vsmx.objects.Controller;
import jpcsp.scheduler.Scheduler;

public abstract class BaseObject extends BaseNativeObject {
	protected Logger log = VSMX.log;
	protected Display display;
	protected Controller controller;
	private String name;

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

	private Field[] getFields() {
		return getClass().getFields();
	}

	private Field[] getSortedFields() {
		Field fields[] = getFields();

		// According the definition of getFields():
		//   The elements in the array returned are not sorted and are not in any particular order.
		// So now, we need to sort the fields according to the "ObjectField" annotation.
		Arrays.sort(fields, new FieldComparator());

		return fields;
	}

	public void read(RCOContext context) {
		Field[] fields = getSortedFields();
		for (Field field : fields) {
			if (BaseType.class.isAssignableFrom(field.getType())) {
				try {
					BaseType baseType = (BaseType) field.get(this);
					if (baseType == null) {
						baseType = (BaseType) field.getType().newInstance();
						field.set(this, baseType);
					}
					baseType.read(context);
				} catch (InstantiationException e) {
					// Ignore error
				} catch (IllegalAccessException e) {
					// Ignore error
				}
			}
		}
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

	public VSMXBaseObject createVSMXObject(VSMXInterpreter interpreter, VSMXBaseObject parent, RCOEntry entry) {
		VSMXNativeObject object = new VSMXNativeObject(interpreter, this);
		setObject(object);
		entry.vsmxBaseObject = object;
		if (entry.label != null) {
			name = entry.label;
			object.setPropertyValue("name", new VSMXString(interpreter, entry.label));
			parent.setPropertyValue(entry.label, object);
		}

		if (entry.parent != null && entry.parent.vsmxBaseObject instanceof VSMXNativeObject) {
			setParent(((VSMXNativeObject) entry.parent.vsmxBaseObject).getObject());
		}

		return object;
	}

	public void setDisplay(Display display) {
		this.display = display;
	}

	public void onDisplayUpdated() {
		display.repaint();
	}

	public void setController(Controller controller) {
		this.controller = controller;
	}

	protected static Scheduler getScheduler() {
		return Emulator.getScheduler();
	}

	public String getName() {
		return name;
	}

	protected void trigger(EventType event) {
		if (event.getEvent() != null) {
			controller.getInterpreter().interpretScript(getObject(), event.getEvent());
		} else if (event.getObject() != null && event.getObject() instanceof BasePositionObject) {
			((BasePositionObject) event.getObject()).setFocus();
		}
	}

	public void init(RCOContext context) {
		Field[] fields = getFields();
		for (Field field : fields) {
			if (BaseType.class.isAssignableFrom(field.getType())) {
				try {
					BaseType baseType = (BaseType) field.get(this);
					if (baseType != null) {
						baseType.init(context);
					}
				} catch (IllegalAccessException e) {
					// Ignore error
				}
			}
		}
	}

	protected void toString(StringBuilder s) {
	}

	@Override
	public String toString() {
		StringBuilder s = new StringBuilder();

		Field[] fields = getSortedFields();
		s.append(String.format("%s[name=%s", getClass().getSimpleName(), name));
		boolean firstField = false;
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
		toString(s);
		s.append("]");

		return s.toString();
	}
}
