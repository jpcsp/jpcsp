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
	private final MMIOHandlerWlanFirmware handlerWlanFirmware;

	public MMIOHandlerWlanFirmware2(int baseAddress, MMIOHandlerWlanFirmware handlerWlanFirmware) {
		super(baseAddress);
		this.handlerWlanFirmware = handlerWlanFirmware;
	}

	@Override
	public void read(StateInputStream stream) throws IOException {
		stream.readVersion(STATE_VERSION);
		super.read(stream);
	}

	@Override
	public void write(StateOutputStream stream) throws IOException {
		stream.writeVersion(STATE_VERSION);
		super.write(stream);
	}

	@Override
	public int read8(int address) {
		int value;
		switch (address - baseAddress) {
			case 0x081C: value = 0; break;
			case 0x0824: value = handlerWlanFirmware.readEEPROMCmd8(); break;
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
			case 0x0200: value = 0; break;
			case 0x0208: value = 0; break;
			case 0x020C: value = 0; break;
			case 0x0224: value = 0; break;
			case 0x0228: value = 0; break;
			case 0x0238: value = 0; break;
			case 0x0808: value = 0; break;
			case 0x8000: value = handlerWlanFirmware.getInterrupt(); break;
			case 0x8008: value = 0; break;
			case 0x9000: value = 0; break;
			case 0x9024: value = 0; break;
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
			case 0x0804: handlerWlanFirmware.eepromMode = value8; if (value8 != 0x82 && value8 != 0x58 && value8 != 0x54) { super.write8(address, value); }; break;
			case 0x0824: handlerWlanFirmware.writeEEPROMCmdLow8(value8); break;
			case 0x0828: handlerWlanFirmware.writeEEPROMCmdHigh8(value8); break;
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
			case 0x0800: if (value16 != 0x0 && value16 != 0x1 && value16 != 0x4 && value16 != 0x8 && value16 != 0x10 && value16 != 0x18 && value16 != 0x20) { super.write16(address, value); }; break;
			case 0x0804: if (value16 != 0x0) { super.write16(address, value); }; break;
			case 0x0808: if (value16 != 0x542) { super.write16(address, value); }; break;
			default: super.write16(address, value); break;
		}

		if (log.isTraceEnabled()) {
			log.trace(String.format("0x%08X - write16(0x%08X, 0x%04X) on %s", getPc(), address, value16, this));
		}
	}

	@Override
	public void write32(int address, int value) {
		switch (address - baseAddress) {
			case 0x0200: if (value != 0x4 && value != 0xC) { super.write32(address, value); } break;
			case 0x0208: if (value != 0x0) { super.write32(address, value); } break;
			case 0x020C: if (value != 0x0 && value != 0x2 && value != 0x10) { super.write32(address, value); } break;
			case 0x0224: if (value != 0x2) { super.write32(address, value); } break;
			case 0x0228: if (value != 0x40) { super.write32(address, value); } break;
			case 0x0238: if (value != 0x0) { super.write32(address, value); } break;
			case 0x8008: if (value != 0x10 && value != 0x100 && value != 0x400 && value != 0x8000 && value != 0x10000 && value != 0x185F0) { super.write32(address, value); } break;
			case 0x800C: if (value != 0x100 && value != 0x400 && value != 0x8000 && value != 0x10000) { super.write32(address, value); } break;
			case 0x9000: if (value != 0xA) { super.write32(address, value); } break;
			case 0x9010: if (value != 0xF) { super.write32(address, value); } break;
			case 0x9024: if (value != 0x1) { super.write32(address, value); } break;
			case 0x9028: if (value != 0x1) { super.write32(address, value); } break;
			default: super.write32(address, value); break;
		}

		if (log.isTraceEnabled()) {
			log.trace(String.format("0x%08X - write32(0x%08X, 0x%08X) on %s", getPc(), address, value, this));
		}
	}
}
