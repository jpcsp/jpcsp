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
package jpcsp;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

import org.apache.log4j.Logger;

import jpcsp.HLE.pspdisplay;
import static jpcsp.MemoryMap.*;

public class Memory {
    private static final int PAGE_COUNT        = 0x00100000;
    private static final int PAGE_MASK         = 0x00000FFF;
    private static final int PAGE_SHIFT        = 12;

    private static final int INDEX_SCRATCHPAD  = 0;
    private static final int INDEX_VRAM        = SIZE_SCRATCHPAD >>> PAGE_SHIFT;
    private static final int INDEX_RAM         = INDEX_VRAM +
                                                 (SIZE_VRAM >>> PAGE_SHIFT);

    private static final int SIZE_ALLMEM       = SIZE_SCRATCHPAD +
                                                 SIZE_VRAM + SIZE_RAM;

    private static Memory instance = null;
    public static Logger log = Logger.getLogger("memory");

    private byte[]     all; // all psp memory is held in here
    private static int[]      map; // hold map of memory
    private ByteBuffer buf; // for easier memory reads/writes

    public ByteBuffer scratchpad;
    public ByteBuffer videoram;
    public ByteBuffer mainmemory;

    public static Memory getInstance() {
        if (instance == null)
            instance = new Memory();
        return instance;
    }

    public void Initialise() {
        Arrays.fill(all, (byte)0);
    }

    private Memory() {
        all = new byte[SIZE_ALLMEM];
        map = new int[PAGE_COUNT];
        buf = ByteBuffer.wrap(all);
        buf.order(ByteOrder.LITTLE_ENDIAN);

        scratchpad = ByteBuffer.wrap(
            all,
            0,
            SIZE_SCRATCHPAD).slice();
        scratchpad.order(ByteOrder.LITTLE_ENDIAN);

        videoram = ByteBuffer.wrap(
            all,
            SIZE_SCRATCHPAD,
            SIZE_VRAM).slice();
        videoram.order(ByteOrder.LITTLE_ENDIAN);

        mainmemory = ByteBuffer.wrap(
            all,
            SIZE_SCRATCHPAD + SIZE_VRAM,
            SIZE_RAM).slice();
        mainmemory.order(ByteOrder.LITTLE_ENDIAN);

        buildMap();
    }

    private void buildMap() {
        int i;
        int page;

        Arrays.fill(map, -1);

        page = START_SCRATCHPAD >>> PAGE_SHIFT;
        for (i = 0; i < (SIZE_SCRATCHPAD >>> PAGE_SHIFT); ++i) {
            map[0x00000 + page + i] = (INDEX_SCRATCHPAD + i) << PAGE_SHIFT;
            map[0x40000 + page + i] = (INDEX_SCRATCHPAD + i) << PAGE_SHIFT;
            map[0x80000 + page + i] = (INDEX_SCRATCHPAD + i) << PAGE_SHIFT;
        }

        page = START_VRAM >>> PAGE_SHIFT;
        for (i = 0; i < (SIZE_VRAM >>> PAGE_SHIFT); ++i) {
            map[0x00000 + page + i] = (INDEX_VRAM + i) << PAGE_SHIFT;
            map[0x40000 + page + i] = (INDEX_VRAM + i) << PAGE_SHIFT;
            map[0x80000 + page + i] = (INDEX_VRAM + i) << PAGE_SHIFT;
        }

        page = START_RAM >>> PAGE_SHIFT;
        for (i = 0; i < (SIZE_RAM >>> PAGE_SHIFT); ++i) {
            map[0x00000 + page + i] = (INDEX_RAM + i) << PAGE_SHIFT;
            map[0x40000 + page + i] = (INDEX_RAM + i) << PAGE_SHIFT;
            map[0x80000 + page + i] = (INDEX_RAM + i) << PAGE_SHIFT;
        }
    }


