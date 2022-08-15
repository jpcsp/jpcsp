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
package jpcsp.memory.mmio.syscon;

import static jpcsp.HLE.modules.sceSyscon.PSP_SYSCON_CMD_BATTERY_GET_SERIAL;
import static jpcsp.HLE.modules.sceSyscon.PSP_SYSCON_CMD_BATTERY_GET_STATUS_CAP;
import static jpcsp.HLE.modules.sceSyscon.PSP_SYSCON_CMD_BATTERY_GET_VOLT;
import static jpcsp.HLE.modules.sceSyscon.getSysconCmdName;
import static jpcsp.memory.mmio.syscon.MMIOHandlerSysconFirmwareSfr.SRIF6;
import static jpcsp.memory.mmio.syscon.MMIOHandlerSysconFirmwareSfr.STIF6;

import static jpcsp.util.Utilities.clearBit;
import static jpcsp.util.Utilities.hasBit;
import static jpcsp.util.Utilities.isRaisingBit;

import java.io.IOException;
import java.util.Arrays;

import org.apache.log4j.Logger;

import jpcsp.hardware.Battery;
import jpcsp.nec78k0.Nec78k0Processor;
import jpcsp.state.IState;
import jpcsp.state.StateInputStream;
import jpcsp.state.StateOutputStream;

/**
 * Serial interface used to communicate with the PSP battery.
 * See the Baryon Sweeper for a simulation of battery responses:
 *     https://github.com/khubik2/pysweeper/blob/master/pysweeper.py
 * 
 * @author gid15
 *
 */
public class SysconSerialInterfaceUART6 implements IState {
	protected Logger log = Nec78k0Processor.log;
	private static final int STATE_VERSION = 0;
	protected final MMIOHandlerSysconFirmwareSfr sfr;
	private int operationMode;
	private int controlRegister;
	private int clockSelection;
	private int baudRateGeneratorControl;
	private int receptionErrorStatus;
	private final int buffer[] = new int[20];
	private int index;
	private int receptionBufferSize;

	public SysconSerialInterfaceUART6(MMIOHandlerSysconFirmwareSfr sfr) {
		this.sfr = sfr;
	}

	@Override
	public void read(StateInputStream stream) throws IOException {
		stream.readVersion(STATE_VERSION);
		operationMode = stream.readInt();
		controlRegister = stream.readInt();
		clockSelection = stream.readInt();
		baudRateGeneratorControl = stream.readInt();
		receptionErrorStatus = stream.readInt();
		receptionBufferSize = stream.readInt();
		stream.readInts(buffer);
		index = stream.readInt();
	}

	@Override
	public void write(StateOutputStream stream) throws IOException {
		stream.writeVersion(STATE_VERSION);
		stream.writeInt(operationMode);
		stream.writeInt(controlRegister);
		stream.writeInt(clockSelection);
		stream.writeInt(baudRateGeneratorControl);
		stream.writeInt(receptionErrorStatus);
		stream.writeInt(receptionBufferSize);
		stream.writeInts(buffer);
		stream.writeInt(index);
	}

	public void reset() {
		operationMode = 0x01;
		controlRegister = 0x16;
		clockSelection = 0x00;
		baudRateGeneratorControl = 0xFF;
		receptionErrorStatus = 0x00;
		receptionBufferSize = 0;
		index = 0;
		Arrays.fill(buffer, 0);
	}

	private boolean isTransmissionEnabled() {
		return hasBit(operationMode, 7) && hasBit(operationMode, 6);
	}

	private boolean isReceptionEnabled() {
		return hasBit(operationMode, 7) && hasBit(operationMode, 5);
	}

	private String bufferToString(int size) {
		StringBuilder s = new StringBuilder();
		for (int i = 0; i < size; i++) {
			if (i > 0) {
				s.append(", ");
			}
			s.append(String.format("0x%02X", buffer[i]));
		}

		return s.toString();
	}

	private int computeChecksum(int size) {
		int checksum = 0;
		for (int i = 0; i < size; i++) {
			checksum += buffer[i];
		}

		return (checksum & 0xFF) ^ 0xFF;
	}

	private boolean isValidChecksum(int checksum, int size) {
		return computeChecksum(size) == checksum;
	}

	private void startReceptionBuffer(int dataLength) {
		receptionBufferSize = 0;
		// Start with fixed byte 0xA5
		buffer[receptionBufferSize++] = 0xA5;
		// Followed by the data length
		buffer[receptionBufferSize++] = dataLength + 2;
		// Followed by fixed byte 0x06
		buffer[receptionBufferSize++] = 0x06;
	}

	private void endReceptionBuffer() {
		// Add checksum as the last received byte
		int checksum = computeChecksum(receptionBufferSize);
		buffer[receptionBufferSize++] = checksum;

		if (log.isDebugEnabled()) {
			log.debug(String.format("UART6 prepared reception buffer: %s", bufferToString(receptionBufferSize)));
		}

		sfr.setInterruptRequest(SRIF6);
	}

	private void addReceptionBufferData8(int data8) {
		buffer[receptionBufferSize++] = data8 & 0xFF;
	}

	private void addReceptionBufferData16(int data16) {
		addReceptionBufferData8(data16);
		addReceptionBufferData8(data16 >> 8);
	}

	private void addReceptionBufferData32(int data32) {
		addReceptionBufferData8(data32);
		addReceptionBufferData8(data32 >> 8);
		addReceptionBufferData8(data32 >> 16);
		addReceptionBufferData8(data32 >> 24);
	}

