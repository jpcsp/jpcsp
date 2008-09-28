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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

public class Elf32 {

    private Elf32Header header;
    private String ElfInfo; // ELF header
    private String ProgInfo; // ELF program headers
    private String SectInfo; // ELF section headers
    private List<Elf32ProgramHeader> programheaders;
    private List<Elf32SectionHeader> sectionheaders;

    public Elf32(ByteBuffer f) throws IOException {
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

    public List<Elf32ProgramHeader> getListProgramHeader() {
        return programheaders;
    }

    public Elf32ProgramHeader getProgramHeader(int index) {
        if (programheaders != null)
            return programheaders.get(index);
        return null;
    }

    public void setListSectionHeader(List<Elf32SectionHeader> sectionheaders) {
        this.sectionheaders = sectionheaders;
    }

    public List<Elf32SectionHeader> getListSectionHeader() {
        return sectionheaders;
    }

    public Elf32SectionHeader getSectionHeader(int index) {
        if (sectionheaders != null)
            return sectionheaders.get(index);
        return null;
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
