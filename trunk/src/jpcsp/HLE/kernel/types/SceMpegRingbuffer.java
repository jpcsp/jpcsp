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

import jpcsp.Memory;
import jpcsp.HLE.Modules;

// some field info from noxa/pspplayer
public class SceMpegRingbuffer extends pspAbstractMemoryMappedStructure {
    // PSP info
    public int packets;
    public int packetsRead;
    public int packetsWritten;
    public int packetsFree; // pspsdk: unk2, noxa: iUnk0
    public int packetSize; // 2048
    public int data; // address, ring buffer
    public int callback_addr; // see sceMpegRingbufferPut
    public int callback_args;
    public int dataUpperBound;
    public int semaID; // unused?
    public int mpeg; // pointer to mpeg struct, fixed up in sceMpegCreate

    public SceMpegRingbuffer(int packets, int data, int size, int callback_addr, int callback_args) {
        this.packets = packets;
        this.packetsRead = 0;
        this.packetsWritten = 0;
        this.packetsFree = 0; // set later
        this.packetSize = 2048;
        this.data = data;
        this.callback_addr = callback_addr;
        this.callback_args = callback_args;
        this.dataUpperBound = data + packets * 2048;
        this.semaID = -1;
        this.mpeg = 0;

        if (dataUpperBound > data + size) {
            dataUpperBound = data + size;
            Modules.log.warn("SceMpegRingbuffer clamping dataUpperBound to " + dataUpperBound);
        }
    }

    private SceMpegRingbuffer() {
    }

    public static SceMpegRingbuffer fromMem(Memory mem, int address) {
        SceMpegRingbuffer ringbuffer = new SceMpegRingbuffer();
        ringbuffer.read(mem, address);
        return ringbuffer;
    }

    public void reset() {
    	packetsRead = 0;
    	packetsWritten = 0;
    	packetsFree = packets;
    }

    public boolean isEmpty() {
    	return packetsFree == packets;
    }

	@Override
	protected void read() {
        packets         = read32();
        packetsRead     = read32();
        packetsWritten  = read32();
        packetsFree     = read32();
        packetSize      = read32();
        data            = read32();
        callback_addr   = read32();
        callback_args   = read32();
        dataUpperBound  = read32();
        semaID          = read32();
        mpeg            = read32();
	}

	@Override
	protected void write() {
        write32(packets);
        write32(packetsRead);
        write32(packetsWritten);
        write32(packetsFree);
        write32(packetSize);
        write32(data);
        write32(callback_addr);
        write32(callback_args);
        write32(dataUpperBound);
        write32(semaID);
        write32(mpeg);
	}

	@Override
	public int sizeof() {
		return 44;
	}

	@Override
	public String toString() {
		return String.format("SceMpegRingbuffer(packets=%d, packetsRead=%d, packetsWritten=%d, packetsFree=%d, packetSize=%d)", packets, packetsRead, packetsWritten, packetsFree, packetSize);
	}
}
