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
package jpcsp.HLE.modules330;

import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLELogging;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.Managers;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.kernel.types.SceKernelMppInfo;
import jpcsp.HLE.modules.IoFileMgrForUser;

@HLELogging
public class StdioForUser extends jpcsp.HLE.modules150.StdioForUser {
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
}
