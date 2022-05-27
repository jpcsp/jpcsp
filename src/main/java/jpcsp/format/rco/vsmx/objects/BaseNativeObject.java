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

import org.apache.log4j.Logger;

import jpcsp.format.rco.vsmx.VSMX;
import jpcsp.format.rco.vsmx.interpreter.VSMXBaseObject;
import jpcsp.format.rco.vsmx.interpreter.VSMXFunction;
import jpcsp.format.rco.vsmx.interpreter.VSMXInterpreter;
import jpcsp.format.rco.vsmx.interpreter.VSMXObject;

public class BaseNativeObject {
	protected static Logger log = VSMX.log;
	private VSMXObject object;
	private BaseNativeObject parent;

	public VSMXObject getObject() {
		return object;
	}

	public void setObject(VSMXObject object) {
		this.object = object;
	}

	public BaseNativeObject getParent() {
		return parent;
	}

	public void setParent(BaseNativeObject parent) {
		this.parent = parent;
	}

	public void callCallback(VSMXInterpreter interpreter, String name, VSMXBaseObject[] arguments) {
		VSMXBaseObject function = getObject().getPropertyValue(name);
		if (function instanceof VSMXFunction) {
			if (log.isDebugEnabled()) {
				log.debug(String.format("callCallback %s, arguments=%s", name, arguments));
			}

			interpreter.interpretFunction((VSMXFunction) function, null, arguments);
		}
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}
}
