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
import jpcsp.HLE.kernel.types.PspGeList;
import jpcsp.graphics.VideoEngine;

/** captures a display list
 * - PspGeList details
 * - backing RAM containing the GE instructions */
public class CaptureList {
	public static Logger log = CaptureManager.log;

    private PspGeList list;

    private CaptureList() {
    }

    public CaptureList(Memory mem, PspGeList list) throws IOException {
    	this.list = new PspGeList(list.id);
    	this.list.init(list.list_addr, list.getStallAddr(), list.cbid, list.optParams);
    }

    public void write(DataOutputStream out) throws IOException {
    	out.writeInt(CaptureManager.PACKET_TYPE_START_LIST);
        out.writeInt(list.list_addr);
        out.writeInt(list.getStallAddr());
        out.writeInt(list.cbid);
    }

    public static CaptureList read(DataInputStream in) throws IOException {
        CaptureList list = new CaptureList();

        int list_addr = in.readInt();
        int stall_addr = in.readInt();
        int cbid = in.readInt();

        if (log.isDebugEnabled()) {
        	log.debug(String.format("CaptureList list_addr=0x%08X, stall_addr=0x%08X, cbid=0x%X", list_addr, stall_addr, cbid));
        }

        list.list = new PspGeList(-1);
        list.list.init(list_addr, 0, 0, null);

        return list;
    }

    public void commit(Memory mem) {
        VideoEngine.getInstance().pushDrawList(list);
    }
}
