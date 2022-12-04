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
package jpcsp.test;

import static jpcsp.HLE.modules.sceSyscon.PSP_SYSCON_CMD_BATTERY_BASE;
import static jpcsp.HLE.modules.sceSyscon.PSP_SYSCON_CMD_BATTERY_CHALLENGE1;
import static jpcsp.HLE.modules.sceSyscon.PSP_SYSCON_CMD_BATTERY_GET_SERIAL;
import static jpcsp.HLE.modules.sceSyscon.PSP_SYSCON_CMD_BATTERY_GET_STATUS_CAP;
import static jpcsp.HLE.modules.sceSyscon.PSP_SYSCON_CMD_BATTERY_GET_TEMP;
import static jpcsp.HLE.modules.sceSyscon.PSP_SYSCON_CMD_BATTERY_GET_VOLT;
import static jpcsp.HLE.modules.sceSyscon.PSP_SYSCON_CMD_BATTERY_NOP;
import static jpcsp.memory.mmio.battery.BatteryEmulator.BATTERY_MODEL_2000;
import static jpcsp.memory.mmio.battery.BatteryEmulator.BATTERY_MODEL_3000;
import static jpcsp.nec78k0.Nec78k0Processor.RESET;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

import jpcsp.Emulator;
import jpcsp.Allegrex.compiler.RuntimeContext;
import jpcsp.autotests.AutoTestsRunner;
import jpcsp.hardware.Battery;
import jpcsp.hardware.Model;
import jpcsp.hardware.Wlan;
import jpcsp.memory.mmio.battery.BatteryEmulator;
import jpcsp.memory.mmio.battery.BatteryMemory;
import jpcsp.memory.mmio.battery.BatterySerialInterface;
import jpcsp.memory.mmio.syscon.SysconBatteryEmulator;
import jpcsp.nec78k0.Nec78k0Disassembler;
import jpcsp.nec78k0.Nec78k0Interpreter;
import jpcsp.nec78k0.Nec78k0Processor;
import jpcsp.util.LWJGLFixer;

/**
 * @author gid15
 *
 */
public class Battery78k0Test {
	public static Logger log;

	public static void main(String[] args) {
        LWJGLFixer.fixOnce();
        DOMConfigurator.configure("LogSettings.xml");
        log = BatteryEmulator.log;
		RuntimeContext.setLog4jMDC();
		Wlan.initialize();
		Battery.initialize();
        new Emulator(new AutoTestsRunner.DummyGUI());
        Emulator.getClock().resume();
		RuntimeContext.debugCodeBlockCalls = true;
		Emulator.run = true;

		new Battery78k0Test().testFirmware();
	}

