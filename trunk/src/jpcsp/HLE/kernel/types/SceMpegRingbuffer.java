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
public class SceMpegRingbuffer {

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

    public void read(Memory mem, int address) {
        packets         = mem.read32(address);
        packetsRead     = mem.read32(address + 4);
        packetsWritten  = mem.read32(address + 8);
        packetsFree     = mem.read32(address + 12);
        packetSize      = mem.read32(address + 16);
        data            = mem.read32(address + 20);
        callback_addr   = mem.read32(address + 24);
        callback_args   = mem.read32(address + 28);
        dataUpperBound  = mem.read32(address + 32);
        semaID          = mem.read32(address + 36);
        mpeg            = mem.read32(address + 40);
    }

    public void write(Memory mem, int address) {
        mem.write32(address, packets);
        mem.write32(address + 4, packetsRead);
        mem.write32(address + 8, packetsWritten);
        mem.write32(address + 12, packetsFree);
        mem.write32(address + 16, packetSize);
        mem.write32(address + 20, data);
        mem.write32(address + 24, callback_addr);
        mem.write32(address + 28, callback_args);
        mem.write32(address + 32, dataUpperBound);
        mem.write32(address + 36, semaID);
        mem.write32(address + 40, mpeg);
    }
}
