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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.log4j.Logger;

import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLELogging;
import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.memory.IMemoryReader;
import jpcsp.memory.IMemoryWriter;
import jpcsp.memory.MemoryReader;
import jpcsp.memory.MemoryWriter;
import jpcsp.util.Utilities;

@HLELogging
public class sceMd5 extends HLEModule {
    public static Logger log = Modules.getLogger("sceMd5");
    protected MessageDigest md5;

	@Override
	public String getName() {
		return "sceMd5";
	}

	@Override
	public void start() {
		try {
			md5 = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			log.error("Cannot find MD5", e);
		}

		super.start();
	}

	@Override
	public void stop() {
		md5 = null;
		super.stop();
	}

	protected static byte[] getMemoryBytes(int address, int size) {
    	byte[] bytes = new byte[size];
    	IMemoryReader memoryReader = MemoryReader.getMemoryReader(address, size, 1);
    	for (int i = 0; i < size; i++) {
    		bytes[i] = (byte) memoryReader.readNext();
    	}

    	return bytes;
	}

	protected static void writeMd5Digest(int address, byte[] digest) {
		// The PSP returns 16 bytes
		final int digestLength = 16;
		int size = digest == null ? 0 : Math.min(digest.length, digestLength);
		IMemoryWriter memoryWriter = MemoryWriter.getMemoryWriter(address, digestLength, 1);
		for (int i = 0; i < size; i++) {
			memoryWriter.writeNext(digest[i] & 0xFF);
		}
		for (int i = size; i < digestLength; i++) {
			memoryWriter.writeNext(0);
		}
		memoryWriter.flush();

		if (log.isTraceEnabled()) {
			log.trace(String.format("return MD5 digest: %s", Utilities.getMemoryDump(address, digestLength)));
		}
	}

	@HLELogging(level="info")
	@HLEFunction(nid = 0x19884A15, version = 150)
    public int sceMd5BlockInit(TPointer contextAddr) {
    	md5.reset();

    	// size of context seems to be 32 + 64 bytes
    	contextAddr.setValue32(0, 0x67452301);
    	contextAddr.setValue32(4, 0xEFCDAB89);
    	contextAddr.setValue32(8, 0x98BADCFE);
    	contextAddr.setValue32(12, 0x10325476);
    	contextAddr.setValue16(20, (short) 0);
    	contextAddr.setValue16(22, (short) 0);
    	contextAddr.setValue32(24, 0);
    	contextAddr.setValue32(28, 0);
    	// followed by 64 bytes, not being initialized here (probably the data block being processed).

    	return 0;
    }

    @HLEFunction(nid = 0xA30206C2, version = 150)
    public int sceMd5BlockUpdate(TPointer contextAddr, TPointer sourceAddr, int size) {
    	byte[] source = getMemoryBytes(sourceAddr.getAddress(), size);
    	md5.update(source);

    	return 0;
    }

    @HLEFunction(nid = 0x4876AFFF, version = 150)
    public int sceMd5BlockResult(TPointer contextAddr, TPointer resultAddr) {
    	byte[] result = md5.digest();
    	writeMd5Digest(resultAddr.getAddress(), result);

    	return 0;
    }

    @HLEFunction(nid = 0x98E31A9E, version = 150)
    public int sceMd5Digest(TPointer sourceAddr, int size, TPointer resultAddr) {
    	byte[] source = getMemoryBytes(sourceAddr.getAddress(), size);
		md5.reset();
		byte[] result = md5.digest(source);
		writeMd5Digest(resultAddr.getAddress(), result);

    	return 0;
    }
}
