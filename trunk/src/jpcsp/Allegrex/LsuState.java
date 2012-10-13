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
package jpcsp.Allegrex;

import jpcsp.Emulator;
import jpcsp.Memory;

/**
 * Load Store Unit, handles memory operations.
 *
 * @author hli
 */
public class LsuState extends MduState {

    public static final Memory memory = Memory.getInstance();
    protected static final boolean CHECK_ALIGNMENT = true;

    @Override
    public void reset() {
    }

    @Override
    public void resetAll() {
        super.resetAll();
    }

    public LsuState() {
    }

    public void copy(LsuState that) {
        super.copy(that);
    }

    public LsuState(LsuState that) {
        super(that);
    }

    public void doLB(int rt, int rs, int simm16) {
        int word = (byte)memory.read8(getRegister(rs) + simm16);
        if (rt != 0) {
            setRegister(rt, word);
        }
    }

    public void doLBU(int rt, int rs, int simm16) {
        int word = memory.read8(getRegister(rs) + simm16) & 0xff;
        if (rt != 0) {
            setRegister(rt, word);
        }
    }

    public void doLH(int rt, int rs, int simm16) {
        if (CHECK_ALIGNMENT) {
            CpuState cpu = Emulator.getProcessor().cpu;
            int address = getRegister(rs) + simm16;
            if ((address & 1) != 0) {
                Memory.log.error(String.format("LH unaligned addr:0x%08x pc:0x%08x", address, cpu.pc));
            }
        }

        int word = (short)memory.read16(getRegister(rs) + simm16);
        if (rt != 0) {
            setRegister(rt, word);
        }
    }

    public void doLHU(int rt, int rs, int simm16) {
        if (CHECK_ALIGNMENT) {
            CpuState cpu = Emulator.getProcessor().cpu;
            int address = getRegister(rs) + simm16;
            if ((address & 1) != 0) {
                Memory.log.error(String.format("LHU unaligned addr:0x%08x pc:0x%08x", address, cpu.pc));
            }
        }

        int word = memory.read16(getRegister(rs) + simm16) & 0xffff;
        if (rt != 0) {
            setRegister(rt, word);
        }
    }

    private static final int[] lwlMask = { 0xffffff, 0xffff, 0xff, 0 };
    private static final int[] lwlShift = { 24, 16, 8, 0 };

    public void doLWL(int rt, int rs, int simm16) {
        int address = getRegister(rs) + simm16;
        int offset = address & 0x3;
        int value = getRegister(rt);

        int data = memory.read32(address & 0xfffffffc);
        if (rt != 0) {
            setRegister(rt, (data << lwlShift[offset]) | (value & lwlMask[offset]));
        }
    }

    public void doLW(int rt, int rs, int simm16) {
        if (CHECK_ALIGNMENT) {
            CpuState cpu = Emulator.getProcessor().cpu;
            int address = getRegister(rs) + simm16;
            if ((address & 3) != 0) {
                Memory.log.error(String.format("LW unaligned addr:0x%08x pc:0x%08x", address, cpu.pc));
            }
        }

        int word = memory.read32(getRegister(rs) + simm16);
        if (rt != 0) {
            setRegister(rt, word);
        }
    }

    private static final int[] lwrMask = { 0, 0xff000000, 0xffff0000, 0xffffff00 };
    private static final int[] lwrShift = { 0, 8, 16, 24 };

    public void doLWR(int rt, int rs, int simm16) {
        int address = getRegister(rs) + simm16;
        int offset = address & 0x3;
        int value = getRegister(rt);

        int data = memory.read32(address & 0xfffffffc);
        if (rt != 0) {
            setRegister(rt, (data >>> lwrShift[offset]) | (value & lwrMask[offset]));
        }
    }

    public void doSB(int rt, int rs, int simm16) {
        memory.write8(getRegister(rs) + simm16, (byte) (getRegister(rt) & 0xFF));
    }

    public void doSH(int rt, int rs, int simm16) {
        if (CHECK_ALIGNMENT) {
            CpuState cpu = Emulator.getProcessor().cpu;
            int address = getRegister(rs) + simm16;
            if ((address & 1) != 0) {
                Memory.log.error(String.format("SH unaligned addr:0x%08x pc:0x%08x", address, cpu.pc));
            }
        }

        memory.write16(getRegister(rs) + simm16, (short) (getRegister(rt) & 0xFFFF));
    }

    private static final int[] swlMask = { 0xffffff00, 0xffff0000, 0xff000000, 0 };
    private static final int[] swlShift = { 24, 16, 8, 0 };

    public void doSWL(int rt, int rs, int simm16) {
        int address = getRegister(rs) + simm16;
        int offset = address & 0x3;
        int value = getRegister(rt);
        int data = memory.read32(address & 0xfffffffc);

        data = (value >>> swlShift[offset]) | (data & swlMask[offset]);

        memory.write32(address & 0xfffffffc, data);
    }

    public void doSW(int rt, int rs, int simm16) {
        if (CHECK_ALIGNMENT) {
            CpuState cpu = Emulator.getProcessor().cpu;
            int address = getRegister(rs) + simm16;
            if ((address & 3) != 0) {
                Memory.log.error(String.format("SW unaligned addr:0x%08x pc:0x%08x", address, cpu.pc));
            }
        }

        memory.write32(getRegister(rs) + simm16, getRegister(rt));
    }

    private static final int[] swrMask = { 0, 0xff, 0xffff, 0xffffff };
    private static final int[] swrShift = { 0, 8, 16, 24 };

    public void doSWR(int rt, int rs, int simm16) {
        int address = getRegister(rs) + simm16;
        int offset = address & 0x3;
        int value = getRegister(rt);
        int data = memory.read32(address & 0xfffffffc);

        data = (value << swrShift[offset]) | (data & swrMask[offset]);

        memory.write32(address & 0xfffffffc, data);
    }

    public void doLL(int rt, int rs, int simm16) {
        int word = memory.read32(getRegister(rs) + simm16);
        if (rt != 0) {
            setRegister(rt, word);
        }
        //ll_bit = 1;
    }

    public void doSC(int rt, int rs, int simm16) {
        memory.write32(getRegister(rs) + simm16, getRegister(rt));
        if (rt != 0) {
            setRegister(rt, 1); // = ll_bit;
        }
    }
}