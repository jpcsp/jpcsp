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

import jpcsp.HLE.kernel.managers.SceUidManager;
import jpcsp.HLE.Modules;
import jpcsp.HLE.pspSysMem;
import jpcsp.Memory;
import jpcsp.util.Utilities;

public class SceKernelMppInfo {

    // PSP info
    public static final int size = 56;
    public String name;
    public int attr;
    public int bufSize;
    public int freeSize;
    public int numSendWaitThreads;
    public int numReceiveWaitThreads;

    private final int sysMemUID;
    // Internal info
    public final int uid;
    public final int partitionid;
    public final int address;
    private int head; // relative to address
    private int tail; // relative to address

    private SceKernelMppInfo(String name, int partitionid, int attr, int size) {
        this.name = name;
        this.attr = attr;

        this.bufSize = size;
        this.freeSize = size;
        this.numSendWaitThreads = 0;
        this.numReceiveWaitThreads = 0;

        int memType = pspSysMem.PSP_SMEM_Low;
        // TODO probably based on attr
        //if ((attr & MSGPIPE_ATTR_ADDR_HIGH) == MSGPIPE_ATTR_ADDR_HIGH)
        //    memType = pspSysMem.PSP_SMEM_High;

        int alignedSize = (size + 0xFF) & ~0xFF; // 256 byte align (or is this stage done by pspsysmem? aren't we using 64-bytes in pspsysmem?)
        address = pspSysMem.getInstance().malloc(partitionid, memType, alignedSize, 0);
        if (address == 0)
            throw new RuntimeException("SceKernelFplInfo: not enough free mem");

        this.sysMemUID = pspSysMem.getInstance().addSysMemInfo(partitionid, "ThreadMan-MsgPipe", memType, alignedSize, address);
        this.uid = SceUidManager.getNewUid("ThreadMan-MsgPipe");
        this.partitionid = partitionid;
        this.head = 0;
        this.tail = 0;
    }

    public static SceKernelMppInfo tryCreateMpp(String name, int partitionid, int attr, int size) {
        SceKernelMppInfo info = null;
        int alignedSize = (size + 0xFF) & ~0xFF; // 256 byte align (or is this stage done by pspsysmem? aren't we using 64-bytes in pspsysmem?)
        int maxFreeSize = pspSysMem.getInstance().maxFreeMemSize();

        if (size <= 0) {
            Modules.log.warn("tryCreateMpp invalid size " + size);
        } else if (alignedSize > maxFreeSize) {
            Modules.log.warn("tryCreateMpp not enough free mem (want=" + alignedSize + ",free=" + maxFreeSize + ",diff=" + (alignedSize - maxFreeSize) + ")");
        } else {
            info = new SceKernelMppInfo(name, partitionid, attr, size);
        }

        return info;
    }

    public void read(Memory mem, int address) {
        int size                = mem.read32(address);
        name                    = Utilities.readStringNZ(mem, address + 4, 32);
        attr                    = mem.read32(address + 36);
        bufSize                 = mem.read32(address + 40);
        freeSize                = mem.read32(address + 44);
        numSendWaitThreads      = mem.read32(address + 48);
        numReceiveWaitThreads   = mem.read32(address + 52);
    }

    public void write(Memory mem, int address) {
        mem.write32(address, size);
        Utilities.writeStringNZ(mem, address + 4, 32, name);
        mem.write32(address + 36, attr);
        mem.write32(address + 40, bufSize);
        mem.write32(address + 44, freeSize);
        mem.write32(address + 48, numSendWaitThreads);
        mem.write32(address + 52, numReceiveWaitThreads);
    }

    public int availableReadSize() {
        return bufSize - freeSize;
    }

    public int availableWriteSize() {
        return freeSize;
    }

    public void deleteSysMemInfo() {
        pspSysMem.getInstance().free(sysMemUID, address);
    }

    // this will clobber itself if used carelessly but won't overflow outside of its allocated memory
    public void append(Memory mem, int src, int size) {
        int copySize;

        freeSize -= size;

        while (size > 0) {
            copySize = Math.min(bufSize - tail, size);
            mem.memcpy(address + tail, src, copySize);
            src += copySize;
            size -= copySize;
            tail = (tail + copySize) % bufSize;
        }
    }

    public void consume(Memory mem, int dst, int size) {
        int copySize;

        freeSize += size;

        while (size > 0) {
            copySize = Math.min(bufSize - head, size);
            mem.memcpy(dst, address + head, copySize);
            dst += copySize;
            size -= copySize;
            head = (tail + copySize) % bufSize;
        }
    }
}
