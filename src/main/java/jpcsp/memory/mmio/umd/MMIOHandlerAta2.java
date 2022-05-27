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
package jpcsp.memory.mmio.umd;

import java.io.IOException;

import org.apache.log4j.Logger;

import jpcsp.HLE.modules.sceAta;
import jpcsp.memory.mmio.MMIOHandlerBase;
import jpcsp.state.StateInputStream;
import jpcsp.state.StateOutputStream;

public class MMIOHandlerAta2 extends MMIOHandlerBase {
	public static Logger log = sceAta.log;
	private static final int STATE_VERSION = 0;

	public MMIOHandlerAta2(int baseAddress) {
		super(baseAddress);
	}

	@Override
	public void read(StateInputStream stream) throws IOException {
		stream.readVersion(STATE_VERSION);
		super.read(stream);
	}

	@Override
	public void write(StateOutputStream stream) throws IOException {
		stream.writeVersion(STATE_VERSION);
		super.write(stream);
	}

	private void writeReset(int value) {
		if (value != 0) {
			MMIOHandlerUmdAta.getInstance().reset();
		}
	}

	@Override
	public int read32(int address) {
		int value;
		switch (address - baseAddress) {
			case 0x00: value = 0x00010033; break; // Unknown value
			case 0x10: value = 0; break; // Reset in progress?
			case 0x34: value = 0; break; // Unknown value
			case 0x40: value = 0; break; // Unknown value, flag 0x2 is being tested
			default: value = super.read32(address); break;
		}

		if (log.isTraceEnabled()) {
			log.trace(String.format("0x%08X - read32(0x%08X) returning 0x%08X", getPc(), address, value));
		}

		return value;
	}

	private void writeUnknown44(int value) {
		int unknownValue = (~value) >>> 16;

		if (log.isDebugEnabled()) {
			log.debug(String.format("writeUnknown44 0x%04X", unknownValue));
		}
	}

	@Override
	public void write32(int address, int value) {
		switch (address - baseAddress) {
			case 0x04: if (value != 0x04028002) { super.write32(address, value); } break; // Unknown value
			case 0x10: writeReset(value); break;
			case 0x1C: if (value != 0x00020A0C) { super.write32(address, value); } break; // Unknown value
			case 0x14: break; // Unknown value
			case 0x34: if (value != 0) { super.write32(address, value); } break; // Unknown value
			case 0x38: if (value != 0x00010100) { super.write32(address, value); } break; // Unknown value
			case 0x40: if (value != 1) { super.write32(address, value); } break; // Unknown value
			case 0x44: writeUnknown44(value); break;
			default: super.write32(address, value); break;
		}

		if (log.isTraceEnabled()) {
			log.trace(String.format("0x%08X - write32(0x%08X, 0x%08X) on %s", getPc(), address, value, this));
		}
	}
}
