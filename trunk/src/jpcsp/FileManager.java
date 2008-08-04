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

import jpcsp.format.Elf32ProgramHeader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.LinkedList;
import java.util.List;
import jpcsp.ElfHeader.ShFlags;
import jpcsp.format.Elf32;
import jpcsp.format.Elf32Header;
import jpcsp.format.Elf32SectionHeader;
import jpcsp.format.PBP;
import jpcsp.format.PSPModuleInfo;
import jpcsp.format.Elf32Relocate;
import jpcsp.util.Utilities;

public class FileManager {
    // TODO : Define a way to use this insteast ElfHeader.ElfInfo; ElfHeader.PbpInfo; ElfHeader.SectInfo

    public static String ElfInfo,  PbpInfo,  SectInfo; // TODO : think a better way

    private PSPModuleInfo moduleInfo;
    private PBP pbp;
    private Elf32 elf;
    private Processor cpu;
    private RandomAccessFile actualFile;
    private String filePath;
    public final static int FORMAT_ELF = 0;
    public final static int FORMAT_PBP = 10;
    public final static int FORMAT_UMD = 20;
    public final static int FORMAT_ISO = 30;
    private int type = -1;
    private long elfoffset = 0;
    private long baseoffset = 0;
    private boolean relocate = false;

    public FileManager(String filePath, Processor cpu) throws FileNotFoundException, IOException {
        this.filePath = filePath;
        this.cpu = cpu; // I want to move this... the cpu behavior can't be here
        this.cpu.reset();
        loadAndDefine(filePath);
    }

    public PSPModuleInfo getPSPModuleInfo() {
        return moduleInfo;
    }

    public PBP getPBP() {
        return pbp;
    }

    public Elf32 getElf32() {
        return elf;
    }

    private RandomAccessFile getActualFile() {
        return actualFile;
    }

    private void setActualFile(RandomAccessFile f) {
        actualFile = f;
    }

    private void loadAndDefine(String filePath) throws FileNotFoundException, IOException {
        RandomAccessFile f = new RandomAccessFile(filePath, "r");
        setActualFile(f);
        try {
            elfoffset = 0;
            baseoffset = 0;
            relocate = false;

            moduleInfo = new PSPModuleInfo();

            //makes sense put the more used first...
            
            /*try pbp format*/
            pbp = new PBP(getActualFile());
            processPbp();
            if (getType() == FORMAT_PBP) {
                return;
            }
            /*end try pbp format*/
            
            /*try elf32 format*/
            elf = new Elf32(getActualFile());
            processElf();
            if (getType() == FORMAT_ELF) {
                return; 
            }
            /*end try elf32 format*/
            
            
            /*try xxxx format*/
            /*try xxxx format*/
            
            
            //NONE FORMAT SELECTED OR DETECTED :( 
        } finally {
            f.close(); // close or let it open... 

        }
    }

    public int getType() {
        return type;
    }
 
    private Elf32SectionHeader firstStep(Elf32Header elf32, RandomAccessFile f, List<Elf32SectionHeader> sectionheaders) throws IOException {
        /** Read the ELF section headers (1st pass) */
        sectionheaders = new LinkedList<Elf32SectionHeader>();
        Elf32SectionHeader shstrtab = null;
        for (int i = 0; i < elf32.getE_shnum(); i++) {
            f.seek(elfoffset + elf32.getE_shoff() + (i * elf32.getE_shentsize()));
            Elf32SectionHeader shdr = new Elf32SectionHeader(f);
            sectionheaders.add(shdr);

            if (shdr.getSh_type() == 3 && shstrtab == null) //ShType.STRTAB
            {
                shstrtab = shdr;
            }

            // Load some sections into memory
            if ((shdr.getSh_flags() & ShFlags.Allocate.getValue()) == ShFlags.Allocate.getValue()) {
                switch (shdr.getSh_type()) {
                    case 1: //ShType.PROGBITS
                        //System.out.println("FEED MEMORY WITH IT!");

                        f.seek(elfoffset + shdr.getSh_offset());
                        int offsettoread = (int) getBaseoffset() + (int) shdr.getSh_addr() - MemoryMap.START_RAM;
                        f.read(Memory.get_instance().mainmemory, offsettoread, (int) shdr.getSh_size());
                        break;
                    case 8: // ShType.NOBITS

                        System.out.println("NO BITS");
                        // zero out this memory(?), from shdr.sh_addr to shdr.sh_addr + shdr.sh_size
                        break;
                }
            }
        }

        return shstrtab;
    }

