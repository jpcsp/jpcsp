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
package jpcsp.memory.mmio;

import static jpcsp.HLE.kernel.managers.IntrManager.PSP_DMA0_INTR;

import java.io.IOException;

import org.apache.log4j.Logger;

import jpcsp.Allegrex.compiler.RuntimeContextLLE;
import jpcsp.HLE.kernel.types.IAction;
import jpcsp.HLE.modules.sceDmac;
import jpcsp.memory.mmio.dmac.DmacProcessor;
import jpcsp.sound.SoundChannel;
import jpcsp.state.StateInputStream;
import jpcsp.state.StateOutputStream;

public class MMIOHandlerDmac extends MMIOHandlerBase {
	public static Logger log = sceDmac.log;
	private static final int STATE_VERSION = 0;
	private final DmacProcessor dmacProcessors[] = new DmacProcessor[8];
	private int flagsCompleted;
	private int flagsError;

	private class DmacCompletedAction implements IAction {
		private int flagCompleted;

		public DmacCompletedAction(int flagCompleted) {
			this.flagCompleted = flagCompleted;
		}

		@Override
		public void execute() {
			memcpyCompleted(flagCompleted);
		}
	}

	public MMIOHandlerDmac(int baseAddress) {
		super(baseAddress);

		SoundChannel.init();
		for (int i = 0; i < dmacProcessors.length; i++) {
			dmacProcessors[i] = new DmacProcessor(getMemory(), getMemory(), baseAddress + 0x100 + i * 0x20, new DmacCompletedAction(1 << i));
		}
	}

	@Override
	public void read(StateInputStream stream) throws IOException {
		stream.readVersion(STATE_VERSION);
		flagsCompleted = stream.readInt();
		flagsError = stream.readInt();
		for (int i = 0; i < dmacProcessors.length; i++) {
			dmacProcessors[i].read(stream);
		}
		super.read(stream);
	}

	@Override
	public void write(StateOutputStream stream) throws IOException {
		stream.writeVersion(STATE_VERSION);
		stream.writeInt(flagsCompleted);
		stream.writeInt(flagsError);
		for (int i = 0; i < dmacProcessors.length; i++) {
			dmacProcessors[i].write(stream);
		}
		super.write(stream);
	}

	@Override
	public void reset() {
		super.reset();

		flagsCompleted = 0;
		flagsError = 0;
		for (int i = 0; i < dmacProcessors.length; i++) {
			dmacProcessors[i].reset();
		}
	}

	private void memcpyCompleted(int flagCompleted) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("memcpyCompleted 0x%X", flagCompleted));
		}
		flagsCompleted |= flagCompleted;

		checkInterrupt();
	}

	private void checkInterrupt() {
		if (flagsCompleted != 0) {
			RuntimeContextLLE.triggerInterrupt(getProcessor(), PSP_DMA0_INTR);
		} else {
			RuntimeContextLLE.clearInterrupt(getProcessor(), PSP_DMA0_INTR);
		}
	}

	@Override
	public int read32(int address) {
		int value;
		switch (address - baseAddress) {
			case 0x004: value = flagsCompleted; break;
			case 0x00C: value = flagsError; break;
			case 0x030: value = 0; break; // Unknown
			case 0x100:
			case 0x104:
			case 0x108:
			case 0x10C:
			case 0x110: value = dmacProcessors[0].read32(address - baseAddress - 0x100); break;
			case 0x120:
			case 0x124:
			case 0x128:
			case 0x12C:
			case 0x130: value = dmacProcessors[1].read32(address - baseAddress - 0x120); break;
			case 0x140:
			case 0x144:
			case 0x148:
			case 0x14C:
			case 0x150: value = dmacProcessors[2].read32(address - baseAddress - 0x140); break;
			case 0x160:
			case 0x164:
			case 0x168:
			case 0x16C:
			case 0x170: value = dmacProcessors[3].read32(address - baseAddress - 0x160); break;
			case 0x180:
			case 0x184:
			case 0x188:
			case 0x18C:
			case 0x190: value = dmacProcessors[4].read32(address - baseAddress - 0x180); break;
			case 0x1A0:
			case 0x1A4:
			case 0x1A8:
			case 0x1AC:
			case 0x1B0: value = dmacProcessors[5].read32(address - baseAddress - 0x1A0); break;
			case 0x1C0:
			case 0x1C4:
			case 0x1C8:
			case 0x1CC:
			case 0x1D0: value = dmacProcessors[6].read32(address - baseAddress - 0x1C0); break;
			case 0x1E0:
			case 0x1E4:
			case 0x1E8:
			case 0x1EC:
			case 0x1F0: value = dmacProcessors[7].read32(address - baseAddress - 0x1E0); break;
			default: value = super.read32(address); break;
		}

		if (log.isTraceEnabled()) {
			log.trace(String.format("0x%08X - read32(0x%08X) returning 0x%08X", getPc(), address, value));
		}

		return value;
	}

	@Override
	public void write32(int address, int value) {
		switch (address - baseAddress) {
			case 0x008: flagsCompleted &= ~value; checkInterrupt(); break;
			case 0x010: flagsError &= ~value; break;
			case 0x030: if (value != 0 && value != 1) { super.write32(address, value); } break; // Unknown
			case 0x034: if (value != 0) { super.write32(address, value); } break; // Unknown
			case 0x100:
			case 0x104:
			case 0x108:
			case 0x10C:
			case 0x110: dmacProcessors[0].write32(address - baseAddress - 0x100, value); break;
			case 0x120:
			case 0x124:
			case 0x128:
			case 0x12C:
			case 0x130: dmacProcessors[1].write32(address - baseAddress - 0x120, value); break;
			case 0x140:
			case 0x144:
			case 0x148:
			case 0x14C:
			case 0x150: dmacProcessors[2].write32(address - baseAddress - 0x140, value); break;
			case 0x160:
			case 0x164:
			case 0x168:
			case 0x16C:
			case 0x170: dmacProcessors[3].write32(address - baseAddress - 0x160, value); break;
			case 0x180:
			case 0x184:
			case 0x188:
			case 0x18C:
			case 0x190: dmacProcessors[4].write32(address - baseAddress - 0x180, value); break;
			case 0x1A0:
			case 0x1A4:
			case 0x1A8:
			case 0x1AC:
			case 0x1B0: dmacProcessors[5].write32(address - baseAddress - 0x1A0, value); break;
			case 0x1C0:
			case 0x1C4:
			case 0x1C8:
			case 0x1CC:
			case 0x1D0: dmacProcessors[6].write32(address - baseAddress - 0x1C0, value); break;
			case 0x1E0:
			case 0x1E4:
			case 0x1E8:
			case 0x1EC:
			case 0x1F0: dmacProcessors[7].write32(address - baseAddress - 0x1E0, value); break;
			default: super.write32(address, value); break;
		}

		if (log.isTraceEnabled()) {
			log.trace(String.format("0x%08X - write32(0x%08X, 0x%08X) on %s", getPc(), address, value, this));
		}
	}
}
