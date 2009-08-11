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

package jpcsp.HLE.kernel.types;

import jpcsp.HLE.kernel.Managers;
import jpcsp.HLE.kernel.managers.SceUidManager;

/** usermode version of SceModule */
public class SceKernelModuleInfo extends pspAbstractMemoryMappedStructure {

    // PSP info
    // http://psp.jim.sh/pspsdk-doc/structSceKernelModuleInfo.html
    public final int size = 96;
    public byte nsegment;
    public byte[] reserved = new byte[3]; // these never get touched, nsegment is not 32-bit in this struct
    public int[] segmentaddr = new int[4];
    public int[] segmentsize = new int[4];
    public int entry_addr;
    public int gp_value;
    public int text_addr;
    public int text_size;
    public int data_size;
    public int bss_size;
    public short attribute;
    public byte[] version = new byte[2];
    public String name;

    public SceKernelModuleInfo() {
    }

    /** SceKernelModuleInfo contains a subset of the data in SceModule */
    public void copy(SceModule sceModule) {
        nsegment = (byte)(sceModule.nsegment & 0xFF);
        segmentaddr[0] = sceModule.segmentaddr[0];
        segmentaddr[1] = sceModule.segmentaddr[1];
        segmentaddr[2] = sceModule.segmentaddr[2];
        segmentaddr[3] = sceModule.segmentaddr[3];
        segmentsize[0] = sceModule.segmentsize[0];
        segmentsize[1] = sceModule.segmentsize[1];
        segmentsize[2] = sceModule.segmentsize[2];
        segmentsize[3] = sceModule.segmentsize[3];
        entry_addr = sceModule.entry_addr;
        gp_value = sceModule.gp_value;
        text_addr = sceModule.text_addr;
        text_size = sceModule.text_size;
        data_size = sceModule.data_size;
        bss_size = sceModule.bss_size;
        attribute = sceModule.attribute;
        version[0] = sceModule.version[0];
        version[1] = sceModule.version[1];
        name = sceModule.modname;
    }

    protected void read() {
        int size = read32();
        setMaxSize(size);

        nsegment        = (byte)(read8() & 0xFF);
        reserved[0]     = (byte)(read8() & 0xFF);
        reserved[1]     = (byte)(read8() & 0xFF);
        reserved[2]     = (byte)(read8() & 0xFF);
        segmentaddr[0]  = read32();
        segmentaddr[1]  = read32();
        segmentaddr[2]  = read32();
        segmentaddr[3]  = read32();
        segmentsize[0]  = read32();
        segmentsize[1]  = read32();
        segmentsize[2]  = read32();
        segmentsize[3]  = read32();
        entry_addr      = read32();
        gp_value        = read32();
        text_addr       = read32();
        text_size       = read32();
        data_size       = read32();
        bss_size        = read32();
        attribute       = (short)(read16() & 0xFFFF);
        version[0]      = (byte)(read8() & 0xFF);
        version[1]      = (byte)(read8() & 0xFF);
        name            = readStringNZ(28);
    }

    protected void write() {
        // TODO read size, but override so min is enough for everything up to and including bss_size
        setMaxSize(size);

        write32(size);
        write8(nsegment);
        writeSkip(3);
        write32(segmentaddr[0]);
        write32(segmentaddr[1]);
        write32(segmentaddr[2]);
        write32(segmentaddr[3]);
        write32(segmentsize[0]);
        write32(segmentsize[1]);
        write32(segmentsize[2]);
        write32(segmentsize[3]);
        write32(entry_addr);
        write32(gp_value);
        write32(text_addr);
        write32(text_size);
        write32(data_size);
        write32(bss_size);
        write16(attribute);
        write8(version[0]);
        write8(version[1]);
        writeStringNZ(28, name);
    }

    public int sizeof() {
        return size;
    }
}
