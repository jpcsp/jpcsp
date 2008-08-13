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

public class Memory {
    //21/07/08 memory using singleton pattern
    private static Memory instance = null;
    public byte[] mainmemory;
    public byte[] scratchpad;
    public byte[] videoram;

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
    }

    private byte[] MemoryRange(int address) throws Exception {
        address = address & 0x3FFFFFFF;

        if ((address >= START_RAM) && (address <= END_RAM)) {
            return mainmemory;
        }

        if ((address >= START_VRAM) && (address <= END_VRAM)) {
            return videoram;
        }

        if ((address >= START_SCRATCHPAD) && (address <= END_SCRATCHPAD)) {
            return scratchpad;
        }

        throw new Exception("InvalidMemoryRange");
    }

    public int read8(int address) {

        try {
            byte[] memory = MemoryRange(address);

            int index = address & 0x01FFFFFF;

            return memory[index];
        } catch (Exception e) {
            System.out.println("read8 to unsupported emulate memory ! " + Integer.toHexString(address));
        }

        return 0;
    }

    public int read16(int address) {

        try {
            byte[] memory = MemoryRange(address);

            int index = address & 0x01FFFFFF;

            return (short) (((short) memory[index + 1] << 8)           |
                           (((short) memory[index + 0] << 0) & 0x00ff));
        } catch (Exception e) {
            System.out.println("read16 to unsupported emulate memory ! " + Integer.toHexString(address));
        }

        return 0;
    }

    public int read32(int address) {

        try {
            byte[] memory = MemoryRange(address);

            int index = address & 0x01FFFFFF;

            return (((int) memory[index + 3] << 24)               |
                   (((int) memory[index + 2] << 16) & 0x00ff0000) |
                   (((int) memory[index + 1] <<  8) & 0x0000ff00) |
                   (((int) memory[index + 0] <<  0) & 0x000000ff));
        } catch (Exception e) {
            System.out.println("read32 to unsupported emulate memory ! " + Integer.toHexString(address));
        }

        return 0;
    }

    public void write8(int address, byte data) {
        try {
            byte[] memory = MemoryRange(address);

            int index = address & 0x01FFFFFF;

            memory[index] = data;
        } catch (Exception e) {
            System.out.println("write8 to unsupported emulate memory ! " + Integer.toHexString(address));
        }
    }

    public void write16(int address, short data) {
        try {
            byte[] memory = MemoryRange(address);

            int index = address & 0x01FFFFFF;

            memory[index + 1] = (byte) ((data >> 8)      );
            memory[index + 0] = (byte) ((data >> 0) & 256);            
        } catch (Exception e) {
            System.out.println("unsupported write16 in addr= " + Integer.toHexString(address) + " data= " + data);
        }
    }

    public void write32(int address, int data) {
        try {
            byte[] memory = MemoryRange(address);

            int index = address & 0x01FFFFFF;

            memory[index + 3] = (byte) ((data >> 24)           );
            memory[index + 2] = (byte) ((data >> 16) & 0xFF0000);            
            memory[index + 1] = (byte) ((data >>  8) & 0x00FF00);
            memory[index + 0] = (byte) ((data >>  0) & 0x0000FF);            
        } catch (Exception e) {
            System.out.println("unsupported write32 in addr= " + Integer.toHexString(address) + " data= " + data);
        }
    }
}
