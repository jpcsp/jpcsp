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
import jpcsp.HLE.CanBeNull;
import jpcsp.HLE.BufferInfo.LengthInfo;
import jpcsp.HLE.BufferInfo.Usage;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.Modules;
import jpcsp.HLE.PspString;
import jpcsp.HLE.TPointer;

public class scePsheet extends HLEModule {
    public static Logger log = Modules.getLogger("scePsheet");
    protected TPointer address;
    protected int size;

    @HLEUnimplemented
    @HLEFunction(nid = 0x302AB4B8, version = 150)
    public int sceDRMInstallInit(@CanBeNull TPointer address, int size) {
    	this.address = address;
    	this.size = size;

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x3CEC4078, version = 150)
    public int sceDRMInstallEnd() {
    	address = TPointer.NULL;
    	size = 0;

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x15355B0E, version = 150)
    public int sceDRMInstallGetPkgInfo(PspString fileName, @CanBeNull @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=16, usage=Usage.in) TPointer key, @BufferInfo(length = 16, usage = Usage.out) TPointer pkgInfo) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xE16F3A48, version = 150)
    public int sceDRMInstallInstall(PspString fileName, @CanBeNull @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=16, usage=Usage.in) TPointer key, int unknown) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x226D9099, version = 150)
    public int sceDRMInstallAbort(int unknown1) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x34E68A41, version = 150)
    public int sceDRMInstallGetFileInfo(PspString fileName, @CanBeNull @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=16, usage=Usage.in) TPointer key, int unknown, @BufferInfo(length = 264, usage = Usage.out) TPointer fileInfo) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x3BA93CFA, version = 150)
    public int scePsheet_3BA93CFA(@BufferInfo(lengthInfo = LengthInfo.fixedLength, length = 8, usage = Usage.out) TPointer unknown) {
    	// Returning the progress of the installation?
    	unknown.setValue32(0, 0);
    	unknown.setValue32(4, 1);

    	return 0;
    }
}
