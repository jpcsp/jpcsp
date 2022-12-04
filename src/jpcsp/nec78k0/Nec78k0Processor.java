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
package jpcsp.nec78k0;

import static jpcsp.Allegrex.compiler.RuntimeContext.debugCodeBlockCalls;
import static jpcsp.Emulator.EMU_STATUS_UNIMPLEMENTED;
import static jpcsp.util.Utilities.clearBit;
import static jpcsp.util.Utilities.clearFlag;
import static jpcsp.util.Utilities.getFlagFromBit;
import static jpcsp.util.Utilities.hasBit;
import static jpcsp.util.Utilities.isRaisingBit;
import static jpcsp.util.Utilities.readUnaligned16;
import static jpcsp.util.Utilities.setBit;
import static jpcsp.util.Utilities.u8;
import static jpcsp.util.Utilities.writeUnaligned16;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import jpcsp.Emulator;
import jpcsp.memory.mmio.syscon.MMIOHandlerSysconFirmwareSfr;
import jpcsp.nec78k0.sfr.Nec78k0InterruptRequestInfo;

/**
 * @author gid15
 *
 */
public class Nec78k0Processor {
	public static boolean disassembleFunctions = false;
	// Interrupt Vector Table addresses
	public static final int RESET = 0x00;
	public static final int BRK   = 0x3E;
	//
	public static final int REGISTER_ADDRESS_BANK0 = 0xFEF8;
	public static final int REGISTER_ADDRESS_BANK1 = 0xFEF0;
	public static final int REGISTER_ADDRESS_BANK2 = 0xFEE8;
	public static final int REGISTER_ADDRESS_BANK3 = 0xFEE0;
	public static final int SFR_ADDRESS = 0xFF00;
	public static final int SHORT_DIRECT_ADDRESS = 0xFE20;
	public static final int SP_ADDRESS = 0xFF1C;
	public static final int PSW_ADDRESS = 0xFF1E;
	public static final int PSW_BIT_CY   = 0; // Carry flag
	public static final int PSW_BIT_ISP  = 1; // In-service priority flag
	public static final int PSW_BIT_RBS0 = 3; // Register bank select flag 0
	public static final int PSW_BIT_AC   = 4; // Auxiliary carry flag
	public static final int PSW_BIT_RBS1 = 5; // Register bank select flag 1
	public static final int PSW_BIT_Z    = 6; // Zero flag
	public static final int PSW_BIT_IE   = 7; // Interrupt enable flag
	public static final int PSW_ACZ_FLAGS = getFlagFromBit(PSW_BIT_AC) | getFlagFromBit(PSW_BIT_Z);
	public static final int PSW_CYACZ_FLAGS = getFlagFromBit(PSW_BIT_CY) | PSW_ACZ_FLAGS;
	public static final int PSW_RB_FLAGS = getFlagFromBit(PSW_BIT_RBS0) | getFlagFromBit(PSW_BIT_RBS1);
	public static final int REG_X = 0;
	public static final int REG_A = 1;
	public static final int REG_C = 2;
	public static final int REG_B = 3;
	public static final int REG_E = 4;
	public static final int REG_D = 5;
	public static final int REG_L = 6;
	public static final int REG_H = 7;
	public static final int REG_PAIR_AX = 0;
	public static final int REG_PAIR_BC = 1;
	public static final int REG_PAIR_DE = 2;
	public static final int REG_PAIR_HL = 3;
	public final Nec78k0Memory mem;
	public Logger log;
	public Nec78k0Interpreter interpreter;
	private Nec78k0Disassembler disassembler;
	// Program counter
	private int pc;
	private int currentInstructionPc;
	private int currentInstructionOpcode;
	private final byte[][] registerBanks = new byte[4][8];
	// Current register bank (from psw)
	private int registerBank;
	// Stack pointer
	private int sp;
	// Program status word
	private int psw;
	// Pending interrupt request
	private final Nec78k0InterruptRequestInfo interruptRequestInfo = new Nec78k0InterruptRequestInfo();
	// Debug
	private Nec78k0Debug debug;

