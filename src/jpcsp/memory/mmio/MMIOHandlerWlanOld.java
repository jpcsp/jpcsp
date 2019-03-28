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

import static jpcsp.HLE.kernel.managers.IntrManager.PSP_WLAN_INTR;
import static jpcsp.util.Utilities.endianSwap16;
import static jpcsp.util.Utilities.endianSwap32;

import java.io.IOException;
import java.util.Arrays;

import org.apache.log4j.Logger;

import jpcsp.Allegrex.compiler.RuntimeContextLLE;
import jpcsp.HLE.modules.sceWlan;
import jpcsp.state.StateInputStream;
import jpcsp.state.StateOutputStream;
import jpcsp.util.Utilities;

public class MMIOHandlerWlanOld extends MMIOHandlerBase {
	public static Logger log = sceWlan.log;
	private static final int STATE_VERSION = 0;
	private static final int COMMAND_BIT = 0x200;
	private static final int INTERRUPT_BIT = 0x800;
	private int unknown00;
	private int command;
	private int preparedCommand;
	private Object dmaLock = new Object();
	private final int data[] = new int[4096];
	private int unknown38;
	private int unknown3C;
	private int unknown40;
	private int index;
	private int totalLength;
	private int currentLength;
	private int registerResult;
	private int registerUnknown25;
	private int registerUnknown27;

	public MMIOHandlerWlanOld(int baseAddress) {
		super(baseAddress);

		unknown00 = 0x0;
		unknown38 = 0x4000 | 0x2000 | 0x1000 | 0x0002;
		registerResult = 0x00;
		registerUnknown25 = 0x00;
		registerUnknown27 = 0x00;
	}

	@Override
	public void read(StateInputStream stream) throws IOException {
		stream.readVersion(STATE_VERSION);
		unknown00 = stream.readInt();
		command = stream.readInt();
		preparedCommand = stream.readInt();
		stream.readInts(data);
		unknown38 = stream.readInt();
		unknown3C = stream.readInt();
		unknown40 = stream.readInt();
		index = stream.readInt();
		totalLength = stream.readInt();
		currentLength = stream.readInt();
		registerResult = stream.readInt();
		registerUnknown25 = stream.readInt();
		registerUnknown27 = stream.readInt();
		super.read(stream);
	}

	@Override
	public void write(StateOutputStream stream) throws IOException {
		stream.writeVersion(STATE_VERSION);
		stream.writeInt(unknown00);
		stream.writeInt(command);
		stream.writeInt(preparedCommand);
		stream.writeInts(data);
		stream.writeInt(unknown38);
		stream.writeInt(unknown3C);
		stream.writeInt(unknown40);
		stream.writeInt(index);
		stream.writeInt(totalLength);
		stream.writeInt(currentLength);
		stream.writeInt(registerResult);
		stream.writeInt(registerUnknown25);
		stream.writeInt(registerUnknown27);
		super.write(stream);
	}

