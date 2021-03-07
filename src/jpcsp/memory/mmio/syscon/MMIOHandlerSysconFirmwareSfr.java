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

import static jpcsp.memory.mmio.syscon.SysconSfrNames.getSfr16Name;
import static jpcsp.memory.mmio.syscon.SysconSfrNames.getSfr1Name;
import static jpcsp.memory.mmio.syscon.SysconSfrNames.getSfr1Names;
import static jpcsp.memory.mmio.syscon.SysconSfrNames.getSfr8Name;
import static jpcsp.memory.mmio.syscon.SysconSfrNames.hasSfr1Name;
import static jpcsp.nec78k0.Nec78k0Processor.BRK;
import static jpcsp.nec78k0.Nec78k0Processor.PSW_ADDRESS;
import static jpcsp.nec78k0.Nec78k0Processor.RESET;
import static jpcsp.nec78k0.Nec78k0Processor.SP_ADDRESS;
import static jpcsp.util.Utilities.clearBit;
import static jpcsp.util.Utilities.clearFlag;
import static jpcsp.util.Utilities.hasBit;
import static jpcsp.util.Utilities.hasFlag;
import static jpcsp.util.Utilities.setBit;

import java.util.Arrays;

import org.apache.log4j.Logger;

import jpcsp.nec78k0.Nec78k0MMIOHandlerBase;
import jpcsp.nec78k0.Nec78k0Processor;
import jpcsp.util.Utilities;

/**
 * @author gid15
 *
 */
