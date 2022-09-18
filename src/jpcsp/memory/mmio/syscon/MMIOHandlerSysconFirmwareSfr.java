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
import static jpcsp.memory.mmio.MMIOHandlerGpio.GPIO_PORT_SYSCON_END_CMD;
import static jpcsp.memory.mmio.syscon.SysconEmulator.firmwareBootloader;
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
import static jpcsp.util.Utilities.isRaisingBit;
import static jpcsp.util.Utilities.setBit;
import static jpcsp.util.Utilities.setByte0;
import static jpcsp.util.Utilities.setByte1;

import java.io.IOException;
import java.util.Arrays;

import org.apache.log4j.Logger;

import jpcsp.Emulator;
import jpcsp.State;
import jpcsp.HLE.modules.sceSyscon;
import jpcsp.hardware.Model;
import jpcsp.memory.mmio.MMIOHandlerGpio;
import jpcsp.nec78k0.Nec78k0MMIOHandlerBase;
import jpcsp.state.StateInputStream;
import jpcsp.state.StateOutputStream;
import jpcsp.util.Utilities;

/**
 * @author gid15
 *
 */
public class MMIOHandlerSysconFirmwareSfr extends Nec78k0MMIOHandlerBase {
	private static final int STATE_VERSION = 0;
	public static final int NUMBER_SPECIAL_FUNCTION_REGISTERS = 256;
	// Interrupt Vector Table addresses
	public static final int INTLVI   = 0x04;
	public static final int INTP0    = 0x06;
	public static final int INTP1    = 0x08; // KEY_POWER
	public static final int INTP2    = 0x0A; // SYSCON_REQ
	public static final int INTP3    = 0x0C; // WLAN_WAKEUP
	public static final int INTP4    = 0x0E; // HPRMC_WAKEUP
	public static final int INTP5    = 0x10; // POMMEL_ALERT
	public static final int INTSRE6  = 0x12;
	public static final int INTSR6   = 0x14;
	public static final int INTST6   = 0x16;
	public static final int INTCSI10 = 0x18; // Serial interface CSI10
	public static final int INTST0   = 0x18;
	public static final int INTTMH1  = 0x1A;
	public static final int INTTMH0  = 0x1C;
	public static final int INTTM50  = 0x1E; // 8-bit timer H0
	public static final int INTTM000 = 0x20;
	public static final int INTTM010 = 0x22;
	public static final int INTAD    = 0x24; // A/D Converter
	public static final int INTSR0   = 0x26;
	public static final int INTWTI   = 0x28; // Watch timer - Interval timer operation 
	public static final int INTTM51  = 0x2A; // 8-bit timer H1
	public static final int INTKR    = 0x2C;
	public static final int INTWT    = 0x2E; // Watch timer - Watch timer operation
	public static final int INTP6    = 0x30;
	public static final int INTP7    = 0x32;
	public static final int INTIIC0  = 0x34; // Serial interface IIC0 (I2C bus mode)
	public static final int INTDMU   = 0x34;
	public static final int INTCSI11 = 0x36; // Serial interface CSI11
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
	public static final int SRIF6   = INTtoIF(INTSR6);
	public static final int STIF6   = INTtoIF(INTST6);
	public static final int TMIF50  = INTtoIF(INTTM50);
	public static final int TMIF000 = INTtoIF(INTTM000);
	public static final int TMIF010 = INTtoIF(INTTM010);
	public static final int ADIF    = INTtoIF(INTAD);
	public static final int TMIF51  = INTtoIF(INTTM51);
	public static final int CSIIF10 = INTtoIF(INTCSI10);
	public static final int CSIIF11 = INTtoIF(INTCSI11);
	public static final int TMIF001 = INTtoIF(INTTM001);
	public static final int TMIF011 = INTtoIF(INTTM011);
	public static final int NUMBER_INTERRUPT_FLAGS = 28;
	//
	public static boolean dummyTesting = false;
	//
	private final SysconScheduler scheduler;
	private final SysconWatchTimer watchTimer;
	private static final int NUMBER_PORTS = 15;
	private final int[] portOutputs = new int[NUMBER_PORTS];
	private final int[] portInputs = new int[NUMBER_PORTS];
	private final int[] portModes = new int[NUMBER_PORTS];
	private final int[] pullUpResistorOptions = new int[NUMBER_PORTS];
	//
	// Ports for TA-085 only (78K0/KF2, PSP Slim):
	//
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

