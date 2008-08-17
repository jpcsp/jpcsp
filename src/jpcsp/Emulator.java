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
package jpcsp;

import java.io.IOException;
import java.util.List;
import java.util.LinkedList;
import jpcsp.format.DeferredStub;
import jpcsp.format.Elf32;
import jpcsp.format.Elf32Relocate;
import jpcsp.format.Elf32SectionHeader;
import jpcsp.format.Elf32SectionHeader.ShType;
import jpcsp.format.Elf32StubHeader;
import static jpcsp.util.Utilities.*;

public class Emulator {

    private static Processor cpu;
    private Gpu gpu;
    private Controller controller;
    private FileManager romManager;
    private boolean mediaImplemented = false;

    public boolean run = false;
    public boolean pause = false;
    public boolean stop = false;
    public boolean resume = false;

    public Emulator() {
        cpu = new Processor();
        gpu = new Gpu();
        controller = new Controller();
    }

    public void load(String filename) throws IOException {
        // TODO: here will load fileName, iso or etc...
        //getProcessor().reset();
        //ELFLoader.LoadPBPELF(filename, getProcessor());
        processLoading(filename);
        if (!mediaImplemented) {
            throw new IOException("This kind of file format still not supported.");
        }
    }

    private void delay(long numberCyclesDelay) {
        throw new UnsupportedOperationException("Not yet implemented");
    }



    private void processLoading(String fileName) throws IOException {
        initNewPsp();
        romManager = new FileManager(fileName);

        switch (romManager.getType()) {
            case FileManager.FORMAT_ELF:
                initElf32();//RAM, CPU, GPU...
                break;
            case FileManager.FORMAT_ISO:
                break;
            case FileManager.FORMAT_PBP:
                initPbp();//RAM, CPU, GPU...
                break;
            case FileManager.FORMAT_UMD:
                break;
            default:
                throw new IOException("Is not an acceptable format, please choose the rigth file.");
        }
    }

    //elf32 initElf32
    private void initElf32() throws IOException {
        mediaImplemented = true;
        initRamBy(romManager.getElf32());
        initCpuBy(romManager.getElf32());
        initDebugWindowsByElf32();
    }

    private void initPbp() throws IOException {
        mediaImplemented = true;
        initRamBy(romManager.getPBP().getElf32());
        initCpuBy(romManager.getPBP().getElf32());
        initDebugWindowsByPbp();
        //RAM, CPU, GPU...
    }