public class MMIOHandlerSysconFirmwareSfr extends Nec78k0MMIOHandlerBase {
	public static final int NUMBER_SPECIAL_FUNCTION_REGISTERS = 256;
	// Interrupt Vector Table addresses
	public static final int INTLVI   = 0x04;
	public static final int INTP0    = 0x06;
	public static final int INTP1    = 0x08;
	public static final int INTP2    = 0x0A;
	public static final int INTP3    = 0x0C;
	public static final int INTP4    = 0x0E;
	public static final int INTP5    = 0x10;
	public static final int INTSRE6  = 0x12;
	public static final int INTSR6   = 0x14;
	public static final int INTST6   = 0x16;
	public static final int INTCSI10 = 0x18;
	public static final int INTST0   = 0x18;
	public static final int INTTMH1  = 0x1A;
	public static final int INTTMH0  = 0x1C;
	public static final int INTTM50  = 0x1E;
	public static final int INTTM000 = 0x20;
	public static final int INTTM010 = 0x22;
	public static final int INTAD    = 0x24;
	public static final int INTSR0   = 0x26;
	public static final int INTWTI   = 0x28;
	public static final int INTTM51  = 0x2A;
	public static final int INTKR    = 0x2C;
	public static final int INTWT    = 0x2E;
	public static final int INTP6    = 0x30;
	public static final int INTP7    = 0x32;
	public static final int INTIIC0  = 0x34;
	public static final int INTDMU   = 0x34;
	public static final int INTCSI11 = 0x36;
	public static final int INTTM001 = 0x38;
	public static final int INTTM011 = 0x3A;
	public static final int INTACSI  = 0x3C;
	// Interrupt Request Flags
	public static final int WTIIF   = INTtoIF(INTWTI);
	public static final int WTIF    = INTtoIF(INTWT);
	public static final int IICIF0  = INTtoIF(INTIIC0);
	public static final int NUMBER_INTERRUPT_FLAGS = 28;
	//
	private final SysconWatchTimer watchTimer;
	private static final int NUMBER_PORTS = 15;
	private final int[] portOutputs = new int[NUMBER_PORTS];
	private final int[] portInputs = new int[NUMBER_PORTS];
	private final int[] portModes = new int[NUMBER_PORTS];
	// Ports:
	// P0.0      LCD32_ON (Output)
	// P0.1      HP_DETECT (Input)
	// P0.2      CPU_SPI1SI (Output)
	// P0.3      CPU_SPI1SO (Input)
	// P0.4      CPU_SPI1SCK (Output)
	// P0.5      STANDBY_HOLD (Output)
	// P0.6      UMD_SW (Input)
	// P1.0      Unused
	// P1.1      Unused
	// P1.2      Unused
	// P1.3      Transmit to Battery
	// P1.4      Receive from Battery
	// P1.5      Unused
	// P1.6      INTP5, POMMEL_ALERT (Input)
	// P1.7      WL_POR (Output)
	// P2.0/ANI0 Unused
	// P2.1/ANI1 Unused
	// P2.2/ANI2 Unused
	// P2.3/ANI3 Unused
	// P2.4/ANI4 Unused
	// P2.5/ANI5 Video detect (Input)
	// P2.6/ANI6 ANALOG_XPORT (Input)
	// P2.7/ANI7 ANALOG_YPORT (Input)
	// P3.0      INTP1, KEY_POWER (Input)
	// P3.1      SYSCON_REQ (Input)
	// P3.2      WLAN_WAKEUP (Input)
	// P3.3      HPRMC_WAKEUP (Input)
	// P4.0      KEY_SELECT (Input)
	// P4.1      KEY_L1 (Input)
	// P4.2      KEY_R1 (Input)
	// P4.3      KEY_START (Input)
	// P4.4      KEY_HOME (Input)
	// P4.5      KEY_HOLD (Input)
	// P4.6      TACHYON_CS (Output)
	// P4.7      Unknown, connected to P12.0?
	// P5.0      KEY_VOLUP (Input)
	// P5.1      KEY_VOLDOWN (Input)
	// P5.2      KEY_DISPLAY (Input)
	// P5.3      KEY_SOUND (Input)
	// P5.4      Unused
	// P5.5      USB_CE
	// P5.6      Unused
	// P5.7      UMD_DETECT (Output)
	// P6.0      I2C_SCL (Output)
	// P6.1      I2C_SDA (Output)
	// P6.2      POMMEL_CS (Output)
	// P6.3      LEPTON_RST (Output)
	// P6.4      GLUON_RST (Output)
	// P6.5      Unused
	// P6.6      Unknown
	// P6.7      TTP1 (Output)
	// P7.0      KEY_UP (Input)
	// P7.1      KEY_RIGHT (Input)
	// P7.2      KEY_DOWN (Input)
	// P7.3      KEY_LEFT (Input)
	// P7.4      KEY_TRIANGLE (Input)
	// P7.5      KEY_CIRCLE (Input)
	// P7.6      KEY_CROSS (Input)
	// P7.7      KEY_SQUARE (Input)
	// P12.0     INTP0, Unknown, connected to P4.7?
	// P12.1     Clock 4 MHz
	// P12.2     Clock 4 MHz
	// P12.3     Clock 32.768 kHz
	// P12.4     Clock 32.768 kHz
	// P13.0     CPU_RESET (Output only)
	// P14.0     Unused
	// P14.1     Unused
	// P14.2     Unused
	// P14.3     Unused
	// P14.4     Unused
	// P14.5     Unused
	private int interruptRequestFlag0; // 16-bit value
	private int interruptRequestFlag1; // 16-bit value
	private int interruptMaskFlag0; // 16-bit value
	private int interruptMaskFlag1; // 16-bit value
	private int prioritySpecificationFlag0; // 16-bit value
	private int prioritySpecificationFlag1; // 16-bit value
	private int watchTimerOperationMode;
	private int i2cShift;
	private int i2cSlaveAddress;
	private int i2cControl;
	private int i2cStatus;
	private int i2cFlag;
	private int i2cClockSelection;
	private int i2cFunctionExpansion;

	public MMIOHandlerSysconFirmwareSfr(int baseAddress) {
		super(baseAddress);

		reset();

		watchTimer = new SysconWatchTimer(this);
		watchTimer.setName("Syscon Watch Timer");
		watchTimer.setDaemon(true);
		watchTimer.setWatchTimerOperationMode(watchTimerOperationMode);
		watchTimer.start();
	}

	public static int INTtoIF(int vectorTableAddress) {
		return (vectorTableAddress >> 1) - 2;
	}

	public static int IFtoINT(int interruptBit) {
		return (interruptBit + 2) << 1;
	}

	@Override
	public void setProcessor(Nec78k0Processor processor) {
		watchTimer.setProcessor(processor);

		super.setProcessor(processor);
	}

	@Override
	public void setLogger(Logger log) {
		watchTimer.setLogger(log);
		super.setLogger(log);
	}

