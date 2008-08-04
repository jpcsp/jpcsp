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

package jpcsp.format;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.List;
import java.util.LinkedList;
import jpcsp.ElfHeader;
import jpcsp.Memory;
import jpcsp.MemoryMap;
import jpcsp.Processor;
import jpcsp.Settings;
import static jpcsp.util.Utilities.*;

/**
 *
 * @author George
 */
public class ELFLoader {
    public static String ElfInfo, PbpInfo, SectInfo;

    enum ShFlags {
        None(0),
        Write(1),
        Allocate(2),
        Execute(4);

        private int value;
        private ShFlags(int val)
        {
            value=val;
        }
        int getValue()
        {
            return value;
        }
    }

    enum ShType {
        NULL(0x00000000),
        PROGBITS(0x00000001),
        SYMTAB(0x00000002),
        STRTAB(0x00000003),
        RELA(0x00000004),
        HASH(0x00000005),
        DYNAMIC(0x00000006),
        NOTE(0x00000007),
        NOBITS(0x00000008),
        REL(0x00000009),
        SHLIB(0x0000000a),
        DYNSYM(0x0000000b),
        PRXREL(0x700000A0);

        private int value;
        private ShType(int val)
        {
            value=val;
        }
        int getValue()
        {
            return value;
        }
    }

    private static class Elf32_Ehdr
    {
        private static final long ELF_MAGIC = 0x464C457FL;

        private long e_magic;
        private int e_class;
        private int e_data;
        private int e_idver;
        private byte[] e_pad = new byte[9];
        private int e_type;
        private int e_machine;
        private long e_version;
        private long e_entry;
        private long e_phoff;
        private long e_shoff;
        private long e_flags;
        private int e_ehsize;
        private int e_phentsize;
        private int e_phnum;
        private int e_shentsize;
        private int e_shnum;
        private int e_shstrndx;

        public static int sizeof() { return 52; } // check
        private Elf32_Ehdr(RandomAccessFile f) throws IOException
        {
            e_magic = readUWord(f);
            if (isValid())
            {
                e_class = readUByte(f);
                e_data = readUByte(f);
                e_idver = readUByte(f);
                f.readFully(e_pad);
                e_type = readUHalf(f);
                e_machine = readUHalf(f);
                e_version = readUWord(f);
                e_entry = readUWord(f);
                e_phoff = readUWord(f);
                e_shoff = readUWord(f);
                e_flags = readUWord(f);
                e_ehsize = readUHalf(f);
                e_phentsize = readUHalf(f);
                e_phnum = readUHalf(f);
                e_shentsize = readUHalf(f);
                e_shnum = readUHalf(f);
                e_shstrndx = readUHalf(f);
            }
        }

        public boolean isValid()
        {
            return ((e_magic & 0xFFFFFFFFL) == ELF_MAGIC);
        }

        @Override
        public String toString()
        {
            StringBuffer str = new StringBuffer();
            str.append("-----ELF HEADER---------" + "\n");
            str.append("e_magic " + "\t " +  formatString("long", Long.toHexString(e_magic & 0xFFFFFFFFL).toUpperCase()) + "\n");
            str.append("e_class " + "\t " +  integerToHex(e_class & 0xFF ) + "\n");
            // str.append("e_class " + "\t " +  formatString("byte", Integer.toHexString(e_class & 0xFF ).toUpperCase())+ "\n");
            str.append("e_data " + "\t\t " + formatString("byte", Integer.toHexString(e_data & 0xFF ).toUpperCase())+ "\n");
            str.append("e_idver " + "\t " + formatString("byte", Integer.toHexString(e_idver & 0xFF).toUpperCase())+ "\n");
            str.append("e_type " + "\t\t " + formatString("short",Integer.toHexString(e_type & 0xFFFF).toUpperCase())+ "\n");
            str.append("e_machine " + "\t " + formatString("short",Integer.toHexString(e_machine & 0xFFFF).toUpperCase())+ "\n");
            str.append("e_version " + "\t " + formatString("long",Long.toHexString(e_version & 0xFFFFFFFFL).toUpperCase())+ "\n");
            str.append("e_entry " + "\t " + formatString("long",Long.toHexString(e_entry & 0xFFFFFFFFL).toUpperCase())+ "\n");
            str.append("e_phoff "+ "\t " + formatString("long",Long.toHexString(e_phoff & 0xFFFFFFFFL).toUpperCase())+ "\n");
            str.append("e_shoff "+ "\t " + formatString("long",Long.toHexString(e_shoff  & 0xFFFFFFFFL).toUpperCase())+ "\n");
            str.append("e_flags "+ "\t " + formatString("long",Long.toHexString(e_flags & 0xFFFFFFFFL).toUpperCase())+ "\n");
            str.append("e_ehsize "+ "\t " + formatString("short",Integer.toHexString(e_ehsize& 0xFFFF).toUpperCase())+ "\n");
            str.append("e_phentsize " + "\t " + formatString("short",Integer.toHexString(e_phentsize& 0xFFFF).toUpperCase())+ "\n");
            str.append("e_phnum " + "\t " + formatString("short",Integer.toHexString(e_phnum& 0xFFFF).toUpperCase())+ "\n");
            str.append("e_shentsize " + "\t " + formatString("short",Integer.toHexString(e_shentsize& 0xFFFF).toUpperCase())+ "\n");
            str.append("e_shnum " + "\t " + formatString("short",Integer.toHexString(e_shnum& 0xFFFF).toUpperCase())+ "\n");
            str.append("e_shstrndx "+ "\t " + formatString("short",Integer.toHexString(e_shstrndx& 0xFFFF).toUpperCase())+ "\n");
            return str.toString();
        }
    }

