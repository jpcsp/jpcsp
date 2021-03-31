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

import static jpcsp.HLE.modules.sceCtrl.PSP_CTRL_CIRCLE;
import static jpcsp.HLE.modules.sceCtrl.PSP_CTRL_CROSS;
import static jpcsp.HLE.modules.sceCtrl.PSP_CTRL_DOWN;
import static jpcsp.HLE.modules.sceCtrl.PSP_CTRL_HOLD;
import static jpcsp.HLE.modules.sceCtrl.PSP_CTRL_HOME;
import static jpcsp.HLE.modules.sceCtrl.PSP_CTRL_LEFT;
import static jpcsp.HLE.modules.sceCtrl.PSP_CTRL_LTRIGGER;
import static jpcsp.HLE.modules.sceCtrl.PSP_CTRL_NOTE;
import static jpcsp.HLE.modules.sceCtrl.PSP_CTRL_RIGHT;
import static jpcsp.HLE.modules.sceCtrl.PSP_CTRL_RTRIGGER;
import static jpcsp.HLE.modules.sceCtrl.PSP_CTRL_SCREEN;
import static jpcsp.HLE.modules.sceCtrl.PSP_CTRL_SELECT;
import static jpcsp.HLE.modules.sceCtrl.PSP_CTRL_SQUARE;
import static jpcsp.HLE.modules.sceCtrl.PSP_CTRL_START;
import static jpcsp.HLE.modules.sceCtrl.PSP_CTRL_TRIANGLE;
import static jpcsp.HLE.modules.sceCtrl.PSP_CTRL_UP;
import static jpcsp.HLE.modules.sceCtrl.PSP_CTRL_VOLDOWN;
import static jpcsp.HLE.modules.sceCtrl.PSP_CTRL_VOLUP;
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
import static jpcsp.util.Utilities.getByte0;
import static jpcsp.util.Utilities.getByte1;
import static jpcsp.util.Utilities.hasBit;
import static jpcsp.util.Utilities.hasFlag;
import static jpcsp.util.Utilities.isFallingBit;
import static jpcsp.util.Utilities.setBit;
import static jpcsp.util.Utilities.setByte0;
import static jpcsp.util.Utilities.setByte1;

import java.io.IOException;
import java.util.Arrays;

import org.apache.log4j.Logger;

import jpcsp.State;
import jpcsp.nec78k0.Nec78k0MMIOHandlerBase;
import jpcsp.nec78k0.Nec78k0Processor;
import jpcsp.state.StateInputStream;
import jpcsp.state.StateOutputStream;
import jpcsp.util.Utilities;

/**
 * @author gid15
 *
 */