	private void setUnknown3C(int value) {
		if ((value & INTERRUPT_BIT) != 0) {
			RuntimeContextLLE.clearInterrupt(getProcessor(), PSP_WLAN_INTR);
			value &= ~INTERRUPT_BIT;
		}

		if ((value & COMMAND_BIT) != 0) {
			value &= ~COMMAND_BIT;
		}

		unknown3C = value;
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
			unknown38 &= 0xFFFFFFE0;

			if (log.isDebugEnabled()) {
				log.debug(String.format("setCommand commandCode=0x%X, commandLength=0x%X", getCommandCode(), getCommandLength()));
			}

			switch (command) {
				case 0x7001:
					clearData(8);
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
				case 0xB002:
					break;
				case 0xA00C:
				case 0xA010:
				case 0xA030:
				case 0xA080:
				case 0xA13C:
				case 0xA200:
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

	private int getData8(int offset) {
		return data[offset];
	}

	private int getData16(int offset) {
		return getData8(offset) | (getData8(offset + 1) << 8);
	}

	private int getData32(int offset) {
		return getData8(offset) | (getData8(offset + 1) << 8) | (getData8(offset + 2) << 16) | (getData8(offset + 3) << 24);
	}

	static private int swap32(int value) {
		return (value >>> 16) | (value << 16);
	}

	private void swapData32(int length) {
		for (int i = 0; i < length; i += 4) {
			setData32(i, swap32(getData32(i)));
		}
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
		if (log.isTraceEnabled()) {
			log.trace(String.format("writeData32 command=0x%X, data=0x%08X, index=0x%X", command, value, index));
		}

		writeData8(value);
		writeData8(value >> 8);
		writeData8(value >> 16);
		writeData8(value >>> 24);

		switch (command) {
			case 0x8004:
				if (index >= 8) {
					preparedCommand = getData32(0);
					if (log.isDebugEnabled()) {
						log.debug(String.format("writeData32 command=0x%X, preparedCommand=0x%X", command, preparedCommand));
					}

					switch (preparedCommand) {
						case 0x122:
							clearData(8);
							setData8(0, 0x24); // Returning 1 unknown byte, only possible value: 0x24
							break;
						case 0x123:
						case 0x124:
							clearData(8);
							setData8(0, 0x00); // Returning 1 unknown byte
							break;
						case 0x125:
							clearData(8);
							setData8(0, registerUnknown25); // Returning 1 unknown byte
							break;
						case 0x127:
							clearData(8);
							setData8(0, registerUnknown27); // Returning 1 unknown byte
							break;
						case 0x154:
							clearData(8);
							setData8(0, registerResult); // Returning 1 unknown byte, 0x04 and 0x80 are required
							break;
						case 0x24A:
							clearData(8);
							setData16(0, endianSwap16(0x200)); // Returning unknown 16 bit value (it is a size for next request, must be between 4 and 0xC00)
setData16(0, endianSwap16(0));
							break;
						case 0x24C:
							clearData(8);
							setData16(0, endianSwap16(0x1000)); // Returning unknown 16 bit value (it is a size for next request)
							break;
						case 0x44A:
							clearData(8);
							setData32(0, endianSwap32(0x55475546)); // Firmware magic
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
						case 0x1560000:
						case 0x15A0000:
						case 0x15E0000:
						case 0x15C0000:
						case 0x2440000:
							clearData(8);
							break;
						default:
							log.error(String.format("writeData32 unknown command=0x%X, data[0]=0x%08X, data[1]=0x%08X", command, getData32(0), getData32(4)));
							Arrays.fill(data, 0);
							break;
					}

					if (log.isDebugEnabled() && ((preparedCommand & 0x0000FFFF) != 0)) {
						int commandCode = preparedCommand & 0xFF;
						int commandLength = (preparedCommand >> 8) & 0xFF;
						switch (commandLength) {
							case 1:
								log.debug(String.format("writeData32 commandCode=0x%X, commandLength=0x%X returning 0x%02X", commandCode, commandLength, getData8(0)));
								break;
							case 2:
								log.debug(String.format("writeData32 commandCode=0x%X, commandLength=0x%X returning 0x%04X", commandCode, commandLength, endianSwap16(getData16(0))));
								break;
							case 4:
								log.debug(String.format("writeData32 commandCode=0x%X, commandLength=0x%X returning 0x%08X", commandCode, commandLength, endianSwap32(getData32(0))));
								break;
							default:
								byte[] bytes = new byte[commandLength];
								for (int i = 0; i < commandLength; i++) {
									bytes[i] = (byte) getData8(i);
								}
								log.debug(String.format("writeData32 commandCode=0x%X, commandLength=0x%X returning %s", commandCode, commandLength, Utilities.getMemoryDump(bytes)));
								break;
						}
					}
				}
				break;
			case 0x9007:
				if (index >= 8) {
					totalLength = endianSwap16(getData16(1));
					int unknown = endianSwap32(getData32(3));
					if (log.isDebugEnabled()) {
						log.debug(String.format("writeData32 command=0x%X, totalLength=0x%X, unknown=0x%X", command, totalLength, unknown));
					}

					clearData(totalLength);
					switch (unknown) {
						case 0:
							// Possible values:
							// 0x0011 0x0001 0x0001
							// 0x0011 0x0001 0x1B18
							// 0x0011 0x0002 0x1B11
							// 0x0011 0x0002 0x0B11
							setData16(0, 0x0011);
							setData16(2, 0x0001);
							setData16(4, 0x1B18);

							for (int i = 1; i < 8; i++) {
								int offset = i * 8;
								setData16(offset + 0, i); // Has to be a value in range [1..8]
								setData16(offset + 2, 0x1234); // Unknown value
								setData16(offset + 4, 0x0040); // Unknown size
							}
							break;
						case 0x1234:
							break;
						case 0x100:
							break;
						default:
							log.error(String.format("writeData32 unknown command=0x%X, totalLength=0x%X, unknown=0x%X", command, totalLength, unknown));
							break;
					}
					swapData32(totalLength);

					currentLength = 0;
					unknown38 |= 0x0002;
					index = 0;
				}
				break;
			case 0xB001:
				if (index >= 8) {
					unknown38 |= 0x0002;

					switch (preparedCommand) {
						case 0x1100000:
						case 0x1260000:
						case 0x15A0000:
						case 0x15C0000:
							if (log.isDebugEnabled()) {
								log.debug(String.format("writeData32 written data for command=0x%X, preparedCommand=0x%X, data=0x%02X", command, preparedCommand, getData8(0)));
							}
							RuntimeContextLLE.triggerInterrupt(getProcessor(), PSP_WLAN_INTR);
							break;
						case 0x1250000:
							if (log.isDebugEnabled()) {
								log.debug(String.format("writeData32 written data for command=0x%X, preparedCommand=0x%X, data=0x%02X", command, preparedCommand, getData8(0)));
							}
							registerUnknown25 = getData8(0);
							RuntimeContextLLE.triggerInterrupt(getProcessor(), PSP_WLAN_INTR);
							break;
						case 0x1270000:
							if (log.isDebugEnabled()) {
								log.debug(String.format("writeData32 written data for command=0x%X, preparedCommand=0x%X, data=0x%02X", command, preparedCommand, getData8(0)));
							}
							registerUnknown27 = getData8(0);
							RuntimeContextLLE.triggerInterrupt(getProcessor(), PSP_WLAN_INTR);
							break;
						case 0x1540000:
							if (log.isDebugEnabled()) {
								log.debug(String.format("writeData32 written data for command=0x%X, preparedCommand=0x%X, data=0x%02X", command, preparedCommand, getData8(0)));
							}
							registerResult &= getData8(0);
							RuntimeContextLLE.triggerInterrupt(getProcessor(), PSP_WLAN_INTR);
							break;
						case 0x15E0000:
							if (log.isDebugEnabled()) {
								log.debug(String.format("writeData32 written data for command=0x%X, preparedCommand=0x%X, data=0x%02X", command, preparedCommand, getData8(0)));
							}
							registerResult |= 0x80;
							RuntimeContextLLE.triggerInterrupt(getProcessor(), PSP_WLAN_INTR);
							break;
						case 0x1560000:
							if (log.isDebugEnabled()) {
								log.debug(String.format("writeData32 written data for command=0x%X, preparedCommand=0x%X, data=0x%02X", command, preparedCommand, getData8(0)));
							}
							registerResult |= 0x04;
							RuntimeContextLLE.triggerInterrupt(getProcessor(), PSP_WLAN_INTR);
							break;
						default:
							log.error(String.format("writeData32 unknown command=0x%X, preparedCommand=0x%X, data[0]=0x%08X, data[1]=0x%08X", command, preparedCommand, getData32(0), getData32(4)));
							Arrays.fill(data, 0);
							break;
					}
				}
				break;
			case 0xB002:
				if (index >= 8) {
					unknown38 |= 0x0002;

					switch (preparedCommand) {
						case 0x2440000:
							if (log.isDebugEnabled()) {
								log.debug(String.format("writeData32 written data for command=0x%X, preparedCommand=0x%X, data=0x%04X", command, preparedCommand, endianSwap16(getData16(0))));
							}
							registerResult |= 0x04 | 0x10;
							RuntimeContextLLE.triggerInterrupt(getProcessor(), PSP_WLAN_INTR);
							break;
						default:
							log.error(String.format("writeData32 unknown command=0x%X, preparedCommand=0x%X, data[0]=0x%08X, data[1]=0x%08X", command, preparedCommand, getData32(0), getData32(4)));
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
			case 0x00: value = unknown00; break;
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
