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
import java.io.RandomAccessFile;
import java.util.List;
import java.util.LinkedList;


/**
 *
 * @author George
 */
public class ElfHeader {

  private static class PBP_Header
  {
     private long p_magic;
     private long p_version;
     private long p_offset_param_sfo;
     private long p_icon0_png;
     private long offset_icon1_pmf;
     private long offset_pic0_png;
     private long offset_pic1_png;
     private long offset_snd0_at3;
     private long offset_psp_data;
     private long offset_psar_data;
    private void read (RandomAccessFile f) throws IOException
    {
     p_magic = readUWord(f);
     p_version = readUWord(f);
     p_offset_param_sfo = readUWord(f);
     p_icon0_png = readUWord(f);
     offset_icon1_pmf = readUWord(f);
     offset_pic0_png = readUWord(f);
     offset_pic1_png = readUWord(f);
     offset_snd0_at3 = readUWord(f);
     offset_psp_data = readUWord(f);
     offset_psar_data = readUWord(f);
    }

        @Override
         public String toString()
     {
       StringBuffer str = new StringBuffer();
       str.append("-----PBP HEADER---------" + "\n");
       str.append("p_magic " + "\t\t" +  Utilities.formatString("long", Long.toHexString(p_magic & 0xFFFFFFFFL).toUpperCase()) + "\n");
       str.append("p_version " + "\t\t" +  Utilities.formatString("long", Long.toHexString(p_version & 0xFFFFFFFFL).toUpperCase()) + "\n");
       str.append("p_offset_param_sfo " + "\t" +  Utilities.formatString("long", Long.toHexString(p_offset_param_sfo & 0xFFFFFFFFL).toUpperCase()) + "\n");
       str.append("p_icon0_png " + "\t\t" +  Utilities.formatString("long", Long.toHexString(p_icon0_png & 0xFFFFFFFFL).toUpperCase()) + "\n");
       str.append("offset_icon1_pmf " + "\t" +  Utilities.formatString("long", Long.toHexString(offset_icon1_pmf & 0xFFFFFFFFL).toUpperCase()) + "\n");
       str.append("offset_pic0_png " + "\t" +  Utilities.formatString("long", Long.toHexString(offset_pic0_png & 0xFFFFFFFFL).toUpperCase()) + "\n");
       str.append("offset_pic1_png " + "\t" +  Utilities.formatString("long", Long.toHexString(offset_pic1_png & 0xFFFFFFFFL).toUpperCase()) + "\n");
       str.append("offset_snd0_at3 " + "\t" +  Utilities.formatString("long", Long.toHexString(offset_snd0_at3 & 0xFFFFFFFFL).toUpperCase()) + "\n");
       str.append("offset_psp_data " + "\t" +  Utilities.formatString("long", Long.toHexString(offset_psp_data & 0xFFFFFFFFL).toUpperCase()) + "\n");
       str.append("offset_psar_data " + "\t" +  Utilities.formatString("long", Long.toHexString(offset_psar_data & 0xFFFFFFFFL).toUpperCase()) + "\n");


       return str.toString();
     }
  }
  private static class Elf32_Ehdr
  {
    private long e_magic;
    private int e_class;
    private int e_data;
    private int e_idver;
    private byte[] e_pad=new byte[9];
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

