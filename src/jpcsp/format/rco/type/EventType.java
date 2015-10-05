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
package jpcsp.format.rco.type;

import jpcsp.format.rco.RCOContext;
import jpcsp.format.rco.object.BaseObject;

public class EventType extends BaseReferenceType {
	private String event;
	private BaseObject object;

	@Override
	public void read(RCOContext context) {
		super.read(context);

		if (referenceType == REFERENCE_TYPE_EVENT) {
			event = context.events.get(value);
		} else if (referenceType == REFERENCE_TYPE_OBJECT) {
			object = context.objects.get(value);
		} else if (referenceType != REFERENCE_TYPE_NONE) {
			log.warn(String.format("EventType unknown referenceType 0x%X", referenceType));
		}
	}

	public String getEvent() {
		return event;
	}

	public BaseObject getObject() {
		return object;
	}

	@Override
	public String toString() {
		if (event != null) {
			return String.format("%s, event='%s'", super.toString(), event);
		}
		if (object != null) {
			return String.format("%s, object='%s'", super.toString(), object);
		}
		return super.toString();
	}
}