	//
	// Ports for TA-086, TA-090, TA-091, TA-093, TA-095, TA-096 (78K0/KE2, PSP Fat, Brite and Street):
	//
	// P0.0      TTP1 (Output)
	// P0.1      Unused
	// P0.2      HPPWR_ON (Output)
	// P0.3      UMD_DETECT (Input)
	// P0.4      TACHYON_CS (Output)
	// P0.5      STANDBY_HOLD (Output)
	// P0.6      POMMEL_CS (Output)
	// P1.0      CPU_SPI1SCK (Output)
	// P1.1      CPU_SPI1SO (Input)
	// P1.2      CPU_SPI1SI (Output)
	// P1.3      Transmit to Battery
	// P1.4      Receive from Battery
	// P1.5      HP_DETECT (Input)
	// P1.6      INTP5, POMMEL_ALERT (Input)
	// P1.7      WL_POR (Output)
	// P2.0/ANI0 KEY_HOME (Input)
	// P2.1/ANI1 KEY_HOLD (Input)
	// P2.2/ANI2 Unused
	// P2.3/ANI3 STANDBY_SIGNAL (Output)
	// P2.4/ANI4 BUFFER_EN (Output)
	// P2.5/ANI5 Unused
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
	// P4.4      Missing
	// P4.5      Missing
	// P4.6      Missing
	// P4.7      Missing
	// P5.0      KEY_VOLUP (Input)
	// P5.1      KEY_VOLDOWN (Input)
	// P5.2      KEY_DISPLAY (Input)
	// P5.3      KEY_SOUND (Input)
	// P5.4      Missing
	// P5.5      Missing
	// P5.6      Missing
	// P5.7      Missing
	// P6.0      I2C_SCL (Output)
	// P6.1      I2C_SDA (Output)
	// P6.2      CPU_RESET (Output)
	// P6.3      LEPTON_RST (Output)
	// P6.4      Missing
	// P6.5      Missing
	// P6.6      Missing
	// P6.7      Missing
	// P7.0      KEY_UP (Input)
	// P7.1      KEY_RIGHT (Input)
	// P7.2      KEY_DOWN (Input)
	// P7.3      KEY_LEFT (Input)
	// P7.4      KEY_TRIANGLE (Input)
	// P7.5      KEY_CIRCLE (Input)
	// P7.6      KEY_CROSS (Input)
	// P7.7      KEY_SQUARE (Input)
	// P12.0     INTP0, PF_DET (Input)
	// P12.1     Clock 4 MHz
	// P12.2     Clock 4 MHz
	// P12.3     Clock 32.768 kHz
	// P12.4     Clock 32.768 kHz
	// P13.0     GLUON_RST (Output)
	// P14.0     INTP6, UMD_SW (Input)
	// P14.1     INTP7, WLAN SWITCH?
	// P14.2     Missing
	// P14.3     Missing
	// P14.4     Missing
	// P14.5     Missing

	//
	// Port differences between TA-085 (78K0/KF2) and TA-086 (78K0/KE2):
	//
	// TTP1           TA-085: P6.7,         TA-086: P0.0
	// HPPWR_ON       TA-085: Not existing, TA-086: P0.2
	// UMD_DETECT     TA-085: P5.7,         TA-086: P0.3
	// TACHYON_CS     TA-085: P4.6,         TA-086: P0.4
	// POMMEL_CS      TA-085: P6.2,         TA-086: P0.6
	// CPU_SPI1SCK    TA-085: P0.4,         TA-086: P1.0
	// CPU_SPI1SO     TA-085: P0.3,         TA-086: P1.1
	// CPU_SPI1SI     TA-085: P0.2,         TA-086: P1.2
	// KEY_HOME       TA-085: P4.4,         TA-086: P2.0/ANI0
	// KEY_HOLD       TA-085: P4.5,         TA-086: P2.1/ANI1
	// STANDBY_SIGNAL TA-085: Not existing, TA-086: P2.3/ANI3
	// BUFFER_EN      TA-085: Not existing, TA-086: P2.4/ANI4
	// USB_CE         TA-085: P5.5,         TA-086: Not existing
	// CPU_RESET      TA-085: P13.0,        TA-086: P6.2
	// PF_DET         TA-085: Not existing, TA-086: P12.0
	// GLUON_RST      TA-085: P6.4,         TA-086: P13.0
	// UMD_SW         TA-085: P0.6,         TA-086: P14.0
	// WLAN SWITCH?   TA-085: P6.4,         TA-086: P14.1

