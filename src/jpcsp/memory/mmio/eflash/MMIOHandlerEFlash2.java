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

public class MMIOHandlerEFlash2 extends MMIOHandlerBase {
	public static Logger log = sceEFlash.log;
	private static final int STATE_VERSION = 0;
	private int unknown1;
	private int unknown2;
	private int unknown3;
	private int unknown4;
	private int unknown5;
	private int unknown6;
	private int unknown7;
	private int unknownE;

	public MMIOHandlerEFlash2(int baseAddress) {
		super(baseAddress);

		reset();
	}

	@Override
	public void read(StateInputStream stream) throws IOException {
		stream.readVersion(STATE_VERSION);
		unknown1 = stream.readInt();
		unknown2 = stream.readInt();
		unknown3 = stream.readInt();
		unknown4 = stream.readInt();
		unknown5 = stream.readInt();
		unknown6 = stream.readInt();
		unknown7 = stream.readInt();
		unknownE = stream.readInt();
		super.read(stream);
	}

	@Override
	public void write(StateOutputStream stream) throws IOException {
		stream.writeVersion(STATE_VERSION);
		stream.writeInt(unknown1);
		stream.writeInt(unknown2);
		stream.writeInt(unknown3);
		stream.writeInt(unknown4);
		stream.writeInt(unknown5);
		stream.writeInt(unknown6);
		stream.writeInt(unknown7);
		stream.writeInt(unknownE);
		super.write(stream);
	}

	@Override
	public void reset() {
		unknown1 = 0;
		unknown2 = 0;
		unknown3 = 0;
		unknown4 = 0;
		unknown5 = 0;
		unknown6 = 0;
		unknown7 = 0;
		unknownE = 0;

		super.reset();
	}

	@Override
	public int read8(int address) {
		int value;
		switch (address - baseAddress) {
			case 0x1: value = unknown1; break;
			case 0x2: value = unknown2; break;
			case 0x3: value = unknown3; break;
			case 0x4: value = unknown4; break;
			case 0x5: value = unknown5; break;
			case 0x6: value = unknown6; break;
			case 0x7: value = unknown7; break;
			case 0xE: value = unknownE; break;
			default: value = super.read8(address); break;
		}

		if (log.isTraceEnabled()) {
			log.trace(String.format("0x%08X - read8(0x%08X) returning 0x%02X", getPc(), address, value));
		}

		return value;
	}

	@Override
	public void write8(int address, byte value) {
		switch (address - baseAddress) {
			case 0xE: unknownE = value & 0xFF; break;
			default: super.write32(address, value); break;
		}

		if (log.isTraceEnabled()) {
			log.trace(String.format("0x%08X - write8(0x%08X, 0x%02X) on %s", getPc(), address, value, this));
		}
	}
}
