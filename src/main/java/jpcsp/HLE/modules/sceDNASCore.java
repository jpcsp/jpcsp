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

import org.apache.log4j.Logger;

import jpcsp.HLE.BufferInfo;
import jpcsp.HLE.BufferInfo.LengthInfo;
import jpcsp.HLE.BufferInfo.Usage;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointer;

public class sceDNASCore extends HLEModule {
    public static Logger log = Modules.getLogger("sceDNASCore");

    @HLEUnimplemented
    @HLEFunction(nid = 0xFA571A75, version = 150)
    public int sceDNASCoreInit() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xD5E80301, version = 150)
    public int sceDNASCoreTerm() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x15096ECD, version = 150)
    public int sceDNASCoreGetHostname(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=128, usage=Usage.out) TPointer hostname) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x2370130E, version = 150)
    public int sceDNASCoreCheckProxyResponse() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x26E1E2BD, version = 150)
    public int sceDNASCoreSetChallenge() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x2B6C67EA, version = 150)
    public int sceDNASCoreCheckGameInfoFlag() {
    	// Has no parameters
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x4108128B, version = 150)
    public int sceDNASCoreMakeConnect() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x80CEC43A, version = 150)
    public int sceDNASCoreMakeResponse() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x822357BB, version = 150)
    public int sceDNASCoreGetResponse() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x8309549E, version = 150)
    public int sceDNASCoreSetResult() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xB6C76A14, version = 150)
    public int sceDNASCoreCheckChallenge() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xBA0A32CA, version = 150)
    public int sceDNASCoreCheckResult() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xBA0D27F8, version = 150)
    public int sceDNASCore_lib_BA0D27F8() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xBF6A7475, version = 150)
    public int sceDNASCoreGetProductCode() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xC54657B7, version = 150)
    public int sceDNASCoreSetProxyResponse() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xDA5939B4, version = 150)
    public int sceDNASCoreGetProxyRequest() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xF0EB4367, version = 150)
    public int sceDNASCoreGetConnect(@BufferInfo(lengthInfo=LengthInfo.returnValue, usage=Usage.out) TPointer connectAddr, int length) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xF479F616, version = 150)
    public int sceDNASCoreGetHostnameBase(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=128, usage=Usage.out) TPointer hostnameBase) {
    	return 0;
    }
}
