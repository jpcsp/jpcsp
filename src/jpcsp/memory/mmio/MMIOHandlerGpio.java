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
import static jpcsp.HLE.kernel.managers.IntrManager.PSP_GPIO_INTR;

import org.apache.log4j.Logger;

import jpcsp.Allegrex.compiler.RuntimeContextLLE;
import jpcsp.HLE.modules.sceGpio;

public class MMIOHandlerGpio extends MMIOHandlerBase {
	public static Logger log = sceGpio.log;
	public static final int BASE_ADDRESS = 0xBE240000;
	private static MMIOHandlerGpio instance;
	public static final int GPIO_BIT_SYSCON_START_CMD = 3;
	public static final int GPIO_BIT_SYSCON_END_CMD = 4;
	public static final int GPIO_BIT_LED_MS = 6;
	public static final int GPIO_BIT_LED_WLAN = 7;
	public static final int GPIO_BIT_BLUETOOTH = 24;
	public static final int GPIO_BIT_UMD = 26;
	private int bits;
	private int isOutput;
	private int isInputOn;
	private int isInterruptEnabled;
	private int isInterruptTriggered;
	private int isEdgeDetection;
	private int isRisingEdge;
	private int isFallingEdge;
	private int isCapturePort;
	private int isTimerCaptureEnabled;

	public static MMIOHandlerGpio getInstance() {
		if (instance == null) {
			instance = new MMIOHandlerGpio(BASE_ADDRESS);
		}
		return instance;
	}

	private MMIOHandlerGpio(int baseAddress) {
		super(baseAddress);
	}

	private boolean hasBit(int value, int bit) {
		return (value & (1 << bit)) != 0;
	}

	private int setBit(int value, int bit) {
		return value | (1 << bit);
	}

	private int clearBit(int value, int bit) {
		return value & ~(1 << bit);
	}

	public void setPortBit(int bit) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("MMIOHandlerGpio.setPortBit 0x%X on %s", bit, this));
		}

		if (!hasBit(bits, bit)) {
			if (hasBit(isRisingEdge, bit)) {
				triggerInterrupt(bit);
			}
			bits = setBit(bits, bit);
		}
	}

	public void clearPortBit(int bit) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("MMIOHandlerGpio.clearPortBit 0x%X on %s", bit, this));
		}

		if (hasBit(bits, bit)) {
			if (hasBit(isFallingEdge, bit)) {
				triggerInterrupt(bit);
			}
			bits = clearBit(bits, bit);
		}
	}

	private void triggerInterrupt(int bit) {
		if (hasBit(isInterruptEnabled, bit)) {
			isInterruptTriggered = setBit(isInterruptTriggered, bit);
			RuntimeContextLLE.triggerInterrupt(getProcessor(), PSP_GPIO_INTR);
		}
	}

	private void setPorts(int value) {
		if (value != 0) {
			for (int i = 0; i < 32; i++) {
				if (hasBit(value, i)) {
					setPortBit(i);
				}
			}
		}
	}

	private void clearPorts(int value) {
		if (value != 0) {
			for (int i = 0; i < 32; i++) {
				if (hasBit(value, i)) {
					clearPortBit(i);
				}
			}
		}
	}

	private void acknowledgeInterrupt(int value) {
		if (value != 0 && isInterruptTriggered != 0) {
			isInterruptTriggered &= ~value;
			if (isInterruptTriggered == 0) {
				clearInterrupt(getProcessor(), PSP_GPIO_INTR);
			}
		}
	}

	@Override
	public int read32(int address) {
		int value;
		switch (address - baseAddress) {
			case 0x00: value = isOutput; break;
			case 0x04: value = bits; break;
			case 0x10: value = isEdgeDetection; break;
			case 0x14: value = isFallingEdge; break;
			case 0x18: value = isRisingEdge; break;
			case 0x1C: value = isInterruptEnabled; break;
			case 0x20: value = isInterruptTriggered; break;
			case 0x30: value = isCapturePort; break;
			case 0x34: value = isTimerCaptureEnabled; break;
			case 0x40: value = isInputOn; break;
			case 0x48: value = 0; break;
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
			case 0x00: isOutput = value; break;
			case 0x08: setPorts(value); break;
			case 0x0C: clearPorts(value); break;
			case 0x10: isEdgeDetection = value; break;
			case 0x14: isFallingEdge = value; break;
			case 0x18: isRisingEdge = value; break;
			case 0x1C: isInterruptEnabled = value; break;
			case 0x24: acknowledgeInterrupt(value); break;
			case 0x30: isCapturePort = value; break;
			case 0x34: isTimerCaptureEnabled = value; break;
			case 0x40: isInputOn = value; break;
			default: super.write32(address, value); break;
		}

		if (log.isTraceEnabled()) {
			log.trace(String.format("0x%08X - write32(0x%08X, 0x%08X) on %s", getPc(), address, value, this));
		}
	}

	@Override
	public String toString() {
		return String.format("MMIOHandlerGpio bits=0x%08X", bits);
	}
}
