/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jpcsp.format;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.List;

public class Elf32 {

    private Elf32Header header;
    private String ElfInfo; // ELF header
    private String ProgInfo; // ELF program headers
    private String SectInfo; // ELF section headers
    private List<Elf32ProgramHeader> programheaders;
    private List<Elf32SectionHeader> sectionheaders;

    public List<Elf32SectionHeader> getListSectionHeader(){
        return sectionheaders;
    }

    public Elf32(RandomAccessFile f) throws IOException {
        header = new Elf32Header(f);
        ElfInfo = header.toString();
        ProgInfo = "";
        SectInfo = "";
    }

    public Elf32Header getHeader() {
        return header;
    }

    public void setHeader(Elf32Header header) {
        this.header = header;
    }

    public void setListProgramHeader(List<Elf32ProgramHeader> programheaders) {
        this.programheaders = programheaders;
    }

    public void setListSectionHeader(List<Elf32SectionHeader> sectionheaders) {
        this.sectionheaders = sectionheaders;
    }

    public String getElfInfo() {
        return ElfInfo;
    }

    public void setElfInfo(String ElfInfo) {
        this.ElfInfo = ElfInfo;
    }

    public String getProgInfo() {
        return ProgInfo;
    }

    public void setProgInfo(String ProgInfo) {
        this.ProgInfo = ProgInfo;
    }

    public String getSectInfo() {
        return SectInfo;
    }

    public void setSectInfo(String SectInfo) {
        this.SectInfo = SectInfo;
    }
}
