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
package jpcsp.memory.mmio.umd;

import static jpcsp.HLE.kernel.managers.IntrManager.PSP_UMD_INTR;
import static jpcsp.filesystems.umdiso.ISectorDevice.sectorLength;
import static jpcsp.memory.mmio.MMIOHandlerGpio.GPIO_PORT_BLUETOOTH;
import static jpcsp.memory.mmio.MMIOHandlerGpio.GPIO_PORT_UMD;

import java.io.IOException;
import java.util.Arrays;

import org.apache.log4j.Logger;

import jpcsp.Allegrex.compiler.RuntimeContextLLE;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.VFS.iso.UmdIsoReaderVirtualFile;
import jpcsp.HLE.modules.sceUmdMan;
import jpcsp.memory.mmio.MMIOHandlerBase;
import jpcsp.memory.mmio.MMIOHandlerGpio;
import jpcsp.state.StateInputStream;
import jpcsp.state.StateOutputStream;
import jpcsp.util.Utilities;

public class MMIOHandlerUmd extends MMIOHandlerBase {
	public static Logger log = sceUmdMan.log;
	private static final int STATE_VERSION = 0;
	public static final int BASE_ADDRESS = 0xBDF00000;
	private static MMIOHandlerUmd instance;
	private int command;
	private int reset;
	// Possible interrupt flags: 0x1, 0x2, 0x10, 0x20, 0x40, 0x80, 0x10000, 0x20000, 0x40000, 0x80000
	private int interrupt;
	private int interruptEnabled;
	private int totalTransferLength;
	protected final int transferAddresses[] = new int[10];
	protected final int transferSizes[] = new int[10];
	private static final int QTGP2[] = { 0x12, 0x34, 0x56, 0x78, 0x9A, 0xBC, 0xDE, 0xF0 };
	private static final int QTGP3[] = { 0x0F, 0xED, 0xCB, 0xA9, 0x87, 0x65, 0x43, 0x21, 0x12, 0x34, 0x56, 0x78, 0x9A, 0xBC, 0xDE, 0xF0 };
	private String umdFileName;
	private UmdIsoReaderVirtualFile vFile;
    public static final int regionCodes[] = {
    		0xFFFFFFFF, 0x80000001,
    		0x00000002, 0x80000000,
    		0x0000000F, 0x80000000,
    		0x00000012, 0x80000000,
    		0x0000001F, 0x80000000,
    		0x00000022, 0x80000000,
    		0x0000002F, 0x80000000,
    		0x00000032, 0x80000000,
    		0x0000003F, 0x80000000,
    		0x00000042, 0x80000000,
    		0x0000004F, 0x80000000,
    		0x1000000F, 0x80000000,
    		0x1000001F, 0x80000000,
    		0x1000002F, 0x80000000,
    		0x1000003F, 0x80000000,
    		0x1000004F, 0x80000000,
    		0x2000000F, 0x80000000,
    		0x00000001, 0x00000000 // Last entry must be 1, 0
    };

	public static MMIOHandlerUmd getInstance() {
		if (instance == null) {
			instance = new MMIOHandlerUmd(BASE_ADDRESS);
		}
		return instance;
	}

	private MMIOHandlerUmd(int baseAddress) {
		super(baseAddress);
	}

	@Override
	public void read(StateInputStream stream) throws IOException {
		stream.readVersion(STATE_VERSION);
		command = stream.readInt();
		reset = stream.readInt();
		interrupt = stream.readInt();
		interruptEnabled = stream.readInt();
		totalTransferLength = stream.readInt();
		stream.readInts(transferAddresses);
		stream.readInts(transferSizes);
		umdFileName = stream.readString();
		super.read(stream);

		switchUmd(umdFileName);
	}

	@Override
	public void write(StateOutputStream stream) throws IOException {
		stream.writeVersion(STATE_VERSION);
		stream.writeInt(command);
		stream.writeInt(reset);
		stream.writeInt(interrupt);
		stream.writeInt(interruptEnabled);
		stream.writeInt(totalTransferLength);
		stream.writeInts(transferAddresses);
		stream.writeInts(transferSizes);
		stream.writeString(umdFileName);
		super.write(stream);
	}

	@Override
	public void reset() {
		super.reset();

		command = 0;
		reset = 0;
		interrupt = 0;
		interruptEnabled = 0;
		totalTransferLength = 0;
		Arrays.fill(transferAddresses, 0);
		Arrays.fill(transferSizes, 0);
	}

	private void updateUmd() {
		if (vFile != null) {
			MMIOHandlerGpio.getInstance().setPort(GPIO_PORT_UMD);
		} else {
			MMIOHandlerGpio.getInstance().clearPort(GPIO_PORT_UMD);
		}
	}

	private void closeFile() {
		umdFileName = null;
		if (vFile != null) {
			vFile.ioClose();
			vFile = null;
			updateUmd();
		}
	}

