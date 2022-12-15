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

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import jpcsp.HLE.BufferInfo;
import jpcsp.HLE.BufferInfo.LengthInfo;
import jpcsp.HLE.BufferInfo.Usage;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.Modules;
import jpcsp.HLE.PspString;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.kernel.types.pspIoDrv;

public class IoFileMgrForKernel extends HLEModule {
    public static Logger log = Modules.getLogger("IoFileMgrForKernel");
    private final Map<String, pspIoDrv> drivers = new HashMap<String, pspIoDrv>();

	@Override
	public void start() {
		drivers.clear();

		super.start();
	}

	public pspIoDrv getDriver(String driverName) {
		return drivers.get(driverName);
	}

	public int hleIoAddDrv(pspIoDrv pspIoDrv) {
    	drivers.put(pspIoDrv.name, pspIoDrv);

    	return 0;
	}

	public int hleIoDelDrv(String driverName) {
    	drivers.remove(driverName);

    	return 0;
	}

	@HLEUnimplemented
    @HLEFunction(nid = 0x8E982A74, version = 150)
    public int sceIoAddDrv(pspIoDrv pspIoDrv) {
    	return hleIoAddDrv(pspIoDrv);
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xC7F35804, version = 150)
    public int sceIoDelDrv(PspString driverName) {
    	return hleIoDelDrv(driverName.getString());
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x2B6A9B21, version = 660)
    @HLEFunction(nid = 0x30E8ABB3, version = 150)
    public int sceIoValidateFd(int fd, int unknownFlags) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x42F954D4, version = 150)
    @HLEFunction(nid = 0x22F15793, version = 660)
    public int sceIoAddHook(@BufferInfo(lengthInfo = LengthInfo.fixedLength, length = 20, usage = Usage.in) TPointer hook) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x18881E58, version = 150)
    public int sceIoGetFdDebugInfo() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x1FC0620B, version = 150)
    public int sceIoGetThreadCwd() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x49356C12, version = 150)
    public int sceIoGetIobUserLevel() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x76DA16E3, version = 150)
    public int sceIoTerminateFd() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x947D7A06, version = 150)
    public int sceIoReopen() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xC30581F4, version = 150)
    public int sceIoChangeThreadCwd() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xC658603A, version = 150)
    public int sceIoCloseAll() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xDCCD6185, version = 150)
    public int sceIoGetUID() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xE5323C5B, version = 150)
    public int IoFileMgrForKernel_E5323C5B() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xE972F70B, version = 150)
    public int IoFileMgrForKernel_E972F70B() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xE4D75BC0, version = 150)
    public int g_deleted_error() {
    	return 0;
    }
}