	public Nec78k0Processor(Nec78k0Memory mem) {
		this.mem = mem;
		log = mem.getSfr().log;
		mem.setProcessor(this);

		if (debugCodeBlockCalls) {
			debug = new Nec78k0Debug(log);
		}
	}

	public void setLogger(Logger log) {
		this.log = log;
	}

	public void setInterpreter(Nec78k0Interpreter interpreter) {
		this.interpreter = interpreter;
	}

	public int startNewInstruction(int addr) {
		pc = addr;
		currentInstructionPc = pc;
		currentInstructionOpcode = 0;
		getNextInstructionOpcode();

		return currentInstructionOpcode;
	}

	public void interpret() {
		currentInstructionPc = pc;
		currentInstructionOpcode = 0;
		getNextInstructionOpcode();
		Nec78k0Instruction instruction = Nec78k0Decoder.instruction(this, currentInstructionOpcode);
		if (log.isTraceEnabled()) {
			String opcode;
			switch (instruction.getInstructionSize()) {
				case 1: opcode = String.format("0x%02X", currentInstructionOpcode); break;
				case 2: opcode = String.format("0x%04X", currentInstructionOpcode); break;
				case 3: opcode = String.format("0x%06X", currentInstructionOpcode); break;
				case 4: opcode = String.format("0x%08X", currentInstructionOpcode); break;
				default: opcode = String.format("0x%X", currentInstructionOpcode); break;
			}
			log.trace(String.format("0x%04X: [%s] - %s", currentInstructionPc, opcode, instruction.disasm(currentInstructionPc, currentInstructionOpcode)));
		}
		instruction.interpret(this, currentInstructionOpcode);
	}

	public int getNextInstructionOpcode() {
		int opcode = mem.internalRead8(pc);
		pc++;
		currentInstructionOpcode = (currentInstructionOpcode << 8) | opcode;

		return currentInstructionOpcode;
	}

	public int getCurrentInstructionPc() {
		return currentInstructionPc;
	}

	public int getCurrentInstructionOpcode() {
		return currentInstructionOpcode;
	}

	public int getNextInstructionPc() {
		return pc;
	}

	public void setPc(int pc) {
		this.pc = pc;
	}

	public int getPc() {
		return pc;
	}

	public boolean isNextInstructionPc(int addr) {
		return pc == addr;
	}

	public int getPsw() {
		return psw;
	}

	public void setPsw(int psw) {
		int oldPsw = this.psw;

		this.psw = psw & 0xFF;

		registerBank = (hasBit(psw, PSW_BIT_RBS0) ? 1 : 0) | (hasBit(psw, PSW_BIT_RBS1) ? 2 : 0);

		if (isRaisingBit(oldPsw, this.psw, PSW_BIT_IE)) {
			if (log.isTraceEnabled()) {
				log.trace(String.format("Enabling interrupts"));
			}
			checkPendingInterrupt();
		}
	}

	public boolean isInterruptEnabled() {
		return hasBit(psw, PSW_BIT_IE);
	}

	public boolean isInServicePriority() {
		return hasBit(psw, PSW_BIT_ISP);
	}

	public boolean isZeroFlag() {
		return hasBit(psw, PSW_BIT_Z);
	}

	public void setRegisterBank(int rbn) {
		psw = clearFlag(psw, PSW_RB_FLAGS);
		if (hasBit(rbn, 0)) {
			psw = setBit(psw, PSW_BIT_RBS0);
		}
		if (hasBit(rbn, 1)) {
			psw = setBit(psw, PSW_BIT_RBS1);
		}
		registerBank = rbn;
	}

	public boolean isAuxiliaryCarryFlag() {
		return hasBit(psw, PSW_BIT_AC);
	}

	public boolean isCarryFlag() {
		return hasBit(psw, PSW_BIT_CY);
	}

	public boolean isPriorityFlag() {
		return hasBit(psw, PSW_BIT_ISP);
	}

