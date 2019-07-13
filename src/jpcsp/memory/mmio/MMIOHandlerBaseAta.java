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

import java.io.IOException;
import java.util.Arrays;

import jpcsp.HLE.kernel.types.IAction;
import jpcsp.scheduler.Scheduler;
import jpcsp.state.StateInputStream;
import jpcsp.state.StateOutputStream;

/**
 * See "ATA Packet Interface for CD-ROMs SFF-8020i" and ATAPI-4 specification
 * (http://www.t13.org/documents/UploadedDocuments/project/d1153r18-ATA-ATAPI-4.pdf)
 *
 * @author gid15
 *
 */
public abstract class MMIOHandlerBaseAta extends MMIOHandlerBase {
	private static final int STATE_VERSION = 0;
	public static final int ATA_STATUS_ERROR = 0x01;
	public static final int ATA_STATUS_DATA_REQUEST = 0x08;
	public static final int ATA_STATUS_DEVICE_READY  = 0x40;
	public static final int ATA_STATUS_BUSY  = 0x80;
	public static final int ATA_INTERRUPT_REASON_CoD = 0x01;
	public static final int ATA_INTERRUPT_REASON_IO = 0x02;
	public static final int ATA_CONTROL_SOFT_RESET = 0x04;
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
	public static final int ATA_SENSE_KEY_NOT_READY = 0x2;
	public static final int ATA_SENSE_ASC_MEDIUM_NOT_PRESENT = 0x3A;
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

	private class PrepareDataEndAction implements IAction {
		private int allocationLength;

		public PrepareDataEndAction(int allocationLength) {
			this.allocationLength = allocationLength;
		}

		@Override
		public void execute() {
			prepareDataEnd(allocationLength);
		}
	}

	private class PacketCommandCompletedAction implements IAction {
		public PacketCommandCompletedAction() {
		}

		@Override
		public void execute() {
			packetCommandCompleted();
		}
	}

	protected MMIOHandlerBaseAta(int baseAddress) {
		super(baseAddress);

		reset();
	}

	@Override
	public void read(StateInputStream stream) throws IOException {
		stream.readVersion(STATE_VERSION);
		stream.readInts(data);
		dataIndex = stream.readInt();
		dataLength = stream.readInt();
		error = stream.readInt();
		features = stream.readInt();
		sectorCount = stream.readInt();
		sectorNumber = stream.readInt();
		cylinderLow = stream.readInt();
		cylinderHigh = stream.readInt();
		drive = stream.readInt();
		status = stream.readInt();
		command = stream.readInt();
		control = stream.readInt();
		pendingOperationCodeParameters = stream.readInt();
		logicalBlockAddress = stream.readInt();
		super.read(stream);
	}

	@Override
	public void write(StateOutputStream stream) throws IOException {
		stream.writeVersion(STATE_VERSION);
		stream.writeInts(data);
		stream.writeInt(dataIndex);
		stream.writeInt(dataLength);
		stream.writeInt(error);
		stream.writeInt(features);
		stream.writeInt(sectorCount);
		stream.writeInt(sectorNumber);
		stream.writeInt(cylinderLow);
		stream.writeInt(cylinderHigh);
		stream.writeInt(drive);
		stream.writeInt(status);
		stream.writeInt(command);
		stream.writeInt(control);
		stream.writeInt(pendingOperationCodeParameters);
		stream.writeInt(logicalBlockAddress);
		super.write(stream);
	}

	private static String getCommandName(int command) {
		switch (command) {
			case ATA_CMD_PACKET: return "PACKET";
		}

		return String.format("UNKNOWN_CMD_0x%02X", command);
	}

	protected static String getOperationCodeName(int operationCode) {
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

	protected abstract int getInterruptNumber();

	protected void setLogicalBlockAddress(int logicalBlockAddress) {
		this.logicalBlockAddress = logicalBlockAddress;
	}

	@Override
	public void reset() {
		super.reset();

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

		if ((control & ATA_CONTROL_SOFT_RESET) != 0) {
			reset();
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

	protected void prepareDataInit(int allocationLength) {
		dataIndex = 0;
		Arrays.fill(data, 0, allocationLength, 0);
	}

	protected void prepareDataEnd(int allocationLength) {
		dataLength = Math.min(allocationLength, dataIndex);
		dataIndex = 0;
		setByteCount(dataLength);
		setInterruptReason(false, true);
		status |= ATA_STATUS_DATA_REQUEST;
		status &= ~ATA_STATUS_BUSY;
		triggerInterrupt(getProcessor(), getInterruptNumber());
	}

	protected void prepareDataEndWithDelay(int allocationLength, int delayUs) {
		if (delayUs <= 0) {
			prepareDataEnd(allocationLength);
		} else {
			Scheduler.getInstance().addAction(Scheduler.getNow() + delayUs, new PrepareDataEndAction(allocationLength));
		}
	}

	protected void prepareData8(int data8) {
		data[dataIndex++] = data8 & 0xFF;
	}

	protected void prepareData16(int data16) {
		prepareData8(data16 >> 8);
		prepareData8(data16);
	}

	protected void prepareData24(int data24) {
		prepareData8(data24 >> 16);
		prepareData8(data24 >> 8);
		prepareData8(data24);
	}

	protected void prepareData32(int data32) {
		prepareData8(data32 >> 24);
		prepareData8(data32 >> 16);
		prepareData8(data32 >> 8);
		prepareData8(data32);
	}

	protected void prepareData(String s) {
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
		triggerInterrupt(getProcessor(), getInterruptNumber());
	}

	public void packetCommandCompletedWithDelay(int delayUs) {
		if (delayUs <= 0) {
			packetCommandCompleted();
		} else {
			Scheduler.getInstance().addAction(Scheduler.getNow() + delayUs, new PacketCommandCompletedAction());
		}
	}

	public int getLogicalBlockAddress() {
		return logicalBlockAddress;
	}

	protected void preparePacketCommandParameterList(int parameterListLength, int operationCode) {
		dataIndex = 0;
		dataLength = parameterListLength;
		setByteCount(parameterListLength);
		pendingOperationCodeParameters = operationCode;
		setInterruptReason(false, false);
		status &= ~ATA_STATUS_BUSY;
		status |= ATA_STATUS_DATA_REQUEST;
		triggerInterrupt(getProcessor(), getInterruptNumber());
	}

	private void executeCommand() {
		status &= ~ATA_STATUS_DATA_REQUEST;
		status |= ATA_STATUS_BUSY;

		switch (command) {
			case ATA_CMD_PACKET:
				if (pendingOperationCodeParameters < 0) {
					executePacketCommand(data);
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

	protected abstract void executePacketCommand(int[] data);

	private void executePacketCommandParameterList(int operationCode) {
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
		clearInterrupt(getProcessor(), getInterruptNumber());
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
