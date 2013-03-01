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
package jpcsp.HLE.modules150;

import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLELogging;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer32;
import jpcsp.HLE.Modules;
import jpcsp.HLE.modules.HLEModule;

import org.apache.log4j.Logger;

@HLELogging
public class sceHprm extends HLEModule {
    public static Logger log = Modules.getLogger("sceHprm");

    @Override
    public String getName() {
    	return "sceHprm";
	}
    
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
    public int sceHprmReadLatch(TPointer latchAddr) {
        return 0;
    }
}