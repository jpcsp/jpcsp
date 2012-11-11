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
package jpcsp.HLE.modules271;

import org.apache.log4j.Logger;

import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLELogging;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.Modules;
import jpcsp.HLE.modules.HLEModule;

@HLELogging
public class sceUsbMic extends HLEModule {
	public static Logger log = Modules.getLogger("sceUsbMic");

	@Override
	public String getName() {
		return "sceUsbMic";
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x06128E42, version = 271)
	public int sceUsbMicPollInputEnd() {
		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x2E6DCDCD, version = 271)
	public int sceUsbMicInputBlocking() {
		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x45310F07, version = 271)
	public int sceUsbMicInputInitEx() {
		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x5F7F368D, version = 271)
	public int sceUsbMicInput() {
		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x63400E20, version = 271)
	public int sceUsbMicGetInputLength() {
		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0xB8E536EB, version = 271)
	public int sceUsbMicInputInit() {
		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0xF899001C, version = 271)
	public int sceUsbMicWaitInputEnd() {
		return 0;
	}
}
