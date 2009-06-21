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

import java.io.InputStream;
import java.io.OutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;

import jpcsp.Memory;
import jpcsp.memory.IMemoryReader;
import jpcsp.memory.MemoryReader;
import jpcsp.graphics.VideoEngine;
import jpcsp.HLE.kernel.types.PspGeList;

/** captures a display list
 * - PspGeList details
 * - backing RAM containing the GE instructions */
public class CaptureList {

    private static final int packetSize = 16;
    private PspGeList list;
    private CaptureRAM listBuffer;

    public CaptureList(PspGeList list) throws Exception {
        this.list = new PspGeList(list.list_addr, list.stall_addr, list.cbid, list.arg_addr);

        if (list.stall_addr - list.list_addr == 0) {
            throw new Exception("Command list is empty");
        }

        // don't support unterminated lists for now
        // TODO scan list for END command
        if (list.stall_addr == 0) {
            throw new Exception("Unterminated command list is not supported yet");
        }

        listBuffer = new CaptureRAM(list.list_addr & 0x3FFFFFFF, list.stall_addr - list.list_addr);
    }

    public void write(OutputStream out) throws IOException {
        DataOutputStream data = new DataOutputStream(out);

        data.writeInt(packetSize);
        data.writeInt(list.list_addr);
        data.writeInt(list.stall_addr);
        data.writeInt(list.cbid);
        data.writeInt(list.arg_addr);

        //VideoEngine.log.info("CaptureList write " + (5 * 4));

        CaptureHeader header = new CaptureHeader(CaptureHeader.PACKET_TYPE_RAM);
        header.write(out);
        listBuffer.write(out);
    }


    private CaptureList() {
    }

    public static CaptureList read(InputStream in) throws IOException {
        CaptureList list = new CaptureList();

        DataInputStream data = new DataInputStream(in);
        int sizeRemaining = data.readInt();
        if (sizeRemaining >= 16) {
            int list_addr = data.readInt(); sizeRemaining -= 4;
            int stall_addr = data.readInt(); sizeRemaining -= 4;
            int cbid = data.readInt(); sizeRemaining -= 4;
            int arg_addr = data.readInt(); sizeRemaining -= 4;
            data.skipBytes(sizeRemaining);

            list.list = new PspGeList(list_addr, stall_addr, cbid, arg_addr);

            CaptureHeader header = CaptureHeader.read(in);
            int packetType = header.getPacketType();
            if (packetType != CaptureHeader.PACKET_TYPE_RAM) {
                throw new IOException("Expected CaptureRAM(" + CaptureHeader.PACKET_TYPE_RAM + ") packet, found " + packetType);
            }
            list.listBuffer = CaptureRAM.read(in);
        } else {
            throw new IOException("Not enough bytes remaining in stream");
        }

        return list;
    }

    //public PspGeList getPspGeList() {
    //    return list;
    //}

    public void commit() {
        VideoEngine.getInstance().pushDrawList(list);
        listBuffer.commit();
    }
}