    public static int indexFromAddr(int address) throws Exception {
        int index = map[address >>> PAGE_SHIFT];
        if (index == -1) {
            throw new Exception(
                "Invalid memory address : " +
                Integer.toHexString(address) +
                " PC=" +
                Integer.toHexString(Emulator.getProcessor().cpu.pc));
        }
        return index;
    }

    public boolean isAddressGood(int address)
    {
        int index = map[address >>> PAGE_SHIFT];
        return (index != -1);
    }

    public int read8(int address) {
        try {
            int page = indexFromAddr(address);
            return (int)buf.get(page + (address & PAGE_MASK)) & 0xFF;
        } catch (Exception e) {
            Memory.log.error("read8 - " + e.getMessage());
            Emulator.PauseEmuWithStatus(Emulator.EMU_STATUS_MEM_READ);
            return 0;
        }
    }

    public int read16(int address) {
        try {
            int page = indexFromAddr(address);
            return (int)buf.getShort(page + (address & PAGE_MASK)) & 0xFFFF;
        } catch (Exception e) {
            Memory.log.error("read16 - " + e.getMessage());
            Emulator.PauseEmuWithStatus(Emulator.EMU_STATUS_MEM_READ);
            return 0;
        }
    }

    public int read32(int address) {
        try {
            int page = indexFromAddr(address);
            return buf.getInt(page + (address & PAGE_MASK));
        } catch (Exception e) {
            //
            // Ugly hack for programs using pspsdk :-(
            //
            // The function pspSdkInstallNoPlainModuleCheckPatch()
            // is trying to patch 2 psp modules and is expecting to have
            // the module stub implemented as a Jump instruction,
            // something like:
            //          [08XXXXXX]: j YYYYYYYY        // YYYYYYYY = XXXXXX << 2
            //          [00000000]: nop
            //
            // Jpcsp is however based on the following code sequence, e.g.:
            //          [03E00008]: jr $ra
            //          [00081B4C]: syscall 0x0206D
            //
            // The function pspSdkInstallNoPlainModuleCheckPatch()
            // is retrieving the address of the Jump instruction and reading
            // from it in kernel mode.
            // On jpcsp, it is thus trying to read at the following address
            //          0x8f800020 = (0x03E00008 << 2) || 0x80000000
            // up to    0x8f8001ac
            //
            // The hack here is to allow these memory reads and returns 0.
            //
            // Here is the C code from pspsdk:
            //
            //          int pspSdkInstallNoPlainModuleCheckPatch(void)
            //          {
            //              u32 *addr;
            //              int i;
            //
            //              addr = (u32*) (0x80000000 | ((sceKernelProbeExecutableObject & 0x03FFFFFF) << 2));
            //              //printf("sceKernelProbeExecutableObject %p\n", addr);
            //              for(i = 0; i < 100; i++)
            //              {
            //                  if((addr[i] & 0xFFE0FFFF) == LOAD_EXEC_PLAIN_CHECK)
            //                  {
            //                      //printf("Found instruction %p\n", &addr[i]);
            //                      addr[i] = (LOAD_EXEC_PLAIN_PATCH | (addr[i] & ~0xFFE0FFFF));
            //                  }
            //              }
            //
            //              addr = (u32*) (0x80000000 | ((sceKernelCheckPspConfig & 0x03FFFFFF) << 2));
            //              //printf("sceCheckPspConfig %p\n", addr);
            //              for(i = 0; i < 100; i++)
            //              {
            //                  if((addr[i] & 0xFFE0FFFF) == LOAD_EXEC_PLAIN_CHECK)
            //                  {
            //                      //printf("Found instruction %p\n", &addr[i]);
            //                      addr[i] = (LOAD_EXEC_PLAIN_PATCH | (addr[i] & ~0xFFE0FFFF));
            //                  }
            //              }
            //
            //              sceKernelDcacheWritebackAll();
            //
            //              return 0;
            //          }
            //
            if (address >= 0x8f800020 && address <= 0x8f8001ac) {
                Memory.log.debug("read32 - ignoring pspSdkInstallNoPlainModuleCheckPatch");
                return 0;
            }

            Memory.log.error("read32 - " + e.getMessage());
            Emulator.PauseEmuWithStatus(Emulator.EMU_STATUS_MEM_READ);
            return 0;
        }
    }

