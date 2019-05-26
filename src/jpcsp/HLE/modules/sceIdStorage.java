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

import static jpcsp.HLE.Modules.sceChkregModule;
import static jpcsp.hardware.Nand.pageSize;
import static jpcsp.memory.mmio.MMIOHandlerUmd.regionCodes;
import static jpcsp.util.Utilities.endianSwap16;
import static jpcsp.util.Utilities.endianSwap32;

import org.apache.log4j.Logger;

import jpcsp.HLE.BufferInfo;
import jpcsp.HLE.BufferInfo.LengthInfo;
import jpcsp.HLE.BufferInfo.Usage;
import jpcsp.hardware.Model;
import jpcsp.hardware.Wlan;
import jpcsp.util.Utilities;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.kernel.types.SceKernelErrors;

public class sceIdStorage extends HLEModule {
	public static Logger log = Modules.getLogger("sceIdStorage");
    public static final int idStorageKeys[] = {
    		// The first 2 entries have to be 0xFFFF
    		0xFFFF,
    		0xFFFF,
    		// The following entries are the keys used when calling sceIdStorageLookup()
    		0x0004,
    		0x0005,
    		0x0006,
    		0x0007,
    		0x0008,
    		0x0010,
    		0x0011,
    		0x0041,
    		0x0043,
    		0x0044,
    		0x0045,
    		0x0046,
    		0x0047,
    		0x0054,
    		0x0100,
    		0x0101,
    		0x0102,
    		0x0103,
    		0x0104,
    		0x0105,
    		0x0106,
    		0x0120,
    		0x0121,
    		0x0122,
    		0x0123,
    		0x0124,
    		0x0125,
    		0x0126,
    		0x0141
    };

    private static void writeStringType(TPointer buffer, int offset, String s) {
    	buffer.setValue8(offset++, (byte) ((s.length() + 1) * 2)); // number of bytes including terminating 0.
    	buffer.setValue8(offset++, (byte) 3); // Showing a string type?
    	for (int i = 0; i < s.length(); i++) {
    		buffer.setValue8(offset++, (byte) s.charAt(i));
    		buffer.setValue8(offset++, (byte) 0);
    	}
    	// Terminating 0
    	buffer.setValue8(offset++, (byte) 0);
    	buffer.setValue8(offset++, (byte) 0);
    }

	public int hleIdStorageReadLeaf(int key, TPointer buffer) {
		buffer.clear(pageSize);

		switch (key) {
			case 0x0004:
				buffer.setValue32(0, 0x4272796E); // Fixed value "nyrB"
				break;
			case 0x0005:
				buffer.setValue32(0, 0x436C6B67); // Fixed value "gklC"
				break;
			case 0x0006:
				buffer.setValue32(0, 0x4D446472); // Fixed value "rdDM"
				break;
			case 0x0007:
				buffer.setValue32(0, 0x41506144); // Fixed value "DaPA"
				break;
			case 0x0008:
				buffer.setValue32(0, 0x4C434470); // Fixed value "pDCL"
				// Used by display_02g.prx (and later models)
				buffer.setUnalignedValue16(16, 0x000A);
				buffer.setUnalignedValue16(18, 0x0019);
				buffer.setUnalignedValue16(20, 0x0010);
				buffer.setUnalignedValue16(22, 0x000E);
				buffer.setUnalignedValue16(24, 0x000B);
				buffer.setUnalignedValue16(26, 0x0008);
				buffer.setUnalignedValue16(28, 0x0072);
				break;
			case 0x0041:
				// Used by usb.prx
				int offset = 0;
				buffer.setValue32(offset, 0x54C);
				offset += 4;
				writeStringType(buffer, 4, "Sony");
				offset += 64;
	
				final int[] types = { 0x1C8, 0x1C9, 0x1CA, 0x1CB, 0x1CC };
				final String[] typeNames = { "PSP Type A", "PSP Type B", "PSP Type C", "PSP Type D", "PSP Type E" };
	
				buffer.setValue32(offset, types.length);
				offset += 4;
	
				for (int i = 0; i < types.length; i++) {
					buffer.setValue32(offset, types[i]);
					offset += 4;
					writeStringType(buffer, offset, typeNames[i]);
					offset += 64;
				}
				break;
			case 0x0044:
				// Used to display the MAC address in the VSH
				buffer.setArray(0, Wlan.getMacAddress());
				break;
			case 0x0045:
				// Used by thread SceWlanChipInit
				int wlanFirmwareVersion = 0;
				if (Model.getGeneration() > 1) {
					wlanFirmwareVersion = 1;
				}
				buffer.setUnsignedValue16(0, 0x0002 | (wlanFirmwareVersion << 12));
				buffer.setUnsignedValue8(2, 0x01);
				buffer.setUnsignedValue8(3, 0x00);
				buffer.setUnsignedValue8(4, 0x00);
				break;
			case 0x0100:
				// Used by sceChkreg_driver_6894A027()
				// A certificate is stored at offset 0x38
				int certificateOffset = 0x38;
				int certificateLength = 0xB8;
				buffer.clear(certificateOffset, certificateLength);
				int unknownValue = sceChkregModule.getValueReturnedBy6894A027();
				buffer.setValue32(certificateOffset + 0, endianSwap32(1));
				buffer.setUnsignedValue16(certificateOffset + 4, endianSwap16(5));
				buffer.setUnsignedValue16(certificateOffset + 6, endianSwap16(1));
				buffer.setUnsignedValue8(certificateOffset + 8, (0x23 << 2) | ((unknownValue >> 6) & 0x03));
				buffer.setUnsignedValue8(certificateOffset + 9, (unknownValue << 2) & 0xFC);
				break;
			case 0x0102:
				// Used by sceChkregCheckRegion()
				buffer.setValue32(0x08C, 0x60); // Number of region code entries?
				for (int i = 0; i < regionCodes.length; i++) {
					buffer.setValue32(0x0B0 + i * 4, regionCodes[i]);
				}
				break;
			case 0x0141:
				// Used by sceChkuppkg from PSP 6.61 EBOOT.PBP
				return SceKernelErrors.ERROR_NOT_FOUND;
		}

		return 0;
	}

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
	@HLEFunction(nid = 0xEB00C509, version = 150)
	public int sceIdStorageReadLeaf(int key, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=512, usage=Usage.out) TPointer buffer) {
		return hleIdStorageReadLeaf(key, buffer);
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
	@HLEFunction(nid = 0x6FE062D1, version = 150)
	public int sceIdStorageLookup(int key, int offset, @BufferInfo(lengthInfo=LengthInfo.nextParameter, usage=Usage.out) TPointer buffer, int length) {
		TPointer leafBuffer = Utilities.allocatePointer(pageSize);
		int result = hleIdStorageReadLeaf(key, leafBuffer);
		if (result < 0) {
			return result;
		}

		buffer.memcpy(leafBuffer.add(offset), length);

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