    private static class Elf32_Phdr
    {
        private long p_type;
        private long p_offset;
        private long p_vaddr;
        private long p_paddr;
        private long p_filesz;
        private long p_memsz;
        private long p_flags; // Bits: 0x1=executable, 0x2=writable, 0x4=readable, demo PRX's were found to be not writable
        private long p_align;

        private static int sizeof() { return 32; }
        private Elf32_Phdr(RandomAccessFile f) throws IOException
        {
            p_type = readUWord(f);
            p_offset = readUWord(f);
            p_vaddr = readUWord(f);
            p_paddr = readUWord(f);
            p_filesz = readUWord(f);
            p_memsz = readUWord(f);
            p_flags = readUWord(f);
            p_align = readUWord(f);
        }

        public String toString()
        {
            StringBuffer str = new StringBuffer();
            str.append("p_type " + "\t\t " + formatString("long", Long.toHexString(p_type & 0xFFFFFFFFL).toUpperCase()) + "\n");
            str.append("p_offset " + "\t " + formatString("long", Long.toHexString(p_offset & 0xFFFFFFFFL).toUpperCase()) + "\n");
            str.append("p_vaddr " + "\t " + formatString("long", Long.toHexString(p_vaddr & 0xFFFFFFFFL).toUpperCase()) + "\n");
            str.append("p_paddr " + "\t " + formatString("long", Long.toHexString(p_paddr & 0xFFFFFFFFL).toUpperCase()) + "\n");
            str.append("p_filesz " + "\t " + formatString("long", Long.toHexString(p_filesz & 0xFFFFFFFFL).toUpperCase()) + "\n");
            str.append("p_memsz " + "\t " + formatString("long", Long.toHexString(p_memsz & 0xFFFFFFFFL).toUpperCase()) + "\n");
            str.append("p_flags " + "\t " + formatString("long", Long.toHexString(p_flags & 0xFFFFFFFFL).toUpperCase()) + "\n");
            str.append("p_align " + "\t " + formatString("long", Long.toHexString(p_align & 0xFFFFFFFFL).toUpperCase()) + "\n");
            return str.toString();
        }
    }

    private static class Elf32_Shdr
    {
        private String sh_namez = "";

        private long sh_name;
        private int sh_type;
        private int sh_flags;
        private long sh_addr;
        private long sh_offset;
        private long sh_size;
        private int sh_link;
        private int sh_info;
        private int sh_addralign;
        private long sh_entsize;

        private static int sizeof() { return 40; }
        private Elf32_Shdr(RandomAccessFile f) throws IOException
        {
            sh_name = readUWord(f);
            sh_type = readWord(f);
            sh_flags = readWord(f);
            sh_addr = readUWord(f);
            sh_offset = readUWord(f);
            sh_size = readUWord(f);
            sh_link = readWord(f);
            sh_info = readWord(f);
            sh_addralign = readWord(f);
            sh_entsize = readWord(f);
        }

