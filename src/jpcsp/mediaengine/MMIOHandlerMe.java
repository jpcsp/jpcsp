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
package jpcsp.mediaengine;

import org.apache.log4j.Logger;

import jpcsp.Processor;
import jpcsp.HLE.modules.sceMeCore;
import jpcsp.memory.mmio.MMIOHandlerBase;

public class MMIOHandlerMe extends MMIOHandlerBase {
	public static Logger log = sceMeCore.log;

	public MMIOHandlerMe(int baseAddress) {
		super(baseAddress);
	}

	@Override
	protected Processor getProcessor() {
		return MEProcessor.getInstance();
	}

	@Override
	public int read32(int address) {
		int value;
		switch (address - baseAddress) {
			default: value = super.read32(address); break;
		}

		if (log.isTraceEnabled()) {
			log.trace(String.format("0x%08X - read32(0x%08X) returning 0x%08X", getProcessor().cpu.pc - 4, address, value));
		}

		return value;
	}

	@Override
	public void write32(int address, int value) {
		switch (address - baseAddress) {
			default: super.write32(address, value); break;
		}

		if (log.isTraceEnabled()) {
			log.trace(String.format("0x%08X - write32(0x%08X, 0x%08X) on %s", getProcessor().cpu.pc - 4, address, value, this));
		}
	}
}