    private void processElf() throws IOException {
        readElf32Header(getElf32().getHeader()); // TODO: I must set the type of media; when I know that it is an elf32 format valid?
        readElfProgramHeaders(getElf32().getHeader(), getActualFile());
        readElfSectionHeaders(getElf32().getHeader(), getActualFile(), getPSPModuleInfo()); // I can set the type for FORMAT_ELF
    }

    private void readElf32Header(Elf32Header elf32) {
        if (!elf32.isValid()) {
            System.out.println("NOT AN ELF FILE");
        } else {
            type = FORMAT_ELF;
        }
        if (!elf32.isMIPSExecutable()) {
            System.out.println("NOT A MIPS executable");
        }
        ElfInfo = elf32.toString(); //better use Elf32Header.getInfo()...    



        if (elf32.isPRXDetected()) {
            System.out.println("PRX detected, requires relocation");
            baseoffset = 0x08900000;
            relocate = true;
        } else if (elf32.requiresRelocation()) {
            // seen in .elf's generated by pspsdk with BUILD_PRX=1 before conversion to .prx
            System.out.println("ELF requires relocation");
            baseoffset = 0x08900000;
            relocate = true;
        }
    }

    private void processPbp() throws IOException {

        if (getPBP().isValid()) {
            
            if (Settings.get_instance().readBoolEmuoptions("pbpunpack")){
                getPBP().unpackPBP(getActualFile());
            }
            elfoffset = getPBP().getOffset_psp_data();
            getActualFile().seek(elfoffset); //seek the new offset
            PbpInfo = getPBP().toString(); //inteast this use PBP.getInfo()
            type = FORMAT_PBP;
        } else {
            elfoffset = 0;
            getActualFile().seek(0);
            PBP.setInfo("-----NOT A PBP FILE---------\n");
        }
    }

    private void readElfProgramHeaders(Elf32Header elf32, RandomAccessFile f) throws IOException {
        List<Elf32ProgramHeader> programheaders = new LinkedList<Elf32ProgramHeader>();
        StringBuffer phsb = new StringBuffer();

        for (int i = 0; i < elf32.getE_phnum(); i++) {
            f.seek(elfoffset + elf32.getE_phoff() + (i * elf32.getE_phentsize()));
            Elf32ProgramHeader phdr = new Elf32ProgramHeader(f);
            programheaders.add(phdr);

            phsb.append("-----PROGRAM HEADER #" + i + "-----" + "\n");
            phsb.append(phdr.toString());

            // yapspd: if the PRX file is a kernel module then the most significant
            // bit must be set in the phsyical address of the first program header.
            if (i == 0 && (phdr.getP_paddr() & 0x80000000L) == 0x80000000L) {
                // kernel mode prx
                System.out.println("Kernel mode PRX detected");
            }
            //SegInfo = phsb.toString();
            ElfInfo += phsb.toString();
        }
    }
    
    private void readElfSectionHeaders(Elf32Header elf32, RandomAccessFile f,PSPModuleInfo moduleinfo) throws IOException {
        List<Elf32SectionHeader> sectionheaders = new LinkedList<Elf32SectionHeader>(); //use in more than one step
        Elf32SectionHeader shstrtab = null; //use in more than one step
        shstrtab = firstStep(elf32, f, sectionheaders);
        secondStep(sectionheaders, shstrtab, f, moduleinfo);
        thirdStep(sectionheaders, f, moduleinfo); //load the main memory?
    }