    private void read (RandomAccessFile f) throws IOException
    {
      e_magic = readUWord(f);
      e_class = readUByte(f);
      e_data = readUByte(f);
      e_idver = readUByte(f);
      f.readFully(e_pad);
      e_type = readUHalf(f);
      e_machine = readUHalf(f);
      e_version = readUWord (f);
      e_entry = readUWord (f);
      e_phoff = readUWord (f);
      e_shoff = readUWord (f);
      e_flags = readUWord (f);
      e_ehsize = readUHalf (f);
      e_phentsize = readUHalf (f);
      e_phnum = readUHalf (f);
      e_shentsize = readUHalf (f);
      e_shnum = readUHalf (f);
      e_shstrndx = readUHalf (f);
    }
        @Override
     public String toString()
     {
       StringBuffer str = new StringBuffer();
       str.append("-----ELF HEADER---------" + "\n");
       str.append("e_magic " + "\t " +  Utilities.formatString("long", Long.toHexString(e_magic & 0xFFFFFFFFL).toUpperCase()) + "\n");
       str.append("e_class " + "\t " +  Utilities.integerToHex(e_class & 0xFF ) + "\n");
       // str.append("e_class " + "\t " +  Utilities.formatString("byte", Integer.toHexString(e_class & 0xFF ).toUpperCase())+ "\n");
       str.append("e_data " + "\t\t " + Utilities.formatString("byte", Integer.toHexString(e_data & 0xFF ).toUpperCase())+ "\n");
       str.append("e_idver " + "\t " + Utilities.formatString("byte", Integer.toHexString(e_idver & 0xFF).toUpperCase())+ "\n");
       str.append("e_type " + "\t\t " + Utilities.formatString("short",Integer.toHexString(e_type & 0xFFFF).toUpperCase())+ "\n");
       str.append("e_machine " + "\t " + Utilities.formatString("short",Integer.toHexString(e_machine & 0xFFFF).toUpperCase())+ "\n");
       str.append("e_version " + "\t " + Utilities.formatString("long",Long.toHexString(e_version & 0xFFFFFFFFL).toUpperCase())+ "\n");
       str.append("e_entry " + "\t " + Utilities.formatString("long",Long.toHexString(e_entry & 0xFFFFFFFFL).toUpperCase())+ "\n");
       str.append("e_phoff "+ "\t " + Utilities.formatString("long",Long.toHexString(e_phoff & 0xFFFFFFFFL).toUpperCase())+ "\n");
       str.append("e_shoff "+ "\t " + Utilities.formatString("long",Long.toHexString(e_shoff  & 0xFFFFFFFFL).toUpperCase())+ "\n");
       str.append("e_flags "+ "\t " + Utilities.formatString("long",Long.toHexString(e_flags & 0xFFFFFFFFL).toUpperCase())+ "\n");
       str.append("e_ehsize "+ "\t " + Utilities.formatString("short",Integer.toHexString(e_ehsize& 0xFFFF).toUpperCase())+ "\n");
       str.append("e_phentsize " + "\t " + Utilities.formatString("short",Integer.toHexString(e_phentsize& 0xFFFF).toUpperCase())+ "\n");
       str.append("e_phnum " + "\t " + Utilities.formatString("short",Integer.toHexString(e_phnum& 0xFFFF).toUpperCase())+ "\n");
       str.append("e_shentsize " + "\t " + Utilities.formatString("short",Integer.toHexString(e_shentsize& 0xFFFF).toUpperCase())+ "\n");
       str.append("e_shnum " + "\t " + Utilities.formatString("short",Integer.toHexString(e_shnum& 0xFFFF).toUpperCase())+ "\n");
       str.append("e_shstrndx "+ "\t " + Utilities.formatString("short",Integer.toHexString(e_shstrndx& 0xFFFF).toUpperCase())+ "\n");
       return str.toString();
     }
  }

  //http://www.caldera.com/developers/devspecs/abi386-4.pdf
  //System V ABI, IA32 Supplement
  private static class Elf32_Phdr
  {
    StringBuffer str = new StringBuffer();
    private int SegmentCounter=0;
    private long p_type;
    private long p_offset;
    private long p_vaddr;
    private long p_paddr;
    private long p_filesz;
    private long p_memsz;
    private long p_flags;
    private long p_align;

    private static int sizeof () { return 32; }
    private void read (RandomAccessFile f) throws IOException
    {
      p_type = readUWord (f);
      p_offset = readUWord (f);
      p_vaddr = readUWord (f);
      p_paddr = readUWord (f);
      p_filesz = readUWord (f);
      p_memsz = readUWord (f);
      p_flags = readUWord (f);
      p_align = readUWord (f);

      //each section is added to the string buffer
      str.append("-----PROGRAM HEADER #"+SegmentCounter+"-----" + "\n");
      str.append("p_type " + "\t\t " +  Utilities.formatString("long", Long.toHexString(p_type & 0xFFFFFFFFL).toUpperCase()) + "\n");
      str.append("p_offset " + "\t " +  Utilities.formatString("long", Long.toHexString(p_offset & 0xFFFFFFFFL).toUpperCase()) + "\n");
      str.append("p_vaddr " + "\t " +  Utilities.formatString("long", Long.toHexString(p_vaddr & 0xFFFFFFFFL).toUpperCase()) + "\n");
      str.append("p_paddr " + "\t " +  Utilities.formatString("long", Long.toHexString(p_paddr & 0xFFFFFFFFL).toUpperCase()) + "\n");
      str.append("p_filesz " + "\t " +  Utilities.formatString("long", Long.toHexString(p_filesz & 0xFFFFFFFFL).toUpperCase()) + "\n");
      str.append("p_memsz " + "\t " +  Utilities.formatString("long", Long.toHexString(p_memsz & 0xFFFFFFFFL).toUpperCase()) + "\n");
      str.append("p_flags " + "\t " +  Utilities.formatString("long", Long.toHexString(p_flags & 0xFFFFFFFFL).toUpperCase()) + "\n");
      str.append("p_align " + "\t " +  Utilities.formatString("long", Long.toHexString(p_align & 0xFFFFFFFFL).toUpperCase()) + "\n");
      SegmentCounter++;
    }

