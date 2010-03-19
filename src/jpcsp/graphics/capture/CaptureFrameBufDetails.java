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

import jpcsp.HLE.pspdisplay;

/** captures sceDisplaySetFrameBuf */
public class CaptureFrameBufDetails {

    private static final int packetSize = 4 * 4;
    private int topaddrFb;
    private int bufferwidthFb;
    private int pixelformatFb;
    private int sync;

    public CaptureFrameBufDetails() {
        pspdisplay display = pspdisplay.getInstance();

        topaddrFb = display.getTopAddrFb();
        bufferwidthFb = display.getBufferWidthFb();
        pixelformatFb = display.getPixelFormatFb();
        sync = display.getSync();
    }

    public void write(OutputStream out) throws IOException {
        DataOutputStream data = new DataOutputStream(out);

        data.writeInt(packetSize);

        data.writeInt(topaddrFb);
        data.writeInt(bufferwidthFb);
        data.writeInt(pixelformatFb);
        data.writeInt(sync);

        //VideoEngine.log.info("CaptureDisplayDetails write " + (4 + packetSize));
    }

    public static CaptureFrameBufDetails read(InputStream in) throws IOException {
        CaptureFrameBufDetails details = new CaptureFrameBufDetails();

        DataInputStream data = new DataInputStream(in);
        int sizeRemaining = data.readInt();
        if (sizeRemaining >= packetSize) {
            details.topaddrFb = data.readInt(); sizeRemaining -= 4;
            details.bufferwidthFb = data.readInt(); sizeRemaining -= 4;
            details.pixelformatFb = data.readInt(); sizeRemaining -= 4;
            details.sync = data.readInt(); sizeRemaining -= 4;

            data.skipBytes(sizeRemaining);
        } else {
            throw new IOException("Not enough bytes remaining in stream");
        }

        return details;
    }

    public void commit() {
        pspdisplay display = pspdisplay.getInstance();

        // This is almost side effect free, but replay is going to trash the emulator state anyway
        display.sceDisplaySetFrameBuf(topaddrFb, bufferwidthFb, pixelformatFb, sync);
    }
}
