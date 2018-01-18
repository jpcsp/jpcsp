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

import static jpcsp.HLE.kernel.managers.IntrManager.PSP_GPIO_INTR;
import static jpcsp.util.Utilities.clearBit;
import static jpcsp.util.Utilities.hasBit;
import static jpcsp.util.Utilities.setBit;

import org.apache.log4j.Logger;

import jpcsp.Allegrex.compiler.RuntimeContextLLE;
import jpcsp.HLE.modules.sceGpio;

public class MMIOHandlerGpio extends MMIOHandlerBase {
	public static Logger log = sceGpio.log;
	public static final int BASE_ADDRESS = 0xBE240000;
	private static MMIOHandlerGpio instance;
	public static final int GPIO_PORT_DISPLAY          = 0x00;
	public static final int GPIO_PORT_WM8750_Port1     = 0x01;
	public static final int GPIO_PORT_SYSCON_START_CMD = 0x03;
	public static final int GPIO_PORT_SYSCON_END_CMD   = 0x04;
	public static final int GPIO_PORT_WM8750_Port5     = 0x05;
	public static final int GPIO_PORT_LED_MS           = 0x06;
	public static final int GPIO_PORT_LED_WLAN         = 0x07;
	public static final int GPIO_PORT_USB              = 0x17;
	public static final int GPIO_PORT_BLUETOOTH        = 0x18;
	public static final int GPIO_PORT_UMD              = 0x1A;
	private static final int NUMBER_PORTS = 32;
	private int ports;
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

	private static String getPortName(int port) {
		switch (port) {
			case GPIO_PORT_DISPLAY: return "DISPLAY";
			case GPIO_PORT_WM8750_Port1: return "WM8750_Port1";
			case GPIO_PORT_SYSCON_START_CMD: return "SYSCON_START_CMD";
			case GPIO_PORT_SYSCON_END_CMD: return "SYSCON_END_CMD";
			case GPIO_PORT_WM8750_Port5: return "WM8750_Port5";
			case GPIO_PORT_LED_MS: return "LED_MS";
			case GPIO_PORT_LED_WLAN: return "LED_WLAN";
			case GPIO_PORT_USB: return "USB";
			case GPIO_PORT_BLUETOOTH: return "BLUETOOTH";
			case GPIO_PORT_UMD: return "UMD";
		}

		return String.format("GPIO_UNKNOWN_PORT_0x%X", port);
	}

	public void setPort(int port) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("MMIOHandlerGpio.setPort 0x%X(%s) on %s", port, getPortName(port), this));
		}

		if (!hasBit(ports, port)) {
			if (hasBit(isRisingEdge, port)) {
				triggerInterrupt(port);
			}
			ports = setBit(ports, port);
		}
	}

	public void clearPort(int port) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("MMIOHandlerGpio.clearPort 0x%X(%s) on %s", port, getPortName(port), this));
		}

		if (hasBit(ports, port)) {
			if (hasBit(isFallingEdge, port)) {
				triggerInterrupt(port);
			}
			ports = clearBit(ports, port);
		}
	}

	private void triggerInterrupt(int bit) {
		if (!hasBit(isInterruptTriggered, bit)) {
			isInterruptTriggered = setBit(isInterruptTriggered, bit);
			checkInterrupt();
		}
	}

	private void checkInterrupt() {
		if ((isInterruptTriggered & isInterruptEnabled) != 0) {
			RuntimeContextLLE.triggerInterrupt(getProcessor(), PSP_GPIO_INTR);
		} else {
			RuntimeContextLLE.clearInterrupt(getProcessor(), PSP_GPIO_INTR);
		}
	}

	private void setPorts(int value) {
		if (value != 0) {
			for (int i = 0; i < NUMBER_PORTS; i++) {
				if (hasBit(value, i)) {
					setPort(i);
				}
			}
		}
	}

	private void clearPorts(int value) {
		if (value != 0) {
			for (int i = 0; i < NUMBER_PORTS; i++) {
				if (hasBit(value, i)) {
					clearPort(i);
				}
			}
		}
	}

	private void acknowledgeInterrupt(int value) {
		if (value != 0 && isInterruptTriggered != 0) {
			isInterruptTriggered &= ~value;
			checkInterrupt();
		}
	}

	private void setInterruptEnabled(int isInterruptEnabled) {
		if (this.isInterruptEnabled != isInterruptEnabled) {
			this.isInterruptEnabled = isInterruptEnabled;
			checkInterrupt();
		}
	}

	@Override
	public int read32(int address) {
		int value;
		switch (address - baseAddress) {
			case 0x00: value = isOutput; break;
			case 0x04: value = ports; break;
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
			case 0x1C: setInterruptEnabled(value); break;
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

	private static String getPortNames(int bits) {
		StringBuilder s = new StringBuilder();
		for (int i = 0; i < NUMBER_PORTS; i++) {
			if (hasBit(bits, i)) {
				if (s.length() > 0) {
					s.append("|");
				}
				s.append(getPortName(i));
			}
		}

		return s.toString();
	}

	@Override
	public String toString() {
		return String.format("MMIOHandlerGpio ports=0x%08X(%s), isInterruptEnabled=0x%08X(%s), isInterruptTriggered=0x%08X(%s), isOutput=0x%08X(%s), isEdgeDetection=0x%08X, isFallingEdge=0x%08X, isRisingEdge=0x%08X, isInputOn=0x%08X", ports, getPortNames(ports), isInterruptEnabled, getPortNames(isInterruptEnabled), isInterruptTriggered, getPortNames(isInterruptTriggered), isOutput, getPortNames(isOutput), isEdgeDetection, isFallingEdge, isRisingEdge, isInputOn);
	}
}
