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
package jpcsp.format.rco.vsmx;

import java.nio.charset.Charset;

import org.apache.log4j.Logger;

public class VSMX {
	public static Logger log = Logger.getLogger("vsmx");
	private static final int VSMX_SIGNATURE = 0x584D5356; // "VSMX"
	private static final int VSMX_VERSION = 0x00010000;
	private byte[] buffer;
	private int offset;
	private VSMXHeader header;
	private VSMXMem mem;
	private String name;

	public VSMXMem getMem() {
		return mem;
	}

	private int read8() {
		return buffer[offset++] & 0xFF;
	}

	private int read16() {
		return read8() | (read8() << 8);
	}

	private int read32() {
		return read16() | (read16() << 16);
	}

	private void read(byte[] buffer) {
		for (int i = 0; i < buffer.length; i++) {
			buffer[i] = (byte) read8();
		}
	}

	private void seek(int offset) {
		this.offset = offset;
	}

	public VSMX(byte[] buffer, String name) {
		this.buffer = buffer;
		this.name = name;

		read();
	}

	public String getName() {
		return name;
	}

	private void readHeader() {
		header = new VSMXHeader();
		header.sig = read32();
		header.ver = read32();
		header.codeOffset = read32();
		header.codeLength = read32();
		header.textOffset = read32();
		header.textLength = read32();
		header.textEntries = read32();
		header.propOffset = read32();
		header.propLength = read32();
		header.propEntries = read32();
		header.namesOffset = read32();
		header.namesLength = read32();
		header.namesEntries = read32();
	}

	private static boolean isZero(byte[] buffer, int offset, int length) {
		for (int i = 0; i < length; i++) {
			if (buffer[offset + i] != (byte) 0) {
				return false;
			}
		}

		return true;
	}

	private String[] readStrings(int stringsOffset, int length, int entries, Charset charset, int bytesPerChar) {
		String[] strings = new String[entries];
		int stringIndex = 0;
		byte[] buffer = new byte[length];
		seek(stringsOffset);
		read(buffer);
		int stringStart = 0;
		for (int i = 0; i < length; i += bytesPerChar) {
			if (isZero(buffer, i, bytesPerChar)) {
				String s = new String(buffer, stringStart, i - stringStart, charset);
				stringStart = i + bytesPerChar;
				strings[stringIndex++] = s;
			}
		}

		if (stringIndex != entries) {
			log.warn(String.format("readStrings: incorrect number of strings read: stringsOffset=0x%X, length=0x%X, entries=0x%X, bytesPerChar=%d, read entries=0x%X", stringsOffset, length, entries, bytesPerChar, stringIndex));
		}

		return strings;
	}

	private void read() {
		readHeader();
		if (header.sig != VSMX_SIGNATURE) {
			log.warn(String.format("Invalid VSMX signature 0x%08X", header.sig));
			return;
		}
		if (header.ver != VSMX_VERSION) {
			log.warn(String.format("Invalid VSMX version 0x%08X", header.ver));
			return;
		}

		if (header.codeOffset > header.size()) {
			log.warn(String.format("VSMX: skipping range after header: 0x%X-0x%X", header.size(), header.codeOffset));
			seek(header.codeOffset);
		}

		if ((header.codeLength % VSMXGroup.SIZE_OF) != 0) {
			log.warn(String.format("VSMX: code length is not aligned to 8 bytes: 0x%X", header.codeLength));
		}

		mem = new VSMXMem();
		mem.codes = new VSMXGroup[header.codeLength / VSMXGroup.SIZE_OF];
		for (int i = 0; i < mem.codes.length; i++) {
			mem.codes[i] = new VSMXGroup();
			mem.codes[i].id = read32();
			mem.codes[i].value = read32();
		}

		mem.texts = readStrings(header.textOffset, header.textLength, header.textEntries, Charset.forName("UTF-16LE"), 2);
		mem.properties = readStrings(header.propOffset, header.propLength, header.propEntries, Charset.forName("UTF-16LE"), 2);
		mem.names = readStrings(header.namesOffset, header.namesLength, header.namesEntries, Charset.forName("ISO-8859-1"), 1);

		if (log.isDebugEnabled()) {
			debug();
		}
	}

