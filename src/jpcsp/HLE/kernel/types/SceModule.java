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

import java.util.List;
import java.util.LinkedList;

import jpcsp.Emulator;
import jpcsp.Loader;
import jpcsp.Memory;
import jpcsp.HLE.pspSysMem;
import jpcsp.HLE.kernel.managers.*;
import jpcsp.format.DeferredStub;
import jpcsp.format.PSF;
import jpcsp.format.PSPModuleInfo;
import jpcsp.util.Utilities;

/** After initialising an instance please call .write() at least once. */
public class SceModule {

    // PSP info
    // http://psp.jim.sh/pspsdk-doc/structSceModule.html
    public int next; // should be handled by a manager
    public short attribute;
    public byte[] version = new byte[2];
    public String modname; // 27 printable chars
    public final byte terminal = (byte)0;
    public int unknown1;
    public int unknown2;
    public final int modid;
    public int[] unknown3 = new int[4];
    public int ent_top;
    public int ent_size; // we'll use bytes (instead of number of entries)
    public int stub_top;
    public int stub_size; // we'll use bytes (instead of number of entries)
    public int[] unknown4 = new int[4];
    public int entry_addr;
    public int gp_value;
    public int text_addr;
    public int text_size;
    public int data_size;
    public int bss_size;
    public int nsegment; // ? maybe init/text/sceStub.text/fini
    public int[] segmentaddr = new int[4]; // static memory footprint of the module
    public int[] segmentsize = new int[4]; // static memory footprint of the module

    // internal info
    public static final int size = 156;
    public final int address;
    public final boolean isFlashModule;
    private static SceModule previousModule; // The last module to be loaded, should be fixed up if that module gets unloaded

    // loader stuff
    public int fileFormat; // See Loader class for valid formats
    public String pspfilename; // boot path, for thread argument
    public PSF psf; // for xmb title, etc

    // The space consumed by the program image
    public int loadAddressLow, loadAddressHigh;
    public int baseAddress; // should in theory be the same as loadAddressLow

    // address/size pairs, used by the debugger/instruction counter
    //public int[] textsection; // see text_addr/text_size
    public int[] initsection;
    public int[] finisection;
    public int[] stubtextsection;

    // deferred import resolving
    public List<DeferredStub> unresolvedImports;
    public int importFixupAttempts;

    private static int sceModuleAddressOffset = 0x08410000;
    public SceModule(boolean isFlashModule) {
        this.isFlashModule = isFlashModule;

        modid = SceUidManager.getNewUid("SceModule");

        // Address this struct will be stored in PSP mem
        // TODO This messes with loader "base address" since the loader has new SceModule() right at the start, and we'd rather not use smem_high since it will make stack allocations "non-pretty"
        //address = pspSysMem.getInstance().malloc(2, pspSysMem.PSP_SMEM_Low, size, 0);
        //pspSysMem.getInstance().addSysMemInfo(2, "ModuleMgr", pspSysMem.PSP_SMEM_Low, size, address);
        address = sceModuleAddressOffset;
        sceModuleAddressOffset += (size + 64) & ~63;

        // Link SceModule structs together
        if (previousModule != null)
            previousModule.next = address;
        previousModule = this;

        // Internal context
        fileFormat = Loader.FORMAT_UNKNOWN;
        //textsection = new int[2];
        initsection = new int[2];
        finisection = new int[2];
        stubtextsection = new int[2];
        unresolvedImports = new LinkedList<DeferredStub>();
        importFixupAttempts = 0;
    }

    /** For use when unloading modules. */
    public void free() {
        pspSysMem.getInstance().free(address);
    }

    public void write(Memory mem, int address) {
        mem.write32(address, next);
        mem.write16(address + 4, attribute);
        mem.write8(address + 6, version[0]);
        mem.write8(address + 7, version[1]);
        Utilities.writeStringNZ(mem, address + 8, 28, modname);
        mem.write32(address + 36, unknown1);
        mem.write32(address + 40, unknown2);
        mem.write32(address + 44, modid);
        mem.write32(address + 48, unknown3[0]);
        mem.write32(address + 52, unknown3[1]);
        mem.write32(address + 56, unknown3[2]);
        mem.write32(address + 60, unknown3[3]);
        mem.write32(address + 64, ent_top);
        mem.write32(address + 68, ent_size);
        mem.write32(address + 72, stub_top);
        mem.write32(address + 76, stub_size);
        mem.write32(address + 80, unknown4[0]);
        mem.write32(address + 84, unknown4[1]);
        mem.write32(address + 88, unknown4[2]);
        mem.write32(address + 92, unknown4[3]);
        mem.write32(address + 96, entry_addr);
        mem.write32(address + 100, gp_value);
        mem.write32(address + 104, text_addr);
        mem.write32(address + 108, text_size);
        mem.write32(address + 112, data_size);
        mem.write32(address + 116, bss_size);
        mem.write32(address + 120, nsegment);
        mem.write32(address + 124, segmentaddr[0]);
        mem.write32(address + 128, segmentaddr[1]);
        mem.write32(address + 132, segmentaddr[2]);
        mem.write32(address + 136, segmentaddr[3]);
        mem.write32(address + 140, segmentsize[0]);
        mem.write32(address + 144, segmentsize[1]);
        mem.write32(address + 148, segmentsize[2]);
        mem.write32(address + 152, segmentsize[3]);
    }

    public void read(Memory mem, int address) {
        // TODO
        Emulator.log.error("UNIMPLEMENTED SceModule read");
    }

    /** initialise ourself from a PSPModuleInfo object.
     * PSPModuleInfo object comes from the loader/ELF. */
    public void copy(PSPModuleInfo moduleInfo) {
        attribute = (short)(moduleInfo.getM_attr() & 0xFFFF);
        version[0] = (byte)( moduleInfo.getM_version()       & 0xFF);
        version[1] = (byte)((moduleInfo.getM_version() >> 8) & 0xFF);
        modname = moduleInfo.getM_namez();
        gp_value = (int)(moduleInfo.getM_gp() & 0xFFFFFFFFL);
        ent_top = (int)moduleInfo.getM_exports();
        ent_size = (int)moduleInfo.getM_exp_end() - ent_top;
        stub_top = (int)moduleInfo.getM_imports();
        stub_size = (int)moduleInfo.getM_imp_end() - stub_top;
    }
}
