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
import jpcsp.Memory;
import jpcsp.HLE.Modules;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.graphics.VideoEngine;

import org.apache.log4j.Logger;

@HLELogging
public class sceDmac extends HLEModule {
    public static Logger log = Modules.getLogger("sceDmac");

	@Override
	public String getName() {
		return "sceDmac";
	}

    @HLEFunction(nid = 0x617F3FE6, version = 150)
    public int sceDmacMemcpy(TPointer dest, TPointer source, int size) {
        // If copying to the VRAM or the frame buffer, do not cache the texture
        if (Memory.isVRAM(dest.getAddress()) || Modules.sceDisplayModule.isFbAddress(dest.getAddress())) {
        	VideoEngine.getInstance().addVideoTexture(dest.getAddress(), dest.getAddress() + size);
        }
        // If copying from the VRAM, force the saving of the GE to memory
        if (Memory.isVRAM(source.getAddress()) && Modules.sceDisplayModule.getSaveGEToTexture()) {
        	VideoEngine.getInstance().addVideoTexture(source.getAddress(), source.getAddress() + size);
        }

        Memory.getInstance().memcpy(dest.getAddress(), source.getAddress(), size);

        return 0;
    }

    @HLEUnimplemented
	@HLEFunction(nid = 0xD97F94D8, version = 150)
	public int sceDmacTryMemcpy() {
    	return 0;
	}
}