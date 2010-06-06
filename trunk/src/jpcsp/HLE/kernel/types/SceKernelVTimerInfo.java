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
package jpcsp.HLE.kernel.types;

import jpcsp.HLE.pspSysMem;
import jpcsp.HLE.kernel.managers.SceUidManager;
import jpcsp.HLE.kernel.types.interrupts.VTimerInterruptHandler;
import jpcsp.scheduler.VTimerInterruptAction;
import jpcsp.scheduler.VTimerInterruptResultAction;

public class SceKernelVTimerInfo extends pspAbstractMemoryMappedStructure {
	public int size;
	public String name;
	public int active;
	public long base;
	public long current;
	public long schedule;
	public int handlerAddress;
	public int handlerArgument;

	public final int uid;
	public VTimerInterruptHandler vtimerInterruptHandler;
	public final VTimerInterruptAction vtimerInterruptAction;
	public final VTimerInterruptResultAction vtimerInterruptResultAction;
	private int internalMemory;

	private static final int DEFAULT_SIZE = 72;
	public static final int ACTIVE_RUNNING = 1;
	public static final int ACTIVE_STOPPED = 0;

	public SceKernelVTimerInfo(String name) {
		size = DEFAULT_SIZE;
		this.name = name;
		active = ACTIVE_STOPPED;

		uid = SceUidManager.getNewUid("ThreadMan-VTimer");
		vtimerInterruptHandler = new VTimerInterruptHandler(this);
		vtimerInterruptAction = new VTimerInterruptAction(this);
		vtimerInterruptResultAction = new VTimerInterruptResultAction(this);
		internalMemory = 0;
	}

	public int getInternalMemory() {
		if (internalMemory == 0) {
			// Allocate enough memory to store "current" and "schedule"
			internalMemory = pspSysMem.getInstance().malloc(2, pspSysMem.PSP_SMEM_Low, 16, 0);
		}

		return internalMemory;
	}

	public void delete() {
		if (internalMemory != 0) {
			pspSysMem.getInstance().free(-1, internalMemory);
			internalMemory = 0;
		}
	}

	@Override
	protected void read() {
		size = read32();
		//setMaxSize(size);
		name = readStringNZ(32);
		active = read32();
		base = read64();
		current = read64();
		schedule = read64();
		handlerAddress = read32();
		handlerArgument = read32();
	}

	@Override
	public int sizeof() {
		return size;
	}

	@Override
	protected void write() {
		setMaxSize(size);
		write32(size);
		writeStringNZ(32, name);
		write32(active);
		write64(base);
		write64(current);
		write64(schedule);
		write32(handlerAddress);
		write32(handlerArgument);
	}
}