	public boolean hasUmdInserted() {
		return vFile != null;
	}

	public void switchUmd(String fileName) throws IOException {
		closeFile();

		if (fileName != null) {
			log.info(String.format("Using UMD '%s'", fileName));

			umdFileName = fileName;
			vFile = new UmdIsoReaderVirtualFile(fileName);
		}

		updateUmd();
	}

	private void setReset(int reset) {
		this.reset = reset;

		if ((reset & 0x1) != 0) {
			MMIOHandlerGpio.getInstance().setPort(GPIO_PORT_BLUETOOTH);
			updateUmd();
		}
	}

	private boolean isUmdFilePresent(String fileName) {
		if (vFile == null) {
			return false;
		}

		return vFile.hasFile(fileName);
	}

	private boolean isGameUmd() {
		return isUmdFilePresent("PSP_GAME/param.sfo");
	}

	private boolean isVideoUmd() {
		return isUmdFilePresent("UMD_VIDEO/param.sfo");
	}

	private boolean isAudioUmd() {
		return isUmdFilePresent("UMD_AUDIO/param.sfo");
	}

	private void setCommand(int command) {
		this.command = command;

		if (log.isDebugEnabled()) {
			log.debug(String.format("MMIOHandlerUmd.setCommand command 0x%X", command));
		}

		switch (command & 0xFF)  {
			case 0x01:
				interrupt |= 0x1;
				break;
			case 0x02:
				interrupt |= 0x1;
				break;
			case 0x03:
				interrupt |= 0x1;
				break;
			case 0x04:
				for (int i = 0; i < QTGP2.length && i < transferSizes[0]; i++) {
					getMemory().write8(transferAddresses[0] + i, (byte) QTGP2[i]);
				}
				interrupt |= 0x1;
				break;
			case 0x05:
				for (int i = 0; i < QTGP3.length && i < transferSizes[0]; i++) {
					getMemory().write8(transferAddresses[0] + i, (byte) QTGP3[i]);
				}
				interrupt |= 0x1;
				break;
			case 0x08:
				int regionCodeType = 0;
				if (isGameUmd()) {
					regionCodeType = 0x00;
				} else if (isVideoUmd()) {
					regionCodeType = 0x20;
				} else if (isAudioUmd()) {
					regionCodeType = 0x40;
				}

				if (log.isDebugEnabled()) {
					log.debug(String.format("MMIOHandlerUmd.setCommand command=0x%X, transferLength=0x%X, umdType=0x%X", command, totalTransferLength, regionCodeType));
				}
				TPointer result = new TPointer(getMemory(), transferAddresses[0]);
				result.setValue32(0, 0x12345678);
				result.setValue32(0, 0x00000000);
				// Number of region entries
				result.setValue32(12, 2);
				// Each region entry has 24 bytes
				final int regionSize = 24;
				TPointer region = new TPointer(result, 40);

				region.clear(regionSize);
				// Take the first region code found in the IdStorage page 0x102 which is matching the UMD type
				for (int i = 0; i < regionCodes.length; i += 2) {
					if ((regionCodes[i] & 0xF0) == regionCodeType) {
						region.setValue32(0, regionCodes[i]); // Region code
						region.setValue32(4, regionCodes[i + 1]);
						break;
					}
				}
				region.add(regionSize);

				region.clear(regionSize);
				// Last region entry must be 1, 0
				region.setValue32(0, 1);
				region.setValue32(4, 0);

				interrupt |= 0x1;
				MMIOHandlerUmdAta.getInstance().commandCompleted();
				break;
			case 0x09:
				interrupt |= 0x1;
				break;
			case 0x0A: // Called after ATA_CMD_OP_READ_BIG to read the data
				int lba = MMIOHandlerUmdAta.getInstance().getLogicalBlockAddress();
				if (log.isDebugEnabled()) {
					log.debug(String.format("MMIOHandlerUmd.setCommand command=0x%X, transferLength=0x%X, lba=0x%X", command, totalTransferLength, lba));
				}

				if (vFile == null) {
					log.error(String.format("MMIOHandlerUmd no UMD loaded"));
				} else {
					int fileLength = totalTransferLength / (sectorLength + 0x10) * sectorLength;
					long offset = (lba & 0xFFFFFFFFL) * sectorLength;
					long seekResult = vFile.ioLseek(offset);
					if (seekResult < 0) {
						log.error(String.format("MMIOHandlerUmd.setCommand seek error 0x%08X", seekResult));
					} else if (seekResult != offset) {
						log.error(String.format("MMIOHandlerUmd.setCommand incorrect seek: offset=0x%X, seekResult=0x%X", offset, seekResult));
					} else {
						for (int i = 0; fileLength > 0 && i < transferAddresses.length; i++) {
							if (log.isDebugEnabled()) {
								log.debug(String.format("MMIOHandlerUmd.setCommand reading 0x%X bytes at 0x%08X", transferSizes[i], transferAddresses[i]));
							}
							int transferLength = transferSizes[i];
							if (transferLength > 0) {
								// The PSP hardware is always rounding the address down to a 32-bit boundary
								TPointer addr = new TPointer(getMemory(), transferAddresses[i] & ~0x3);

								int readResult = vFile.ioRead(addr, transferLength);
								if (readResult < 0) {
									log.error(String.format("MMIOHandlerUmd.setCommand read error 0x%08X", readResult));
									break;
								} else {
									if (readResult != transferLength) {
										log.error(String.format("MMIOHandlerUmd.setCommand uncomplete read: transferLength=0x%X, readLength=0x%X", transferLength, readResult));
										break;
									}
		
									if (log.isTraceEnabled()) {
										log.trace(String.format("MMIOHandlerUmd.setCommand read 0x%X bytes: %s", readResult, Utilities.getMemoryDump(addr, readResult)));
									}
								}
								fileLength -= transferLength;
							}
						}
					}
				}

				interrupt |= 0x1;
				MMIOHandlerUmdAta.getInstance().commandCompleted();
				break;
			case 0x0B:
				break;
			default:
				log.error(String.format("MMIOHandlerUmd.setCommand unknown command 0x%X", command));
				break;
		}

		checkInterrupt();
	}

