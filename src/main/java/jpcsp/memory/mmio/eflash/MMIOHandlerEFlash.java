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
import static jpcsp.HLE.kernel.managers.IntrManager.PSP_EFLASH_ATA2_INTR;
import static jpcsp.util.Utilities.clearFlag;
import static jpcsp.util.Utilities.setFlag;

import java.io.IOException;

import org.apache.log4j.Logger;

import jpcsp.HLE.modules.sceEFlash;
import jpcsp.memory.mmio.MMIOHandlerBase;
import jpcsp.state.StateInputStream;
import jpcsp.state.StateOutputStream;

/**
 * PSP Go 16GB internal memory (eflash)
 *
 */
public class MMIOHandlerEFlash extends MMIOHandlerBase {
	public static Logger log = sceEFlash.log;
	public static final int BASE_ADDRESS = 0xBD900000;
	private static MMIOHandlerEFlash instance;
	private static final int STATE_VERSION = 0;
	private int unknown14;
	private int unknown18;
	private int unknown24;
	private int unknown28;
	private int unknown34;
	private int unknown38;
	private int unknown40;
	private int interrupt;

	public static MMIOHandlerEFlash getInstance() {
		if (instance == null) {
			instance = new MMIOHandlerEFlash(BASE_ADDRESS);
		}

		return instance;
	}

	private MMIOHandlerEFlash(int baseAddress) {
		super(baseAddress);

		reset();
	}

	@Override
	public void read(StateInputStream stream) throws IOException {
		stream.readVersion(STATE_VERSION);
		unknown14 = stream.readInt();
		unknown18 = stream.readInt();
		unknown24 = stream.readInt();
		unknown28 = stream.readInt();
		unknown34 = stream.readInt();
		unknown38 = stream.readInt();
		unknown40 = stream.readInt();
		interrupt = stream.readInt();
		super.read(stream);
	}

	@Override
	public void write(StateOutputStream stream) throws IOException {
		stream.writeVersion(STATE_VERSION);
		stream.writeInt(unknown14);
		stream.writeInt(unknown18);
		stream.writeInt(unknown24);
		stream.writeInt(unknown28);
		stream.writeInt(unknown34);
		stream.writeInt(unknown38);
		stream.writeInt(unknown40);
		stream.writeInt(interrupt);
		super.write(stream);
	}

	@Override
	public void reset() {
		unknown14 = 0;
		unknown18 = 0;
		unknown24 = 0;
		unknown28 = 0;
		unknown34 = 0;
		unknown38 = 0;
		unknown40 = 0;
		interrupt = 0;

		super.reset();
	}

	private void writeReset(int value) {
		if (value == 1) {
			reset();
		}
	}

	private void checkInterrupt() {
		if ((interrupt & 0xFFFF0000) != 0) {
			triggerInterrupt(getProcessor(), PSP_EFLASH_ATA2_INTR);
		} else {
			clearInterrupt(getProcessor(), PSP_EFLASH_ATA2_INTR);
		}
	}

	public void setInterruptFlag(int value) {
		interrupt = setFlag(interrupt, value);
		checkInterrupt();
	}

	private void clearInterruptFlag(int value) {
		interrupt = clearFlag(interrupt, value);
		checkInterrupt();
	}

	@Override
	public int read32(int address) {
		int value;
		switch (address - baseAddress) {
			case 0x34: value = unknown34; break;
			case 0x44: value = interrupt; break;
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
			case 0x04: if (value != 0x04024002) { super.write32(address, value); } break;
			case 0x10: writeReset(value); break;
			case 0x14: unknown14 = value; break;
			case 0x18: unknown18 = value; break;
			case 0x24: unknown24 = value; break;
			case 0x28: unknown28 = value; break;
			case 0x2C: if (value != 0) { super.write32(address, value); } break;
			case 0x30: if (value != 0) { super.write32(address, value); } break;
			case 0x34: unknown34 = value; break;
			case 0x38: unknown38 = value; break;
			case 0x40: unknown40 = value; break;
			case 0x44: clearInterruptFlag(value); break;
			default: super.write32(address, value); break;
		}

		if (log.isTraceEnabled()) {
			log.trace(String.format("0x%08X - write32(0x%08X, 0x%08X) on %s", getPc(), address, value, this));
		}
	}
}
