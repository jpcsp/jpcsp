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

/**
 *
 * @author hli
 */
public class SceKernelModuleInfo {

    // PSP info
    // http://psp.jim.sh/pspsdk-doc/structSceKernelModuleInfo.html
    public final int size = 92;
    public int nsegment; // ?
    public int[] segmentaddr = new int[4]; // ?
    public int[] segmentsize = new int[4]; // ?
    public int entry_addr;
    public int gp_value;
    public int text_addr;
    public int text_size;
    public int data_size;
    public int bss_size;
    public short attribute;
    public byte[] version = new byte[2];
    public String name;

    // Internal info
    public final int uid;

    public SceKernelModuleInfo() {
        uid = SceUidManager.getNewUid("SceKernelModuleInfo");
    }

    /** SceKernelModuleInfo contains a subset of the data in SceModule */
    public void copy(SceModule sceModule) {
        nsegment = sceModule.nsegment;
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
        data_size = sceModule.data_size;
        bss_size = sceModule.bss_size;
        attribute = sceModule.attribute;
        version[0] = sceModule.version[0];
        version[1] = sceModule.version[1];
        name = sceModule.modname;
    }
}
