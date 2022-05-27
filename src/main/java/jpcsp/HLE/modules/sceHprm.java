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
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer32;
import jpcsp.HLE.Modules;

import org.apache.log4j.Logger;

public class sceHprm extends HLEModule {
    public static Logger log = Modules.getLogger("sceHprm");

    private boolean enableRemote = false;
    private boolean enableHeadphone = false;
    private boolean enableMicrophone = false;

    @HLEUnimplemented
    @HLEFunction(nid = 0xC7154136, version = 150)
    public int sceHprmRegisterCallback() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x444ED0B7, version = 150)
    public int sceHprmUnregisterCallback() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x71B5FB67, version = 150)
    public int sceHprmGetHpDetect() {
    	return 0;
    }

    @HLEFunction(nid = 0x208DB1BD, version = 150)
    public boolean sceHprmIsRemoteExist() {
        if (log.isDebugEnabled()) {
        	log.debug(String.format("sceHprmIsRemoteExist returning %b", enableRemote));
        }

        return enableRemote;
    }

    @HLEFunction(nid = 0x7E69EDA4, version = 150)
    public boolean sceHprmIsHeadphoneExist() {
        if (log.isDebugEnabled()) {
        	log.debug(String.format("sceHprmIsHeadphoneExist returning %b", enableHeadphone));
        }

        return enableHeadphone;
    }

    @HLEFunction(nid = 0x219C58F1, version = 150)
    public boolean sceHprmIsMicrophoneExist() {
        if (log.isDebugEnabled()) {
        	log.debug(String.format("sceHprmIsMicrophoneExist returning %b", enableMicrophone));
        }

        return enableMicrophone;
    }

    @HLEFunction(nid = 0x1910B327, version = 150)
    public int sceHprmPeekCurrentKey(TPointer32 keyAddr) {
        keyAddr.setValue(0); // fake

        return 0; // check
    }

    @HLEFunction(nid = 0x2BCEC83E, version = 150)
    public int sceHprmPeekLatch(TPointer latchAddr) {
        return 0;
    }

    @HLEFunction(nid = 0x40D2F9F0, version = 150)
    @HLEFunction(nid = 0xE9B776BE, version = 660)
    public int sceHprmReadLatch(TPointer32 latchAddr) {
    	// Return dummy values
    	latchAddr.setValue( 0, 0);
    	latchAddr.setValue( 4, 0);
    	latchAddr.setValue( 8, 0);
    	latchAddr.setValue(12, 0);

    	return 0;
    }

    /**
     * @return 0 - Cable not connected
     * @return 1 - S-Video Cable / AV (composite) cable
     * @return 2 - D Terminal Cable / Component Cable
     * @return < 0 - Error
     **/
    @HLEUnimplemented
    @HLEFunction(nid = 0x1528D408, version = 150)
    public int sceHprm_driver_1528D408() {
        return 1;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xDC895B2B, version = 660)
    public int sceHprm_driver_DC895B2B() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xBAD0828E, version = 150)
    @HLEFunction(nid = 0x0B83352B, version = 660)
    public int sceHprmGetModel(@CanBeNull @BufferInfo(usage=Usage.out) TPointer32 unknown1, @CanBeNull @BufferInfo(usage=Usage.out) TPointer32 unknown2, @CanBeNull @BufferInfo(usage=Usage.out) TPointer32 unknown3, @CanBeNull @BufferInfo(usage=Usage.out) TPointer32 unknown4) {
    	// Return dummy values
    	unknown1.setValue(0);
    	unknown2.setValue(0);
    	unknown3.setValue(0);
    	unknown4.setValue(0);

        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x8EC787E0, version = 150)
    @HLEFunction(nid = 0xA0B1A19B, version = 660)
    public int sceHprmUpdateCableType() {
        return 0;
    }
}