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

import jpcsp.Memory;
import jpcsp.Allegrex.CpuState;
import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.types.SceKernelVTimerInfo;

public class VTimerInterruptHandler extends AbstractAllegrexInterruptHandler {
	private SceKernelVTimerInfo sceKernelVTimerInfo;

	public VTimerInterruptHandler(SceKernelVTimerInfo sceKernelVTimerInfo) {
		super(sceKernelVTimerInfo.handlerAddress);
		setArgument(0, sceKernelVTimerInfo.uid);
		setArgument(3, sceKernelVTimerInfo.handlerArgument);
	}

	@Override
	public void copyArgumentsToCpu(CpuState cpu) {
		Memory mem = Memory.getInstance();
		int internalMemory = sceKernelVTimerInfo.getInternalMemory();
		if (internalMemory != 0) {
			mem.write64(internalMemory    , sceKernelVTimerInfo.schedule);
			mem.write64(internalMemory + 8, Modules.TimerManager.getVTimerTime(sceKernelVTimerInfo));
			setArgument(1, internalMemory    );
			setArgument(2, internalMemory + 8);
		} else {
			setArgument(1, 0);
			setArgument(2, 0);
		}

		super.copyArgumentsToCpu(cpu);
	}
}
