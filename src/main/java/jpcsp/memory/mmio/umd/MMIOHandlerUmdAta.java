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

import static jpcsp.HLE.kernel.managers.IntrManager.PSP_ATA_INTR;
import static jpcsp.filesystems.umdiso.ISectorDevice.sectorLength;

import jpcsp.HLE.modules.sceAta;
import jpcsp.memory.mmio.MMIOHandlerBaseAta;

/**
 * Ata interface for the UMD
 *
 * @author gid15
 *
 */
public class MMIOHandlerUmdAta extends MMIOHandlerBaseAta {
	private static MMIOHandlerUmdAta instance;
	public static final int BASE_ADDRESS = 0xBD700000;

	public static MMIOHandlerUmdAta getInstance() {
		if (instance == null) {
			instance = new MMIOHandlerUmdAta(BASE_ADDRESS);
		}
		return instance;
	}

	private MMIOHandlerUmdAta(int baseAddress) {
		super(baseAddress);

		log = sceAta.log;
	}

	@Override
	protected int getInterruptNumber() {
		return PSP_ATA_INTR;
	}

	@Override
	protected boolean supportsCmdPacket() {
		return true;
	}

	@Override
	protected void executeCommand(int command, int[] data, int dataLength, int totalDataLength, boolean firstCommand) {
		log.error(String.format("MMIOHandlerUmdAta.executeCommand unimplemented command 0x%X", command));
	}

