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

import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.Modules;
import jpcsp.HLE.PspString;
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
    @HLEFunction(nid = 0x2B6A9B21, version = 150)
    public int IoFileMgrForKernel_30E8ABB3() {
    	return 0;
    }
}
