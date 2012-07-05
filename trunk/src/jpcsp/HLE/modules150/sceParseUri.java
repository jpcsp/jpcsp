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

import org.apache.log4j.Logger;

import jpcsp.Memory;
import jpcsp.HLE.CanBeNull;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.Modules;
import jpcsp.HLE.PspString;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer32;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.util.Utilities;

public class sceParseUri extends HLEModule {
	protected static Logger log = Modules.getLogger("sceParseUri");

	@Override
	public String getName() {
		return "sceParseUri";
	}

	@HLEFunction(nid = 0x568518C9, version = 150)
	public int sceUriParse(@CanBeNull TPointer parsedUriArea, PspString url, @CanBeNull TPointer workArea, @CanBeNull TPointer32 workAreaSizeAddr, int workAreaSize) {
		log.warn(String.format("Unimplement sceUriParse parsedUriArea=%s, url='%s', workArea=%s, workAreaSizeAddr=%s, workAreaSize=%d", parsedUriArea, url, workArea, workAreaSizeAddr, workAreaSize));
		Memory mem = Memory.getInstance();

		if (workArea.isNull()) {
			workAreaSizeAddr.setValue(32); // ???
		} else {
			Utilities.writeStringNZ(mem, workArea.getAddress(), workAreaSize, "Test sceUriParse");

			// Unknown structure for the parsedUriArea
			mem.memset(parsedUriArea.getAddress(), (byte) 0, 44);
			parsedUriArea.setValue32(0, url.getAddress());
			parsedUriArea.setValue32(4, url.getString().length());
		}

		return 0;
	}

	@HLEFunction(nid = 0x7EE318AF, version = 150)
	public int sceUriBuild(@CanBeNull TPointer workArea, @CanBeNull TPointer32 workAreaSizeAddr, int workAreaSize, @CanBeNull TPointer parsedUriArea, int unknown2) {
		log.warn(String.format("Unimplement sceUriBuild workArea=%s, workAreaSizeAddr=%s, workAreaSize=%d, parsedUriArea=%s, unknown2=0x%08X", workArea, workAreaSizeAddr, workAreaSize, parsedUriArea, unknown2));
		Memory mem = Memory.getInstance();

		// Retrieve values as set by sceUriParse
		int urlAddr = parsedUriArea.getValue32(0);
		int urlLength = parsedUriArea.getValue32(4);

		if (workArea.isNull()) {
			workAreaSizeAddr.setValue(urlLength); // ???
		} else {
			mem.memcpy(workArea.getAddress(), urlAddr, urlLength);
		}

		return 0;
	}

	@HLEFunction(nid = 0x49E950EC, version = 150)
	public int sceUriEscape() {
		log.warn(String.format("Unimplemented sceUriEscape <unknown parameters>"));

		return 0;
	}

	@HLEFunction(nid = 0x062BB07E, version = 150)
	public int sceUriUnescape() {
		log.warn(String.format("Unimplemented sceUriUnescape <unknown parameters>"));

		return 0;
	}
}
