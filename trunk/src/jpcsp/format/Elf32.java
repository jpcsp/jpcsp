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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import jpcsp.Emulator;
import jpcsp.util.Utilities;

public class Elf32 {

    // File offset
    private int elfOffset;

    // Headers
    private Elf32Header header;
    private List<Elf32ProgramHeader> programHeaderList;
    private List<Elf32SectionHeader> sectionHeaderList;
    private HashMap<String, Elf32SectionHeader> sectionHeaderMap;
    private Elf32SectionHeader shstrtab;

    // Debug info
    private String ElfInfo; // ELF header
    private String ProgInfo; // ELF program headers
    private String SectInfo; // ELF section headers

    public Elf32(ByteBuffer f) throws IOException {
        elfOffset = f.position();
        loadHeader(f);
        if (header.isValid()) {
            loadProgramHeaders(f);
            loadSectionHeaders(f);
        }
    }

    private void loadHeader(ByteBuffer f) throws IOException
    {
        header = new Elf32Header(f);
        ElfInfo = header.toString();
    }

    private void loadProgramHeaders(ByteBuffer f) throws IOException {
        programHeaderList = new LinkedList<Elf32ProgramHeader>();
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < header.getE_phnum(); i++) {
            f.position((int)(elfOffset + header.getE_phoff() + (i * header.getE_phentsize())));
            Elf32ProgramHeader phdr = new Elf32ProgramHeader(f);

            // Save loaded header
            programHeaderList.add(phdr);

            // Construct ELF program header info for debugger
            sb.append("-----PROGRAM HEADER #" + i + "-----" + "\n");
            sb.append(phdr.toString());

            // yapspd: if the PRX file is a kernel module then the most significant
            // bit must be set in the phsyical address of the first program header.
            if (i == 0 && (phdr.getP_paddr() & 0x80000000L) == 0x80000000L) {
                Emulator.log.debug("Kernel mode PRX detected");
            }
        }

        ProgInfo = sb.toString();
    }

    private void loadSectionHeaders(ByteBuffer f) throws IOException {
        sectionHeaderList = new LinkedList<Elf32SectionHeader>();
        sectionHeaderMap = new HashMap<String, Elf32SectionHeader>();

        // 1st pass
        // - save headers
        // - find .shstrtab
        for (int i = 0; i < header.getE_shnum(); i++) {
            f.position((int)(elfOffset + header.getE_shoff() + (i * header.getE_shentsize())));
            Elf32SectionHeader shdr = new Elf32SectionHeader(f);

            // Save loaded header
            sectionHeaderList.add(shdr);

            // Find the .shstrtab section
            if (shdr.getSh_type() == Elf32SectionHeader.SHT_STRTAB && // 0x00000003
                shstrtab == null &&
                // Some programs have 2 STRTAB headers,
                // the header with size 1 has to be ignored.
                shdr.getSh_size() > 1) {
                shstrtab = shdr;
            }
        }

        if (shstrtab == null) {
            Emulator.log.warn(".shstrtab section not found");
            return;
        }

        // 2nd pass
        // - Construct ELF section header info for debugger
        StringBuilder sb = new StringBuilder();
        int SectionCounter = 0;
        for (Elf32SectionHeader shdr : sectionHeaderList) {
            int position = (int)(elfOffset + shstrtab.getSh_offset() + shdr.getSh_name());
            f.position(position); // removed past end of file check (fiveofhearts 18/10/08)

            // Number the section
            sb.append("-----SECTION HEADER #" + SectionCounter + "-----" + "\n");

            String SectionName = Utilities.readStringZ(f); // removed readStringZ exception check (fiveofhearts 18/10/08)
            if (SectionName.length() > 0) {
                shdr.setSh_namez(SectionName);
                sb.append(SectionName + "\n");
                sectionHeaderMap.put(SectionName, shdr);
            } else {
                //Emulator.log.debug("Section header #" + SectionCounter + " has no name");
            }

            // Add this section header's info
            sb.append(shdr.toString());
            SectionCounter++;
        }

        SectInfo = sb.toString();
    }

    /** @return The elf was loaded from some kind of file or buffer. The elf
     * offset is an offset into this buffer where the elf actually starts. If
     * the returned offset is non-zero this is typically due to the elf being
     * embedded inside a pbp. */
    public int getElfOffset() {
        return elfOffset;
    }

    public Elf32Header getHeader() {
        return header;
    }

    public List<Elf32ProgramHeader> getProgramHeaderList() {
        return programHeaderList;
    }

    public Elf32ProgramHeader getProgramHeader(int index) {
        return programHeaderList.get(index);
    }

    public List<Elf32SectionHeader> getSectionHeaderList() {
        return sectionHeaderList;
    }

    public Elf32SectionHeader getSectionHeader(int index) {
        return sectionHeaderList.get(index);
    }

    public Elf32SectionHeader getSectionHeader(String name) {
        return sectionHeaderMap.get(name);
    }

    public String getElfInfo() {
        return ElfInfo;
    }

    public String getProgInfo() {
        return ProgInfo;
    }

    public String getSectInfo() {
        return SectInfo;
    }
}
