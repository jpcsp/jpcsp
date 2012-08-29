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
import jpcsp.HLE.HLELogging;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.Modules;
import jpcsp.HLE.PspString;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer32;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.memory.IMemoryReader;
import jpcsp.memory.IMemoryWriter;
import jpcsp.memory.MemoryReader;
import jpcsp.memory.MemoryWriter;

@HLELogging
public class sceParseUri extends HLEModule {
	public static Logger log = Modules.getLogger("sceParseUri");
	private static final boolean[] escapeCharTable = new boolean[] {
		 true,  true,  true,  true,  true,  true,  true,  true,  true,  true,  true,  true,  true,  true,  true,  true,
		 true,  true,  true,  true,  true,  true,  true,  true,  true,  true,  true,  true,  true,  true,  true,  true,
		 true,  true,  true,  true,  true,  true,  true,  true,  true,  true, false,  true,  true, false, false,  true,
		false, false, false, false, false, false, false, false, false, false,  true,  true,  true,  true,  true,  true,
		 true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false,
		false, false, false, false, false, false, false, false, false, false, false,  true,  true,  true,  true, false,
		 true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false,
		false, false, false, false, false, false, false, false, false, false, false,  true,  true,  true,  true,  true,
		 true,  true,  true,  true,  true,  true,  true,  true,  true,  true,  true,  true,  true,  true,  true,  true,
		 true,  true,  true,  true,  true,  true,  true,  true,  true,  true,  true,  true,  true,  true,  true,  true,
		 true,  true,  true,  true,  true,  true,  true,  true,  true,  true,  true,  true,  true,  true,  true,  true,
		 true,  true,  true,  true,  true,  true,  true,  true,  true,  true,  true,  true,  true,  true,  true,  true,
		 true,  true,  true,  true,  true,  true,  true,  true,  true,  true,  true,  true,  true,  true,  true,  true,
		 true,  true,  true,  true,  true,  true,  true,  true,  true,  true,  true,  true,  true,  true,  true,  true,
		 true,  true,  true,  true,  true,  true,  true,  true,  true,  true,  true,  true,  true,  true,  true,  true,
		 true,  true,  true,  true,  true,  true,  true,  true,  true,  true,  true,  true,  true,  true,  true,  true
	};
	private static final int[] hexTable = new int[] {
		'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
	};

	@Override
	public String getName() {
		return "sceParseUri";
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x568518C9, version = 150)
	public int sceUriParse(@CanBeNull TPointer parsedUriArea, PspString url, @CanBeNull TPointer workArea, @CanBeNull TPointer32 workAreaSizeAddr, int workAreaSize) {
		if (workArea.isNull()) {
			workAreaSizeAddr.setValue(32); // ???
		} else {
			workArea.setStringNZ(workAreaSize, "Test sceUriParse");

			// Unknown structure for the parsedUriArea
			parsedUriArea.clear(44);
			parsedUriArea.setValue32(0, url.getAddress());
			parsedUriArea.setValue32(4, url.getString().length());
		}

		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x7EE318AF, version = 150)
	public int sceUriBuild(@CanBeNull TPointer workArea, @CanBeNull TPointer32 workAreaSizeAddr, int workAreaSize, @CanBeNull TPointer parsedUriArea, int unknown2) {
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

	@HLEUnimplemented
	@HLEFunction(nid = 0x49E950EC, version = 150)
	public int sceUriEscape(@CanBeNull TPointer escapedAddr, @CanBeNull TPointer32 escapedLengthAddr, int escapedBufferLength, TPointer source) {
		IMemoryReader memoryReader = MemoryReader.getMemoryReader(source.getAddress(), 1);
		IMemoryWriter memoryWriter = MemoryWriter.getMemoryWriter(escapedAddr.getAddress(), escapedBufferLength, 1);
		int escapedLength = 0;
		while (true) {
			int c = memoryReader.readNext();
			if (c == 0) {
				if (escapedLength < escapedBufferLength) {
					memoryWriter.writeNext(c);
				}
				break;
			}
			if (escapeCharTable[c]) {
				if (escapedLength + 3 > escapedBufferLength) {
					break;
				}
				memoryWriter.writeNext('%');
				memoryWriter.writeNext(hexTable[c >> 4]);
				memoryWriter.writeNext(hexTable[c & 0x0F]);
				escapedLength += 3;
			} else {
				if (escapedLength + 1 > escapedBufferLength) {
					break;
				}
				memoryWriter.writeNext(c);
				escapedLength++;
			}
		}
		memoryWriter.flush();
		escapedLengthAddr.setValue(escapedLength);

		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x062BB07E, version = 150)
	public int sceUriUnescape() {
		return 0;
	}
}