	@Override
	public void reset() {
		Arrays.fill(portOutputs, 0x00);
		Arrays.fill(portInputs, 0x00);
		Arrays.fill(portModes, 0xFF);
		interruptRequestFlag0 = 0x0000;
		interruptRequestFlag1 = 0x0000;
		interruptMaskFlag0 = 0xFFFF;
		interruptMaskFlag1 = 0xFFFF;
		prioritySpecificationFlag0 = 0xFFFF;
		prioritySpecificationFlag1 = 0xFFFF;
		watchTimerOperationMode = 0x00;
		// I2C
		i2cShift = 0x00;
		i2cSlaveAddress = 0x00;
		i2cControl = 0x00;
		i2cStatus = 0x00;
		i2cFlag = 0x00;
		i2cClockSelection = 0x00;
		i2cFunctionExpansion = 0x00;

		// Input P12.0 = 1
		setPortInputBit(12, 0);
	}

	/////////////////////
	// Ports
	/////////////////////

	public void setPortOutputBit(int port, int bit) {
		portOutputs[port] = setBit(portOutputs[port], bit);
	}

	public void clearPortOutputBit(int port, int bit) {
		portOutputs[port] = clearBit(portOutputs[port], bit);
	}

	public void setPortInputBit(int port, int bit) {
		portInputs[port] = setBit(portInputs[port], bit);
	}

	public void clearPortInputBit(int port, int bit) {
		portInputs[port] = clearBit(portInputs[port], bit);
	}

	private int getPortValue(int port) {
		return (portInputs[port] & portModes[port]) | (portOutputs[port] & ~portModes[port]);
	}

	private void setPortOutput(int port, int value) {
		portOutputs[port] = value;
	}

	private int getPortMode(int port) {
		return portModes[port];
	}

	private void setPortMode(int port, int value) {
		portModes[port] = value;
	}

	/////////////////////
	// Watch Dog Timer
	/////////////////////

	private void setWatchdogTimerEnable(int value) {
		if (value == 0xAC) {
			if (log.isDebugEnabled()) {
				log.debug(String.format("Start Watch Dog Timer"));
			}
		}
	}

	/////////////////////
	// Watch Timer
	/////////////////////

	private void setWatchTimerOperationMode(int value) {
		if (watchTimerOperationMode != value) {
			watchTimerOperationMode = value;
			watchTimer.setWatchTimerOperationMode(watchTimerOperationMode);
		}
	}

	/////////////////////
	// Interrupts
	/////////////////////

	public static String getInterruptName(int vectorTableAddress) {
		switch (vectorTableAddress) {
			case RESET    : return "RESET";
			case INTLVI   : return "INTLVI";
			case INTP0    : return "INTP0";
			case INTP1    : return "INTP1";
			case INTP2    : return "INTP2";
			case INTP3    : return "INTP3";
			case INTP4    : return "INTP4";
			case INTP5    : return "INTP5";
			case INTSRE6  : return "INTSRE6";
			case INTSR6   : return "INTSR6";
			case INTST6   : return "INTST6";
			case INTCSI10 : return "INTCSI10";
			case INTTMH1  : return "INTTMH1";
			case INTTMH0  : return "INTTMH0";
			case INTTM50  : return "INTTM50";
			case INTTM000 : return "INTTM000";
			case INTTM010 : return "INTTM010";
			case INTAD    : return "INTAD";
			case INTSR0   : return "INTSR0";
			case INTWTI   : return "INTWTI";
			case INTTM51  : return "INTTM51";
			case INTKR    : return "INTKR";
			case INTWT    : return "INTWT";
			case INTP6    : return "INTP6";
			case INTP7    : return "INTP7";
			case INTIIC0  : return "INTIIC0";
			case INTCSI11 : return "INTCSI11";
			case INTTM001 : return "INTTM001";
			case INTTM011 : return "INTTM011";
			case INTACSI  : return "INTACSI";
			case BRK      : return "BRK";
		}
		return String.format("INT_0x%02X", vectorTableAddress);
	}

