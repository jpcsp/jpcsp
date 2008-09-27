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
import java.nio.ByteBuffer;
import java.util.List;
import java.util.LinkedList;

import org.apache.log4j.Logger;

import jpcsp.Allegrex.CpuState;
import jpcsp.Debugger.MemoryViewer;
import jpcsp.Debugger.DisassemblerModule.DisassemblerFrame;
import jpcsp.HLE.Modules;
import jpcsp.format.DeferredStub;
import jpcsp.format.Elf32;
import jpcsp.format.Elf32Relocate;
import jpcsp.format.Elf32SectionHeader;
import jpcsp.format.Elf32SectionHeader.ShType;
import jpcsp.format.Elf32StubHeader;
import static jpcsp.util.Utilities.*;

public class Emulator implements Runnable {
public static String ElfInfo, ProgInfo, PbpInfo, SectInfo;
    private static Processor processor;
    private static Recompiler recompiler;
    private static Controller controller;
    private FileManager romManager;
    private boolean mediaImplemented = false;
    private Thread mainThread;
    public static boolean run = false;
    public static boolean pause = false;
    private static MainGUI gui;
    private static DisassemblerFrame debugger;
    private static MemoryViewer memview;
    private String pspfilename;
    public static Logger log = Logger.getLogger("misc");

    // For instruction counter
    public static int[] textsection = new int[2];
    public static int[] initsection = new int[2];
    public static int[] finisection = new int[2];
    public static int[] Stubtextsection=new int[2];

    public Emulator(MainGUI gui) {
        Emulator.gui = gui;
        processor = new Processor();

        if (Settings.get_instance().readBoolOptions("emuoptions/recompiler"))
            recompiler = new Recompiler();
        else
            recompiler = null;

        controller = new Controller();
        mainThread = new Thread(this, "Emu");
    }

    /* unused ?
    public void load(SeekableDataInput f) throws IOException
    {
        this.pspfilename = "";

        initNewPsp();
        romManager = new FileManager(f);
        initElf32();

        // Delete breakpoints and reset to PC
        if (debugger != null) {
            debugger.resetDebugger();
        }
    }
    */

    public void load(String pspfilename, ByteBuffer f) throws IOException {
        this.pspfilename = pspfilename;

        //  here load fileName, iso or etc...
        processLoading(f);
        if (!mediaImplemented) {
            throw new IOException("This kind of file format still not supported.");
        }

        // Delete breakpoints and reset to PC
        if (debugger != null) {
            debugger.resetDebugger();
        }
    }