	public int getSp() {
		return sp;
	}

	public void setSp(int sp) {
		this.sp = sp & 0xFFFF;
	}

	public void reset() {
		pc = 0;
		pc = mem.read16(RESET);
		psw = setBit(0x00, PSW_BIT_ISP);

		if (disassembleFunctions) {
			disassemble(pc);
		}
	}

	public static int getValue8(int value) {
		return value & 0xFF;
	}

	public static int getValue16(int value) {
		return value & 0xFFFF;
	}

	public static boolean isZero8(int value) {
		return getValue8(value) == 0x00;
	}

	public static boolean isZero16(int value) {
		return getValue16(value) == 0x0000;
	}

	public int getRegister(int r) {
		return getRegister(r, registerBank);
	}

	public int getRegister(int r, int rb) {
		return u8(registerBanks[rb][r & 0x7]);
	}

	public int getRegisterPair(int rp) {
		return getRegisterPair(rp, registerBank);
	}

	public int getRegisterPair(int rp, int rb) {
		int r = rp << 1;
		return getRegister(r, rb) | (getRegister(r + 1, rb) << 8);
	}

	public void setRegister(int r, int value) {
		setRegister(r, registerBank, value);
	}

	public void setRegister(int r, int rb, int value) {
		registerBanks[rb][r & 0x7] = (byte) value;
	}

	public void setRegisterPair(int rp, int value) {
		setRegisterPair(rp, registerBank, value);
	}

	public void setRegisterPair(int rp, int rb, int value) {
		int r = rp << 1;
		setRegister(r, rb, value);
		setRegister(r + 1, rb, value >> 8);
	}

	public void setPswResult(int result) {
		if (isZero8(result)) {
			psw = setBit(psw, PSW_BIT_Z);
		} else {
			psw = clearBit(psw, PSW_BIT_Z);
		}
	}

	public void setPswResult(int result, boolean cy, boolean ac) {
		psw = clearFlag(psw, PSW_CYACZ_FLAGS);
		if (isZero8(result)) {
			psw = setBit(psw, PSW_BIT_Z);
		}
		if (cy) {
			psw = setBit(psw, PSW_BIT_CY);
		}
		if (ac) {
			psw = setBit(psw, PSW_BIT_AC);
		}
	}

	public void setPswResult16(int result, boolean cy, boolean ac) {
		psw = clearFlag(psw, PSW_CYACZ_FLAGS);
		if (isZero16(result)) {
			psw = setBit(psw, PSW_BIT_Z);
		}
		if (cy) {
			psw = setBit(psw, PSW_BIT_CY);
		}
		if (ac) {
			psw = setBit(psw, PSW_BIT_AC);
		}
	}

	public void setPswResult(int result, boolean ac) {
		psw = clearFlag(psw, PSW_ACZ_FLAGS);
		if (isZero8(result)) {
			psw = setBit(psw, PSW_BIT_Z);
		}
		if (ac) {
			psw = setBit(psw, PSW_BIT_AC);
		}
	}

	public void setCarryFlag(boolean cy) {
		if (cy) {
			psw = setBit(psw, PSW_BIT_CY);
		} else {
			psw = clearBit(psw, PSW_BIT_CY);
		}
	}

	public static int getSaddr(int saddr) {
		saddr &= 0xFF;
		if (saddr >= 0x20) {
			return SHORT_DIRECT_ADDRESS + saddr - 0x20;
		} else {
			return SFR_ADDRESS + saddr;
		}
	}

	public static int getSfr(int sfr) {
		sfr &= 0xFF;
		return SFR_ADDRESS + sfr;
	}

	public void push8(int value) {
		sp -= 1;
		mem.write8(sp, (byte) value);
	}

	public void push16(int value) {
		if (debugCodeBlockCalls) {
			// The bootloader code is using at 2 places a "push" to simulate a kind of "call"
			if (getCurrentInstructionPc() == 0x09F4 || getCurrentInstructionPc() == 0x0A1F) {
				debug.call(this, value);
			}
		}

		sp -= 2;
		writeUnaligned16(mem, sp, value);
	}

