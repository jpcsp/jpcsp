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

import static jpcsp.Allegrex.compiler.RuntimeContextLLE.clearInterruptException;
import static jpcsp.Allegrex.compiler.RuntimeContextLLE.triggerInterruptException;
import static jpcsp.HLE.kernel.managers.IntrManager.IP2;
import static jpcsp.HLE.kernel.managers.IntrManager.PSP_ATA_INTR;
import static jpcsp.HLE.kernel.managers.IntrManager.PSP_I2C_INTR;
import static jpcsp.HLE.kernel.managers.IntrManager.PSP_MECODEC_INTR;
import static jpcsp.HLE.kernel.managers.IntrManager.PSP_VBLANK_INTR;
import static jpcsp.util.Utilities.clearBit;
import static jpcsp.util.Utilities.hasBit;

import java.io.IOException;
import java.util.Arrays;

import org.apache.log4j.Logger;

import jpcsp.Processor;
import jpcsp.Allegrex.compiler.RuntimeContextLLE;
import jpcsp.HLE.kernel.managers.IntrManager;
import jpcsp.HLE.modules.InterruptManager;
import jpcsp.state.StateInputStream;
import jpcsp.state.StateOutputStream;

public class MMIOHandlerInterruptMan extends MMIOHandlerBase {
	public static Logger log = InterruptManager.log;
	private static final int STATE_VERSION = 0;
	private static MMIOHandlerProxyOnCpu instance;
	public static final int BASE_ADDRESS = 0xBC300000;
	public static final int NUMBER_INTERRUPTS = 64;
	private final boolean interruptTriggered[] = new boolean[NUMBER_INTERRUPTS];
	private final boolean interruptEnabled[] = new boolean[NUMBER_INTERRUPTS];
	private final Processor processor;

	public static MMIOHandlerInterruptMan getInstance(Processor processor) {
		return (MMIOHandlerInterruptMan) getProxyInstance().getInstance(processor);
	}

	public static MMIOHandlerProxyOnCpu getProxyInstance() {
		if (instance == null) {
			MMIOHandlerInterruptMan mainInstance = new MMIOHandlerInterruptMan(BASE_ADDRESS, RuntimeContextLLE.getMainProcessor());
			MMIOHandlerInterruptMan meInstance = new MMIOHandlerInterruptMan(BASE_ADDRESS, RuntimeContextLLE.getMediaEngineProcessor());
			instance = new MMIOHandlerProxyOnCpu(mainInstance, meInstance);
		}
		return instance;
	}

	private MMIOHandlerInterruptMan(int baseAddress, Processor processor) {
		super(baseAddress);
		this.processor = processor;
	}

	@Override
	protected Processor getProcessor() {
		return processor;
	}

	@Override
	public void read(StateInputStream stream) throws IOException {
		stream.readVersion(STATE_VERSION);
		stream.readBooleans(interruptTriggered);
		stream.readBooleans(interruptEnabled);
		super.read(stream);
	}

	@Override
	public void write(StateOutputStream stream) throws IOException {
		stream.writeVersion(STATE_VERSION);
		stream.writeBooleans(interruptTriggered);
		stream.writeBooleans(interruptEnabled);
		super.write(stream);
	}

	@Override
	public void reset() {
		super.reset();

		Arrays.fill(interruptTriggered, false);
		Arrays.fill(interruptEnabled, false);
	}

	public void triggerInterrupt(int interruptNumber) {
		if (!hasInterruptTriggered(interruptNumber)) {
			interruptTriggered[interruptNumber] = true;
			checkException();
		}
	}

	public void clearInterrupt(int interruptNumber) {
		if (hasInterruptTriggered(interruptNumber)) {
			interruptTriggered[interruptNumber] = false;
			checkException();
		}
	}

	public void enableInterrupt(int interruptNumber) {
		if (!hasInterruptEnabled(interruptNumber)) {
			interruptEnabled[interruptNumber] = true;
			checkException();
		}
	}

	public boolean hasInterruptEnabled(int interruptNumber) {
		return interruptEnabled[interruptNumber];
	}

	public boolean hasInterruptTriggered(int interruptNumber) {
		return interruptTriggered[interruptNumber];
	}

	private void checkException() {
		if (doTriggerException()) {
			triggerInterruptException(getProcessor(), IP2);
		} else {
			clearInterruptException(getProcessor(), IP2);
		}
	}

	public boolean doTriggerException() {
		for (int i = 0; i < NUMBER_INTERRUPTS; i++) {
			if (interruptTriggered[i] && interruptEnabled[i]) {
				if (log.isDebugEnabled()) {
					log.debug(String.format("doTriggerException on %s", this));
				}
				return true;
			}
		}

		return false;
	}

