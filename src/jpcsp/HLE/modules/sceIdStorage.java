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
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointer;

public class sceIdStorage extends HLEModule {
	public static Logger log = Modules.getLogger("sceIdStorage");

	@HLEUnimplemented
	@HLEFunction(nid = 0xAB129D20, version = 150)
	public int sceIdStorageInit() {
		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x2CE0BE69, version = 150)
	public int sceIdStorageEnd() {
		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0xF77565B6, version = 150)
	public int sceIdStorageSuspend() {
		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0xFE51173D, version = 150)
	public int sceIdStorageResume() {
		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0xEB830733, version = 150)
	public int sceIdStorageGetLeafSize() {
		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0xFEFA40C2, version = 150)
	public int sceIdStorageIsFormatted() {
		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x2D633688, version = 150)
	public int sceIdStorageIsReadOnly() {
		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0xB9069BAD, version = 150)
	public int sceIdStorageIsDirty() {
		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x958089DB, version = 150)
	public int sceIdStorageFormat() {
		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0xF4BCB3EE, version = 150)
	public int sceIdStorageUnformat() {
		return 0;
	}

	/**
	 * Retrieves the whole 512 byte container for the key.
	 * 
	 * @param key    idstorage key
	 * @param buffer buffer with at last 512 bytes of storage 
	 * @return       0.
	 */
	@HLEUnimplemented
	@HLEFunction(nid = 0xEB00C509, version = 150)
	public int sceIdStorageReadLeaf(int key, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=512, usage=Usage.out) TPointer buffer) {
		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x1FA4D135, version = 150)
	public int sceIdStorageWriteLeaf() {
		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x08A471A6, version = 150)
	public int sceIdStorageCreateLeaf() {
		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x2C97AB36, version = 150)
	public int sceIdStorageDeleteLeaf() {
		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x99ACCB71, version = 150)
	public int sceIdStorage_driver_99ACCB71() {
		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x37833CB8, version = 150)
	public int sceIdStorage_driver_37833CB8() {
		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x31E08AFB, version = 150)
	public int sceIdStorageEnumId() {
		return 0;
	}

	/**
	 * Retrieves the value associated with a key.
	 * 
	 * @param key     	idstorage key 
	 * @param offset    offset within the 512 byte leaf 
	 * @param buffer    buffer with enough storage
	 * @param length    amount of data to retrieve (offset + length must be <= 512 bytes)
	 * @return
	 */
	@HLEUnimplemented
	@HLEFunction(nid = 0x6FE062D1, version = 150)
	public int sceIdStorageLookup(int key, int offset, @BufferInfo(lengthInfo=LengthInfo.nextParameter, usage=Usage.out) TPointer buffer, int length) {
		buffer.clear(length);
		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x683AAC10, version = 150)
	public int sceIdStorageUpdate() {
		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x3AD32523, version = 150)
	public int sceIdStorageFlush() {
		return 0;
	}
}
