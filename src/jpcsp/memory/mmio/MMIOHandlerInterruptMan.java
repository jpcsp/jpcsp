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

import jpcsp.Emulator;
import jpcsp.HLE.kernel.managers.IntrManager;

public class MMIOHandlerInterruptMan extends MMIOHandlerBase {
	public final boolean interruptTriggered[] = new boolean[64];
	public final boolean interruptEnabled[] = new boolean[64];
	public final boolean interruptOccured[] = new boolean[64];

	public MMIOHandlerInterruptMan(int baseAddress) {
		super(baseAddress);
	}

	private void setBits(boolean values[], int value, int offset, int mask) {
		for (int i = 0; mask != 0; i++, value >>>= 1, mask >>>= 1) {
			if ((mask & 1) != 0) {
				values[offset + i] = (value & 1) != 0;
			}
		}
	}

	private void setBits1(boolean values[], int value) {
		setBits(values, value, 0, 0xDFFFFFF0);
	}

	private void setBits2(boolean values[], int value) {
		setBits(values, value, 32, 0xFFFF3F3F);
	}

	private void setBits3(boolean values[], int value) {
		int value3 = (value & 0xC0) | ((value >> 2) & 0xC000);
		setBits(values, value3, 32, 0x0000C0C0);
	}

	private int getBits(boolean values[], int offset) {
		int value = 0;
		for (int i = 31; i >= 0; i--) {
			value <<= 1;
			if (values[offset + i]) {
				value |= 1;
			}
		}

		return value;
	}

	private int getBits1(boolean values[]) {
		return getBits(values, 0);
	}

	private int getBits2(boolean values[]) {
		return getBits(values, 32) & 0xFFFF3F3F;
	}

	private int getBits3(boolean values[]) {
		int value3 = getBits(values, 32);
		value3 = (value3 & 0xC0) | ((value3 & 0xC000) << 2);
		return value3;
	}

	@Override
	public int read32(int address) {
		if (log.isTraceEnabled()) {
			log.trace(String.format("0x%08X - read32(0x%08X) on %s", Emulator.getProcessor().cpu.pc, address, this));
		}

		switch (address - baseAddress) {
			// Interrupt triggered:
			case 0 : return getBits1(interruptTriggered);
			case 16: return getBits2(interruptTriggered);
			case 32: return getBits3(interruptTriggered);
			// Interrupt occured:
			case 4 : return getBits1(interruptOccured);
			case 20: return getBits2(interruptOccured);
			case 36: return getBits3(interruptOccured);
			// Interrupt enabled:
			case 8 : return getBits1(interruptEnabled);
			case 24: return getBits2(interruptEnabled);
			case 40: return getBits3(interruptEnabled);
		}
		return super.read32(address);
	}

	@Override
	public void write32(int address, int value) {
		switch (address - baseAddress) {
			// Interrupt triggered:
			case 0 : setBits1(interruptTriggered, value); break;
			case 16: setBits2(interruptTriggered, value); break;
			case 32: setBits3(interruptTriggered, value); break;
			// Interrupt occured:
			case 4 : setBits1(interruptOccured, value); break;
			case 20: setBits2(interruptOccured, value); break;
			case 36: setBits3(interruptOccured, value); break;
			// Interrupt enabled:
			case 8 : setBits1(interruptEnabled, value); break;
			case 24: setBits2(interruptEnabled, value); break;
			case 40: setBits3(interruptEnabled, value); break;
			// Unknown:
			default: super.write32(address, value); break;
		}

		if (log.isTraceEnabled()) {
			log.trace(String.format("0x%08X - write32(0x%08X, 0x%08X) on %s", Emulator.getProcessor().cpu.pc, address, value, this));
		}
	}

	private void toString(StringBuilder sb, String name, boolean values[]) {
		if (sb.length() > 0) {
			sb.append(", ");
		}
		sb.append(name);
		sb.append("[");
		boolean first = true;
		for (int i = 0; i < values.length; i++) {
			if (values[i]) {
				if (first) {
					first = false;
				} else {
					sb.append("|");
				}
				sb.append(IntrManager.getInterruptName(i));
			}
		}
		sb.append("]");
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		toString(sb, "interruptTriggered", interruptTriggered);
		toString(sb, "interruptOccured", interruptOccured);
		toString(sb, "interruptEnabled", interruptEnabled);

		return sb.toString();
	}
}
