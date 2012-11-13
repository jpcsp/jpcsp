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
import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.memory.IMemoryReader;
import jpcsp.memory.IMemoryWriter;
import jpcsp.memory.MemoryReader;
import jpcsp.memory.MemoryWriter;
import jpcsp.util.Utilities;

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

	@HLEFunction(nid = 0x19884A15, version = 150)
    public int sceMd5_19884A15(TPointer unknown) {
    	log.warn(String.format("PARTIAL sceMd5_19884A15 unknown=%s", unknown));

    	md5.reset();

    	return 0;
    }

    @HLEFunction(nid = 0xA30206C2, version = 150)
    public int sceMd5_A30206C2(TPointer unknown1, TPointer sourceAddr, int size) {
    	log.warn(String.format("PARTIAL sceMd5_A30206C2 unknown1=%s, sourceAddr=%s, size=%d: %s", unknown1, sourceAddr, size, Utilities.getMemoryDump(sourceAddr.getAddress(), size)));

    	byte[] source = getMemoryBytes(sourceAddr.getAddress(), size);
    	md5.update(source);

    	return 0;
    }

    @HLEFunction(nid = 0x4876AFFF, version = 150)
    public int sceMd5_4876AFFF(TPointer unknown1, TPointer resultAddr) {
    	log.warn(String.format("PARTIAL sceMd5_4876AFFF unknown1=%s, resultAddr=%s", unknown1, resultAddr));

    	byte[] result = md5.digest();
    	writeMd5Digest(resultAddr.getAddress(), result);

    	return 0;
    }

    @HLEFunction(nid = 0x98E31A9E, version = 150)
    public int sceMd5_98E31A9E(TPointer sourceAddr, int size, TPointer resultAddr) {
    	log.warn(String.format("PARTIAL sceMd5_98E31A9E sourceAddr=%s, size=%d, resultAddr=%s: %s", sourceAddr, size, resultAddr, Utilities.getMemoryDump(sourceAddr.getAddress(), size)));

    	byte[] source = getMemoryBytes(sourceAddr.getAddress(), size);
		md5.reset();
		byte[] result = md5.digest(source);
		writeMd5Digest(resultAddr.getAddress(), result);

    	return 0;
    }
}
