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

import static jpcsp.HLE.Modules.sceNandModule;
import static jpcsp.HLE.Modules.sceSysregModule;
import static jpcsp.HLE.kernel.managers.IntrManager.PSP_NAND_INTR;
import static jpcsp.HLE.modules.sceNand.pagesPerBlock;
import static jpcsp.util.Utilities.endianSwap16;
import static jpcsp.util.Utilities.lineSeparator;

import org.apache.log4j.Logger;

import jpcsp.Allegrex.compiler.RuntimeContextLLE;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.modules.sceNand;
import jpcsp.memory.IntArrayMemory;
import jpcsp.util.Utilities;

/**
 * PSP NAND hardware interface
 * See http://www.alldatasheet.com/datasheet-pdf/pdf/37107/SAMSUNG/K9F5608U0C.html
 *
 * @author gid15
 *
 */
public class MMIOHandlerNand extends MMIOHandlerBase {
	public static Logger log = sceNand.log;
	public static final int BASE_ADDRESS = 0xBD101000;
	public static final int PSP_NAND_CONTROL_AUTO_USER_ECC = 0x10000;
	public static final int PSP_NAND_STATUS_READY = 0x01;
	public static final int PSP_NAND_STATUS_WRITE_ENABLED = 0x80;
	public static final int PSP_NAND_INTR_WRITE_COMPLETED = 0x002;
	public static final int PSP_NAND_INTR_READ_COMPLETED = 0x001;
	public static final int PSP_NAND_COMMAND_READ_EXTRA = 0x50;
	public static final int PSP_NAND_COMMAND_ERASE_BLOCK = 0x60;
	public static final int PSP_NAND_COMMAND_GET_READ_STATUS = 0x70;
	public static final int PSP_NAND_COMMAND_READ_ID = 0x90;
	public static final int PSP_NAND_COMMAND_ERASE_BLOCK_CONFIRM = 0xD0;
	public static final int PSP_NAND_COMMAND_RESET = 0xFF;
	private static final int DMA_CONTROL_START = 0x0001;
	private static final int DMA_CONTROL_WRITE = 0x0002;
	private static MMIOHandlerNand instance;
	private final IntArrayMemory pageDataMemory;
	private final IntArrayMemory pageEccMemory;
	private final IntArrayMemory dataMemory;
	private final IntArrayMemory scrambleBufferMemory;
	private int control;
	private int status;
	private int command;
	private int pageAddress;
	private final int data[] = new int[4];
	private int dataIndex;
	private int dmaAddress;
	private int dmaControl;
	private int dmaStatus;
	private int dmaInterrupt;
	private int unknown200;
	private boolean needPageAddress;
	private final int[] scrambleBuffer = new int[sceNand.pageSize >> 2];

	public static MMIOHandlerNand getInstance() {
		if (instance == null) {
			instance = new MMIOHandlerNand(BASE_ADDRESS);
		}
		return instance;
	}

	private MMIOHandlerNand(int baseAddress) {
		super(baseAddress);

		pageDataMemory = new IntArrayMemory(MMIOHandlerNandPage.getInstance().getData());
		pageEccMemory = new IntArrayMemory(MMIOHandlerNandPage.getInstance().getEcc());
		dataMemory = new IntArrayMemory(data);
		scrambleBufferMemory = new IntArrayMemory(scrambleBuffer);

		status = PSP_NAND_STATUS_READY;
	}

	private void startCommand(int command) {
		this.command = command;

		dataIndex = 0;
		needPageAddress = false;
		switch (command) {
			case PSP_NAND_COMMAND_RESET:
				break;
			case PSP_NAND_COMMAND_GET_READ_STATUS:
				data[0] = status & PSP_NAND_STATUS_WRITE_ENABLED;
				break;
			case PSP_NAND_COMMAND_READ_ID:
				// The pageAddress is written after the command
				needPageAddress = true;
				break;
			case PSP_NAND_COMMAND_READ_EXTRA:
				// The pageAddress is written after the command
				needPageAddress = true;
				break;
			case PSP_NAND_COMMAND_ERASE_BLOCK:
				// The pageAddress is written after the command
				needPageAddress = true;
				break;
			case PSP_NAND_COMMAND_ERASE_BLOCK_CONFIRM:
				// We don't need to erase blocks...
				if (log.isDebugEnabled()) {
					log.debug(String.format("PSP_NAND_COMMAND_ERASE_BLOCK ppn=0x%X", pageAddress >> 10));
				}
//				triggerInterrupt(PSP_NAND_INTR_WRITE_COMPLETED); // TODO Unknown value
				break;
			default:
				log.error(String.format("MMIOHandlerNand.startCommand unknown command 0x%X", command));
				break;
		}
	}

