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
import jpcsp.format.rco.vsmx.interpreter.VSMXBaseObject;
import jpcsp.format.rco.vsmx.interpreter.VSMXNativeObject;
import jpcsp.format.rco.vsmx.interpreter.VSMXObject;

public class Resource extends BaseNativeObject {
	public static final String objectName = "resource";

	public static VSMXNativeObject create(RCOEntry mainTable) {
		Resource resource = new Resource();
		VSMXNativeObject object = new VSMXNativeObject(resource);
		resource.setObject(object);

		createTable(object, mainTable, RCO.RCO_TABLE_OBJ, "pagetable");
		createTable(object, mainTable, RCO.RCO_TABLE_ANIM, "animtable");
		createTable(object, mainTable, RCO.RCO_TABLE_SOUND, "soundtable");
		createTable(object, mainTable, RCO.RCO_TABLE_IMG, "texturetable");

		return object;
	}

	private static RCOEntry[] findEntries(RCOEntry mainTable, int id) {
		for (RCOEntry entry : mainTable.subEntries) {
			if (entry.id == id) {
				return entry.subEntries;
			}
		}

		return null;
	}

	private static void createObjectFromEntry(VSMXBaseObject parent, RCOEntry entry) {
		if (entry.label == null || entry.obj == null) {
			return;
		}

		VSMXBaseObject object;
		if (entry.obj != null) {
			object = entry.obj.createVSMXObject(parent, entry);
		} else {
			object = parent;
		}

		if (entry.subEntries != null) {
			for (RCOEntry subEntry : entry.subEntries) {
				createObjectFromEntry(object, subEntry);
			}
		}
	}

	private static void createTable(VSMXObject parent, RCOEntry mainTable, int id, String name) {
		VSMXBaseObject table = new VSMXObject();
		parent.setPropertyValue(name, table);

		RCOEntry entries[] = findEntries(mainTable, id);

		if (entries != null) {
			for (RCOEntry entry : entries) {
				createObjectFromEntry(table, entry);
			}
		}
	}
}
