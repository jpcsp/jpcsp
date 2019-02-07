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

import static jpcsp.util.Utilities.readCompleteFile;
import static jpcsp.util.Utilities.write8;
import static jpcsp.util.Utilities.writeCompleteFile;
import static jpcsp.util.Utilities.writeStringNZ;
import static jpcsp.util.Utilities.writeUnaligned32;

import org.apache.log4j.Logger;

import jpcsp.HLE.BufferInfo;
import jpcsp.HLE.BufferInfo.LengthInfo;
import jpcsp.HLE.BufferInfo.Usage;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer32;
import jpcsp.crypto.CryptoEngine;
import jpcsp.crypto.PRX;
import jpcsp.hardware.Model;

public class sceResmgr extends HLEModule {
    public static Logger log = Modules.getLogger("sceResmgr");
	// Fake a version 6.59 so that the PSP Update 6.60 can be executed
	public static final String dummyIndexDatContent = "release:6.59:\n" +
			"build:5454,0,3,1,0:builder@vsh-build6\n" +
			"system:57716@release_660,0x06060010:\n" +
			"vsh:p6616@release_660,v58533@release_660,20110727:\n" +
			"target:1:WorldWide\n";

    @Override
	public void start() {
    	createDummyIndexDat();

    	super.start();
	}

    private static void createDummyIndexDat() {
    	String indexDatFileName = String.format("flash0:/vsh/etc/index_%02dg.dat", Model.getGeneration());
    	byte[] content = readCompleteFile(indexDatFileName);
    	if (content != null && content.length > 0) {
    		// File already exists
    		return;
    	}

    	// A few entries in PreDecrypt.xml will allow the decryption of this dummy file
    	byte[] buffer = new byte[0x1F0];
    	write8(buffer, 0x7C, PRX.DECRYPT_MODE_NO_EXEC); // decryptMode
    	writeUnaligned32(buffer, 0xB0, 0x9F); // dataSize
    	writeUnaligned32(buffer, 0xB4, 0x80); // dataOffset
    	int tag;
    	switch (Model.getGeneration()) {
    		case 1:  tag = 0x0B2B90F0; break;
    		case 2:  tag = 0x0B2B91F0; break;
    		default: tag = 0x0B2B92F0; break;
    	}
    	writeUnaligned32(buffer, 0xD0, tag); // tag

    	writeStringNZ(buffer, 0x150, buffer.length - 0x150, dummyIndexDatContent);

    	writeCompleteFile(indexDatFileName, buffer, true);
    }

	@HLEFunction(nid = 0x9DC14891, version = 150)
    public int sceResmgr_9DC14891(@BufferInfo(lengthInfo=LengthInfo.nextParameter, usage=Usage.inout) TPointer buffer, int bufferSize, @BufferInfo(usage=Usage.out) TPointer32 resultLengthAddr) {
    	int resultLength;

    	// Nothing to do if the buffer is already decrypted
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