	private void checkInterrupt() {
		if ((interrupt & interruptEnabled) != 0) {
			RuntimeContextLLE.triggerInterrupt(getProcessor(), PSP_UMD_INTR);
		} else {
			RuntimeContextLLE.clearInterrupt(getProcessor(), PSP_UMD_INTR);
		}
	}

	private void clearInterrupt(int interrupt) {
		this.interrupt &= ~interrupt;

		checkInterrupt();
	}

	private void enableInterrupt(int interruptEnabled) {
		this.interruptEnabled |= interruptEnabled;

		checkInterrupt();
	}

	private void disableInterrupt(int interruptEnabled) {
		this.interruptEnabled &= ~interruptEnabled;

		checkInterrupt();
	}

	@Override
	public int read32(int address) {
		int value;
		switch (address - baseAddress) {
			case 0x08: value = reset; break;
			case 0x10: value = 0; break; // Unknown value
			case 0x14: value = 0; break; // Unknown value
			case 0x18: value = 0; break; // flags 0x10 and 0x100 are being tested
			case 0x1C: value = 0; break; // Tests: (value & 0x1) != 0 (meaning timeout occured?), value < 0x11
			case 0x20: value = interrupt; break;
			case 0x24: value = 0; break; // Unknown value
			case 0x28: value = interruptEnabled; break; // Unknown value
			case 0x2C: value = 0; break; // Unknown value
			case 0x30: value = 0; break; // Unknown value, error code?
			case 0x38: value = 0; break; // Unknown value
			case 0x90: value = totalTransferLength; break;
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
			case 0x08: setReset(value); break;
			case 0x10: setCommand(value); break;
			case 0x24: clearInterrupt(value); break; // Not sure about the meaning
			case 0x28: enableInterrupt(value); break;
			case 0x2C: disableInterrupt(value); break; // Not sure about the meaning
			case 0x30: if (value != 0x4) { super.write32(address, value); } break; // Unknown value
			case 0x38: if (value != 0x4) { super.write32(address, value); } break; // Unknown value
			case 0x40: transferAddresses[0] = value; break;
			case 0x44: transferSizes[0] = value; break;
			case 0x48: transferAddresses[1] = value; break;
			case 0x4C: transferSizes[1] = value; break;
			case 0x50: transferAddresses[2] = value; break;
			case 0x54: transferSizes[2] = value; break;
			case 0x58: transferAddresses[3] = value; break;
			case 0x5C: transferSizes[3] = value; break;
			case 0x60: transferAddresses[4] = value; break;
			case 0x64: transferSizes[4] = value; break;
			case 0x68: transferAddresses[5] = value; break;
			case 0x6C: transferSizes[5] = value; break;
			case 0x70: transferAddresses[6] = value; break;
			case 0x74: transferSizes[6] = value; break;
			case 0x78: transferAddresses[7] = value; break;
			case 0x7C: transferSizes[7] = value; break;
			case 0x80: transferAddresses[8] = value; break;
			case 0x84: transferSizes[8] = value; break;
			case 0x88: transferAddresses[9] = value; break;
			case 0x8C: transferSizes[9] = value; break;
			case 0x90: totalTransferLength = value; break;
			case 0x94: if (value != 1) { super.write32(address, value); } break; // Unknown value, possible values: 0, 1
			default: super.write32(address, value); break;
		}

		if (log.isTraceEnabled()) {
			log.trace(String.format("0x%08X - write32(0x%08X, 0x%08X) on %s", getPc(), address, value, this));
		}
	}
}
