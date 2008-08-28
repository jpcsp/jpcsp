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

    private byte[]     all; // all psp memory is held in here
    private int[]      map; // hold map of memory
    private ByteBuffer buf; // for easier memory reads/writes

    public ByteBuffer scratchpad;
    public ByteBuffer videoram;
    public ByteBuffer mainmemory;

    public static Memory get_instance() {
        if (instance == null)
            instance = new Memory();
        return instance;
    }

    public void NullMemory() {
        instance = null;
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

        for (i = 0; i < PAGE_COUNT; ++i)
            map[i] = -1;

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


    private int indexFromAddr(int address) throws Exception {
        int index = map[address >>> PAGE_SHIFT];
        if (index == -1) {
            throw new Exception(
                "Invalid memory address : " +
                Integer.toHexString(address) +
                " PC=" +
                Integer.toHexString(Emulator.getProcessor().pc));
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
            System.out.println("read8 - " + e.getMessage());
            Emulator.PauseEmu();
            return 0;
        }
    }

    public int read16(int address) {
        try {
            int page = indexFromAddr(address);
            return (int)buf.getShort(page + (address & PAGE_MASK)) & 0xFFFF;
        } catch (Exception e) {
            System.out.println("read16 - " + e.getMessage());
            Emulator.PauseEmu();
            return 0;
        }
    }

    public int read32(int address) {
        try {
            int page = indexFromAddr(address);
            return buf.getInt(page + (address & PAGE_MASK));
        } catch (Exception e) {
            System.out.println("read32 - " + e.getMessage());
            Emulator.PauseEmu();
            return 0;
        }
    }

    public void write8(int address, byte data) {
        try {
            int page = indexFromAddr(address);
            buf.put(page + (address & PAGE_MASK), data);
            pspdisplay.get_instance().write8(address, data);
        } catch (Exception e) {
            System.out.println("write8 - " + e.getMessage());
            Emulator.PauseEmu();
        }
    }

    public void write16(int address, short data) {
        try {
            int page = indexFromAddr(address);
            buf.putShort(page + (address & PAGE_MASK), data);
            pspdisplay.get_instance().write16(address, data);
        } catch (Exception e) {
            System.out.println("write16 - " + e.getMessage());
            Emulator.PauseEmu();
        }
    }

    public void write32(int address, int data) {
        try {
            int page = indexFromAddr(address);
            buf.putInt(page + (address & PAGE_MASK), data);
            pspdisplay.get_instance().write32(address, data);
        } catch (Exception e) {
            System.out.println("write32 - " + e.getMessage());
            Emulator.PauseEmu();
        }
    }

    public void write64(int address, long data) {
        try {
            int page = indexFromAddr(address);
            buf.putLong(page + (address & PAGE_MASK), data);
            //pspdisplay.get_instance().write64(address, data);
        } catch (Exception e) {
            System.out.println("write64 - " + e.getMessage());
            Emulator.PauseEmu();
        }
    }
}