public class MMIOHandlerSysconFirmwareSfr extends Nec78k0MMIOHandlerBase {
	private static final int STATE_VERSION = 0;
	private static boolean dummyTesting = true;
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
	public static final int PIF0    = INTtoIF(INTP0);
	public static final int PIF1    = INTtoIF(INTP1);
	public static final int PIF2    = INTtoIF(INTP2);
	public static final int PIF3    = INTtoIF(INTP3);
	public static final int PIF4    = INTtoIF(INTP4);
	public static final int PIF5    = INTtoIF(INTP5);
	public static final int PIF6    = INTtoIF(INTP6);
	public static final int PIF7    = INTtoIF(INTP7);
	public static final int NUMBER_INTERRUPT_FLAGS = 28;
	//
	// I2C Control
	public static final int IICE0 = 7; // I2C operation enable
	public static final int LREL0 = 6; // Exit from communications
	public static final int WREL0 = 5; // Wait cancellation
	public static final int SPIE0 = 4; // Enable/disable generation of interrupt request when stop condition is detected
	public static final int WTIM0 = 3; // Control of wait and interrupt request generation
	public static final int ACKE0 = 2; // Acknowledgement control
	public static final int STT0  = 1; // Start condition trigger
	public static final int SPT0  = 0; // Stop condition trigger
	// I2C Status
	public static final int MSTS0 = 7; // Master device status
	public static final int ALD0  = 6; // Detection of arbitration loss
	public static final int EXC0  = 5; // Detection of extension code reception
	public static final int COI0  = 4; // Detection of matching addresses
	public static final int TRC0  = 3; // Detection of transmit/receive status
	public static final int ACKD0 = 2; // Detection of acknowledge
	public static final int STD0  = 1; // Detection of start condition
	public static final int SPD0  = 0; // Detection of stop condition
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
	// P3.1      INTP2, SYSCON_REQ (Input)
	// P3.2      INTP3, WLAN_WAKEUP (Input)
	// P3.3      INTP4, HPRMC_WAKEUP (Input)
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
	// P14.0     Unused, INTP6
	// P14.1     Unused, INTP7
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
	private final int[] i2cBuffer = new int[MMIOHandlerSyscon.MAX_DATA_LENGTH];
	private int i2cBufferIndex;
	private int adConverterMode;
	private int analogInputChannelSpecification;
	private int adPortConfiguration;
	private int timerCompare50;
	private int timerHCompare00;
	private int timerHCompare10;
	private int timerHCompare01;
	private int timerHCompare11;
	private int timerClockSelection50;
	private int timerModeControl50;
	private int timerClockSelection51;
	private int timerCompare51;
	private int timerModeControl51;
	private int timerModeControl00;
	private int prescalerMode00;
	private int compareControl00;
	private int timerCompare000;
	private int asynchronousSerialInterfaceOperationMode;
	private int timerHModeRegister0;
	private int timerHModeRegister1;
	private int serialOperationMode10;
	private int serialClockSelection10;
	private int serialOperationMode11;
	private int internalOscillationMode;
	private int mainClockMode;
	private int mainOscillationControl;
	private int oscillationStabilizationTimeSelect;
	private int processorClockControl;
	private int oscillationStabilizationTimeCounterStatus;
	private int externalInterruptRisingEdgeEnable;
	private int externalInterruptFallingEdgeEnable;
	private int clockSelection6;
	private int baudRateGeneratorControl6;
	private int asnychronousSerialInterfaceControl6;
	private int clockOperationModeSelect;
	private int internalMemorySizeSwitching;
	private int internalExpansionRAMSizeSwitching;

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
	public void read(StateInputStream stream) throws IOException {
		stream.readVersion(STATE_VERSION);
		stream.readInts(portInputs);
		stream.readInts(portOutputs);
		stream.readInts(portModes);
		stream.readInts(i2cBuffer);
		interruptRequestFlag0 = stream.readInt();
		interruptRequestFlag1 = stream.readInt();
		interruptMaskFlag0 = stream.readInt();
		interruptMaskFlag1 = stream.readInt();
		prioritySpecificationFlag0 = stream.readInt();
		prioritySpecificationFlag1 = stream.readInt();
		watchTimerOperationMode = stream.readInt();
		i2cShift = stream.readInt();
		i2cSlaveAddress = stream.readInt();
		i2cControl = stream.readInt();
		i2cStatus = stream.readInt();
		i2cFlag = stream.readInt();
		i2cClockSelection = stream.readInt();
		i2cFunctionExpansion = stream.readInt();
		i2cBufferIndex = stream.readInt();
		adConverterMode = stream.readInt();
		analogInputChannelSpecification = stream.readInt();
		adPortConfiguration = stream.readInt();
		timerCompare50 = stream.readInt();
		timerHCompare00 = stream.readInt();
		timerHCompare10 = stream.readInt();
		timerHCompare01 = stream.readInt();
		timerHCompare11 = stream.readInt();
		timerClockSelection50 = stream.readInt();
		timerModeControl50 = stream.readInt();
		timerClockSelection51 = stream.readInt();
		timerCompare51 = stream.readInt();
		timerModeControl51 = stream.readInt();
		timerModeControl00 = stream.readInt();
		prescalerMode00 = stream.readInt();
		compareControl00 = stream.readInt();
		timerCompare000 = stream.readInt();
		asynchronousSerialInterfaceOperationMode = stream.readInt();
		timerHModeRegister0 = stream.readInt();
		timerHModeRegister1 = stream.readInt();
		serialOperationMode10 = stream.readInt();
		serialClockSelection10 = stream.readInt();
		serialOperationMode11 = stream.readInt();
		internalOscillationMode = stream.readInt();
		mainClockMode = stream.readInt();
		mainOscillationControl = stream.readInt();
		oscillationStabilizationTimeSelect = stream.readInt();
		processorClockControl = stream.readInt();
		oscillationStabilizationTimeCounterStatus = stream.readInt();
		externalInterruptRisingEdgeEnable = stream.readInt();
		externalInterruptFallingEdgeEnable = stream.readInt();
		clockSelection6 = stream.readInt();
		baudRateGeneratorControl6 = stream.readInt();
		asnychronousSerialInterfaceControl6 = stream.readInt();
		clockOperationModeSelect = stream.readInt();
		internalMemorySizeSwitching = stream.readInt();
		internalExpansionRAMSizeSwitching = stream.readInt();
		super.read(stream);
	}