	public void debug() {
		for (int i = 0; i < mem.codes.length; i++) {
			StringBuilder s = new StringBuilder();
			VSMXGroup code = mem.codes[i];
			int opcode = code.id & 0xFF;
			if (opcode >= 0 && opcode < VSMXCode.VsmxDecOps.length) {
				s.append(VSMXCode.VsmxDecOps[opcode]);
			} else {
				s.append(String.format("UNKNOWN_%X", opcode));
			}

			switch (opcode) {
				case VSMXCode.VID_CONST_BOOL:
					if (code.value == 1) {
						s.append(" true");
					} else if (code.value == 0) {
						s.append(" false");
					} else {
						s.append(String.format(" 0x%X", code.value));
					}
					break;
				case VSMXCode.VID_CONST_INT:
				case VSMXCode.VID_DEBUG_LINE:
					s.append(String.format(" %d", code.value));
					break;
				case VSMXCode.VID_CONST_FLOAT:
					s.append(String.format(" %f", code.getFloatValue()));
					break;
				case VSMXCode.VID_CONST_STRING:
				case VSMXCode.VID_DEBUG_FILE:
					s.append(String.format(" '%s'", mem.texts[code.value]));
					break;
				case VSMXCode.VID_VARIABLE:
					s.append(String.format(" %s", mem.names[code.value]));
					break;
				case VSMXCode.VID_PROPERTY:
				case VSMXCode.VID_METHOD:
				case VSMXCode.VID_SET_ATTR:
				case VSMXCode.VID_UNSET:
				case VSMXCode.VID_OBJ_ADD_ATTR:
					s.append(String.format(" %s", mem.properties[code.value]));
					break;
				case VSMXCode.VID_FUNCTION:
					int n = (code.id >> 16) & 0xFF;
					if (n != 0) {
						log.warn(String.format("Unexpected localvars value for function at line %d, expected 0, got %d", i, n));
					}
					int args = (code.id >> 8) & 0xFF;
					int localVars = (code.id >> 24) & 0xFF;
					s.append(String.format(" args=%d, localVars=%d, startLine=%d", args, localVars, code.value));
					break;
				case VSMXCode.VID_UNNAMED_VAR:
					s.append(String.format(" %d", code.value));
					break;
				// jumps
				case VSMXCode.VID_JUMP:
				case VSMXCode.VID_JUMP_TRUE:
				case VSMXCode.VID_JUMP_FALSE:
					s.append(String.format(" line=%d", code.value));
					break;
				// function calls
				case VSMXCode.VID_CALL_FUNC:
				case VSMXCode.VID_CALL_METHOD:
				case VSMXCode.VID_CALL_NEW:
					s.append(String.format(" args=%d", code.value));
					break;
				case VSMXCode.VID_MAKE_FLOAT_ARRAY:
					s.append(String.format(" items=%d", code.value));
					break;
				// ops w/o arg - check for zero
				case VSMXCode.VID_OPERATOR_ASSIGN:
				case VSMXCode.VID_OPERATOR_ADD:
				case VSMXCode.VID_OPERATOR_SUBTRACT:
				case VSMXCode.VID_OPERATOR_MULTIPLY:
				case VSMXCode.VID_OPERATOR_DIVIDE:
				case VSMXCode.VID_OPERATOR_MOD:
				case VSMXCode.VID_OPERATOR_POSITIVE:
				case VSMXCode.VID_OPERATOR_NEGATE:
				case VSMXCode.VID_OPERATOR_NOT:
				case VSMXCode.VID_P_INCREMENT:
				case VSMXCode.VID_P_DECREMENT:
				case VSMXCode.VID_INCREMENT:
				case VSMXCode.VID_DECREMENT:
				case VSMXCode.VID_OPERATOR_TYPEOF:
				case VSMXCode.VID_OPERATOR_EQUAL:
				case VSMXCode.VID_OPERATOR_NOT_EQUAL:
				case VSMXCode.VID_OPERATOR_IDENTITY:
				case VSMXCode.VID_OPERATOR_NON_IDENTITY:
				case VSMXCode.VID_OPERATOR_LT:
				case VSMXCode.VID_OPERATOR_LTE:
				case VSMXCode.VID_OPERATOR_GT:
				case VSMXCode.VID_OPERATOR_GTE:
				case VSMXCode.VID_OPERATOR_B_AND:
				case VSMXCode.VID_OPERATOR_B_XOR:
				case VSMXCode.VID_OPERATOR_B_OR:
				case VSMXCode.VID_OPERATOR_B_NOT:
				case VSMXCode.VID_OPERATOR_LSHIFT:
				case VSMXCode.VID_OPERATOR_RSHIFT:
				case VSMXCode.VID_OPERATOR_URSHIFT:
				case VSMXCode.VID_STACK_COPY:
				case VSMXCode.VID_STACK_SWAP:
				case VSMXCode.VID_END_STMT:
				case VSMXCode.VID_CONST_NULL:
				case VSMXCode.VID_CONST_EMPTYARRAY:
				case VSMXCode.VID_CONST_OBJECT:
				case VSMXCode.VID_ARRAY:
				case VSMXCode.VID_THIS:
				case VSMXCode.VID_ARRAY_INDEX:
				case VSMXCode.VID_ARRAY_INDEX_ASSIGN:
				case VSMXCode.VID_ARRAY_PUSH:
				case VSMXCode.VID_RETURN:
				case VSMXCode.VID_END:
					if (code.value != 0) {
						log.warn(String.format("Unexpected non-zero value at line #%d: 0x%X!", i, code.value));
					}
					break;
				default:
					s.append(String.format(" 0x%X", code.value));
					break;
			}

			log.debug(String.format("Line#%d: %s", i, s.toString()));
		}

		log.debug(decompile());
	}

	private String decompile() {
		VSMXDecompiler decompiler = new VSMXDecompiler(this);

		return decompiler.toString();
	}
}
