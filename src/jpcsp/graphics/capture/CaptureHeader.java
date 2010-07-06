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

/**
 * generic packet header for saved capture stream
 * immutable */
public class CaptureHeader {

    // for use by other classes
    public static final int PACKET_TYPE_RESERVED = 0;
    public static final int PACKET_TYPE_LIST = 1;
    public static final int PACKET_TYPE_RAM = 2;
    public static final int PACKET_TYPE_DISPLAY_DETAILS = 3;
    public static final int PACKET_TYPE_FRAMEBUF_DETAILS = 4;


    private static final int size = 4;
    private int packetType;

    public CaptureHeader(int packetType) {
        this.packetType = packetType;
    }

    public void write(OutputStream out) throws IOException {
        DataOutputStream data = new DataOutputStream(out);
        data.writeInt(size);
        data.writeInt(packetType);
    }

    private CaptureHeader() {
    }

    public static CaptureHeader read(InputStream in) throws IOException {
        CaptureHeader header = new CaptureHeader();

        DataInputStream data = new DataInputStream(in);
        int sizeRemaining = data.readInt();
        if (sizeRemaining >= size) {
            header.packetType = data.readInt(); sizeRemaining -= 4;
            data.skipBytes(sizeRemaining);
            //VideoEngine.log.info("CaptureHeader type " + header.packetType);
        } else {
            throw new IOException("Not enough bytes remaining in stream");
        }

        return header;
    }

    public int getPacketType() {
        return packetType;
    }
}