    public long read64(int address) {
        try {
            int page = indexFromAddr(address);
            return buf.getLong(page + (address & PAGE_MASK));
        } catch (Exception e) {
            Memory.log.error("read64 - " + e.getMessage());
            Emulator.PauseEmuWithStatus(Emulator.EMU_STATUS_MEM_READ);
            return 0;
        }
    }

    /*
    public float[] read128f(int address) {
        try {
            int page = indexFromAddr(address);
            return buf.getInt(page + (address & PAGE_MASK));
        } catch (Exception e) {
            Memory.log.error("read32 - " + e.getMessage());
            Emulator.PauseEmuWithStatus(Emulator.EMU_STATUS_MEM_READ);
            return 0;
        }
    }
    */

    public int read32(int page, int address) {
        try {
            return buf.getInt(page + (address & PAGE_MASK));
        } catch (Exception e) {
            Memory.log.error("read32 - " + e.getMessage());
            Emulator.PauseEmuWithStatus(Emulator.EMU_STATUS_MEM_READ);
            return 0;
        }
    }

    public void write8(int address, byte data) {
        try {
            int page = indexFromAddr(address);
            buf.put(page + (address & PAGE_MASK), data);
            pspdisplay.getInstance().write8(address, data);
        } catch (Exception e) {
            Memory.log.error("write8 - " + e.getMessage());
            Emulator.PauseEmuWithStatus(Emulator.EMU_STATUS_MEM_WRITE);
        }
    }

    public void write8(int page, int address, byte data) {
        try {
            buf.put(page + (address & PAGE_MASK), data);
            pspdisplay.getInstance().write8(address, data);
        } catch (Exception e) {
            Memory.log.error("write8 - " + e.getMessage());
            Emulator.PauseEmuWithStatus(Emulator.EMU_STATUS_MEM_WRITE);
        }
    }

    public void write16(int address, short data) {
        try {
            int page = indexFromAddr(address);
            buf.putShort(page + (address & PAGE_MASK), data);
            pspdisplay.getInstance().write16(address, data);
        } catch (Exception e) {
            Memory.log.error("write16 - " + e.getMessage());
            Emulator.PauseEmuWithStatus(Emulator.EMU_STATUS_MEM_WRITE);
        }
    }

    public void write32(int address, int data) {
        try {
            int page = indexFromAddr(address);
            buf.putInt(page + (address & PAGE_MASK), data);
            pspdisplay.getInstance().write32(address, data);
        } catch (Exception e) {
            Memory.log.error("write32 - " + e.getMessage());
            Emulator.PauseEmuWithStatus(Emulator.EMU_STATUS_MEM_WRITE);
        }
    }

    public void write32(int page, int address, int data) {
        try {
            buf.putInt(page + (address & PAGE_MASK), data);
            pspdisplay.getInstance().write32(address, data);
        } catch (Exception e) {
            Memory.log.error("write32 - " + e.getMessage());
            Emulator.PauseEmuWithStatus(Emulator.EMU_STATUS_MEM_WRITE);
        }
    }

    public void write64(int address, long data) {
        try {
            int page = indexFromAddr(address);
            buf.putLong(page + (address & PAGE_MASK), data);
            //pspdisplay.getInstance().write64(address, data);
        } catch (Exception e) {
            Memory.log.error("write64 - " + e.getMessage());
            Emulator.PauseEmuWithStatus(Emulator.EMU_STATUS_MEM_WRITE);
        }
    }

    public void load(ByteBuffer buffer) {

    }

    public void save(ByteBuffer buffer) {

    }
}
