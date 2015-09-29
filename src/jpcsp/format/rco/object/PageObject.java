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

import jpcsp.format.RCO.RCOEntry;
import jpcsp.format.rco.ObjectField;
import jpcsp.format.rco.type.EventType;
import jpcsp.format.rco.type.IntType;
import jpcsp.format.rco.vsmx.interpreter.VSMXBaseObject;
import jpcsp.format.rco.vsmx.interpreter.VSMXInterpreter;
import jpcsp.format.rco.vsmx.interpreter.VSMXObject;
import jpcsp.format.rco.vsmx.objects.Resource;

public class PageObject extends BaseObject {
	@ObjectField(order = 1)
	public IntType unknownInt0;
	@ObjectField(order = 2)
	public EventType onInit;
	@ObjectField(order = 3)
	public EventType onCancel;
	@ObjectField(order = 4)
	public EventType onContextMenu;
	@ObjectField(order = 5)
	public EventType onActivate;

	@Override
	public VSMXBaseObject createVSMXObject(VSMXInterpreter interpreter, VSMXBaseObject parent, RCOEntry entry) {
		VSMXBaseObject object = super.createVSMXObject(interpreter, parent, entry);

		VSMXObject root = new VSMXObject(interpreter, null);
		object.setPropertyValue(Resource.rootName, root);

		return root;
	}

	public VSMXBaseObject open(VSMXBaseObject object) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("PageObject.open"));
		}

		return object;
	}

	public VSMXBaseObject activate(VSMXBaseObject object) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("PageObject.activate"));
		}

		return object;
	}
}
