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

import static jpcsp.Allegrex.compiler.RuntimeContextLLE.clearInterrupt;
import static jpcsp.Allegrex.compiler.RuntimeContextLLE.triggerInterrupt;
import static jpcsp.HLE.kernel.managers.IntrManager.PSP_ATA_INTR;

import java.util.Arrays;

import org.apache.log4j.Logger;

import jpcsp.HLE.modules.sceAta;

/**
 * See "ATA Packet Interface for CD-ROMs SFF-8020i" and ATAPI-4 specification
 * (http://www.t13.org/documents/UploadedDocuments/project/d1153r18-ATA-ATAPI-4.pdf)
 *
 * @author gid15
 *
 */
public class MMIOHandlerAta extends MMIOHandlerBase {
	public static Logger log = sceAta.log;
	private static MMIOHandlerAta instance;
	public static final int BASE_ADDRESS = 0xBD700000;
	public static final int ATA_STATUS_ERROR = 0x01;
	public static final int ATA_STATUS_DATA_REQUEST = 0x08;
	public static final int ATA_STATUS_DEVICE_READY  = 0x40;
	public static final int ATA_STATUS_BUSY  = 0x80;
	public static final int ATA_INTERRUPT_REASON_CoD = 0x01;
	public static final int ATA_INTERRUPT_REASON_IO = 0x02;
	public static final int ATA_CMD_PACKET = 0xA0;
	public static final int ATA_CMD_OP_TEST_UNIT_READY = 0x00;
	public static final int ATA_CMD_OP_REQUEST_SENSE = 0x03;
	public static final int ATA_CMD_OP_INQUIRY = 0x12;
	public static final int ATA_CMD_OP_START_STOP = 0x1B;
	public static final int ATA_CMD_OP_PREVENT_ALLOW = 0x1E;
	public static final int ATA_CMD_OP_READ_BIG = 0x28;
	public static final int ATA_CMD_OP_SEEK = 0x2B;
	public static final int ATA_CMD_OP_READ_POSITION = 0x34;
	public static final int ATA_CMD_OP_READ_DISC_INFO = 0x51;
	public static final int ATA_CMD_OP_MODE_SELECT_BIG = 0x55;
	public static final int ATA_CMD_OP_MODE_SENSE_BIG = 0x5A;
	public static final int ATA_CMD_OP_READ_STRUCTURE = 0xAD;
	public static final int ATA_CMD_OP_SET_SPEED = 0xBB;
	public static final int ATA_CMD_OP_UNKNOWN_F0 = 0xF0;
	public static final int ATA_CMD_OP_UNKNOWN_F1 = 0xF1;
	public static final int ATA_CMD_OP_UNKNOWN_F7 = 0xF7;
	public static final int ATA_CMD_OP_UNKNOWN_FC = 0xFC;
	public static final int ATA_INQUIRY_PERIPHERAL_DEVICE_TYPE_CDROM = 0x05;
	public static final int ATA_SENSE_KEY_NO_SENSE = 0x0;
	public static final int ATA_SENSE_KEY_UNKNOWN9 = 0x9;
	public static final int ATA_PAGE_CODE_POWER_CONDITION = 0x1A;
	private final int[] data = new int[256];
	private int dataIndex;
	private int dataLength;
	private int error;
	private int features;
	private int sectorCount;
	private int sectorNumber;
	private int cylinderLow;
	private int cylinderHigh;
	private int drive;
	private int status;
	private int command;
	private int control;
	private int pendingOperationCodeParameters;
	private int logicalBlockAddress;

	public static MMIOHandlerAta getInstance() {
		if (instance == null) {
			instance = new MMIOHandlerAta(BASE_ADDRESS);
		}
		return instance;
	}

	private MMIOHandlerAta(int baseAddress) {
		super(baseAddress);

		setSignature();
	}

	private static String getCommandName(int command) {
		switch (command) {
			case ATA_CMD_PACKET: return "PACKET";
		}

		return String.format("UNKNOWN_CMD_0x%02X", command);
	}

