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
package jpcsp.memory.mmio.wlan;

import java.io.IOException;

import org.apache.log4j.Logger;

import jpcsp.HLE.modules.sceWlan;
import jpcsp.state.StateInputStream;
import jpcsp.state.StateOutputStream;

/**
 * MMIO for Wlan when accessed from the Wlan Firmware (ARM processor)
 * at address 0x90000000.
 * 
 * @author gid15
 *
 */
public class MMIOHandlerWlanFirmware2 extends MMIOARMHandlerBase {
	public static Logger log = sceWlan.log;
	private static final int STATE_VERSION = 0;
	private int unknown824;
	private int unknown828;

	public MMIOHandlerWlanFirmware2(int baseAddress) {
		super(baseAddress);
	}

	@Override
	public void read(StateInputStream stream) throws IOException {
		stream.readVersion(STATE_VERSION);
		unknown824 = stream.readInt();
		unknown828 = stream.readInt();
		super.read(stream);
	}

	@Override
	public void write(StateOutputStream stream) throws IOException {
		stream.writeVersion(STATE_VERSION);
		stream.writeInt(unknown824);
		stream.writeInt(unknown828);
		super.write(stream);
	}

	@Override
	public int read8(int address) {
		int value;
		switch (address - baseAddress) {
			case 0x081C: value = 0; break;
			case 0x0824: value = 0; break;
			default: value = super.read8(address); break;
		}

		if (log.isTraceEnabled()) {
			log.trace(String.format("0x%08X - read8(0x%08X) returning 0x%02X", getPc(), address, value));
		}

		return value;
	}

	@Override
	public int read16(int address) {
		int value;
		switch (address - baseAddress) {
			case 0x0800: value = 0; break;
			case 0x0804: value = 0; break;
			default: value = super.read16(address); break;
		}

		if (log.isTraceEnabled()) {
			log.trace(String.format("0x%08X - read16(0x%08X) returning 0x%04X", getPc(), address, value));
		}

		return value;
	}

	@Override
	public int read32(int address) {
		int value;
		switch (address - baseAddress) {
			case 0x9000: value = 0; break;
			case 0x0808: value = 0; break;
			default: value = super.read32(address); break;
		}

		if (log.isTraceEnabled()) {
			log.trace(String.format("0x%08X - read32(0x%08X) returning 0x%08X", getPc(), address, value));
		}

		return value;
	}

	@Override
	public void write8(int address, byte value) {
		final int value8 = value & 0xFF;
		switch (address - baseAddress) {
			case 0x0000: if (value8 != 0x01) { super.write8(address, value); }; break;
			case 0x0004: if (value8 != 0x00) { super.write8(address, value); }; break;
			case 0x0804: if (value8 != 0x82 && value8 != 0x58 && value8 != 0x54) { super.write8(address, value); }; break;
			case 0x0824: unknown824 = value8; break;
			case 0x0828: unknown828 = value8; break;
			default: super.write8(address, value); break;
		}

		if (log.isTraceEnabled()) {
			log.trace(String.format("0x%08X - write8(0x%08X, 0x%02X) on %s", getPc(), address, value8, this));
		}
	}

	@Override
	public void write16(int address, short value) {
		final int value16 = value & 0xFFFF;
		switch (address - baseAddress) {
			case 0x0800: if (value16 != 0x8 && value16 != 0x1 && value16 != 0x0) { super.write16(address, value); }; break;
			case 0x0804: if (value16 != 0x0) { super.write16(address, value); }; break;
			case 0x0808: if (value16 != 0x542) { super.write16(address, value); }; break;
			default: super.write16(address, value); break;
		}

		if (log.isTraceEnabled()) {
			log.trace(String.format("0x%08X - write16(0x%08X, 0x%04X) on %s", getPc(), address, value16, this));
		}
	}
}