	public void testFirmware() {
		int model = Model.MODEL_PSP_BRITE;
//		model = Model.MODEL_PSP_SLIM;

		Model.setModel(model);

		BatteryMemory mem = new BatteryMemory(log);
		Nec78k0Processor processor = new Nec78k0Processor(mem);
		Nec78k0Interpreter interpreter = new Nec78k0Interpreter(processor);
		Nec78k0Disassembler disassembler = processor.getDisassembler();
		mem.getSfr().getSerialInterfaceUART6().setSerialInterface(new BatterySerialInterface(mem.getSfr(), mem.getSfr().getSerialInterfaceUART6()));

		BatteryEmulator.load(mem, interpreter);

		int batteryModel = BatteryEmulator.getBatteryModel();

		disassembler.setDataRange(0x4000, 0x7FFF);
		disassembler.setDataRange(0x8000, 0xBFFF);

		if (batteryModel == BATTERY_MODEL_3000) {
			disassembler.setDataRange(0x0640, 0x08FF);
			disassembler.setDataRange(0x0D39, 0x141F);
			disassembler.setDataRange(0x3FE5, 0x3FFF);
		} else if (batteryModel == BATTERY_MODEL_2000) {
			disassembler.setDataRange(0x0623, 0x08FF);
			disassembler.setDataRange(0x0D39, 0x141F);
			disassembler.setDataRange(0x3FAB, 0x3FFF);
		} else {
			log.error(String.format("Unsupported Battery model %d", batteryModel));
		}

		processor.disassemble(mem.read16(RESET));
		for (int addr = 0x0400; addr < 0x4000; addr++) {
			if (!disassembler.disassembledAddresses.contains(addr)) {
				processor.disassemble(addr);
			}
		}

//		BatteryEmulator.writeEeprom16(0x06, 0x0044);
//		BatteryEmulator.writeEeprom16(0x20, 0x0010);

		BatteryEmulator.writeEeprom16(0x00, 0x04E2);
		BatteryEmulator.writeEeprom16(0x02, 0x0F02);
		BatteryEmulator.writeEeprom16(0x04, 0x000A);
		BatteryEmulator.writeEeprom16(0x05, 0x0028);
		BatteryEmulator.writeEeprom16(0x06, 0x0041);
		BatteryEmulator.writeEeprom16(0x07, 0xF057);
		BatteryEmulator.writeEeprom16(0x09, 0x4401);
		BatteryEmulator.writeEeprom16(0x10, 0x0C2E);
		BatteryEmulator.writeEeprom16(0x11, 0x0083);
		BatteryEmulator.writeEeprom16(0x20, 0x0003);
		BatteryEmulator.writeEeprom16(0x21, 0x023D);
		BatteryEmulator.writeEeprom16(0x22, 0x0317);
		BatteryEmulator.writeEeprom16(0x23, 0xFFC9);
		BatteryEmulator.writeEeprom16(0x24, 0x027C);
		BatteryEmulator.writeEeprom16(0x25, 0x027B);
		BatteryEmulator.writeEeprom16(0x30, 0x0474);
		BatteryEmulator.writeEeprom16(0x31, 0x0066);
		BatteryEmulator.writeEeprom16(0x32, 0x00A0);
		BatteryEmulator.writeEeprom16(0x33, 0x0016);
		BatteryEmulator.writeEeprom16(0x34, 0x0020);
		BatteryEmulator.writeEeprom16(0x35, 0x0032);
		BatteryEmulator.writeEeprom16(0x36, 0x0230);
		BatteryEmulator.writeEeprom16(0x37, 0x276A);
		BatteryEmulator.writeEeprom16(0x38, 0x26D7);
		BatteryEmulator.writeEeprom16(0x39, 0x2275);
		BatteryEmulator.writeEeprom16(0x3A, 0x1F4B);
		BatteryEmulator.writeEeprom16(0x3B, 0x9339); // Checksum of the values 0x37-0x3A
		BatteryEmulator.writeEeprom16(0x40, 0x0000);
		BatteryEmulator.writeEeprom16(0x41, 0x0000);
		BatteryEmulator.writeEeprom16(0x79, 0x3952);
		BatteryEmulator.writeEeprom16(0x7A, 0x1467);
		BatteryEmulator.writeEeprom16(0x7B, 0x1474);
		BatteryEmulator.writeEeprom16(0x7C, 0x0001);
		BatteryEmulator.writeEeprom16(0x7D, 0x0002);
		BatteryEmulator.writeEeprom16(0x7E, 0x0003);
		BatteryEmulator.writeEeprom16(0x7F, 0xA9A2);
		processor.reset();
		interpreter.run();

		BatterySerialInterface batterySerialInterface = (BatterySerialInterface) mem.getSfr().getSerialInterfaceUART6().getConnectedSerialInterface();
		if (false) {
			batterySerialInterface.sendRequest(PSP_SYSCON_CMD_BATTERY_GET_SERIAL, null);
		} else if (false) {
			batterySerialInterface.sendRequest(PSP_SYSCON_CMD_BATTERY_GET_STATUS_CAP, null);
		} else if (false) {
			batterySerialInterface.sendRequest(0x76, null);
		} else if (false) {
			batterySerialInterface.sendRequest(PSP_SYSCON_CMD_BATTERY_GET_VOLT, null);
		} else if (false) {
			batterySerialInterface.sendRequest(PSP_SYSCON_CMD_BATTERY_GET_TEMP, null);
		} else if (false) {
			batterySerialInterface.sendRequest(PSP_SYSCON_CMD_BATTERY_NOP, null);
		} else if (true) {
			int keyId = 0x0D;
			int[] cmdParams = new int[] { keyId, 0x46, 0xE5, 0x95, 0x2F, 0xB3, 0xB2, 0x30, 0x5C };
			batterySerialInterface.sendRequest(PSP_SYSCON_CMD_BATTERY_CHALLENGE1, cmdParams);

			SysconBatteryEmulator sysconBatteryEmulator = new SysconBatteryEmulator(null, null);
			sysconBatteryEmulator.setLogger(log);
			sysconBatteryEmulator.transmit(0x5A);
			sysconBatteryEmulator.transmit(cmdParams.length + 2);
			sysconBatteryEmulator.transmit(PSP_SYSCON_CMD_BATTERY_CHALLENGE1 - PSP_SYSCON_CMD_BATTERY_BASE);
			for (int i = 0; i < cmdParams.length; i++) {
				sysconBatteryEmulator.transmit(cmdParams[i]);
			}
			sysconBatteryEmulator.transmitChecksum();
			sysconBatteryEmulator.startReception();
		}

		long minimumDuration = 1000L; // Run for at least 3 seconds
		long start = now();
		while ((now() - start) < minimumDuration) {
			interpreter.setHalted(false);
			interpreter.run();
		}
	}

	public static long now() {
		return Emulator.getClock().currentTimeMillis();
	}
}
