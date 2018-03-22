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

import static jpcsp.util.Utilities.endianSwap16;

import java.util.Arrays;

import org.apache.log4j.Logger;

import jpcsp.HLE.modules.sceWlan;
import jpcsp.util.Utilities;

public class MMIOHandlerWlan extends MMIOHandlerBase {
	public static Logger log = sceWlan.log;
	private int command;
	private Object dmaLock = new Object();
	private final int data[] = new int[4096];
	private int unknown38;
	private int unknown3C;
	private int unknown40;
	private int index;
	private int totalLength;
	private int currentLength;

	public MMIOHandlerWlan(int baseAddress) {
		super(baseAddress);

		unknown38 = 0x4000 | 0x2000 | 0x1000 | 0x0002;
	}

	private void setUnknown3C(int value) {
		unknown3C = value & ~0x200;
	}

	private int getCommandCode() {
		return command >>> 12;
	}

	private int getCommandLength() {
		return command & 0xFFF;
	}

	private void setCommand(int value) {
		synchronized(dmaLock) {
			command = value;
			index = 0;
			unknown38 &= 0xFFFFFFF0;

			switch (command) {
				case 0x7001:
					setData8(0, 0x80);
					totalLength = getCommandLength();
					currentLength = 0;
					break;
				case 0x4001:
				case 0x4002:
				case 0x4004:
					totalLength = getCommandLength();
					currentLength = 0;
					break;
				case 0x5040:
					clearData(getCommandLength());
					// Possible values:
					// 0x0011 0x0001 0x0001
					// 0x0011 0x0001 0x1B18
					// 0x0011 0x0002 0x1B11
					// 0x0011 0x0002 0x0B11
					setData32(0, swap32(0x00010011));
					setData32(4, swap32(0x00001B18));

					setData32(8 , swap32(0x12340001));
					setData32(12, swap32(0x00000040));
					break;
				case 0x5200:
					clearData(getCommandLength());
					break;
				case 0x8004:
					break;
				case 0x9007:
					break;
				case 0xB001:
					break;
				default:
					log.error(String.format("setCommand unknown command=0x%X", command));
					Arrays.fill(data, 0);
					break;
			}
		}
	}

	private int readData8() {
		while (true) {
			synchronized (dmaLock) {
				int commandCode = getCommandCode();
				if (commandCode == 0x5 || commandCode == 0x7 || commandCode == 0x4) {
					if ((unknown38 & 0x3) != 0x2) {
						break;
					}
				}
			}
			Utilities.sleep(100);
		}

		int value;
		synchronized (dmaLock) {
			value = data[index++] & 0xFF;

			if (index == getCommandLength()) {
				currentLength += index;

				unknown38 |= 0x0002;
				if (currentLength >= totalLength) {
					unknown38 |= 0x0001;
				}
			}
		}

		return value;
	}

	private int readData16() {
		return readData8() | (readData8() << 8);
	}

	private int readData32() {
		return readData8() | (readData8() << 8) | (readData8() << 16) | (readData8() << 24);
	}

	private int getData16(int offset) {
		return data[offset] | (data[offset + 1] << 8);
	}

	private int getData32(int offset) {
		return data[offset] | (data[offset + 1] << 8) | (data[offset + 2] << 16) | (data[offset + 3] << 24);
	}

	static private int swap32(int value) {
		return (value >>> 16) | (value << 16);
	}

	private void setData8(int offset, int value) {
		data[offset] = value & 0xFF;
	}

	private void setData16(int offset, int value) {
		setData8(offset++, value);
		setData8(offset, value >> 8);
	}

	private void setData32(int offset, int value) {
		setData8(offset++, value);
		setData8(offset++, value >> 8);
		setData8(offset++, value >> 16);
		setData8(offset, value >> 24);
	}

	private void clearData(int offset, int length) {
		for (int i = 0; i < length; i++) {
			setData8(offset++, 0);
		}
	}

	private void clearData(int length) {
		clearData(0, length);
	}

	private void writeData8(int value) {
		data[index++] = value & 0xFF;
	}

	private void writeData32(int value) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("writeData32 command=0x%X, data=0x%08X, index=0x%X", command, value, index));
		}
		writeData8(value);
		writeData8(value >> 8);
		writeData8(value >> 16);
		writeData8(value >>> 24);

		switch (command) {
			case 0x8004:
				if (index >= 8) {
					switch (getData32(0)) {
						case 0x122:
						case 0x123:
						case 0x124:
						case 0x127:
							clearData(8);
							setData8(0, 0x00); // Returning 1 unknown byte
							break;
						case 0x125:
							clearData(8);
							setData8(0, 0x01); // Returning 1 unknown byte
							break;
						case 0x154:
							clearData(8);
							setData8(0, 0xFF); // Returning 1 unknown byte
							break;
						case 0x24A:
							clearData(8);
							setData16(0, endianSwap16(0xC00)); // Returning unknown 16 bit value (it is a size for next request, must be between 4 and 0xC00)
							break;
						case 0x44E:
							clearData(8);
							setData32(0, 0x100F0000); // Returning unknown 32 bit value
							// Possible values are 0xNNNNxxxx, with NNNN being one of the following:
							// 0x1002
							// 0x1008
							// 0x100D
							// 0x100E
							// 0x100F
							// 0x1020
							// 0x1040
							// 0x1080
							// 0x10A0
							// 0x10C0
							break;
						case 0x1100000:
						case 0x1250000:
						case 0x1260000:
						case 0x1540000:
						case 0x15A0000:
						case 0x15C0000:
						case 0x15E0000:
							clearData(8);
							break;
						default:
							log.error(String.format("writeData32 unknown command=0x%X, data[0]=0x%08X, data[1]=0x%08X", command, getData32(0), getData32(4)));
							Arrays.fill(data, 0);
							break;
					}
				}
				break;
			case 0x9007:
				if (index >= 8) {
					totalLength = endianSwap16(getData16(1));
					if (log.isDebugEnabled()) {
						log.debug(String.format("writeData32 command=0x%X, totalLength=0x%X", command, totalLength));
					}
					currentLength = 0;
					unknown38 |= 0x0002;
					index = 0;
				}
				break;
			case 0xB001:
				if (index >= 8) {
					switch (getData32(0)) {
						default:
							log.error(String.format("writeData32 unknown command=0x%X, data[0]=0x%08X, data[1]=0x%08X", command, getData32(0), getData32(4)));
							Arrays.fill(data, 0);
							break;
					}
				}
				break;
		}
	}

	@Override
	public int read16(int address) {
		int value;
		switch (address - baseAddress) {
			case 0x34: value = readData16(); break;
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
			case 0x30: value = command; break;
			case 0x34: value = readData32(); break;
			case 0x38: value = unknown38; break;
			case 0x3C: value = unknown3C; break;
			case 0x40: value = unknown40; break;
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
			case 0x30: setCommand(value); break;
			case 0x34: writeData32(value); break;
			case 0x3C: setUnknown3C(value); break;
			case 0x40: unknown40 = value; break;
			default: super.write32(address, value); break;
		}

		if (log.isTraceEnabled()) {
			log.trace(String.format("0x%08X - write32(0x%08X, 0x%08X) on %s", getPc(), address, value, this));
		}
	}
}
