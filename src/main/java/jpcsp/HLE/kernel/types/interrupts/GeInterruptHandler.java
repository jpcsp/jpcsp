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
package jpcsp.HLE.kernel.types.interrupts;

import jpcsp.HLE.kernel.managers.IntrManager;

public class GeInterruptHandler extends AbstractInterruptHandler {
	private GeCallbackInterruptHandler geCallbackInterruptHandler;
	private AfterGeCallbackAction afterGeCallbackAction;

	public GeInterruptHandler(GeCallbackInterruptHandler geCallbackInterruptHandler, int listId, int behavior, int id) {
		this.geCallbackInterruptHandler = geCallbackInterruptHandler;

		// Argument $a0 of GE callback is the signal/finish ID
		geCallbackInterruptHandler.setId(id);

		if (listId >= 0) {
			afterGeCallbackAction = new AfterGeCallbackAction(listId, behavior);
		} else {
			afterGeCallbackAction = null;
		}
	}

	@Override
	protected void executeInterrupt() {
		// Trigger GE interrupt: execute GeCallback
		IntrManager.getInstance().triggerInterrupt(IntrManager.PSP_GE_INTR, afterGeCallbackAction, null, geCallbackInterruptHandler);
	}
}
