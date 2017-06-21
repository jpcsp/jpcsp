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

public class sceNand extends HLEModule {
    public static Logger log = Modules.getLogger("sceNand");

    @HLEUnimplemented
    @HLEFunction(nid = 0xB07C41D4, version = 150)
    public int sceNandGetPagesPerBlock() {
    	// Has no parameters
        return 0x20;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xCE9843E6, version = 150)
    public int sceNandGetPageSize() {
    	// Has no parameters
        return 0x200;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xAE4438C7, version = 150)
    public int sceNandLock(int mode) {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x41FFA822, version = 150)
    public int sceNandUnlock() {
    	// Has no parameters
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x01F09203, version = 150)
    public int sceNandIsBadBlock(int ppn) {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x0BEE8F36, version = 150)
    public int sceNandSetScramble(int scrmb) {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x84EE5D76, version = 150)
    public boolean sceNandSetWriteProtect(boolean protect) {
        return false;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x8932166A, version = 150)
    public int sceNandWritePagesRawExtra(int ppn, TPointer user, TPointer spare, int len) {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x5182C394, version = 150)
    public int sceNandReadExtraOnly(int ppn, TPointer spare, int len) {
    	spare.clear(len * 16);
    	spare.setValue32(8, 0x6DC64A38);

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x89BDCA08, version = 150)
    public int sceNandReadPages(int ppn, TPointer user, @CanBeNull TPointer spare, int len) {
    	user.clear(len * 512);
		spare.clear(16);

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xE05AE88D, version = 150)
    public int sceNandReadPagesRawExtra(int ppn, TPointer user, TPointer spare, int len) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xC32EA051, version = 150)
    public int sceNandReadBlockWithRetry(int ppn, TPointer user, TPointer spare) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xB2B021E5, version = 150)
    public int sceNandWriteBlockWithVerify(int ppn, TPointer user, TPointer spare) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xC1376222, version = 150)
    public int sceNandGetTotalBlocks() {
    	// Has no parameters
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xE41A11DE, version = 150)
    public int sceNandReadStatus() {
    	// Has no parameters
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xEB0A0022, version = 150)
    public int sceNandEraseBlock(int ppn) {
    	return 0;
    }
}
