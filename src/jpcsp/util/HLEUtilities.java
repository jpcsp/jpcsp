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
package jpcsp.util;

import static jpcsp.Allegrex.Common._ra;
import static jpcsp.Allegrex.Common._v0;
import static jpcsp.Allegrex.Common._zr;

import jpcsp.AllegrexOpcodes;
import jpcsp.Memory;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.HLEModuleFunction;
import jpcsp.HLE.Modules;
import jpcsp.HLE.SyscallHandler;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointerFunction;
import jpcsp.HLE.modules.SysMemUserForUser;
import jpcsp.HLE.modules.SysMemUserForUser.SysMemInfo;
import jpcsp.memory.IMemoryWriter;
import jpcsp.memory.MemoryWriter;

public class HLEUtilities {
	private static HLEUtilities instance;
	private int internalMemoryAddress;
	private int internalMemorySize;

	public static HLEUtilities getInstance() {
		if (instance == null) {
			instance = new HLEUtilities();
		}

		return instance;
	}

	private HLEUtilities() {
	}

	public void init() {
		internalMemoryAddress = 0;
		internalMemorySize = 0;
	}

	public int allocateInternalMemory(int size) {
		// Align on a multiple of 8 bytes
		size = Utilities.alignUp(size, 7);

		if (internalMemorySize < size) {
			SysMemInfo sysMemInfo = Modules.SysMemUserForUserModule.malloc(SysMemUserForUser.KERNEL_PARTITION_ID, "HLE Internal Memory", SysMemUserForUser.PSP_SMEM_Low, Math.max(256, size), 0);
			internalMemoryAddress = sysMemInfo.addr;
			internalMemorySize = sysMemInfo.allocatedSize;
		}

		int addr = internalMemoryAddress;
		internalMemoryAddress += size;
		internalMemorySize -= size;

		return addr;
	}

    public static int NOP() {
    	// sll $zr, $zr, 0 <=> nop
    	return (AllegrexOpcodes.SLL << 26) | (_zr << 16) | (_zr << 11) | (0 << 6);
    }

    public static int SYNC() {
    	return (AllegrexOpcodes.SPECIAL << 26) | AllegrexOpcodes.SYNC;
    }

    public static int MOVE(int rd, int rs) {
    	// addu rd, rs, $zr <=> move rd, rs
    	return AllegrexOpcodes.ADDU | (rd << 11) | (_zr << 16) | (rs << 21);
    }

    public static int LUI(int rd, int imm16) {
    	return (AllegrexOpcodes.LUI << 26) | (rd << 16) | (imm16 & 0xFFFF);
    }

    public static int ADDIU(int rt, int rs, int imm16) {
    	return (AllegrexOpcodes.ADDIU << 26) | (rs << 21) | (rt << 16) | (imm16 & 0xFFFF);
    }

    public static int LI(int rt, int imm16) {
    	if (imm16 == 0) {
    		return MOVE(rt, _zr);
    	}
    	return ADDIU(rt, _zr, imm16);
    }

    public static int ORI(int rt, int rs, int imm16) {
    	return (AllegrexOpcodes.ORI << 26) | (rs << 21) | (rt << 16) | (imm16 & 0xFFFF);
    }

    public static int SW(int rt, int base, int imm16) {
    	return (AllegrexOpcodes.SW << 26) | (base << 21) | (rt << 16) | (imm16 & 0xFFFF);
    }

    public static int SB(int rt, int rs, int imm16) {
    	return (AllegrexOpcodes.SB << 26) | (rs << 21) | (rt << 16) | (imm16 & 0xFFFF);
    }

    public static int LW(int rt, int base, int imm16) {
    	return (AllegrexOpcodes.LW << 26) | (base << 21) | (rt << 16) | (imm16 & 0xFFFF);
    }

    public static int JAL(int address) {
    	return (AllegrexOpcodes.JAL << 26) | ((address >> 2) & 0x03FFFFFF);
    }

    public static int J(int address) {
    	return (AllegrexOpcodes.J << 26) | ((address >> 2) & 0x03FFFFFF);
    }

    public static int SYSCALL(int syscallCode) {
    	return (AllegrexOpcodes.SPECIAL << 26) | AllegrexOpcodes.SYSCALL | ((syscallCode & 0xFFFFF) << 6);
    }

