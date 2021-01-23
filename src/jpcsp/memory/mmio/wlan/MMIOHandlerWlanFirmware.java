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

import static jpcsp.hardware.Wlan.MAC_ADDRESS_LENGTH;
import static jpcsp.hardware.Wlan.getMacAddress;
import static jpcsp.util.Utilities.clearFlag;
import static jpcsp.util.Utilities.hasBit;
import static jpcsp.util.Utilities.readUnaligned16;
import static jpcsp.util.Utilities.readUnaligned32;
import static jpcsp.util.Utilities.setBit;
import static jpcsp.util.Utilities.setFlag;
import static jpcsp.util.Utilities.writeUnaligned16;
import static jpcsp.util.Utilities.writeUnaligned32;

import java.io.IOException;
import java.util.Arrays;

import org.apache.log4j.Logger;

import jpcsp.Memory;
import jpcsp.HLE.modules.sceWlan;
import jpcsp.hardware.Model;
import jpcsp.state.StateInputStream;
import jpcsp.state.StateOutputStream;
import jpcsp.util.Utilities;

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
	public static final int INTERRUPT_UNKNOWN_4 = 4;
	public static final int INTERRUPT_UNKNOWN_8 = 8;
	public static final int INTERRUPT_UNKNOWN_10 = 10;
	public static final int INTERRUPT_UNKNOWN_15 = 15;
	public static final int INTERRUPT_UNKNOWN_16 = 16;
	private static int EEPROM_ADDRESS_SIZE = 3;
	private int addr;
	private int status;
	private int length;
	private int interrupt;
	private int addr0038;
	private int addr0040;
	private int addr0044;
	private int unknown2030;
	private int unknown3024;
	private int addrA000;
	private int addrA004;
	private int addrA008;
	private int lengthA3F4;
	private int addrA3F4;
	private int lengthA3FC;
	private int addrA3FC;
	private int lengthA404;
	private int addrA404;
	private int lengthA40C;
	private int addrA40C;
	private int lengthA414;
	private int addrA414;
	private int lengthA41C;
	private int addrA41C;
	private int unknownA510;
	private int unknownA53C;
	private int lengthE810;
	private int addrE810;
	private int addrE820;
	protected int eepromMode;
	private int eepromIndex;
	private int eepromCmd;
	private final byte[] eepromData = new byte[0x200]; 
	private final byte[] macAddress = new byte[MAC_ADDRESS_LENGTH];
	private volatile byte[] data;
	private volatile int dataOffset;
	private volatile int dataLength;

	public MMIOHandlerWlanFirmware(int baseAddress) {
		super(baseAddress);

		if (Model.getGeneration() >= 2) {
			// Required for PSP generation 2 or later
			EEPROM_ADDRESS_SIZE = 2;
		}

		Arrays.fill(macAddress, (byte) 0);
		initEEPROM();
	}

	public void test() {
		interrupt = setBit(interrupt, INTERRUPT_UNKNOWN_4);
		interrupt = setBit(interrupt, INTERRUPT_UNKNOWN_8);
		interrupt = setBit(interrupt, INTERRUPT_UNKNOWN_10);
		interrupt = setBit(interrupt, INTERRUPT_UNKNOWN_15);
		interrupt = setBit(interrupt, INTERRUPT_UNKNOWN_16);
		unknown3024 = 0x1;
		unknownA510 = 0x6F22C8BF;
		status |= STATUS_COMPLETED;
		Memory mem = getMemory();
		mem.write16(addr + 0, (short) MMIOHandlerWlan.CMD_GET_HW_SPEC);
		mem.write16(addr + 2, (short) 46);
		mem.write16(addr + 4, (short) 0);
	}

	@Override
	public void read(StateInputStream stream) throws IOException {
		stream.readVersion(STATE_VERSION);
		EEPROM_ADDRESS_SIZE = stream.readInt();
		addr = stream.readInt();
		status = stream.readInt();
		length = stream.readInt();
		interrupt = stream.readInt();
		addr0038 = stream.readInt();
		addr0040 = stream.readInt();
		addr0044 = stream.readInt();
		unknown2030 = stream.readInt();
		unknown3024 = stream.readInt();
		addrA000 = stream.readInt();
		addrA004 = stream.readInt();
		addrA008 = stream.readInt();
		lengthA3F4 = stream.readInt();
		addrA3F4 = stream.readInt();
		lengthA3FC = stream.readInt();
		addrA3FC = stream.readInt();
		lengthA404 = stream.readInt();
		addrA404 = stream.readInt();
		lengthA40C = stream.readInt();
		addrA40C = stream.readInt();
		lengthA414 = stream.readInt();
		addrA414 = stream.readInt();
		lengthA41C = stream.readInt();
		addrA41C = stream.readInt();
		unknownA510 = stream.readInt();
		unknownA53C = stream.readInt();
		lengthE810 = stream.readInt();
		addrE810 = stream.readInt();
		addrE820 = stream.readInt();
		eepromMode = stream.readInt();
		eepromIndex = stream.readInt();
		eepromCmd = stream.readInt();
		stream.readBytes(eepromData);
		stream.readBytes(macAddress);
		super.read(stream);
	}

	@Override
	public void write(StateOutputStream stream) throws IOException {
		stream.writeVersion(STATE_VERSION);
		stream.writeInt(EEPROM_ADDRESS_SIZE);
		stream.writeInt(addr);
		stream.writeInt(status);
		stream.writeInt(length);
		stream.writeInt(interrupt);
		stream.writeInt(addr0038);
		stream.writeInt(addr0040);
		stream.writeInt(addr0044);
		stream.writeInt(unknown2030);
		stream.writeInt(unknown3024);
		stream.writeInt(addrA000);
		stream.writeInt(addrA004);
		stream.writeInt(addrA008);
		stream.writeInt(lengthA3F4);
		stream.writeInt(addrA3F4);
		stream.writeInt(lengthA3FC);
		stream.writeInt(addrA3FC);
		stream.writeInt(lengthA404);
		stream.writeInt(addrA404);
		stream.writeInt(lengthA40C);
		stream.writeInt(addrA40C);
		stream.writeInt(lengthA414);
		stream.writeInt(addrA414);
		stream.writeInt(lengthA41C);
		stream.writeInt(addrA41C);
		stream.writeInt(unknownA510);
		stream.writeInt(unknownA53C);
		stream.writeInt(lengthE810);
		stream.writeInt(addrE810);
		stream.writeInt(addrE820);
		stream.writeInt(eepromMode);
		stream.writeInt(eepromIndex);
		stream.writeInt(eepromCmd);
		stream.writeBytes(eepromData);
		stream.writeBytes(macAddress);
		super.write(stream);
	}

	private void checksumEEPROM(int offset, int length) {
		int checksum = 0;
		for (int i = 0; i < length; i++) {
			checksum += eepromData[offset + i] & 0xFF;
		}
		writeUnaligned32(eepromData, offset + length - 4, checksum);
	}

	private void initEEPROM() {
		// Clear EEPROM
		Arrays.fill(eepromData, (byte) 0);

		int offset1 = 0x80;
		int length1 = 0x40;
		int offset2 = offset1 + length1;
		int length2 = 0x84;
		int offset3 = 0x20;
		int length3 = 0x0;
		int offset4 = 0x50;

		writeUnaligned32(eepromData, 0x00, 0x38333058); // magic "830X"
		writeUnaligned32(eepromData, 0x04, length2); // boot2 length
		writeUnaligned32(eepromData, 0x08, offset2); // boot2 EEPROM offset
		System.arraycopy(getMacAddress(), 0, eepromData, 0x0C, MAC_ADDRESS_LENGTH); // MAC address
		Utilities.write8(eepromData, 0x12, 0x02); // MAC address followed by 0x02 0x00
		Utilities.write8(eepromData, 0x13, 0x00);
		writeUnaligned32(eepromData, 0x14, length3); // a length
		writeUnaligned32(eepromData, 0x18, offset3); // an EEPROM offset
		writeUnaligned32(eepromData, 0x1C, 0x0); // a destination pointer (in RAM?)

		writeUnaligned32(eepromData, offset3 + length3 + 0x4, setBit(0, 0));
		writeUnaligned32(eepromData, 0x48, 0x0004);
		writeUnaligned32(eepromData, 0x4C, offset4 & 0xFFFF);
		int value1 = 0x0004; // 16 bits
		int value2 = 0x0C; // 8 bits
		int checksum = 0x101 - ((value1 & 0xFF) + ((value1 >> 8) & 0xFF) + (value2 & 0xFF)); // 8 bits
		writeUnaligned32(eepromData, offset4, (value1 << 16) | (checksum << 8) | value2);
		writeUnaligned32(eepromData, offset4 + 4, 0xFFFFFFFF);

		eepromData[offset2 + 2] = (byte) 0x00;
		eepromData[offset2 + 3] = (byte) 0x14;

		// Compute checksums
		checksumEEPROM(offset1, length1);
		checksumEEPROM(offset2, length2);
	}

	public void setData(byte[] data, int dataLength) {
		this.data = data;
		this.dataLength = dataLength;
		dataOffset = 0;
	}

	public int getLength() {
		return length;
	}

	public int getAddr() {
		return addr;
	}

	public int getAddrE810() {
		return addrE810;
	}

	public int getLengthE810() {
		return lengthE810;
	}

	public void clearLengthE810() {
		lengthE810 = 0;
	}

	private int getStatus( ) {
		int returnedStatus = status;

		if (data != null && length > 0) {
			Memory mem = getMemory();
			if (log.isDebugEnabled()) {
				log.debug(String.format("Reading 0x%X / 0x%X", dataOffset, dataLength));
			}

			for (int i = 0; i < length; i++) {
				if (dataOffset < dataLength) {
					mem.write8(addr + i, data[dataOffset++]);
				} else {
					mem.write8(addr + i, (byte) 0);
				}
			}

			if (log.isDebugEnabled()) {
				if (length == 0x10 && mem.read32(addr + 0) == 0x1) {
					log.debug(String.format("Reading data to 0x%08X, length=0x%X", mem.read32(addr + 4), mem.read32(addr + 8) - 4));
				}
			}

			if (dataOffset >= dataLength) {
				dataOffset = 0;
				dataLength = 0;
				data = null;
			}
			returnedStatus |= STATUS_COMPLETED;
		}

		return returnedStatus;
	}

	private void clearStatus(int mask) {
		status &= mask;
	}

	public int getInterrupt() {
		return interrupt;
	}

	private void checkInterrupt() {
		if (interrupt != 0) {
			WlanEmulator.getInstance().getTxManager().triggerIrqException();
		}
	}

	private void clearInterrupt(int flags) {
		interrupt = clearFlag(interrupt, flags);
	}

	public void setInterrupt(int interruptBit) {
		interrupt = setBit(interrupt, interruptBit);

		// Both INTERRUPT_UNKNOWN_4 and unknown3024 bit 0 seems to be related
		if (interruptBit == INTERRUPT_UNKNOWN_4) {
			unknown3024 = setBit(unknown3024, 0);
		}
		if (interruptBit == INTERRUPT_UNKNOWN_8) {
			status = setFlag(status, STATUS_COMPLETED);
		}
		if (interruptBit == INTERRUPT_UNKNOWN_10) {
			unknownA510 = 0x6F22C8BF;
		}

		checkInterrupt();
	}

	private void clearUnknown3024(int mask) {
		unknown3024 &= mask;

		// Seems to clear the interrupt bit
		if (hasBit(mask, 0)) {
			clearInterrupt(INTERRUPT_UNKNOWN_4);
		}
	}

	private int getUnknown2030() {
		int value4 = 0; // [0..13]
		boolean flag = false;
		return flag ? 0x2000 | (value4 << 5) : value4;
//		return unknown2030;
	}

	private void writeEEPROMCmdMode54(int value, boolean highByte) {
		switch (eepromIndex) {
			case 0:
			case 1:
				if (highByte) {
					eepromCmd = (eepromCmd & 0x00FFFF) | (value << 16);
				} else {
					eepromCmd = (eepromCmd & 0xFF7FFF) | ((value & 0x80) << 8);
				}
				break;
			case 2:
			case 3:
				if (highByte) {
					eepromCmd = (eepromCmd & 0xFF80FF) | ((value & 0x7F) << 8);
				} else {
					eepromCmd = (eepromCmd & 0xFFFF00) | value;
				}
				break;
		}
		eepromIndex++;

		if (eepromIndex >= 4) {
			if (log.isDebugEnabled()) {
				log.debug(String.format("writeEEPROMCmdMode54 unknown eepromCmd=0x%06X", eepromCmd));
			}
			eepromCmd = 0;
			eepromIndex = 0;
		}
	}

	private void writeEEPROMCmdMode58(int value, boolean highByte) {
		if (value == 0x00 && highByte) {
			eepromCmd = 0x86 << 24;
			eepromIndex = 0;
			if (log.isDebugEnabled()) {
				log.debug(String.format("writeEEPROMCmdMode58 value=0x%02X", value, highByte));
			}
		} else if (value == 0x01 && highByte) {
			eepromCmd = 0x00 << 24;
			eepromIndex = 0;
			if (log.isDebugEnabled()) {
				log.debug(String.format("writeEEPROMCmdMode58 value=0x%02X", value, highByte));
			}
		} else if (value == 0x0C && highByte) {
			eepromCmd = 0x00 << 24;
			eepromIndex = 0;
			if (log.isDebugEnabled()) {
				log.debug(String.format("writeEEPROMCmdMode58 value=0x%02X", value, highByte));
			}
		} else {
			if (log.isDebugEnabled()) {
				log.debug(String.format("writeEEPROMCmdMode58 unknown value=0x%02X, highByte=%b", value, highByte));
			}
		}
	}

	private void writeEEPROMCmdMode82(int value) {
		switch (eepromIndex) {
			case 0: eepromCmd = (eepromCmd & 0x00FFFFFF) | (value << 24); break;
			case 1: eepromCmd = (eepromCmd & 0xFF00FFFF) | (value << 16); break;
			case 2: eepromCmd = (eepromCmd & 0xFFFF00FF) | (value <<  8); break;
			case 3: eepromCmd = (eepromCmd & 0xFFFFFF00) | (value      ); break;
		}
		eepromIndex++;

		if (eepromIndex == EEPROM_ADDRESS_SIZE + 1) {
			int cmd;
			int addr;
			switch (EEPROM_ADDRESS_SIZE) {
				case 2:
					cmd = (eepromCmd >> 24) & 0xFF;
					addr = (eepromCmd & 0x00FFFF00) >> 8;
					break;
				case 3:
					cmd = (eepromCmd >> 24) & 0xFF;
					addr = eepromCmd & 0x00FFFFFF;
					break;
				default:
					log.error(String.format("Unknown EEPROM_ADDRESS_SIZE=%d", EEPROM_ADDRESS_SIZE));
					cmd = 0;
					addr = 0;
					break;
			}

			if (cmd == 3) {
				// Read EEPROM
				eepromCmd = readUnaligned32(eepromData, addr);
			} else {
				log.error(String.format("writeEEPROMData8 unknown cmd=0x%02X", cmd));
				eepromCmd = 0;
			}
			eepromIndex = 0;

			if (log.isDebugEnabled()) {
				log.debug(String.format("writeEEPROMCmd8 cmd=0x%02X, addr=0x%X returning 0x%08X", cmd, addr, eepromCmd));
			}
		}
	}

	public void writeEEPROMCmdHigh8(int value) {
		switch (eepromMode) {
			case 0x54: writeEEPROMCmdMode54(value, true); break;
			case 0x58: writeEEPROMCmdMode58(value, true); break;
			default:
				log.error(String.format("writeEEPROMCmdHigh8 unknown eepromMode=0x%X, value=0x%X", eepromMode, value));
				break;
		}
	}

	public void writeEEPROMCmdLow8(int value) {
		switch (eepromMode) {
			case 0x54: writeEEPROMCmdMode54(value, false); break;
			case 0x58: writeEEPROMCmdMode58(value, false); break;
			case 0x82: writeEEPROMCmdMode82(value); break;
			default:
				log.error(String.format("writeEEPROMCmdLow8 unknown eepromMode=0x%X, value=0x%X", eepromMode, value));
				break;
		}
	}

	public int readEEPROMCmd8() {
		int value = 0;
		switch (eepromIndex) {
			case 0: value = eepromCmd >> 24; break;
			case 1: value = eepromCmd >> 16; break;
			case 2: value = eepromCmd >>  8; break;
			case 3: value = eepromCmd      ; break;
		}
		value &= 0xFF;
		eepromIndex++;

		if (eepromIndex == 4) {
			eepromCmd = 0;
			eepromIndex = 0;
		}

		return value;
	}

	private int getEEPROMConfig() {
		int config = 0;

		// Bit 4 is set if 2-byte EEPROM addresses should be used, but cleared if 3-byte addresses should be used
		if (EEPROM_ADDRESS_SIZE == 2) {
			config = setBit(config, 4);
		}

		return config;
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
			case 0xC100: value = 0; break;
			case 0xC104: value = 0; break;
			case 0xC11C: value = 0; break;
			case 0xC124: value = readEEPROMCmd8(); break;
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
			case 0x2000: value = getEEPROMConfig(); break;
			case 0x2018: value = 0x0003; break;
			case 0x2030: value = getUnknown2030() & 0xFFFF; break;
			case 0xA52C: value = readUnaligned16(macAddress, 4); break;
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
			case 0x000C: value = length; break;
			case 0x0010: value = 0; break;
			case 0x0024: value = getStatus(); break;
			case 0x0034: value = 0; break;
			case 0x2000: value = 0; break;
			case 0x200C: value = 0; break;
			case 0x2018: value = 0; break;
			case 0x2030: value = getUnknown2030(); break;
			case 0x2044: value = 0; break;
			case 0x204C: value = 0; break;
			case 0x2060: value = 0; break;
			case 0x2064: value = 0; break;
			case 0x2070: value = 0x00020000; break;
			case 0x2078: value = 0; break;
			case 0x2800: value = getInterrupt(); break;
			case 0x2808: value = 0; break;
			case 0x3024: value = unknown3024; break;
			case 0xA000: value = 0; break;
			case 0xA010: value = 0; break;
			case 0xA014: value = 0; break;
			case 0xA040: value = 0; break;
			case 0xA044: value = 0; break;
			case 0xA048: value = 0; break;
			case 0xA04C: value = 0; break;
			case 0xA074: value = 0; break;
			case 0xA07C: value = 0; break;
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
			case 0xA138: value = 0; break;
			case 0xA2D0: value = 0; break;
			case 0xA2D4: value = 0; break;
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
			case 0xA430: value = 0; break;
			case 0xA440: value = 0; break;
			case 0xA444: value = 0; break;
			case 0xA448: value = 0; break;
			case 0xA44C: value = 0; break;
			case 0xA450: value = 0; break;
			case 0xA454: value = 0; break;
			case 0xA458: value = 0; break;
			case 0xA45C: value = 0; break;
			case 0xA46C: value = 0; break;
			case 0xA510: value = unknownA510; break;
			case 0xA514: value = 0; break;
			case 0xA528: value = readUnaligned32(macAddress, 0); break;
			case 0xA52C: value = readUnaligned16(macAddress, 4); break;
			case 0xA530: value = 0; break;
			case 0xA534: value = 0; break;
			case 0xA53C: value = unknownA53C; break;
			case 0xA5F0: value = 0; break;
			case 0xA600: value = 0; break;
			case 0xA604: value = 0; break;
			case 0xA608: value = 0; break;
			case 0xA60C: value = 0; break;
			case 0xA610: value = 0; break;
			case 0xA614: value = 0; break;
			case 0xA618: value = 0; break;
			case 0xA61C: value = 0; break;
			case 0xA620: value = 0; break;
			case 0xA624: value = 0; break;
			case 0xA628: value = 0; break;
			case 0xA62C: value = 0; break;
			case 0xA644: value = 0; break;
			case 0xA650: value = 0; break;
			case 0xA658: value = 0; break;
			case 0xA8E4: value = 0; break;
			case 0xA824: value = 0; break;
			case 0xA848: value = 0; break;
			case 0xA890: value = 0; break;
			case 0xA930: value = 0; break;
			case 0xD00C: value = 0; break;
			case 0xE800: value = lengthE810; break;
			case 0xE840: value = 0; break;
			case 0xE848: value = 0; break;
			case 0xE8A0: value = 0; break;
			case 0xE8A4: value = 0; break;
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
			case 0xA550: if (value8 != 0x11) { super.write8(address, value); }; break;
			case 0xC000: if (value8 != 0x01) { super.write8(address, value); }; break;
			case 0xC004: if (value8 != 0x00) { super.write8(address, value); }; break;
			case 0xC100: if (value8 != 0x00) { super.write8(address, value); }; break;
			case 0xC104: eepromMode = value8; if (value8 != 0x00 && value8 != 0x82 && value8 != 0x54 && value8 != 0x58) { super.write8(address, value); }; break;
			case 0xC124: writeEEPROMCmdLow8(value8); break;
			case 0xC128: writeEEPROMCmdHigh8(value8); break;
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
			case 0x000C: length = value16; break;
			case 0x200C: if (value16 != 0x777F && value16 != 0xFFFF) { super.write16(address, value); } break;
			case 0x2030: unknown2030 = value16; break;
			case 0xA640: if (value16 != 0x2) { super.write16(address, value); } break;
			case 0xC100: if (value16 != 0x2 && value16 != 0x4 && value16 != 0x8) { super.write16(address, value); } break;
			case 0xC108: if (value16 != 0x582) { super.write16(address, value); } break;
			default: super.write16(address, value); break;
		}

		if (log.isTraceEnabled()) {
			log.trace(String.format("0x%08X - write16(0x%08X, 0x%04X) on %s", getPc(), address, value16, this));
		}
	}

	@Override
	public void write32(int address, int value) {
		switch (address - baseAddress) {
			case 0x0000: if (value != 0x0) { super.write32(address, value); } break;
			case 0x000C: length = value; break;
			case 0x0010: if (value != 0x0 && value != 0x4010) { super.write32(address, value); } break;
			case 0x0020: if (value != 0x0 && value != 0x1) { super.write32(address, value); } break;
			case 0x0024: clearStatus(value); break;
			case 0x0028: if (value != 0xFF) { super.write32(address, value); } break;
			case 0x002C: if (value != 0xFF) { super.write32(address, value); } break;
			case 0x0030: if (value != 0xFF) { super.write32(address, value); } break;
			case 0x0034: if (value != 0x4 && value != 0x5 && value != 0x8 && value != 0x10) { super.write32(address, value); } break;
			case 0x0038: addr0038 = value; break;
			case 0x003C: addr = value; break;
			case 0x0040: addr0040 = value; break;
			case 0x0044: addr0044 = value; break;
			case 0x0048: if (value != 0xC0000000) { super.write32(address, value); } break;
			case 0x0064: if (value != 0x200) { super.write32(address, value); } break;
			case 0x200C: if (value != 0xFFFFFFFF && value != 0x101073EA && value != 0x8 && value != 0x0) { super.write32(address, value); } break;
			case 0x2030: unknown2030 = value; break;
			case 0x2044: if (value != 0x0) { super.write32(address, value); } break;
			case 0x204C: if (value != 0x0 && value != 0x20) { super.write32(address, value); } break;
			case 0x2060: if (value != 0x0) { super.write32(address, value); } break;
			case 0x2064: if (value != 0x0) { super.write32(address, value); } break;
			case 0x2074: if (value != 0x80000000 && value != 0x80000001 && value != 0x3 && value != 0x4 && value != 0x5 && value != 0x6 && value != 0x7 && value != 0x8 && value != 0x9 && value != 0xA && value != 0xB && value != 0xC && value != 0xD && value != 0xE) { super.write32(address, value); } break;
			case 0x2078: if (value != 0xFF000007 && value != 0x5) { super.write32(address, value); } break;
			case 0x2808: if (value != 0x0 && value != 0x10 && value != 0x185F0 && value != 0x800000 && value != 0x100 && value != 0x400 && value != 0x8000 && value != 0x10000) { super.write32(address, value); } break;
			case 0x280C: clearInterrupt(value); break;
			case 0x3000: if (value != 0xA) { super.write32(address, value); } break;
			case 0x3010: if (value != 0xB) { super.write32(address, value); } break;
			case 0x3024: clearUnknown3024(value); break;
			case 0x3028: if (value != 0x1) { super.write32(address, value); } break;
			case 0xA000: addrA000 = value; break;
			case 0xA004: addrA004 = value; break;
			case 0xA008: addrA008 = value; break;
			case 0xA010: if (value != 0x0) { super.write32(address, value); } break;
			case 0xA014: if (value != 0x0) { super.write32(address, value); } break;
			case 0xA018: if (value != 0x0) { super.write32(address, value); } break;
			case 0xA01C: if (value != 0x0) { super.write32(address, value); } break;
			case 0xA020: if (value != 0x0) { super.write32(address, value); } break;
			case 0xA024: if (value != 0x0) { super.write32(address, value); } break;
			case 0xA028: if (value != 0x0) { super.write32(address, value); } break;
			case 0xA02C: if (value != 0x0) { super.write32(address, value); } break;
			case 0xA040: if (value != 0x0 && value != 0x4) { super.write32(address, value); } break;
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
			case 0xA090: if (value != 0xFF && value != 0x0) { super.write32(address, value); } break;
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
			case 0xA0E0: if (value != 0x1F && value != 0x5B) { super.write32(address, value); } break;
			case 0xA100: if (value != 0x0 && value != 0x14) { super.write32(address, value); } break;
			case 0xA104: if (value != 0x0 && value != 0x100 && value != 0x1000) { super.write32(address, value); } break;
			case 0xA108: if (value != 0x0 && value != 0x100 && value != 0x1000) { super.write32(address, value); } break;
			case 0xA10C: if (value != 0x0 && value != 0x100 && value != 0x1000) { super.write32(address, value); } break;
			case 0xA110: if (value != 0x0 && value != 0x100 && value != 0x1000) { super.write32(address, value); } break;
			case 0xA114: if (value != 0x0 && value != 0x100 && value != 0x1000) { super.write32(address, value); } break;
			case 0xA118: if (value != 0x0 && value != 0x100 && value != 0x1000) { super.write32(address, value); } break;
			case 0xA11C: if (value != 0x0 && value != 0x100 && value != 0x1000) { super.write32(address, value); } break;
			case 0xA120: if (value != 0x0 && value != 0x100 && value != 0x1000) { super.write32(address, value); } break;
			case 0xA124: if (value != 0x0 && value != 0x100 && value != 0x1000) { super.write32(address, value); } break;
			case 0xA128: if (value != 0x0 && value != 0x100 && value != 0x1000) { super.write32(address, value); } break;
			case 0xA12C: if (value != 0x0 && value != 0x100 && value != 0x1000) { super.write32(address, value); } break;
			case 0xA130: if (value != 0x0 && value != 0x100 && value != 0x1000) { super.write32(address, value); } break;
			case 0xA134: if (value != 0x0 && value != 0x100 && value != 0x1000 && value != 0xA) { super.write32(address, value); } break;
			case 0xA138: if (value != 0x14 && value != 0x56E) { super.write32(address, value); } break;
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
			case 0xA218: if (value != 0x150) { super.write32(address, value); } break;
			case 0xA240: if (value != 0xA4050 && value != 0x3000) { super.write32(address, value); } break;
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
			case 0xA2D0: if (value != 0x0 && value != 0x8000) { super.write32(address, value); } break;
			case 0xA2D4: if (value != 0x0 && value != 0x10) { super.write32(address, value); } break;
			case 0xA300: if (value != 0x0 && value != 0x8 && value != 0xA && value != 0xC) { super.write32(address, value); } break;
			case 0xA3E0: if (value != 0x60) { super.write32(address, value); } break;
			case 0xA3F0: lengthA3F4 = value; break;
			case 0xA3F4: addrA3F4 = value; break;
			case 0xA3F8: lengthA3FC = value; break;
			case 0xA3FC: addrA3FC = value; break;
			case 0xA400: lengthA404 = value; break;
			case 0xA404: addrA404 = value; break;
			case 0xA408: lengthA40C = value; break;
			case 0xA40C: addrA40C = value; break;
			case 0xA410: lengthA414 = value; break;
			case 0xA414: addrA414 = value; break;
			case 0xA418: lengthA41C = value; break;
			case 0xA41C: addrA41C = value; break;
			case 0xA430: if (value != 0x0) { super.write32(address, value); } break;
			case 0xA434: if (value != 0x0) { super.write32(address, value); } break;
			case 0xA438: if (value != 0x0) { super.write32(address, value); } break;
			case 0xA43C: if (value != 0x0) { super.write32(address, value); } break;
			case 0xA440: if (value != 0x0) { super.write32(address, value); } break;
			case 0xA45C: if (value != 0x200) { super.write32(address, value); } break;
			case 0xA468: if (value != 0x1E) { super.write32(address, value); } break;
			case 0xA46C: if (value != 0xFDDDF015 && value != 0xFFDDF015 && value != 0x40 && value != 0x0) { super.write32(address, value); } break;
			case 0xA470: if (value != 0xFFFFFFFF) { super.write32(address, value); } break;
			case 0xA500: if (value != 0x0 && value != 0x2) { super.write32(address, value); } break;
			case 0xA510: unknownA510 &= value; break; // Writing seems to clear bits
			case 0xA514: if (value != 0x0 && value != 0x6F32CCBF && value != 0x6F128CBF && value != 0x02000000 && value != 0x08000000) { super.write32(address, value); } break;
			case 0xA518: if (value != 0xFFFFFFFF) { super.write32(address, value); } break;
			case 0xA528: writeUnaligned32(macAddress, 0, value); break;
			case 0xA52C: writeUnaligned16(macAddress, 4, value & 0xFFFF); break;
			case 0xA530: if (value != 0x0 && (value & 0x000000FF) != 0x00000002) { super.write32(address, value); } break; // Seems to be 0 or a random value always ending with 02 (a locally administered MAC address?)
			case 0xA534: if (value != 0x0 && (value & 0xFFFF0000) != 0x00000000) { super.write32(address, value); } break; // Seems to be 0 or a random 16-bit value
			case 0xA53C: unknownA53C = value; if (value != 0x4 && value != 0x40404) { super.write32(address, value); } break;
			case 0xA554: if (value != 0x5F505350) { super.write32(address, value); } break;
			case 0xA558: if (value != 0x454C5541) { super.write32(address, value); } break;
			case 0xA55C: if (value != 0x39303053) { super.write32(address, value); } break;
			case 0xA560: if (value != 0x4C5F3138) { super.write32(address, value); } break;
			case 0xA564: if (value != 0x0000005F) { super.write32(address, value); } break;
			case 0xA568: if (value != 0x00000000) { super.write32(address, value); } break;
			case 0xA56C: if (value != 0x00000000) { super.write32(address, value); } break;
			case 0xA570: if (value != 0x00000000) { super.write32(address, value); } break;
			case 0xA588: if (value != 0x1) { super.write32(address, value); } break;
			case 0xA58C: if (value != 0x18000) { super.write32(address, value); } break;
			case 0xA5F0: if (value != 0x1 && value != 0x40000000) { super.write32(address, value); } break;
			case 0xA600: if (value != 0x0) { super.write32(address, value); } break;
			case 0xA604: if (value != 0x0) { super.write32(address, value); } break;
			case 0xA608: if (value != 0x32000) { super.write32(address, value); } break;
			case 0xA60C: if (value != 0x0) { super.write32(address, value); } break;
			case 0xA610: if (value != 0x0) { super.write32(address, value); } break;
			case 0xA614: if (value != 0x0) { super.write32(address, value); } break;
			case 0xA618: if (value != 0x64) { super.write32(address, value); } break;
			case 0xA61C: if (value != 0x0) { super.write32(address, value); } break;
			case 0xA620: if (value != 0xFF) { super.write32(address, value); } break;
			case 0xA624: if (value != 0x0) { super.write32(address, value); } break;
			case 0xA628: if (value != 0x0 && value != 0x1 && value != 0xFF) { super.write32(address, value); } break;
			case 0xA62C: if (value != 0x0) { super.write32(address, value); } break;
			case 0xA640: if (value != 0x1 && value != 0x80 && value != 0x100 && value != 0x1FF) { super.write32(address, value); } break;
			case 0xA644: if (value != 0x0 && value != 0x1) { super.write32(address, value); } break;
			case 0xA650: if (value != 0x0 && value != 0x4) { super.write32(address, value); } break;
			case 0xA658: if (value != 0x0) { super.write32(address, value); } break;
			case 0xA660: if (value != 0x28 && value != 0x2C) { super.write32(address, value); } break;
			case 0xA670: if (value != 0x14) { super.write32(address, value); } break;
			case 0xA674: if (value != 0xA) { super.write32(address, value); } break;
			case 0xA678: if (value != 0x10) { super.write32(address, value); } break;
			case 0xA67C: if (value != 0x10 && value != 0x50) { super.write32(address, value); } break;
			case 0xA680: if (value != 0x16) { super.write32(address, value); } break;
			case 0xA684: if (value != 0x16) { super.write32(address, value); } break;
			case 0xA688: if (value != 0x16) { super.write32(address, value); } break;
			case 0xA68C: if (value != 0x16) { super.write32(address, value); } break;
			case 0xA690: if (value != 0x16) { super.write32(address, value); } break;
			case 0xA694: if (value != 0x16) { super.write32(address, value); } break;
			case 0xA698: if (value != 0x16 && value != 0x19 && value != 0x32) { super.write32(address, value); } break;
			case 0xA69C: if (value != 0x16 && value != 0x32) { super.write32(address, value); } break;
			case 0xA800: if (value != 0x1 && value != 0x4 && value != 0x2) { super.write32(address, value); } break;
			case 0xA820: if (value != 0x10200 && value != 0x10C00) { super.write32(address, value); } break;
			case 0xA824: if (value != 0x1000008 && value != 0x0 && value != 0x80 && value != 0x80008 && value != 0x400) { super.write32(address, value); } break;
			case 0xA86C: if (value != 0x14 && value != 0x1F) { super.write32(address, value); } break;
			case 0xA874: if (value != 0x1) { super.write32(address, value); } break;
			case 0xA878: if (value != 0x20000) { super.write32(address, value); } break;
			case 0xA87C: if (value != 0xC8 && value != 0x1FF) { super.write32(address, value); } break;
			case 0xA8D0: if (value != 0x00) { super.write32(address, value); } break;
			case 0xA8D4: if (value != 0xFFFFFFFF) { super.write32(address, value); } break;
			case 0xA8E4: if (value != 0x0 && value != 0x2) { super.write32(address, value); } break;
			case 0xD00C: if (value != 0x10) { super.write32(address, value); } break;
			case 0xE800: lengthE810 = value; break;
			case 0xE810: addrE810 = value; break;
			case 0xE820: addrE820 = value; break;
			case 0xE830: if (value != 0x0) { super.write32(address, value); } break;
			case 0xE840: if (value != 0x0 && value != 0x100000 && value != 0x1BC0) { super.write32(address, value); } break;
			case 0xE844: if (value != 0x0) { super.write32(address, value); } break;
			case 0xE8A0: if (value != 0x0) { super.write32(address, value); } break;
			case 0xE8A4: if (value != 0x0) { super.write32(address, value); } break;
			default: super.write32(address, value); break;
		}

		if (log.isTraceEnabled()) {
			log.trace(String.format("0x%08X - write32(0x%08X, 0x%08X) on %s", getPc(), address, value, this));
		}
	}
}