    private void processLoading(ByteBuffer f) throws IOException {
        initNewPsp();
        romManager = new FileManager(f);

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
            case FileManager.FORMAT_PSP:
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
                if (shdr.getSh_type() == ShType.PRXREL.getValue() /*|| // 0x700000A0
                        shdr.getSh_type() == ShType.REL.getValue()*/) // 0x00000009
                {
                    Elf32Relocate rel = new Elf32Relocate();
                    romManager.getActualFile().position((int)(romManager.getElfoffset() + shdr.getSh_offset()));

                    int RelCount = (int) shdr.getSh_size() / Elf32Relocate.sizeof();
                    Memory.log.debug(shdr.getSh_namez() + ": relocating " + RelCount + " entries");

                    int AHL = 0; // (AHI << 16) | (ALO & 0xFFFF)

                    int HI_addr = 0; // We'll use this to relocate R_MIPS_HI16 when we get a R_MIPS_LO16

                    for (int i = 0; i < RelCount; i++) {
                        rel.read(romManager.getActualFile());

                        int R_TYPE = (int) (rel.getR_info() & 0xFF);
                        int OFS_BASE = (int) ((rel.getR_info() >> 8) & 0xFF);
                        int ADDR_BASE = (int) ((rel.getR_info() >> 16) & 0xFF);
                        //System.out.println("type=" + R_TYPE + ",base=" + OFS_BASE + ",addr=" + ADDR_BASE + "");

                        int data = Memory.getInstance().read32((int) romManager.getBaseoffset() + (int) rel.getR_offset());
                        long result = 0; // Used to hold the result of relocation, OR this back into data

                        // these are the addends?
                        // SysV ABI MIPS quote: "Because MIPS uses only Elf32_Rel re-location entries, the relocated field holds the addend."
                        int half16 = data & 0x0000FFFF; // 31/07/08 unused (fiveofhearts)

                        int word32 = data & 0xFFFFFFFF; // <=> data;
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


                                    result = AHL + S;
                                    data &= ~0x0000FFFF;
                                    data |= result & 0x0000FFFF; // truncate

                                    // Process deferred R_MIPS_HI16
                                    int data2 = Memory.getInstance().read32(HI_addr);
                                    data2 &= ~0x0000FFFF;
                                    data2 |= (result >> 16) & 0x0000FFFF; // truncate

                                    Memory.getInstance().write32(HI_addr, data2);
                                break;

                            case 4: //R_MIPS_26
                                A = targ26;

                                // docs say "sign-extend(A < 2)", but is it meant to be A << 2? if so then there's no point sign extending
                                //result = (sign-extend(A < 2) + S) >> 2;
                                //result = (((A < 2) ? 0xFFFFFFFF : 0x00000000) + S) >> 2;
                                result = ((A << 2) + S) >> 2; // copied from soywiz/pspemulator

                                data &= ~0x03FFFFFF;
                                data |= (int) (result & 0x03FFFFFF); // truncate
                                break;

                            case 2: //R_MIPS_32
                                data += S;
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
                            	Memory.log.warn("Unhandled relocation type " + R_TYPE + " at " + String.format("%08x", (int) romManager.getBaseoffset() + (int) rel.getR_offset()));
                                break;
                        }

                        //System.out.println("Relocation type " + R_TYPE + " at " + String.format("%08x", (int)baseoffset + (int)rel.r_offset));
                        Memory.getInstance().write32((int) romManager.getBaseoffset() + (int) rel.getR_offset(), data);
                    }
                }
            }
        }
        int numberoffailedNIDS=0;
        int numberofmappedNIDS=0;
        // Imports
        for (Elf32SectionHeader shdr : elf.getListSectionHeader()) {
            if (shdr.getSh_namez().equals(".lib.stub")) {
                Memory mem = Memory.getInstance();
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

                            Modules.log.debug("Mapped NID " + Integer.toHexString(nid) + " to export");
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
                                numberofmappedNIDS++;
                                //System.out.println("Mapped NID " + Integer.toHexString(nid) + " to syscall " + Integer.toHexString(code));
                            }
                            else
                            {
                                // Save nid for deferred fixup
                                deferred.add(new DeferredStub(stubHeader.getModuleNamez(), importAddress, nid));
                                Modules.log.warn("Failed to map NID " + Integer.toHexString(nid) + " (load time)");
                                numberoffailedNIDS++;
                            }
                        }
                    }
                }

                romManager.addDeferredImports(deferred);
            }
            //the following are used for the instruction counter panel
            if(shdr.getSh_namez().equals(".text"))
            {
                textsection[0] = (int)(romManager.getBaseoffset() + shdr.getSh_addr());
                textsection[1] = (int)shdr.getSh_size();
            }
            if(shdr.getSh_namez().equals(".init"))
            {
                initsection[0] = (int)(romManager.getBaseoffset() + shdr.getSh_addr());
                initsection[1] = (int)shdr.getSh_size();
            }
            if(shdr.getSh_namez().equals(".fini"))
            {
                finisection[0] = (int)(romManager.getBaseoffset() + shdr.getSh_addr());
                finisection[1] = (int)shdr.getSh_size();
            }
            if(shdr.getSh_namez().equals(".sceStub.text"))
            {
                Stubtextsection[0] = (int)(romManager.getBaseoffset() + shdr.getSh_addr());
                Stubtextsection[1] = (int)shdr.getSh_size();
            }

            //test the instruction counter
            //if (/*shdr.getSh_namez().equals(".text") || */shdr.getSh_namez().equals(".init") /*|| shdr.getSh_namez().equals(".fini")*/) {
            /*
               int sectionAddress = (int)(romManager.getBaseoffset() + shdr.getSh_addr());
               System.out.println(Integer.toHexString(sectionAddress) + " size = " + shdr.getSh_size());
               for(int i =0; i< shdr.getSh_size(); i+=4)
               {
                 int memread32 = Memory.get_instance().read32(sectionAddress+i);
                 //System.out.println(memread32);
                 jpcsp.Allegrex.Decoder.instruction(memread32).increaseCount();
               }


            }
            System.out.println(jpcsp.Allegrex.Instructions.ADDIU.getCount());*/
        }
        Modules.log.info(numberofmappedNIDS + " NIDS mapped");
        if(numberoffailedNIDS>0) Modules.log.warn("Total Failed to map NIDS = " + numberoffailedNIDS);
    }

    private void initCpuBy(Elf32 elf) {
        //set the default values for registers not sure if they are correct and UNTESTED!!
        //some settings from soywiz/pspemulator
        CpuState cpu = processor.cpu;

        cpu.pc = (int)(romManager.getBaseoffset() + elf.getHeader().getE_entry()); //set the pc register.
        cpu.npc = cpu.pc + 4;
        // Gets set in ThreadMan cpu.gpr[4] = 0; //a0
        // Gets set in ThreadMan cpu.gpr[5] = (int) romManager.getBaseoffset() + (int) elf.getHeader().getE_entry(); // argumentsPointer a1 reg
        //cpu.gpr[6] = 0; //a2
        // Gets set in ThreadMan cpu.gpr[26] = 0x09F00000; //k0
        cpu.gpr[27] = 0; //k1 should probably be 0
        cpu.gpr[28] = (int)(romManager.getBaseoffset() + romManager.getPSPModuleInfo().getM_gp()); //gp reg    gp register should get the GlobalPointer!!!
        // Gets set in ThreadMan cpu.gpr[29] = 0x09F00000; //sp
        // Gets set in ThreadMan cpu.gpr[31] = 0x08000004; //ra, should this be 0?
        // All other registers are uninitialised/random values

        jpcsp.HLE.modules.HLEModuleManager.get_instance().Initialise();
        jpcsp.HLE.pspSysMem.get_instance().Initialise(romManager.getLoadAddressLow(), romManager.getLoadAddressHigh() - romManager.getLoadAddressLow());
        jpcsp.HLE.ThreadMan.get_instance().Initialise(cpu.pc, romManager.getPSPModuleInfo().getM_attr(), pspfilename);
        jpcsp.HLE.psputils.get_instance().Initialise();
        jpcsp.HLE.pspge.get_instance().Initialise();
        jpcsp.HLE.pspdisplay.get_instance().Initialise();
        jpcsp.HLE.pspiofilemgr.get_instance().Initialise();
    }

    private void initDebugWindowsByPbp() {
    }

    private void initDebugWindowsByElf32() {
    }

    private void initNewPsp() {
        getProcessor().reset();
        Memory.getInstance().Initialise();
        NIDMapper.get_instance().Initialise();

        if (memview != null)
            memview.RefreshMemory();
    }

    @Override
    public void run()
    {
        while (true) {
            try {
             synchronized(this) {
                    while (pause)
                        wait();
                }
            } catch (InterruptedException e){
            }

            if (recompiler != null) {
                recompiler.run();
            } else {
                processor.step();
                jpcsp.HLE.pspge.get_instance().step();
                jpcsp.HLE.ThreadMan.get_instance().step();
                jpcsp.HLE.pspdisplay.get_instance().step();
                jpcsp.HLE.modules.HLEModuleManager.get_instance().step();
                controller.checkControllerState();

                if (debugger != null)
                    debugger.step();
                //delay(cpu.numberCyclesDelay());
            }
        }

    }
    public synchronized void RunEmu()
    {
        if (!mediaImplemented)
            return;

        if (pause)
        {
            pause = false;
            notify();
        }
        else if (!run)
        {
            run = true;
            mainThread.start();
        }

        jpcsp.HLE.ThreadMan.get_instance().clearSyscallFreeCycles();

        gui.RefreshButtons();
        if (debugger != null)
            debugger.RefreshButtons();
    }
    // static so Memory can pause emu on invalid read/write
    public static synchronized void PauseEmu()
    {
        if (run && !pause)
        {
            pause = true;

            gui.RefreshButtons();

            if (debugger != null)
                debugger.RefreshButtons();

            if (memview != null)
                memview.RefreshMemory();
        }
    }
    public static void setFpsTitle(String fps)
    {
         gui.setMainTitle(fps);
    }
    public static Processor getProcessor() {
        return processor;
    }

    public static Memory getMemory() {
        return Memory.getInstance();
    }

    public static Controller getController() {
        return controller;
    }

    public void setDebugger(DisassemblerFrame debugger) {
        this.debugger = debugger;
    }

    public void setMemoryViewer(MemoryViewer memview) {
        this.memview = memview;
    }
}