    private void initRamBy(Elf32 elf) throws IOException {
        // Relocation
        if (elf.getHeader().requiresRelocation()) {
            for (Elf32SectionHeader shdr : elf.getListSectionHeader()) {
                if (shdr.getSh_type() == ShType.PRXREL.getValue() || // 0x700000A0
                        shdr.getSh_type() == ShType.REL.getValue()) // 0x00000009
                {
                    Elf32Relocate rel = new Elf32Relocate();
                    romManager.getActualFile().seek(romManager.getElfoffset() + shdr.getSh_offset());

                    int RelCount = (int) shdr.getSh_size() / Elf32Relocate.sizeof();
                    System.out.println(shdr.getSh_namez() + ": relocating " + RelCount + " entries");

                    int AHL = 0; // (AHI << 16) | (ALO & 0xFFFF)

                    int HI_addr = 0; // We'll use this to relocate R_MIPS_HI16 when we get a R_MIPS_LO16

                    // Relocation modes, only 1 is allowed at a time
                    boolean external = true; // copied from soywiz/pspemulator

                    boolean local = false;
                    boolean _gp_disp = false;

                    for (int i = 0; i < RelCount; i++) {
                        rel.read(romManager.getActualFile());

                        int R_TYPE = (int) (rel.getR_info() & 0xFF);
                        int OFS_BASE = (int) ((rel.getR_info() >> 8) & 0xFF);
                        int ADDR_BASE = (int) ((rel.getR_info() >> 16) & 0xFF);
                        //System.out.println("type=" + R_TYPE + ",base=" + OFS_BASE + ",addr=" + ADDR_BASE + "");

                        int data = Memory.get_instance().read32((int) romManager.getBaseoffset() + (int) rel.getR_offset());
                        long result = 0; // Used to hold the result of relocation, OR this back into data

                        // these are the addends?
                        // SysV ABI MIPS quote: "Because MIPS uses only Elf32_Rel re-location entries, the relocated field holds the addend."
                        int half16 = data & 0x0000FFFF; // 31/07/08 unused (fiveofhearts)

                        int word32 = data & 0xFFFFFFFF;
                        int targ26 = data & 0x03FFFFFF;
                        int hi16 = data & 0x0000FFFF;
                        int lo16 = data & 0x0000FFFF;
                        int rel16 = data & 0x0000FFFF;
                        int lit16 = data & 0x0000FFFF; // 31/07/08 unused (fiveofhearts)

                        int pc = data & 0x0000FFFF; // 31/07/08 unused (fiveofhearts)

                        int A = 0; // addend
                        // moved outside the loop so context is saved
                        //int AHL = 0; // (AHI << 16) | (ALO & 0xFFFF)

                        int P = (int) romManager.getBaseoffset() + (int) rel.getR_offset(); // address of instruction being relocated? 31/07/08 unused when external=true (fiveofhearts)

                        int S = (int) romManager.getBaseoffset(); // ? copied from soywiz/pspemulator, but doesn't match the docs (fiveofhearts)

                        int G = 0; // ? 31/07/08 unused (fiveofhearts)

                        int GP = (int) romManager.getBaseoffset() + (int) romManager.getPSPModuleInfo().getM_gp(); // final gp value, computed correctly? 31/07/08 only used in R_MIPS_GPREL16 which is untested (fiveofhearts)

                        int GP0 = (int) romManager.getPSPModuleInfo().getM_gp(); // gp value, computed correctly? 31/07/08 unused when external=true (fiveofhearts)

                        int EA = 0; // ? 31/07/08 unused (fiveofhearts)

                        int L = 0; // ? 31/07/08 unused (fiveofhearts)

                        switch (R_TYPE) {
                            case 0: //R_MIPS_NONE
                                // Don't do anything

                                break;

                            case 5: //R_MIPS_HI16

                                A = hi16;
                                AHL = A << 16;
                                HI_addr = (int) romManager.getBaseoffset() + (int) rel.getR_offset();
                                break;

                            case 6: //R_MIPS_LO16

                                A = lo16;
                                AHL &= ~0x0000FFFF; // delete lower bits, since many R_MIPS_LO16 can follow one R_MIPS_HI16

                                AHL |= A & 0x0000FFFF;

                                if (external || local) {
                                    result = AHL + S;
                                    data &= ~0x0000FFFF;
                                    data |= result & 0x0000FFFF; // truncate

                                    // Process deferred R_MIPS_HI16
                                    int data2 = Memory.get_instance().read32(HI_addr);
                                    data2 &= ~0x0000FFFF;
                                    data2 |= (result >> 16) & 0x0000FFFF; // truncate

                                    Memory.get_instance().write32(HI_addr, data2);
                                } else if (_gp_disp) {
                                    result = AHL + GP - P + 4;

                                    // verify
                                    if ((result & ~0xFFFFFFFF) != 0) {
                                        throw new IOException("Relocation overflow (R_MIPS_LO16)");
                                    }
                                    data &= ~0x0000FFFF;
                                    data |= result & 0x0000FFFF;

                                    // Process deferred R_MIPS_HI16
                                    int data2 = Memory.get_instance().read32(HI_addr);

                                    result = AHL + GP - P;

                                    // verify
                                    if ((result & ~0xFFFFFFFF) != 0) {
                                        throw new IOException("Relocation overflow (R_MIPS_HI16)");
                                    }
                                    data2 &= ~0x0000FFFF;
                                    data2 |= (result >> 16) & 0x0000FFFF;
                                    Memory.get_instance().write32(HI_addr, data2);
                                }
                                break;

                            case 4: //R_MIPS_26

                                if (local) {
                                    A = targ26;
                                    result = ((A << 2) | (P & 0xf0000000) + S) >> 2;
                                    data &= ~0x03FFFFFF;
                                    data |= (int) (result & 0x03FFFFFF); // truncate

                                } else if (external) {
                                    A = targ26;

                                    // docs say "sign-extend(A < 2)", but is it meant to be A << 2? if so then there's no point sign extending
                                    //result = (sign-extend(A < 2) + S) >> 2;
                                    //result = (((A < 2) ? 0xFFFFFFFF : 0x00000000) + S) >> 2;
                                    result = ((A << 2) + S) >> 2; // copied from soywiz/pspemulator

                                    data &= ~0x03FFFFFF;
                                    data |= (int) (result & 0x03FFFFFF); // truncate

                                }
                                break;

                            case 2: //R_MIPS_32
                                // This doesn't match soywiz/pspemulator but it generates more sensible addresses (fiveofhearts)

                                A = word32;
                                result = S + A;
                                data &= ~0xFFFFFFFF;
                                data |= (int) (result & 0xFFFFFFFF); // truncate

                                break;

                            /* sample before relocation: 0x00015020: 0x8F828008 '....' - lw         $v0, -32760($gp)
                            case 7: //R_MIPS_GPREL16
                                // 31/07/08 untested (fiveofhearts)
                                System.out.println("Untested relocation type " + R_TYPE + " at " + String.format("%08x", (int)baseoffset + (int)rel.r_offset));

                                if (external)
                                {
                                    A = rel16;

                                    //result = sign-extend(A) + S + GP;
                                    result = (((A & 0x00008000) != 0) ? A & 0xFFFF0000 : A) + S + GP;

                                    // verify
                                    if ((result & ~0x0000FFFF) != 0)
                                        throw new IOException("Relocation overflow (R_MIPS_GPREL16)");

                                    data &= ~0x0000FFFF;
                                    data |= (int)(result & 0x0000FFFF);
                                }
                                else if (local)
                                {
                                    A = rel16;

                                    //result = sign-extend(A) + S + GP;
                                    result = (((A & 0x00008000) != 0) ? A & 0xFFFF0000 : A) + S + GP0 - GP;

                                    // verify
                                    if ((result & ~0x0000FFFF) != 0)
                                        throw new IOException("Relocation overflow (R_MIPS_GPREL16)");

                                    data &= ~0x0000FFFF;
                                    data |= (int)(result & 0x0000FFFF);
                                }
                                break;
                            */

                            default:
                                System.out.println("Unhandled relocation type " + R_TYPE + " at " + String.format("%08x", (int) romManager.getBaseoffset() + (int) rel.getR_offset()));
                                break;
                        }

                        //System.out.println("Relocation type " + R_TYPE + " at " + String.format("%08x", (int)baseoffset + (int)rel.r_offset));
                        Memory.get_instance().write32((int) romManager.getBaseoffset() + (int) rel.getR_offset(), data);
                    }
                }
            }
        }

        // Imports
        for (Elf32SectionHeader shdr : elf.getListSectionHeader()) {
            if (shdr.getSh_namez().equals(".lib.stub")) {
                Memory mem = Memory.get_instance();
                int stubHeadersAddress = (int)(romManager.getBaseoffset() + shdr.getSh_addr());
                int stubHeadersCount = (int)(shdr.getSh_size() / Elf32StubHeader.sizeof());

                Elf32StubHeader stubHeader;
                List<DeferredStub> deferred = new LinkedList<DeferredStub>();
                NIDMapper nidMapper = NIDMapper.get_instance();

                //System.out.println(shdr.getSh_namez() + ":" + stubsCount + " module entries");

                for (int i = 0; i < stubHeadersCount; i++)
                {
                    stubHeader = new Elf32StubHeader(mem, stubHeadersAddress);
                    stubHeader.setModuleNamez(readStringZ(mem.mainmemory, (int)(stubHeader.getOffsetModuleName() - MemoryMap.START_RAM)));
                    stubHeadersAddress += Elf32StubHeader.sizeof(); //stubHeader.s_size * 4;
                    //System.out.println(stubHeader.toString());

                    for (int j = 0; j < stubHeader.getImports(); j++)
                    {
                        int nid = mem.read32((int)(stubHeader.getOffsetNid() + j * 4));
                        int importAddress = (int)(stubHeader.getOffsetText() + j * 8);
                        int exportAddress;
                        int code;

                        // Attempt to fixup stub to point to an already loaded module export
                        exportAddress = nidMapper.moduleNidToAddress(stubHeader.getModuleNamez(), nid);
                        if (exportAddress != -1)
                        {
                            int instruction = // j <jumpAddress>
                                ((jpcsp.AllegrexOpcodes.J & 0x3f) << 26)
                                | ((exportAddress >>> 2) & 0x03ffffff);

                            mem.write32(importAddress, instruction);

                            System.out.println("Mapped NID " + Integer.toHexString(nid) + " to export");
                        }

                        // Attempt to fixup stub to known syscalls
                        else
                        {
                            code = nidMapper.nidToSyscall(nid);
                            if (code != -1)
                            {
                                // Fixup stub, replacing nop with syscall
                                int instruction = // syscall <code>
                                    ((jpcsp.AllegrexOpcodes.SPECIAL & 0x3f) << 26)
                                    | (jpcsp.AllegrexOpcodes.SYSCALL & 0x3f)
                                    | ((code & 0x000fffff) << 6);

                                mem.write32(importAddress + 4, instruction);

                                //System.out.println("Mapped NID " + Integer.toHexString(nid) + " to syscall " + Integer.toHexString(code));
                            }
                            else
                            {
                                // Save nid for deferred fixup
                                deferred.add(new DeferredStub(stubHeader.getModuleNamez(), importAddress, nid));
                                System.out.println("Failed to map NID " + Integer.toHexString(nid) + " (load time)");
                            }
                        }
                    }
                }

                romManager.addDeferredImports(deferred);
            }
        }
    }

