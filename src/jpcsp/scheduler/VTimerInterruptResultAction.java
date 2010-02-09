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
package jpcsp.scheduler;

import jpcsp.Emulator;
import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.types.IAction;
import jpcsp.HLE.kernel.types.SceKernelVTimerInfo;
import jpcsp.HLE.modules.TimerManager;

public class VTimerInterruptResultAction implements IAction {
	private SceKernelVTimerInfo sceKernelVTimerInfo;

	public VTimerInterruptResultAction(SceKernelVTimerInfo sceKernelVTimerInfo) {
		this.sceKernelVTimerInfo = sceKernelVTimerInfo;
	}

	@Override
	public void execute() {
		TimerManager timerManager = Modules.TimerManager;

		int vtimerInterruptResult = Emulator.getProcessor().cpu.gpr[2];
		if (Modules.log.isDebugEnabled()) {
			Modules.log.debug("VTimer returned value " + vtimerInterruptResult);
		}

		if (vtimerInterruptResult == 0) {
			// VTimer is canceled
			timerManager.cancelVTimer(sceKernelVTimerInfo);
		} else {
			timerManager.rescheduleVTimer(sceKernelVTimerInfo, vtimerInterruptResult);
		}
	}

}