	private static String getOperationCodeName(int operationCode) {
		switch (operationCode) {
			case ATA_CMD_OP_TEST_UNIT_READY: return "TEST_UNIT_READY";
			case ATA_CMD_OP_REQUEST_SENSE:   return "REQUEST_SENSE";
			case ATA_CMD_OP_INQUIRY:         return "INQUIRY";
			case ATA_CMD_OP_START_STOP:      return "START_STOP";
			case ATA_CMD_OP_PREVENT_ALLOW:   return "PREVENT_ALLOW";
			case ATA_CMD_OP_READ_BIG:        return "READ_BIG";
			case ATA_CMD_OP_SEEK:            return "SEEK";
			case ATA_CMD_OP_READ_POSITION:   return "READ_POSITION";
			case ATA_CMD_OP_READ_DISC_INFO:  return "READ_DISC_INFO";
			case ATA_CMD_OP_MODE_SELECT_BIG: return "MODE_SELECT_BIG";
			case ATA_CMD_OP_MODE_SENSE_BIG:  return "MODE_SENSE_BIG";
			case ATA_CMD_OP_READ_STRUCTURE:  return "READ_STRUCTURE";
			case ATA_CMD_OP_SET_SPEED:       return "SET_SPEED";
		}

		return String.format("UNKNOWN_OP_0x%02X", operationCode);
	}

	private void setSignature() {
		// This is the required signature for a device supporting the ATA_CMD_PACKET command
		sectorCount = 0x01;
		sectorNumber = 0x01;
		cylinderLow = 0x14;
		cylinderHigh = 0xEB;
		drive = 0x00;
	}

	private void setByteCount(int byteCount) {
		cylinderLow = byteCount & 0xFF;
		cylinderHigh = (byteCount >> 8) & 0xFF;
	}

	private void setInterruptReason(boolean CoD, boolean io) {
		if (CoD) {
			sectorCount |= ATA_INTERRUPT_REASON_CoD;
		} else {
			sectorCount &= ~ATA_INTERRUPT_REASON_CoD;
		}

		if (io) {
			sectorCount |= ATA_INTERRUPT_REASON_IO;
		} else {
			sectorCount &= ~ATA_INTERRUPT_REASON_IO;
		}
	}

	private void setCommand(int command) {
		this.command = command;
		dataIndex = 0;

		if (log.isDebugEnabled()) {
			log.debug(String.format("MMIOHandlerAta.setCommand command 0x%02X(%s)", command, getCommandName(this.command)));
		}

		switch (command) {
			case ATA_CMD_PACKET:
				status |= ATA_STATUS_BUSY;
				setInterruptReason(true, false);
				status |= ATA_STATUS_DATA_REQUEST;
				status &= ~ATA_STATUS_BUSY;
				dataLength = 12;
				pendingOperationCodeParameters = -1;
				break;
			default:
				log.error(String.format("MMIOHandlerAta.setCommand unknown command 0x%02X", command));
				break;
		}
	}

	private void setControl(int control) {
		this.control = control;

		if (log.isDebugEnabled()) {
			log.debug(String.format("MMIOHandlerAta.setControl control 0x%02X", this.control));
		}
	}

	private void setFeatures(int features) {
		this.features = features;

		if (log.isDebugEnabled()) {
			log.debug(String.format("MMIOHandlerAta.setFeatures features 0x%02X", this.features));
		}
	}

	private void writeData16(int data16) {
		if (dataIndex < dataLength) {
			data[dataIndex++] = data16 & 0xFF;
			if (dataIndex < dataLength) {
				data[dataIndex++] = (data16 >> 8) & 0xFF;
			}
		}

		if (log.isDebugEnabled()) {
			log.debug(String.format("MMIOHandlerAta.writeData 0x%04X", data16));
		}

		if (dataIndex >= dataLength) {
			dataLength = 0;
			dataIndex = 0;
			executeCommand();
		}
	}

	private int getData16() {
		int data16 = 0;
		if (dataIndex < dataLength) {
			data16 = data[dataIndex++];
			if (dataIndex < dataLength) {
				data16 |= data[dataIndex++] << 8;
			}
		}

		if (log.isDebugEnabled()) {
			log.debug(String.format("MMIOHandlerAta.getData16 returning 0x%04X", data16));
		}

		if (dataIndex >= dataLength) {
			packetCommandCompleted();
		}

		return data16;
	}

	private void prepareDataInit(int allocationLength) {
		dataIndex = 0;
		Arrays.fill(data, 0, allocationLength, 0);
	}