    private void initCpuBy(Elf32 elf) {
        //set the default values for registers not sure if they are correct and UNTESTED!!
        //some settings from soywiz/pspemulator
        cpu.pc = (int) romManager.getBaseoffset() + (int) elf.getHeader().getE_entry(); //set the pc register.
        cpu.npc = cpu.pc + 4;
        cpu.gpr[4] = 0; //a0
        cpu.gpr[5] = (int) romManager.getBaseoffset() + (int) elf.getHeader().getE_entry(); // argumentsPointer a1 reg
        cpu.gpr[6] = 0; //a2
        cpu.gpr[26] = 0x09F00000; //k0
        cpu.gpr[27] = 0; //k1 should probably be 0
        cpu.gpr[28] = (int) romManager.getBaseoffset() + (int) romManager.getPSPModuleInfo().getM_gp(); //gp reg    gp register should get the GlobalPointer!!!
        cpu.gpr[29] = 0x09F00000; //sp
        cpu.gpr[31] = 0x08000004; //ra, should this be 0?
        // All other registers are uninitialised/random values

        jpcsp.HLE.ThreadMan.get_instance().Initialise(cpu.pc, romManager.getPSPModuleInfo().getM_attr());
        jpcsp.HLE.Utils.get_instance().Initialise();
        jpcsp.HLE.pspdisplay.get_instance().Initialise();
    }

    private void initDebugWindowsByPbp() {
    }

    private void initDebugWindowsByElf32() {
    }

    private void initNewPsp() {
        getProcessor().reset();
        Memory.get_instance().NullMemory();
        gpu.clean();
        NIDMapper.get_instance().Initialise("syscalls.txt", "FW 1.50");
    }

    public void run() throws GeneralJpcspException {
        // basic code, just one thread by now... it's just a view
       // run = true;
        while (run == true) {
            cpu.step();
            jpcsp.HLE.ThreadMan.get_instance().step();
            gpu.draw();
            controller.checkControllerState();
            //delay(cpu.numberCyclesDelay());
        }
    }

    public void pause() {
        pause = true;
        run = resume = stop = false;
    }

    public void resume() throws GeneralJpcspException {
        resume = true;
        run = pause = stop = false;
        run();
    }

    public void stop() {
        //probally make more and more!!!... stuffs here
        stop = true ;
        run = resume = pause = false;
    }

    public static Processor getProcessor() {
        return cpu;
    }

    public static Memory getMemory() {
        return Memory.get_instance();
    }
}
