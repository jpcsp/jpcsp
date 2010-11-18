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
package jpcsp.util;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import jpcsp.Memory;

public class FIFOByteBuffer {
    private byte[] buffer;
    private int bufferReadOffset;
    private int bufferWriteOffset;
    private int bufferLength;

    public FIFOByteBuffer() {
    	buffer = new byte[0];
    	clear();
    }

    public FIFOByteBuffer(byte[] buffer) {
    	this.buffer = buffer;
    	bufferReadOffset = 0;
    	bufferWriteOffset = 0;
    	bufferLength = buffer.length;
    }

    private int incrementOffset(int offset, int n) {
    	offset += n;
    	if (offset >= buffer.length) {
    		offset -= buffer.length;
    	}

    	return offset;
    }

    public void clear() {
    	bufferReadOffset = 0;
    	bufferWriteOffset = 0;
    	bufferLength = 0;
    }

    private void checkBufferForWrite(int length) {
    	if (bufferLength + length > buffer.length) {
    		// The buffer has to be extended
    		byte[] extendedBuffer = new byte[bufferLength + length];
    		if (bufferReadOffset + bufferLength <= buffer.length) {
    			System.arraycopy(buffer, bufferReadOffset, extendedBuffer, 0, bufferLength);
    		} else {
    			int lengthEndBuffer = buffer.length - bufferReadOffset;
    			System.arraycopy(buffer, bufferReadOffset, extendedBuffer, 0, lengthEndBuffer);
    			System.arraycopy(buffer, 0, extendedBuffer, lengthEndBuffer, bufferLength - lengthEndBuffer);
    		}
    		buffer = extendedBuffer;
			bufferReadOffset = 0;
			bufferWriteOffset = bufferLength;
    	}
    }

    public void write(Buffer src, int length) {
    	checkBufferForWrite(length);

    	// Copy the src content to the buffer at offset bufferWriteOffset
    	if (bufferWriteOffset + length <= buffer.length) {
    		// No buffer wrap, only 1 copy operation necessary
    		ByteBuffer byteBuffer = ByteBuffer.wrap(buffer, bufferWriteOffset, length);
    		Utilities.putBuffer(byteBuffer, src, ByteOrder.LITTLE_ENDIAN, length);
    	} else {
    		// The buffer wraps at the end, 2 copy operations necessary
    		int lengthEndBuffer = buffer.length - bufferWriteOffset;
    		ByteBuffer byteBuffer = ByteBuffer.wrap(buffer, bufferWriteOffset, lengthEndBuffer);
    		Utilities.putBuffer(byteBuffer, src, ByteOrder.LITTLE_ENDIAN, lengthEndBuffer);
    		byteBuffer = ByteBuffer.wrap(buffer, 0, length - lengthEndBuffer);
    		Utilities.putBuffer(byteBuffer, src, ByteOrder.LITTLE_ENDIAN, length - lengthEndBuffer);
    	}
    	bufferWriteOffset = incrementOffset(bufferWriteOffset, length);
        bufferLength += length;
    }

    public void write(int address, int length) {
        if (length > 0 && Memory.isAddressGood(address)) {
        	Buffer memoryBuffer = Memory.getInstance().getBuffer(address, length);
        	write(memoryBuffer, length);
        }
    }

    public void write(ByteBuffer src) {
    	write(src, src.remaining());
    }

    public void write(byte[] src) {
    	write(ByteBuffer.wrap(src), src.length);
    }

    public int readByteBuffer(ByteBuffer dst) {
    	int length = dst.remaining();
    	if (length > bufferLength) {
    		length = bufferLength;
    	}

    	if (bufferReadOffset + length > buffer.length) {
    		int lengthEndBuffer = buffer.length - bufferReadOffset;
    		dst.put(buffer, bufferReadOffset, lengthEndBuffer);
    		dst.put(buffer, 0, length - lengthEndBuffer);
    	} else {
    		dst.put(buffer, bufferReadOffset, length);
    	}
		bufferReadOffset = incrementOffset(bufferReadOffset, length);
		bufferLength -= length;

    	return length;
	}

	public int length() {
    	return bufferLength;
    }

	public void delete() {
		buffer = null;
	}

	@Override
	public String toString() {
		return String.format("FIFOByteBuffer(size=%d, bufferLength=%d, readOffset=%d, writeOffset=%d)", buffer.length, bufferLength, bufferReadOffset, bufferWriteOffset);
	}
}
