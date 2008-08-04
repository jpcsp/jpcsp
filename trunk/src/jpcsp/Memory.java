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

    public Memory() {
        mainmemory = new byte[0x01FFFFFF]; //32mb main ram
        scratchpad = new byte[0x00003FFF]; //16kb scratchpad
        videoram = new byte[0x001FFFFF]; // 2mb videoram
    }
    public int read8(int address)
    {
         if ((address >= START_RAM) && (address <= END_RAM)) {
           int i = address - 0x08000000;
           return mainmemory[i];
          }
        System.out.println("read8 to unsupported emulate memory ! " + address);
        return 0;
    }
    public int read16(int address)
    {
       if ((address >= START_RAM) && (address <= END_RAM)) {
           int i = address - 0x08000000;
               return (short)(((short)mainmemory[i+1] << 8) | (((short)mainmemory[i])&0x00ff));
          }
        System.out.println("read16 to unsupported emulate memory ! " + address);
        return 0;
    }
    public int read32(int address) { //for testing supports only RAM!
        if ((address >= START_RAM) && (address <= END_RAM)) {
           int i = address - 0x08000000;
           return (((int) mainmemory[i + 3] << 24) |
                (((int) mainmemory[i + 2] << 16) & 0x00ff0000) |
                (((int) mainmemory[i + 1] << 8) & 0x0000ff00) |
                (((int) mainmemory[i]) & 0x000000ff));
        }
        System.out.println("read32 to unsupported emulate memory ! " + address);
        return 0;
    }
    public void write8(int address , byte data)
    {
         if ((address >= START_RAM) && (address <= END_RAM)) {
           int i = address - 0x08000000;
           mainmemory[i] = data;
         }
         else
         {
             System.out.println("unsupported write8 in addr= " + address + " data= " + data);
         }

    }
    public void write16(int address , short data)
    {
         if ((address >= START_RAM) && (address <= END_RAM)) {
           int i = address - 0x08000000;
           mainmemory[i+1] = (byte)(data >> 8);
	   mainmemory[i] = (byte)(data & 0x00ff);
         }
         else
         {
             System.out.println("unsupported write16 in addr= " + address + " data= " + data);
         }
    }
    public void write32(int address, int data)
    {
         if ((address >= START_RAM) && (address <= END_RAM)) {
           int i = address - 0x08000000;
           mainmemory[i+3] = (byte)(data >> 24);
	   mainmemory[i+2] = (byte)((data & 0x00ff0000) >> 16);
	   mainmemory[i+1] = (byte)((data & 0x0000ff00) >> 8);
	   mainmemory[i] = (byte)(data & 0x000000ff);
         }
         else
         {
             System.out.println("unsupported write32 in addr= " + address + " data= " + data);
         }

    }
}
