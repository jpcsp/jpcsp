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
import java.io.InputStream;
import java.io.OutputStream;

import jpcsp.Memory;
import jpcsp.HLE.kernel.types.PspGeList;
import jpcsp.graphics.GeCommands;
import jpcsp.graphics.VideoEngine;

/** captures a display list
 * - PspGeList details
 * - backing RAM containing the GE instructions */
public class CaptureList {

    private static final int packetSize = 16;
    private PspGeList list;
    private CaptureRAM listBuffer;

    public CaptureList(PspGeList list) throws Exception {
    	this.list = new PspGeList(list.id);
    	this.list.init(list.list_addr, list.getStallAddr(), list.cbid, list.arg_addr);

        if (list.getStallAddr() - list.list_addr == 0) {
        	VideoEngine.log.error("Capture: Command list is empty");
        }

        int listSize = 0;
        if (list.getStallAddr() == 0) {
            // Scan list for END command
        	Memory mem = Memory.getInstance();
        	for (int listPc = list.list_addr; mem.isAddressGood(listPc); listPc += 4) {
        		int instruction = mem.read32(listPc);
        		int command = VideoEngine.command(instruction);
        		if (command == GeCommands.END) {
        			listSize = listPc - list.list_addr + 4;
        			break;
        		} else if (command == GeCommands.JUMP) {
        			VideoEngine.log.error("Found a JUMP instruction while scanning the list. Aborting the scan.");
        			listSize = listPc - list.list_addr + 4;
        			break;
        		} else if (command == GeCommands.RET) {
        			VideoEngine.log.error("Found a RET instruction while scanning the list. Aborting the scan.");
        			listSize = listPc - list.list_addr + 4;
        			break;
        		} else if (command == GeCommands.CALL) {
        			VideoEngine.log.warn("Found a CALL instruction while scanning the list. Ignoring the called list.");
        		}
        	}
        } else {
        	listSize = list.getStallAddr() - list.list_addr;
        }

        listBuffer = new CaptureRAM(list.list_addr & Memory.addressMask, listSize);
    }

    public void write(OutputStream out) throws IOException {
        DataOutputStream data = new DataOutputStream(out);

        data.writeInt(packetSize);
        data.writeInt(list.list_addr);
        data.writeInt(list.getStallAddr());
        data.writeInt(list.cbid);
        data.writeInt(list.arg_addr);

        //VideoEngine.log.info("CaptureList write " + (5 * 4));

        CaptureHeader header = new CaptureHeader(CaptureHeader.PACKET_TYPE_RAM);
        header.write(out);
        listBuffer.write(out);
    }


    private CaptureList() {
    }

    public static CaptureList read(InputStream in) throws IOException {
        CaptureList list = new CaptureList();

        DataInputStream data = new DataInputStream(in);
        int sizeRemaining = data.readInt();
        if (sizeRemaining >= 16) {
            int list_addr = data.readInt(); sizeRemaining -= 4;
            int stall_addr = data.readInt(); sizeRemaining -= 4;
            int cbid = data.readInt(); sizeRemaining -= 4;
            int arg_addr = data.readInt(); sizeRemaining -= 4;
            data.skipBytes(sizeRemaining);

            list.list = new PspGeList(0);
            list.list.init(list_addr, stall_addr, cbid, arg_addr);

            CaptureHeader header = CaptureHeader.read(in);
            int packetType = header.getPacketType();
            if (packetType != CaptureHeader.PACKET_TYPE_RAM) {
                throw new IOException("Expected CaptureRAM(" + CaptureHeader.PACKET_TYPE_RAM + ") packet, found " + packetType);
            }
            list.listBuffer = CaptureRAM.read(in);
        } else {
            throw new IOException("Not enough bytes remaining in stream");
        }

        return list;
    }

    //public PspGeList getPspGeList() {
    //    return list;
    //}

    public void commit() {
        VideoEngine.getInstance().pushDrawList(list);
        listBuffer.commit();
    }
}