	public void setOperationMode(int value) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("UART6 setOperationMode 0x%02X", value));
		}

		if (isRaisingBit(operationMode, value, 6)) {
			// Starting transmission
			index = 0;
		} else if (isRaisingBit(operationMode, value, 5)) {
			// Starting reception
			int transmissionLength = index;
			index = 0;
			receptionBufferSize = 0;

			if (transmissionLength >= 2) {
				int command = buffer[0];
				int length = buffer[1];
				if (command == 0x5A) {
					if (length + 2 == transmissionLength) {
						int checksum = buffer[length + 1];
						if (isValidChecksum(checksum, length + 1)) {
							int batteryCommand = buffer[2];
							int sysconCmdBattery = batteryCommand + 0x60;
							switch (sysconCmdBattery) {
								case PSP_SYSCON_CMD_BATTERY_GET_STATUS_CAP:
									if (log.isDebugEnabled()) {
										log.debug(String.format("UART6 received syscon command %s", getSysconCmdName(sysconCmdBattery)));
									}
									startReceptionBuffer(3);
									addReceptionBufferData8(0x00);
									addReceptionBufferData16(0x1234);
									endReceptionBuffer();
									break;
								case PSP_SYSCON_CMD_BATTERY_GET_VOLT:
									if (log.isDebugEnabled()) {
										log.debug(String.format("UART6 received syscon command %s", getSysconCmdName(sysconCmdBattery)));
									}
									startReceptionBuffer(2);
									addReceptionBufferData16(Battery.getVoltage());
									endReceptionBuffer();
									break;
								case PSP_SYSCON_CMD_BATTERY_GET_SERIAL:
									if (log.isDebugEnabled()) {
										log.debug(String.format("UART6 received syscon command %s", getSysconCmdName(sysconCmdBattery)));
									}
									startReceptionBuffer(4);
									addReceptionBufferData32(Battery.readEepromBatterySerialNumber());
									endReceptionBuffer();
									break;
								case 0xE0:
									if (log.isDebugEnabled()) {
										log.debug(String.format("UART6 received syscon command %s", getSysconCmdName(sysconCmdBattery)));
									}
									startReceptionBuffer(8);
									addReceptionBufferData8(0x01);
									addReceptionBufferData8(0x02);
									addReceptionBufferData8(0x03);
									addReceptionBufferData8(0x04);
									addReceptionBufferData8(0x05);
									addReceptionBufferData8(0x06);
									addReceptionBufferData8(0x07);
									addReceptionBufferData8(0x08);
									endReceptionBuffer();
									break;
								default:
									log.error(String.format("UART6 setOperationMode starting reception for unknown battery command 0x%02X(%s): %s", batteryCommand, getSysconCmdName(sysconCmdBattery), bufferToString(transmissionLength)));
									break;
							}
						} else {
							log.error(String.format("UART6 setOperationMode invalid checksum 0x%02X: %s", checksum, bufferToString(transmissionLength)));
						}
					} else {
						log.error(String.format("UART6 setOperationMode starting reception for unknown command length 0x%02X: %s", length, bufferToString(transmissionLength)));
					}
				} else {
					log.error(String.format("UART6 setOperationMode starting reception for unknown command 0x%02X: %s", command, bufferToString(transmissionLength)));
				}
			} else {
				log.error(String.format("UART6 setOperationMode starting reception for unknown command buffer: %s", bufferToString(transmissionLength)));
			}
		}

		operationMode = value;
	}

	public int getOperationMode() {
		if (log.isDebugEnabled()) {
			log.debug(String.format("UART6 getOperationMode returning 0x%02X", operationMode));
		}
		return operationMode;
	}

	public int getControlRegister() {
		if (log.isDebugEnabled()) {
			log.debug(String.format("UART6 getControlRegister returning 0x%02X", controlRegister));
		}
		return controlRegister;
	}

	public void setControlRegister(int value) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("UART6 setControlRegister 0x%02X", value));
		}

		// Bit 7 is read-only
		value = clearBit(value, 7);

		controlRegister = value;
	}

	public int getClockSelection() {
		return clockSelection;
	}

	public void setClockSelection(int value) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("UART6 setClockSelection 0x%02X", value));
		}

		clockSelection = value;
	}

	public int getBaudRateGeneratorControl() {
		if (log.isDebugEnabled()) {
			log.debug(String.format("UART6 getBaudRateGeneratorControl returning 0x%02X", baudRateGeneratorControl));
		}
		return baudRateGeneratorControl;
	}

	public void setBaudRateGeneratorControl(int value) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("UART6 setBaudRateGeneratorControl 0x%02X", value));
		}

		baudRateGeneratorControl = value;
	}

	public int getReceptionErrorStatus() {
		if (log.isDebugEnabled()) {
			log.debug(String.format("UART6 getReceptionErrorStatus returning 0x%02X", receptionErrorStatus));
		}
		return receptionErrorStatus;
	}

	public void setTransmitRegister(int value) {
		if (isTransmissionEnabled()) {
			if (log.isDebugEnabled()) {
				log.debug(String.format("UART6 setTransmitRegister 0x%02X", value));
			}

			buffer[index++] = value;

			sfr.setInterruptRequest(STIF6);
		} else {
			log.error(String.format("UART6 setTransmitRegister 0x%02X but transmission not enabled", value));
		}
	}

	public int getReceiveRegister() {
		int value = 0;
		if (isReceptionEnabled()) {
			if (index < receptionBufferSize) {
				value = buffer[index++];

				sfr.setInterruptRequest(SRIF6);

				if (log.isDebugEnabled()) {
					log.debug(String.format("UART6 getReceiveRegister returning 0x%02X", value));
				}
			} else {
				log.error(String.format("UART6 getReceiveRegister reception buffer exhausted %d", receptionBufferSize));
			}
		} else {
			log.error(String.format("UART6 getReceiveRegister reception not enabled"));
		}

		return value;
	}
}
