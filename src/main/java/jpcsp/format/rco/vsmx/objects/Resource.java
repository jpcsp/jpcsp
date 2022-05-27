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
package jpcsp.format.rco.vsmx.objects;

import jpcsp.format.RCO;
import jpcsp.format.RCO.RCOEntry;
import jpcsp.format.rco.Display;
import jpcsp.format.rco.vsmx.interpreter.VSMXBaseObject;
import jpcsp.format.rco.vsmx.interpreter.VSMXInterpreter;
import jpcsp.format.rco.vsmx.interpreter.VSMXNativeObject;
import jpcsp.format.rco.vsmx.interpreter.VSMXObject;

public class Resource extends BaseNativeObject {
	public static final String objectName = "resource";
	public static final String rootName = "root";
	public static final String childrenName = "children";
	public static final String textureName = "texture";

	public static VSMXNativeObject create(VSMXInterpreter interpreter, Display display, VSMXNativeObject vsmxController, RCOEntry mainTable) {
		Resource resource = new Resource(interpreter);
		VSMXNativeObject object = new VSMXNativeObject(interpreter, resource);
		resource.setObject(object);

		Controller controller = (Controller) vsmxController.getObject();
		createTable(interpreter, display, controller, object, mainTable, RCO.RCO_TABLE_OBJ, "pagetable");
		createTable(interpreter, display, controller, object, mainTable, RCO.RCO_TABLE_ANIM, "animtable");
		createTable(interpreter, display, controller, object, mainTable, RCO.RCO_TABLE_SOUND, "soundtable");
		createTable(interpreter, display, controller, object, mainTable, RCO.RCO_TABLE_IMG, "texturetable");

		return object;
	}

	private Resource(VSMXInterpreter interpreter) {
	}

	private static RCOEntry[] findEntries(RCOEntry mainTable, int id) {
		for (RCOEntry entry : mainTable.subEntries) {
			if (entry.id == id) {
				return entry.subEntries;
			}
		}

		return null;
	}

	private static void createObjectFromEntry(VSMXInterpreter interpreter, Display display, Controller controller, VSMXBaseObject parent, RCOEntry entry) {
		if (entry.obj == null) {
			return;
		}

		VSMXBaseObject object;
		if (entry.obj != null) {
			entry.obj.setDisplay(display);
			entry.obj.setController(controller);
			object = entry.obj.createVSMXObject(interpreter, parent, entry);
		} else {
			object = parent;
		}

		if (entry.subEntries != null) {
			VSMXObject children = new VSMXObject(interpreter, null);
			object.setPropertyValue(childrenName, children);
			for (int i = 0; i < entry.subEntries.length; i++) {
				RCOEntry child = entry.subEntries[i];
				if (child.label == null) {
					child.label = String.format("%04d", i);
				}
				createObjectFromEntry(interpreter, display, controller, children, child);
			}
		}
	}

	private static void createTable(VSMXInterpreter interpreter, Display display, Controller controller, VSMXObject parent, RCOEntry mainTable, int id, String name) {
		VSMXBaseObject table = new VSMXObject(interpreter, null);
		parent.setPropertyValue(name, table);

		RCOEntry entries[] = findEntries(mainTable, id);

		if (entries != null) {
			for (RCOEntry entry : entries) {
				createObjectFromEntry(interpreter, display, controller, table, entry);
			}
		}
	}
}
