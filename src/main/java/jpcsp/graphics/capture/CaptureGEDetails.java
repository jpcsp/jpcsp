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

import org.apache.log4j.Logger;

import jpcsp.HLE.Modules;
import jpcsp.HLE.modules.sceDisplay;

public class CaptureGEDetails {
	public static Logger log = CaptureManager.log;
    private int topAddrGe;
    private int bufferWidthGe;
    private int pixelFormatGe;
    private boolean endOfList;

	public CaptureGEDetails(boolean endOfList) {
        sceDisplay display = Modules.sceDisplayModule;

        topAddrGe = display.getTopAddrGe();
        bufferWidthGe = display.getBufferWidthGe();
        pixelFormatGe = display.getPixelFormatGe();
        this.endOfList = endOfList;
	}

	private CaptureGEDetails() {
	}

	public void write(DataOutputStream out) throws IOException {
		out.writeInt(CaptureManager.PACKET_TYPE_GE_DETAILS);
    	out.writeInt(topAddrGe);
    	out.writeInt(bufferWidthGe);
    	out.writeInt(pixelFormatGe);
    	out.writeBoolean(endOfList);
	}

	public static CaptureGEDetails read(DataInputStream in) throws IOException {
		CaptureGEDetails geDetails = new CaptureGEDetails();

		geDetails.topAddrGe = in.readInt();
		geDetails.bufferWidthGe = in.readInt();
		geDetails.pixelFormatGe = in.readInt();
		geDetails.endOfList = in.readBoolean();

        if (log.isDebugEnabled()) {
        	log.debug(String.format("CaptureGEDetails topAddrGe=0x%08X, bufferWidthGe=%d, pixelFormatGe=%d, endOfList=%b", geDetails.topAddrGe, geDetails.bufferWidthGe, geDetails.pixelFormatGe, geDetails.endOfList));
        }

        return geDetails;
	}

	public boolean isEndOfList() {
		return endOfList;
	}

	public void commit() {
		Modules.sceDisplayModule.hleDisplaySetGeBuf(topAddrGe, bufferWidthGe, pixelFormatGe, false, false);

		// Force the display of the GE to the screen,
		// i.e. set the FB to the same address as the GE
		if (endOfList) {
			Modules.sceDisplayModule.hleDisplaySetFrameBuf(topAddrGe, bufferWidthGe, pixelFormatGe, 0);
			Modules.sceDisplayModule.step(true);
		}
	}
}