	private int interruptRequestFlag0; // 16-bit value
	private int interruptRequestFlag1; // 16-bit value
	private int interruptMaskFlag0; // 16-bit value
	private int interruptMaskFlag1; // 16-bit value
	private int prioritySpecificationFlag0; // 16-bit value
	private int prioritySpecificationFlag1; // 16-bit value
	private final SysconI2c i2c;
	private final SysconAdConverter adConverter;
	private final SysconTimerEventCounter16 timer00;
	private final SysconTimerEventCounter16 timer01;
	private final SysconTimerEventCounter8 timer50;
	private final SysconTimerEventCounter8 timer51;
	private int timerHCompare00;
	private int timerHCompare10;
	private int timerHCompare01;
	private int timerHCompare11;
	private int timerHModeRegister0;
	private int timerHModeRegister1;
	private final SysconSerialInterfaceCSI1n serialInterfaceCSI10;
	private final SysconSerialInterfaceCSI1n serialInterfaceCSI11;
	private final SysconSerialInterfaceUART6 serialInterfaceUART6;
	private int internalOscillationMode;
	private int mainClockMode;
	private int mainOscillationControl;
	private int oscillationStabilizationTimeSelect;
	private int processorClockControl;
	private int oscillationStabilizationTimeCounterStatus;
	private int externalInterruptRisingEdgeEnable;
	private int externalInterruptFallingEdgeEnable;
	private int clockOperationModeSelect;
	private int internalMemorySizeSwitching;
	private int internalExpansionRAMSizeSwitching;
	private int keyPowerStartup;
	private final SysconSecureFlash secureFlash;
	private int bootloaderP1_4_switch;

	public MMIOHandlerSysconFirmwareSfr(int baseAddress) {
		super(baseAddress);

		scheduler = new SysconScheduler();

		watchTimer = new SysconWatchTimer(this, scheduler);
		i2c = new SysconI2c(this);
		adConverter = new SysconAdConverter(this, scheduler);
		timer00 = new SysconTimerEventCounter16(this, scheduler, "Timer00", TMIF000, TMIF010);
		timer01 = new SysconTimerEventCounter16(this, scheduler, "Timer01", TMIF001, TMIF011);
		timer50 = new SysconTimerEventCounter8(this, scheduler, "Timer50", TMIF50);
		timer51 = new SysconTimerEventCounter8(this, scheduler, "Timer51", TMIF51);
		serialInterfaceCSI10 = new SysconSerialInterfaceCSI1n(this, "CSI10", CSIIF10);
		serialInterfaceCSI11 = new SysconSerialInterfaceCSI1n(this, "CSI11", CSIIF11);
		serialInterfaceUART6 = new SysconSerialInterfaceUART6(this);
		secureFlash = new SysconSecureFlash(this);

		reset();

		scheduler.setName("Syscon Scheduler");
		scheduler.setDaemon(true);
		scheduler.start();
	}

	private boolean isKE2() {
		return !isKF2();
	}

	private boolean isKF2() {
		return Model.getModel() == Model.MODEL_PSP_SLIM || Model.getModel() == Model.MODEL_PSP_GO;
	}

	private SysconSerialInterfaceCSI1n getSysconSerialInterface() {
		return isKF2() ? serialInterfaceCSI11 : serialInterfaceCSI10;
	}

	public SysconSecureFlash getSysconSecureFlash() {
		return secureFlash;
	}

	public SysconSerialInterfaceUART6 getSysconSerialInterfaceUART6() {
		return serialInterfaceUART6;
	}

	public static int INTtoIF(int vectorTableAddress) {
		return (vectorTableAddress >> 1) - 2;
	}

	public static int IFtoINT(int interruptBit) {
		return (interruptBit + 2) << 1;
	}

	@Override
	public void setLogger(Logger log) {
		scheduler.setLogger(log);
		watchTimer.setLogger(log);

		super.setLogger(log);
	}

