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

import static jpcsp.HLE.kernel.managers.IntrManager.PSP_UMD_INTR;
import static jpcsp.filesystems.umdiso.ISectorDevice.sectorLength;
import static jpcsp.memory.mmio.MMIOHandlerGpio.GPIO_PORT_BLUETOOTH;
import static jpcsp.memory.mmio.MMIOHandlerGpio.GPIO_PORT_UMD;

import org.apache.log4j.Logger;

import jpcsp.Allegrex.compiler.RuntimeContextLLE;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.VFS.IVirtualFile;
import jpcsp.HLE.VFS.IVirtualFileSystem;
import jpcsp.HLE.VFS.local.LocalVirtualFileSystem;
import jpcsp.HLE.modules.IoFileMgrForUser;
import jpcsp.HLE.modules.sceNand;
import jpcsp.HLE.modules.sceUmdMan;
import jpcsp.util.Utilities;

public class MMIOHandlerUmd extends MMIOHandlerBase {
	public static Logger log = sceUmdMan.log;
	protected int command;
	private int reset;
	// Possible interrupt flags: 0x1, 0x2, 0x10, 0x20, 0x40, 0x80, 0x10000, 0x20000, 0x40000, 0x80000
	private int interrupt;
	private int interruptEnabled;
	private int totalTransferLength;
	protected final int transferAddresses[] = new int[10];
	protected final int transferSizes[] = new int[10];
	private static final int QTGP2[] = { 0x12, 0x34, 0x56, 0x78, 0x9A, 0xBC, 0xDE, 0xF0 };
	private static final int QTGP3[] = { 0x0F, 0xED, 0xCB, 0xA9, 0x87, 0x65, 0x43, 0x21, 0x12, 0x34, 0x56, 0x78, 0x9A, 0xBC, 0xDE, 0xF0 };
	IVirtualFile vFile;

	public MMIOHandlerUmd(int baseAddress) {
		super(baseAddress);

		IVirtualFileSystem vfs = new LocalVirtualFileSystem("umdimages/", false);
		vFile = vfs.ioOpen("test.iso", IoFileMgrForUser.PSP_O_RDONLY, 0);
	}

	private void setReset(int reset) {
		this.reset = reset;

		if ((reset & 0x1) != 0) {
			MMIOHandlerGpio.getInstance().setPort(GPIO_PORT_BLUETOOTH);
			MMIOHandlerGpio.getInstance().setPort(GPIO_PORT_UMD);
		}
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
				if (log.isDebugEnabled()) {
					log.debug(String.format("MMIOHandlerUmd.setCommand command=0x%X, transferLength=0x%X", command, totalTransferLength));
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
				// Take any region code found in the IdStorage page 0x102
				region.setValue32(0, sceNand.regionCodes[2]); // Region code
				region.setValue32(4, sceNand.regionCodes[3]);
				region.add(regionSize);

				region.clear(regionSize);
				// Last region entry must be 1, 0
				region.setValue32(0, 1);
				region.setValue32(4, 0);

				interrupt |= 0x1;
				MMIOHandlerAta.getInstance().packetCommandCompleted();
				break;
			case 0x09:
				interrupt |= 0x1;
				break;
			case 0x0A: // Called after ATA_CMD_OP_READ_BIG to read the data
				if (log.isDebugEnabled()) {
					log.debug(String.format("MMIOHandlerUmd.setCommand command=0x%X, transferLength=0x%X", command, totalTransferLength));
				}

				int fileLength = totalTransferLength / (sectorLength + 0x10) * sectorLength;
				long offset = (MMIOHandlerAta.getInstance().getLogicalBlockAddress() & 0xFFFFFFFFL) * sectorLength;
				long seekResult = vFile.ioLseek(offset);
				if (seekResult < 0) {
					log.error(String.format("MMIOHandlerUmd.setCommand seek error 0x%08X", seekResult));
				} else if (seekResult != offset) {
					log.error(String.format("MMIOHandlerUmd.setCommand incorrect seek: offset=0x%X, seekResult=0x%X", offset, seekResult));
				} else {
					for (int i = 0; fileLength > 0 && i < transferAddresses.length; i++) {
						int transferLength = transferSizes[i];
						if (transferLength > 0) {
							TPointer addr = new TPointer(getMemory(), transferAddresses[i]);
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
									log.trace(String.format("MMIOHandlerUmd.setCommand read 0x%X bytes: %s", readResult, Utilities.getMemoryDump(addr.getAddress(), readResult)));
								}
							}
							fileLength -= transferLength;
						}
					}
				}

				interrupt |= 0x1;
				MMIOHandlerAta.getInstance().packetCommandCompleted();
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
