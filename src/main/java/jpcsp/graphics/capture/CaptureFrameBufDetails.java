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

import jpcsp.HLE.Modules;
import jpcsp.HLE.modules.sceDisplay;

/** captures sceDisplaySetFrameBuf */
public class CaptureFrameBufDetails {
    private int topaddrFb;
    private int bufferwidthFb;
    private int pixelformatFb;
    private int sync;

    public CaptureFrameBufDetails() {
        sceDisplay display = Modules.sceDisplayModule;

        topaddrFb = display.getTopAddrFb();
        bufferwidthFb = display.getBufferWidthFb();
        pixelformatFb = display.getPixelFormatFb();
        sync = display.getSync();
    }

    public void write(DataOutputStream out) throws IOException {
    	out.writeInt(CaptureManager.PACKET_TYPE_GE_DETAILS);
    	out.writeInt(topaddrFb);
    	out.writeInt(bufferwidthFb);
    	out.writeInt(pixelformatFb);
    	out.writeInt(sync);
    }

    public static CaptureFrameBufDetails read(DataInputStream in) throws IOException {
        CaptureFrameBufDetails details = new CaptureFrameBufDetails();

        details.topaddrFb = in.readInt();
        details.bufferwidthFb = in.readInt();
        details.pixelformatFb = in.readInt();
        details.sync = in.readInt();

        return details;
    }

    public void commit() {
        sceDisplay display = Modules.sceDisplayModule;

        // This is almost side effect free, but replay is going to trash the emulator state anyway
        display.hleDisplaySetFrameBuf(topaddrFb, bufferwidthFb, pixelformatFb, sync);
    }
}