	@Override
	public void write(StateOutputStream stream) throws IOException {
		stream.writeVersion(STATE_VERSION);
		stream.writeInts(portInputs);
		stream.writeInts(portOutputs);
		stream.writeInts(portModes);
		stream.writeInts(i2cBuffer);
		stream.writeInt(interruptRequestFlag0);
		stream.writeInt(interruptRequestFlag1);
		stream.writeInt(interruptMaskFlag0);
		stream.writeInt(interruptMaskFlag1);
		stream.writeInt(prioritySpecificationFlag0);
		stream.writeInt(prioritySpecificationFlag1);
		stream.writeInt(watchTimerOperationMode);
		stream.writeInt(i2cShift);
		stream.writeInt(i2cSlaveAddress);
		stream.writeInt(i2cControl);
		stream.writeInt(i2cStatus);
		stream.writeInt(i2cFlag);
		stream.writeInt(i2cClockSelection);
		stream.writeInt(i2cFunctionExpansion);
		stream.writeInt(i2cBufferIndex);
		stream.writeInt(adConverterMode);
		stream.writeInt(analogInputChannelSpecification);
		stream.writeInt(adPortConfiguration);
		stream.writeInt(timerCompare50);
		stream.writeInt(timerHCompare00);
		stream.writeInt(timerHCompare10);
		stream.writeInt(timerHCompare01);
		stream.writeInt(timerHCompare11);
		stream.writeInt(timerClockSelection50);
		stream.writeInt(timerModeControl50);
		stream.writeInt(timerClockSelection51);
		stream.writeInt(timerCompare51);
		stream.writeInt(timerModeControl51);
		stream.writeInt(timerModeControl00);
		stream.writeInt(prescalerMode00);
		stream.writeInt(compareControl00);
		stream.writeInt(timerCompare000);
		stream.writeInt(asynchronousSerialInterfaceOperationMode);
		stream.writeInt(timerHModeRegister0);
		stream.writeInt(timerHModeRegister1);
		stream.writeInt(serialOperationMode10);
		stream.writeInt(serialClockSelection10);
		stream.writeInt(serialOperationMode11);
		stream.writeInt(internalOscillationMode);
		stream.writeInt(mainClockMode);
		stream.writeInt(mainOscillationControl);
		stream.writeInt(oscillationStabilizationTimeSelect);
		stream.writeInt(processorClockControl);
		stream.writeInt(oscillationStabilizationTimeCounterStatus);
		stream.writeInt(externalInterruptRisingEdgeEnable);
		stream.writeInt(externalInterruptFallingEdgeEnable);
		stream.writeInt(clockSelection6);
		stream.writeInt(baudRateGeneratorControl6);
		stream.writeInt(asnychronousSerialInterfaceControl6);
		stream.writeInt(clockOperationModeSelect);
		stream.writeInt(internalMemorySizeSwitching);
		stream.writeInt(internalExpansionRAMSizeSwitching);
		super.write(stream);
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

		adConverterMode = 0x00;
		timerModeControl50 = 0x00;
		timerModeControl51 = 0x00;
		asynchronousSerialInterfaceOperationMode = 0x01;
		timerHModeRegister0 = 0x00;
		timerHModeRegister1 = 0x00;
		serialOperationMode10 = 0x00;
		serialClockSelection10 = 0x00;
		serialOperationMode11 = 0x00;
		internalOscillationMode = 0x80;
		mainClockMode = 0x00;
		mainOscillationControl = 0x80;
		processorClockControl = 0x01;

		// Input P12.0 = 1
		setPortInputBit(12, 0);
		// Input P1.6 = 1
		setPortInputBit(1, 6);
		// Input P3.0 = 1
//		setPortInputBit(3, 0);
		setInterruptRequest(PIF0);
		setInterruptRequest(PIF1);
		setInterruptRequest(PIF5);
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

	private void setButtonPortInput(int port, int bit, int key, int buttons) {
		// Ports for keys have inverted logic: 0 means key pressed, 1 means key released
		if (hasFlag(buttons, key)) {
			clearPortInputBit(port, bit);
		} else {
			setPortInputBit(port, bit);
		}
	}

	private void updateButtonsPortInput(int port) {
		State.controller.hleControllerPoll();
		int buttons = State.controller.getButtons();

		// Only those ports are connected to keys/buttons
		switch (port) {
			case 4:
				setButtonPortInput(4, 0, PSP_CTRL_SELECT, buttons);
				setButtonPortInput(4, 1, PSP_CTRL_LTRIGGER, buttons);
				setButtonPortInput(4, 2, PSP_CTRL_RTRIGGER, buttons);
				setButtonPortInput(4, 3, PSP_CTRL_START, buttons);
				setButtonPortInput(4, 4, PSP_CTRL_HOME, buttons);
				setButtonPortInput(4, 5, PSP_CTRL_HOLD, buttons);
				break;
			case 5:
				setButtonPortInput(5, 0, PSP_CTRL_VOLUP, buttons);
				setButtonPortInput(5, 1, PSP_CTRL_VOLDOWN, buttons);
				setButtonPortInput(5, 2, PSP_CTRL_SCREEN, buttons);
				setButtonPortInput(5, 3, PSP_CTRL_NOTE, buttons);
				break;
			case 7:
				setButtonPortInput(7, 0, PSP_CTRL_UP, buttons);
				setButtonPortInput(7, 1, PSP_CTRL_RIGHT, buttons);
				setButtonPortInput(7, 2, PSP_CTRL_DOWN, buttons);
				setButtonPortInput(7, 3, PSP_CTRL_LEFT, buttons);
				setButtonPortInput(7, 4, PSP_CTRL_TRIANGLE, buttons);
				setButtonPortInput(7, 5, PSP_CTRL_CIRCLE, buttons);
				setButtonPortInput(7, 6, PSP_CTRL_CROSS, buttons);
				setButtonPortInput(7, 7, PSP_CTRL_SQUARE, buttons);
				break;
		}
	}

	private void updatePortInput(int port) {
		switch (port) {
			case 4:
			case 5:
			case 7:
				updateButtonsPortInput(port);
				break;
		}
	}

	private int getPortValue(int port) {
		updatePortInput(port);

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

	/////////////////////////
	// Timer/Event Counters
	/////////////////////////

	private void setTimerModeControl50(int value) {
		if (hasBit(value, 0)) {
			log.error(String.format("setTimerModeControl50 unimplemented output enabled 0x%02X", value));
		}
		if (hasBit(value, 7)) {
			log.error(String.format("setTimerModeControl50 unimplemented TM50 count operation 0x%02X", value));
		}

		timerModeControl50 = value;
	}

	private void setTimerModeControl51(int value) {
		if (hasBit(value, 0)) {
			log.error(String.format("setTimerModeControl51 unimplemented output enabled 0x%02X", value));
		}
		if (hasBit(value, 7)) {
			log.error(String.format("setTimerModeControl51 unimplemented TM51 count operation 0x%02X", value));
		}

		timerModeControl51 = value;
	}

	private void setTimerModeControl00(int value) {
		if (hasBit(value, 2) || hasBit(value, 3)) {
			log.error(String.format("setTimerModeControl00 unimplemented operation enable 0x%02X", value));
		}

		timerModeControl00 = value;
	}

	/////////////////////////
	// Timers
	/////////////////////////

	private void setTimerHModeRegister0(int value) {
		if (hasBit(value, 0)) {
			log.error(String.format("setTimerHModeRegister0 unimplemented output enabled 0x%02X", value));
		}
		if (hasBit(value, 7)) {
			log.error(String.format("setTimerHModeRegister0 unimplemented timer operation 0x%02X", value));
		}

		timerHModeRegister0 = value;
	}

	private void setTimerHModeRegister1(int value) {
		if (hasBit(value, 0)) {
			log.error(String.format("setTimerHModeRegister1 unimplemented output enabled 0x%02X", value));
		}
		if (hasBit(value, 7)) {
			log.error(String.format("setTimerHModeRegister1 unimplemented timer operation 0x%02X", value));
		}

		timerHModeRegister1 = value;
	}

	/////////////////////
	// Clock Generator
	/////////////////////

	private void setMainOscillationControl(int value) {
		// Starting oscillator?
		if (isFallingBit(mainOscillationControl, value, 7)) {
			// Start counting the oscillation stabilization time
			oscillationStabilizationTimeCounterStatus = 0x00;
		}
		mainOscillationControl = value;
	}

	private int getOscillationStabilizationTimeCounterStatus() {
		int status = oscillationStabilizationTimeCounterStatus;

		switch (oscillationStabilizationTimeCounterStatus) {
			case 0x00: oscillationStabilizationTimeCounterStatus = setBit(oscillationStabilizationTimeCounterStatus, 4); break;
			case 0x10: oscillationStabilizationTimeCounterStatus = setBit(oscillationStabilizationTimeCounterStatus, 3); break;
			case 0x18: oscillationStabilizationTimeCounterStatus = setBit(oscillationStabilizationTimeCounterStatus, 2); break;
			case 0x1C: oscillationStabilizationTimeCounterStatus = setBit(oscillationStabilizationTimeCounterStatus, 1); break;
			case 0x1E: oscillationStabilizationTimeCounterStatus = setBit(oscillationStabilizationTimeCounterStatus, 0); break;
			case 0x1F: break;
			default: log.error(String.format("Invalid oscillationStabilizationTimeCounterStatus=0x%02X", oscillationStabilizationTimeCounterStatus));
		}

		return status;
	}

	private void setMainClockMode(int value) {
		// Bit 1 is read-only
		mainClockMode = setBit(value, mainClockMode, 1);

		// Switching from the internal high-speed oscillation clock to the high-speed system clock?
		if (hasBit(mainClockMode, 0) && hasBit(mainClockMode, 2)) {
			// Now operating with the high-speed system clock
			mainClockMode = setBit(mainClockMode , 1);
		}
	}

	private void setProcessorClockControl(int value) {
		// Bit 5 is read-only
		processorClockControl = setBit(value, processorClockControl, 5);

		if (hasBit(processorClockControl, 4)) {
			processorClockControl = setBit(processorClockControl, 5);
		} else {
			processorClockControl = clearBit(processorClockControl, 5);
		}
	}

	/////////////////////
	// Reset Function
	/////////////////////

	private int getResetControlFlag() {
		return 0x00;
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

		if (processor != null) {
			processor.interpreter.setHalted(false);
		}

		if (log.isDebugEnabled()) {
			log.debug(String.format("setInterruptRequest bit=0x%X(%s), interrupts %s, %s", bit, getInterruptName(IFtoINT(bit)), processor.isInterruptEnabled() ? "enabled" : "disabled", debugInterruptRequests()));
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

	private void setInterruptMaskFlag0(int interruptMaskFlag0) {
		this.interruptMaskFlag0 = interruptMaskFlag0;
	}

	private void setInterruptMaskFlag1(int interruptMaskFlag1) {
		this.interruptMaskFlag1 = interruptMaskFlag1;
	}

	/////////////////////
	// I2C Interface
	/////////////////////

	public int getI2cShift() {
		if (log.isDebugEnabled()) {
			log.debug(String.format("getI2cShift 0x%02X", i2cShift));
		}
		return i2cShift;
	}

	public void setI2cShift(int i2cShift) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("setI2cShift 0x%02X", i2cShift));
		}
		this.i2cShift = i2cShift;

		// Start condition detected?
		if (hasI2cStatusBit(STD0)) {
			setI2cSlaveAddress(i2cShift);
			clearI2cStatusBit(STD0);
			clearI2cStatusBit(SPD0); // Clear detection of stop condition
			i2cBufferIndex = 0;
			if (dummyTesting && isI2cRead()) {
				i2cBuffer[0] = 0x00;
				i2cBuffer[1] = 0x00;
			}
		} else if (isI2cWrite()) {
			i2cBuffer[i2cBufferIndex++] = i2cShift;
		}

		setI2cStatusBit(ACKD0); // Detection of acknowledge
		setInterruptRequest(IICIF0);
	}

	public int getI2cSlaveAddress() {
		// Bit 0 is fixed to 0
		return clearBit(i2cSlaveAddress, 0);
	}

	public void setI2cSlaveAddress(int i2cSlaveAddress) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("setI2cSlaveAddress 0x%02X", i2cSlaveAddress));
		}
		this.i2cSlaveAddress = i2cSlaveAddress;
	}

	private boolean isI2cRead() {
		return hasBit(i2cSlaveAddress, 0);
	}

	private boolean isI2cWrite() {
		return !isI2cRead();
	}

	public int getI2cControl() {
		return i2cControl;
	}

	public void setI2cControl(int i2cControl) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("setI2cControl 0x%02X(%s)", i2cControl, getSfr1Names(0xFFA6, i2cControl)));
		}

		// Trigger stop condition?
		if (hasBit(i2cControl, SPT0)) {
			clearI2cStatusBit(ACKD0); // Clear detection of acknowledge
			setI2cSlaveAddress(0); // Clear slave address
			setI2cStatusBit(SPD0); // Detection of stop condition
			i2cControl = clearBit(i2cControl, SPT0);
			i2cBufferIndex = 0;
		}

		// Trigger start condition?
		if (hasBit(i2cControl, STT0)) {
			setI2cStatusBit(STD0); // Detection of start condition
			i2cControl = clearBit(i2cControl, STT0);
		}

		// Wait cancellation?
		if (hasBit(i2cControl, WREL0)) {
			if (isI2cRead()) {
				setI2cShift(i2cBuffer[i2cBufferIndex++]);
			}
			i2cControl = clearBit(i2cControl, WREL0);
		}

		this.i2cControl = i2cControl;
	}

	public int getI2cStatus() {
		if (log.isDebugEnabled()) {
			log.debug(String.format("getI2cStatus 0x%02X(%s)", i2cStatus, getSfr1Names(0xFFAA, i2cStatus)));
		}
		return i2cStatus;
	}

	private void setI2cStatusBit(int bit) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("setI2cStatusBit %s", getSfr1Name(0xFFAA, bit)));
		}
		i2cStatus = setBit(i2cStatus, bit);
	}

	private void clearI2cStatusBit(int bit) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("clearI2cStatusBit %s", getSfr1Name(0xFFAA, bit)));
		}
		i2cStatus = clearBit(i2cStatus, bit);
	}

	private boolean hasI2cStatusBit(int bit) {
		boolean result = hasBit(i2cStatus, bit);
		if (log.isDebugEnabled()) {
			log.debug(String.format("hasI2cStatusBit %s returning %b", getSfr1Name(0xFFAA, bit), result));
		}
		return result;
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
	// A/D Converter
	/////////////////////

	private void setAdConverterMode(int value) {
		if (hasBit(value, 0)) {
			log.error(String.format("setAdConverterMode unimplemented comparator operation 0x%02X", value));
		}
		if (hasBit(value, 7)) {
			log.error(String.format("setAdConverterMode unimplemented conversion operation 0x%02X", value));
		}

		adConverterMode = value;
	}

	/////////////////////
	// Serial Interface
	/////////////////////

	private void setAsynchronousSerialInterfaceOperationMode(int value) {
		if (hasBit(value, 5)) {
			log.error(String.format("setAsynchronousSerialInterfaceOperationMode unimplemented reception 0x%02X", value));
		}
		if (hasBit(value, 6)) {
			log.error(String.format("setAsynchronousSerialInterfaceOperationMode unimplemented transmission 0x%02X", value));
		}
		if (hasBit(value, 7)) {
			log.error(String.format("setAsynchronousSerialInterfaceOperationMode unimplemented operation of internal operation clock 0x%02X", value));
		}

		asynchronousSerialInterfaceOperationMode = value;
	}

	private void setSerialOperationMode10(int value) {
		if (hasBit(value, 7)) {
			log.error(String.format("setSerialOperationMode10 unimplemented operation in 3-wire serial I/O mode 0x%02X", value));
		}
		if (hasBit(value, 6)) {
			log.error(String.format("setSerialOperationMode10 unimplemented transmit/receive mode 0x%02X", value));
		}

		// Bit 0 is read-only
		serialOperationMode10 = setBit(value, serialOperationMode10, 0);
	}

	private void setSerialOperationMode11(int value) {
		if (hasBit(value, 7)) {
			log.error(String.format("setSerialOperationMode11 unimplemented operation in 3-wire serial I/O mode 0x%02X", value));
		}
		if (hasBit(value, 6)) {
			log.error(String.format("setSerialOperationMode11 unimplemented transmit/receive mode 0x%02X", value));
		}

		// Bit 0 is read-only
		serialOperationMode11 = setBit(value, serialOperationMode11, 0);
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
			case 0xFF28: value = adConverterMode; break;
			case 0xFF2C: value = getPortMode(12); break;
			case 0xFF2E: value = getPortMode(14); break;
			case 0xFF43: value = timerModeControl51 & 0xF3; break; // Bits 2 and 3 are write-only
			case 0xFF50: value = asynchronousSerialInterfaceOperationMode; break;
			case 0xFF69: value = timerHModeRegister0; break;
			case 0xFF6C: value = timerHModeRegister1; break;
			case 0xFF6F: value = watchTimerOperationMode; break;
			case 0xFF80: value = serialOperationMode10; break;
			case 0xFF81: value = serialClockSelection10; break;
			case 0xFF88: value = serialOperationMode11; break;
			case 0xFFA0: value = internalOscillationMode; break;
			case 0xFFA1: value = mainClockMode; break;
			case 0xFFA2: value = mainOscillationControl; break;
			case 0xFFA3: value = getOscillationStabilizationTimeCounterStatus(); break;
			case 0xFFA5: value = getI2cShift(); break;
			case 0xFFA6: value = getI2cControl(); break;
			case 0xFFA7: value = getI2cSlaveAddress(); break;
			case 0xFFA8: value = getI2cClockSelection(); break;
			case 0xFFA9: value = getI2cFunctionExpansion(); break;
			case 0xFFAA: value = getI2cStatus(); break;
			case 0xFFAB: value = getI2cFlag(); break;
			case 0xFFAC: value = getResetControlFlag(); break;
			case 0xFFE0: value = getByte0(interruptRequestFlag0); break;
			case 0xFFE1: value = getByte1(interruptRequestFlag0); break;
			case 0xFFE2: value = getByte0(interruptRequestFlag1); break;
			case 0xFFE3: value = getByte1(interruptRequestFlag1); break;
			case 0xFFE4: value = getByte0(interruptMaskFlag0); break;
			case 0xFFE5: value = getByte1(interruptMaskFlag0); break;
			case 0xFFE6: value = getByte0(interruptMaskFlag1); break;
			case 0xFFE7: value = getByte1(interruptMaskFlag1); break;
			case 0xFFFB: value = processorClockControl; break;
			default: value = super.read8(address); break;
		}

		return value;
	}

	@Override
	public int read8(int address) {
		int value = internalRead8(address);

		if (log.isTraceEnabled()) {
			if (hasSfr1Name(address)) {
				log.trace(String.format("0x%04X - read8(%s) returning 0x%02X%s", getPc(), getSfr8Name(address), value, getSfr1Names(address, value)));
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
			case 0xFF17: timerCompare50 = value8; break;
			case 0xFF18: timerHCompare00 = value8; break;
			case 0xFF19: timerHCompare10 = value8; break;
			case 0xFF1A: timerHCompare01 = value8; break;
			case 0xFF1B: timerHCompare11 = value8; break;
			case PSW_ADDRESS: processor.setPsw(value8); break;
			case 0xFF20: setPortMode(0, value8); break;
			case 0xFF21: setPortMode(1, value8); break;
			case 0xFF22: setPortMode(2, value8); break;
			case 0xFF23: setPortMode(3, value8); break;
			case 0xFF24: setPortMode(4, value8); break;
			case 0xFF25: setPortMode(5, value8); break;
			case 0xFF26: setPortMode(6, value8); break;
			case 0xFF27: setPortMode(7, value8); break;
			case 0xFF28: setAdConverterMode(value8); break;
			case 0xFF29: analogInputChannelSpecification = value8; break;
			case 0xFF2C: setPortMode(12, value8); break;
			case 0xFF2E: setPortMode(14, value8); break;
			case 0xFF2F: adPortConfiguration = value8; break;
			case 0xFF41: timerCompare51 = value8; break;
			case 0xFF43: setTimerModeControl51(value8); break;
			case 0xFF48: externalInterruptRisingEdgeEnable = value8; break;
			case 0xFF49: externalInterruptFallingEdgeEnable = value8; break;
			case 0xFF50: setAsynchronousSerialInterfaceOperationMode(value8); break;
			case 0xFF56: clockSelection6 = value8; break;
			case 0xFF57: baudRateGeneratorControl6 = value8; break;
			case 0xFF58: asynchronousSerialInterfaceOperationMode = value8; break;
			case 0xFF69: setTimerHModeRegister0(value8); break;
			case 0xFF6A: timerClockSelection50 = value; break;
			case 0xFF6B: setTimerModeControl50(value8); break;
			case 0xFF6C: setTimerHModeRegister1(value8); break;
			case 0xFF6F: setWatchTimerOperationMode(value8); break;
			case 0xFF80: setSerialOperationMode10(value8); break;
			case 0xFF81: serialClockSelection10 = value8; break;
			case 0xFF88: setSerialOperationMode11(value8); break;
			case 0xFF8C: timerClockSelection51 = value8; break;
			case 0xFF99: setWatchdogTimerEnable(value8); break;
			case 0xFF9F: clockOperationModeSelect = value8; break;
			case 0xFFA0: internalOscillationMode = value8; break;
			case 0xFFA1: setMainClockMode(value8); break;
			case 0xFFA2: setMainOscillationControl(value8); break;
			case 0xFFA4: oscillationStabilizationTimeSelect = value8; break;
			case 0xFFA5: setI2cShift(value8); break;
			case 0xFFA6: setI2cControl(value8); break;
			case 0xFFA7: setI2cSlaveAddress(value8); break;
			case 0xFFA8: setI2cClockSelection(value8); break;
			case 0xFFA9: setI2cFunctionExpansion(value8); break;
			case 0xFFAB: setI2cFlag(value8); break;
			case 0xFFBA: setTimerModeControl00(value8); break;
			case 0xFFBB: prescalerMode00 = value8; break;
			case 0xFFBC: compareControl00 = value8; break;
			case 0xFFC0: if (value8 != 0xA5) { super.write8(address, value); } break; // Unknown register, used only on some hardware
			case 0xFFC4: if (value8 != 0x01 && value8 != 0xFE) { super.write8(address, value); } break; // Unknown register, used only on some hardware
			case 0xFFE0: interruptRequestFlag0 = setByte0(interruptRequestFlag0, value8); break;
			case 0xFFE1: interruptRequestFlag0 = setByte1(interruptRequestFlag0, value8); break;
			case 0xFFE2: interruptRequestFlag1 = setByte0(interruptRequestFlag1, value8); break;
			case 0xFFE3: interruptRequestFlag1 = setByte1(interruptRequestFlag1, value8); break;
			case 0xFFE4: setInterruptMaskFlag0(setByte0(interruptMaskFlag0, value8)); break;
			case 0xFFE5: setInterruptMaskFlag0(setByte1(interruptMaskFlag0, value8)); break;
			case 0xFFE6: setInterruptMaskFlag1(setByte0(interruptMaskFlag1, value8)); break;
			case 0xFFE7: setInterruptMaskFlag1(setByte1(interruptMaskFlag1, value8)); break;
			case 0xFFE8: prioritySpecificationFlag0 = setByte0(prioritySpecificationFlag0, value8); break;
			case 0xFFE9: prioritySpecificationFlag0 = setByte1(prioritySpecificationFlag0, value8); break;
			case 0xFFEA: prioritySpecificationFlag1 = setByte0(prioritySpecificationFlag1, value8); break;
			case 0xFFEB: prioritySpecificationFlag1 = setByte1(prioritySpecificationFlag1, value8); break;
			case 0xFFF0: internalMemorySizeSwitching = value8; break;
			case 0xFFF4: internalExpansionRAMSizeSwitching = value8; break;
			case 0xFFFB: setProcessorClockControl(value8); break;
			default: super.write8(address, value); break;
		}
	}

	@Override
	public void write8(int address, byte value) {
		internalWrite8(address, value);

		if (log.isTraceEnabled()) {
			if (hasSfr1Name(address)) {
				log.trace(String.format("0x%04X - write8(%s, 0x%02X%s) on %s", getPc(), getSfr8Name(address), value & 0xFF, getSfr1Names(address, value), this));
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
			case 0xFF12: timerCompare000 = value16; break;
			case SP_ADDRESS: processor.setSp(value16); break;
			case 0xFFE0: interruptRequestFlag0 = value16; break;
			case 0xFFE2: interruptRequestFlag1 = value16; break;
			case 0xFFE4: setInterruptMaskFlag0(value16); break;
			case 0xFFE6: setInterruptMaskFlag1(value16); break;
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
