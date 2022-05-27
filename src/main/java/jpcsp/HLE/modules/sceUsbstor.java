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

import jpcsp.HLE.BufferInfo;
import jpcsp.HLE.BufferInfo.LengthInfo;
import jpcsp.HLE.BufferInfo.Usage;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.Modules;
import jpcsp.HLE.PspString;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer8;

import org.apache.log4j.Logger;

public class sceUsbstor extends HLEModule {
	public static Logger log = Modules.getLogger("sceUsbstor");

	@HLEUnimplemented
	@HLEFunction(nid = 0x7B810720, version = 150)
	public int sceUsbstorMsSetWorkBuf(TPointer workBuffer, int workBufferSize) {
		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x9C029B16, version = 150)
	public int  sceUsbstorMs_9C029B16(TPointer buffer, int bufferSize) {
		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x9569F268, version = 150)
	public int sceUsbstorMsSetVSHInfo(PspString version) {
		// E.g. version == "6.60"
		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x576E7F6F, version = 150)
	public int sceUsbstorMsSetProductInfo(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=44, usage=Usage.in) TPointer productInfo) {
		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x4B10A7F5, version = 150)
	public int sceUsbstorMsRegisterEventFlag(int eventFlagUid) {
		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0xFF0C3873, version = 150)
	public int sceUsbstorMsUnregisterEventFlag(int eventFlagUid) {
		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x382898DE, version = 150)
	public int sceUsbstormlnRegisterBuffer(int buffer, int bufferSize) {
		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x25B6F372, version = 150)
	public int sceUsbstormlnUnregisterBuffer(int buffer) {
		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0xDEC0FE8C, version = 150)
	public int sceUsbstormlnWaitStatus() {
		// Has no parameters
		return 0x10;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0xE11DEFDF, version = 150)
	public int sceUsbstormlnCancelWaitStatus() {
		// Has no parameters
		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x60066CFE, version = 150)
	public int sceUsbstorGetStatus() {
		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x56AA41EA, version = 150)
	public int sceUsbstorMs_56AA41EA(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=32, usage=Usage.out) TPointer unknown) {
		unknown.clear(32);
		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x762F7FDF, version = 150)
	public int sceUsbstorMsNotifyEventDone(int unknown1, int unknown2) {
		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0xABE9F2C7, version = 150)
	public int sceUsbstorMsGetApInfo(@BufferInfo(lengthInfo=LengthInfo.variableLength, usage=Usage.out) TPointer apInfo) {
		int length = apInfo.getValue32();
		apInfo.clear(4, length - 4);

		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x1F4AC19C, version = 150)
	public int sceUsbstormlnGetCommand(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=12, usage=Usage.out) TPointer unknown) {
		unknown.clear(12);

		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x5821060D, version = 150)
	public int sceUsbstormlnNotifyResponse(int unknown1, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=3, usage=Usage.in) TPointer8 unknown2) {
		return 0;
	}
}
