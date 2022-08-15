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

import static jpcsp.HLE.modules.sceSyscon.getSysconCmdName;
import static jpcsp.memory.mmio.syscon.MMIOHandlerSysconFirmwareSfr.getInterruptName;
import static jpcsp.nec78k0.Nec78k0Memory.BASE_RAM0;
import static jpcsp.nec78k0.Nec78k0Memory.END_RAM0;
import static jpcsp.util.Utilities.readUnaligned32;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

import jpcsp.Emulator;
import jpcsp.Allegrex.compiler.RuntimeContext;
import jpcsp.autotests.AutoTestsRunner;
import jpcsp.hardware.Battery;
import jpcsp.hardware.Model;
import jpcsp.hardware.Wlan;
import jpcsp.memory.mmio.syscon.MMIOHandlerSysconFirmwareSfr;
import jpcsp.memory.mmio.syscon.SysconEmulator;
import jpcsp.nec78k0.Nec78k0Interpreter;
import jpcsp.nec78k0.Nec78k0Memory;
import jpcsp.nec78k0.Nec78k0Processor;
import jpcsp.util.LWJGLFixer;

/**
 * @author gid15
 *
 */
public class Syscon78k0Test {
	public static Logger log;

	public static void main(String[] args) {
        LWJGLFixer.fixOnce();
        DOMConfigurator.configure("LogSettings.xml");
        log = Nec78k0Processor.log;
		RuntimeContext.setLog4jMDC();
		Wlan.initialize();
		Battery.initialize();
        new Emulator(new AutoTestsRunner.DummyGUI());
        Emulator.getClock().resume();
		RuntimeContext.debugCodeBlockCalls = true;
		Emulator.run = true;

		new Syscon78k0Test().testFirmware();
	}

	public void testFirmware() {
		int model = Model.MODEL_PSP_SLIM;
		model = Model.MODEL_PSP_FAT;

		Model.setModel(model);
		String fileName = SysconEmulator.getFirmwareFileName();
		log.debug(String.format("Reading %s", fileName));
		File inputFile = new File(fileName);
		byte[] buffer = new byte[(int) inputFile.length()];
		int length = buffer.length;
		try {
			InputStream is = new FileInputStream(inputFile);
			length = is.read(buffer);
			is.close();
		} catch (IOException e) {
			log.error(e);
		}

		Nec78k0Memory mem = new Nec78k0Memory(log);
		Nec78k0Processor processor = new Nec78k0Processor(mem);
		Nec78k0Interpreter interpreter = new Nec78k0Interpreter(processor);

		int baseAddress = BASE_RAM0;
		length = Math.min(length, END_RAM0);
		for (int i = 0; i < length; i += 4) {
			mem.write32(baseAddress + i, readUnaligned32(buffer, i));
		}

		for (int i = 0; i < 0x40; i += 2) {
			int addr = mem.internalRead16(i);
			if (addr != 0 && addr != 0xFFFF) {
				log.info(String.format("Disassembling Vector Table entry 0x%02X(%s): 0x%04X", i, getInterruptName(i), addr));
				processor.disassemble(addr);
			}
		}

		if (model == Model.MODEL_PSP_SLIM) {
			// The below offsets are taken from the syscon firmware on the TA-085 motherboard
			for (int i = 0x88; i < 0xAE; i += 2) {
				int addr = mem.internalRead16(i);
				if (addr != 0 && addr != 0xFFFF) {
					log.info(String.format("Disassembling sysconCmdGetOps table 0x%02X(%s): 0x%04X", i, getSysconCmdName((i - 0x88) / 2), addr));
					processor.disassemble(addr);
				}
			}
			for (int i = 0xAE; i < 0xDC; i += 2) {
				int addr = mem.internalRead16(i);
				if (addr != 0 && addr != 0xFFFF) {
					log.info(String.format("Disassembling mainOperations table 0x%02X(%s): 0x%04X", i, getSysconCmdName((i - 0xAE) / 2 + 0x20), addr));
					processor.disassemble(addr);
				}
			}
			for (int i = 0xDC; i < 0x10A; i += 2) {
				int addr = mem.internalRead16(i);
				if (addr != 0 && addr != 0xFFFF) {
					log.info(String.format("Disassembling peripheralOperations table 0x%02X(%s): 0x%04X", i, getSysconCmdName((i - 0xDC) / 2 + 0x40), addr));
					processor.disassemble(addr);
				}
			}
			processor.disassemble(0x8100);
			for (int i = 2; i <= 13; i++) {
				int addr = mem.internalRead16(0x4DA8 + (i - 2) * 2);
				log.info(String.format("Disassembling switch table from 0x4DA6: case 0x%02X at 0x%04X", i, addr));
				processor.disassemble(addr);
			}
			for (int i = 1; i <= 20; i++) {
				int addr = mem.internalRead16(0x3264 + (i - 1) * 2);
				log.info(String.format("Disassembling switch table from 0x3262: case 0x%02X at 0x%04X", i, addr));
				processor.disassemble(addr);
			}
		} else if (model == Model.MODEL_PSP_FAT) {
			// The below offsets are taken from the syscon firmware on the TA-086 motherboard
			for (int i = 0x88; i < 0xAE; i += 2) {
				int addr = mem.internalRead16(i);
				if (addr != 0 && addr != 0xFFFF) {
					log.info(String.format("Disassembling sysconCmdGetOps table 0x%02X(%s): 0x%04X", i, getSysconCmdName((i - 0x88) / 2), addr));
					processor.disassemble(addr);
				}
			}
			for (int i = 0xAC; i < 0xDA; i += 2) {
				int addr = mem.internalRead16(i);
				if (addr != 0 && addr != 0xFFFF) {
					log.info(String.format("Disassembling mainOperations table 0x%02X(%s): 0x%04X", i, getSysconCmdName((i - 0xAE) / 2 + 0x20), addr));
					processor.disassemble(addr);
				}
			}
			for (int i = 0xDA; i < 0x108; i += 2) {
				int addr = mem.internalRead16(i);
				if (addr != 0 && addr != 0xFFFF) {
					log.info(String.format("Disassembling peripheralOperations table 0x%02X(%s): 0x%04X", i, getSysconCmdName((i - 0xDC) / 2 + 0x40), addr));
					processor.disassemble(addr);
				}
			}
			processor.disassemble(0x8100);
			for (int i = 1; i <= 12; i++) {
				int addr = mem.internalRead16(0x25C2 + (i - 1) * 2);
				log.info(String.format("Disassembling switch table from 0x25C0: case 0x%02X at 0x%04X", i, addr));
				processor.disassemble(addr);
			}
		}

		SysconEmulator.disable();
		MMIOHandlerSysconFirmwareSfr.dummyTesting = true;
		Nec78k0Processor.disassembleFunctions = true;

		processor.reset();

		interpreter.run();

		long minimumDuration = 3000L; // Run for at least 3 seconds
		long start = now();
		while ((now() - start) < minimumDuration) {
			interpreter.run();
		}
	}

	public static long now() {
		return Emulator.getClock().currentTimeMillis();
	}
}
