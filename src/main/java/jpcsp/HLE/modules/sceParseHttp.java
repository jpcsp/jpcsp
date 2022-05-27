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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jpcsp.Memory;
import jpcsp.HLE.BufferInfo;
import jpcsp.HLE.BufferInfo.LengthInfo;
import jpcsp.HLE.BufferInfo.Usage;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.Modules;
import jpcsp.HLE.PspString;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer32;
import jpcsp.HLE.kernel.types.SceKernelErrors;
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
			if (c == '\n' || c == '\r') {
				break;
			}
			line.append((char) c);
		}

		return line.toString();
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0xAD7BFDEF, version = 150)
	public int sceParseHttpResponseHeader(@BufferInfo(lengthInfo=LengthInfo.nextParameter, usage=Usage.in) TPointer header, int headerLength, PspString fieldName, @BufferInfo(usage=Usage.out) TPointer32 valueAddr, @BufferInfo(usage=Usage.out) TPointer32 valueLength) {
		IMemoryReader memoryReader = MemoryReader.getMemoryReader(header.getAddress(), headerLength, 1);
		int endAddress = header.getAddress() + headerLength;

		boolean found = false;
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
						found = true;
						if (log.isDebugEnabled()) {
							log.debug(String.format("sceParseHttpResponseHeader returning valueLength=0x%X: %s", valueLength.getValue(), Utilities.getMemoryDump(valueAddr.getValue(), valueLength.getValue())));
						}
						break;
					}
				}
			}
		}

		if (!found) {
			valueAddr.setValue(0);
			valueLength.setValue(0);
			return SceKernelErrors.ERROR_PARSE_HTTP_NOT_FOUND;
		}

		return memoryReader.getCurrentAddress() - 1 - header.getAddress();
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x8077A433, version = 150)
	public int sceParseHttpStatusLine(@BufferInfo(lengthInfo=LengthInfo.nextParameter, usage=Usage.in) TPointer header, int headerLength, @BufferInfo(usage=Usage.out) TPointer32 httpVersionMajorAddr, @BufferInfo(usage=Usage.out) TPointer32 httpVersionMinorAddr, @BufferInfo(usage=Usage.out) TPointer32 httpStatusCodeAddr, @BufferInfo(usage=Usage.out) TPointer32 httpStatusCommentAddr, @BufferInfo(usage=Usage.out) TPointer32 httpStatusCommentLengthAddr) {
		IMemoryReader memoryReader = MemoryReader.getMemoryReader(header.getAddress(), headerLength, 1);
		String headerString = getHeaderString(memoryReader);
		Pattern pattern = Pattern.compile("HTTP/(\\d)\\.(\\d)\\s+(\\d+)(.*)");
		Matcher matcher = pattern.matcher(headerString);
		if (!matcher.matches()) {
			return -1;
		}

		int httpVersionMajor = Integer.parseInt(matcher.group(1));
		int httpVersionMinor = Integer.parseInt(matcher.group(2));
		int httpStatusCode = Integer.parseInt(matcher.group(3));
		String httpStatusComment = matcher.group(4);

		httpVersionMajorAddr.setValue(httpVersionMajor);
		httpVersionMinorAddr.setValue(httpVersionMinor);
		httpStatusCodeAddr.setValue(httpStatusCode);
		httpStatusCommentAddr.setValue(header.getAddress() + headerString.indexOf(httpStatusComment));
		httpStatusCommentLengthAddr.setValue(httpStatusComment.length());

		return 0;
	}
}
