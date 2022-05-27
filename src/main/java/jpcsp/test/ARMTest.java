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

import static jpcsp.Emulator.EMU_STATUS_MEM_READ;
import static jpcsp.Emulator.EMU_STATUS_MEM_WRITE;
import static jpcsp.util.Utilities.readUnaligned32;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

import jpcsp.Emulator;
import jpcsp.Allegrex.compiler.RuntimeContext;
import jpcsp.arm.ARMDisassembler;
import jpcsp.arm.ARMInterpreter;
import jpcsp.arm.ARMMemory;
import jpcsp.arm.ARMProcessor;
import jpcsp.hardware.Model;
import jpcsp.hardware.Wlan;
import jpcsp.memory.mmio.wlan.WlanEmulator;
import jpcsp.util.LWJGLFixer;

/**
 * @author gid15
 *
 */
public class ARMTest {
	public static Logger log;

	public static void main(String[] args) {
        LWJGLFixer.fixOnce();
        DOMConfigurator.configure("LogSettings.xml");
        log = ARMProcessor.log;
		RuntimeContext.setLog4jMDC();
		Wlan.initialize();

		new ARMTest().testFirmware();
	}

	public void testFirmware() {
		int model = Model.MODEL_PSP_SLIM;
//		model = Model.MODEL_PSP_FAT;

		Model.setModel(model);
		File inputFile = new File(String.format("wlanfirm_%02dg.prx", Model.getGeneration()));
		byte[] buffer = new byte[(int) inputFile.length()];
		int length = buffer.length;
		try {
			InputStream is = new FileInputStream(inputFile);
			length = is.read(buffer);
			is.close();
		} catch (IOException e) {
			log.error(e);
		}
		int countFound = 0;
		int bootCodeOffset = 0;
		int bootCodeSize = 0;
		int dataOffset = 0;
		int dataSize = 0;
		int elfOffset = 0;
		for (int offset = 0; offset < length & countFound < 5; offset += 4) {
			int opcode = readUnaligned32(buffer, offset);
			if ((opcode & 0xFFFF8000) == 0x27BD8000) {
				// addiu $sp, $sp, -NNNN
				elfOffset = offset;
				countFound++;
			} else if ((opcode & 0xFC1F0000) == 0x24040000 && (opcode & 0x03E00000) != 0) {
				// addiu $a0, $reg, 0xNNNN
				bootCodeOffset = opcode & 0xFFFF;
				countFound++;
			} else if ((opcode & 0xFFFF0000) == 0x24050000) {
				// addiu $a1, $zr, 0xNNNN
				bootCodeSize = opcode & 0xFFFF;
				countFound++;
			} else if ((opcode & 0xFC1F0000) == 0x24060000) {
				// addiu $a2, $reg, 0xNNNN
				dataOffset = opcode & 0xFFFF;
				countFound++;
			} else if ((opcode & 0xFC1F0000) == 0x34070000) {
				// ori $a3, $reg, 0xNNNN
				dataSize = opcode & 0xFFFF;
				dataSize += 0x10000;
				countFound++;
			}
		}
		if (log.isDebugEnabled()) {
			log.debug(String.format("Reading '%s', elfOffset=0x%X, bootCodeOffset=0x%X, bootCodeSize=0x%X, dataOffset=0x%X, dataSize=0x%X", inputFile, elfOffset, bootCodeOffset, bootCodeSize, dataOffset, dataSize));
		}

		byte[] data = new byte[dataSize];
		System.arraycopy(buffer, elfOffset + dataOffset, data, 0, dataSize);

		ARMMemory mem = WlanEmulator.getInstance().getMemory();
		int baseAddress = ARMMemory.BASE_RAM0;
		for (int i = 0; i < bootCodeSize; i += 4) {
			mem.write32(baseAddress + i, readUnaligned32(buffer, elfOffset + bootCodeOffset + i));
		}

		mem.getHandlerWlanFirmware().setData(data, data.length);

		ARMProcessor processor = WlanEmulator.getInstance().getProcessor();
		ARMInterpreter interpreter = processor.interpreter;

		if (false) {
			ARMDisassembler disassembler = new ARMDisassembler(log, Level.INFO, processor.mem, processor.interpreter);
			disassembler.disasm(0);
		}
		RuntimeContext.debugCodeBlockCalls = true;
		Emulator.run = true;
		WlanEmulator.getInstance().bootFromThread();

		if (false) {
			int addr = Model.getGeneration() >= 2 ? 0x0000EFD9 : 0x0000C979;
			processor.setRegister(0, 0x18);
			log.debug(String.format("Executing 0x%08X", addr));
			processor.jumpWithMode(addr);
			Emulator.pause = false;
			interpreter.run();
		}
		if (false) {
			while (Emulator.status == EMU_STATUS_MEM_READ || Emulator.status == EMU_STATUS_MEM_WRITE) {
				Emulator.pause = false;
				interpreter.run();
			}
		}
		if (false && Model.getGeneration() >= 2) {
			// Init exception registers in all processor modes
			interpreter.disasm(0x00000F59, 0x4);
			interpreter.disasm(0x00000F5C, 0xC8);
			//
			interpreter.disasm(0x0000EFD9, 0x46);
			// Process interrupt flags read from 0x80002800
			interpreter.disasm(0x0000103D, 0x8);
			interpreter.disasm(0x00001871, 0x54);
		}
		if (false) {
			Emulator.pause = false;
			processor.interruptRequestException();
			interpreter.run();
		}
	}
}