	private void prepareDataEnd(int allocationLength) {
		dataLength = dataIndex;
		dataIndex = 0;
		setByteCount(Math.min(allocationLength, dataLength));
		setInterruptReason(false, true);
		status |= ATA_STATUS_DATA_REQUEST;
		status &= ~ATA_STATUS_BUSY;
		triggerInterrupt(getProcessor(), PSP_ATA_INTR);
	}

	private void prepareData8(int data8) {
		data[dataIndex++] = data8 & 0xFF;
	}

	private void prepareData16(int data16) {
		prepareData8(data16 >> 8);
		prepareData8(data16);
	}

	private void prepareData24(int data24) {
		prepareData8(data24 >> 16);
		prepareData8(data24 >> 8);
		prepareData8(data24);
	}

	private void prepareData32(int data32) {
		prepareData8(data32 >> 24);
		prepareData8(data32 >> 16);
		prepareData8(data32 >> 8);
		prepareData8(data32);
	}

	private void prepareData(String s) {
		if (s != null) {
			for (int i = 0; i < s.length(); i++) {
				prepareData8((int) s.charAt(i));
			}
		}
	}

	public void packetCommandCompleted() {
		dataLength = 0;
		dataIndex = 0;
		status &= ~ATA_STATUS_DATA_REQUEST;
		status &= ~ATA_STATUS_BUSY;
		status |= ATA_STATUS_DEVICE_READY;
		setInterruptReason(true, true);
		triggerInterrupt(getProcessor(), PSP_ATA_INTR);
	}

	public int getLogicalBlockAddress() {
		return logicalBlockAddress;
	}

	private void preparePacketCommandParameterList(int parameterListLength, int operationCode) {
		dataIndex = 0;
		dataLength = parameterListLength;
		setByteCount(parameterListLength);
		pendingOperationCodeParameters = operationCode;
		setInterruptReason(false, false);
		status &= ~ATA_STATUS_BUSY;
		status |= ATA_STATUS_DATA_REQUEST;
		triggerInterrupt(getProcessor(), PSP_ATA_INTR);
	}

	private void executeCommand() {
		status &= ~ATA_STATUS_DATA_REQUEST;
		status |= ATA_STATUS_BUSY;

		switch (command) {
			case ATA_CMD_PACKET:
				if (pendingOperationCodeParameters < 0) {
					executePacketCommand();
				} else {
					executePacketCommandParameterList(pendingOperationCodeParameters);
					pendingOperationCodeParameters = -1;
				}
				break;
			default:
				log.error(String.format("MMIOHandlerAta.executeCommand unknown command 0x%02X", command));
				break;
		}
	}