	private void setBits(boolean values[], int value, int offset, int mask) {
		for (int i = 0; mask != 0; i++, value >>>= 1, mask >>>= 1) {
			if ((mask & 1) != 0) {
				values[offset + i] = (value & 1) != 0;
			}
		}
		checkException();
	}

	private void setBits1(boolean values[], int value) {
		setBits(values, value, 0, 0xDFFFFFF0);
	}

	private void setBits2(boolean values[], int value) {
		setBits(values, value, 32, 0xFFFF3F3F);
	}

	private void setBits3(boolean values[], int value) {
		int value3 = (value & 0xC0) | ((value >> 2) & 0xC000);
		setBits(values, value3, 32, 0x0000C0C0);
	}

	private int getBits(boolean values[], int offset) {
		int value = 0;
		for (int i = 31; i >= 0; i--) {
			value <<= 1;
			if (values[offset + i]) {
				value |= 1;
			}
		}

		return value;
	}

	private int getBits1(boolean values[]) {
		return getBits(values, 0);
	}

	private int getBits2(boolean values[]) {
		return getBits(values, 32) & 0xFFFF3F3F;
	}

	private int getBits3(boolean values[]) {
		int value3 = getBits(values, 32);
		value3 = (value3 & 0xC0) | ((value3 & 0xC000) << 2);
		return value3;
	}

	@Override
	public int read32(int address) {
		int value;
		switch (address - baseAddress) {
			// Interrupt triggered (only those which are enabled):
			case 0x00: value = getBits1(interruptTriggered) & getBits1(interruptEnabled); break;
			case 0x10: value = getBits2(interruptTriggered) & getBits2(interruptEnabled); break;
			case 0x20: value = getBits3(interruptTriggered) & getBits3(interruptEnabled); break;
			// Interrupt occurred (read only inside sceKernelIsInterruptOccurred, never written):
			case 0x04: value = getBits1(interruptTriggered); break;
			case 0x14: value = getBits2(interruptTriggered); break;
			case 0x24: value = getBits3(interruptTriggered); break;
			// Interrupt enabled:
			case 0x08: value = getBits1(interruptEnabled); break;
			case 0x18: value = getBits2(interruptEnabled); break;
			case 0x28: value = getBits3(interruptEnabled); break;
			default: value = super.read32(address); break;
		}

		if (log.isTraceEnabled()) {
			log.trace(String.format("0x%08X - read32(0x%08X) on %s returning 0x%08X", getPc(), address, this, value));
		}

		return value;
	}

	@Override
	public void write32(int address, int value) {
		switch (address - baseAddress) {
			// Interrupt triggered:
			case 0x00:
				// interruptman.prx is only writing the values 0x80000000 and 0x40000000
				// which seems to have the effect of clearing the triggers for these interrupts.
				// The Media Engine is also writing the value 0x00000020 and 0x00001000.
				if (hasBit(value, PSP_VBLANK_INTR)) {
					clearInterrupt(PSP_VBLANK_INTR);
					value = clearBit(value, PSP_VBLANK_INTR);
				}
				if (hasBit(value, PSP_MECODEC_INTR)) {
					clearInterrupt(PSP_MECODEC_INTR);
					value = clearBit(value, PSP_MECODEC_INTR);
				}
				if (hasBit(value, PSP_ATA_INTR)) {
					clearInterrupt(PSP_ATA_INTR);
					value = clearBit(value, PSP_ATA_INTR);
				}
				if (hasBit(value, PSP_I2C_INTR)) {
					clearInterrupt(PSP_I2C_INTR);
					value = clearBit(value, PSP_I2C_INTR);
				}
				if (value != 0) {
					super.write32(address, value);
				}
				break;
			// Interrupt enabled:
			case 0x08: setBits1(interruptEnabled, value); break;
			case 0x18: setBits2(interruptEnabled, value); break;
			case 0x28: setBits3(interruptEnabled, value); break;
			// Unknown:
			default: super.write32(address, value); break;
		}

		if (log.isTraceEnabled()) {
			log.trace(String.format("0x%08X - write32(0x%08X, 0x%08X) on %s", getPc(), address, value, this));
		}
	}

	private void toString(StringBuilder sb, String name, boolean values[]) {
		if (sb.length() > 0) {
			sb.append(", ");
		}
		sb.append(name);
		sb.append("[");
		boolean first = true;
		for (int i = 0; i < values.length; i++) {
			if (values[i]) {
				if (first) {
					first = false;
				} else {
					sb.append("|");
				}
				sb.append(IntrManager.getInterruptName(i));
			}
		}
		sb.append("]");
	}

	public String toStringInterruptTriggered() {
		StringBuilder sb = new StringBuilder();
		toString(sb, "", interruptTriggered);

		return sb.toString();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		toString(sb, "interruptTriggered", interruptTriggered);
		toString(sb, "interruptEnabled", interruptEnabled);

		return sb.toString();
	}
}
