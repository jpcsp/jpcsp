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

/**
 * generic packet header for saved capture stream
 * immutable
 **/
public class CaptureHeader {

    // for use by other classes
    public static final int PACKET_TYPE_RESERVED = 0;
    public static final int PACKET_TYPE_LIST = 1;
    public static final int PACKET_TYPE_RAM = 2;
    public static final int PACKET_TYPE_FRAMEBUF_DETAILS = 3;

    private static final int size = 4;
    private int packetType;

    public CaptureHeader(int packetType) {
        this.packetType = packetType;
    }

    public void write(DataOutputStream out) throws IOException {
        out.writeInt(size);
        out.writeInt(packetType);
    }

    private CaptureHeader() {
    }

    public static CaptureHeader read(DataInputStream in) throws IOException {
        CaptureHeader header = new CaptureHeader();

        int sizeRemaining = in.readInt();
        if (sizeRemaining >= size) {
            header.packetType = in.readInt(); sizeRemaining -= 4;
            in.skipBytes(sizeRemaining);
        } else {
            throw new IOException("Not enough bytes remaining in stream");
        }

        return header;
    }

    public int getPacketType() {
        return packetType;
    }
}
