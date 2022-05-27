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
package jpcsp.HLE.kernel.types;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import jpcsp.HLE.TPointer;
import jpcsp.util.Utilities;

public class SceNpTicket extends pspAbstractMemoryMappedStructure {
	public static final int NUMBER_PARAMETERS = 12;
	public int version;
	public int size;
	public int unknown;
	public int sizeParams;
	public List<TicketParam> parameters = new LinkedList<SceNpTicket.TicketParam>();
	public byte[] unknownBytes;

	public static class TicketParam {
		public static final int PARAM_TYPE_NULL = 0;
		public static final int PARAM_TYPE_INT = 1;
		public static final int PARAM_TYPE_LONG = 2;
		public static final int PARAM_TYPE_STRING = 4;
		public static final int PARAM_TYPE_DATE = 7;
		public static final int PARAM_TYPE_STRING_ASCII = 8;
		public int type;
		private byte[] value;

		public TicketParam(int type, byte[] value) {
			this.type = type;
			this.value = value;
		}

		public int getType() {
			return type;
		}

		private int getIntValue(int offset) {
			return Utilities.endianSwap32(Utilities.readUnaligned32(value, offset));
		}

		public int getIntValue() {
			return getIntValue(0);
		}

		public long getLongValue() {
			return (((long) getIntValue(0)) << 32) | (getIntValue(4) & 0xFFFFFFFFL);
		}

		public String getStringValue() {
			int length = value.length;
			for (int i = 0; i < value.length; i++) {
				if (value[i] == (byte) 0) {
					length = i;
					break;
				}
			}
			return new String(value, 0, length);
		}

		public byte[] getBytesValue() {
			return value;
		}

		public Date getDateValue() {
			return new Date(getLongValue());
		}

		public void writeForPSP(TPointer buffer) {
			switch (type) {
				case PARAM_TYPE_INT:
					// This value is written in PSP endianness
					buffer.setValue32(getIntValue());
					break;
				case PARAM_TYPE_DATE:
				case PARAM_TYPE_LONG:
					// This value is written in PSP endianness
					buffer.setValue64(getLongValue());
					break;
				case PARAM_TYPE_STRING:
				case PARAM_TYPE_STRING_ASCII:
					int length = value.length;
					if (length >= 256) {
						length = 255; // PSP returns maximum 255 bytes
					}
			    	Utilities.writeBytes(buffer.getAddress(), length, value, 0);
			    	// Add trailing 0
			    	buffer.setValue8(length, (byte) 0);
					break;
				default:
					// Copy nothing
			    	break;
			}
		}

		@Override
		public String toString() {
			switch (type) {
				case PARAM_TYPE_INT:
					return String.format("0x%X", getIntValue());
				case PARAM_TYPE_STRING_ASCII:
				case PARAM_TYPE_STRING:
					return getStringValue();
				case PARAM_TYPE_DATE:
					return getDateValue().toString();
				case PARAM_TYPE_NULL:
					return "null";
				case PARAM_TYPE_LONG:
					return String.format("0x%16X", getLongValue());
			}
			return String.format("type=%d, value=%s", type, Utilities.getMemoryDump(value, 0, value.length));
		}
	}

	@Override
	protected void read() {
		version = read32();
		size = endianSwap32(read32());
		unknown = endianSwap16((short) read16());
		sizeParams = endianSwap16((short) read16());

		parameters.clear();
		for (int i = 0; i < NUMBER_PARAMETERS; i++) {
			int type = endianSwap16((short) read16());
			int length = endianSwap16((short) read16());

			byte[] value = new byte[length];
			read8Array(value);

			TicketParam ticketParam = new TicketParam(type, value);
			parameters.add(ticketParam);
		}

		unknownBytes = new byte[size - getOffset() + 8];
		read8Array(unknownBytes);
	}

	@Override
	protected void write() {
		write32(version);
		write32(endianSwap32(size));
		write16((short) endianSwap16((short) unknown));
		write16((short) endianSwap16((short) sizeParams));

		for (TicketParam ticketParam : parameters) {
			write16((short) endianSwap16((short) ticketParam.getType()));
			byte[] value = ticketParam.getBytesValue();
			write16((short) endianSwap16((short) value.length));
			write8Array(value);
		}

		write8Array(unknownBytes);
	}

	public byte[] toByteArray() {
		byte[] bytes = new byte[sizeof()];
		ByteBuffer b = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);

		b.putInt(version);
		b.putInt(endianSwap32(size));
		b.putShort((short) endianSwap16((short) unknown));
		b.putShort((short) endianSwap16((short) sizeParams));

		for (TicketParam ticketParam : parameters) {
			b.putShort((short) endianSwap16((short) ticketParam.getType()));
			byte[] value = ticketParam.getBytesValue();
			b.putShort((short) endianSwap16((short) value.length));
			b.put(value);
		}

		b.put(unknownBytes);

		return bytes;
	}

	public void read(byte[] bytes, int offset, int length) {
		ByteBuffer b = ByteBuffer.wrap(bytes, offset, length).order(ByteOrder.LITTLE_ENDIAN);

		version = b.getInt();
		size = endianSwap32(b.getInt());
		unknown = endianSwap16(b.getShort());
		sizeParams = endianSwap16(b.getShort());
		int readSize = 12;

		parameters.clear();
		for (int i = 0; i < NUMBER_PARAMETERS; i++) {
			int type = endianSwap16(b.getShort());
			int valueLength = endianSwap16(b.getShort());
			byte[] value = new byte[valueLength];
			b.get(value);

			readSize += 4 + valueLength;

			TicketParam ticketParam = new TicketParam(type, value);
			parameters.add(ticketParam);
		}

		unknownBytes = new byte[size - readSize + 8];
		b.get(unknownBytes);
	}

	@Override
	public int sizeof() {
		return size + 8;
	}

	@Override
	public String toString() {
		StringBuilder s = new StringBuilder();
		s.append(String.format("version=0x%08X", version));
		s.append(String.format(", size=0x%X", size));
		s.append(String.format(", unknown=0x%X", unknown));
		for (int i = 0; i < parameters.size(); i++) {
			TicketParam param = parameters.get(i);
			s.append(String.format(", param#%d=%s", i, param));
		}
		s.append(String.format(", unknownBytes: %s", Utilities.getMemoryDump(unknownBytes, 0, unknownBytes.length)));

		return s.toString();
	}
}
