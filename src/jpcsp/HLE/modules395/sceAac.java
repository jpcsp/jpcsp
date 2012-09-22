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
package jpcsp.HLE.modules395;

import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_AAC_INVALID_ADDRESS;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_AAC_INVALID_ID;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_AAC_INVALID_PARAMETER;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_AAC_RESOURCE_NOT_INITIALIZED;

import org.apache.log4j.Logger;

import jpcsp.Memory;
import jpcsp.HLE.CanBeNull;
import jpcsp.HLE.CheckArgument;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLELogging;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.Modules;
import jpcsp.HLE.SceKernelErrorException;
import jpcsp.HLE.TPointer32;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.HLE.modules150.SysMemUserForUser.SysMemInfo;

@HLELogging
public class sceAac extends HLEModule {
	public static Logger log = Modules.getLogger("sceAac");
	protected SysMemInfo resourceMem;
	protected AacInfo[] ids;

	protected static class AacInfo {
		private boolean init;

		public boolean isInit() {
			return init;
		}

		public void init() {
			init = true;
		}

		public void exit() {
			init = false;
		}
	}

	@Override
	public String getName() {
		return "sceAac";
	}

	@Override
	public void start() {
		ids = null;

		super.start();
	}

	public int checkId(int id) {
		if (ids == null || ids.length == 0) {
			throw new SceKernelErrorException(ERROR_AAC_RESOURCE_NOT_INITIALIZED);
		}
		if (id < 0 || id >= ids.length) {
			throw new SceKernelErrorException(ERROR_AAC_INVALID_ID);
		}

		return id;
	}

	public int checkInitId(int id) {
		id = checkId(id);
		if (!ids[id].isInit()) {
			throw new SceKernelErrorException(SceKernelErrors.ERROR_AAC_ID_NOT_INITIALIZED);
		}

		return id;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0xE0C89ACA, version = 395)
	public int sceAacInit(@CanBeNull TPointer32 parameters, int unknown1, int unknown2, int unknown3) {
		if (parameters.isNull()) {
			return ERROR_AAC_INVALID_ADDRESS;
		}

		int value0 = parameters.getValue(0);
		int value4 = parameters.getValue(4);
		int value8 = parameters.getValue(8);
		int value12 = parameters.getValue(12);
		int value16 = parameters.getValue(16);
		int value20 = parameters.getValue(20);
		int value24 = parameters.getValue(24);
		int value28 = parameters.getValue(28);
		int freq = parameters.getValue(32);
		int value36 = parameters.getValue(36);
		if (log.isDebugEnabled()) {
			log.debug(String.format("sceAacInit parameters: value0=0x%08X, value4=0x%08X, value8=0x%08X, value12=0x%08X, value16=0x%08X, value20=0x%08X, value24=0x%08X, value28=0x%08X, freq=%d, value36=0x%08X", value0, value4, value8, value12, value16, value20, value24, value28, freq, value36));
		}

		if (value16 == 0 || value24 == 0) {
			return ERROR_AAC_INVALID_ADDRESS;
		}
		if (value4 < 0 || value4 > value12) {
			return ERROR_AAC_INVALID_PARAMETER;
		}
		if (value4 == value12) {
			if (value0 >= value8) {
				return ERROR_AAC_INVALID_PARAMETER;
			}
		}
		if (value20 < 8192 || value28 < 8192 || value36 != 0) {
			return ERROR_AAC_INVALID_PARAMETER;
		}
		if (freq != 44100 && freq != 32000 && freq != 48000 && freq != 24000) {
			return ERROR_AAC_INVALID_PARAMETER;
		}

		int id = -1;
		for (int i = 0; i < ids.length; i++) {
			if (!ids[i].isInit()) {
				id = i;
				break;
			}
		}
		if (id < 0) {
			return SceKernelErrors.ERROR_AAC_NO_MORE_FREE_ID;
		}

		ids[id].init();

		return id;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x33B8C009, version = 395)
	public int sceAacExit(@CheckArgument("checkId") int id) {
		ids[id].exit();

		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x5CFFC57C, version = 395)
	public int sceAacInitResource(int numberIds) {
		int memSize = numberIds * 0x19000;
		resourceMem = Modules.SysMemUserForUserModule.malloc(SysMemUserForUser.USER_PARTITION_ID, "SceLibAacResource", SysMemUserForUser.PSP_SMEM_Low, memSize, 0);

		if (resourceMem == null) {
			return SceKernelErrors.ERROR_AAC_NOT_ENOUGH_MEMORY;
		}

		Memory.getInstance().memset(resourceMem.addr, (byte) 0, memSize);

		ids = new AacInfo[numberIds];
		for (int i = 0; i < numberIds; i++) {
			ids[i] = new AacInfo();
		}

		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x23D35CAE, version = 395)
	public int sceAacTermResource() {
		if (resourceMem != null) {
			Modules.SysMemUserForUserModule.free(resourceMem);
			resourceMem = null;
		}

		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x7E4CFEE4, version = 395)
	public int sceAacDecode(@CheckArgument("checkInitId") int id, @CanBeNull TPointer32 bufferAddress) {
		bufferAddress.setValue(resourceMem.addr);

		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x523347D9, version = 395)
	public int sceAacGetLoopNum() {
		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0xBBDD6403, version = 395)
	public int sceAacSetLoopNum() {
		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0xD7C51541, version = 395)
	public int sceAacCheckStreamDataNeeded(@CheckArgument("checkInitId") int id) {
		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0xAC6DCBE3, version = 395)
	public int sceAacNotifyAddStreamData(@CheckArgument("checkInitId") int id, int unknown) {
		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x02098C69, version = 395)
	public int sceAacGetInfoToAddStreamData(@CheckArgument("checkInitId") int id, TPointer32 unknown1, TPointer32 unknown2, TPointer32 unknown3) {
		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x6DC7758A, version = 395)
	public int sceAacGetMaxOutputSample() {
		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x506BF66C, version = 395)
	public int sceAacGetSumDecodedSample() {
		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0xD2DA2BBA, version = 395)
	public int sceAacResetPlayPosition() {
		return 0;
	}
}
