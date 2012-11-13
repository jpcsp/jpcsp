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
package jpcsp.HLE.modules200;

import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLELogging;
import jpcsp.HLE.HLEUnimplemented;

import org.lwjgl.LWJGLException;

@HLELogging
public class sceDisplay extends jpcsp.HLE.modules150.sceDisplay {
	public sceDisplay() throws LWJGLException {
		super();
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0xBF79F646, version = 200)
	public int sceDisplayGetResumeMode() {
		return 0;
	}
    
	@HLEUnimplemented
	@HLEFunction(nid = 0x69B53541, version = 200)
	public int sceDisplayGetVblankRest() {
		return 0;
	}
    
	@HLEUnimplemented
	@HLEFunction(nid = 0x21038913, version = 200)
	public int sceDisplayIsVsync() {
		return 0;
	}
}