    public String toString()
    {
      return str.toString();
    }
  }

  private static class Elf32_Shdr
  {
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

    private static int sizeof () { return 40; }
    private void read (RandomAccessFile f) throws IOException
    {
      sh_name = readUWord (f);
      sh_type = readWord (f);
      sh_flags = readWord (f);
      sh_addr = readUWord (f);
      sh_offset = readUWord (f);
      sh_size = readUWord (f);
      sh_link = readWord (f);
      sh_info = readWord (f);
      sh_addralign = readWord (f);
      sh_entsize = readWord (f);
    }

    public String toString()
    {
      StringBuffer str = new StringBuffer();
      str.append("sh_name " + "\t " +  Utilities.formatString("long", Long.toHexString(sh_name & 0xFFFFFFFFL).toUpperCase()) + "\n");
      str.append("sh_type " + "\t " +  Utilities.formatString("long", Long.toHexString(sh_type & 0xFFFFFFFFL).toUpperCase()) + "\n");
      str.append("sh_flags " + "\t " +  Utilities.integerToHex(sh_flags & 0xFF ) + "\n");
      str.append("sh_addr " + "\t " +  Utilities.formatString("long", Long.toHexString(sh_addr & 0xFFFFFFFFL).toUpperCase()) + "\n");
      str.append("sh_offset " + "\t " +  Utilities.formatString("long", Long.toHexString(sh_offset & 0xFFFFFFFFL).toUpperCase()) + "\n");
      str.append("sh_size " + "\t " +  Utilities.formatString("long", Long.toHexString(sh_size & 0xFFFFFFFFL).toUpperCase()) + "\n");
      str.append("sh_link " + "\t " +  Utilities.integerToHex(sh_link & 0xFF ) + "\n");
      str.append("sh_info " + "\t " +  Utilities.integerToHex(sh_info & 0xFF ) + "\n");
      str.append("sh_addralign " + "\t " +  Utilities.integerToHex(sh_addralign & 0xFF ) + "\n");
      str.append("sh_entsize " + "\t " +  Utilities.formatString("long", Long.toHexString(sh_entsize & 0xFFFFFFFFL).toUpperCase()) + "\n");
      return str.toString();
    }
  }

  private static class Elf32_Rel
  {
    private long r_offset;
    private long r_info;

    private static int sizeof () { return 8; }
    private void read (RandomAccessFile f) throws IOException
    {
      r_offset = readUWord (f);
      r_info = readUWord (f);
    }

    public String toString()
    {
      StringBuffer str = new StringBuffer();
      str.append("r_offset " + "\t " +  Utilities.formatString("long", Long.toHexString(r_offset & 0xFFFFFFFFL).toUpperCase()) + "\n");
      str.append("r_info " + "\t\t " +  Utilities.formatString("long", Long.toHexString(r_info & 0xFFFFFFFFL).toUpperCase()) + "\n");
      return str.toString();
    }
  }
    private static class PSPModuleInfo
    {
        private int m_attr;
        private int m_version;
        private byte[] m_name = new byte[28];
        private long m_gp;
        private long m_exports;
        private long m_exp_end;
        private long m_imports;
        private long m_imp_end;

        private void read (RandomAccessFile f) throws IOException
        {
            m_attr = readUHalf (f);
            m_version = readUHalf (f);
            f.readFully(m_name);
            m_gp = readUWord (f);
            m_exports = readUWord (f); // .lib.ent
            m_exp_end = readUWord (f);
            m_imports = readUWord (f); // .lib.stub
            m_imp_end = readUWord (f);
        }
    }

  private static int readUByte (RandomAccessFile f) throws IOException
  {
    return f.readUnsignedByte();
  }

  private static int readUHalf (RandomAccessFile f) throws IOException
  {
      return f.readUnsignedByte () | (f.readUnsignedByte () << 8);
  }

