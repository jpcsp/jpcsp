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
	public static final int ATA_STATUS_ERROR = 0x01;
	public static final int ATA_STATUS_DATA_REQUEST = 0x08;
	public static final int ATA_STATUS_DEVICE_READY  = 0x40;
	public static final int ATA_STATUS_BUSY  = 0x80;
	public static final int ATA_INTERRUPT_REASON_CoD = 0x01;
	public static final int ATA_INTERRUPT_REASON_IO = 0x02;
	public static final int ATA_CMD_PACKET = 0xA0;
	public static final int ATA_CMD_OP_INQUIRY = 0x12;
	public static final int PERIPHERAL_DEVICE_TYPE_CDROM = 0x05;
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

	public MMIOHandlerAta(int baseAddress) {
		super(baseAddress);

		sectorCount = 0x01;
		sectorNumber = 0x01;
		cylinderLow = 0x14;
		cylinderHigh = 0xEB;
		drive = 0x00;
	}

	private static String getCommandName(int command) {
		switch (command) {
			case ATA_CMD_PACKET: return "PACKET";
		}

		return String.format("UNKNOWN_CMD_0x%02X", command);
	}

	private int getByteCount() {
		return cylinderLow | (cylinderHigh << 8);
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
		data[dataIndex++] = data16 & 0xFF;
		data[dataIndex++] = (data16 >> 8) & 0xFF;

		if (log.isDebugEnabled()) {
			log.debug(String.format("MMIOHandlerAta.writeData 0x%04X", data16));
		}

		if (dataIndex == dataLength) {
			dataLength = 0;
			dataIndex = 0;
			executeCommand();
		}
	}

	private int getData16() {
		int data16 = data[dataIndex++];
		data16 |= data[dataIndex++] << 8;

		if (log.isDebugEnabled()) {
			log.debug(String.format("MMIOHandlerAta.getData16 returning 0x%04X", data16));
		}

		if (dataIndex == dataLength) {
			dataLength = 0;
			dataIndex = 0;
			status &= ~ATA_STATUS_DATA_REQUEST;
			status &= ~ATA_STATUS_BUSY;
			setInterruptReason(true, true);
			triggerInterrupt(getProcessor(), PSP_ATA_INTR);
		}

		return data16;
	}

	private void prepareData8(int data8) {
		data[dataIndex++] = data8;
	}

	private void prepareData(String s) {
		if (s != null) {
			for (int i = 0; i < s.length(); i++) {
				prepareData8(s.charAt(i) & 0xFF);
			}
		}
	}

	private void executeCommand() {
		status &= ~ATA_STATUS_DATA_REQUEST;
		status |= ATA_STATUS_BUSY;

		switch (command) {
			case ATA_CMD_PACKET:
				executePacketCommand();
				break;
			default:
				log.error(String.format("MMIOHandlerAta.executeCommand unknown command 0x%02X", command));
				break;
		}
	}

	private void executePacketCommand() {
		int operationCode = data[0];

		switch (operationCode) {
			case ATA_CMD_OP_INQUIRY:
				int allocationLength = data[4];
				dataIndex = 0;
				prepareData8(PERIPHERAL_DEVICE_TYPE_CDROM);
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
				prepareData("1.020               "); // Vendor-specific
				dataLength = dataIndex;
				dataIndex = 0;
				setByteCount(Math.min(allocationLength, dataLength));
				setInterruptReason(false, true);
				status |= ATA_STATUS_DATA_REQUEST;
				status &= ~ATA_STATUS_BUSY;
				triggerInterrupt(getProcessor(), PSP_ATA_INTR);
				if (log.isDebugEnabled()) {
					log.debug(String.format("ATA_CMD_OP_INQUIRY allocationLength=0x%X", allocationLength));
				}
				break;
			default:
				log.error(String.format("MMIOHandlerAta.executePacketCommand unknown operation code 0x%02X", operationCode));
				break;
		}
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
