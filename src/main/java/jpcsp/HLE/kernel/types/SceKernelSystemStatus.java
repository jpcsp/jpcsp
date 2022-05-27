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

public class SceKernelSystemStatus extends pspAbstractMemoryMappedStructureVariableLength {
	/** The status ? */
	public int status;
	/** The number of cpu clocks in the idle thread */
	public long idleClocks;
	/** Number of times we resumed from idle */
	public int comesOutOfIdleCount;
	/** Number of thread context switches */
	public int threadSwitchCount;
	/** Number of vfpu switches ? */
	public int vfpuSwitchCount;

	@Override
	protected void read() {
		super.read();
		status = read32();
		idleClocks = read64();
		comesOutOfIdleCount = read32();
		threadSwitchCount = read32();
		vfpuSwitchCount = read32();
	}

	@Override
	protected void write() {
		super.write();
		write32(status);
		write64(idleClocks);
		write32(comesOutOfIdleCount);
		write32(threadSwitchCount);
		write32(vfpuSwitchCount);
	}
}
