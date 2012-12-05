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

import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointer;

public class SceMpegRingbuffer extends pspAbstractMemoryMappedStructure {
	private static final int ringbufferPacketSize = 2048;
    // PSP info
    private int packets;
    private int packetsRead;
    private int packetsWritten;
    private int packetsInRingbuffer;
    private int packetSize; // 2048
    private int data; // address, ring buffer
    private int callbackAddr; // see sceMpegRingbufferPut
    private int callbackArgs;
    private int dataUpperBound;
    private int semaID; // unused?
    private int mpeg; // pointer to mpeg struct, fixed up in sceMpegCreate

    public SceMpegRingbuffer(int packets, int data, int size, int callbackAddr, int callbackArgs) {
        this.packets = packets;
        this.packetsRead = 0;
        this.packetsWritten = 0;
        this.packetsInRingbuffer = 0;
        this.packetSize = ringbufferPacketSize;
        this.data = data;
        this.callbackAddr = callbackAddr;
        this.callbackArgs = callbackArgs;
        this.dataUpperBound = data + packets * ringbufferPacketSize;
        this.semaID = -1;
        this.mpeg = 0;

        if (dataUpperBound > data + size) {
            dataUpperBound = data + size;
            Modules.log.warn("SceMpegRingbuffer clamping dataUpperBound to " + dataUpperBound);
        }
    }

    private SceMpegRingbuffer() {
    }

    public static SceMpegRingbuffer fromMem(TPointer address) {
        SceMpegRingbuffer ringbuffer = new SceMpegRingbuffer();
        ringbuffer.read(address);

        return ringbuffer;
    }

    public void reset() {
    	packetsRead = 0;
    	packetsWritten = 0;
    	packetsInRingbuffer = 0;
    }

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
        mpeg                = read32();
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
        write32(mpeg);
	}

	public int getFreePackets() {
		return packets - packetsInRingbuffer;
	}

	public void addPackets(int packetsAdded) {
		packetsRead += packetsAdded;
		packetsWritten += packetsAdded;
		packetsInRingbuffer += packetsAdded;
	}

	public void consumeAllPackets() {
		packetsInRingbuffer = 0;
	}

	public int getPacketsInRingbuffer() {
		return packetsInRingbuffer;
	}

    public boolean isEmpty() {
    	return getPacketsInRingbuffer() == 0;
    }

	public void consumePackets(int consumedPackets) {
		if (consumedPackets > 0) {
			packetsInRingbuffer -= consumedPackets;
			if (packetsInRingbuffer < 0) {
				packetsInRingbuffer = 0;
			}
		}
	}

	public int getReadPackets() {
		return packetsRead;
	}

	public void setReadPackets(int packetsRead) {
		this.packetsRead = packetsRead;
	}

	public int getProcessedPackets() {
		return getReadPackets() - getPacketsInRingbuffer();
	}

	public int getTotalPackets() {
		return packets;
	}

	public int getPacketSize() {
		return packetSize;
	}

	public int getBaseDataAddr() {
		return data;
	}

	public int getTmpAddress(int length) {
		return dataUpperBound - length;
	}

	public void setMpeg(int mpeg) {
		this.mpeg = mpeg;
	}

	public int getUpperDataAddr() {
		return dataUpperBound;
	}

	public int getCallbackAddr() {
		return callbackAddr;
	}

	public int getCallbackArgs() {
		return callbackArgs;
	}

	@Override
	public int sizeof() {
		return 44;
	}

	@Override
	public String toString() {
		return String.format("SceMpegRingbuffer(packets=%d, packetsRead=%d, packetsWritten=%d, packetsFree=%d, packetSize=%d)", packets, packetsRead, packetsWritten, getFreePackets(), packetSize);
	}
}
