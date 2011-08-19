package jpcsp.HLE;

import jpcsp.Memory;
import java.io.*;

final public class TPointer implements ITPointerBase {
	private Memory memory;
	private int address;
	
	class TPointerOutputStream extends OutputStream {
		int offset = 0;
		
		public TPointerOutputStream(int offset) {
			this.offset = offset;
		}
		
		@Override
		public void write(int b) throws IOException {
			setValue8(offset++, (byte)b);
		}
	}
	
	class TPointerInputStream extends InputStream {
		int offset = 0;
		
		public TPointerInputStream(int offset) {
			this.offset = offset;
		}
		
		@Override
		public int read() throws IOException {
			return getValue8(offset++);
		}
		
	}

	public TPointer(Memory memory, int address) {
		this.memory = memory;
		this.address = address;
	}
	
	public boolean isAddressGood() {
		return Memory.isAddressGood(address);
	}
	
	public int getAddress() {
		return address;
	}
	
	public Memory getMemory() {
		return memory;
	}

	public byte  getValue8() { return getValue8(0); }
	public short getValue16() { return getValue16(0); }
	public int   getValue32() { return getValue32(0); }
	public long  getValue64() { return getValue64(0); }

	public void setValue8(byte value) { setValue8(0, value); }
	public void setValue16(short value) { setValue16(0, value); }
	public void setValue32(int value) { setValue32(0, value); }
	public void setValue64(long value) { setValue64(0, value); }

	public byte  getValue8(int offset) { return (byte)this.memory.read8(this.address + offset); }
	public short getValue16(int offset) { return (short)this.memory.read16(this.address + offset); }
	public int   getValue32(int offset) { return this.memory.read32(this.address + offset); }
	public long  getValue64(int offset) { return this.memory.read64(this.address + offset); }
	
	public void setValue8(int offset, byte value) { if (isAddressGood()) this.memory.write8(this.address + offset, value); }
	public void setValue16(int offset, short value) { if (isAddressGood()) this.memory.write16(this.address + offset, value); }
	public void setValue32(int offset, int value) { if (isAddressGood()) this.memory.write32(this.address + offset, value); }
	public void setValue64(int offset, long value) { if (isAddressGood()) this.memory.write64(this.address + offset, value); }
	
	public void setObject(int offset, Object object) {
		SerializeMemory.serialize(object, new TPointerOutputStream(offset));
	}
	
	public <T> T getObject(Class<T> objectClass, int offset) {
		return SerializeMemory.unserialize(objectClass, new TPointerInputStream(offset));
	}

	/*
	public T getValue() {
		return getValue(0);
	}
	
	public void setValue(T value) {
		setValue(value, 0);
	}

	@SuppressWarnings("unchecked")
	public T getValue(int offset) {
		if (this.genericClass == Byte.class) {
			return (T)(Integer)this.memory.read8(this.address + offset);
		}
		else if (this.genericClass == Short.class) {
			return (T)(Integer)this.memory.read16(this.address + offset);
		}
		else if (this.genericClass == Integer.class) {
			return (T)(Integer)this.memory.read32(this.address + offset);
		}
		else if (this.genericClass == Long.class) {
			return (T)(Long)this.memory.read64(this.address + offset);
		}

		throw(new RuntimeException("Unknown type to get the value from '" + this.genericClass + "'"));	
	}
	
	public void setValue(T value, int offset) {
		if (this.genericClass == Byte.class) {
			this.memory.write8(this.address + offset, (byte)(int)(Integer)value);
		}
		else if (this.genericClass == Short.class) {
			this.memory.write16(this.address + offset, (short)(int)(Integer)value);
		}
		else if (this.genericClass == Integer.class) {
			this.memory.write32(this.address + offset, (int)(Integer)value);
		}
		else if (this.genericClass == Long.class) {
			this.memory.write64(this.address + offset, (long)(Long)value);
		}

		throw(new RuntimeException("Unknown type to set the value to '" + this.genericClass + "'"));	
	}
	*/
}
