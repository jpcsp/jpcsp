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
import static jpcsp.memory.mmio.MMIOHandlerGpio.GPIO_PORT_BLUETOOTH;

import org.apache.log4j.Logger;

import jpcsp.Allegrex.compiler.RuntimeContextLLE;
import jpcsp.HLE.modules.sceUmdMan;

public class MMIOHandlerUmd extends MMIOHandlerBase {
	public static Logger log = sceUmdMan.log;
	protected int command;
	private int reset;
	// Possible interrupt flags: 0x1, 0x2, 0x10, 0x20, 0x40, 0x80, 0x10000, 0x20000, 0x40000, 0x80000
	private int interrupt;
	private int interruptEnabled;
	protected final int unknownAddresses[] = new int[10];
	protected final int unknownValues[] = new int[10];

	public MMIOHandlerUmd(int baseAddress) {
		super(baseAddress);
	}

	private void setReset(int reset) {
		this.reset = reset;

		if ((reset & 0x1) != 0) {
			MMIOHandlerGpio.getInstance().setPort(GPIO_PORT_BLUETOOTH);
		}
	}

	private void setCommand(int command) {
		this.command = command;

		switch (command & 0xFF)  {
			case 0x01:
				//interrupt |= 0x1 | 0x2 | 0x10 | 0x20 | 0x40 | 0x80 | 0x10000 | 0x20000 | 0x40000 | 0x80000;
				interrupt |= 0x1 | 0x2 | 0x10 | 0x20 | 0x40 | 0x80;
				break;
			case 0x02:
				interrupt |= 0x1 | 0x2;
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
			case 0x18: value = 0; break; // flags 0x10 and 0x100 are being tested
			case 0x1C: value = 0x20; value = 0x00; break; // Tests: (value & 0x1) != 0 (meaning timeout occured?), value < 0x11 
			case 0x20: value = interrupt; break;
			case 0x24: value = 0; break; // Unknown value
			case 0x28: value = interruptEnabled; break; // Unknown value
			case 0x2C: value = 0; break; // Unknown value
			case 0x30: value = 0; break; // Unknown value, error code?
			case 0x38: value = 0; break; // Unknown value
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
			case 0x40: unknownAddresses[0] = value; break;
			case 0x44: unknownValues[0] = value; break;
			case 0x48: unknownAddresses[1] = value; break;
			case 0x4C: unknownValues[1] = value; break;
			case 0x50: unknownAddresses[2] = value; break;
			case 0x54: unknownValues[2] = value; break;
			case 0x58: unknownAddresses[3] = value; break;
			case 0x5C: unknownValues[3] = value; break;
			case 0x60: unknownAddresses[4] = value; break;
			case 0x64: unknownValues[4] = value; break;
			case 0x68: unknownAddresses[5] = value; break;
			case 0x6C: unknownValues[5] = value; break;
			case 0x70: unknownAddresses[6] = value; break;
			case 0x74: unknownValues[6] = value; break;
			case 0x78: unknownAddresses[7] = value; break;
			case 0x7C: unknownValues[7] = value; break;
			case 0x80: unknownAddresses[8] = value; break;
			case 0x84: unknownValues[8] = value; break;
			case 0x88: unknownAddresses[9] = value; break;
			case 0x8C: unknownValues[9] = value; break;
			default: super.write32(address, value); break;
		}

		if (log.isTraceEnabled()) {
			log.trace(String.format("0x%08X - write32(0x%08X, 0x%08X) on %s", getPc(), address, value, this));
		}
	}
}
