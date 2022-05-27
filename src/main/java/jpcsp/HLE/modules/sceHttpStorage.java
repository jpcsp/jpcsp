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
import jpcsp.HLE.BufferInfo.LengthInfo;
import jpcsp.HLE.BufferInfo.Usage;
import jpcsp.HLE.CheckArgument;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.Modules;
import jpcsp.HLE.SceKernelErrorException;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer8;
import jpcsp.HLE.kernel.types.SceKernelErrors;

import org.apache.log4j.Logger;

public class sceHttpStorage extends HLEModule {
    public static Logger log = Modules.getLogger("sceHttpStorage");
    private static final int TYPE_AUTH_DAT = 0;
    private static final int TYPE_COOKIE_DAT = 1;
    private final int fileIds[] = new int[2];
    private static final String fileNames[] = {
    	"flash1:/net/http/auth.dat",
    	"flash1:/net/http/cookie.dat"
    };

    public int checkType(int type) {
    	if (type != TYPE_AUTH_DAT && type != TYPE_COOKIE_DAT) {
			throw new SceKernelErrorException(SceKernelErrors.ERROR_INVALID_ID);
    	}

    	return type;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x2D8DAE58, version = 150)
    public int sceHttpStorageGetstat(@CheckArgument("checkType") int type, TPointer statAddr) {
    	return Modules.IoFileMgrForUserModule.hleIoGetstat(0, fileNames[type], statAddr);
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x700AAD44, version = 150)
    public int sceHttpStorageOpen(@CheckArgument("checkType") int type, int flags, int permissions) {
    	int fileId = Modules.IoFileMgrForUserModule.hleIoOpen(0, fileNames[type], flags, permissions, false);
    	if (fileId < 0) {
    		return fileId;
    	}

    	fileIds[type] = fileId;

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xCDA3D8F6, version = 150)
    public int sceHttpStorageClose(@CheckArgument("checkType") int type) {
    	int result = Modules.IoFileMgrForUserModule.sceIoClose(fileIds[type]);
    	fileIds[type] = -1;

    	return result;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xB33389CE, version = 150)
    public int sceHttpStorageLseek(@CheckArgument("checkType") int type, long offset, int whence) {
    	return (int) Modules.IoFileMgrForUserModule.sceIoLseek(fileIds[type], offset, whence);
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xCDDF1103, version = 150)
    public int sceHttpStorageRead(@CheckArgument("checkType") int type, TPointer buffer, int bufferSize) {
    	return Modules.IoFileMgrForUserModule.sceIoRead(fileIds[type], buffer, bufferSize);
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x24AA94F4, version = 150)
    public int sceHttpStorageWrite(@CheckArgument("checkType") int type, TPointer buffer, int bufferSize) {
    	return Modules.IoFileMgrForUserModule.sceIoWrite(fileIds[type], buffer, bufferSize);
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x04EF00F8, version = 150)
    public int sceHttpStorage_04EF00F8(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=8, usage=Usage.out) TPointer8 psCode) {
    	return Modules.sceChkregModule.sceChkregGetPsCode(psCode);
    }
}