	public int pop8() {
		int value = mem.read8(sp);
		sp += 1;

		return value;
	}

	public int pop16() {
		int value = readUnaligned16(mem, sp);
		sp += 2;

		return value;
	}

	public void disassemble(int addr) {
		getDisassembler().disasm(addr);
	}

	public Nec78k0Disassembler getDisassembler() {
		if (disassembler == null) {
			disassembler = new Nec78k0Disassembler(log, Level.DEBUG, this);
		}
		return disassembler;
	}

	public void call(int addr) {
		addr &= 0xFFFF;

		if (addr == 0x0000) {
			log.error(String.format("Calling address 0x%04X, something is wrong", addr));
			Emulator.PauseEmuWithStatus(EMU_STATUS_UNIMPLEMENTED);
		} else if (disassembleFunctions) {
			disassemble(addr);
		}

		if (debugCodeBlockCalls) {
			debug.call(this, addr);
		}

		push16(pc);

		setPc(addr);
		checkPendingInterrupt();
	}

	public void ret() {
		if (debugCodeBlockCalls) {
			debug.ret();
		}

		setPc(pop16());
		checkPendingInterrupt();
	}

	public void reti() {
		if (debugCodeBlockCalls) {
			debug.ret();
		}

		setPc(pop16());
		setPsw(pop8());
	}

	public boolean checkPendingInterrupt() {
		boolean interruptTriggered = false;

		if (isInterruptEnabled()) {
			interruptRequestInfo.vectorTableAddress = -1;
			interruptRequestInfo.highPriority = false;
			mem.getSfr().checkInterrupts(interruptRequestInfo);
			if (interruptRequestInfo.vectorTableAddress >= 0) {
				mem.getSfr().clearInterruptRequest(interruptRequestInfo.interruptRequestBit);
				interrupt(interruptRequestInfo.vectorTableAddress, interruptRequestInfo.highPriority);
				interruptTriggered = true;
			}
		}

		return interruptTriggered;
	}

	private void interrupt(int vectorTableAddress, boolean highPriority) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("Triggering interrupt 0x%02X(%s), highPriority=%b", vectorTableAddress, MMIOHandlerSysconFirmwareSfr.getInterruptName(vectorTableAddress), highPriority));
		}

		int addr = mem.read16(vectorTableAddress);

		if (debugCodeBlockCalls) {
			debug.callInterrupt(this, addr, vectorTableAddress);
		}

		push8(psw);
		push16(pc);

		if (highPriority) {
			psw = clearBit(psw, PSW_BIT_ISP);
		} else {
			psw = setBit(psw, PSW_BIT_ISP);
		}
		psw = clearBit(psw, PSW_BIT_IE);
		pc = addr;

		if (disassembleFunctions) {
			disassemble(pc);
		}
	}

	public void branch(int disp) {
		setPc(pc + disp);
		checkPendingInterrupt();
	}

	public void jump(int addr, boolean dynamicAddr) {
		addr &= 0xFFFF;

		if (addr == 0x0000) {
			log.error(String.format("Jumping to address 0x%04X, something is wrong", addr));
			Emulator.PauseEmuWithStatus(EMU_STATUS_UNIMPLEMENTED);
		}

		if (debugCodeBlockCalls && dynamicAddr) {
			if (mem.internalRead8(currentInstructionPc - 1) == 0xB3) {
				debug.call(this, addr);
			}
		}

		setPc(addr);
		checkPendingInterrupt();

		if (disassembleFunctions && dynamicAddr) {
			disassemble(pc);
		}
	}

	public void halt() {
		if (log.isDebugEnabled()) {
			log.debug(String.format("halt at 0x%04X", getCurrentInstructionPc()));
			if (debugCodeBlockCalls) {
				debug.dump();
			}
		}

		if (!checkPendingInterrupt()) {
			interpreter.setHalted(true);
			interpreter.exitInterpreter();
		}
	}
}
