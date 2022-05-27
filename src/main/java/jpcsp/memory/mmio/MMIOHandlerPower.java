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

import org.apache.log4j.Logger;

import jpcsp.HLE.modules.scePower;
import jpcsp.state.IState;
import jpcsp.state.StateInputStream;
import jpcsp.state.StateOutputStream;

public class MMIOHandlerPower extends MMIOHandlerBase {
	public static Logger log = scePower.log;
	private static final int STATE_VERSION = 0;
	private final PowerObject unknown1 = new PowerObject();
	private final PowerObject unknown2 = new PowerObject();
	private final PowerObject unknown3 = new PowerObject();

	private static class PowerObject implements IState {
		private static final int STATE_VERSION = 0;
		public int unknown00;
		public int unknown04;
		public int unknown08;
		public int unknown0C;
		public int unknown10;
		public int unknown14;
		public int unknown18;
		public int unknown1C;

		private int getUnknown0C() {
			unknown0C = unknown04;
			return unknown0C;
		}

		public int read32(int offset) {
			int value = 0;
			switch (offset) {
				case 0x00: value = unknown00; break;
				case 0x04: value = unknown04; break;
				case 0x08: value = unknown08; break;
				case 0x0C: value = getUnknown0C(); break;
				case 0x10: value = unknown10; break;
				case 0x14: value = unknown14; break;
				case 0x18: value = unknown18; break;
				case 0x1C: value = unknown1C; break;
			}

			return value;
		}

		public void write32(int offset, int value) {
			switch (offset) {
				case 0x00: unknown00 = value; break;
				case 0x04: unknown04 = value; break;
				case 0x08: unknown08 = value; break;
				case 0x0C: unknown0C = value; break;
				case 0x10: unknown10 = value; break;
				case 0x14: unknown14 = value; break;
				case 0x18: unknown18 = value; break;
				case 0x1C: unknown1C = value; break;
			}
		}

		@Override
		public void read(StateInputStream stream) throws IOException {
			stream.readVersion(STATE_VERSION);
			unknown00 = stream.readInt();
			unknown04 = stream.readInt();
			unknown08 = stream.readInt();
			unknown0C = stream.readInt();
			unknown10 = stream.readInt();
			unknown14 = stream.readInt();
			unknown18 = stream.readInt();
			unknown1C = stream.readInt();
		}

		@Override
		public void write(StateOutputStream stream) throws IOException {
			stream.writeVersion(STATE_VERSION);
			stream.writeInt(unknown00);
			stream.writeInt(unknown04);
			stream.writeInt(unknown08);
			stream.writeInt(unknown0C);
			stream.writeInt(unknown10);
			stream.writeInt(unknown14);
			stream.writeInt(unknown18);
			stream.writeInt(unknown1C);
		}

		public void reset() {
			unknown00 = 0;
			unknown04 = 0;
			unknown08 = 0;
			unknown0C = 0;
			unknown10 = 0;
			unknown14 = 0;
			unknown18 = 0;
			unknown1C = 0;
		}
	}

	public MMIOHandlerPower(int baseAddress) {
		super(baseAddress);
	}

	@Override
	public void read(StateInputStream stream) throws IOException {
		stream.readVersion(STATE_VERSION);
		unknown1.read(stream);
		unknown2.read(stream);
		unknown3.read(stream);
		super.read(stream);
	}

	@Override
	public void write(StateOutputStream stream) throws IOException {
		stream.writeVersion(STATE_VERSION);
		unknown1.write(stream);
		unknown2.write(stream);
		unknown3.write(stream);
		super.write(stream);
	}

	@Override
	public void reset() {
		super.reset();

		unknown1.reset();
		unknown2.reset();
		unknown3.reset();
	}

	@Override
	public int read32(int address) {
		int value;
		switch (address - baseAddress) {
			case 0x00:
			case 0x04:
			case 0x08:
			case 0x0C:
			case 0x10:
			case 0x14:
			case 0x18:
			case 0x1C: value = unknown1.read32(address - baseAddress); break;
			case 0x20:
			case 0x24:
			case 0x28:
			case 0x2C:
			case 0x30:
			case 0x34:
			case 0x38:
			case 0x3C: value = unknown2.read32(address - baseAddress - 0x20); break;
			case 0x40:
			case 0x44:
			case 0x48:
			case 0x4C:
			case 0x50:
			case 0x54:
			case 0x58:
			case 0x5C: value = unknown3.read32(address - baseAddress - 0x40); break;
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
			case 0x00:
			case 0x04:
			case 0x08:
			case 0x0C:
			case 0x10:
			case 0x14:
			case 0x18:
			case 0x1C: unknown1.write32(address - baseAddress, value); break;
			case 0x20:
			case 0x24:
			case 0x28:
			case 0x2C:
			case 0x30:
			case 0x34:
			case 0x38:
			case 0x3C: unknown2.write32(address - baseAddress - 0x20, value); break;
			case 0x40:
			case 0x44:
			case 0x48:
			case 0x4C:
			case 0x50:
			case 0x54:
			case 0x58:
			case 0x5C: unknown3.write32(address - baseAddress - 0x40, value); break;
			default: super.write32(address, value); break;
		}

		if (log.isTraceEnabled()) {
			log.trace(String.format("0x%08X - write32(0x%08X, 0x%08X) on %s", getPc(), address, value, this));
		}
	}
}
