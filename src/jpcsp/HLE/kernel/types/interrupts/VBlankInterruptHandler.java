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

import java.util.LinkedList;
import java.util.List;

import jpcsp.Emulator;
import jpcsp.HLE.kernel.managers.IntrManager;
import jpcsp.HLE.kernel.types.IAction;
import jpcsp.scheduler.Scheduler;

public class VBlankInterruptHandler extends AbstractInterruptHandler {
	private List<IAction> vblankActions = new LinkedList<IAction>();
	private List<IAction> vblankActionsOnce = new LinkedList<IAction>();

	@Override
	protected void executeInterrupt() {
		Scheduler scheduler = Emulator.getScheduler();

		// Re-schedule next VBLANK interrupt in 1/60 second
		scheduler.addAction(Scheduler.getNow() + IntrManager.VBLANK_SCHEDULE_MICROS, this);

		// Execute all the registered VBlank actions (each time)
		for (IAction action : vblankActions) {
			if (action != null) {
				action.execute();
			}
		}

		// Execute all the registered VBlank actions (once)
		for (IAction action : vblankActionsOnce) {
			if (action != null) {
				action.execute();
			}
		}
		vblankActionsOnce.clear();

		// Trigger VBLANK interrupt
		IntrManager.getInstance().triggerInterrupt(IntrManager.PSP_VBLANK_INTR, null, null);
	}

	public void addVBlankAction(IAction action) {
		vblankActions.add(action);
	}

	public boolean removeVBlankAction(IAction action) {
		return vblankActions.remove(action);
	}

	public void addVBlankActionOnce(IAction action) {
		vblankActionsOnce.add(action);
	}

	public boolean removeVBlankActionOnce(IAction action) {
		return vblankActionsOnce.remove(action);
	}
}
