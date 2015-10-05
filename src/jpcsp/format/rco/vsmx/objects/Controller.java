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

import jpcsp.format.rco.object.BasePositionObject;
import jpcsp.format.rco.vsmx.VSMX;
import jpcsp.format.rco.vsmx.interpreter.VSMXArray;
import jpcsp.format.rco.vsmx.interpreter.VSMXBaseObject;
import jpcsp.format.rco.vsmx.interpreter.VSMXInterpreter;
import jpcsp.format.rco.vsmx.interpreter.VSMXNativeObject;
import jpcsp.format.rco.vsmx.interpreter.VSMXNull;

public class Controller extends BaseNativeObject {
	private static final Logger log = VSMX.log;
	public static final String objectName = "controller";
	private VSMXBaseObject userData;
	private String resource; // E.g. "EN100000"
	private BasePositionObject focus;
	private VSMXInterpreter interpreter;

	public static VSMXNativeObject create(VSMXInterpreter interpreter, String resource) {
		Controller controller = new Controller(interpreter);
		VSMXNativeObject object = new VSMXNativeObject(interpreter, controller);
		controller.setObject(object);
		controller.resource = resource;

		// Callbacks
		object.setPropertyValue("onChangeResource", VSMXNull.singleton);
		object.setPropertyValue("onMenu", VSMXNull.singleton);
		object.setPropertyValue("onExit", VSMXNull.singleton);
		object.setPropertyValue("onAutoPlay", VSMXNull.singleton);
		object.setPropertyValue("onContinuePlay", VSMXNull.singleton);

		return object;
	}

	private Controller(VSMXInterpreter interpreter) {
		this.interpreter = interpreter;
		userData = new VSMXArray(interpreter);
	}

	public VSMXInterpreter getInterpreter() {
		return interpreter;
	}

	public VSMXBaseObject getUserData(VSMXBaseObject object) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("Controller.getUserData() returning %s", userData));
		}

		return userData;
	}

	public void setUserData(VSMXBaseObject object, VSMXBaseObject userData) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("Controller.setUserData(%s)", userData));
		}

		this.userData = userData;
	}

	public String getResource() {
		return resource;
	}

	public void changeResource(VSMXBaseObject object, VSMXBaseObject resource) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("Controller.changeResource(%s)", resource));
		}
		this.resource = resource.getStringValue();
	}

	public void setFocus(BasePositionObject focus) {
		this.focus = focus;
	}

	public void onUp() {
		if (log.isDebugEnabled()) {
			log.debug(String.format("Controller.onUp focus=%s", focus));
		}

		if (focus != null) {
			focus.onUp();
		}
	}

	public void onDown() {
		if (log.isDebugEnabled()) {
			log.debug(String.format("Controller.onDown focus=%s", focus));
		}

		if (focus != null) {
			focus.onDown();
		}
	}

	public void onLeft() {
		if (log.isDebugEnabled()) {
			log.debug(String.format("Controller.onLeft focus=%s", focus));
		}

		if (focus != null) {
			focus.onLeft();;
		}
	}

	public void onRight() {
		if (log.isDebugEnabled()) {
			log.debug(String.format("Controller.onRight focus=%s", focus));
		}

		if (focus != null) {
			focus.onRight();
		}
	}

	public void onPush() {
		if (log.isDebugEnabled()) {
			log.debug(String.format("Controller.onPush focus=%s", focus));
		}

		if (focus != null) {
			focus.onPush();
		}
	}
}
