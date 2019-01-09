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

import jpcsp.HLE.BufferInfo;
import jpcsp.HLE.BufferInfo.LengthInfo;
import jpcsp.HLE.BufferInfo.Usage;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer32;
import jpcsp.HLE.VFS.IVirtualFile;
import jpcsp.HLE.VFS.IVirtualFileSystem;
import jpcsp.HLE.kernel.types.SceIoStat;
import jpcsp.crypto.CryptoEngine;

public class sceResmgr extends HLEModule {
    public static Logger log = Modules.getLogger("sceResmgr");

    @Override
	public void start() {
    	createDummyIndexDat();

    	super.start();
	}

    private static void createDummyIndexDat() {
    	String fileName = "flash0:/vsh/etc/index_01g.dat";
    	StringBuilder localFileName = new StringBuilder();
    	IVirtualFileSystem vfs = Modules.IoFileMgrForUserModule.getVirtualFileSystem(fileName, localFileName);
    	if (vfs == null) {
    		return;
    	}

    	SceIoStat stat = new SceIoStat();
    	int result = vfs.ioGetstat(localFileName.toString(), stat);
    	if (result == 0 && stat.size > 0L) {
    		// File already exists
    		return;
    	}

    	vfs.ioMkdir("vsh", 0777);
    	vfs.ioMkdir("vsh/etc", 0777);
    	IVirtualFile vFile = vfs.ioOpen(localFileName.toString(), IoFileMgrForUser.PSP_O_WRONLY, 0777);

    	String dummyContent = "release:6.60:\n";
    	dummyContent += "build:5455,0,3,1,0:builder@vsh-build6\n";
    	dummyContent += "system:57716@release_660,0x06060010:\n";
    	dummyContent += "vsh:p6616@release_660,v58533@release_660,20110727:\n";
    	dummyContent += "target:1:WorldWide\n";

    	byte[] bytes = dummyContent.getBytes();
    	vFile.ioWrite(bytes, 0, bytes.length);
    	vFile.ioClose();
    }

	@HLEFunction(nid = 0x9DC14891, version = 150)
    public int sceResmgr_9DC14891(@BufferInfo(lengthInfo=LengthInfo.nextParameter, usage=Usage.inout) TPointer buffer, int bufferSize, @BufferInfo(usage=Usage.out) TPointer32 resultLengthAddr) {
    	int resultLength;

    	// Nothing to do ff the buffer is already decrypted
    	if ("release:".equals(buffer.getStringNZ(0, 8))) {
    		resultLength = bufferSize;
    	} else {
    		byte[] buf = buffer.getArray8(bufferSize);

	    	int result = new CryptoEngine().getPRXEngine().DecryptPRX(buf, bufferSize, 9, null, null);
	    	if (result < 0) {
	    		log.error(String.format("sceResmgr_9DC14891 returning error 0x%08X", result));
	    		return result;
	    	}

	    	resultLength = result;
	    	buffer.setArray(buf, resultLength);
    	}

    	resultLengthAddr.setValue(resultLength);

    	return 0;
    }
}
