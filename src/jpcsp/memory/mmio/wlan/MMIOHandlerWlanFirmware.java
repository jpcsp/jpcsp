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

import jpcsp.Memory;
import jpcsp.HLE.modules.sceWlan;
import jpcsp.state.StateInputStream;
import jpcsp.state.StateOutputStream;

/**
 * MMIO for Wlan when accessed from the Wlan Firmware (ARM processor)
 * at address 0x80000000.
 * 
 * @author gid15
 *
 */
public class MMIOHandlerWlanFirmware extends MMIOARMHandlerBase {
	public static Logger log = sceWlan.log;
	private static final int STATE_VERSION = 0;
	private static final int STATUS_COMPLETED = 0x4;
	private int addr;
	private int status;
	private int length;
	private int[] data;
	private int dataOffset;
	private int dataLength;

	public MMIOHandlerWlanFirmware(int baseAddress) {
		super(baseAddress);
	}

	@Override
	public void read(StateInputStream stream) throws IOException {
		stream.readVersion(STATE_VERSION);
		addr = stream.readInt();
		status = stream.readInt();
		length = stream.readInt();
		super.read(stream);
	}

	@Override
	public void write(StateOutputStream stream) throws IOException {
		stream.writeVersion(STATE_VERSION);
		stream.writeInt(addr);
		stream.writeInt(status);
		stream.writeInt(length);
		super.write(stream);
	}

	public void setData(int[] data, int dataLength) {
		this.data = data;
		this.dataLength = dataLength;
		dataOffset = 0;
	}

	private int getStatus( ) {
		if (data != null) {
			Memory mem = getMemory();
			if (log.isDebugEnabled()) {
				log.debug(String.format("Reading 0x%X / 0x%X", dataOffset, dataLength));
			}

			for (int i = 0; i < length; i += 4) {
				if (dataOffset < dataLength) {
					mem.write32(addr + i, data[dataOffset++]);
				} else {
					mem.write32(addr + i, 0);
				}
			}

			if (log.isDebugEnabled()) {
				if (length == 0x10 && mem.read32(addr + 0) == 0x1) {
					log.debug(String.format("Reading data to 0x%08X, length=0x%X", mem.read32(addr + 4), mem.read32(addr + 8) - 4));
				}
			}
		}

		return status | STATUS_COMPLETED;
	}

	@Override
	public int read8(int address) {
		int value;
		switch (address - baseAddress) {
			case 0xA100: value = 0; break;
			case 0xA104: value = 0; break;
			case 0xA108: value = 0; break;
			case 0xA10C: value = 0; break;
			case 0xA110: value = 0; break;
			case 0xA114: value = 0; break;
			case 0xA118: value = 0; break;
			case 0xA11C: value = 0; break;
			case 0xA120: value = 0; break;
			case 0xA124: value = 0; break;
			case 0xA128: value = 0; break;
			case 0xA12C: value = 0; break;
			case 0xA130: value = 0; break;
			case 0xA134: value = 0; break;
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
			case 0x2030: value = 0; break;
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
			case 0x0024: value = getStatus(); break;
			case 0x2018: value = 0; break;
			case 0x2030: value = 0; break;
			case 0x2060: value = 0; break;
			case 0x2064: value = 0; break;
			case 0xA100: value = 0; break;
			case 0xA104: value = 0; break;
			case 0xA108: value = 0; break;
			case 0xA10C: value = 0; break;
			case 0xA110: value = 0; break;
			case 0xA114: value = 0; break;
			case 0xA118: value = 0; break;
			case 0xA11C: value = 0; break;
			case 0xA120: value = 0; break;
			case 0xA124: value = 0; break;
			case 0xA128: value = 0; break;
			case 0xA12C: value = 0; break;
			case 0xA130: value = 0; break;
			case 0xA134: value = 0; break;
			case 0xA300: value = 0; break;
			case 0xA3F0: value = 0; break;
			case 0xA3F4: value = 0; break;
			case 0xA3F8: value = 0; break;
			case 0xA3FC: value = 0; break;
			case 0xA400: value = 0; break;
			case 0xA404: value = 0; break;
			case 0xA408: value = 0; break;
			case 0xA40C: value = 0; break;
			case 0xA410: value = 0; break;
			case 0xA414: value = 0; break;
			case 0xA41C: value = 0; break;
			case 0xA444: value = 0; break;
			case 0xA448: value = 0; break;
			case 0xA44C: value = 0; break;
			case 0xA450: value = 0; break;
			case 0xA454: value = 0; break;
			case 0xA45C: value = 0; break;
			case 0xA528: value = 0; break;
			case 0xA5F0: value = 0; break;
			case 0xA824: value = 0; break;
			default: value = super.read32(address); break;
		}

		if (log.isTraceEnabled()) {
			log.trace(String.format("0x%08X - read32(0x%08X) returning 0x%08X", getPc(), address, value));
		}

		return value;
	}

