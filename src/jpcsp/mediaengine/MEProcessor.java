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
package jpcsp.mediaengine;

import org.apache.log4j.Logger;

import jpcsp.Emulator;
import jpcsp.Processor;
import jpcsp.Allegrex.compiler.RuntimeContextLLE;

/**
 * The PSP Media Engine is very close to the PSP main CPU. It has the same instructions
 * with the addition of 3 new ones. It has a FPU but no VFPU.
 *
 * The ME specific instructions are:
 * - mtvme rt, imm16
 *      opcode: 0xB0E00000 | (rt << 16) | imm16
 *      stores the content of the CPU register rt to an unknown VME register imm16
 * - mfvme rt, imm16
 *      opcode: 0x68E00000 | (rt << 16) | imm16
 *      loads the content of an unknown VME register imm16 to the CPU register rt
 * - dbreak
 *      opcode: 0x7000003F
 *      debugging break causing a trap to the address 0xBFC01000 (?)
 *
 * @author gid15
 *
 */
public class MEProcessor extends Processor {
	public static Logger log = Logger.getLogger("me");
	public static final int CPUID_ME = 1;
	private static MEProcessor instance;
	private MEMemory meMemory;
	private final int[] vmeRegisters = new int[0x590]; // Highest VME register number seen is 0x058F
	private boolean halt;

	public static MEProcessor getInstance() {
		if (instance == null) {
			instance = new MEProcessor();
		}
		return instance;
	}

	private MEProcessor() {
		meMemory = new MEMemory(RuntimeContextLLE.getMMIO());
		cpu.setMemory(meMemory);

		// CPUID is 1 for the ME
		cp0.setCpuid(CPUID_ME);

		// BEV = 1 during bootstrapping
		cp0.setStatus(0x00400000);

		halt();
	}

	public MEMemory getMEMemory() {
		return meMemory;
	}

	public int getVmeRegister(int reg) {
		return vmeRegisters[reg];
	}

	public void setVmeRegister(int reg, int value) {
		vmeRegisters[reg] = value;
	}

	public void triggerException(int IP) {
		int status = cp0.getStatus();
		status |= (IP & 0xFF) << 8;
		cp0.setStatus(status);

		if ((status & 0x00400000) == 0) {
			cpu.pc = cp0.getEbase();
		} else {
			cpu.pc = 0xBFC00000;
		}

		halt = false;
	}

	public void halt() {
		halt = true;
	}

	public boolean isHalted() {
		return halt;
	}

	public void run() {
		while (!halt && !Emulator.pause) {
			step();
		}
	}
}
