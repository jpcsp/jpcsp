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

import static jpcsp.HLE.Modules.sceMeCoreModule;

import org.apache.log4j.Logger;

import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.Modules;
import jpcsp.memory.mmio.MMIOHandlerMeCore;

public class sceMePower extends HLEModule {
    public static Logger log = Modules.getLogger("sceMePower");

    @HLEFunction(nid = 0x1862B784, version = 150)
    public int sceMePower_driver_1862B784(int numerator, int denominator) {
    	return sceMeCoreModule.hleMeCore_driver_FA398D71(MMIOHandlerMeCore.MECommand.ME_CMD_CPU);
    }

    @HLEFunction(nid = 0xE9F69ACF, version = 150)
    public int sceMePower_driver_E9F69ACF(int numerator, int denominator) {
    	return sceMeCoreModule.hleMeCore_driver_FA398D71(MMIOHandlerMeCore.MECommand.ME_CMD_POWER);
    }

    @HLEFunction(nid = 0xB37562AA, version = 150)
    public int sceMePowerControlAvcPower(boolean power) {
    	// Can be ignored
    	return 0;
    }

    @HLEFunction(nid = 0x984E2608, version = 150)
    public int sceMePower_driver_984E2608(boolean enabled) {
    	MMIOHandlerMeCore.MECommand cmd;
    	if (enabled) {
    		cmd = MMIOHandlerMeCore.MECommand.ME_CMD_AW_EDRAM_BUS_CLOCK_ENABLE;
    	} else {
    		cmd = MMIOHandlerMeCore.MECommand.ME_CMD_AW_EDRAM_BUS_CLOCK_DISABLE;
    	}
    	return sceMeCoreModule.hleMeCore_driver_FA398D71(cmd);
    }
}