	private void executePacketCommand() {
		int operationCode = data[0];
		int allocationLength;
		int unknown;

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
				prepareData8(0x21); // ATAPI Version, Response Data Format
				prepareData8(51); // Additional Length (number of bytes following this one)
				prepareData8(0x00); // Reserved
				prepareData8(0x00); // Reserved
				prepareData8(0x00); // Reserved
				prepareData("        "); // Vendor Identification
				prepareData("                "); // Product Identification
				prepareData("    "); // Product Revision Level
				prepareData("1.090               "); // Vendor-specific
				prepareDataEnd(allocationLength);
				break;
			case ATA_CMD_OP_TEST_UNIT_READY:
				if (log.isDebugEnabled()) {
					log.debug(String.format("ATA_CMD_OP_TEST_UNIT_READY"));
				}

				packetCommandCompleted();
				break;
			case ATA_CMD_OP_REQUEST_SENSE:
				allocationLength = data[4];

				if (log.isDebugEnabled()) {
					log.debug(String.format("ATA_CMD_OP_REQUEST_SENSE allocationLength=0x%X", allocationLength));
				}

				prepareDataInit(allocationLength);
				boolean mediaPresent = true;
				prepareData8(0x80); // Valid bit, no Error Code
				prepareData8(0x00); // Reserved
				if (mediaPresent) {
					prepareData8(ATA_SENSE_KEY_NO_SENSE); // Successful command
				} else {
					prepareData8(ATA_SENSE_KEY_UNKNOWN9); // Media not present
				}
				prepareData32(0); // Information
				prepareData8(10); // Additional Sense Length
				prepareData32(0); // Command Specific Information
				if (mediaPresent) {
					prepareData8(0); // Additional Sense Code
				} else {
					prepareData8(5); // Additional Sense Code: media not present
				}
				prepareData8(0); // Additional Sense Code Qualifier
				prepareData8(0); // Field Replaceable Unit Code
				prepareData8(0); // SKSV / Sense Key Specific
				prepareData8(0); // Sense Key Specific
				prepareData8(0); // Sense Key Specific
				prepareDataEnd(allocationLength);
				break;
			case ATA_CMD_OP_READ_STRUCTURE:
				allocationLength = data[9] | (data[8] << 8);
				int formatCode = data[7];

				if (log.isDebugEnabled()) {
					log.debug(String.format("ATA_CMD_OP_READ_STRUCTURE formatCode=0x%X, allocationLength=0x%X", formatCode, allocationLength));
				}

				prepareDataInit(allocationLength);
				switch (formatCode) {
					case 0x00:
						prepareData16(20); // DVD Structure Data Length
						prepareData8(0); // Reserved
						prepareData8(0); // Reserved
						prepareData8(0xA0); // Book Type (0xA => DVD+R) / Book Version
						prepareData8(0x10); // Disc Size (1 => 80mm) / Minimum Rate (0 => 2.52 Mbps)
						prepareData8(0); // Number of Layers / Track Path / Layer Type
						prepareData8(0); // Linear Density / Track Density
						prepareData8(0); // Reserved
						prepareData24(0x030000); // Starting Sector Number of Main Data (0x030000 is the only valid value)
						prepareData8(0); // Reserved
						prepareData24(0x040000); // End Sector of Main Data
						prepareData8(0); // Reserved
						prepareData24(0x000000); // End Sector Number in Layer 0
						prepareData8(0); // BCA (Burst Cutting Area) Flag
						prepareData8(0); // Reserved
						break;
					default:
						log.error(String.format("ATA_CMD_OP_READ_STRUCTURE unknown formatCode=0x%X", formatCode));
						break;
				}
				prepareDataEnd(allocationLength);
				break;
			case ATA_CMD_OP_UNKNOWN_F0:
				allocationLength = data[1];

				if (log.isDebugEnabled()) {
					log.debug(String.format("ATA_CMD_OP_UNKNOWN_F0 allocationLength=0x%X", allocationLength));
				}

				// Unknown 1 byte is being returned
				prepareDataInit(allocationLength);
				prepareData8(0x08); // Unknown value. The following values are accepted: 0x08, 0x47, 0x48, 0x50
				prepareDataEnd(allocationLength);
				break;
			case ATA_CMD_OP_MODE_SENSE_BIG:
				allocationLength = data[8] | (data[7] << 8);
				int pageCode = data[2] & 0x3F;

				if (log.isDebugEnabled()) {
					log.debug(String.format("ATA_CMD_OP_MODE_SENSE_BIG pageCode=0x%X, allocationLength=0x%X", pageCode, allocationLength));
				}

				prepareDataInit(allocationLength);
				switch (pageCode) {
					case ATA_PAGE_CODE_POWER_CONDITION:
						prepareData16(8);
						prepareData8(0);
						prepareData8(0);
						prepareData8(0);
						prepareData8(0);
						prepareData8(0);
						prepareData8(0);
						break;
					default:
						log.error(String.format("ATA_CMD_OP_MODE_SENSE_BIG unknown pageCode=0x%X", pageCode));
						break;
				}
				prepareDataEnd(allocationLength);
				break;
			case ATA_CMD_OP_MODE_SELECT_BIG:
				int parameterListLength = data[8] | (data[7] << 8);

				if (log.isDebugEnabled()) {
					log.debug(String.format("ATA_CMD_OP_MODE_SELECT_BIG parameterListLength=0x%X", parameterListLength));
				}

				preparePacketCommandParameterList(parameterListLength, operationCode);
				break;
			case ATA_CMD_OP_UNKNOWN_F1:
				unknown = data[7] | (data[6] << 8);

				if (log.isDebugEnabled()) {
					log.debug(String.format("ATA_CMD_OP_UNKNOWN_F1 unknown=0x%X", unknown));
				}

				prepareDataInit(unknown);
				prepareDataEnd(unknown);
				break;
			case ATA_CMD_OP_UNKNOWN_F7:
				unknown = data[2];

				if (log.isDebugEnabled()) {
					log.debug(String.format("ATA_CMD_OP_UNKNOWN_F7 unknown=0x%X", unknown));
				}

				packetCommandCompleted();
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
				prepareDataEnd(allocationLength);
				break;
			case ATA_CMD_OP_READ_BIG:
				logicalBlockAddress = data[5] | (data[4] << 8) | (data[3] << 16) | (data[2] << 24);
				int transferLength = data[8] | (data[7] << 8);

				if (log.isDebugEnabled()) {
					log.debug(String.format("ATA_CMD_OP_READ_BIG logicalBlockAddress=0x%X, transferLength=0x%X", logicalBlockAddress, transferLength));
				}

				prepareDataInit(transferLength);
				prepareDataEnd(transferLength);
				break;
			default:
				log.error(String.format("MMIOHandlerAta.executePacketCommand unknown operation code 0x%02X(%s)", operationCode, getOperationCodeName(operationCode)));
				break;
		}
	}

	private void executePacketCommandParameterList(int operationCode) {
		switch (operationCode) {
			case ATA_CMD_OP_MODE_SELECT_BIG:
				int pageCode = data[0] & 0x3F;
				int pageLength = data[1];

				if (pageCode != 0) {
					log.error(String.format("ATA_CMD_OP_MODE_SELECT_BIG parameter unknown pageCode=0x%X", pageCode));
				}
				if (pageLength != 8) {
					log.error(String.format("ATA_CMD_OP_MODE_SELECT_BIG parameter unknown pageLength=0x%X", pageLength));
				}

				if (log.isDebugEnabled()) {
					log.debug(String.format("ATA_CMD_OP_MODE_SELECT_BIG parameters pageCode=0x%X, pageLength=0x%X", pageCode, pageLength));
				}
				break;
			default:
				log.error(String.format("MMIOHandlerAta.executePacketCommandParameterList unknown operation code 0x%02X(%s)", operationCode, getOperationCodeName(operationCode)));
				break;
		}

		packetCommandCompleted();
	}

	private void endOfData(int value) {
		if (value != 0) {
			log.error(String.format("MMIOHandlerAta.endOfData unknown value=0x%02X", value));
		}
	}

	/*
	 * Returns the regular status and clears the interrupt
	 */
	private int getRegularStatus() {
		clearInterrupt(getProcessor(), PSP_ATA_INTR);
		return status;
	}

	/*
	 * Returns the regular status but does not clear the interrupt
	 */
	private int getAlternateStatus() {
		return status;
	}

	@Override
	public int read8(int address) {
		int value;
		switch (address - baseAddress) {
			case 0x1: value = error; break;
			case 0x2: value = sectorCount; break;
			case 0x3: value = sectorNumber; break;
			case 0x4: value = cylinderLow; break;
			case 0x5: value = cylinderHigh; break;
			case 0x6: value = drive; break;
			case 0x7: value = getRegularStatus();  break;
			case 0xE: value = getAlternateStatus(); break;
			default: value = super.read8(address); break;
		}

		if (log.isTraceEnabled()) {
			log.trace(String.format("0x%08X - read8(0x%08X) returning 0x%02X", getPc(), address, value));
		}

		return value;
	}

	@Override
	public void write8(int address, byte value) {
		final int value8 = value & 0xFF;
		switch (address - baseAddress) {
			case 0x1: setFeatures(value8); break;
			case 0x2: sectorCount = value8; break;
			case 0x3: sectorNumber = value8; break;
			case 0x4: cylinderLow = value8; break;
			case 0x5: cylinderHigh = value8; break;
			case 0x6: drive = value8; break;
			case 0x7: setCommand(value8); break;
			case 0x8: endOfData(value8); break;
			case 0xE: setControl(value8); break;
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
			case 0x0: writeData16(value16); break;
			default: super.write16(address, value); break;
		}

		if (log.isTraceEnabled()) {
			log.trace(String.format("0x%08X - write16(0x%08X, 0x%04X) on %s", getPc(), address, value16, this));
		}
	}

	@Override
	public int read16(int address) {
		int value;
		switch (address - baseAddress) {
			case 0x0: value = getData16(); break;
			default: value = super.read16(address); break;
		}

		if (log.isTraceEnabled()) {
			log.trace(String.format("0x%08X - read16(0x%08X) returning 0x%04X", getPc(), address, value));
		}

		return value;
	}
}
