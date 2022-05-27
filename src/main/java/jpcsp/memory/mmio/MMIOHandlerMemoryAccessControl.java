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

import java.io.IOException;
import java.util.Arrays;

import jpcsp.state.StateInputStream;
import jpcsp.state.StateOutputStream;

public class MMIOHandlerMemoryAccessControl extends MMIOHandlerBase {
	private static final int STATE_VERSION = 0;
	private final int memoryProtection[] = new int[4];
	private int unknown10;
	private int unknown14;
	private int unknown18;
	private int unknown1C;
	private int unknown20;
	private int unknown24;
	private int unknown28;
	private int unknown2C;
	private int unknown30;
	private int unknown34;
	private int unknown38;
	private int unknown3C;
	private final int[] unknown40_to_7C = new int[0x10];

	public MMIOHandlerMemoryAccessControl(int baseAddress) {
		super(baseAddress);
	}

	@Override
	public void read(StateInputStream stream) throws IOException {
		stream.readVersion(STATE_VERSION);
		stream.readInts(memoryProtection);
		unknown10 = stream.readInt();
		unknown14 = stream.readInt();
		unknown18 = stream.readInt();
		unknown1C = stream.readInt();
		unknown20 = stream.readInt();
		unknown24 = stream.readInt();
		unknown28 = stream.readInt();
		unknown2C = stream.readInt();
		unknown30 = stream.readInt();
		unknown34 = stream.readInt();
		unknown38 = stream.readInt();
		unknown3C = stream.readInt();
		stream.readInts(unknown40_to_7C);
		super.read(stream);
	}

	@Override
	public void write(StateOutputStream stream) throws IOException {
		stream.writeVersion(STATE_VERSION);
		stream.writeInts(memoryProtection);
		stream.writeInt(unknown10);
		stream.writeInt(unknown14);
		stream.writeInt(unknown18);
		stream.writeInt(unknown1C);
		stream.writeInt(unknown20);
		stream.writeInt(unknown24);
		stream.writeInt(unknown28);
		stream.writeInt(unknown2C);
		stream.writeInt(unknown30);
		stream.writeInt(unknown34);
		stream.writeInt(unknown38);
		stream.writeInt(unknown3C);
		stream.writeInts(unknown40_to_7C);
		super.write(stream);
	}

	@Override
	public void reset() {
		super.reset();

		Arrays.fill(memoryProtection, 0);
		unknown10 = 0;
		unknown14 = 0;
		unknown18 = 0;
		unknown1C = 0;
		unknown20 = 0;
		unknown24 = 0;
		unknown28 = 0;
		unknown2C = 0;
		unknown30 = 0;
		unknown34 = 0;
		unknown38 = 0;
		unknown3C = 0;
		Arrays.fill(unknown40_to_7C, 0);
	}

	@Override
	public int read32(int address) {
		int value;
		switch (address - baseAddress) {
			case 0x00: value = memoryProtection[0]; break;
			case 0x04: value = memoryProtection[1]; break;
			case 0x08: value = memoryProtection[2]; break;
			case 0x0C: value = memoryProtection[3]; break;
			case 0x10: value = unknown10; break;
			case 0x14: value = unknown14; break;
			case 0x18: value = unknown18; break;
			case 0x1C: value = unknown1C; break;
			case 0x20: value = unknown20; break;
			case 0x24: value = unknown24; break;
			case 0x28: value = unknown28; break;
			case 0x2C: value = unknown2C; break;
			case 0x30: value = unknown30; break;
			case 0x34: value = unknown34; break;
			case 0x38: value = unknown38; break;
			case 0x3C: value = unknown3C; break;
			case 0x40: value = unknown40_to_7C[ 0]; break;
			case 0x44: value = unknown40_to_7C[ 1]; break;
			case 0x48: value = unknown40_to_7C[ 2]; break;
			case 0x4C: value = unknown40_to_7C[ 3]; break;
			case 0x50: value = unknown40_to_7C[ 4]; break;
			case 0x54: value = unknown40_to_7C[ 5]; break;
			case 0x58: value = unknown40_to_7C[ 6]; break;
			case 0x5C: value = unknown40_to_7C[ 7]; break;
			case 0x60: value = unknown40_to_7C[ 8]; break;
			case 0x64: value = unknown40_to_7C[ 9]; break;
			case 0x68: value = unknown40_to_7C[10]; break;
			case 0x6C: value = unknown40_to_7C[11]; break;
			case 0x70: value = unknown40_to_7C[12]; break;
			case 0x74: value = unknown40_to_7C[13]; break;
			case 0x78: value = unknown40_to_7C[14]; break;
			case 0x7C: value = unknown40_to_7C[15]; break;
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
			case 0x00: memoryProtection[0] = value; break;
			case 0x04: memoryProtection[1] = value; break;
			case 0x08: memoryProtection[2] = value; break;
			case 0x0C: memoryProtection[3] = value; break;
			case 0x10: unknown10 = value; break;
			case 0x14: unknown14 = value; break;
			case 0x18: unknown18 = value; break;
			case 0x1C: unknown1C = value; break;
			case 0x20: unknown20 = value; break;
			case 0x24: unknown24 = value; break;
			case 0x28: unknown28 = value; break;
			case 0x2C: unknown2C = value; break;
			case 0x30: unknown30 = value; break;
			case 0x34: unknown34 = value; break;
			case 0x38: unknown38 = value; break;
			case 0x3C: unknown3C = value; break;
			case 0x40: unknown40_to_7C[ 0] = value; break;
			case 0x44: unknown40_to_7C[ 1] = value; break;
			case 0x48: unknown40_to_7C[ 2] = value; break;
			case 0x4C: unknown40_to_7C[ 3] = value; break;
			case 0x50: unknown40_to_7C[ 4] = value; break;
			case 0x54: unknown40_to_7C[ 5] = value; break;
			case 0x58: unknown40_to_7C[ 6] = value; break;
			case 0x5C: unknown40_to_7C[ 7] = value; break;
			case 0x60: unknown40_to_7C[ 8] = value; break;
			case 0x64: unknown40_to_7C[ 9] = value; break;
			case 0x68: unknown40_to_7C[10] = value; break;
			case 0x6C: unknown40_to_7C[11] = value; break;
			case 0x70: unknown40_to_7C[12] = value; break;
			case 0x74: unknown40_to_7C[13] = value; break;
			case 0x78: unknown40_to_7C[14] = value; break;
			case 0x7C: unknown40_to_7C[15] = value; break;
			default: super.write32(address, value); break;
		}

		if (log.isTraceEnabled()) {
			log.trace(String.format("0x%08X - write32(0x%08X, 0x%08X) on %s", getPc(), address, value, this));
		}
	}
}
