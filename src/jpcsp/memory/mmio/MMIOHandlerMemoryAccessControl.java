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
	private int unknown40;
	private int unknown44;
	private int unknown48;
	private int unknown50;

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
		unknown40 = stream.readInt();
		unknown44 = stream.readInt();
		unknown48 = stream.readInt();
		unknown50 = stream.readInt();
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
		stream.writeInt(unknown40);
		stream.writeInt(unknown44);
		stream.writeInt(unknown48);
		stream.writeInt(unknown50);
		super.write(stream);
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
			case 0x40: value = unknown40; break;
			case 0x44: value = unknown44; break;
			case 0x48: value = unknown48; break;
			case 0x50: value = unknown50; break;
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
			case 0x40: unknown40 = value; break;
			case 0x44: unknown44 = value; break;
			case 0x48: unknown48 = value; break;
			case 0x50: unknown50 = value; break;
			default: super.write32(address, value); break;
		}

		if (log.isTraceEnabled()) {
			log.trace(String.format("0x%08X - write32(0x%08X, 0x%08X) on %s", getPc(), address, value, this));
		}
	}
}
