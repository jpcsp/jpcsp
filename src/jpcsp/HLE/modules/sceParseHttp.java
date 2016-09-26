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

import jpcsp.Memory;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.Modules;
import jpcsp.HLE.PspString;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer32;
import jpcsp.memory.IMemoryReader;
import jpcsp.memory.MemoryReader;
import jpcsp.util.Utilities;

import org.apache.log4j.Logger;

public class sceParseHttp extends HLEModule {
	public static Logger log = Modules.getLogger("sceParseHttp");

	private String getHeaderString(IMemoryReader memoryReader) {
		StringBuilder line = new StringBuilder();
		while (true) {
			int c = memoryReader.readNext();
			if (c == '\n') {
				break;
			}
			line.append((char) c);
		}

		return line.toString();
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0xAD7BFDEF, version = 150)
	public int sceParseHttpResponseHeader(TPointer header, int headerLength, PspString fieldName, TPointer32 valueAddr, TPointer32 valueLength) {
		IMemoryReader memoryReader = MemoryReader.getMemoryReader(header.getAddress(), headerLength, 1);
		int endAddress = header.getAddress() + headerLength;

		while (memoryReader.getCurrentAddress() < endAddress) {
			int addr = memoryReader.getCurrentAddress();
			String headerString = getHeaderString(memoryReader);
			String[] fields = headerString.split(" *: *", 2);
			if (fields != null && fields.length == 2) {
				if (fields[0].equalsIgnoreCase(fieldName.getString())) {
					addr += fields[0].length();
					Memory mem = header.getMemory();
					int c;
					while (true) {
						c = mem.read8(addr);
						if (c != ' ') {
							break;
						}
						addr++;
					}
					c = mem.read8(addr++);
					if (c == ':') {
						while (true) {
							c = mem.read8(addr);
							if (c != ' ') {
								break;
							}
							addr++;
						}

						valueLength.setValue(memoryReader.getCurrentAddress() - addr - 1);
						valueAddr.setValue(addr);
						if (log.isDebugEnabled()) {
							log.debug(String.format("sceParseHttpResponseHeader returning valueLength=0x%X: %s", valueLength.getValue(), Utilities.getMemoryDump(valueAddr.getValue(), valueLength.getValue())));
						}
						break;
					}
				}
			}
		}

		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x8077A433, version = 150)
	public int sceParseHttpStatusLine() {
		return 0;
	}
}