        public String toString()
        {
            StringBuffer str = new StringBuffer();
            str.append("sh_name " + "\t " + formatString("long", Long.toHexString(sh_name & 0xFFFFFFFFL).toUpperCase()) + "\n");
            str.append("sh_type " + "\t " + formatString("long", Long.toHexString(sh_type & 0xFFFFFFFFL).toUpperCase()) + "\n");
            str.append("sh_flags " + "\t " + integerToHex(sh_flags & 0xFF ) + "\n");
            str.append("sh_addr " + "\t " + formatString("long", Long.toHexString(sh_addr & 0xFFFFFFFFL).toUpperCase()) + "\n");
            str.append("sh_offset " + "\t " + formatString("long", Long.toHexString(sh_offset & 0xFFFFFFFFL).toUpperCase()) + "\n");
            str.append("sh_size " + "\t " + formatString("long", Long.toHexString(sh_size & 0xFFFFFFFFL).toUpperCase()) + "\n");
            str.append("sh_link " + "\t " + integerToHex(sh_link & 0xFF ) + "\n");
            str.append("sh_info " + "\t " + integerToHex(sh_info & 0xFF ) + "\n");
            str.append("sh_addralign " + "\t " + integerToHex(sh_addralign & 0xFF ) + "\n");
            str.append("sh_entsize " + "\t " + formatString("long", Long.toHexString(sh_entsize & 0xFFFFFFFFL).toUpperCase()) + "\n");
            return str.toString();
        }
    }

    private static class Elf32_Rel
    {
        private long r_offset;
        private long r_info;

        private static int sizeof() { return 8; }
        private Elf32_Rel(RandomAccessFile f) throws IOException
        {
            r_offset = readUWord (f);
            r_info = readUWord (f);
        }

        public String toString()
        {
            StringBuffer str = new StringBuffer();
            str.append("r_offset " + "\t " +  formatString("long", Long.toHexString(r_offset & 0xFFFFFFFFL).toUpperCase()) + "\n");
            str.append("r_info " + "\t\t " +  formatString("long", Long.toHexString(r_info & 0xFFFFFFFFL).toUpperCase()) + "\n");
            return str.toString();
        }
    }

    private static class PSPModuleInfo
    {
        private String m_namez = ""; // String version of m_name

        private int m_attr;
        private int m_version;
        private byte[] m_name = new byte[28];
        private long m_gp;
        private long m_exports; // .lib.ent
        private long m_exp_end;
        private long m_imports; // .lib.stub
        private long m_imp_end;

        private static int sizeof() { return 52; } // check
        private PSPModuleInfo(RandomAccessFile f) throws IOException
        {
            m_attr = readUHalf(f);
            m_version = readUHalf(f);
            f.readFully(m_name);
            m_gp = readUWord(f);
            m_exports = readUWord(f);
            m_exp_end = readUWord(f);
            m_imports = readUWord(f);
            m_imp_end = readUWord(f);

            // Convert the array of bytes used for the module name to a Java String
            // If we don't do it this way then there will be too many nul-bytes on the end, and some shells print them all!
            int len;
            for (len = 0; len < 28 && m_name[len] != 0; len++);
            m_namez = new String(m_name, 0, len);
        }
    }

    private static void processProgramHeaders(RandomAccessFile f, long elfoffset, Elf32_Ehdr ehdr, List<Elf32_Phdr> programheaders) throws IOException
    {
        StringBuffer phsb = new StringBuffer();
        for (int i = 0; i < ehdr.e_phnum; i++)
        {
            // Read information about this section.
            f.seek(elfoffset + ehdr.e_phoff + (i * ehdr.e_phentsize));
            Elf32_Phdr phdr = new Elf32_Phdr(f);
            programheaders.add(phdr);

            phsb.append("-----PROGRAM HEADER #" + i + "-----" + "\n");
            phsb.append(phdr.toString());

            // yapspd: if the PRX file is a kernel module then the most significant
            // bit must be set in the phsyical address of the first program header.
            if (i == 0 && (phdr.p_paddr & 0x80000000L) == 0x80000000L)
            {
                // kernel mode prx
                System.out.println("Kernel mode PRX detected");
            }
        }
        ElfInfo += phsb.toString();
    }