	@Override
	protected void executePacketCommand(int[] data) {
		int operationCode = data[0];
		int allocationLength;
		int unknown;
		int delayUs;

		switch (operationCode) {
			case ATA_CMD_OP_INQUIRY:
				allocationLength = data[4];

				if (log.isDebugEnabled()) {
					log.debug(String.format("ATA_CMD_OP_INQUIRY allocationLength=0x%X", allocationLength));
				}

				prepareDataInit(allocationLength);
				prepareData8(ATA_INQUIRY_PERIPHERAL_DEVICE_TYPE_CDROM);
				prepareData8(0x80); // Medium is removable
				prepareData8(0x00); // ISO Version, ACMA Version, ANSI Version
				prepareData8(0x32); // ATAPI Version, Response Data Format
				prepareData8(0x5C); // Additional Length (number of bytes following this one)
				prepareData8(0x00); // Reserved
				prepareData8(0x00); // Reserved
				prepareData8(0x00); // Reserved
				prepareData("SCEI    "); // Vendor Identification
				prepareData("UMD ROM DRIVE   "); // Product Identification
				prepareData("    "); // Product Revision Level
				prepareData("1.090 Oct18 ,2004   "); // Vendor-specific
				// Duration 2ms
				prepareDataEndWithDelay(allocationLength, allocationLength, 2000);
				break;
			case ATA_CMD_OP_TEST_UNIT_READY:
				if (log.isDebugEnabled()) {
					log.debug(String.format("ATA_CMD_OP_TEST_UNIT_READY"));
				}

				// Duration 1ms
				commandCompletedWithDelay(1000);
				break;
			case ATA_CMD_OP_REQUEST_SENSE:
				allocationLength = data[4];

				if (log.isDebugEnabled()) {
					log.debug(String.format("ATA_CMD_OP_REQUEST_SENSE allocationLength=0x%X", allocationLength));
				}

				prepareDataInit(allocationLength);
				boolean mediumPresent = MMIOHandlerUmd.getInstance().hasUmdInserted();
				prepareData8(0x80); // Valid bit, no Error Code
				prepareData8(0x00); // Reserved
				if (mediumPresent) {
					prepareData8(ATA_SENSE_KEY_NO_SENSE); // Successful command
				} else {
					prepareData8(ATA_SENSE_KEY_NOT_READY); // Medium not present
				}
				prepareData32(0); // Information
				prepareData8(10); // Additional Sense Length
				prepareData32(0); // Command Specific Information
				if (mediumPresent) {
					prepareData8(0); // Additional Sense Code
					prepareData8(0); // Additional Sense Code Qualifier
				} else {
					prepareData8(ATA_SENSE_ASC_MEDIUM_NOT_PRESENT); // Additional Sense Code: medium not present
					prepareData8(0x02); // Additional Sense Code Qualifier
				}
				prepareData8(0); // Field Replaceable Unit Code
				prepareData8(0); // SKSV / Sense Key Specific
				prepareData8(0); // Sense Key Specific
				prepareData8(0); // Sense Key Specific
				// Duration 2ms
				prepareDataEndWithDelay(allocationLength, allocationLength, 2000);
				break;
			case ATA_CMD_OP_READ_STRUCTURE:
				allocationLength = data[9] | (data[8] << 8);
				int formatCode = data[7];

				if (log.isDebugEnabled()) {
					log.debug(String.format("ATA_CMD_OP_READ_STRUCTURE formatCode=0x%X, allocationLength=0x%X", formatCode, allocationLength));
				}

				int returnLength = allocationLength + 4;
				prepareDataInit(returnLength);
				delayUs = 0;
				switch (formatCode) {
					case 0x00:
						final int numberOfSectors = 1800 * (1024 * 1024 / sectorLength); // 1.8GB
						final int startingSectorNumber = 0x030000;
						final int endSectorNumber = startingSectorNumber + numberOfSectors - 1;
						prepareData16(allocationLength + 4); // DVD Structure Data Length
						prepareData8(0); // Reserved
						prepareData8(0); // Reserved
						prepareData8(0x80); // Book Type / Part Version
						prepareData8(0x00); // Disc Size (1 => 120mm) / Minimum Rate (0 => 2.52 Mbps)
						prepareData8(0x01); // Number of Layers / Track Path / Layer Type (1 => the Layer contains embossed user data area)
						prepareData8(0xE0); // Linear Density / Track Density
						prepareData8(0); // Reserved
						prepareData24(startingSectorNumber); // Starting Sector Number of Main Data (0x030000 is the only valid value)
						prepareData8(0); // Reserved
						prepareData24(endSectorNumber); // End Sector of Main Data
						prepareData8(0); // Reserved
						prepareData24(0x000000); // End Sector Number in Layer 0
						prepareData8(0); // BCA (Burst Cutting Area) Flag
						prepareData8(0x07); // Reserved
						for (int i = 22; i < returnLength; i++) {
							prepareData8(0); // Unknown
						}
						// Duration 6ms
						delayUs = 6000;
						break;
					default:
						log.error(String.format("ATA_CMD_OP_READ_STRUCTURE unknown formatCode=0x%X", formatCode));
						break;
				}
				prepareDataEndWithDelay(returnLength, returnLength, delayUs);
				break;
			case ATA_CMD_OP_UNKNOWN_F0:
				unknown = data[1];

				if (log.isDebugEnabled()) {
					log.debug(String.format("ATA_CMD_OP_UNKNOWN_F0 unknown=0x%X", unknown));
				}

				// Unknown 1 byte is being returned
				allocationLength = 1;
				prepareDataInit(allocationLength);
				prepareData8(0x08); // Unknown value. The following values are accepted: 0x08, 0x47, 0x48, 0x50
				prepareDataEnd(allocationLength, allocationLength);
				break;
			case ATA_CMD_OP_MODE_SENSE_BIG:
				allocationLength = data[8] | (data[7] << 8);
				int pageCode = data[2] & 0x3F;

				if (log.isDebugEnabled()) {
					log.debug(String.format("ATA_CMD_OP_MODE_SENSE_BIG pageCode=0x%X, allocationLength=0x%X", pageCode, allocationLength));
				}

				prepareDataInit(allocationLength);
				delayUs = 0;
				switch (pageCode) {
					case ATA_PAGE_CODE_POWER_CONDITION:
						prepareData16(26); // Length of following data
						prepareData8(0);
						prepareData8(0);
						prepareData8(0);
						prepareData8(0);
						prepareData8(0);
						prepareData8(0);
						// The following values are unknown, they are returned by a real PSP
						prepareData8(0x9A);
						prepareData8(0x12);
						prepareData8(0);
						prepareData8(0x02);
						prepareData8(0);
						prepareData8(0);
						prepareData8(0);
						prepareData8(0x06);
						prepareData8(0);
						prepareData8(0);
						prepareData8(0);
						prepareData8(0);
						prepareData8(0);
						prepareData8(0);
						prepareData8(0);
						prepareData8(0x04);
						prepareData8(0);
						prepareData8(0);
						prepareData8(0);
						prepareData8(0x04);
						// Duration 2ms
						delayUs = 2000;
						break;
					default:
						log.error(String.format("ATA_CMD_OP_MODE_SENSE_BIG unknown pageCode=0x%X", pageCode));
						break;
				}
				prepareDataEndWithDelay(allocationLength, allocationLength, delayUs);
				break;
			case ATA_CMD_OP_MODE_SELECT_BIG:
				int parameterListLength = data[8] | (data[7] << 8);
				unknown = data[1];

				if (log.isDebugEnabled()) {
					log.debug(String.format("ATA_CMD_OP_MODE_SELECT_BIG parameterListLength=0x%X, unknown=0x%X", parameterListLength, unknown));
				}

				preparePacketCommandParameterList(parameterListLength, operationCode);
				break;
			case ATA_CMD_OP_UNKNOWN_F1:
				unknown = data[7] | (data[6] << 8);

				if (log.isDebugEnabled()) {
					log.debug(String.format("ATA_CMD_OP_UNKNOWN_F1 unknown=0x%X", unknown));
				}

				prepareDataInit(0);
				prepareDataEnd(0, 0);
				break;
			case ATA_CMD_OP_UNKNOWN_F7:
				unknown = data[2];

				if (log.isDebugEnabled()) {
					log.debug(String.format("ATA_CMD_OP_UNKNOWN_F7 unknown=0x%X", unknown));
				}

				commandCompleted();
				break;
			case ATA_CMD_OP_UNKNOWN_FC:
				allocationLength = data[8] | (data[7] << 8);

				if (log.isDebugEnabled()) {
					log.debug(String.format("ATA_CMD_OP_UNKNOWN_FC allocationLength=0x%X", allocationLength));
				}

				prepareDataInit(allocationLength);
				for (int i = 0; i < allocationLength; i++) {
					prepareData8(0);
				}
				prepareDataEnd(allocationLength, allocationLength);
				break;
			case ATA_CMD_OP_READ_BIG:
				int logicalBlockAddress = data[5] | (data[4] << 8) | (data[3] << 16) | (data[2] << 24);
				int numberOfSectorsToTransfer = data[8] | (data[7] << 8);

				if (log.isDebugEnabled()) {
					log.debug(String.format("ATA_CMD_OP_READ_BIG logicalBlockAddress=0x%X, numberOfSectorsToTransfer=0x%X", logicalBlockAddress, numberOfSectorsToTransfer));
				}

				setLogicalBlockAddress(logicalBlockAddress);
				prepareDataInit(0);
				// Duration 1ms (TODO duration not verified on a real PSP)
				prepareDataEndWithDelay(0, 0, 1000);
				break;
			default:
				log.error(String.format("MMIOHandlerUmdAta.executePacketCommand unknown operation code 0x%02X(%s)", operationCode, getOperationCodeName(operationCode)));
				break;
		}
	}

