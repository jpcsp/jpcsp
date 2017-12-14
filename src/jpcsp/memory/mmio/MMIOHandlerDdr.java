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

import org.apache.log4j.Logger;

import jpcsp.HLE.modules.sceDdr;

public class MMIOHandlerDdr extends MMIOHandlerBase {
	public static Logger log = sceDdr.log;
	private int unknown40;

	public MMIOHandlerDdr(int baseAddress) {
		super(baseAddress);
	}

	private void doFlush(int value) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("MMIOHandlerDdr.doFlush 0x%01X", value));
		}
	}

	@Override
	public int read32(int address) {
		int value;
		switch (address - baseAddress) {
			case 0x04: value = 0; break;
			case 0x30: value = 0; break; // Unknown, used during sceDdrChangePllClock()
			case 0x40: value = unknown40; unknown40 ^= 0x100; break; // Unknown, used during sceDdrChangePllClock()
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
			case 0x04: doFlush(value); break;
			case 0x30: break; // Unknown, used during sceDdrChangePllClock()
			case 0x34: break; // Unknown, used during sceDdrChangePllClock()
			case 0x40: break; // Unknown, used during sceDdrChangePllClock()
			case 0x44: break; // Unknown, used during sceDdrChangePllClock()
			default: super.write32(address, value); break;
		}

		if (log.isTraceEnabled()) {
			log.trace(String.format("0x%08X - write32(0x%08X, 0x%08X) on %s", getPc(), address, value, this));
		}
	}
}
