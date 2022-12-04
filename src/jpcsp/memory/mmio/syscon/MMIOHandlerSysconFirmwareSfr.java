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
import static jpcsp.util.Utilities.hasBit;
import static jpcsp.util.Utilities.isFallingBit;
import static jpcsp.util.Utilities.isRaisingBit;

import java.io.IOException;

import jpcsp.State;
import jpcsp.HLE.modules.sceSyscon;
import jpcsp.hardware.Model;
import jpcsp.memory.mmio.MMIOHandlerGpio;
import jpcsp.memory.mmio.battery.BatteryEmulator;
import jpcsp.nec78k0.sfr.Nec78k0Sfr;
import jpcsp.state.StateInputStream;
import jpcsp.state.StateOutputStream;

/**
 * Special Function Registers (SFR) for a NEC 78k0 running as Syscon processor in a PSP.
 * 
 * @author gid15
 *
 */
public class MMIOHandlerSysconFirmwareSfr extends Nec78k0Sfr {
	private static final int STATE_VERSION = 0;
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

	public static boolean dummyTesting = false;
	//
	private int keyPowerStartup;
	private int bootloaderP1_4_switch;

	public MMIOHandlerSysconFirmwareSfr(int baseAddress) {
		super(baseAddress);

		i2c = new SysconI2c(this);
		adConverter = new SysconAdConverter(this, scheduler);

		// When emulating the firmware bootloader,
		// the serial interface is connected to an external system.
		// Otherwise, the serial interface is connected to the PSP battery.
		if (SysconEmulator.firmwareBootloader) {
			serialInterfaceUART6.setSerialInterface(new SysconBootloaderEmulator(this, serialInterfaceUART6));
		} else if (BatteryEmulator.isEnabled()) {
			serialInterfaceUART6.setSerialInterface(null); // We will be set later
		} else {
			serialInterfaceUART6.setSerialInterface(new SysconBatteryEmulator(this, serialInterfaceUART6));
		}

		reset();
	}

	@Override
	public void read(StateInputStream stream) throws IOException {
		stream.readVersion(STATE_VERSION);
		keyPowerStartup = stream.readInt();
		bootloaderP1_4_switch = stream.readInt();
		super.read(stream);
	}

	@Override
	public void write(StateOutputStream stream) throws IOException {
		stream.writeVersion(STATE_VERSION);
		stream.writeInt(keyPowerStartup);
		stream.writeInt(bootloaderP1_4_switch);
		super.write(stream);
	}

	@Override
	public void reset() {
		super.reset();

		// Input P12.0 = 1
		setPortInputBit(12, 0);
		// Input P1.6 = 1
		setPortInputBit(1, 6);

		keyPowerStartup = 0;
		bootloaderP1_4_switch = 0;

		setInterruptRequest(PIF1);
	}

	public void startSysconCmd(int[] data) {
		getSerialInterface().setReceiveBuffer(data);

		// Generate a SYSCON_REQ (INTP2) interrupt request
		setInterruptRequest(PIF2);
	}

	public void endSysconCmd() {
		MMIOHandlerSyscon.getInstance().clearData();
		int data[] = new int[MMIOHandlerSyscon.MAX_DATA_LENGTH];
		int length = getSerialInterface().getSendBuffer(data);
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
						if (hasBit(getPortInput(port), 4)) {
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

	@Override
	protected int getPortValue(int port) {
		updatePortInput(port);

		return super.getPortValue(port);
	}

	@Override
	protected void setPortOutput(int port, int value) {
		int oldValue = getPortOutput(port);
		super.setPortOutput(port, value);

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

	/////////////////////
	// Interrupts
	/////////////////////

	@Override
	protected void setInterruptMaskFlag0(int interruptMaskFlag0) {
		if (dummyTesting) {
			if (hasBit(getSerialInterface().getOperationMode(), 7) && isFallingBit(this.interruptMaskFlag0, interruptMaskFlag0, PIF2)) {
				startSysconCmd(new int[] { sceSyscon.PSP_SYSCON_CMD_GET_BARYON, 2, 0xFC, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 });
//				startSysconCmd(new int[] { sceSyscon.PSP_SYSCON_CMD_GET_TIMESTAMP, 2, 0xEC, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 });
//				startSysconCmd(new int[] { sceSyscon.PSP_SYSCON_CMD_CTRL_VOLTAGE, 5, 0x01, 0x00, 0x07, 0xB0, 0x00, 0x00, 0x00, 0x00 });
			}
		}

		super.setInterruptMaskFlag0(interruptMaskFlag0);
	}

	@Override
	public String toString() {
		return String.format("Syscon SFR %s", debugInterruptRequests());
	}
}
