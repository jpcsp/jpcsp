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

import static jpcsp.HLE.kernel.managers.IntrManager.PSP_I2C_INTR;
import static jpcsp.util.Utilities.clearFlag;
import static jpcsp.util.Utilities.hasFlag;
import static jpcsp.util.Utilities.setFlag;

import java.io.IOException;
import java.util.Arrays;

import org.apache.log4j.Logger;

import jpcsp.Allegrex.compiler.RuntimeContextLLE;
import jpcsp.HLE.kernel.types.IAction;
import jpcsp.HLE.modules.sceI2c;
import jpcsp.memory.mmio.cy27040.CY27040;
import jpcsp.memory.mmio.wm1801.WM1801;
import jpcsp.memory.mmio.wm8750.WM8750;
import jpcsp.scheduler.Scheduler;
import jpcsp.state.StateInputStream;
import jpcsp.state.StateOutputStream;

public class MMIOHandlerI2c extends MMIOHandlerBase {
	public static Logger log = sceI2c.log;
	private static final int STATE_VERSION = 0;
	public static final int PSP_CY27040_I2C_ADDR = 0xD2;
	public static final int PSP_WM8750_I2C_ADDR = 0x34;
	public static final int PSP_WM1801_I2C_ADDR = 0x94;
	private static final int INTERRUPT_FLAG = 0x1;
	private int i2cAddress;
	private int dataLength;
	private int transmitData[] = new int[16];
	private int receiveData[] = new int[16];
	private int dataIndex = -1;
	private int interrupt;
	private final IAction completeCommandAction = new CompleteCommandAction();

	private class CompleteCommandAction implements IAction {
		@Override
		public void execute() {
			completeCommand();
		}
	}

	public MMIOHandlerI2c(int baseAddress) {
		super(baseAddress);
	}

	@Override
	public void read(StateInputStream stream) throws IOException {
		stream.readVersion(STATE_VERSION);
		i2cAddress = stream.readInt();
		dataLength = stream.readInt();
		dataIndex = stream.readInt();
		interrupt = stream.readInt();
		stream.readInts(transmitData);
		stream.readInts(receiveData);

		CY27040.getInstance().read(stream);
    	WM8750.getInstance().read(stream);
    	WM1801.getInstance().read(stream);

    	super.read(stream);
	}

	@Override
	public void write(StateOutputStream stream) throws IOException {
		stream.writeVersion(STATE_VERSION);
		stream.writeInt(i2cAddress);
		stream.writeInt(dataLength);
		stream.writeInt(dataIndex);
		stream.writeInt(interrupt);
		stream.writeInts(transmitData);
		stream.writeInts(receiveData);

		CY27040.getInstance().write(stream);
    	WM8750.getInstance().write(stream);
    	WM1801.getInstance().write(stream);

    	super.write(stream);
	}

	@Override
	public void reset() {
		super.reset();

		i2cAddress = 0;
		dataLength = 0;
		dataIndex = -1;
		interrupt = 0;
		Arrays.fill(transmitData, 0);
		Arrays.fill(receiveData, 0);

		CY27040.getInstance().reset();
		WM8750.getInstance().reset();
		WM1801.getInstance().reset();
	}

	private void writeData(int value) {
		if (dataIndex < 0) {
			i2cAddress = value;
		} else {
			transmitData[dataIndex] = value & 0xFF;
		}
		dataIndex++;
	}

	private int readData() {
		dataIndex++;
		int value = receiveData[dataIndex];

		return value;
	}

	private void writeDataLength(int value) {
		dataLength = value;
		dataIndex = -1;
	}

	private void checkInterrupt() {
		if (hasFlag(interrupt, INTERRUPT_FLAG)) {
			RuntimeContextLLE.triggerInterrupt(getProcessor(), PSP_I2C_INTR);
		} else {
			RuntimeContextLLE.clearInterrupt(getProcessor(), PSP_I2C_INTR);
		}
	}

	private void completeCommand() {
		interrupt = setFlag(interrupt, INTERRUPT_FLAG);
		checkInterrupt();
	}

	private void clearInterrupt(int value) {
		interrupt = clearFlag(interrupt, value);
		checkInterrupt();
	}

	private void startCommand(int command) {
		int delayCompleteCommand = 0;

		// 0x85 is used by sceI2cMasterTransmitReceive after writing the transmit data (prefixed by the transmit address)
		// 0x8A is used by sceI2cMasterTransmitReceive after writing the receive address
		// 0x87 is used by sceI2cMasterTransmit after writing the transmit data (prefixed by the transmit address)
		switch (command) {
			case 0x85:
				// Nothing to do for now
				break;
			case 0x8A:
				// sceI2cMasterTransmitReceive
				// Receiving on the transmit address + 1
				switch (i2cAddress ^ 0x01) {
					case PSP_CY27040_I2C_ADDR:
						CY27040.getInstance().executeTransmitReceiveCommand(transmitData, receiveData);
						delayCompleteCommand = 10000;
						break;
					case PSP_WM8750_I2C_ADDR:
						WM8750.getInstance().executeTransmitReceiveCommand(transmitData, receiveData);
						break;
					case PSP_WM1801_I2C_ADDR:
						WM1801.getInstance().executeTransmitReceiveCommand(transmitData, receiveData);
						break;
					default:
						log.error(String.format("MMIOHandlerI2c.startCommand 0x%X unknown i2cAddress=0x%X", command, i2cAddress));
						return;
				}
				break;
			case 0x87:
				// sceI2cMasterTransmit
				switch (i2cAddress) {
					case PSP_CY27040_I2C_ADDR:
						CY27040.getInstance().executeTransmitCommand(transmitData);
						break;
					case PSP_WM8750_I2C_ADDR:
						WM8750.getInstance().executeTransmitCommand(transmitData);
						break;
					case PSP_WM1801_I2C_ADDR:
						WM1801.getInstance().executeTransmitCommand(transmitData);
						break;
					default:
						log.error(String.format("MMIOHandlerI2c.startCommand 0x%X unknown i2cAddress=0x%X", command, i2cAddress));
						return;
				}
				break;
			default:
				log.error(String.format("MMIOHandlerI2c.startCommand unknown command=0x%X", command));
				return;
		}

		if (delayCompleteCommand > 0) {
			Scheduler.getInstance().addAction(Scheduler.getNow() + delayCompleteCommand, completeCommandAction);
		} else {
			completeCommand();
		}
	}

	@Override
	public int read32(int address) {
		int value;
		switch (address - baseAddress) {
			case 0x00: value = 0; break; // Unknown
			case 0x04: value = 0; break; // Unknown
			case 0x08: value = dataLength; break;
			case 0x0C: value = readData(); break;
			case 0x10: value = 0; break; // Unknown
			case 0x14: value = 0; break; // Unknown
			case 0x1C: value = 0; break; // Unknown
			case 0x28: value = interrupt; break;
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
			case 0x04: startCommand(value); break; // Unknown
			case 0x08: writeDataLength(value); break;
			case 0x0C: writeData(value); break;
			case 0x10: break; // Unknown
			case 0x14: break; // Unknown
			case 0x1C: break; // Unknown
			case 0x28: clearInterrupt(value); break;
			case 0x2C: break; // Unknown
			default: super.write32(address, value); break;
		}

		if (log.isTraceEnabled()) {
			log.trace(String.format("0x%08X - write32(0x%08X, 0x%08X) on %s", getPc(), address, value, this));
		}
	}

	@Override
	public String toString() {
		return String.format("i2cAddress=0x%X", i2cAddress);
	}
}
