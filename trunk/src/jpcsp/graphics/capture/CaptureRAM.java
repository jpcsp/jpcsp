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
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;

import jpcsp.Memory;
import jpcsp.graphics.VideoEngine;
import jpcsp.memory.IMemoryReader;
import jpcsp.memory.MemoryReader;

/** captures a piece of RAM */
public class CaptureRAM {

    private int packetSize;
    private int address;
    private int length;
    private Buffer buffer;

    public CaptureRAM(int address, int length) throws IOException {
        packetSize = 8 + length;
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

    public void write(OutputStream out) throws IOException {
        DataOutputStream data = new DataOutputStream(out);

        data.writeInt(packetSize);
        data.writeInt(address);
        data.writeInt(length);

        if (buffer instanceof ByteBuffer) {
            WritableByteChannel channel = Channels.newChannel(out);
            channel.write((ByteBuffer)buffer);
        } else {
            IMemoryReader reader = MemoryReader.getMemoryReader(address, length, 1);
            for (int i = 0; i < length; i++){
                data.writeByte(reader.readNext());
            }
        }

        VideoEngine.log.info(String.format("Saved memory %08x - %08x (len %08x)", address, address + length, length));
        //VideoEngine.log.info("CaptureRAM write " + ((3 * 4) + length));

        //data.flush();
        //out.flush();
    }


    private CaptureRAM() {
    }

    public static CaptureRAM read(InputStream in) throws IOException {
        CaptureRAM ramFragment = new CaptureRAM();

        DataInputStream data = new DataInputStream(in);
        int sizeRemaining = data.readInt();
        if (sizeRemaining >= 8) {
            ramFragment.address = data.readInt(); sizeRemaining -= 4;
            ramFragment.length = data.readInt(); sizeRemaining -= 4;

            if (sizeRemaining > data.available()) {
                VideoEngine.log.warn("CaptureRAM read want=" + sizeRemaining + " available=" + data.available());
            }

            if (sizeRemaining >= ramFragment.length) {
                ByteBuffer bb = ByteBuffer.allocate(ramFragment.length);
                byte[] b = bb.array();
                if (b == null) {
                    throw new IOException("Buffer is not backed by an array");
                }
                data.readFully(b, 0, ramFragment.length);
                ramFragment.buffer = bb;
                sizeRemaining -= ramFragment.length;

                data.skipBytes(sizeRemaining);

                VideoEngine.log.info(String.format("Loaded memory %08x - %08x (len %08x)",
                    ramFragment.address, ramFragment.address + ramFragment.length, ramFragment.length));
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
