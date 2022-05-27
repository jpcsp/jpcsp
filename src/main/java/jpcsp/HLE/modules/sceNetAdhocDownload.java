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

import jpcsp.HLE.BufferInfo;
import jpcsp.HLE.CanBeNull;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer32;
import jpcsp.HLE.BufferInfo.LengthInfo;
import jpcsp.HLE.BufferInfo.Usage;

import org.apache.log4j.Logger;

public class sceNetAdhocDownload extends HLEModule {
    public static Logger log = Modules.getLogger("sceNetAdhocDownload");

    @HLEUnimplemented
    @HLEFunction(nid = 0x117CA01A, version = 150)
    public int sceNetAdhocDownloadTermServer() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x13DAB550, version = 150)
    public int sceNetAdhocDownloadCreateServer() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x1AD5CC88, version = 150)
    public int sceNetAdhocDownloadAbortRecv() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x22C2BCC6, version = 150)
    public int sceNetAdhocDownloadGetServerList(@BufferInfo(usage=Usage.inout) TPointer32 sizeAddr, @CanBeNull @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=160, usage=Usage.out) TPointer buf) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x2B6FB0DA, version = 150)
    public int sceNetAdhocDownloadStartServer() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x3082F4E2, version = 150)
    public int sceNetAdhocDownloadInitClient() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x314ED31E, version = 150)
    public int sceNetAdhocDownloadStartClient() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x378D4311, version = 150)
    public int sceNetAdhocDownloadDeleteClient() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x469F6B83, version = 150)
    public int sceNetAdhocDownloadStopClient() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x4E6029F1, version = 150)
    public int sceNetAdhocDownloadRequestSession() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x57A51DD0, version = 150)
    public int sceNetAdhocDownloadCreateClient(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=32, usage=Usage.inout) TPointer unknown1, int unknown2) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x7A483F9E, version = 150)
    public int sceNetAdhocDownloadDeleteServer() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x8846D2B0, version = 150)
    public int sceNetAdhocDownloadRecv() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x8A5500E0, version = 150)
    public int sceNetAdhocDownloadAbortRequestSession() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xA21FEF45, version = 150)
    public int sceNetAdhocDownloadInitServer() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xA70FDFBE, version = 150)
    public int sceNetAdhocDownloadAbortSend() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xBF1433F0, version = 150)
    public int sceNetAdhocDownloadTermClient() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xC421875C, version = 150)
    public int sceNetAdhocDownloadAbortReplySession() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xD0189004, version = 150)
    public int sceNetAdhocDownloadSend() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xF76147B1, version = 150)
    public int sceNetAdhocDownloadStopServer() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xF8FC359E, version = 150)
    public int sceNetAdhocDownloadReplySession() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x0A531FBE, version = 150)
    public int sceNetAdhocDownload_0A531FBE() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x24FD9B7A, version = 150)
    public int sceNetAdhocDownload_24FD9B7A(int evthPri, int evthPartitionId, int evthStack, int inthPri, int inthPartitionId, int inthStack) {
    	// Calls sceNetAdhocMatchingStart2 with the received parameters
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x3B6A7B35, version = 150)
    public int sceNetAdhocDownload_3B6A7B35() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x48252AF0, version = 150)
    public int sceNetAdhocDownload_48252AF0() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x48F87AB1, version = 150)
    public int sceNetAdhocDownload_48F87AB1() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x4C542FD1, version = 150)
    public int sceNetAdhocDownload_4C542FD1() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x827157E2, version = 150)
    public int sceNetAdhocDownload_827157E2() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x8A987F07, version = 150)
    public int sceNetAdhocDownload_8A987F07() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xA45230D9, version = 150)
    public int sceNetAdhocDownload_A45230D9() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xC8402ACD, version = 150)
    public int sceNetAdhocDownload_C8402ACD() {
    	return 0;
    }
}
