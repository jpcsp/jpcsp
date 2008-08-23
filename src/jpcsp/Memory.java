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

import static jpcsp.MemoryMap.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Memory {
    //21/07/08 memory using singleton pattern
    private static Memory instance = null;
    public byte[] mainmemory;
    public byte[] scratchpad;
    public byte[] videoram;
    public ByteBuffer mainmemorybuf;
    public ByteBuffer scratchpadbuf;
    public ByteBuffer videorambuf;
    private ByteBuffer range;
    private int index;

    public static Memory get_instance() {
        if (instance == null) {
            instance = new Memory();
        }
        return instance;
    }

    public void NullMemory() {
        instance = null;
    }

    private Memory() { //no one can instantiate it, except itself
        mainmemory = new byte[0x01FFFFFF]; //32mb main ram
        scratchpad = new byte[0x00003FFF]; //16kb scratchpad
        videoram = new byte[0x001FFFFF]; // 2mb videoram
        mainmemorybuf = ByteBuffer.wrap(mainmemory);
        scratchpadbuf = ByteBuffer.wrap(scratchpad);
        videorambuf = ByteBuffer.wrap(videoram);
        mainmemorybuf.order(ByteOrder.LITTLE_ENDIAN);
        scratchpadbuf.order(ByteOrder.LITTLE_ENDIAN);
        videorambuf.order(ByteOrder.LITTLE_ENDIAN);
    }

    private void setRange(int address) throws Exception {

        // K0,K2/KS,K3 segment ?
        if ((address & 0x80000000) != 0) {

            // K2/KS,K3 segment ?
            if ((address & 0x40000000) != 0) {
                throw new Exception("Invalid memory address : " + Integer.toHexString(address) + " PC=" + Integer.toHexString(Emulator.getProcessor().pc));
            }
            // bits 31, 30 and 29 set to 0
            address &= 0x1FFFFFFF;
        } else {
            // bits 31 and 30 set to 0
            address &= 0x3FFFFFFF;
        }

        if ((address >= START_RAM) && (address <= END_RAM)) {
            index = address - START_RAM;
            range = mainmemorybuf;
            return;
        }

        if ((address >= START_VRAM) && (address <= END_VRAM)) {
            index = address - START_VRAM;
            range = videorambuf;
            return;
        }

        if ((address >= START_SCRATCHPAD) && (address <= END_SCRATCHPAD)) {
            index = address - START_SCRATCHPAD;
            range = scratchpadbuf;
            return;
        }

        throw new Exception("Invalid memory address : " + Integer.toHexString(address) + " PC=" + Integer.toHexString(Emulator.getProcessor().pc));
    }

    public int read8(int address) {
        try {
            setRange(address);
            return range.get(index);
        } catch (Exception e) {
            System.out.println("read8 - " + e.getMessage());
        }
        return 0;
    }

    public int read16(int address) {

        try {
            setRange(address);
            return range.getShort(index);
        } catch (Exception e) {
            System.out.println("read16 - " + e.getMessage());
        }

        return 0;
    }

    public int read32(int address) {

        try {
            setRange(address);
            return range.getInt(index);
        } catch (Exception e) {
            System.out.println("read32 - " + e.getMessage());
        }

        return 0;
    }

    public void write8(int address, byte data) {
        try {
            setRange(address);
            range.put(index, data);
        } catch (Exception e) {
            System.out.println("write8 - " + Integer.toHexString(data) + " - " + e.getMessage());
        }
    }

    public void write16(int address, short data) {
        try {
            setRange(address);
            range.putShort(index, data);
        } catch (Exception e) {
            System.out.println("write16 - " + Integer.toHexString(data) + " - " + e.getMessage());
        }
    }

    public void write32(int address, int data) {
        try {
            setRange(address);
            range.putInt(index, data);
        } catch (Exception e) {
            System.out.println("write32 - " + Integer.toHexString(data) + " - " + e.getMessage());
        }
    }
}
