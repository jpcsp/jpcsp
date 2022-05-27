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
package jpcsp.graphics.capture;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.apache.log4j.Logger;

import jpcsp.Memory;
import jpcsp.HLE.TPointer;

/** captures a piece of RAM */
public class CaptureRAM {
	public static Logger log = CaptureManager.log;
    private final int address;
    private final int length;
    private final byte[] buffer;

    public CaptureRAM(Memory mem, int address, int length) throws IOException {
        this.address = address;
        this.length = length;
        TPointer pointer = new TPointer(mem, address);
        buffer = pointer.getArray8(length);
    }

    public CaptureRAM(int address, int length, byte[] buffer) throws IOException {
        this.address = address;
        this.length = length;
        this.buffer = buffer;
    }

    public void write(DataOutputStream out) throws IOException {
    	out.writeInt(CaptureManager.PACKET_TYPE_RAM);
    	out.writeInt(address);
    	out.writeInt(length);
    	out.write(buffer, 0, length);

        if (log.isDebugEnabled()) {
        	log.debug(String.format("Saved memory 0x%08X - 0x%08X (len 0x%X)", address, address + length, length));
        }
    }

    public static CaptureRAM read(DataInputStream in) throws IOException {
        int address = in.readInt();
        int length = in.readInt();

    	byte[] buffer = new byte[length];
    	in.readFully(buffer);

        if (log.isDebugEnabled()) {
        	log.debug(String.format("Loaded memory 0x%08X - 0x%08X (len 0x%X)", address, address + length, length));
        }

        return new CaptureRAM(address, length, buffer);
    }

    public void commit(Memory mem) {
        TPointer pointer = new TPointer(mem, address);
        pointer.setArray(buffer, length);
    }
}
