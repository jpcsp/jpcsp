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

package jpcsp.media;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

import jpcsp.Memory;
import jpcsp.memory.IMemoryReader;
import jpcsp.memory.MemoryReader;

/*
 * Common interface for PSMF/MPEG -> Media Engine communication.
 *
 */
public class PacketChannel implements ReadableByteChannel {
    private byte[] buffer;
    private int bufferReadOffset;
    private int bufferWriteOffset;
    private int bufferLength;

    public PacketChannel() {
    	buffer = new byte[0];
    	clear();
    }

    public PacketChannel(byte[] buffer) {
    	this.buffer = buffer;
    	bufferReadOffset = 0;
    	bufferWriteOffset = 0;
    	bufferLength = buffer.length;
    }

    private int incrementOffset(int offset) {
    	return incrementOffset(offset, 1);
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

    public void writePacket(int address, int length) {
        if (length > 0 && Memory.isAddressGood(address)) {
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

        	IMemoryReader memoryReader = MemoryReader.getMemoryReader(address, length, 1);
            for (int i = 0; i < length; i++) {
                buffer[bufferWriteOffset] = (byte) memoryReader.readNext();
                bufferWriteOffset = incrementOffset(bufferWriteOffset);
            }
            bufferLength += length;
        }
    }

    @Override
	public int read(ByteBuffer dst) throws IOException {
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

    @Override
	public void close() throws IOException {
    	buffer = null;
	}

    @Override
	public boolean isOpen() {
		return true;
	}

    public int length() {
    	return bufferLength;
    }

    @Override
	public String toString() {
		return String.format("PacketChannel(size=%d, bufferLength=%d, readOffset=%d, writeOffset=%d)", buffer.length, bufferLength, bufferReadOffset, bufferWriteOffset);
	}
}