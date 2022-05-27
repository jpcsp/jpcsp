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

import static jpcsp.crypto.KIRK.PSP_KIRK_CMD_DECRYPT_FUSE;
import static jpcsp.crypto.KIRK.PSP_KIRK_CMD_MODE_DECRYPT_CBC;
import static jpcsp.util.Utilities.writeUnaligned32;

import org.apache.log4j.Logger;

import jpcsp.HLE.BufferInfo;
import jpcsp.HLE.BufferInfo.LengthInfo;
import jpcsp.HLE.BufferInfo.Usage;
import jpcsp.crypto.CryptoEngine;
import jpcsp.crypto.KeyVault;
import jpcsp.crypto.PRX;
import jpcsp.crypto.PRX.TAG_INFO;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer32;

public class memlmd extends HLEModule {
    public static Logger log = Modules.getLogger("memlmd");
    private PRX prxEngine = new CryptoEngine().getPRXEngine();

    public static byte[] getKey(int[] intKey) {
    	byte[] key = new byte[intKey.length << 2];
    	for (int i = 0, j = 0; i < intKey.length; i++) {
    		key[j++] = (byte) intKey[i];
    		key[j++] = (byte) (intKey[i] >> 8);
    		key[j++] = (byte) (intKey[i] >> 16);
    		key[j++] = (byte) (intKey[i] >> 24);
    	}

    	return key;
    }

    public int hleMemlmd_6192F715(byte[] buffer, int size) {
    	final byte[] xorInputKey = getKey(new int[] { 0xB04D85AA, 0xEB47CAFF, 0xE4D77F38, 0x10B0623D });
    	final byte[] xorOutputKey = getKey(new int[] { 0x31A8F671, 0x1EFFE01E, 0xD26CBA50, 0x2DD62D98 });
    	final int outSize = 0xD0;
    	final byte[] tmpBuffer = new byte[0x14 + outSize];

    	writeUnaligned32(tmpBuffer, 0x00, PSP_KIRK_CMD_MODE_DECRYPT_CBC);
    	writeUnaligned32(tmpBuffer, 0x04, 0);
    	writeUnaligned32(tmpBuffer, 0x08, 0);
    	writeUnaligned32(tmpBuffer, 0x0C, 0x100);
    	writeUnaligned32(tmpBuffer, 0x10, outSize);

    	for (int i = 0; i < outSize; i++) {
    		tmpBuffer[0x14 + i] = (byte) (buffer[0x80 + i] ^ xorInputKey[i & 0xF]);
    	}

    	int result = Modules.semaphoreModule.hleUtilsBufferCopyWithRange(tmpBuffer, outSize, tmpBuffer, tmpBuffer.length, PSP_KIRK_CMD_DECRYPT_FUSE);
    	if (result != 0) {
    		return -1;
    	}

    	for (int i = 0; i < outSize; i++) {
    		tmpBuffer[i] ^= xorOutputKey[i & 0xF];
    	}

    	System.arraycopy(tmpBuffer, 0x40, buffer, 0x80, 0x90);
    	System.arraycopy(tmpBuffer, 0, buffer, 0x110, 0x40);

    	return 0;
    }

    @HLEUnimplemented
	@HLEFunction(nid = 0x6192F715, version = 660)
	public int memlmd_6192F715(@BufferInfo(lengthInfo=LengthInfo.nextParameter, usage=Usage.inout, maxDumpLength=512) TPointer buffer, int size) {
    	byte[] byteBuffer = new byte[0x80 + 0xD0];
    	buffer.getArray8(byteBuffer);
    	int result = hleMemlmd_6192F715(byteBuffer, size);
    	buffer.setArray(byteBuffer);

    	return result;
    }

    /*
     * See
     *    https://github.com/uofw/uofw/blob/master/src/loadcore/loadcore.c
     * CheckLatestSubType()
     */
    @HLEUnimplemented
	@HLEFunction(nid = 0x9D36A439, version = 660)
	public boolean memlmd_9D36A439(int subType) {
		return true;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0xF26A33C3, version = 660)
	public int memlmd_F26A33C3(int unknown, int hardwarePtr) {
		return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0x2F3D7E2D, version = 660)
	public int memlmd_2F3D7E2D() {
    	// Has no parameters
		return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0x2AE425D2, version = 660)
	public boolean memlmd_2AE425D2(int subType) {
		return true;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0x8BDB1A3E, version = 150)
	@HLEFunction(nid = 0xEF73E85B, version = 660)
	public int sceUtilsGetLoadModuleABLength(@BufferInfo(lengthInfo = LengthInfo.nextParameter, usage = Usage.inout) TPointer loadModuleBuffer, int size, @BufferInfo(usage = Usage.out) TPointer32 lengthAddr) {
    	if (Modules.LoadCoreForKernelModule.decodeInitModuleData(loadModuleBuffer, size, lengthAddr)) {
    		return 0;
    	}

    	byte[] bytes = loadModuleBuffer.getArray8(size);

    	int tag = loadModuleBuffer.getUnalignedValue32(0xD0);
    	TAG_INFO tagInfo;
    	switch (tag) {
    		case 0x00000000:
    	    	tagInfo = new TAG_INFO(tag, KeyVault.key_00000000, 0x42, 0);
    			break;
    		case 0x01000000:
    	    	tagInfo = new TAG_INFO(tag, KeyVault.key_01000000, 0x43, 0);
    	    	break;
	    	default:
	    		log.error(String.format("sceUtilsGetLoadModuleABLength unknown tag 0x%08X", tag));
    	    	return -1;
    	}

    	final int type = 0;
    	int result = prxEngine.DecryptPRX(bytes, size, type, null, null, false, tagInfo);
    	if (result >= 0) {
    		lengthAddr.setValue(result);
    		loadModuleBuffer.setArray(bytes, result);
    		result = 0;
    	}

    	return result;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0x185F0A2A, version = 150)
	@HLEFunction(nid = 0xCF03556B, version = 660)
	public int sceUtilsGetLoadModuleABLengthByPolling(@BufferInfo(lengthInfo = LengthInfo.nextParameter, usage = Usage.inout) TPointer loadModuleBuffer, int size, @BufferInfo(usage = Usage.out) TPointer32 lengthAddr) {
    	return sceUtilsGetLoadModuleABLength(loadModuleBuffer, size, lengthAddr);
	}
}