	@Override
	public void read(StateInputStream stream) throws IOException {
		stream.readVersion(STATE_VERSION);
		stream.readInts(portInputs);
		stream.readInts(portOutputs);
		stream.readInts(portModes);
		stream.readInts(pullUpResistorOptions);
		interruptRequestFlag0 = stream.readInt();
		interruptRequestFlag1 = stream.readInt();
		interruptMaskFlag0 = stream.readInt();
		interruptMaskFlag1 = stream.readInt();
		prioritySpecificationFlag0 = stream.readInt();
		prioritySpecificationFlag1 = stream.readInt();
		i2c.read(stream);
		adConverter.read(stream);
		timerHCompare00 = stream.readInt();
		timerHCompare10 = stream.readInt();
		timerHCompare01 = stream.readInt();
		timerHCompare11 = stream.readInt();
		timer00.read(stream);
		timer01.read(stream);
		timer50.read(stream);
		timer51.read(stream);
		watchTimer.read(stream);
		timerHModeRegister0 = stream.readInt();
		timerHModeRegister1 = stream.readInt();
		serialInterfaceCSI10.read(stream);
		serialInterfaceCSI11.read(stream);
		serialInterfaceUART6.read(stream);
		internalOscillationMode = stream.readInt();
		mainClockMode = stream.readInt();
		mainOscillationControl = stream.readInt();
		oscillationStabilizationTimeSelect = stream.readInt();
		processorClockControl = stream.readInt();
		oscillationStabilizationTimeCounterStatus = stream.readInt();
		externalInterruptRisingEdgeEnable = stream.readInt();
		externalInterruptFallingEdgeEnable = stream.readInt();
		clockOperationModeSelect = stream.readInt();
		internalMemorySizeSwitching = stream.readInt();
		internalExpansionRAMSizeSwitching = stream.readInt();
		secureFlash.read(stream);
		bootloaderP1_4_switch = stream.readInt();
		super.read(stream);
	}

	@Override
	public void write(StateOutputStream stream) throws IOException {
		stream.writeVersion(STATE_VERSION);
		stream.writeInts(portInputs);
		stream.writeInts(portOutputs);
		stream.writeInts(portModes);
		stream.writeInts(pullUpResistorOptions);
		stream.writeInt(interruptRequestFlag0);
		stream.writeInt(interruptRequestFlag1);
		stream.writeInt(interruptMaskFlag0);
		stream.writeInt(interruptMaskFlag1);
		stream.writeInt(prioritySpecificationFlag0);
		stream.writeInt(prioritySpecificationFlag1);
		i2c.write(stream);
		adConverter.write(stream);
		stream.writeInt(timerHCompare00);
		stream.writeInt(timerHCompare10);
		stream.writeInt(timerHCompare01);
		stream.writeInt(timerHCompare11);
		timer00.write(stream);
		timer01.write(stream);
		timer50.write(stream);
		timer51.write(stream);
		watchTimer.write(stream);
		stream.writeInt(timerHModeRegister0);
		stream.writeInt(timerHModeRegister1);
		serialInterfaceCSI10.write(stream);
		serialInterfaceCSI11.write(stream);
		serialInterfaceUART6.write(stream);
		stream.writeInt(internalOscillationMode);
		stream.writeInt(mainClockMode);
		stream.writeInt(mainOscillationControl);
		stream.writeInt(oscillationStabilizationTimeSelect);
		stream.writeInt(processorClockControl);
		stream.writeInt(oscillationStabilizationTimeCounterStatus);
		stream.writeInt(externalInterruptRisingEdgeEnable);
		stream.writeInt(externalInterruptFallingEdgeEnable);
		stream.writeInt(clockOperationModeSelect);
		stream.writeInt(internalMemorySizeSwitching);
		stream.writeInt(internalExpansionRAMSizeSwitching);
		secureFlash.write(stream);
		stream.writeInt(bootloaderP1_4_switch);
		super.write(stream);
	}

	@Override
	public void reset() {
		Arrays.fill(portOutputs, 0x00);
		Arrays.fill(portInputs, 0x00);
		Arrays.fill(portModes, 0xFF);
		Arrays.fill(pullUpResistorOptions, 0x00);
		interruptRequestFlag0 = 0x0000;
		interruptRequestFlag1 = 0x0000;
		interruptMaskFlag0 = 0xFFFF;
		interruptMaskFlag1 = 0xFFFF;
		prioritySpecificationFlag0 = 0xFFFF;
		prioritySpecificationFlag1 = 0xFFFF;
		i2c.reset();
		watchTimer.reset();
		adConverter.reset();
		timer00.reset();
		timer01.reset();
		timer50.reset();
		timer51.reset();
		watchTimer.reset();
		timerHModeRegister0 = 0x00;
		timerHModeRegister1 = 0x00;
		serialInterfaceCSI10.reset();
		serialInterfaceCSI11.reset();
		serialInterfaceUART6.reset();
		internalOscillationMode = 0x80;
		mainClockMode = 0x00;
		mainOscillationControl = 0x80;
		processorClockControl = 0x01;
		secureFlash.reset();

		// Input P12.0 = 1
		setPortInputBit(12, 0);
		// Input P1.6 = 1
		setPortInputBit(1, 6);

		keyPowerStartup = 0;
		bootloaderP1_4_switch = 0;

		setInterruptRequest(PIF1);
	}

