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
import jpcsp.MemoryMap;
import jpcsp.memory.IMemoryReader;
import jpcsp.memory.MemoryReader;
import jpcsp.graphics.VideoEngine;
import jpcsp.HLE.pspdisplay;

/** captures draw, depth and display buffers along with their settings (width, height, etc) */
public class CaptureDisplayDetails {

    private static final boolean captureRenderTargets = false;

    private static final int packetSize = (5 + 4) * 4;

    private int fbp;
    private int fbw;
    private int zbp;
    private int zbw;
    private int psm;

    private int topaddrFb;
    private int bufferwidthFb;
    private int pixelformatFb;
    private int sync;

    private CaptureRAM drawBuffer;
    private CaptureRAM depthBuffer;
    private CaptureRAM displayBuffer;

    public CaptureDisplayDetails()  throws IOException {
        VideoEngine ge = VideoEngine.getInstance();
        pspdisplay display = pspdisplay.getInstance();

        fbp = ge.getFBP();
        fbw = ge.getFBW();
        zbp = ge.getZBP();
        zbw = ge.getZBW();
        psm = ge.getPSM();

        topaddrFb = display.getTopAddrFb();
        bufferwidthFb = display.getBufferWidthFb();
        pixelformatFb = display.getPixelFormatFb();
        sync = display.getSync();

        // TODO clamp lengths to within valid RAM range
        int pixelFormatBytes = (psm == pspdisplay.PSP_DISPLAY_PIXEL_FORMAT_8888) ? 4 : 2;
        drawBuffer = new CaptureRAM(fbp + MemoryMap.START_VRAM, fbw * 272 * pixelFormatBytes);

        depthBuffer = new CaptureRAM(zbp + MemoryMap.START_VRAM, zbw * 272 * 2);

        pixelFormatBytes = (pixelformatFb == pspdisplay.PSP_DISPLAY_PIXEL_FORMAT_8888) ? 4 : 2;
        displayBuffer = new CaptureRAM(topaddrFb, bufferwidthFb * 272 * pixelFormatBytes);
    }

    public void write(OutputStream out) throws IOException {
        DataOutputStream data = new DataOutputStream(out);

        data.writeInt(packetSize);
        data.writeInt(fbp);
        data.writeInt(fbw);
        data.writeInt(zbp);
        data.writeInt(zbw);
        data.writeInt(psm);

        data.writeInt(topaddrFb);
        data.writeInt(bufferwidthFb);
        data.writeInt(pixelformatFb);
        data.writeInt(sync);

        //VideoEngine.log.info("CaptureDisplayDetails write " + (4 + packetSize));

        if (captureRenderTargets) {
            // write draw buffer
            CaptureHeader header = new CaptureHeader(CaptureHeader.PACKET_TYPE_RAM);
            header.write(out);
            drawBuffer.write(out);

            // write depth buffer
            header = new CaptureHeader(CaptureHeader.PACKET_TYPE_RAM);
            header.write(out);
            depthBuffer.write(out);

            // write display buffer
            header = new CaptureHeader(CaptureHeader.PACKET_TYPE_RAM);
            header.write(out);
            displayBuffer.write(out);
        }
    }

    public static CaptureDisplayDetails read(InputStream in) throws IOException {
        CaptureDisplayDetails details = new CaptureDisplayDetails();

        DataInputStream data = new DataInputStream(in);
        int sizeRemaining = data.readInt();
        if (sizeRemaining >= packetSize) {
            details.fbp = data.readInt(); sizeRemaining -= 4;
            details.fbw = data.readInt(); sizeRemaining -= 4;
            details.zbp = data.readInt(); sizeRemaining -= 4;
            details.zbw = data.readInt(); sizeRemaining -= 4;
            details.psm = data.readInt(); sizeRemaining -= 4;

            details.topaddrFb = data.readInt(); sizeRemaining -= 4;
            details.bufferwidthFb = data.readInt(); sizeRemaining -= 4;
            details.pixelformatFb = data.readInt(); sizeRemaining -= 4;
            details.sync = data.readInt(); sizeRemaining -= 4;

            data.skipBytes(sizeRemaining);

            if (captureRenderTargets) {
                // read draw, depth and display buffers
                CaptureHeader header = CaptureHeader.read(in);
                int packetType = header.getPacketType();
                if (packetType != CaptureHeader.PACKET_TYPE_RAM) {
                    throw new IOException("Expected CaptureRAM(" + CaptureHeader.PACKET_TYPE_RAM + ") packet, found " + packetType);
                }
                details.drawBuffer = CaptureRAM.read(in);

                header = CaptureHeader.read(in);
                packetType = header.getPacketType();
                if (packetType != CaptureHeader.PACKET_TYPE_RAM) {
                    throw new IOException("Expected CaptureRAM(" + CaptureHeader.PACKET_TYPE_RAM + ") packet, found " + packetType);
                }
                details.depthBuffer = CaptureRAM.read(in);

                header = CaptureHeader.read(in);
                packetType = header.getPacketType();
                if (packetType != CaptureHeader.PACKET_TYPE_RAM) {
                    throw new IOException("Expected CaptureRAM(" + CaptureHeader.PACKET_TYPE_RAM + ") packet, found " + packetType);
                }
                details.displayBuffer = CaptureRAM.read(in);
            }
        } else {
            throw new IOException("Not enough bytes remaining in stream");
        }

        return details;
    }

    public void commit() {
        pspdisplay display = pspdisplay.getInstance();
        //VideoEngine ge = VideoEngine.getInstance();

        // This is almost side effect free, but replay is going to trash the emulator state anyway
        display.sceDisplaySetFrameBuf(topaddrFb, bufferwidthFb, pixelformatFb, sync);
        display.hleDisplaySetGeBuf(null, fbp, fbw, psm, false);

        if (captureRenderTargets) {
            drawBuffer.commit();
            depthBuffer.commit();
            displayBuffer.commit();
        }
    }
}
