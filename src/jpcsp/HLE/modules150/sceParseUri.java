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

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.log4j.Logger;

import jpcsp.HLE.CanBeNull;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLELogging;
import jpcsp.HLE.Modules;
import jpcsp.HLE.PspString;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer32;
import jpcsp.HLE.kernel.types.pspParsedUri;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.memory.IMemoryReader;
import jpcsp.memory.IMemoryWriter;
import jpcsp.memory.MemoryReader;
import jpcsp.memory.MemoryWriter;
import jpcsp.util.Utilities;

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

	protected int getHexValue(int hexChar) {
		if (hexChar >= '0' && hexChar <= '9') {
			return hexChar - '0';
		}
		if (hexChar >= 'A' && hexChar <= 'F') {
			return hexChar - 'A' + 10;
		}
		if (hexChar >= 'a' && hexChar <= 'f') {
			return hexChar - 'a' + 10;
		}

		return 0;
	}

	private int addString(TPointer workArea, int workAreaSize, int offset, String s) {
		if (s == null) {
			s = "";
		}

		int length = s.length() + 1;
		if (offset + length > workAreaSize) {
			length = workAreaSize - offset;
			if (length <= 0) {
				return offset;
			}
		}

		workArea.setStringNZ(offset, length, s);

		return offset + length;
	}

	private String getUriComponent(int componentAddr, int flags, int flag) {
		if ((flags & flag) == 0 || componentAddr == 0) {
			return null;
		}

		return Utilities.readStringZ(componentAddr);
	}

	@HLEFunction(nid = 0x568518C9, version = 150)
	public int sceUriParse(@CanBeNull TPointer parsedUriArea, PspString url, @CanBeNull TPointer workArea, @CanBeNull TPointer32 workAreaSizeAddr, int workAreaSize) {
		if (parsedUriArea.isNull() || workArea.isNull()) {
			// The required workArea size if maximum the size if the URL + 7 times the null-byte
			// for string termination.
			workAreaSizeAddr.setValue(url.getString().length() + 7);
			return 0;
		}

		// Parse the URL into URI components
		URI uri;
		try {
			uri = new URI(url.getString());
		} catch (URISyntaxException e) {
			log.error("parsedUriArea", e);
			return -1;
		}

		// Parsing of the userInfo in the format "<userName>:<password>"
		String userInfo = uri.getUserInfo();
		String userInfoUserName = userInfo;
		String userInfoPassword = "";
		int userInfoColon = userInfo.indexOf(":");
		if (userInfoColon >= 0) {
			userInfoUserName = userInfo.substring(0, userInfoColon);
			userInfoPassword = userInfo.substring(userInfoColon + 1);
		}

		pspParsedUri parsedUri = new pspParsedUri();
		int offset = 0;

		// Store the URI components in sequence into workArea
		// and store the respective addresses into the parsedUri structure.
		parsedUri.schemeAddr = workArea.getAddress() + offset;
		offset = addString(workArea, workAreaSize, offset, uri.getScheme());

		parsedUri.userInfoUserNameAddr = workArea.getAddress() + offset;
		offset = addString(workArea, workAreaSize, offset, userInfoUserName);

		parsedUri.userInfoPasswordAddr = workArea.getAddress() + offset;
		offset = addString(workArea, workAreaSize, offset, userInfoPassword);

		parsedUri.hostAddr = workArea.getAddress() + offset;
		offset = addString(workArea, workAreaSize, offset, uri.getHost());

		parsedUri.pathAddr = workArea.getAddress() + offset;
		offset = addString(workArea, workAreaSize, offset, uri.getPath());

		parsedUri.queryAddr = workArea.getAddress() + offset;
		offset = addString(workArea, workAreaSize, offset, uri.getQuery());

		parsedUri.fragmentAddr = workArea.getAddress() + offset;
		offset = addString(workArea, workAreaSize, offset, uri.getFragment());

		workAreaSizeAddr.setValue(offset);
		parsedUri.write(parsedUriArea);

		return 0;
	}

	@HLEFunction(nid = 0x7EE318AF, version = 150)
	public int sceUriBuild(@CanBeNull TPointer workArea, @CanBeNull TPointer32 workAreaSizeAddr, int workAreaSize, pspParsedUri parsedUri, int flags) {
		// Extract the URI components from the parseUri structure
		String scheme = getUriComponent(parsedUri.schemeAddr, flags, 0x1);
		String userInfoUserName = getUriComponent(parsedUri.userInfoUserNameAddr, flags, 0x10);
		String userInfoPassword = getUriComponent(parsedUri.userInfoPasswordAddr, flags, 0x20);
		String host = getUriComponent(parsedUri.hostAddr, flags, 0x2);
		String path = getUriComponent(parsedUri.pathAddr, flags, 0x8);
		String query = getUriComponent(parsedUri.queryAddr, flags, 0x40);
		String fragment = getUriComponent(parsedUri.fragmentAddr, flags, 0x80);
		int port = (flags & 0x4) != 0 ? parsedUri.port : -1;

		// Build the userInfo in format "<userName>:<password>"
		String userInfo = null;
		if (userInfoUserName != null || userInfoPassword != null) {
			if (userInfoUserName == null) {
				userInfo = ":" + userInfoPassword;
			} else if (userInfoPassword == null) {
				userInfo = userInfoUserName;
			} else {
				userInfo = userInfoUserName + ":" + userInfoPassword;
			}
		}

		// Build the complete URI
		URI uri;
		try {
			uri = new URI(scheme, userInfo, host, port, path, query, fragment);
		} catch (URISyntaxException e) {
			log.error("sceUriBuild", e);
			return -1;
		}

		// Return the URI and its size
		String resultUri = uri.toASCIIString();
		if (workArea.isNotNull()) {
			workArea.setStringNZ(workAreaSize, resultUri);

			if (log.isDebugEnabled()) {
				log.debug(String.format("sceUriBuild returning '%s'", resultUri));
			}
		}
		workAreaSizeAddr.setValue(resultUri.length());

		return 0;
	}

	@HLEFunction(nid = 0x49E950EC, version = 150)
	public int sceUriEscape(@CanBeNull TPointer escapedAddr, @CanBeNull TPointer32 escapedLengthAddr, int escapedBufferLength, TPointer source) {
		IMemoryReader memoryReader = MemoryReader.getMemoryReader(source.getAddress(), 1);
		IMemoryWriter memoryWriter = null;
		if (escapedAddr.isNotNull()) {
			memoryWriter = MemoryWriter.getMemoryWriter(escapedAddr.getAddress(), escapedBufferLength, 1);
		}
		int escapedLength = 0;
		while (true) {
			int c = memoryReader.readNext();
			if (c == 0) {
				if (escapedAddr.isNotNull()) {
					if (escapedLength < escapedBufferLength) {
						memoryWriter.writeNext(c);
					}
				}
				escapedLength++;
				break;
			}
			if (escapeCharTable[c]) {
				if (escapedAddr.isNotNull()) {
					if (escapedLength + 3 > escapedBufferLength) {
						break;
					}
					memoryWriter.writeNext('%');
					memoryWriter.writeNext(hexTable[c >> 4]);
					memoryWriter.writeNext(hexTable[c & 0x0F]);
				}
				escapedLength += 3;
			} else {
				if (escapedAddr.isNotNull()) {
					if (escapedLength + 1 > escapedBufferLength) {
						break;
					}
					memoryWriter.writeNext(c);
				}
				escapedLength++;
			}
		}
		if (memoryWriter != null) {
			memoryWriter.flush();
		}
		escapedLengthAddr.setValue(escapedLength);

		return 0;
	}

	@HLEFunction(nid = 0x062BB07E, version = 150)
	public int sceUriUnescape(@CanBeNull TPointer unescapedAddr, @CanBeNull TPointer32 unescapedLengthAddr, int unescapedBufferLength, TPointer source) {
		IMemoryReader memoryReader = MemoryReader.getMemoryReader(source.getAddress(), 1);
		IMemoryWriter memoryWriter = null;
		if (unescapedAddr.isNotNull()) {
			memoryWriter = MemoryWriter.getMemoryWriter(unescapedAddr.getAddress(), unescapedBufferLength, 1);
		}
		int unescapedLength = 0;
		while (true) {
			int c = memoryReader.readNext();
			if (c == 0) {
				if (unescapedAddr.isNotNull()) {
					if (unescapedLength < unescapedBufferLength) {
						memoryWriter.writeNext(c);
					}
				}
				unescapedLength++;
				break;
			}
			if (unescapedAddr.isNotNull()) {
				if (unescapedLength + 1 > unescapedBufferLength) {
					break;
				}
				if (c == '%') {
					int hex1 = memoryReader.readNext();
					int hex2 = memoryReader.readNext();
					c = (getHexValue(hex1) << 4) + getHexValue(hex2);
				}
				// Remark: '+' sign is not unescaped to ' ' by this function
				memoryWriter.writeNext(c);
			}
			unescapedLength++;
		}
		if (memoryWriter != null) {
			memoryWriter.flush();
		}
		unescapedLengthAddr.setValue(unescapedLength);

		return 0;
	}
}