	private void startCommandWithPageAddress() {
		needPageAddress = false;

		switch (command) {
			case PSP_NAND_COMMAND_READ_ID:
				if (pageAddress == 0) {
					// This ID will configure:
					// - sceNandGetPageSize returning 0x200
					// - sceNandGetPagesPerBlock returning 0x20
					// - sceNandGetTotalBlocks returning 0x800
					data[0] = 0xEC; // Manufacturer's code (SAMSUNG)
					data[1] = 0x75; // Device code (K9F5608U0C)
				}
				break;
			case PSP_NAND_COMMAND_READ_EXTRA:
				TPointer spare = dataMemory.getPointer();
				sceNandModule.hleNandReadPages(pageAddress >> 10, TPointer.NULL, spare, 1, true, true, true);
				break;
		}
	}

	private void endCommand(int value) {
		if (value == 1) {
			dataIndex = 0;
		}
	}

	private void writePageAddress(int pageAddress) {
		this.pageAddress = pageAddress;

		if (needPageAddress) {
			startCommandWithPageAddress();
		}
	}

	private int readData() {
		return data[dataIndex++];
	}

	private int getScrambleBootSector(long fuseId, int partitionNumber) {
		int scramble = ((int) fuseId) ^ Integer.rotateLeft((int) (fuseId >> 32), partitionNumber * 2);
		if (scramble == 0) {
			scramble = Integer.rotateLeft(0xC4536DE6, partitionNumber);
		}

		return scramble;
	}

	private int getScrambleDataSector(long fuseId, int partitionNumber) {
		if (partitionNumber == 3) {
			return 0x3C22812A;
		}

		int scramble = ((int) fuseId) ^ Integer.rotateRight((int) (fuseId >> 32), partitionNumber * 3);
		scramble ^= 0x556D81FE;
		if (scramble == 0) {
			scramble = Integer.rotateRight(0x556D81FE, partitionNumber);
		}

		return scramble;
	}

	private int getScramble(int ppn) {
		long fuseId = sceSysregModule.sceSysregGetFuseId();
		int lbn = sceNandModule.getLbnFromPpn(ppn);
		int sector = ppn % pagesPerBlock;

		if (lbn == 0x003 && sector == 0) {
			return getScrambleBootSector(fuseId, 0); // flash0 boot sector
		} else if (lbn >= 0x004 && lbn < 0x601) {
			return getScrambleDataSector(fuseId, 0); // flash0
		} else if (lbn >= 0x602 && lbn < 0x702) {
			return 0; // flash1 is not scrambled
		} else if (lbn == 0x703 && sector == 0) {
			return getScrambleBootSector(fuseId, 2); // flash2 boot sector
		} else if (lbn >= 0x704 && lbn < 0x742) {
			return getScrambleDataSector(fuseId, 2); // flash2
		} else if (lbn == 0x742 && sector == 0) {
			return getScrambleBootSector(fuseId, 3); // flash3 boot sector
		} else if (lbn >= 0x743) {
			return getScrambleDataSector(fuseId, 3); // flash3
		}

		return 0;
	}