	public static long now() {
		return Emulator.getClock().microTime();
	}

	public void startSysconCmd(int[] data) {
		getSysconSerialInterface().setReceiveBuffer(data);

		// Generate a SYSCON_REQ (INTP2) interrupt request
		setInterruptRequest(PIF2);
	}

	public void endSysconCmd() {
		MMIOHandlerSyscon.getInstance().clearData();
		int data[] = new int[MMIOHandlerSyscon.MAX_DATA_LENGTH];
		int length = getSysconSerialInterface().getSendBuffer(data);
		for (int i = 0; i < length; i++) {
			MMIOHandlerSyscon.getInstance().setDataValue(i, data[i]);
		}
		if (log.isDebugEnabled()) {
			StringBuilder s = new StringBuilder();
			for (int i = 0; i < length; i++) {
				if (i > 0) {
					s.append(", ");
				}
				s.append(String.format("0x%02X", data[i]));
			}
			log.debug(String.format("End of syscon cmd: %s", s));
		}
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
			case 2:
				if (isKE2()) {
					setButtonPortInput(2, 0, PSP_CTRL_HOME, buttons);
					setButtonPortInput(2, 1, PSP_CTRL_HOLD, buttons);
				}
				break;
			case 3:
				final int keyPowerStartupCount = Model.getModel() == Model.MODEL_PSP_FAT ? 10 : 20;
				if (keyPowerStartup > keyPowerStartupCount) {
					setButtonPortInput(3, 0, PSP_CTRL_HOLD, buttons);
				} else {
					// When booting, the KEY_POWER is pressed,
					// simulate the release of this key after a short time
					if (keyPowerStartup == 10) {
						// Input P3.0 = 1: KEY_POWER is released
						setPortInputBit(3, 0);
					} else {
						// Input P3.0 = 0: KEY_POWER is pressed
						clearPortInputBit(3, 0);
					}
					keyPowerStartup++;
				}
				break;
			case 4:
				setButtonPortInput(4, 0, PSP_CTRL_SELECT, buttons);
				setButtonPortInput(4, 1, PSP_CTRL_LTRIGGER, buttons);
				setButtonPortInput(4, 2, PSP_CTRL_RTRIGGER, buttons);
				setButtonPortInput(4, 3, PSP_CTRL_START, buttons);
				if (isKF2()) {
					setButtonPortInput(4, 4, PSP_CTRL_HOME, buttons);
					setButtonPortInput(4, 5, PSP_CTRL_HOLD, buttons);
				}
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
			case 1:
				// The bootloader firmware requires that P1.4 is alternating its value,
				// but each value should stay unchanged during at least 2 read operations.
				// E.g., it should read: 0, 0, 1, 1, 0, 0, 1, 1...
				if (firmwareBootloader) {
					if (bootloaderP1_4_switch > 0) {
						// Alternate P1.4 value
						if (hasBit(portInputs[port], 4)) {
							clearPortInputBit(port, 4);
						} else {
							setPortInputBit(port, 4);
						}
						bootloaderP1_4_switch = 0;
					} else {
						bootloaderP1_4_switch++;
					}
				}
				break;
			case 2:
			case 3:
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
		int oldValue = portOutputs[port];
		portOutputs[port] = value;

		// TACHYON_CS (Output) - is connected to the main processor on GPIO 4 with inverted logic
		final int tachyonPort = isKF2() ? 4 : 0;
		final int tachyonBit = isKF2() ? 6 : 4;
		if (port == tachyonPort) {
			if (isRaisingBit(oldValue, value, tachyonBit)) {
				MMIOHandlerGpio.getInstance().clearPort(GPIO_PORT_SYSCON_END_CMD);
			} else if (isFallingBit(oldValue, value, tachyonBit)) {
				// End of syscon command
				endSysconCmd();
				MMIOHandlerGpio.getInstance().setPort(GPIO_PORT_SYSCON_END_CMD);
			}
		}
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
			if (log.isTraceEnabled()) {
				log.trace(String.format("Start Watch Dog Timer"));
			}
		}
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
			log.debug(String.format("setInterruptRequest bit=0x%X(%s), interrupts %s, %s", bit, getInterruptName(IFtoINT(bit)), processor == null ? "" : processor.isInterruptEnabled() ? "enabled" : "disabled", debugInterruptRequests()));
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
		if (dummyTesting) {
			if (hasBit(getSysconSerialInterface().getOperationMode(), 7) && isFallingBit(this.interruptMaskFlag0, interruptMaskFlag0, PIF2)) {
				startSysconCmd(new int[] { sceSyscon.PSP_SYSCON_CMD_GET_BARYON, 2, 0xFC, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 });
//				startSysconCmd(new int[] { sceSyscon.PSP_SYSCON_CMD_GET_TIMESTAMP, 2, 0xEC, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 });
//				startSysconCmd(new int[] { sceSyscon.PSP_SYSCON_CMD_CTRL_VOLTAGE, 5, 0x01, 0x00, 0x07, 0xB0, 0x00, 0x00, 0x00, 0x00 });
			}
		}

