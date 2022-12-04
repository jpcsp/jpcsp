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
package jpcsp.memory.mmio.battery;

import static jpcsp.nec78k0.Nec78k0Processor.SFR_ADDRESS;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import jpcsp.memory.mmio.IMMIOHandler;
import jpcsp.nec78k0.Nec78k0MMIOHandlerReadWrite;
import jpcsp.nec78k0.Nec78k0Memory;
import jpcsp.nec78k0.Nec78k0Processor;

/**
 * NEC 78k0 Memory map used by the Battery firmware:
 *   - [0x0000..0xFEDF]: RAM
 *   - [0xFEE0..0xFEE7]: Register address banks 3
 *   - [0xFEE8..0xFEEF]: Register address banks 2
 *   - [0xFEF0..0xFEF7]: Register address banks 1
 *   - [0xFEF8..0xFEFF]: Register address banks 0
 *   - [0xFF00..0xFFFF]: Special Function Registers (SFR)
 *   - [0xFF1C..0xFF1D]: Register SP
 *   - [0xFF1E..0xFF1E]: Register PSW
 *
 * @author gid15
 *
 */
public class BatteryMemory extends Nec78k0Memory {
	// The first 1k of the battery memory has not (yet) been dumped
	private static final int BASE_RAM_UNKNOWN = 0x0000;
	private static final int SIZE_RAM_UNKNOWN = 0x0400;
	private static final int END_RAM_UNKNOWN = BASE_RAM_UNKNOWN + SIZE_RAM_UNKNOWN - 1;
	private final int[] ramUnknown;
	private final Nec78k0MMIOHandlerReadWrite ramUnknownHandler;
	private final Set<Integer> ramKnown;

	public BatteryMemory(Logger log) {
		super(log, new MMIOHandlerBatteryFirmwareSfr(SFR_ADDRESS), BASE_RAM0, SIZE_RAM0);

		ramUnknown = new int[SIZE_RAM_UNKNOWN >> 2];
		ramUnknownHandler = new Nec78k0MMIOHandlerReadWrite(BASE_RAM_UNKNOWN, SIZE_RAM_UNKNOWN, ramUnknown);
		ramUnknownHandler.setLogger(log);
		ramUnknownHandler.setLogLevel(Level.WARN);

		ramKnown = new HashSet<Integer>();
	}

	@Override
	public void setProcessor(Nec78k0Processor processor) {
		ramUnknownHandler.setProcessor(processor);

		super.setProcessor(processor);
	}

	@Override
	protected IMMIOHandler getHandler(int address) {
		if (address >= BASE_RAM_UNKNOWN && address <= END_RAM_UNKNOWN && !ramKnown.contains(address)) {
			return ramUnknownHandler;
		}

		return super.getHandler(address);
	}

	public void setRamKnown(int address) {
		ramKnown.add(address);
	}

	public void setRamKnown(int address, int size) {
		for (int i = 0; i < size; i++) {
			setRamKnown(address + i);
		}
	}
}