    public static int SYSCALL(HLEModule hleModule, String hleFunctionName) {
    	HLEModuleFunction hleModuleFunction = hleModule.getHleFunctionByName(hleFunctionName);
    	if (hleModuleFunction == null) {
    		return SYSCALL(SyscallHandler.syscallUnmappedImport);
    	}

    	// syscall [functionName]
    	return SYSCALL(hleModuleFunction.getSyscallCode());
    }

    public static int JR() {
    	return JR(_ra);
    }

    public static int JR(int reg) {
    	// jr $reg
    	return (AllegrexOpcodes.SPECIAL << 26) | AllegrexOpcodes.JR | (reg << 21);
    }

    public static int B(int destination) {
    	// beq $zr, $zr, destination <=> b destination
    	return (AllegrexOpcodes.BEQ << 26) | (_zr << 21) | (_zr << 16) | (destination & 0x0000FFFF);
    }

    public static int BREAK(int breakCode) {
    	return (AllegrexOpcodes.SPECIAL << 26) | AllegrexOpcodes.BREAK | (breakCode << 6);
    }

    public static int ERET() {
    	return (AllegrexOpcodes.COP0 << 26) | AllegrexOpcodes.ERET | 0x8;
    }

    public void installHLESyscall(int address, HLEModule hleModule, String hleFunctionName) {
    	installHLESyscall(new TPointer(Memory.getInstance(), address), hleModule, hleFunctionName);
    }

    public void installHLESyscall(TPointerFunction address, HLEModule hleModule, String hleFunctionName) {
    	installHLESyscall(address.getPointer(), hleModule, hleFunctionName);
    }

    public void installHLESyscall(TPointer address, HLEModule hleModule, String hleFunctionName) {
        IMemoryWriter memoryWriter = MemoryWriter.getMemoryWriter(address, 8, 4);
        memoryWriter.writeNext(JR());
        memoryWriter.writeNext(SYSCALL(hleModule, hleFunctionName));
        memoryWriter.flush();
    }

    public void installHLESyscallWithJump(TPointer address, HLEModule hleModule, String hleFunctionName) {
        IMemoryWriter memoryWriter = MemoryWriter.getMemoryWriter(address, 16, 4);
        memoryWriter.writeNext(NOP());
        memoryWriter.writeNext(SYSCALL(hleModule, hleFunctionName));
        memoryWriter.writeNext(JR(_v0));
        memoryWriter.writeNext(NOP());
        memoryWriter.flush();
    }

    public int installHLESyscall(HLEModule hleModule, String hleFunctionName) {
    	int addr = allocateInternalMemory(8);
        IMemoryWriter memoryWriter = MemoryWriter.getMemoryWriter(addr, 8, 4);
        memoryWriter.writeNext(JR());
        memoryWriter.writeNext(SYSCALL(hleModule, hleFunctionName));
        memoryWriter.flush();

    	return addr;
    }

    public int installLoopHandler(HLEModule hleModule, String hleFunctionName) {
    	int address = allocateInternalMemory(8);
        IMemoryWriter memoryWriter = MemoryWriter.getMemoryWriter(address, 8, 4);
        memoryWriter.writeNext(HLEUtilities.B(-1));
        memoryWriter.writeNext(HLEUtilities.SYSCALL(hleModule, hleFunctionName));
        memoryWriter.flush();

        return address;
    }

    public int installHLEThread(int address, HLEModule hleModule, String hleFunctionName) {
        IMemoryWriter memoryWriter = MemoryWriter.getMemoryWriter(address, 8, 4);
        memoryWriter.writeNext(B(-1));
        memoryWriter.writeNext(SYSCALL(hleModule, hleFunctionName));
        memoryWriter.flush();

        return address;
    }

    public int installHLEThread(HLEModule hleModule, String hleFunctionName) {
    	return installHLEThread(allocateInternalMemory(8), hleModule, hleFunctionName);
    }

    public int installHLEInterruptHandler(HLEModule hleModule, String hleFunctionName) {
    	int addr = allocateInternalMemory(16);
        IMemoryWriter memoryWriter = MemoryWriter.getMemoryWriter(addr, 16, 4);
        memoryWriter.writeNext(NOP()); // Do not start the CodeBlock with a SYSCALL, the compiler would assume it is not returning
        memoryWriter.writeNext(SYSCALL(hleModule, hleFunctionName));
        memoryWriter.writeNext(ERET());
        memoryWriter.writeNext(NOP());
        memoryWriter.flush();

    	return addr;
    }
}