    // 1st pass:
    // - load all section headers into a temporary list for use in 2nd and 3rd pass
    // - locate the shstrtab section
    // - load required sections into memory
    private static Elf32_Shdr processSectionHeaders1(RandomAccessFile f, long elfoffset, Elf32_Ehdr ehdr, List<Elf32_Shdr> sectionheaders, long loadoffset) throws IOException
    {
        Elf32_Shdr shstrtab = null;

        for (int i = 0; i < ehdr.e_shnum; i++)
        {
            // Read information about this section.
            f.seek(elfoffset + ehdr.e_shoff + (i * ehdr.e_shentsize));
            Elf32_Shdr shdr = new Elf32_Shdr(f);
            sectionheaders.add(shdr);

            // Save the shstrtab for the 2nd pass
            if (shdr.sh_type == ShType.STRTAB.getValue() && shstrtab == null)
            {
                shstrtab = shdr;
            }

            // Load some sections into memory
            if ((shdr.sh_flags & ShFlags.Allocate.getValue()) == ShFlags.Allocate.getValue())
            {
                 switch(shdr.sh_type)
                 {
                     case 1: //ShType.PROGBITS
                         //System.out.println("FEED MEMORY WITH IT!");
                         f.seek(elfoffset + shdr.sh_offset);
                         int offsettoread = (int)(loadoffset + shdr.sh_addr - MemoryMap.START_RAM);
                         f.read(Memory.get_instance().mainmemory, offsettoread, (int)shdr.sh_size);
                         break;

                     case 8: // ShType.NOBITS
                         System.out.println("NO BITS " + String.format("0x%08x to 0x%08x", (int)loadoffset + (int)shdr.sh_addr, (int)loadoffset + (int)shdr.sh_addr + (int)shdr.sh_size));
                         // zero out this memory
                         offsettoread = (int)(loadoffset + shdr.sh_addr - MemoryMap.START_RAM);
                         byte[] mainmemory = Memory.get_instance().mainmemory;
                         for (int j = 0; j < (int)shdr.sh_size; j++)
                             mainmemory[offsettoread + j] = 0;
                         break;
                 }
            }
        }

        return shstrtab;
    }

