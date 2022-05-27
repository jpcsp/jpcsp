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

import jpcsp.format.rco.vsmx.INativeFunction;
import jpcsp.format.rco.vsmx.VSMX;
import jpcsp.format.rco.vsmx.interpreter.VSMXBaseObject;
import jpcsp.format.rco.vsmx.interpreter.VSMXNull;

public abstract class BaseNativeFunction implements INativeFunction {
	public static final Logger log = VSMX.log;
	protected String objectName;

	protected BaseNativeFunction(String objectName) {
		this.objectName = objectName;
	}

	@Override
	public int getArgs() {
		return 0;
	}

	@Override
	public VSMXBaseObject call(VSMXBaseObject[] arguments) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("Calling %s", toString(arguments)));
		}
		return VSMXNull.singleton;
	}

	protected String toString(VSMXBaseObject[] arguments) {
		StringBuilder s = new StringBuilder();
		if (objectName != null) {
			s.append(String.format("%s.", objectName));
		}
		s.append(String.format("%s(", toString()));
		if (arguments != null) {
			int firstArgument = objectName != null ? 1 : 0;
			for (int i = firstArgument; i < arguments.length; i++) {
				if (i > firstArgument) {
					s.append(", ");
				}
				s.append(arguments[i].toString());
			}
		}
		s.append(")");

		return s.toString();
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}
}
