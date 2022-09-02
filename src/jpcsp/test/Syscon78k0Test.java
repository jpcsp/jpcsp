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
import static jpcsp.util.Utilities.internalReadUnaligned16;

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
import jpcsp.nec78k0.Nec78k0Instructions;
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
		model = Model.MODEL_PSP_STREET;

		Model.setModel(model);

		Nec78k0Memory mem = new Nec78k0Memory(log);
		Nec78k0Processor processor = new Nec78k0Processor(mem);
		Nec78k0Interpreter interpreter = new Nec78k0Interpreter(processor);

		SysconEmulator.load(mem);

		if (model == Model.MODEL_PSP_STREET) {
			// The below offsets and function names are taken from
			//     https://github.com/uofw/uofw/blob/master/src/syscon_firmware/firmware_ta096.c
			Nec78k0Instructions.registerFunctionName(0x4CE0, "do_encrypt");
			Nec78k0Instructions.registerFunctionName(0x4D02, "aes_key_expand");
			Nec78k0Instructions.registerFunctionName(0x5011, "memcpy");
			Nec78k0Instructions.registerFunctionName(0x5039, "memcmp");
			Nec78k0Instructions.registerFunctionName(0x5076, "xorloop_0x10");
			Nec78k0Instructions.registerFunctionName(0x50A7, "memset");
			Nec78k0Instructions.registerFunctionName(0x50C4, "generate_challenge");
			Nec78k0Instructions.registerFunctionName(0x5103, "final_key_encryption_cbc");
			Nec78k0Instructions.registerFunctionName(0x55CF, "read_secure_flash");
		} else if (model == Model.MODEL_PSP_BRITE2) {
			Nec78k0Instructions.registerFunctionName(0x524A, "do_encrypt");
			Nec78k0Instructions.registerFunctionName(0x526C, "aes_key_expand");
			Nec78k0Instructions.registerFunctionName(0x557B, "memcpy");
			Nec78k0Instructions.registerFunctionName(0x55E0, "xorloop_0x10");
			Nec78k0Instructions.registerFunctionName(0x566D, "final_key_encryption_cbc");
			Nec78k0Instructions.registerFunctionName(0x5B42, "read_secure_flash");
		}

		for (int i = 0; i < 0x40; i += 2) {
			int addr = mem.internalRead16(i);
			if (addr != 0 && addr != 0xFFFF) {
				log.info(String.format("Disassembling Vector Table entry 0x%02X(%s): 0x%04X", i, getInterruptName(i), addr));
				processor.disassemble(addr);
			}
		}

		if (mem.internalRead32(0x8100) != 0xFFFFFFFF) {
			processor.disassemble(0x8100);
			for (int i = 0; i < 10; i++) {
				int value = mem.internalRead8(0x9FD2 - i);
				int addr = internalReadUnaligned16(mem, 0x9FD3 + i * 2);
				log.info(String.format("Disassembling switch table from 0x816D: case 0x%02X at 0x%04X", value, addr));
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
			for (int i = 1; i <= 12; i++) {
				int addr = mem.internalRead16(0x25C2 + (i - 1) * 2);
				log.info(String.format("Disassembling switch table from 0x25C0: case 0x%02X at 0x%04X", i, addr));
				processor.disassemble(addr);
			}
		} else if (model == Model.MODEL_PSP_BRITE) {
			// The below offsets are taken from the syscon firmware on the TA-090 motherboard
			for (int i = 2; i <= 13; i++) {
				int addr = internalReadUnaligned16(mem, 0x4DD1 + (i - 2) * 2);
				log.info(String.format("Disassembling switch table from 0x4DCF: case 0x%02X at 0x%04X", i, addr));
				processor.disassemble(addr);
			}
		} else if (model == Model.MODEL_PSP_STREET) {
			// The below offsets are taken from the syscon firmware on the TA-096 motherboard
			for (int i = 0x8A; i < 0xB0; i += 2) {
				int addr = mem.internalRead16(i);
				if (addr != 0 && addr != 0xFFFF) {
					log.info(String.format("Disassembling sysconCmdGetOps table 0x%02X(%s): 0x%04X", i, getSysconCmdName((i - 0x8A) / 2), addr));
					processor.disassemble(addr);
				}
			}
			for (int i = 0xB0; i < 0xDE; i += 2) {
				int addr = mem.internalRead16(i);
				if (addr != 0 && addr != 0xFFFF) {
					log.info(String.format("Disassembling mainOperations table 0x%02X(%s): 0x%04X", i, getSysconCmdName((i - 0xB0) / 2 + 0x20), addr));
					processor.disassemble(addr);
				}
			}
			for (int i = 0xE0; i < 0x10E; i += 2) {
				int addr = mem.internalRead16(i);
				if (addr != 0 && addr != 0xFFFF) {
					log.info(String.format("Disassembling peripheralOperations table 0x%02X(%s): 0x%04X", i, getSysconCmdName((i - 0xE0) / 2 + 0x40), addr));
					processor.disassemble(addr);
				}
			}

			for (int i = 1; i <= 21; i++) {
				int addr = mem.internalRead16(0x277A + (i - 1) * 2);
				log.info(String.format("Disassembling switch table from 0x2778: case 0x%02X at 0x%04X", i, addr));
				processor.disassemble(addr);
			}
			for (int i = 2; i <= 13; i++) {
				int addr = internalReadUnaligned16(mem, 0x51BA + (i - 2) * 2);
				log.info(String.format("Disassembling switch table from 0x51B8: case 0x%02X at 0x%04X", i, addr));
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
