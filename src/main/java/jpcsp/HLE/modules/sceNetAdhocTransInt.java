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
package jpcsp.HLE.modules;

import org.apache.log4j.Logger;

import jpcsp.HLE.CanBeNull;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer32;

public class sceNetAdhocTransInt extends HLEModule {
    public static Logger log = Modules.getLogger("sceNetAdhocTransInt");

    @HLEUnimplemented
    @HLEFunction(nid = 0x0B1F77E1, version = 150)
    public int sceNetAdhocTransferSocketAbortParent() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x0CAE4A7B, version = 150)
    public int sceNetAdhocTransferRequestSession() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x24F04EF8, version = 150)
    public int sceNetAdhocTransferDeleteChild() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x40D77905, version = 150)
    public int sceNetAdhocTransferGetChildRequest() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x48381015, version = 150)
    public int sceNetAdhocTransferAbortRequestSession() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x541E3EAB, version = 150)
    public int sceNetAdhocTransferTermChild() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x552E0C4F, version = 150)
    public int sceNetAdhocTransferInitParent() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x645E312A, version = 150)
    public int sceNetAdhocTransferStopChild() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x658833CC, version = 150)
    public int sceNetAdhocTransferStartParent() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x69F19666, version = 150)
    public int sceNetAdhocTransferCreateParent() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x6D8B0D00, version = 150)
    public int sceNetAdhocTransferRecvParent() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x6F595DFA, version = 150)
    public int sceNetAdhocTransferSocketAbortChild() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x75DAEB6B, version = 150)
    public int sceNetAdhocTransferInitChild(int memorySize) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x7CFB68C9, version = 150)
    public int sceNetAdhocTransferGetMallocStatChild() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x83FA2BBA, version = 150)
    public int sceNetAdhocTransferAbortReplySession() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x8897358A, version = 150)
    public int sceNetAdhocTransferGetParentList(TPointer32 count, @CanBeNull TPointer buffer) {
    	count.setValue(0);

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x97D389A3, version = 150)
    public int sceNetAdhocTransferSendChild() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x9C6EE447, version = 150)
    public int sceNetAdhocTransferSendParent() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xA64B28F7, version = 150)
    public int sceNetAdhocTransferStartChild(int unknown1, int unknown2) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xA735B9B2, version = 150)
    public int sceNetAdhocTransferTermParent() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xAD0E88C6, version = 150)
    public int sceNetAdhocTransferReplySession() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xB0975880, version = 150)
    public int sceNetAdhocTransferGetMallocStatParent() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xCD469448, version = 150)
    public int sceNetAdhocTransferStopParent() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xCD9F1D46, version = 150)
    public int sceNetAdhocTransferCreateChild(int unknown1, int unknown2) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xD438691D, version = 150)
    public int sceNetAdhocTransferRecvChild() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xE3707672, version = 150)
    public int sceNetAdhocTransferDeleteParent() {
    	return 0;
    }
}
