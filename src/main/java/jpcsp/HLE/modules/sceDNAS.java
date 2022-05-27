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
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer32;
import jpcsp.HLE.kernel.Managers;

import org.apache.log4j.Logger;

public class sceDNAS extends HLEModule {
    public static Logger log = Modules.getLogger("sceDNAS");
    private int eventFlagUid;

    @HLEUnimplemented
    @HLEFunction(nid = 0x0D560144, version = 150)
    public int sceDNASInit(int unknown1, int unknown2) {
    	eventFlagUid = Managers.eventFlags.sceKernelCreateEventFlag("SceDNASExternal", 0, 0, TPointer.NULL);

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xBE0998D7, version = 150)
    public int sceDNASTerm() {
    	// Has no parameters
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x45C1AAF5, version = 150)
    public int sceDNASGetEventFlag(@BufferInfo(usage=Usage.out) TPointer32 unknown) {
    	unknown.setValue(eventFlagUid);

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xF3787AD8, version = 150)
    public int sceDNASInternalStart(int unknown) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xCA8B8909, version = 150)
    public int sceDNASNetStart() {
    	// Has no parameters
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x9FF48DD3, version = 150)
    public int sceDNASStop() {
    	// Has no parameters
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x6929100C, version = 150)
    public int sceDNASGetProductCode() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xA646E771, version = 150)
    public int sceDNASGetState(@BufferInfo(usage=Usage.out) TPointer32 stateAddr, @BufferInfo(usage=Usage.out) TPointer32 errorCodeAddr) {
    	stateAddr.setValue(0);
    	errorCodeAddr.setValue(0);
    	Managers.eventFlags.sceKernelSetEventFlag(eventFlagUid, 1);

    	return 0;
    }
}
