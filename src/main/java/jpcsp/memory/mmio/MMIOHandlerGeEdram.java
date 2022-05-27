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

import jpcsp.state.StateInputStream;
import jpcsp.state.StateOutputStream;

public class MMIOHandlerGeEdram extends MMIOHandlerBase {
	public static Logger log = MMIOHandlerGe.log;
	private static final int STATE_VERSION = 0;
	private int unknown00;
	private int unknown20;
	private int unknown30;
	private int unknown40;
	private int unknown70;
	private int unknown80;
	private int unknown90;

	public MMIOHandlerGeEdram(int baseAddress) {
		super(baseAddress);

		reset();
	}

	@Override
	public void read(StateInputStream stream) throws IOException {
		stream.readVersion(STATE_VERSION);
		unknown00 = stream.readInt();
		unknown20 = stream.readInt();
		unknown30 = stream.readInt();
		unknown40 = stream.readInt();
		unknown70 = stream.readInt();
		unknown80 = stream.readInt();
		unknown90 = stream.readInt();
		super.read(stream);
	}

	@Override
	public void write(StateOutputStream stream) throws IOException {
		stream.writeVersion(STATE_VERSION);
		stream.writeInt(unknown00);
		stream.writeInt(unknown20);
		stream.writeInt(unknown30);
		stream.writeInt(unknown40);
		stream.writeInt(unknown70);
		stream.writeInt(unknown80);
		stream.writeInt(unknown90);
		super.write(stream);
	}

	@Override
	public void reset() {
		super.reset();

		unknown00 = 0x00012223;
		unknown20 = 0;
		unknown30 = 0;
		unknown40 = 0;
		unknown70 = 0;
		unknown80 = 0;
		unknown90 = 0;
	}

	@Override
	public int read32(int address) {
		int value;
		switch (address - baseAddress) {
			case 0x00: value = unknown00; break;
			case 0x10: value = 0; break;
			case 0x20: value = unknown20; break;
			case 0x30: value = unknown30; break;
			case 0x40: value = unknown40; break;
			case 0x70: value = unknown70; break;
			case 0x80: value = unknown80; break;
			case 0x90: value = unknown90; break;
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
			case 0x00: unknown00 = value; break;
			case 0x10: if (value != 0 && value != 1 && value != 2) { super.write32(address, value); }; break;
			case 0x20: unknown20 = value; break;
			case 0x30: unknown30 = value; break;
			case 0x40: unknown40 = value; break;
			case 0x70: unknown70 = value; break;
			case 0x80: unknown80 = value; break;
			case 0x90: unknown90 = value; break;
			default: super.write32(address, value); break;
		}

		if (log.isTraceEnabled()) {
			log.trace(String.format("0x%08X - write32(0x%08X, 0x%08X) on %s", getPc(), address, value, this));
		}
	}
}
