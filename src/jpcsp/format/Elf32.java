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
    private Elf32ProgramHeader programHeader;
    private Elf32SectionHeader sectionHeader;
    private Elf32Relocate relocate;
    private List<Elf32SectionHeader> sectionheaders;
    
    public List<Elf32SectionHeader> getListSectionHeader(){
        return sectionheaders;
    }
    
    public Elf32(RandomAccessFile f) throws IOException {
        header = new Elf32Header(f);
        sectionHeader = new Elf32SectionHeader();
    }

    public Elf32Header getHeader() {
        return header;
    }

    public void setHeader(Elf32Header header) {
        this.header = header;
    }

    public Elf32ProgramHeader getProgramHeader() {
        return programHeader;
    }

    public void setListSectionHeader(List<Elf32SectionHeader> sectionheaders) {
        this.sectionheaders = sectionheaders;
    }

    public void setProgramHeader(Elf32ProgramHeader programHeader) {
        this.programHeader = programHeader;
    }

    public Elf32SectionHeader getSectionHeader() {
        return sectionHeader;
    }

    public void setSectionHeader(Elf32SectionHeader sectionHeader) {
        this.sectionHeader = sectionHeader;
    }

    public Elf32Relocate getRelocate() {
        return relocate;
    }

    public void setRelocate(Elf32Relocate relocate) {
        this.relocate = relocate;
    }
}