		this.interruptMaskFlag0 = interruptMaskFlag0;
	}

	private void setInterruptMaskFlag1(int interruptMaskFlag1) {
		this.interruptMaskFlag1 = interruptMaskFlag1;
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
			case 0xFF08: value = getByte0(adConverter.getResult()); break;
			case 0xFF09: value = getByte1(adConverter.getResult()); break;
			case 0xFF0A: value = serialInterfaceUART6.getReceiveRegister(); break;
			case 0xFF0C: value = getPortValue(12); break;
			case 0xFF0D: value = getPortValue(13); break;
			case 0xFF0E: value = getPortValue(14); break;
			case 0xFF0F: value = serialInterfaceCSI10.getIOShift(); break;
			case 0xFF10: value = getByte0(timer00.getTimerCounter()); break;
			case 0xFF11: value = getByte1(timer00.getTimerCounter()); break;
			case 0xFF16: value = timer50.getTimerCounter(); break;
			case PSW_ADDRESS: value = processor.getPsw(); break;
			case 0xFF1F: value = timer51.getTimerCounter(); break;
			case 0xFF20: value = getPortMode(0); break;
			case 0xFF21: value = getPortMode(1); break;
			case 0xFF22: value = getPortMode(2); break;
			case 0xFF23: value = getPortMode(3); break;
			case 0xFF24: value = getPortMode(4); break;
			case 0xFF25: value = getPortMode(5); break;
			case 0xFF26: value = getPortMode(6); break;
			case 0xFF27: value = getPortMode(7); break;
			case 0xFF28: value = adConverter.getMode(); break;
			case 0xFF2C: value = getPortMode(12); break;
			case 0xFF2E: value = getPortMode(14); break;
			case 0xFF30: value = pullUpResistorOptions[0]; break;
			case 0xFF31: value = pullUpResistorOptions[1]; break;
			case 0xFF33: value = pullUpResistorOptions[3]; break;
			case 0xFF34: value = pullUpResistorOptions[4]; break;
			case 0xFF35: value = pullUpResistorOptions[5]; break;
			case 0xFF36: value = pullUpResistorOptions[6]; break;
			case 0xFF37: value = pullUpResistorOptions[7]; break;
			case 0xFF3C: value = pullUpResistorOptions[12]; break;
			case 0xFF3E: value = pullUpResistorOptions[14]; break;
			case 0xFF43: value = timer51.getTimerModeControl(); break;
			case 0xFF4A: value = serialInterfaceCSI11.getIOShift(); break;
			case 0xFF4C: value = serialInterfaceCSI11.getTransmitBuffer(); break;
			case 0xFF50: value = serialInterfaceUART6.getOperationMode(); break;
			case 0xFF53: value = serialInterfaceUART6.getReceptionErrorStatus(); break;
			case 0xFF55: value = serialInterfaceUART6.getTransmissionStatus(); break;
			case 0xFF56: value = serialInterfaceUART6.getClockSelection(); break;
			case 0xFF57: value = serialInterfaceUART6.getBaudRateGeneratorControl(); break;
			case 0xFF58: value = serialInterfaceUART6.getControlRegister(); break;
			case 0xFF69: value = timerHModeRegister0; break;
			case 0xFF6B: value = timer50.getTimerModeControl(); break;
			case 0xFF6C: value = timerHModeRegister1; break;
			case 0xFF6F: value = watchTimer.getOperationMode(); break;
			case 0xFF80: value = serialInterfaceCSI10.getOperationMode(); break;
			case 0xFF81: value = serialInterfaceCSI10.getClockSelection(); break;
			case 0xFF84: value = serialInterfaceCSI10.getTransmitBuffer(); break;
			case 0xFF88: value = serialInterfaceCSI11.getOperationMode(); break;
			case 0xFF89: value = serialInterfaceCSI11.getClockSelection(); break;
			case 0xFF9F: value = clockOperationModeSelect; break;
			case 0xFFA0: value = internalOscillationMode; break;
			case 0xFFA1: value = mainClockMode; break;
			case 0xFFA2: value = mainOscillationControl; break;
			case 0xFFA3: value = getOscillationStabilizationTimeCounterStatus(); break;
			case 0xFFA5: value = i2c.getShift(); break;
			case 0xFFA6: value = i2c.getControl(); break;
			case 0xFFA7: value = i2c.getSlaveAddress(); break;
			case 0xFFA8: value = i2c.getClockSelection(); break;
			case 0xFFA9: value = i2c.getFunctionExpansion(); break;
			case 0xFFAA: value = i2c.getStatus(); break;
			case 0xFFAB: value = i2c.getFlag(); break;
			case 0xFFAC: value = getResetControlFlag(); break;
			case 0xFFB0: value = getByte0(timer01.getTimerCounter()); break;
			case 0xFFB1: value = getByte1(timer01.getTimerCounter()); break;
			case 0xFFB6: value = timer01.getTimerModeControl(); break;
			case 0xFFBA: value = timer00.getTimerModeControl(); break;
			case 0xFFC1: value = secureFlash.getUnknown1(); break;
			case 0xFFC4: value = secureFlash.getUnknown4(); break;
			case 0xFFC5: value = secureFlash.getUnknown5(); break;
			case 0xFFC6: value = secureFlash.getUnknown6(); break;
			case 0xFFC7: value = secureFlash.getUnknown7(); break;
			case 0xFFC8: value = secureFlash.getUnknown8(); break;
			case 0xFFCA: value = secureFlash.getFlashProgrammingModeControl(); break;
			case 0xFFE0: value = getByte0(interruptRequestFlag0); break;
			case 0xFFE1: value = getByte1(interruptRequestFlag0); break;
			case 0xFFE2: value = getByte0(interruptRequestFlag1); break;
			case 0xFFE3: value = getByte1(interruptRequestFlag1); break;
			case 0xFFE4: value = getByte0(interruptMaskFlag0); break;
			case 0xFFE5: value = getByte1(interruptMaskFlag0); break;
			case 0xFFE6: value = getByte0(interruptMaskFlag1); break;
			case 0xFFE7: value = getByte1(interruptMaskFlag1); break;
			case 0xFFF5: value = 0x10; break; // Unknown register, used only on some hardware
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
			case 0xFF08: value = adConverter.getResult(); break;
			case 0xFF10: value = timer00.getTimerCounter(); break;
			case SP_ADDRESS: value = processor.getSp(); break;
			case 0xFFB0: value = timer01.getTimerCounter(); break;
			case 0xFFE0: value = interruptRequestFlag0; break;
			case 0xFFE2: value = interruptRequestFlag1; break;
			case 0xFFE4: value = interruptMaskFlag0; break;
			case 0xFFE6: value = interruptMaskFlag1; break;
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
			case 0xFF0B: serialInterfaceUART6.setTransmitRegister(value8); break;
			case 0xFF0C: setPortOutput(12, value8); break;
			case 0xFF0D: setPortOutput(13, value8); break;
			case 0xFF0E: setPortOutput(14, value8); break;
			case 0xFF17: timer50.setCompare0(value8); break;
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
			case 0xFF28: adConverter.setMode(value8); break;
			case 0xFF29: adConverter.setAnalogInputChannelSpecification(value8); break;
			case 0xFF2C: setPortMode(12, value8); break;
			case 0xFF2E: setPortMode(14, value8); break;
			case 0xFF2F: adConverter.setPortConfiguration(value8); break;
			case 0xFF30: pullUpResistorOptions[0] = value8; break;
			case 0xFF31: pullUpResistorOptions[1] = value8; break;
			case 0xFF33: pullUpResistorOptions[3] = value8; break;
			case 0xFF34: pullUpResistorOptions[4] = value8; break;
			case 0xFF35: pullUpResistorOptions[5] = value8; break;
			case 0xFF36: pullUpResistorOptions[6] = value8; break;
			case 0xFF37: pullUpResistorOptions[7] = value8; break;
			case 0xFF3C: pullUpResistorOptions[12] = value8; break;
			case 0xFF3E: pullUpResistorOptions[14] = value8; break;
			case 0xFF41: timer51.setCompare0(value8); break;
			case 0xFF43: timer51.setTimerModeControl(value8); break;
			case 0xFF48: externalInterruptRisingEdgeEnable = value8; break;
			case 0xFF49: externalInterruptFallingEdgeEnable = value8; break;
			case 0xFF4C: serialInterfaceCSI11.setTransmitBuffer(value8); break;
			case 0xFF50: serialInterfaceUART6.setOperationMode(value8); break;
			case 0xFF56: serialInterfaceUART6.setClockSelection(value8); break;
			case 0xFF57: serialInterfaceUART6.setBaudRateGeneratorControl(value8); break;
			case 0xFF58: serialInterfaceUART6.setControlRegister(value8); break;
			case 0xFF69: setTimerHModeRegister0(value8); break;
			case 0xFF6A: timer50.setClockSelection(value8); break;
			case 0xFF6B: timer50.setTimerModeControl(value8); break;
			case 0xFF6C: setTimerHModeRegister1(value8); break;
			case 0xFF6F: watchTimer.setOperationMode(value8); break;
			case 0xFF80: serialInterfaceCSI10.setOperationMode(value8); break;
			case 0xFF81: serialInterfaceCSI10.setClockSelection(value8); break;
			case 0xFF84: serialInterfaceCSI10.setTransmitBuffer(value8); break;
			case 0xFF88: serialInterfaceCSI11.setOperationMode(value8); break;
			case 0xFF89: serialInterfaceCSI11.setClockSelection(value8); break;
			case 0xFF8C: timer51.setClockSelection(value8); break;
			case 0xFF99: setWatchdogTimerEnable(value8); break;
			case 0xFF9F: clockOperationModeSelect = value8; break;
			case 0xFFA0: internalOscillationMode = value8; break;
			case 0xFFA1: setMainClockMode(value8); break;
			case 0xFFA2: setMainOscillationControl(value8); break;
			case 0xFFA4: oscillationStabilizationTimeSelect = value8; break;
			case 0xFFA5: i2c.setShift(value8); break;
			case 0xFFA6: i2c.setControl(value8); break;
			case 0xFFA7: i2c.setSlaveAddress(value8); break;
			case 0xFFA8: i2c.setClockSelection(value8); break;
			case 0xFFA9: i2c.setFunctionExpansion(value8); break;
			case 0xFFAB: i2c.setFlag(value8); break;
			case 0xFFB6: timer01.setTimerModeControl(value8); break;
			case 0xFFB7: timer01.setPrescalerMode(value8); break;
			case 0xFFB8: timer01.setCompareControl(value8); break;
			case 0xFFB9: timer01.setOutputControl(value8); break;
			case 0xFFBA: timer00.setTimerModeControl(value8); break;
			case 0xFFBB: timer00.setPrescalerMode(value8); break;
			case 0xFFBC: timer00.setCompareControl(value8); break;
			case 0xFFBD: timer00.setOutputControl(value8); break;
			case 0xFFC0: secureFlash.setFlashProtectCommandRegister(value8); break;
			case 0xFFC1: secureFlash.setUnknown1(value8); break;
			case 0xFFC4: secureFlash.setUnknown4(value8); break;
			case 0xFFC5: secureFlash.setUnknown5(value8); break;
			case 0xFFC6: secureFlash.setUnknown6(value8); break;
			case 0xFFC7: secureFlash.setUnknown7(value8); break;
			case 0xFFC8: secureFlash.setAddress(setByte0(secureFlash.getAddress(), value8)); break;
			case 0xFFC9: secureFlash.setAddress(setByte1(secureFlash.getAddress(), value8)); break;
			case 0xFFCA: secureFlash.setFlashProgrammingModeControlRegister(value8); break;
			case 0xFFCB: secureFlash.setUnknownB(value8); break;
			case 0xFFCC: secureFlash.setWriteData0(value8); break;
			case 0xFFCD: secureFlash.setWriteData1(value8); break;
			case 0xFFCE: secureFlash.setWriteData2(value8); break;
			case 0xFFCF: secureFlash.setWriteData3(value8); break;
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
			case 0xFF12: timer00.setCompare00(value16); break;
			case 0xFF14: timer00.setCompare01(value16); break;
			case SP_ADDRESS: processor.setSp(value16); break;
			case 0xFFB2: timer01.setCompare00(value16); break;
			case 0xFFB4: timer01.setCompare01(value16); break;
			case 0xFFC8: secureFlash.setAddress(value16); break;
			case 0xFFCC: secureFlash.setWriteData2(value16 & 0xFF); secureFlash.setWriteData3(value16 >> 8); break;
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
