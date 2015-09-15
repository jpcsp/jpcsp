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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.apache.log4j.Logger;

import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLELogging;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointer;
import jpcsp.crypto.CryptoEngine;
import jpcsp.memory.IMemoryReader;
import jpcsp.memory.IMemoryWriter;
import jpcsp.memory.MemoryReader;
import jpcsp.memory.MemoryWriter;
import jpcsp.util.Utilities;

public class semaphore extends HLEModule {
	public static Logger log = Modules.getLogger("semaphore");

	@HLELogging(level = "info")
    @HLEFunction(nid = 0x4C537C72, version = 150)
    public int sceUtilsBufferCopyWithRange(TPointer outAddr, int outSize, TPointer inAddr, int inSize, int cmd) {
		// The input size needs for some KIRK commands to be 16-bytes aligned
    	inSize = Utilities.alignUp(inSize, 15);

    	// Read the whole input buffer, including a possible header
    	// (up to 144 bytes, depending on the KIRK command)
    	byte[] inBytes = new byte[inSize + 144]; // Up to 144 bytes header
    	ByteBuffer inBuffer = ByteBuffer.wrap(inBytes).order(ByteOrder.LITTLE_ENDIAN);
    	IMemoryReader memoryReaderIn = MemoryReader.getMemoryReader(inAddr.getAddress(), inBytes.length, 1);
    	for (int i = 0; i < inBytes.length; i++) {
    		inBytes[i] = (byte) memoryReaderIn.readNext();
    	}

    	if (log.isTraceEnabled()) {
    		log.trace(String.format("sceUtilsBufferCopyWithRange inAddr: %s", Utilities.getMemoryDump(inAddr.getAddress(), inSize)));
    	}

    	// Some KIRK commands (e.g. PSP_KIRK_CMD_SHA1_HASH) only update a part of the output buffer.
    	// Read the whole output buffer so that it can be updated completely after the KIRK call.
    	byte[] outBytes = new byte[Utilities.alignUp(outSize, 15)];
    	ByteBuffer outBuffer = ByteBuffer.wrap(outBytes).order(ByteOrder.LITTLE_ENDIAN);
    	IMemoryReader memoryReaderOut = MemoryReader.getMemoryReader(outAddr.getAddress(), outBytes.length, 1);
    	for (int i = 0; i < outBytes.length; i++) {
    		outBytes[i] = (byte) memoryReaderOut.readNext();
    	}

    	// Call the KIRK engine to perform the given command
    	CryptoEngine crypto = new CryptoEngine();
    	crypto.getKIRKEngine().hleUtilsBufferCopyWithRange(outBuffer, outSize, inBuffer, inSize, cmd);

    	// Write back the whole output buffer to the memory.
    	IMemoryWriter memoryWriter = MemoryWriter.getMemoryWriter(outAddr.getAddress(), outSize, 1);
    	for (int i = 0; i < outSize; i++) {
    		memoryWriter.writeNext(outBytes[i] & 0xFF);
    	}
    	memoryWriter.flush();

    	if (log.isTraceEnabled()) {
    		log.trace(String.format("sceUtilsBufferCopyWithRange outAddr: %s", Utilities.getMemoryDump(outAddr.getAddress(), outSize)));
    	}

    	return 0;
    }

	@HLELogging(level = "info")
    @HLEFunction(nid = 0x77E97079, version = 150)
    public int sceUtilsBufferCopyByPollingWithRange(TPointer outAddr, int outSize, TPointer inAddr, int inSize, int cmd) {
		return sceUtilsBufferCopyWithRange(outAddr, outSize, inAddr, inSize, cmd);
	}
}
