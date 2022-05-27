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
package jpcsp.memory.mmio.eflash;

import static jpcsp.Allegrex.compiler.RuntimeContextLLE.clearInterrupt;
import static jpcsp.Allegrex.compiler.RuntimeContextLLE.triggerInterrupt;
import static jpcsp.HLE.kernel.managers.IntrManager.PSP_EFLASH_DMA_INTR;
import static jpcsp.util.Utilities.clearFlag;
import static jpcsp.util.Utilities.hasBit;
import static jpcsp.util.Utilities.setFlag;

import java.io.IOException;

import org.apache.log4j.Logger;

import jpcsp.Memory;
import jpcsp.HLE.modules.sceEFlash;
import jpcsp.memory.mmio.MMIOHandlerBase;
import jpcsp.state.StateInputStream;
import jpcsp.state.StateOutputStream;

public class MMIOHandlerEFlashDma extends MMIOHandlerBase {
	public static Logger log = sceEFlash.log;
	private static final int STATE_VERSION = 0;
	private int control;
	private int interrupt;
	private int unknown28;
	private int dmaAddr;
	private int dmaSize;
	private int unknown40;

	public MMIOHandlerEFlashDma(int baseAddress) {
		super(baseAddress);

		reset();
	}

	@Override
	public void read(StateInputStream stream) throws IOException {
		stream.readVersion(STATE_VERSION);
		control = stream.readInt();
		interrupt = stream.readInt();
		unknown28 = stream.readInt();
		dmaAddr = stream.readInt();
		dmaSize = stream.readInt();
		unknown40 = stream.readInt();
		super.read(stream);
	}

	@Override
	public void write(StateOutputStream stream) throws IOException {
		stream.writeVersion(STATE_VERSION);
		stream.writeInt(control);
		stream.writeInt(interrupt);
		stream.writeInt(unknown28);
		stream.writeInt(dmaAddr);
		stream.writeInt(dmaSize);
		stream.writeInt(unknown40);
		super.write(stream);
	}

	@Override
	public void reset() {
		control = 0;
		interrupt = 0;
		unknown28 = 0;
		dmaAddr = 0;
		dmaSize = 0;
		unknown40 = 0;

		super.reset();
	}

	private void writeReset(int value) {
		if (value == 1) {
			reset();

			// This also performs a reset of the EFlash Ata interface
			MMIOHandlerEFlashAta.getInstance().reset();
		}
	}

	private void checkInterrupt() {
		if (interrupt != 0) {
			triggerInterrupt(getProcessor(), PSP_EFLASH_DMA_INTR);
		} else {
			clearInterrupt(getProcessor(), PSP_EFLASH_DMA_INTR);
		}
	}

	private void clearInterruptFlag(int value) {
		interrupt = clearFlag(interrupt, value);
		checkInterrupt();
	}

	private void setInterruptFlag(int value) {
		interrupt = setFlag(interrupt, value);
		checkInterrupt();
	}

	private void clearUnknown28(int value) {
		unknown28 = clearFlag(unknown28, value);
	}

	private void writeUnknown28(int value) {
		unknown28 = value;
	}

	private void writeControl(int value) {
		control = value;

		if (hasBit(control, 0)) {
			// Copy to or from memory?
			if (hasBit(control, 1)) {
				// Copy from EFlash ATA to memory (for ATA read operation)
				MMIOHandlerEFlashAta eflashAta = MMIOHandlerEFlashAta.getInstance();
				Memory mem = getMemory();
				int addr = dmaAddr;
				for (int j = 0; j < dmaSize; j += 2, addr += 2) {
					int data16 = eflashAta.read16(MMIOHandlerEFlashAta.BASE_ADDRESS);
					mem.writeUnsigned16(addr, data16);
				}

				setInterruptFlag(0x1);
			} else {
				// Copy from memory to EFlash ATA (for ATA write operation)
				MMIOHandlerEFlashAta eflashAta = MMIOHandlerEFlashAta.getInstance();
				Memory mem = getMemory();
				int addr = dmaAddr;
				for (int j = 0; j < dmaSize; j += 2, addr += 2) {
					int data16 = mem.read16(addr);
					eflashAta.write16(MMIOHandlerEFlashAta.BASE_ADDRESS, (short) data16);
				}

				setInterruptFlag(0x1);
			}
		}
	}

	@Override
	public int read32(int address) {
		int value;
		switch (address - baseAddress) {
			case 0x20: value = interrupt; break;
			case 0x28: value = unknown28; break;
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
			case 0x08: writeReset(value); break;
			case 0x10: writeControl(value); break;
			case 0x24: clearInterruptFlag(value); break;
			case 0x28: writeUnknown28(value); break;
			case 0x2C: clearUnknown28(value); break;
			case 0x30: dmaAddr = value; break;
			case 0x34: dmaSize = value; break;
			case 0x40: unknown40 = value; break;
			case 0x44: if (value != 0) { super.write32(address, value); } break;
			default: super.write32(address, value); break;
		}

		if (log.isTraceEnabled()) {
			log.trace(String.format("0x%08X - write32(0x%08X, 0x%08X) on %s", getPc(), address, value, this));
		}
	}
}
