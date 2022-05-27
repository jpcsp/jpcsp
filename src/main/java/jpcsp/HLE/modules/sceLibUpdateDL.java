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
package jpcsp.HLE.modules;

import jpcsp.HLE.BufferInfo;
import jpcsp.HLE.BufferInfo.Usage;
import jpcsp.HLE.CanBeNull;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.Modules;
import jpcsp.HLE.PspString;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer32;
import jpcsp.HLE.kernel.types.SceKernelErrors;

import org.apache.log4j.Logger;

public class sceLibUpdateDL extends HLEModule {
    public static Logger log = Modules.getLogger("sceLibUpdateDL");

    @HLEUnimplemented
    @HLEFunction(nid = 0xFC1AB540, version = 150)
    public int sceUpdateDownloadInit() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xF6690A9A, version = 150)
    public int sceUpdateDownloadInitEx(TPointer unknown1, TPointer unknown2) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xD6A09757, version = 150)
    public int sceUpdateDownloadEnd() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x4F49C9C1, version = 150)
    public int sceUpdateDownloadAbort() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xFD675E8D, version = 150)
    public int sceUpdateDownloadConnectServer(@CanBeNull @BufferInfo(usage=Usage.out) TPointer32 unknown1, @BufferInfo(usage=Usage.out) TPointer32 unknown2) {
    	unknown1.setValue(0);
    	unknown2.setValue(0);

    	return SceKernelErrors.ERROR_LIB_UPDATE_LATEST_VERSION_INSTALLED;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xA3000F72, version = 150)
    public int sceUpdateDownloadCreateCtx() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x782EF929, version = 150)
    public int sceUpdateDownloadDeleteCtx() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xFA9AA797, version = 150)
    public int sceUpdateDownloadReadData() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xC3E1C200, version = 150)
    public int sceUpdateDownloadSetBuildNum() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xB2EC0E06, version = 150)
    public int sceUpdateDownloadSetProductCode() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xC6BFE5B8, version = 150)
    public int sceUpdateDownloadSetRange() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x59106229, version = 150)
    public int sceUpdateDownloadSetUrl() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xC1AF1076, version = 150)
    public int sceUpdateDownloadSetVersion(int buildNumber) {
    	// E.g. buildNumber is 5455 (can also be found in version.txt)
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x34243B86, version = 150)
    public int sceLibUpdateDL_34243B86(PspString release) {
    	// E.g. release is "6.60"
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x88FF3935, version = 150)
    public int  sceUpdateDownloadSetDestCode(int destCode) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xB5DB018D, version = 150)
    public int  sceUpdateDownloadSetServerRegion(int serverRegion1, int serverRegion2) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x36D8F34B, version = 150)
    public int sceUpdate_36D8F34B(int unknown) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xF7E66CB4, version = 150)
    public int sceUpdate_F7E66CB4() {
    	return 0;
    }
}