	private void executePacketCommandParameterList(int operationCode, int[] data) {
		switch (operationCode) {
			case ATA_CMD_OP_MODE_SELECT_BIG:
				int pageCode = data[0] & 0x3F;
				int pageLength = data[1];

				if (pageCode != 0) {
					log.error(String.format("ATA_CMD_OP_MODE_SELECT_BIG parameter unknown pageCode=0x%X", pageCode));
				}
				if (pageLength != 0x1A) {
					log.error(String.format("ATA_CMD_OP_MODE_SELECT_BIG parameter unknown pageLength=0x%X", pageLength));
				}

				if (log.isDebugEnabled()) {
					log.debug(String.format("ATA_CMD_OP_MODE_SELECT_BIG parameters pageCode=0x%X, pageLength=0x%X", pageCode, pageLength));
				}
				break;
			default:
				log.error(String.format("MMIOHandlerBaseAta.executePacketCommandParameterList unknown operation code 0x%02X(%s)", operationCode, getOperationCodeName(operationCode)));
				break;
		}

		commandCompleted();
	}

	@Override
	protected void executeCommandWithData(int command, int pendingOperationCodeParameters, int[] data, int dataLength, boolean firstCommand, boolean lastCommand) {
		switch (command) {
			case ATA_CMD_PACKET:
				if (pendingOperationCodeParameters < 0) {
					executePacketCommand(data);
				} else {
					executePacketCommandParameterList(pendingOperationCodeParameters, data);
				}
				break;
			default:
				log.error(String.format("MMIOHandlerUmdAta.executeCommandWithData unknown command 0x%X(%s)", command, getCommandName(command)));
				break;
		}
	}
}
