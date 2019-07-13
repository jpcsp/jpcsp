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
	private static final int STATE_VERSION = 0;
	private int unknown14;
	private int unknown18;
	private int unknown34;
	private int unknown44;

	public MMIOHandlerEFlash(int baseAddress) {
		super(baseAddress);

		reset();
	}

	@Override
	public void read(StateInputStream stream) throws IOException {
		stream.readVersion(STATE_VERSION);
		unknown14 = stream.readInt();
		unknown18 = stream.readInt();
		unknown34 = stream.readInt();
		unknown44 = stream.readInt();
		super.read(stream);
	}

	@Override
	public void write(StateOutputStream stream) throws IOException {
		stream.writeVersion(STATE_VERSION);
		stream.writeInt(unknown14);
		stream.writeInt(unknown18);
		stream.writeInt(unknown34);
		stream.writeInt(unknown44);
		super.write(stream);
	}

	@Override
	public void reset() {
		unknown14 = 0;
		unknown18 = 0;
		unknown34 = 0;
		unknown44 = 0;

		super.reset();
	}

	private void writeReset(int value) {
		if (value == 1) {
			reset();
		}
	}

	@Override
	public int read32(int address) {
		int value;
		switch (address - baseAddress) {
			case 0x34: value = unknown34; break;
			case 0x44: value = unknown44; break;
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
			case 0x24: if (value != 0) { super.write32(address, value); } break;
			case 0x28: if (value != 0) { super.write32(address, value); } break;
			case 0x2C: if (value != 0) { super.write32(address, value); } break;
			case 0x30: if (value != 0) { super.write32(address, value); } break;
			case 0x34: unknown34 = value; break;
			case 0x44: unknown44 = value; break;
			default: super.write32(address, value); break;
		}

		if (log.isTraceEnabled()) {
			log.trace(String.format("0x%08X - write32(0x%08X, 0x%08X) on %s", getPc(), address, value, this));
		}
	}
}