	private void checkInterrupts(SysconInterruptRequestInfo info, int interruptRequestFlag, int interruptMaskFlag, int prioritySpecificationFlag, int baseInterruptVectorTableAddress, int numberInterrupts, int baseInterruptNumber) {
		interruptRequestFlag = clearFlag(interruptRequestFlag, interruptMaskFlag);
		if (interruptRequestFlag == 0) {
			return;
		}

		int firstBit = Integer.numberOfTrailingZeros(interruptRequestFlag);
		int afterLastBit = Math.min(numberInterrupts, 32 - Integer.numberOfLeadingZeros(interruptRequestFlag));
		for (int i = firstBit, flag = Utilities.getFlagFromBit(firstBit); i < afterLastBit; i++, flag <<= 1) {
			if (hasFlag(interruptRequestFlag, flag)) {
				int interruptVectorTableAddress = baseInterruptVectorTableAddress + (i << 1);
				if (hasFlag(prioritySpecificationFlag, flag)) {
					// High priority
					if (!info.highPriority) {
						info.vectorTableAddress = interruptVectorTableAddress;
						info.highPriority = true;
						info.interruptRequestBit = baseInterruptNumber + i;
						break;
					}
				} else {
					// Low priority
					if (processor.isInServicePriority()) {
						info.vectorTableAddress = interruptVectorTableAddress;
						info.highPriority = false;
						info.interruptRequestBit = baseInterruptNumber + i;
					}
				}
			}
		}
	}

	public String debugInterruptRequests() {
		StringBuilder s = new StringBuilder();

		for (int i = 0; i < NUMBER_INTERRUPT_FLAGS; i++) {
			if (hasInterruptRequest(i)) {
				if (s.length() == 0) {
					s.append("InterruptRequests: ");
				} else {
					s.append("|");
				}
				s.append(getInterruptName(IFtoINT(i)));
			}
		}

		if (interruptMaskFlag0 != 0xFFFF || interruptMaskFlag1 != 0xFFFF) {
			boolean first = true;
			for (int i = 0; i < NUMBER_INTERRUPT_FLAGS; i++) {
				if (!hasInterruptMask(i)) {
					if (first) {
						if (s.length() > 0) {
							s.append(", ");
						}
						s.append("Non-masked Interrupts: ");
						first = false;
					} else {
						s.append("|");
					}
					s.append(getInterruptName(IFtoINT(i)));
				}
			}
		}

		return s.toString();
	}

	public void checkInterrupts(SysconInterruptRequestInfo info) {
		checkInterrupts(info, interruptRequestFlag0, interruptMaskFlag0, prioritySpecificationFlag0, 0x04, 16, 0);
		if (!info.highPriority) {
			checkInterrupts(info, interruptRequestFlag1, interruptMaskFlag1, prioritySpecificationFlag1, 0x24, NUMBER_INTERRUPT_FLAGS - 16, 16);
		}
	}

	public void setInterruptRequest(int bit) {
		if (bit < 16) {
			interruptRequestFlag0 = setBit(interruptRequestFlag0, bit);
		} else if (bit < 32) {
			interruptRequestFlag1 = setBit(interruptRequestFlag1, bit - 16);
		}

		if (log.isDebugEnabled()) {
			log.debug(String.format("setInterruptRequest bit=0x%X(%s), %s", bit, getInterruptName(IFtoINT(bit)), debugInterruptRequests()));
		}
	}

