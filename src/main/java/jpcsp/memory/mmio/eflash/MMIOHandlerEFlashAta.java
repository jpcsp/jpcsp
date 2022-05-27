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
package jpcsp.memory.mmio.eflash;

import static jpcsp.util.Utilities.GB;
import static jpcsp.util.Utilities.endianSwap32;
import static jpcsp.util.Utilities.writeUnaligned32;

import java.io.IOException;

import jpcsp.HLE.VFS.IVirtualFile;
import jpcsp.HLE.VFS.NullVirtualFile;
import jpcsp.HLE.VFS.WriteCacheVirtualFile;
import jpcsp.HLE.modules.sceEFlash;
import jpcsp.memory.mmio.MMIOHandlerBaseAta;
import jpcsp.state.IState;
import jpcsp.state.StateInputStream;
import jpcsp.state.StateOutputStream;
import jpcsp.util.Utilities;

public class MMIOHandlerEFlashAta extends MMIOHandlerBaseAta {
	public static final int BASE_ADDRESS = 0xBDA00000;
	private static MMIOHandlerEFlashAta instance;
	private static final int STATE_VERSION = 0;
	private static final long eflashSize = 16L * GB; // 16 GB
	private IVirtualFile eflashFile;
	private final byte[] buffer = new byte[SECTOR_SIZE];

	public static MMIOHandlerEFlashAta getInstance() {
		if (instance == null) {
			instance = new MMIOHandlerEFlashAta(BASE_ADDRESS);
		}

		return instance;
	}

	private MMIOHandlerEFlashAta(int baseAddress) {
		super(baseAddress);

		log = sceEFlash.log;

		eflashFile = new WriteCacheVirtualFile(log, new NullVirtualFile(eflashSize), true);

		// See
		//   https://github.com/uyjulian/pfsshell/tree/master/apa/src
		// for more information about the APA partition system
		eflashFile.ioLseek(0x2000 * SECTOR_SIZE);
		writeUnaligned32(buffer, 0, 0x4150414C); // "LAPA"
		writeUnaligned32(buffer, 4, 0); // number of sectors in the APA journal
		eflashFile.ioWrite(buffer, 0, SECTOR_SIZE);
	}

	@Override
	public void read(StateInputStream stream) throws IOException {
		stream.readVersion(STATE_VERSION);
		if (eflashFile instanceof IState) {
			((IState) eflashFile).read(stream);
		}
		super.read(stream);
	}

	@Override
	public void write(StateOutputStream stream) throws IOException {
		stream.writeVersion(STATE_VERSION);
		if (eflashFile instanceof IState) {
			((IState) eflashFile).write(stream);
		}
		super.write(stream);
	}

	@Override
	protected int getInterruptNumber() {
		return -1;
	}

	@Override
	protected boolean supportsCmdPacket() {
		return false;
	}

	@Override
	protected void executePacketCommand(int[] data) {
		int operationCode = data[0];
		log.error(String.format("MMIOHandlerEFlashAta.executePacketCommand unimplemented operation code 0x%02X(%s)", operationCode, getOperationCodeName(operationCode)));
	}

	@Override
	protected void setInterruptReason(boolean CoD, boolean io) {
		super.setInterruptReason(CoD, io);
		if (!CoD && io) {
			int flags = 0x00010001;
			if (getCommand() == ATA_CMD_READ) {
				flags |= 0x00020002;
			}
			MMIOHandlerEFlash.getInstance().setInterruptFlag(flags);
		}
	}

	private long getFileOffset(int lba) {
		return (lba & 0xFFFFFFFFL) * SECTOR_SIZE;
	}

