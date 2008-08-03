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

import java.io.File;
import java.io.FileOutputStream;
import jpcsp.util.Utilities;
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
     
     
     //private long pbp_size;
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
     //pbp_size = f.length();
     
     
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
        private long size_p_offset_param_sfo;
    private long size_p_icon0_png;
    private long size_offset_icon1_pmf;
    private long size_offset_pic0_png;
    private long size_offset_pic1_png;
    private long size_offset_snd0_at3;
    private long size_offset_psp_data;
    //private long size_offset_psar_data;
    public static boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
        String[] children = dir.list();
        for (int i=0; i<children.length; i++) {
        boolean success = deleteDir(new File(dir, children[i]));
        if (!success) {
        return false;
        }
        }
        }
        // The directory is now empty so delete it
        return dir.delete();
    }
    public void unpackPBP(RandomAccessFile f)throws IOException
    {
         size_p_offset_param_sfo = p_icon0_png - p_offset_param_sfo;
         size_p_icon0_png        = offset_icon1_pmf - p_icon0_png;
         size_offset_icon1_pmf   = offset_pic0_png -offset_icon1_pmf;
         size_offset_pic0_png    = offset_pic1_png - offset_pic0_png;
         size_offset_pic1_png    = offset_snd0_at3 -   offset_pic1_png;
         size_offset_snd0_at3    = offset_psp_data - offset_snd0_at3;
        size_offset_psp_data     = offset_psar_data - offset_psp_data;
         //size_offset_psar_data   =  pbp_size -  size_offset_psar_data; //not needed?
        
        File dir = new File("unpacked-pbp");
        deleteDir(dir);//delete all files and directory
        dir.mkdir();
        if(size_p_offset_param_sfo >0)//correct
        {
          byte[] data = new byte[(int)size_p_offset_param_sfo];
          f.seek(p_offset_param_sfo);
          f.readFully(data);
          FileOutputStream f1 = new FileOutputStream("unpacked-pbp/param.sfo");
          f1.write(data);
          f1.close();          
        }
        if(size_p_icon0_png>0)
        {
          byte[] data = new byte[(int)size_p_icon0_png];
          f.seek(p_icon0_png);
          f.readFully(data);
          FileOutputStream f1 = new FileOutputStream("unpacked-pbp/icon0.png");
          f1.write(data);
          f1.close();   
        }
        if(size_offset_icon1_pmf>0)
        {
          byte[] data = new byte[(int)size_offset_icon1_pmf];
          f.seek(offset_icon1_pmf);
          f.readFully(data);
          FileOutputStream f1 = new FileOutputStream("unpacked-pbp/icon1.pmf");
          f1.write(data);
          f1.close();   
        }
        if(size_offset_pic0_png>0)
        {
          byte[] data = new byte[(int)size_offset_pic0_png];
          f.seek(offset_pic0_png);
          f.readFully(data);
          FileOutputStream f1 = new FileOutputStream("unpacked-pbp/pic0.png");
          f1.write(data);
          f1.close();   
        }
        if(size_offset_pic1_png>0)
        {
          byte[] data = new byte[(int)size_offset_pic1_png];
          f.seek(offset_pic1_png);
          f.readFully(data);
          FileOutputStream f1 = new FileOutputStream("unpacked-pbp/pic1.png");
          f1.write(data);
          f1.close();   
        }
        if(size_offset_snd0_at3>0)
        {
          byte[] data = new byte[(int)size_offset_snd0_at3];
          f.seek(offset_snd0_at3);
          f.readFully(data);
          FileOutputStream f1 = new FileOutputStream("unpacked-pbp/snd0.at3");
          f1.write(data);
          f1.close();   
        }
        if(size_offset_psp_data >0)
        {
          byte[] data = new byte[(int)size_offset_psp_data ];
          f.seek(offset_psp_data);
          f.readFully(data);
          FileOutputStream f1 = new FileOutputStream("unpacked-pbp/data.psp");
          f1.write(data);
          f1.close();          
        }

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
    private long p_type;
    private long p_offset;
    private long p_vaddr;
    private long p_paddr;
    private long p_filesz;
    private long p_memsz;
    private long p_flags; // Bits: 0x1=executable, 0x2=writable, 0x4=readable, demo PRX's were found to be not writable
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
    }

    public String toString()
    {
      StringBuffer str = new StringBuffer();
      str.append("p_type " + "\t\t " +  Utilities.formatString("long", Long.toHexString(p_type & 0xFFFFFFFFL).toUpperCase()) + "\n");
      str.append("p_offset " + "\t " +  Utilities.formatString("long", Long.toHexString(p_offset & 0xFFFFFFFFL).toUpperCase()) + "\n");
      str.append("p_vaddr " + "\t " +  Utilities.formatString("long", Long.toHexString(p_vaddr & 0xFFFFFFFFL).toUpperCase()) + "\n");
      str.append("p_paddr " + "\t " +  Utilities.formatString("long", Long.toHexString(p_paddr & 0xFFFFFFFFL).toUpperCase()) + "\n");
      str.append("p_filesz " + "\t " +  Utilities.formatString("long", Long.toHexString(p_filesz & 0xFFFFFFFFL).toUpperCase()) + "\n");
      str.append("p_memsz " + "\t " +  Utilities.formatString("long", Long.toHexString(p_memsz & 0xFFFFFFFFL).toUpperCase()) + "\n");
      str.append("p_flags " + "\t " +  Utilities.formatString("long", Long.toHexString(p_flags & 0xFFFFFFFFL).toUpperCase()) + "\n");
      str.append("p_align " + "\t " +  Utilities.formatString("long", Long.toHexString(p_align & 0xFFFFFFFFL).toUpperCase()) + "\n");
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
        private String m_namez = ""; // String version of m_name

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

            // Convert the array of bytes used for the module name to a Java String
            // If we don't do it this way then there will be too many nul-bytes on the end, and some shells print them all!
            int len;
            for (len = 0; len < 28 && m_name[len] != 0; len++);
            m_namez = new String(m_name, 0, len);
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
      //readByte() isn't more correct? (already exists one readUWord() method to unsign values)
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
    boolean relocate = false;
    /** Read pbp **/
    PBP_Header pbp = new PBP_Header();
    pbp.read(f);
    if(Long.toHexString(pbp.p_magic & 0xFFFFFFFFL).equals("50425000"))//file is pbp 50425000 == 0x3016CA8??? the comparison is made by hexa
        // TODO : check the documentation ...
    {
        if(Settings.get_instance().readBoolEmuoptions("pbpunpack")) 
            pbp.unpackPBP(f);
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
        System.out.println("PRX detected, requires relocation");
        baseoffset = 0x08900000;
        relocate = true;
    }
    else if (ehdr.e_entry < MemoryMap.START_RAM)
    {
        // seen in .elf's generated by pspsdk with BUILD_PRX=1 before conversion to .prx
        System.out.println("ELF requires relocation");
        baseoffset = 0x08900000;
        relocate = true;
    }

    /** Read the ELF program headers */
    List<Elf32_Phdr> programheaders = new LinkedList<Elf32_Phdr>();
    StringBuffer phsb = new StringBuffer();
    for (int i = 0; i < ehdr.e_phnum; i++)
    {
        Elf32_Phdr phdr = new Elf32_Phdr();
        programheaders.add(phdr);

        // Read information about this section.
        f.seek(elfoffset + ehdr.e_phoff + (i * ehdr.e_phentsize));
        phdr.read(f);

        phsb.append("-----PROGRAM HEADER #"+i+"-----" + "\n");
        phsb.append(phdr.toString());

        // yapspd: if the PRX file is a kernel module then the most significant
        // bit must be set in the phsyical address of the first program header.
        if (i == 0 && (phdr.p_paddr & 0x80000000L) == 0x80000000L)
        {
            // kernel mode prx
            System.out.println("Kernel mode PRX detected");
        }
    }
    //SegInfo = phsb.toString();
    ElfInfo += phsb.toString();

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

        if (shdr.sh_type == 3 && shstrtab == null) //ShType.STRTAB
            shstrtab = shdr;

        // Load some sections into memory
        if((shdr.sh_flags & ShFlags.Allocate.getValue())== ShFlags.Allocate.getValue())
        {
             switch(shdr.sh_type)
             {
                 case 1: //ShType.PROGBITS
                     //System.out.println("FEED MEMORY WITH IT!");
                     f.seek(elfoffset + shdr.sh_offset);
                     int offsettoread = (int)baseoffset + (int)shdr.sh_addr - MemoryMap.START_RAM;
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
    StringBuffer shsb = new StringBuffer();
    int SectionCounter = 0;
    for (Elf32_Shdr shdr: sectionheaders)
    {
        // Number the section
        shsb.append("-----SECTION HEADER #"+SectionCounter+"-----" + "\n");

        // Resolve section name (if possible)
        if (shstrtab != null)
        {
            f.seek(elfoffset + shstrtab.sh_offset + shdr.sh_name);
            String SectionName = readStringZ(f);
            if (SectionName.length() > 0)
            {
                shdr.sh_namez = SectionName;
                shsb.append(SectionName + "\n");

                // Get module infos
                if (SectionName.matches(".rodata.sceModuleInfo"))
                {
                    f.seek(elfoffset + shdr.sh_offset);
                    moduleinfo.read(f);
                    //System.out.println(Long.toHexString(moduleinfo.m_gp));

                    System.out.println("Found ModuleInfo name:'" + moduleinfo.m_namez
                        + "' version:" + Utilities.formatString("short", Integer.toHexString(moduleinfo.m_version & 0xFFFF).toUpperCase()));

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

    // 3rd pass relocate
    if (relocate)
    {
        for (Elf32_Shdr shdr: sectionheaders)
        {
            if (shdr.sh_type == 0x700000A0 || // PRX reloc magic
                shdr.sh_type == 0x00000009) //ShType.REL
            {
                Elf32_Rel rel = new Elf32_Rel();

                f.seek(elfoffset + shdr.sh_offset);

                int RelCount = (int)shdr.sh_size / Elf32_Rel.sizeof();
                System.out.println(shdr.sh_namez + ": relocating " + RelCount + " entries");

                int AHL = 0; // (AHI << 16) | (ALO & 0xFFFF)
                int HI_addr = 0; // We'll use this to relocate R_MIPS_HI16 when we get a R_MIPS_LO16

                // Relocation modes, only 1 is allowed at a time
                boolean external = true; // copied from soywiz/pspemulator
                boolean local = false;
                boolean _gp_disp = false;

                for (int i = 0; i < RelCount; i++)
                {
                    rel.read(f);

                    int R_TYPE = (int)(rel.r_info & 0xFF);
                    int OFS_BASE = (int)((rel.r_info >> 8) & 0xFF);
                    int ADDR_BASE = (int)((rel.r_info >> 16) & 0xFF);
                    //System.out.println("type=" + R_TYPE + ",base=" + OFS_BASE + ",addr=" + ADDR_BASE + "");

                    int data = Memory.get_instance().read32((int)baseoffset + (int)rel.r_offset);
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
                    int P = (int)baseoffset + (int)rel.r_offset; // address of instruction being relocated? 31/07/08 unused when external=true (fiveofhearts)
                    int S = (int)baseoffset; // ? copied from soywiz/pspemulator, but doesn't match the docs (fiveofhearts)
                    int G = 0; // ? 31/07/08 unused (fiveofhearts)
                    int GP = (int)baseoffset + (int)moduleinfo.m_gp; // final gp value, computed correctly? 31/07/08 only used in R_MIPS_GPREL16 which is untested (fiveofhearts)
                    int GP0 = (int)moduleinfo.m_gp; // gp value, computed correctly? 31/07/08 unused when external=true (fiveofhearts)
                    int EA = 0; // ? 31/07/08 unused (fiveofhearts)
                    int L = 0; // ? 31/07/08 unused (fiveofhearts)

                    switch(R_TYPE)
                    {
                        case 0: //R_MIPS_NONE
                            // Don't do anything
                            break;

                        case 5: //R_MIPS_HI16
                            A = hi16;
                            AHL = A << 16;
                            HI_addr = (int)baseoffset + (int)rel.r_offset;
                            break;

                        case 6: //R_MIPS_LO16
                            A = lo16;
                            AHL &= ~0x0000FFFF; // delete lower bits, since many R_MIPS_LO16 can follow one R_MIPS_HI16
                            AHL |= A & 0x0000FFFF;

                            if (external || local)
                            {
                                result = AHL + S;
                                data &= ~0x0000FFFF;
                                data |= result & 0x0000FFFF; // truncate

                                // Process deferred R_MIPS_HI16
                                int data2 = Memory.get_instance().read32(HI_addr);
                                data2 &= ~0x0000FFFF;
                                data2 |= (result >> 16) & 0x0000FFFF; // truncate
                                Memory.get_instance().write32(HI_addr, data2);
                            }
                            else if (_gp_disp)
                            {
                                result = AHL + GP - P + 4;

                                // verify
                                if ((result & ~0xFFFFFFFF) != 0)
                                    throw new IOException("Relocation overflow (R_MIPS_LO16)");

                                data &= ~0x0000FFFF;
                                data |= result & 0x0000FFFF;

                                // Process deferred R_MIPS_HI16
                                int data2 = Memory.get_instance().read32(HI_addr);

                                result = AHL + GP - P;

                                // verify
                                if ((result & ~0xFFFFFFFF) != 0)
                                    throw new IOException("Relocation overflow (R_MIPS_HI16)");

                                data2 &= ~0x0000FFFF;
                                data2 |= (result >> 16) & 0x0000FFFF;
                                Memory.get_instance().write32(HI_addr, data2);
                            }
                            break;

                        case 4: //R_MIPS_26
                            if (local)
                            {
                                A = targ26;
                                result = ((A << 2) | (P & 0xf0000000) + S) >> 2;
                                data &= ~0x03FFFFFF;
                                data |= (int)(result & 0x03FFFFFF); // truncate
                            }
                            else if (external)
                            {
                                A = targ26;

                                // docs say "sign-extend(A < 2)", but is it meant to be A << 2? if so then there's no point sign extending
                                //result = (sign-extend(A < 2) + S) >> 2;
                                //result = (((A < 2) ? 0xFFFFFFFF : 0x00000000) + S) >> 2;
                                result = ((A << 2) + S) >> 2; // copied from soywiz/pspemulator

                                data &= ~0x03FFFFFF;
                                data |= (int)(result & 0x03FFFFFF); // truncate
                            }
                            break;

                        case 2: //R_MIPS_32
                            // This doesn't match soywiz/pspemulator but it generates more sensible addresses (fiveofhearts)
                            A = word32;
                            result = S + A;
                            data &= ~0xFFFFFFFF;
                            data |= (int)(result & 0xFFFFFFFF); // truncate
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
                            System.out.println("Unhandled relocation type " + R_TYPE + " at " + String.format("%08x", (int)baseoffset + (int)rel.r_offset));
                            break;
                    }

                    //System.out.println("Relocation type " + R_TYPE + " at " + String.format("%08x", (int)baseoffset + (int)rel.r_offset));
                    Memory.get_instance().write32((int)baseoffset + (int)rel.r_offset, data);
                }
            }
        }
    }

    //set the default values for registers not sure if they are correct and UNTESTED!!
    // from soywiz/pspemulator
    p.pc = (int)baseoffset + (int)ehdr.e_entry; //set the pc register.
    p.cpuregisters[31] = 0x08000004; //ra, should this be 0?
    p.cpuregisters[5] = (int)baseoffset + (int)ehdr.e_entry; // argumentsPointer a1 reg
    p.cpuregisters[28] = (int)baseoffset + (int)moduleinfo.m_gp; //gp reg    gp register should get the GlobalPointer!!!
    p.cpuregisters[29] = 0x09F00000; //sp
    p.cpuregisters[26] = 0x09F00000; //k0

    f.close();
  }

}