	public void clearInterruptRequest(int bit) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("clearInterruptRequest bit=0x%X(%s)", bit, getInterruptName(IFtoINT(bit))));
		}

		if (bit < 16) {
			interruptRequestFlag0 = clearBit(interruptRequestFlag0, bit);
		} else if (bit < 32) {
			interruptRequestFlag1 = clearBit(interruptRequestFlag1, bit - 16);
		}
	}

	public boolean hasInterruptRequest(int bit) {
		if (bit < 16) {
			return hasBit(interruptRequestFlag0, bit);
		} else if (bit < 32) {
			return hasBit(interruptRequestFlag1, bit - 16);
		}

		return false;
	}

	public boolean hasInterruptMask(int bit) {
		if (bit < 16) {
			return hasBit(interruptMaskFlag0, bit);
		} else if (bit < 32) {
			return hasBit(interruptMaskFlag1, bit - 16);
		}

		return false;
	}

	/////////////////////
	// I2C Interface
	/////////////////////

	public int getI2cShift() {
		return i2cShift;
	}

	public void setI2cShift(int i2cShift) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("setI2cShift 0x%02X", i2cShift));
		}
		this.i2cShift = i2cShift;
	}

	public int getI2cSlaveAddress() {
		return i2cSlaveAddress;
	}

	public void setI2cSlaveAddress(int i2cSlaveAddress) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("setI2cSlaveAddress 0x%02X", i2cSlaveAddress));
		}
		this.i2cSlaveAddress = i2cSlaveAddress;
	}

	public int getI2cControl() {
		return i2cControl;
	}

	public void setI2cControl(int i2cControl) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("setI2cControl 0x%02X", i2cControl));
		}
		this.i2cControl = i2cControl;
	}

	public int getI2cStatus() {
		return i2cStatus;
	}

	public void setI2cStatus(int i2cStatus) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("setI2cStatus 0x%02X", i2cStatus));
		}
		this.i2cStatus = i2cStatus;
	}

	public int getI2cFlag() {
		return i2cFlag;
	}

	public void setI2cFlag(int i2cFlag) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("setI2cFlag 0x%02X", i2cFlag));
		}
		this.i2cFlag = i2cFlag;
	}

	public int getI2cClockSelection() {
		return i2cClockSelection;
	}

	public void setI2cClockSelection(int i2cClockSelection) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("setI2cClockSelection 0x%02X", i2cClockSelection));
		}
		this.i2cClockSelection = i2cClockSelection;
	}

	public int getI2cFunctionExpansion() {
		return i2cFunctionExpansion;
	}

	public void setI2cFunctionExpansion(int i2cFunctionExpansion) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("setI2cFunctionExpansion 0x%02X", i2cFunctionExpansion));
		}
		this.i2cFunctionExpansion = i2cFunctionExpansion;
	}

	/////////////////////
	// MMIO
	/////////////////////

	public boolean read1(int address, int bit) {
		boolean value = hasBit(internalRead8(address), bit);

		if (log.isTraceEnabled()) {
			log.trace(String.format("0x%04X - read1(%s, %s) returning %b", getPc(), getSfr8Name(address), getSfr1Name(address, bit), value));
		}

		return value;
	}

	// Equivalent to read8() but without logging.
	// Logging is done in the calling method.
	@Override
	public int internalRead8(int address) {
		int value;
		switch (address) {
			case 0xFF00: value = getPortValue(0); break;
			case 0xFF01: value = getPortValue(1); break;
			case 0xFF02: value = getPortValue(2); break;
			case 0xFF03: value = getPortValue(3); break;
			case 0xFF04: value = getPortValue(4); break;
			case 0xFF05: value = getPortValue(5); break;
			case 0xFF06: value = getPortValue(6); break;
			case 0xFF07: value = getPortValue(7); break;
			case 0xFF0C: value = getPortValue(12); break;
			case 0xFF0D: value = getPortValue(13); break;
			case 0xFF0E: value = getPortValue(14); break;
			case PSW_ADDRESS: value = processor.getPsw(); break;
			case 0xFF20: value = getPortMode(0); break;
			case 0xFF21: value = getPortMode(1); break;
			case 0xFF22: value = getPortMode(2); break;
			case 0xFF23: value = getPortMode(3); break;
			case 0xFF24: value = getPortMode(4); break;
			case 0xFF25: value = getPortMode(5); break;
			case 0xFF26: value = getPortMode(6); break;
			case 0xFF27: value = getPortMode(7); break;
			case 0xFF28: value = 0x00; break;
			case 0xFF2C: value = getPortMode(12); break;
			case 0xFF2E: value = getPortMode(14); break;
			case 0xFF43: value = 0x00; break;
			case 0xFF50: value = 0x00; break;
			case 0xFF69: value = 0x00; break;
			case 0xFF6C: value = 0x00; break;
			case 0xFF6F: value = watchTimerOperationMode; break;
			case 0xFF80: value = 0x00; break;
			case 0xFF81: value = 0x00; break;
			case 0xFF88: value = 0x00; break;
			case 0xFFA0: value = 0x80; break;
			case 0xFFA1: value = 0x02; break;
			case 0xFFA2: value = 0x00; break;
			case 0xFFA3: value = 0x10; break;
			case 0xFFA5: value = getI2cShift(); break;
			case 0xFFA6: value = getI2cControl(); break;
			case 0xFFA7: value = getI2cSlaveAddress(); break;
			case 0xFFA8: value = getI2cClockSelection(); break;
			case 0xFFA9: value = getI2cFunctionExpansion(); break;
			case 0xFFAA: value = getI2cStatus(); break;
			case 0xFFAB: value = getI2cFlag(); break;
			case 0xFFAC: value = 0x00; break;
			case 0xFFE0: value = interruptRequestFlag0 & 0xFF; break;
			case 0xFFE1: value = (interruptRequestFlag0 >> 8) & 0xFF; break;
			case 0xFFE2: value = interruptRequestFlag1 & 0xFF; break;
			case 0xFFE3: value = (interruptRequestFlag1 >> 8) & 0xFF; break;
			case 0xFFE4: value = interruptMaskFlag0 & 0xFF; break;
			case 0xFFE5: value = (interruptMaskFlag0 >> 8) & 0xFF; break;
			case 0xFFE6: value = interruptMaskFlag1 & 0xFF; break;
			case 0xFFE7: value = (interruptMaskFlag1 >> 8) & 0xFF; break;
			case 0xFFFB: value = 0x00; break;
			default: value = super.read8(address); break;
		}

		return value;
	}

	@Override
	public int read8(int address) {
		int value = internalRead8(address);

		if (log.isTraceEnabled()) {
			if (hasSfr1Name(address)) {
				log.trace(String.format("0x%04X - read8(%s) returning 0x%02X(%s)", getPc(), getSfr8Name(address), value, getSfr1Names(address, value)));
			} else {
				log.trace(String.format("0x%04X - read8(%s) returning 0x%02X", getPc(), getSfr8Name(address), value));
			}
		}

		return value;
	}

	// Equivalent to read16() but without logging.
	// Logging is done in the calling method.
	@Override
	public int internalRead16(int address) {
		int value;
		switch (address) {
			case SP_ADDRESS: value = processor.getSp(); break;
			default: value = super.read16(address); break;
		}

		return value;
	}

	@Override
	public int read16(int address) {
		int value = internalRead16(address);

		if (log.isTraceEnabled()) {
			log.trace(String.format("0x%04X - read16(%s) returning 0x%04X", getPc(), getSfr16Name(address), value));
		}

		return value;
	}

	public void write1(int address, int bit, boolean value) {
		int value8 = internalRead8(address);
		if (value) {
			value8 = setBit(value8, bit);
		} else {
			value8 = clearBit(value8, bit);
		}
		internalWrite8(address, (byte) value8);

		if (log.isTraceEnabled()) {
			log.trace(String.format("0x%04X - write1(%s, %s, %b) on %s", getPc(), getSfr8Name(address), getSfr1Name(address, bit), value, this));
		}
	}

	public void set1(int address, int bit) {
		int value8 = internalRead8(address);
		value8 = setBit(value8, bit);
		internalWrite8(address, (byte) value8);

		if (log.isTraceEnabled()) {
			log.trace(String.format("0x%04X - set1(%s, %s) on %s", getPc(), getSfr8Name(address), getSfr1Name(address, bit), this));
		}
	}

	public void clear1(int address, int bit) {
		int value8 = internalRead8(address);
		value8 = clearBit(value8, bit);
		internalWrite8(address, (byte) value8);

		if (log.isTraceEnabled()) {
			log.trace(String.format("0x%04X - clear1(%s, %s) on %s", getPc(), getSfr8Name(address), getSfr1Name(address, bit), this));
		}
	}

	// Equivalent to write8() but without logging.
	// Logging is done in the calling method.
	private void internalWrite8(int address, byte value) {
		final int value8 = value & 0xFF;
		switch (address) {
			case 0xFF00: setPortOutput(0, value8); break;
			case 0xFF01: setPortOutput(1, value8); break;
			case 0xFF02: setPortOutput(2, value8); break;
			case 0xFF03: setPortOutput(3, value8); break;
			case 0xFF04: setPortOutput(4, value8); break;
			case 0xFF05: setPortOutput(5, value8); break;
			case 0xFF06: setPortOutput(6, value8); break;
			case 0xFF07: setPortOutput(7, value8); break;
			case 0xFF0C: setPortOutput(12, value8); break;
			case 0xFF0D: setPortOutput(13, value8); break;
			case 0xFF0E: setPortOutput(14, value8); break;
			case 0xFF17: if (value8 != 0x53) { super.write8(address, value); } break;
			case 0xFF18: if (value8 != 0x21) { super.write8(address, value); } break;
			case 0xFF1A: if (value8 != 0x0C) { super.write8(address, value); } break;
			case PSW_ADDRESS: processor.setPsw(value8); break;
			case 0xFF20: setPortMode(0, value8); break;
			case 0xFF21: setPortMode(1, value8); break;
			case 0xFF22: setPortMode(2, value8); break;
			case 0xFF23: setPortMode(3, value8); break;
			case 0xFF24: setPortMode(4, value8); break;
			case 0xFF25: setPortMode(5, value8); break;
			case 0xFF26: setPortMode(6, value8); break;
			case 0xFF27: setPortMode(7, value8); break;
			case 0xFF28: if (value8 != 0x00 && value8 != 0x04 && value8 != 0x22) { super.write8(address, value); } break;
			case 0xFF29: if (value8 != 0x06) { super.write8(address, value); } break;
			case 0xFF2C: setPortMode(12, value8); break;
			case 0xFF2E: setPortMode(14, value8); break;
			case 0xFF2F: if (value8 != 0x00 && value8 != 0x04 && value8 != 0x05) { super.write8(address, value); } break;
			case 0xFF41: if (value8 != 0x00 && value8 != 0x96) { super.write8(address, value); } break;
			case 0xFF43: if (value8 != 0x00 && value8 != 0x80) { super.write8(address, value); } break;
			case 0xFF48: if (value8 != 0x04) { super.write8(address, value); } break;
			case 0xFF49: if (value8 != 0x23 && value8 != 0x3B) { super.write8(address, value); } break;
			case 0xFF50: if (value8 != 0x00 && value8 != 0x1D) { super.write8(address, value); } break;
			case 0xFF56: if (value8 != 0x00 && value8 != 0x01) { super.write8(address, value); } break;
			case 0xFF57: if (value8 != 0x68) { super.write8(address, value); } break;
			case 0xFF58: if (value8 != 0x16) { super.write8(address, value); } break;
			case 0xFF69: if (value8 != 0x00 && value8 != 0x30) { super.write8(address, value); } break;
			case 0xFF6A: if (value8 != 0x07) { super.write8(address, value); } break;
			case 0xFF6B: if (value8 != 0x00) { super.write8(address, value); } break;
			case 0xFF6C: if (value8 != 0x00 && value8 != 0x50 && value8 != 0x80) { super.write8(address, value); } break;
			case 0xFF6F: setWatchTimerOperationMode(value8); break;
			case 0xFF80: if (value8 != 0x00) { super.write8(address, value); } break;
			case 0xFF81: if (value8 != 0x00) { super.write8(address, value); } break;
			case 0xFF88: if (value8 != 0x00) { super.write8(address, value); } break;
			case 0xFF8C: if (value8 != 0x04 && value8 != 0x05) { super.write8(address, value); } break;
			case 0xFF99: setWatchdogTimerEnable(value8); break;
			case 0xFF9F: if (value8 != 0x10 && value8 != 0x50) { super.write8(address, value); } break;
			case 0xFFA0: if (value8 != 0x80 && value8 != 0x81 && value8 != 0x82) { super.write8(address, value); } break;
			case 0xFFA1: if (value8 != 0x00 && value8 != 0x05) { super.write8(address, value); } break;
			case 0xFFA2: if (value8 != 0x00 && value8 != 0x80) { super.write8(address, value); } break;
			case 0xFFA4: if (value8 != 0x01) { super.write8(address, value); } break;
			case 0xFFA5: setI2cShift(value8); break;
			case 0xFFA6: setI2cControl(value8); break;
			case 0xFFA7: setI2cSlaveAddress(value8); break;
			case 0xFFA8: setI2cClockSelection(value8); break;
			case 0xFFA9: setI2cFunctionExpansion(value8); break;
			case 0xFFAB: setI2cFlag(value8); break;
			case 0xFFBA: if (value8 != 0x00 && value8 != 0x04) { super.write8(address, value); } break;
			case 0xFFBB: if (value8 != 0x01) { super.write8(address, value); } break;
			case 0xFFBC: if (value8 != 0x00) { super.write8(address, value); } break;
			case 0xFFC0: if (value8 != 0xA5) { super.write8(address, value); } break;
			case 0xFFC4: if (value8 != 0x01 && value8 != 0xFE) { super.write8(address, value); } break;
			case 0xFFE0: interruptRequestFlag0 = (interruptRequestFlag0 & 0xFF00) | (value8     ); break;
			case 0xFFE1: interruptRequestFlag0 = (interruptRequestFlag0 & 0x00FF) | (value8 << 8); break;
			case 0xFFE2: interruptRequestFlag1 = (interruptRequestFlag1 & 0xFF00) | (value8     ); break;
			case 0xFFE3: interruptRequestFlag1 = (interruptRequestFlag1 & 0x00FF) | (value8 << 8); break;
			case 0xFFE4: interruptMaskFlag0 = (interruptMaskFlag0 & 0xFF00) | (value8     ); break;
			case 0xFFE5: interruptMaskFlag0 = (interruptMaskFlag0 & 0x00FF) | (value8 << 8); break;
			case 0xFFE6: interruptMaskFlag1 = (interruptMaskFlag1 & 0xFF00) | (value8     ); break;
			case 0xFFE7: interruptMaskFlag1 = (interruptMaskFlag1 & 0x00FF) | (value8 << 8); break;
			case 0xFFE8: prioritySpecificationFlag0 = (prioritySpecificationFlag0 & 0xFF00) | (value8     ); break;
			case 0xFFE9: prioritySpecificationFlag0 = (prioritySpecificationFlag0 & 0x00FF) | (value8 << 8); break;
			case 0xFFEA: prioritySpecificationFlag1 = (prioritySpecificationFlag1 & 0xFF00) | (value8     ); break;
			case 0xFFEB: prioritySpecificationFlag1 = (prioritySpecificationFlag1 & 0x00FF) | (value8 << 8); break;
			case 0xFFF0: if (value8 != 0x04 && value8 != 0x06 && value8 != 0xC6) { super.write8(address, value); } break;
			case 0xFFF4: if (value8 != 0x0A && value8 != 0x0C) { super.write8(address, value); } break;
			case 0xFFFB: if (value8 != 0x00 && value8 != 0x10 && value8 != 0x40 && value8 != 0x41) { super.write8(address, value); } break;
			default: super.write8(address, value); break;
		}
	}

	@Override
	public void write8(int address, byte value) {
		internalWrite8(address, value);

		if (log.isTraceEnabled()) {
			if (hasSfr1Name(address)) {
				log.trace(String.format("0x%04X - write8(%s, 0x%02X(%s)) on %s", getPc(), getSfr8Name(address), value & 0xFF, getSfr1Names(address, value), this));
			} else {
				log.trace(String.format("0x%04X - write8(%s, 0x%02X) on %s", getPc(), getSfr8Name(address), value & 0xFF, this));
			}
		}
	}

	// Equivalent to write16() but without logging.
	// Logging is done in the calling method.
	private void internalWrite16(int address, short value) {
		final int value16 = value & 0xFFFF;
		switch (address) {
			case 0xFF12: if (value16 != 0xFFFF) { super.write16(address, value); } break;
			case SP_ADDRESS: processor.setSp(value16); break;
			case 0xFFE0: interruptRequestFlag0 = value16; break;
			case 0xFFE2: interruptRequestFlag1 = value16; break;
			case 0xFFE4: interruptMaskFlag0 = value16; break;
			case 0xFFE6: interruptMaskFlag1 = value16; break;
			case 0xFFE8: prioritySpecificationFlag0 = value16; break;
			case 0xFFEA: prioritySpecificationFlag1 = value16; break;
			default: super.write16(address, value); break;
		}
	}

	@Override
	public void write16(int address, short value) {
		internalWrite16(address, value);

		if (log.isTraceEnabled()) {
			log.trace(String.format("0x%04X - write16(%s, 0x%04X) on %s", getPc(), getSfr16Name(address), value & 0xFFFF, this));
		}
	}

	@Override
	public String toString() {
		return String.format("Syscon SFR %s", debugInterruptRequests());
	}
}