    private void secondStep(List<Elf32SectionHeader> sectionheaders, Elf32SectionHeader shstrtab, RandomAccessFile f,PSPModuleInfo moduleinfo) throws IOException {
        // 2nd pass generate info string for the GUI and get module infos
        //moduleinfo = new PSPModuleInfo(); moved to loadAndDefine()
         StringBuffer shsb = new StringBuffer();
        int SectionCounter = 0;
        for (Elf32SectionHeader shdr : sectionheaders) {
            // Number the section
            shsb.append("-----SECTION HEADER #" + SectionCounter + "-----" + "\n");

            // Resolve section name (if possible)
            if (shstrtab != null) {
                f.seek(elfoffset + shstrtab.getSh_offset() + shdr.getSh_name());
                String SectionName = Utilities.readStringZ(f);
                if (SectionName.length() > 0) {
                    shdr.setSh_namez(SectionName);
                    shsb.append(SectionName + "\n");

                    // Get module infos
                    if (SectionName.matches(".rodata.sceModuleInfo")) {
                        f.seek(elfoffset + shdr.getSh_offset());
                        moduleinfo.read(f);
                        //System.out.println(Long.toHexString(moduleinfo.m_gp));

                        System.out.println("Found ModuleInfo name:'" + moduleinfo.getM_namez() + "' version:" + Utilities.formatString("short", Integer.toHexString(moduleinfo.getM_version() & 0xFFFF).toUpperCase()));

                        if ((moduleinfo.getM_attr() & 0x1000) != 0) {
                            System.out.println("Kernel mode module detected");
                        }
                        if ((moduleinfo.getM_attr() & 0x0800) != 0) {
                            System.out.println("VSH mode module detected");
                        }
                    }
                }
            }

            // Add the normal info
            shsb.append(shdr.toString());
            SectionCounter++;
        }
        SectInfo = shsb.toString();

    }

    private void thirdStep(List<Elf32SectionHeader> sectionheaders, RandomAccessFile f,PSPModuleInfo moduleinfo) throws IOException {
        // 3rd pass relocate
        if (relocate) {
            for (Elf32SectionHeader shdr : sectionheaders) {
                if (shdr.getSh_type() == 0x700000A0 || // PRX reloc magic
                        shdr.getSh_type() == 0x00000009) //ShType.REL
                {
                    Elf32Relocate rel = new Elf32Relocate();
                    f.seek(elfoffset + shdr.getSh_offset());

                    int RelCount = (int) shdr.getSh_size() / Elf32Relocate.sizeof();
                    System.out.println(shdr.getSh_namez() + ": relocating " + RelCount + " entries");

                    int AHL = 0; // (AHI << 16) | (ALO & 0xFFFF)

                    int HI_addr = 0; // We'll use this to relocate R_MIPS_HI16 when we get a R_MIPS_LO16

                    // Relocation modes, only 1 is allowed at a time
                    boolean external = true; // copied from soywiz/pspemulator

                    boolean local = false;
                    boolean _gp_disp = false;

                    for (int i = 0; i < RelCount; i++) {
                        rel.read(f);

                        int R_TYPE = (int) (rel.getR_info() & 0xFF);
                        int OFS_BASE = (int) ((rel.getR_info() >> 8) & 0xFF);
                        int ADDR_BASE = (int) ((rel.getR_info() >> 16) & 0xFF);
                        //System.out.println("type=" + R_TYPE + ",base=" + OFS_BASE + ",addr=" + ADDR_BASE + "");

                        int data = Memory.get_instance().read32((int) getBaseoffset() + (int) rel.getR_offset());
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

                        int P = (int) getBaseoffset() + (int) rel.getR_offset(); // address of instruction being relocated? 31/07/08 unused when external=true (fiveofhearts)

                        int S = (int) getBaseoffset(); // ? copied from soywiz/pspemulator, but doesn't match the docs (fiveofhearts)

                        int G = 0; // ? 31/07/08 unused (fiveofhearts)

                        int GP = (int) getBaseoffset() + (int) moduleinfo.getM_gp(); // final gp value, computed correctly? 31/07/08 only used in R_MIPS_GPREL16 which is untested (fiveofhearts)

                        int GP0 = (int) moduleinfo.getM_gp(); // gp value, computed correctly? 31/07/08 unused when external=true (fiveofhearts)

                        int EA = 0; // ? 31/07/08 unused (fiveofhearts)

                        int L = 0; // ? 31/07/08 unused (fiveofhearts)

                        switch (R_TYPE) {
                            case 0: //R_MIPS_NONE
                                // Don't do anything

                                break;

                            case 5: //R_MIPS_HI16

                                A = hi16;
                                AHL = A << 16;
                                HI_addr = (int) getBaseoffset() + (int) rel.getR_offset();
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
                                System.out.println("Unhandled relocation type " + R_TYPE + " at " + String.format("%08x", (int) getBaseoffset() + (int) rel.getR_offset()));
                                break;
                        }

                        //System.out.println("Relocation type " + R_TYPE + " at " + String.format("%08x", (int)baseoffset + (int)rel.r_offset));
                        Memory.get_instance().write32((int) getBaseoffset() + (int) rel.getR_offset(), data);
                    }
                }
            }
        }

    }

    public long getBaseoffset() {
        return baseoffset;
    }
}
