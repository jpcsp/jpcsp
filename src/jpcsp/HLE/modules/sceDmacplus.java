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

import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.Modules;
import jpcsp.graphics.GeCommands;

public class sceDmacplus extends HLEModule {
    public static Logger log = Modules.getLogger("sceDmacplus");
    public static final int pixelFormatFromCode[] = {
    		GeCommands.PSM_32BIT_ABGR8888,
    		GeCommands.PSM_16BIT_BGR5650,
    		GeCommands.PSM_16BIT_ABGR5551,
    		GeCommands.PSM_16BIT_ABGR4444
    };

    @HLEUnimplemented
	@HLEFunction(nid = 0xE9B746F9, version = 150)
	public int sceDmacplusLcdcDisable() {
    	return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0xED849260, version = 150)
	public int sceDmacplusLcdcEnable() {
    	return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0x88ACB6F1, version = 150)
	public int sceDmacplusLcdcSetFormat(int displayWidth, int displayFrameBufferWidth, int displayPixelFormatCoded) {
    	int pixelFormat = pixelFormatFromCode[displayPixelFormatCoded];
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("sceDmacplusLcdcSetFormat pixelFormat=%d", pixelFormat));
    	}
    	return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0xA3AA8D00, version = 150)
	public int sceDmacplusLcdcSetBaseAddr(int frameBufferAddress) {
    	return 0;
	}
}
