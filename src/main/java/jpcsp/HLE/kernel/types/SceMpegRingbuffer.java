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
package jpcsp.HLE.kernel.types;

import static jpcsp.HLE.modules.sceMpeg.isMpeg260;

import jpcsp.HLE.TPointer32;

public class SceMpegRingbuffer extends pspAbstractMemoryMappedStructure {
	public static final int ringbufferPacketSize = 2048;
    // PSP info
    public int packets;
    public int packetsRead;
    public int packetsWritten;
    public int packetsInRingbuffer;
    public int packetSize; // 2048
    public int data; // address, ring buffer
    public int callbackAddr; // see sceMpegRingbufferPut
    public int callbackArgs;
    public int dataUpperBound;
    public int semaID; // unused?
    public TPointer32 mpeg; // pointer to mpeg struct, set in sceMpegCreate
    public int gp; // "gp" register for the callback, only after PSP v2.60

	@Override
	protected void read() {
        packets             = read32();
        packetsRead         = read32();
        packetsWritten      = read32();
        packetsInRingbuffer = read32();
        packetSize          = read32();
        data                = read32();
        callbackAddr        = read32();
        callbackArgs        = read32();
        dataUpperBound      = read32();
        semaID              = read32();
        mpeg                = readPointer32();
        if (!isMpeg260()) {
        	gp              = read32();
        }
	}

	@Override
	protected void write() {
        write32(packets);
        write32(packetsRead);
        write32(packetsWritten);
        write32(packetsInRingbuffer);
        write32(packetSize);
        write32(data);
        write32(callbackAddr);
        write32(callbackArgs);
        write32(dataUpperBound);
        write32(semaID);
        writePointer32(mpeg);
        if (!isMpeg260()) {
        	write32(gp);
        }
	}

	@Override
	public int sizeof() {
		if (isMpeg260()) {
			// No "gp" field up to PSP v2.60
			return 44;
		}
		return 48;
	}
}
