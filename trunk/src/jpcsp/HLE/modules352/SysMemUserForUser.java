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
package jpcsp.HLE.modules352;

import jpcsp.HLE.CanBeNull;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLELogging;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.PspString;
import jpcsp.HLE.StringInfo;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer32;
import jpcsp.HLE.kernel.types.SceKernelErrors;

@HLELogging
public class SysMemUserForUser extends jpcsp.HLE.modules280.SysMemUserForUser {
    // sceKernelFreeMemoryBlock (internal name)
	@HLEFunction(nid = 0x50F61D8A, version = 352)
	public int SysMemUserForUser_50F61D8A(int uid) {
		SysMemInfo info = blockList.remove(uid);
        if (info == null) {
            log.warn("SysMemUserForUser_50F61D8A(uid=0x" + Integer.toHexString(uid) + ") unknown uid");
            return SceKernelErrors.ERROR_KERNEL_UNKNOWN_UID;
        }

        free(info);

        return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0xACBD88CA, version = 352)
	public int SysMemUserForUser_ACBD88CA() {
		return 0;
	}

    // sceKernelGetMemoryBlockAddr (internal name)
	@HLEFunction(nid = 0xDB83A952, version = 352)
	public int SysMemUserForUser_DB83A952(int uid, TPointer32 addr) {
		SysMemInfo info = blockList.get(uid);
        if (info == null) {
            log.warn(String.format("SysMemUserForUser_DB83A952 uid=0x%X, addr=%s: unknown uid", uid, addr));
            return SceKernelErrors.ERROR_KERNEL_UNKNOWN_UID;
        }

        addr.setValue(info.addr);

        return 0;
	}

	// sceKernelAllocMemoryBlock (internal name)
	@HLEFunction(nid = 0xFE707FDF, version = 352)
	public int SysMemUserForUser_FE707FDF(@StringInfo(maxLength=32) PspString name, int type, int size, @CanBeNull TPointer paramsAddr) {
        if (paramsAddr.isNotNull()) {
        	int length = paramsAddr.getValue32();
        	if (length != 4) {
        		log.warn(String.format("SysMemUserForUser_FE707FDF: unknown parameters with length=%d", length));
        	}
        }

        if (type < PSP_SMEM_Low || type > PSP_SMEM_High) {
            return SceKernelErrors.ERROR_KERNEL_ILLEGAL_MEMBLOCK_ALLOC_TYPE;
        }

        // Always allocate memory in user area (partitionid == 2).
        SysMemInfo info = malloc(SysMemUserForUser.USER_PARTITION_ID, name.getString(), type, size, 0);
        if (info == null) {
        	return SceKernelErrors.ERROR_KERNEL_FAILED_ALLOC_MEMBLOCK;
        }

        return info.uid;
	}
}