	@Override
	protected void executeCommand(int command, int[] data, int dataLength, int totalDataLength, boolean firstCommand) {
		int delayUs;

		switch (command) {
			case ATA_CMD_ID_ATA:
				dataLength = 512;
				prepareDataInit(dataLength);
				for (int i = 0; i < 0x78; i++) {
					prepareData8(0);
				}
				prepareData32(endianSwap32(0x12345678)); // Unknown 32-bit value
				for (int i = 0x7C; i < dataLength; i++) {
					prepareData8(0);
				}
				// Duration 2ms
				prepareDataEndWithDelay(dataLength, dataLength, 2000);
				break;
			case ATA_CMD_SET_FEATURES:
				int subcommandCode = getFeatures();
				delayUs = 0;
				dataLength = 0;
				switch (subcommandCode) {
					case SETFEATURES_XFER: // Set transfer mode based on value in Sector Count register
						int transferMode = getSectorCount();
						switch (transferMode) {
							case XFER_UDMA_2:
								break;
							default:
								log.error(String.format("MMIOHandlerEFlashAta.executeCommand ATA_CMD_SET_FEATURES, SETFEATURES_XFER unimplemented transferMode=0x%X", transferMode));
						}
						break;
					default:
						log.error(String.format("MMIOHandlerEFlashAta.executeCommand ATA_CMD_SET_FEATURES unimplemented subcommandCode=0x%X", subcommandCode));
						break;
				}

				prepareDataEndWithDelay(dataLength, dataLength, delayUs);
				break;
			case ATA_CMD_READ:
				dataLength = 0;
				delayUs = 0;
				if (isLBA()) {
					int lba = firstCommand ? getLBA() : getLogicalBlockAddress();
					int sectorCount = getSectorCount();
					setLogicalBlockAddress(lba + 1);
					dataLength = SECTOR_SIZE;
					if (firstCommand) {
						totalDataLength = sectorCount * SECTOR_SIZE;
					}

					eflashFile.ioLseek(getFileOffset(lba));
					eflashFile.ioRead(buffer, 0, SECTOR_SIZE);

					prepareDataInit(dataLength);
					for (int i = 0; i < dataLength; i++) {
						prepareData8(buffer[i] & 0xFF);
					}

					if (log.isDebugEnabled()) {
						log.debug(String.format("MMIOHandlerEFlashAta.executeCommand ATA_CMD_READ LBA=0x%X, sectorCount=0x%X", lba, sectorCount));
						if (log.isTraceEnabled()) {
							log.trace(String.format("data: %s", Utilities.getMemoryDump(buffer)));
						}
					}
				} else {
					log.error(String.format("MMIOHandlerEFlashAta.executeCommand ATA_CMD_READ unimplemented non-LBA read"));
				}
				prepareDataEnd(dataLength, totalDataLength);
				break;
			case ATA_CMD_WRITE:
				if (isLBA()) {
					int lba = getLBA();
					int sectorCount = getSectorCount();
					setLogicalBlockAddress(lba);
					dataLength = SECTOR_SIZE;
					totalDataLength = sectorCount * SECTOR_SIZE;
					prepareDataReceive(dataLength, totalDataLength);

					if (log.isDebugEnabled()) {
						log.debug(String.format("MMIOHandlerEFlashAta.executeCommand ATA_CMD_WRITE LBA=0x%X, sectorCount=0x%X", lba, sectorCount));
					}
				} else {
					log.error(String.format("MMIOHandlerEFlashAta.executeCommand ATA_CMD_WRITE unimplemented non-LBA read"));
				}
				break;
			case ATA_CMD_FLUSH:
				prepareDataEnd(0, 0);
				break;
			case ATA_CMD_SLEEP:
				prepareDataEnd(0, 0);
				break;
			case ATA_CMD_DEV_RESET:
				prepareDataEnd(0, 0);
				break;
			case ATA_CMD_STANDBYNOW1:
				prepareDataEnd(0, 0);
				break;
			default:
				log.error(String.format("MMIOHandlerEFlashAta.executeCommand unimplemented command 0x%X(%s)", command, getCommandName(command)));
				break;
		}
	}

	@Override
	protected void executeCommandWithData(int command, int pendingOperationCodeParameters, int[] data, int dataLength, boolean firstCommand, boolean lastCommand) {
		switch (command) {
			case ATA_CMD_WRITE:
				if (isLBA()) {
					int lba = firstCommand ? getLBA() : getLogicalBlockAddress();
					setLogicalBlockAddress(lba + 1);

					for (int i = 0; i < SECTOR_SIZE; i++) {
						buffer[i] = (byte) data[i];
					}
					eflashFile.ioLseek(getFileOffset(lba));
					eflashFile.ioWrite(buffer, 0, SECTOR_SIZE);

					if (log.isDebugEnabled()) {
						log.debug(String.format("MMIOHandlerEFlashAta.executeCommandWithData ATA_CMD_WRITE lba=0x%X, lastCommand=%b", lba, lastCommand));
						if (log.isTraceEnabled()) {
							log.trace(String.format("data: %s", Utilities.getMemoryDump(buffer)));
						}
					}
				} else {
					log.error(String.format("MMIOHandlerEFlashAta.executeCommandWithData ATA_CMD_WRITE unimplemented non-LBA read"));
				}

				if (lastCommand) {
					MMIOHandlerEFlash.getInstance().setInterruptFlag(0x00010001 | 0x00020002);
					commandCompleted();
				}
				break;
			default:
				log.error(String.format("MMIOHandlerEFlashAta.executeCommandWithData unknown command 0x%X(%s)", command, getCommandName(command)));
				break;
		}
	}
}