  private static int readWord (RandomAccessFile f) throws IOException
  {
      return (f.readUnsignedByte () | (f.readUnsignedByte () << 8)
	      | (f.readUnsignedByte () << 16) | (f.readUnsignedByte () << 24));
  }

  private static long readUWord (RandomAccessFile f) throws IOException
  {
	long l = (f.readUnsignedByte () | (f.readUnsignedByte () << 8)
		  | (f.readUnsignedByte () << 16) | (f.readUnsignedByte () << 24));
	return (l & 0xFFFFFFFFL);
   }

  private static String readStringZ (RandomAccessFile f) throws IOException
  {
      StringBuffer sb = new StringBuffer();
      int b;
      for(;;)
      {
        b = f.readUnsignedByte();
        if (b == 0) break;
        sb.append((char)b);
      }
    return sb.toString();
  }

   enum ShFlags { None(0) , Write(1) , Allocate(2) , Execute(4);
            private int value;
            ShFlags(int val)
            {
                value=val;
            }
            int getValue()
            {
                return value;
            }

    }

   enum ShType { NULL(0), PROGBITS(1) ,SYMTAB(2) ,STRTAB(3),
		 RELA(4),HASH(5),DYNAMIC(6),NOTE(7),NOBITS(8)
		 ,REL(9),SHLIB(10),DYNSYM(11);
             private int value;
            ShType(int val)
            {
                value=val;
            }
            int getValue()
            {
                return value;
            }
   }

