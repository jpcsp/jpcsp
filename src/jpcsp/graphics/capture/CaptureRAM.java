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
package jpcsp.graphics.capture;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;

import org.apache.log4j.Logger;

import jpcsp.Memory;
import jpcsp.memory.IMemoryReader;
import jpcsp.memory.MemoryReader;

/** captures a piece of RAM */
public class CaptureRAM {
	public static Logger log = CaptureManager.log;

	private static final int headerSize = 8;
    private int packetSize;
    private int address;
    private int length;
    private Buffer buffer;

    public CaptureRAM(int address, int length) throws IOException {
        packetSize = headerSize + length;
        this.address = address;
        this.length = length;

        Memory mem = Memory.getInstance();
        if (Memory.isAddressGood(address)) {
        	buffer = mem.getBuffer(address, length);
        }

        if (buffer == null) {
            throw new IOException(String.format("CaptureRAM: Unable to read buffer %08x - %08x", address, address + length));
        }
    }

    public void write(DataOutputStream out) throws IOException {
    	out.writeInt(packetSize);
    	out.writeInt(address);
    	out.writeInt(length);

        if (buffer instanceof ByteBuffer) {
            WritableByteChannel channel = Channels.newChannel(out);
            channel.write((ByteBuffer)buffer);
        } else {
            IMemoryReader reader = MemoryReader.getMemoryReader(address, length, 1);
            for (int i = 0; i < length; i++){
            	out.writeByte(reader.readNext());
            }
        }

        if (log.isDebugEnabled()) {
        	log.debug(String.format("Saved memory 0x%08X - 0x%08X (len 0x%X)", address, address + length, length));
        }
    }


    private CaptureRAM() {
    }

    public static CaptureRAM read(DataInputStream in) throws IOException {
        CaptureRAM ramFragment = new CaptureRAM();

        int sizeRemaining = in.readInt();
        if (sizeRemaining >= headerSize) {
            ramFragment.address = in.readInt(); sizeRemaining -= 4;
            ramFragment.length = in.readInt(); sizeRemaining -= 4;

            if (sizeRemaining > in.available()) {
                log.warn(String.format("CaptureRAM read want=0x%X, available=0x%X", sizeRemaining, in.available()));
            }

            if (sizeRemaining >= ramFragment.length) {
                ByteBuffer bb = ByteBuffer.allocate(ramFragment.length);
                byte[] b = bb.array();
                if (b == null) {
                    throw new IOException("Buffer is not backed by an array");
                }
                in.readFully(b, 0, ramFragment.length);
                ramFragment.buffer = bb;
                sizeRemaining -= ramFragment.length;

                in.skipBytes(sizeRemaining);

                if (log.isDebugEnabled()) {
                	log.debug(String.format("Loaded memory 0x%08X - 0x%08X (len 0x%X)", ramFragment.address, ramFragment.address + ramFragment.length, ramFragment.length));
                }
            } else {
                throw new IOException("Not enough bytes remaining in stream");
            }
        } else {
            throw new IOException("Not enough bytes remaining in stream");
        }

        return ramFragment;
    }

    public void commit() {
        Memory.getInstance().copyToMemory(address, (ByteBuffer)buffer, length);
    }

    public int getAddress() {
        return address;
    }

    public int getLength() {
        return length;
    }

    public Buffer getBuffer() {
        return buffer;
    }
}