	@Override
	public void write16(int address, short value) {
		final int value16 = value & 0xFFFF;
		switch (address - baseAddress) {
			case 0x000C: length = value16; break;
			default: super.write16(address, value); break;
		}

		if (log.isTraceEnabled()) {
			log.trace(String.format("0x%08X - write16(0x%08X, 0x%04X) on %s", getPc(), address, value16, this));
		}
	}

	@Override
	public void write32(int address, int value) {
		switch (address - baseAddress) {
			case 0x0020: if (value != 0x1) { super.write32(address, value); } break;
			case 0x0024: if (value != 0x0) { super.write32(address, value); } break;
			case 0x0034: if (value != 0x4) { super.write32(address, value); } break;
			case 0x003C: addr = value; break;
			case 0x200C: if (value != 0xFFFFFFFF && value != 0x101073EA) { super.write32(address, value); } break;
			case 0x2030: if (value != 0x912 && value != 0x638C6 && value != 0x38C0) { super.write32(address, value); } break;
			case 0x2060: if (value != 0x0) { super.write32(address, value); } break;
			case 0x2064: if (value != 0x0) { super.write32(address, value); } break;
			case 0x2808: if (value != 0x10) { super.write32(address, value); } break;
			case 0x3000: if (value != 0xA) { super.write32(address, value); } break;
			case 0x3010: if (value != 0xB) { super.write32(address, value); } break;
			case 0x3024: if (value != 0x1) { super.write32(address, value); } break;
			case 0x3028: if (value != 0x1) { super.write32(address, value); } break;
			case 0xA000: if (value != 0x1000) { super.write32(address, value); } break;
			case 0xA004: if (value != 0x4) { super.write32(address, value); } break;
			case 0xA008: if (value != 0x4) { super.write32(address, value); } break;
			case 0xA010: if (value != 0x0) { super.write32(address, value); } break;
			case 0xA014: if (value != 0x0) { super.write32(address, value); } break;
			case 0xA018: if (value != 0x0) { super.write32(address, value); } break;
			case 0xA01C: if (value != 0x0) { super.write32(address, value); } break;
			case 0xA020: if (value != 0x0) { super.write32(address, value); } break;
			case 0xA024: if (value != 0x0) { super.write32(address, value); } break;
			case 0xA028: if (value != 0x0) { super.write32(address, value); } break;
			case 0xA02C: if (value != 0x0) { super.write32(address, value); } break;
			case 0xA040: if (value != 0x0) { super.write32(address, value); } break;
			case 0xA044: if (value != 0x0) { super.write32(address, value); } break;
			case 0xA048: if (value != 0x0) { super.write32(address, value); } break;
			case 0xA04C: if (value != 0x0) { super.write32(address, value); } break;
			case 0xA050: if (value != 0x0) { super.write32(address, value); } break;
			case 0xA054: if (value != 0x0) { super.write32(address, value); } break;
			case 0xA058: if (value != 0x0) { super.write32(address, value); } break;
			case 0xA05C: if (value != 0x0) { super.write32(address, value); } break;
			case 0xA060: if (value != 0x0) { super.write32(address, value); } break;
			case 0xA064: if (value != 0x0) { super.write32(address, value); } break;
			case 0xA068: if (value != 0x0) { super.write32(address, value); } break;
			case 0xA06C: if (value != 0x0) { super.write32(address, value); } break;
			case 0xA070: if (value != 0x0) { super.write32(address, value); } break;
			case 0xA074: if (value != 0x0) { super.write32(address, value); } break;
			case 0xA078: if (value != 0x0) { super.write32(address, value); } break;
			case 0xA07C: if (value != 0x0) { super.write32(address, value); } break;
			case 0xA0A0: if (value != 0x7) { super.write32(address, value); } break;
			case 0xA0A4: if (value != 0x1F) { super.write32(address, value); } break;
			case 0xA0A8: if (value != 0x7) { super.write32(address, value); } break;
			case 0xA0AC: if (value != 0x1F) { super.write32(address, value); } break;
			case 0xA0B0: if (value != 0x7) { super.write32(address, value); } break;
			case 0xA0B4: if (value != 0x1F) { super.write32(address, value); } break;
			case 0xA0B8: if (value != 0x7) { super.write32(address, value); } break;
			case 0xA0BC: if (value != 0x1F) { super.write32(address, value); } break;
			case 0xA0C0: if (value != 0x7) { super.write32(address, value); } break;
			case 0xA0C4: if (value != 0x1F) { super.write32(address, value); } break;
			case 0xA0C8: if (value != 0x7) { super.write32(address, value); } break;
			case 0xA0CC: if (value != 0x1F) { super.write32(address, value); } break;
			case 0xA0D0: if (value != 0x7) { super.write32(address, value); } break;
			case 0xA0D4: if (value != 0x1F) { super.write32(address, value); } break;
			case 0xA0D8: if (value != 0x7) { super.write32(address, value); } break;
			case 0xA0DC: if (value != 0x1F) { super.write32(address, value); } break;
			case 0xA0E0: if (value != 0x1F) { super.write32(address, value); } break;
			case 0xA100: if (value != 0x0) { super.write32(address, value); } break;
			case 0xA104: if (value != 0x1000) { super.write32(address, value); } break;
			case 0xA108: if (value != 0x1000) { super.write32(address, value); } break;
			case 0xA10C: if (value != 0x1000) { super.write32(address, value); } break;
			case 0xA110: if (value != 0x1000) { super.write32(address, value); } break;
			case 0xA114: if (value != 0x1000) { super.write32(address, value); } break;
			case 0xA118: if (value != 0x1000) { super.write32(address, value); } break;
			case 0xA11C: if (value != 0x1000) { super.write32(address, value); } break;
			case 0xA120: if (value != 0x1000) { super.write32(address, value); } break;
			case 0xA124: if (value != 0x1000) { super.write32(address, value); } break;
			case 0xA128: if (value != 0x1000) { super.write32(address, value); } break;
			case 0xA12C: if (value != 0x1000) { super.write32(address, value); } break;
			case 0xA130: if (value != 0x1000) { super.write32(address, value); } break;
			case 0xA134: if (value != 0x1000) { super.write32(address, value); } break;
			case 0xA138: if (value != 0x56E) { super.write32(address, value); } break;
			case 0xA160: if (value != 0x20) { super.write32(address, value); } break;
			case 0xA164: if (value != 0x20) { super.write32(address, value); } break;
			case 0xA180: if (value != 0x0) { super.write32(address, value); } break;
			case 0xA184: if (value != 0x0) { super.write32(address, value); } break;
			case 0xA188: if (value != 0x0) { super.write32(address, value); } break;
			case 0xA18C: if (value != 0x0) { super.write32(address, value); } break;
			case 0xA190: if (value != 0x0) { super.write32(address, value); } break;
			case 0xA1C0: if (value != 0x13A) { super.write32(address, value); } break;
			case 0xA1C4: if (value != 0xA2 && value != 0x102) { super.write32(address, value); } break;
			case 0xA1C8: if (value != 0x7E && value != 0xDE) { super.write32(address, value); } break;
			case 0xA1CC: if (value != 0x74 && value != 0xD4) { super.write32(address, value); } break;
			case 0xA1D0: if (value != 0xCF) { super.write32(address, value); } break;
			case 0xA1D4: if (value != 0x34) { super.write32(address, value); } break;
			case 0xA1D8: if (value != 0x2D) { super.write32(address, value); } break;
			case 0xA1DC: if (value != 0x2A) { super.write32(address, value); } break;
			case 0xA1E0: if (value != 0x27) { super.write32(address, value); } break;
			case 0xA1E4: if (value != 0x26) { super.write32(address, value); } break;
			case 0xA1E8: if (value != 0x24) { super.write32(address, value); } break;
			case 0xA1EC: if (value != 0x23) { super.write32(address, value); } break;
			case 0xA1F0: if (value != 0x23) { super.write32(address, value); } break;
			case 0xA1F4: if (value != 0x23) { super.write32(address, value); } break;
			case 0xA204: if (value != 0xA) { super.write32(address, value); } break;
			case 0xA208: if (value != 0xA) { super.write32(address, value); } break;
			case 0xA210: if (value != 0x150) { super.write32(address, value); } break;
			case 0xA214: if (value != 0x150) { super.write32(address, value); } break;
			case 0xA240: if (value != 0xA4050) { super.write32(address, value); } break;
			case 0xA244: if (value != 0x50142020) { super.write32(address, value); } break;
			case 0xA260: if (value != 0x0) { super.write32(address, value); } break;
			case 0xA264: if (value != 0x0) { super.write32(address, value); } break;
			case 0xA268: if (value != 0x0) { super.write32(address, value); } break;
			case 0xA26C: if (value != 0x0) { super.write32(address, value); } break;
			case 0xA270: if (value != 0x0) { super.write32(address, value); } break;
			case 0xA274: if (value != 0x0) { super.write32(address, value); } break;
			case 0xA278: if (value != 0x0) { super.write32(address, value); } break;
			case 0xA27C: if (value != 0x0) { super.write32(address, value); } break;
			case 0xA2B0: if (value != 0x60) { super.write32(address, value); } break;
			case 0xA2B4: if (value != 0x60) { super.write32(address, value); } break;
			case 0xA2B8: if (value != 0x60) { super.write32(address, value); } break;
			case 0xA2BC: if (value != 0x60) { super.write32(address, value); } break;
			case 0xA2C0: if (value != 0x60) { super.write32(address, value); } break;
			case 0xA2C4: if (value != 0x60) { super.write32(address, value); } break;
			case 0xA2C8: if (value != 0x60) { super.write32(address, value); } break;
			case 0xA2CC: if (value != 0x60) { super.write32(address, value); } break;
			case 0xA300: if (value != 0xA) { super.write32(address, value); } break;
			case 0xA3E0: if (value != 0x60) { super.write32(address, value); } break;
			case 0xA3F0: if (value != 0x3800) { super.write32(address, value); } break;
			case 0xA3F4: if (value != 0x4) { super.write32(address, value); } break;
			case 0xA3F8: if (value != 0x640) { super.write32(address, value); } break;
			case 0xA3FC: if (value != 0x4) { super.write32(address, value); } break;
			case 0xA400: if (value != 0x640) { super.write32(address, value); } break;
			case 0xA404: if (value != 0x644) { super.write32(address, value); } break;
			case 0xA408: if (value != 0x640) { super.write32(address, value); } break;
			case 0xA40C: if (value != 0xC84) { super.write32(address, value); } break;
			case 0xA410: if (value != 0xC00) { super.write32(address, value); } break;
			case 0xA414: if (value != 0x4) { super.write32(address, value); } break;
			case 0xA418: if (value != 0x400) { super.write32(address, value); } break;
			case 0xA41C: if (value != 0x4) { super.write32(address, value); } break;
			case 0xA430: if (value != 0x0) { super.write32(address, value); } break;
			case 0xA434: if (value != 0x0) { super.write32(address, value); } break;
			case 0xA438: if (value != 0x0) { super.write32(address, value); } break;
			case 0xA43C: if (value != 0x0) { super.write32(address, value); } break;
			case 0xA440: if (value != 0x0) { super.write32(address, value); } break;
			case 0xA45C: if (value != 0x200) { super.write32(address, value); } break;
			case 0xA468: if (value != 0x1E) { super.write32(address, value); } break;
			case 0xA46C: if (value != 0xFDDDF015) { super.write32(address, value); } break;
			case 0xA470: if (value != 0xFFFFFFFF) { super.write32(address, value); } break;
			case 0xA500: if (value != 0x0) { super.write32(address, value); } break;
			case 0xA528: if (value != 0x0) { super.write32(address, value); } break;
			case 0xA52C: if (value != 0x0) { super.write32(address, value); } break;
			case 0xA588: if (value != 0x1) { super.write32(address, value); } break;
			case 0xA58C: if (value != 0x18000) { super.write32(address, value); } break;
			case 0xA5F0: if (value != 0x1) { super.write32(address, value); } break;
			case 0xA618: if (value != 0x64) { super.write32(address, value); } break;
			case 0xA620: if (value != 0xFF) { super.write32(address, value); } break;
			case 0xA628: if (value != 0xFF) { super.write32(address, value); } break;
			case 0xA62C: if (value != 0x0) { super.write32(address, value); } break;
			case 0xA640: if (value != 0x1FF) { super.write32(address, value); } break;
			case 0xA650: if (value != 0x0) { super.write32(address, value); } break;
			case 0xA660: if (value != 0x28) { super.write32(address, value); } break;
			case 0xA670: if (value != 0x14) { super.write32(address, value); } break;
			case 0xA674: if (value != 0xA) { super.write32(address, value); } break;
			case 0xA678: if (value != 0x10) { super.write32(address, value); } break;
			case 0xA67C: if (value != 0x10) { super.write32(address, value); } break;
			case 0xA680: if (value != 0x16) { super.write32(address, value); } break;
			case 0xA684: if (value != 0x16) { super.write32(address, value); } break;
			case 0xA688: if (value != 0x16) { super.write32(address, value); } break;
			case 0xA68C: if (value != 0x16) { super.write32(address, value); } break;
			case 0xA690: if (value != 0x16) { super.write32(address, value); } break;
			case 0xA694: if (value != 0x16) { super.write32(address, value); } break;
			case 0xA698: if (value != 0x16) { super.write32(address, value); } break;
			case 0xA69C: if (value != 0x16) { super.write32(address, value); } break;
			case 0xA800: if (value != 0x1 && value != 0x4 && value != 0x2) { super.write32(address, value); } break;
			case 0xA824: if (value != 0x1000008 && value != 0x0 && value != 0x80) { super.write32(address, value); } break;
			case 0xA874: if (value != 0x1) { super.write32(address, value); } break;
			case 0xA878: if (value != 0x20000) { super.write32(address, value); } break;
			case 0xA8D0: if (value != 0x00) { super.write32(address, value); } break;
			case 0xA8D4: if (value != 0xFFFFFFFF) { super.write32(address, value); } break;
			default: super.write32(address, value); break;
		}

		if (log.isTraceEnabled()) {
			log.trace(String.format("0x%08X - write32(0x%08X, 0x%08X) on %s", getPc(), address, value, this));
		}
	}
}