  public static String ElfInfo,PbpInfo,SectInfo;
  static void readHeader(String file,Processor p) throws IOException
  {
    Memory.get_instance().NullMemory(); //re-initiate *test
    RandomAccessFile f = new RandomAccessFile (file, "r");
    long elfoffset = 0;
    long baseoffset = 0;
    /** Read pbp **/
    PBP_Header pbp = new PBP_Header();
    pbp.read(f);
    if(Long.toHexString(pbp.p_magic & 0xFFFFFFFFL).equals("50425000"))//file is pbp
    {
        elfoffset = pbp.offset_psp_data;
        f.seek(pbp.offset_psp_data); //seek the new offset!
        PbpInfo = pbp.toString();
    }
    else
    {
        elfoffset = 0;
        f.seek(0); // start read from start file is not pbp check if it an elf;
        PbpInfo = "-----NOT A PBP FILE---------\n";
    }

    /** Read the ELF header. */
    Elf32_Ehdr ehdr = new Elf32_Ehdr ();
    ehdr.read (f);
    if(!Long.toHexString(ehdr.e_magic & 0xFFFFFFFFL).toUpperCase().equals("464C457F"))
    {
        System.out.println("NOT AN ELF FILE");

    }
    if(!Integer.toHexString(ehdr.e_machine & 0xFFFF).equals("8"))
    {
        System.out.println("NOT A MIPS executable");
    }
    ElfInfo = ehdr.toString();
    if ((ehdr.e_type & 0xFFFF) == 0xFFA0)
    {
        System.out.println("PRX detected");
        baseoffset = 0x08900000;
    }

    /** Read the ELF program headers */
    Elf32_Phdr phdr = new Elf32_Phdr();
    for (int i = 0; i < ehdr.e_phnum; i++)
    {
        // Read information about this section.
        f.seek(elfoffset + ehdr.e_phoff + (i * ehdr.e_phentsize));
        phdr.read(f);

        // yapspd: if the PRX file is a kernel module then the most significant
        // bit must be set in the phsyical address of the first program header.
        if (i == 0 && (phdr.p_paddr & 0x80000000L) == 0x80000000L)
        {
            // kernel mode prx
            System.out.println("Kernel mode PRX detected");
        }
    }
    //SegInfo = phdr.toString();
    ElfInfo += phdr.toString();

    /** Read the ELF section headers (1st pass) */
    List<Elf32_Shdr> sectionheaders = new LinkedList<Elf32_Shdr>();
    Elf32_Shdr shstrtab = null;
    for (int i = 0; i < ehdr.e_shnum; i++)
    {
        Elf32_Shdr shdr = new Elf32_Shdr();
        sectionheaders.add(shdr);

        // Read information about this section.
        f.seek (elfoffset + ehdr.e_shoff + (i * ehdr.e_shentsize));
        shdr.read (f);
        //System.out.println(shdr.toString());

        if (shdr.sh_type == 3) //ShType.STRTAB
            shstrtab = shdr;

        // Load some sections into memory
        if((shdr.sh_flags & ShFlags.Allocate.getValue())== ShFlags.Allocate.getValue())
        {
             switch(shdr.sh_type)
             {
                 case 1: //ShType.PROGBITS
                     //System.out.println("FEED MEMORY WITH IT!");
                     f.seek(elfoffset + shdr.sh_offset);
                     long rambase = Long.parseLong("08000000",16);//convert hex to integer..
                     int offsettoread = (int)baseoffset + (int)shdr.sh_addr - (int)rambase;
                     f.read(Memory.get_instance().mainmemory,offsettoread,(int)shdr.sh_size);
                     break;
                 case 8: // ShType.NOBITS
                     System.out.println("NO BITS");
                     // zero out this memory(?), from shdr.sh_addr to shdr.sh_addr + shdr.sh_size
                     break;
             }
        }
    }

    // 2nd pass generate info string for the GUI and get module infos
    PSPModuleInfo moduleinfo = new PSPModuleInfo();
    StringBuffer str = new StringBuffer();
    int SectionCounter = 0;
    for (Elf32_Shdr shdr: sectionheaders)
    {
        // Number the section
        str.append("-----SECTION HEADER #"+SectionCounter+"-----" + "\n");

        // Resolve section name (if possible)
        if (shstrtab != null)
        {
            f.seek(elfoffset + shstrtab.sh_offset + shdr.sh_name);
            String SectionName = readStringZ(f);
            if (SectionName.length() > 0)
            {
                str.append(SectionName + "\n");

                // Get module infos
                if (SectionName.matches(".rodata.sceModuleInfo"))
                {
                    f.seek(elfoffset + shdr.sh_offset);
                    moduleinfo.read(f);
                    //System.out.println(Long.toHexString(moduleinfo.m_gp));

                    System.out.println("Found ModuleInfo name:'" + new String(moduleinfo.m_name)
                        + "' version:" + Utilities.formatString("short", Integer.toHexString(moduleinfo.m_version & 0xFFFF).toUpperCase()));

                    if ((moduleinfo.m_attr & 0x1000) != 0)
                    {
                        System.out.println("Kernel mode module detected");
                    }
                }
            }
        }

        // Add the normal info
        str.append(shdr.toString());
        SectionCounter++;
    }
    SectInfo = str.toString();

    // 3rd pass relocate PRX
    if ((ehdr.e_type & 0xFFFF) == 0xFFA0) // PRX magic
    {
        for (Elf32_Shdr shdr: sectionheaders)
        {
            if (shdr.sh_type == 0x700000A0) // PRX reloc magic
            {
                Elf32_Rel rel = new Elf32_Rel();

                f.seek(elfoffset + shdr.sh_offset);

                int RelCount = (int)shdr.sh_size / Elf32_Shdr.sizeof();
                //System.out.println("relocating " + RelCount + " entries");

                for (int i = 0; i < RelCount; i++)
                {
                    rel.read(f);

                    int R_TYPE = (int)(rel.r_info & 0xFF);
                    int OFS_BASE = (int)((rel.r_info >> 8) & 0xFF);
                    int ADDR_BASE = (int)((rel.r_info >> 16) & 0xFF);

                    /* TODO
                    System.out.println("type=" + R_TYPE + ",base=" + OFS_BASE + ",addr=" + ADDR_BASE + "");

                    int data = Memory.get_instance().read32((int)baseoffset + (int)rel.r_offset);

                    switch(R_TYPE)
                    {
                        case 0: //R_MIPS_NONE
                            break;
                        case 5: //R_MIPS_HI16
                            break;
                        case 6: //R_MIPS_LO16
                            break;
                        case 4: //R_MIPS_26
                            break;
                        case 2: //R_MIPS_32
                            break;
                    }

                    Memory.get_instance().write32((int)baseoffset + (int)rel.r_offset, data);
                    */
                }
            }
        }
    }

    //set the default values for registers not sure if they are correct and UNTESTED!!
    // from soywiz/pspemulator
    p.pc = (int)baseoffset + (int)ehdr.e_entry; //set the pc register.
    p.cpuregisters[31] = 0x08000004; //ra
    p.cpuregisters[5] = (int)baseoffset + (int)ehdr.e_entry; // argumentsPointer a1 reg
    p.cpuregisters[28] =  (int)moduleinfo.m_gp; //gp reg    gp register should get the GlobalPointer!!!
    p.cpuregisters[29] = 0x09F00000; //sp
    p.cpuregisters[26] = 0x09F00000; //k0

    f.close();
  }
}
