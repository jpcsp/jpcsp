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
import jpcsp.util.Utilities;

public class MMIOHandlerEFlash3 extends MMIOHandlerBase {
	public static Logger log = sceEFlash.log;
	private static final int STATE_VERSION = 0;
	private int unknown28;
	private int unknown20;

	public MMIOHandlerEFlash3(int baseAddress) {
		super(baseAddress);

		reset();
	}

	@Override
	public void read(StateInputStream stream) throws IOException {
		stream.readVersion(STATE_VERSION);
		unknown20 = stream.readInt();
		unknown28 = stream.readInt();
		super.read(stream);
	}

	@Override
	public void write(StateOutputStream stream) throws IOException {
		stream.writeVersion(STATE_VERSION);
		stream.writeInt(unknown20);
		stream.writeInt(unknown28);
		super.write(stream);
	}

	@Override
	public void reset() {
		unknown20 = 0;
		unknown28 = 0;

		super.reset();
	}

	private void writeReset(int value) {
		if (value == 1) {
			reset();
		}
	}

	private void clearUnknown20(int value) {
		unknown20 = Utilities.clearFlag(unknown20, value);
	}

	private void clearUnknown28(int value) {
		unknown28 = Utilities.clearFlag(unknown28, value);
	}

	private void writeUnknown28(int value) {
		unknown28 = value;
	}

	@Override
	public int read32(int address) {
		int value;
		switch (address - baseAddress) {
			case 0x20: value = unknown20; break;
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
			case 0x24: clearUnknown20(value); break;
			case 0x28: writeUnknown28(value); break;
			case 0x2C: clearUnknown28(value); break;
			case 0x40: if (value != 0) { super.write32(address, value); } break;
			case 0x44: if (value != 0) { super.write32(address, value); } break;
			default: super.write32(address, value); break;
		}

		if (log.isTraceEnabled()) {
			log.trace(String.format("0x%08X - write32(0x%08X, 0x%08X) on %s", getPc(), address, value, this));
		}
	}
}