    // 2nd pass:
    // - generate SectInfo GUI string
    // - resolve section names using shstrtab
    // - process sceModuleInfo
    private static PSPModuleInfo processSectionHeaders2(RandomAccessFile f, long elfoffset, List<Elf32_Shdr> sectionheaders, Elf32_Shdr shstrtab) throws IOException
    {
        StringBuffer shsb = new StringBuffer();
        PSPModuleInfo moduleinfo = null;
        int SectionCounter = 0;

        for (Elf32_Shdr shdr: sectionheaders)
        {
            // Number the section
            shsb.append("-----SECTION HEADER #" + SectionCounter + "-----" + "\n");

            // Resolve section name (if possible)
            // Look for sceModuleInfo and process it
            if (shstrtab != null)
            {
                f.seek(elfoffset + shstrtab.sh_offset + shdr.sh_name);
                String SectionName = readStringZ(f);
                if (SectionName.length() > 0)
                {
                    shdr.sh_namez = SectionName;
                    shsb.append(SectionName + "\n");

                    // Process sceModuleInfo
                    if (SectionName.matches(".rodata.sceModuleInfo"))
                    {
                        f.seek(elfoffset + shdr.sh_offset);
                        moduleinfo = new PSPModuleInfo(f);
                        //System.out.println(Long.toHexString(moduleinfo.m_gp));

                        System.out.println("Found ModuleInfo name:'" + moduleinfo.m_namez
                            + "' version:" + formatString("short", Integer.toHexString(moduleinfo.m_version & 0xFFFF).toUpperCase()));

                        // Print some interesting infos
                        if ((moduleinfo.m_attr & 0x1000) != 0)
                        {
                            System.out.println("Kernel mode module detected");
                        }
                        if ((moduleinfo.m_attr & 0x0800) != 0)
                        {
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

        return moduleinfo;
    }

    // 3rd pass (only call if we need to relocate)
    // - relocate sections
    private static void processRelocationSections(RandomAccessFile f, long elfoffset, List<Elf32_Shdr> sectionheaders, long loadoffset, int gpreg) throws IOException
    {
        for (Elf32_Shdr shdr: sectionheaders)
        {
            if (shdr.sh_type == 0x700000A0 || // PRX reloc magic
                shdr.sh_type == 0x00000009) //ShType.REL
            {
                f.seek(elfoffset + shdr.sh_offset);

                int RelCount = (int)shdr.sh_size / Elf32_Rel.sizeof();
                System.out.println(shdr.sh_namez + ": relocating " + RelCount + " entries");

                int AHL = 0; // (AHI << 16) | (ALO & 0xFFFF)
                int HI_addr = 0; // We'll use this to relocate R_MIPS_HI16 when we get a R_MIPS_LO16

                for (int i = 0; i < RelCount; i++)
                {
                    Elf32_Rel rel = new Elf32_Rel(f);

                    int R_TYPE = (int)(rel.r_info & 0xFF);
                    int OFS_BASE = (int)((rel.r_info >> 8) & 0xFF);
                    int ADDR_BASE = (int)((rel.r_info >> 16) & 0xFF);
                    //System.out.println("type=" + R_TYPE + ",base=" + OFS_BASE + ",addr=" + ADDR_BASE + "");

                    int data = Memory.get_instance().read32((int)(loadoffset + rel.r_offset));
                    long result = 0; // Used to hold the result of relocation, OR this back into data

                    // moved outside the loop so context is saved
                    //int AHL = 0; // (AHI << 16) | (ALO & 0xFFFF)
                    int S = (int)loadoffset; // ? copied from soywiz/pspemulator, but doesn't match the docs (fiveofhearts)
                    int GP = (int)(loadoffset + gpreg); // final gp value, computed correctly? 31/07/08 only used in R_MIPS_GPREL16 which is untested (fiveofhearts)

                    switch(R_TYPE)
                    {
                        case 0: //R_MIPS_NONE
                            // Don't do anything
                            break;

                        case 5: //R_MIPS_HI16
                            AHL = (data & 0x0000FFFF) << 16;
                            HI_addr = (int)(loadoffset + rel.r_offset);
                            break;

                        case 6: //R_MIPS_LO16
                            AHL &= ~0x0000FFFF; // delete lower bits, since many R_MIPS_LO16 can follow one R_MIPS_HI16
                            AHL |= data & 0x0000FFFF;

                            result = AHL + S;
                            data &= ~0x0000FFFF;
                            data |= result & 0x0000FFFF; // truncate

                            // Process deferred R_MIPS_HI16
                            int data2 = Memory.get_instance().read32(HI_addr);
                            data2 &= ~0x0000FFFF;
                            data2 |= (result >> 16) & 0x0000FFFF; // truncate
                            Memory.get_instance().write32(HI_addr, data2);
                            break;

                        case 4: //R_MIPS_26
                            result = (((data & 0x03FFFFFF) << 2) + S) >> 2; // copied from soywiz/pspemulator
                            data &= ~0x03FFFFFF;
                            data |= (int)(result & 0x03FFFFFF); // truncate
                            break;

                        case 2: //R_MIPS_32
                            result = S + (data & 0xFFFFFFFF);
                            data &= ~0xFFFFFFFF;
                            data |= (int)(result & 0xFFFFFFFF); // truncate
                            break;

                        /* sample before relocation: 0x00015020: 0x8F828008 '....' - lw         $v0, -32760($gp)
                        case 7: //R_MIPS_GPREL16
                            // 31/07/08 untested (fiveofhearts)
                            System.out.println("Untested relocation type " + R_TYPE + " at " + String.format("%08x", (int)loadoffset + (int)rel.r_offset));

                            A = data & 0x0000FFFF;

                            //result = sign-extend(A) + S + GP;
                            result = (((A & 0x00008000) != 0) ? A & 0xFFFF0000 : A) + S + GP;

                            // verify
                            if ((result & ~0x0000FFFF) != 0)
                                throw new IOException("Relocation overflow (R_MIPS_GPREL16)");

                            data &= ~0x0000FFFF;
                            data |= (int)(result & 0x0000FFFF);
                            break;
                            */

                        default:
                            System.out.println("Unhandled relocation type " + R_TYPE + " at " + String.format("%08x", (int)(loadoffset + rel.r_offset)));
                            break;
                    }

                    //System.out.println("Relocation type " + R_TYPE + " at " + String.format("%08x", (int)loadoffset + (int)rel.r_offset));
                    Memory.get_instance().write32((int)(loadoffset + rel.r_offset), data);
                }
            }
        }
    }

    private static void LoadELF(Processor p, RandomAccessFile f, long elfoffset) throws IOException
    {
        Elf32_Ehdr ehdr = null;
        List<Elf32_Phdr> programheaders = new LinkedList<Elf32_Phdr>();
        List<Elf32_Shdr> sectionheaders = new LinkedList<Elf32_Shdr>();
        Elf32_Shdr shstrtab = null;
        PSPModuleInfo moduleinfo = null;
        long loadoffset;
        boolean relocate;

        f.seek(elfoffset);
        ehdr = new Elf32_Ehdr(f);

        // Print some infos
        if((ehdr.e_machine & 0xFFFF) != 0x0008)
        {
            System.out.println("NOT A MIPS executable");
        }

        // Find load offset
        if ((ehdr.e_type & 0xFFFF) == 0xFFA0)
        {
            System.out.println("PRX detected, requires relocation");
            loadoffset = 0x08900000;
            relocate = true;
        }
        else if (ehdr.e_entry < MemoryMap.START_RAM)
        {
            // seen in .elf's generated by pspsdk with BUILD_PRX=1 before conversion to .prx
            System.out.println("ELF requires relocation!");
            loadoffset = 0x08900000;
            relocate = true;
        }
        else
        {
            loadoffset = 0;
            relocate = false;
        }

        /** Read the ELF program headers */
        processProgramHeaders(f, elfoffset, ehdr, programheaders);

        /** Read the ELF section headers */
        shstrtab = processSectionHeaders1(f, elfoffset, ehdr, sectionheaders, loadoffset);
        moduleinfo = processSectionHeaders2(f, elfoffset, sectionheaders, shstrtab);

        /** Relocate ELF sections */
        if (relocate)
            processRelocationSections(f, elfoffset, sectionheaders, loadoffset, (int)moduleinfo.m_gp);

        /** set the default values for registers */
        // Not sure if they are correct and UNTESTED!!
        // From soywiz/pspemulator
        p.pc = (int)(loadoffset + ehdr.e_entry); //pc, set the pc register.
        p.cpuregisters[4] = 0; //a0
        p.cpuregisters[5] = (int)(loadoffset + ehdr.e_entry); //a1, argumentsPointer reg
        p.cpuregisters[6] = 0; //a2
        p.cpuregisters[26] = 0x09F00000; //k0
        p.cpuregisters[28] = (int)(loadoffset + moduleinfo.m_gp); //gp, should get the GlobalPointer!!!
        p.cpuregisters[29] = 0x09F00000; //sp
        p.cpuregisters[31] = 0x08000004; //ra, should this be 0?
        // All other registers are uninitialised/random values
    }

    public static void LoadPBPELF(String filename, Processor p) throws IOException
    {
        System.out.println("LoadPBPELF: " + filename);

        Memory.get_instance().NullMemory(); //re-initiate *test
        RandomAccessFile f = new RandomAccessFile(filename, "r");
        long elfoffset = 0;

        /** Read PBP and find ELF offset **/
        PBP pbp = new PBP(f);
        if (pbp.isValid())
        {
            if (Settings.get_instance().readBoolEmuoptions("pbpunpack"))
                PBP.unpackPBP(f);

            elfoffset = pbp.getOffsetPspData();
            PbpInfo = pbp.toString();
        }
        else
        {
            elfoffset = 0;
            PbpInfo = "-----NOT A PBP FILE---------\n";
        }

        /** Read ELF */
        f.seek(elfoffset);
        Elf32_Ehdr ehdr = new Elf32_Ehdr(f);
        if (ehdr.isValid())
        {
            LoadELF(p, f, elfoffset);
            ElfInfo = ehdr.toString();
        }
        else
        {
            System.out.println("NOT AN ELF FILE");
            ElfInfo = "-----NOT AN ELF FILE---------\n";
        }

        f.close();

		// TODO delete ElfHeader.java and fix up refs to Info strings
        ElfHeader.PbpInfo = PbpInfo;
        ElfHeader.ElfInfo = ElfInfo;
        ElfHeader.SectInfo = SectInfo;
    }
}