	private void startDma(int dmaControl) {
		this.dmaControl = dmaControl;

		if ((dmaControl & DMA_CONTROL_START) != 0) {
			int ppn = dmaAddress >> 10;
			int scramble = getScramble(ppn);

			// Read or write operation?
			if ((dmaControl & DMA_CONTROL_WRITE) != 0) {
				int lbn = endianSwap16(pageEccMemory.read16(6) & 0xFFFF);
				if (lbn == 0xFFFF) {
					if (log.isDebugEnabled()) {
						log.debug(String.format("writing to ppn=0x%X with lbn=0x%X ignored", ppn, lbn));
					}
				} else {
					TPointer user = pageDataMemory.getPointer();
					TPointer spare = pageEccMemory.getPointer();
					sceNandModule.hleNandWritePages(ppn, user, spare, 1, true, true, true);
				}

				if (log.isDebugEnabled()) {
					byte[] userBytes = new byte[sceNand.pageSize];
					for (int i = 0; i < userBytes.length; i++) {
						userBytes[i] = (byte) pageDataMemory.read8(i);
					}
					byte[] spareBytes = new byte[16];
					for (int i = 0; i < spareBytes.length; i++) {
						spareBytes[i] = (byte) pageEccMemory.read8(i);
					}
					log.debug(String.format("hleNandWritePages ppn=0x%X, lbn=0x%X, scramble=0x%X: %s%sSpare: %s", ppn, lbn, scramble, Utilities.getMemoryDump(userBytes), lineSeparator, Utilities.getMemoryDump(spareBytes)));
				}

				triggerInterrupt(PSP_NAND_INTR_WRITE_COMPLETED);
			} else {
				TPointer user = scramble != 0 ? scrambleBufferMemory.getPointer() : pageDataMemory.getPointer();
				TPointer spare = pageEccMemory.getPointer();
				sceNandModule.hleNandReadPages(ppn, user, spare, 1, true, true, true);

				if (log.isDebugEnabled()) {
					byte[] bytes = new byte[sceNand.pageSize];
					user = scramble != 0 ? scrambleBufferMemory.getPointer() : pageDataMemory.getPointer();
					for (int i = 0; i < bytes.length; i++) {
						bytes[i] = user.getValue8(i);
					}
					log.debug(String.format("hleNandReadPages ppn=0x%X, scramble=0x%X: %s", ppn, scramble, Utilities.getMemoryDump(bytes)));
				}

				if (scramble != 0) {
					sceNand.scramblePage(scramble, ppn, scrambleBuffer, MMIOHandlerNandPage.getInstance().getData());
				}

				triggerInterrupt(PSP_NAND_INTR_READ_COMPLETED);
			}
		}
	}

	private void writeDmaInterrupt(int dmaInterrupt) {
		// No idea how writing to the dmaInterrupt field is working.
		// I was not able to change any information in this field on a real PSP.
		// The following seems to make the PSP code happy...
		this.dmaInterrupt &= ~0x003;
		checkInterrupt();
	}

	private void triggerInterrupt(int dmaInterrupt) {
		// No idea how triggering an interrupt is working.
		// I was not able to reproduce it on a real PSP.
		// The following seems to make the PSP code happy...
		this.dmaInterrupt |= 0x300 | dmaInterrupt;
		checkInterrupt();
	}

	private void checkInterrupt() {
		if ((dmaInterrupt & 0x003) != 0) {
			RuntimeContextLLE.triggerInterrupt(getProcessor(), PSP_NAND_INTR);
		} else {
			RuntimeContextLLE.clearInterrupt(getProcessor(), PSP_NAND_INTR);
		}
	}

	@Override
	public int read32(int address) {
		int value;
		switch (address - baseAddress) {
			case 0x000: value = control; break;
			case 0x004: value = status; break;
			case 0x008: value = command; break;
			case 0x00C: value = pageAddress; break;
			case 0x020: value = dmaAddress; break;
			case 0x024: value = dmaControl; break;
			case 0x028: value = dmaStatus; break;
			case 0x038: value = dmaInterrupt; break;
			case 0x200: value = unknown200; break;
			case 0x300: value = readData(); break;
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
			case 0x000: control = value; break;
			case 0x004: status = value; break;
			case 0x008: startCommand(value); break;
			case 0x00C: writePageAddress(value); break;
			case 0x014: endCommand(value); break;
			case 0x020: dmaAddress = value; break;
			case 0x024: startDma(value); break;
			case 0x028: dmaStatus = value; break;
			case 0x038: writeDmaInterrupt(value); break;
			case 0x200: unknown200 = value; break;
			default: super.write32(address, value); break;
		}

		if (log.isTraceEnabled()) {
			log.trace(String.format("0x%08X - write32(0x%08X, 0x%08X) on %s", getPc(), address, value, this));
		}
	}
}
