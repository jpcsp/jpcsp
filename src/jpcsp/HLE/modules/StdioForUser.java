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

import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.Managers;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.kernel.types.SceKernelMppInfo;

import org.apache.log4j.Logger;

public class StdioForUser extends HLEModule {
    public static Logger log = Modules.getLogger("StdioForUser");

    @HLEUnimplemented
    @HLEFunction(nid = 0x3054D478, version = 150)
    public int sceKernelStdioRead() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x0CBB0571, version = 150)
    public int sceKernelStdioLseek() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xA46785C9, version = 150)
    public int sceKernelStdioSendChar() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xA3B931DB, version = 150)
    public int sceKernelStdioWrite() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x9D061C19, version = 150)
    public int sceKernelStdioClose() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x924ABA61, version = 150)
    public int sceKernelStdioOpen() {
    	return 0;
    }

    @HLEFunction(nid = 0x172D316E, version = 150)
    public int sceKernelStdin() {
        return IoFileMgrForUser.STDIN_ID;
    }

    @HLEFunction(nid = 0xA6BAB2E9, version = 150)
    public int sceKernelStdout() {
        return IoFileMgrForUser.STDOUT_ID;
    }

    @HLEFunction(nid = 0xF78BA90A, version = 150)
    public int sceKernelStderr() {
        return IoFileMgrForUser.STDERR_ID;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x432D8F5C, version = 300)
    public int sceKernelRegisterStdoutPipe(int msgPipeUid) {
    	SceKernelMppInfo msgPipeInfo = Managers.msgPipes.getMsgPipeInfo(msgPipeUid);
    	if (msgPipeInfo == null) {
    		return SceKernelErrors.ERROR_KERNEL_ILLEGAL_ARGUMENT;
    	}

    	log.info(String.format("sceKernelRegisterStdoutPipe %s", msgPipeInfo));

    	Modules.IoFileMgrForUserModule.hleRegisterStdPipe(IoFileMgrForUser.STDOUT_ID, msgPipeInfo);

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x6F797E03, version = 300)
    public int sceKernelRegisterStderrPipe(int msgPipeUid) {
    	SceKernelMppInfo msgPipeInfo = Managers.msgPipes.getMsgPipeInfo(msgPipeUid);
    	if (msgPipeInfo == null) {
    		return SceKernelErrors.ERROR_KERNEL_ILLEGAL_ARGUMENT;
    	}

    	log.info(String.format("sceKernelRegisterStderrPipe %s", msgPipeInfo));

    	Modules.IoFileMgrForUserModule.hleRegisterStdPipe(IoFileMgrForUser.STDERR_ID, msgPipeInfo);

    	return 0;
    }
}