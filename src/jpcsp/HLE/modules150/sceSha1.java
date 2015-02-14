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
package jpcsp.HLE.modules150;

import org.apache.log4j.Logger;

import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLELogging;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.crypto.SHA1;
import jpcsp.memory.IMemoryReader;
import jpcsp.memory.IMemoryWriter;
import jpcsp.memory.MemoryReader;
import jpcsp.memory.MemoryWriter;
import jpcsp.util.Utilities;

@HLELogging
public class sceSha1 extends HLEModule {
    public static Logger log = Modules.getLogger("sceSha1");

    @Override
    public String getName() {
	return "sceSha1";
    }

    @Override
    public void start() {
        super.start();
    }

    @Override
    public void stop() {
	super.stop();
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x4A80340A, version = 150)
    public int sceSha1BlockInit(TPointer sha) {
	return 0;
    }
    
    @HLEUnimplemented
    @HLEFunction(nid = 0x5AF85569, version = 150)
    public int sceSha1BlockUpdate(TPointer sha, TPointer data, int length) {
	return 0;
    }
    
    @HLEUnimplemented
    @HLEFunction(nid = 0x78EDE680, version = 150)
    public int sceSha1BlockResult(TPointer sha, TPointer digest) {
	return 0;
    }
    
    @HLEFunction(nid = 0xB94ACDAE, version = 150)
    public int sceSha1Digest(TPointer data, int length, TPointer digest) {
        if (log.isTraceEnabled()) {
            log.trace(String.format("sceSha1Digest data:%s", Utilities.getMemoryDump(data.getAddress(), length)));
	}

        // Read in the source data.
	byte[] b = new byte[length];
	IMemoryReader memoryReader = MemoryReader.getMemoryReader(data.getAddress(), length, 1);
	for (int i = 0; i < length; i++) {
            b[i] = (byte) memoryReader.readNext();
	}

        // Calculate SHA-1.
        SHA1 sha1 = new SHA1();
        byte[] d = sha1.doSHA1(b, length);
        
        // Write back the resulting digest.
        IMemoryWriter memoryWriter = MemoryWriter.getMemoryWriter(digest.getAddress(), 0x14, 1);
	for (int i = 0; i < 0x14; i++) {
            memoryWriter.writeNext((byte) d[i]);
	}

	return 0;
    }
}
