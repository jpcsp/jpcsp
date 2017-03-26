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
package jpcsp.HLE.kernel.types;

public class SceNetIfHandle extends pspAbstractMemoryMappedStructure {
	public int callbackArg4;
	public int upCallbackAddr;
	public int downCallbackAddr;
	public int sendCallbackAddr;
	public int ioctlCallbackAddr;
	public int addrFirstMessageToBeSent;
	public int addrLastMessageToBeSent;
	public int numberOfMessagesToBeSent;
	public int unknown36;
	public int unknown40;
	public int handleInternalAddr;
	public SceNetIfHandleInternal handleInternal;

	public static class SceNetIfHandleInternal extends pspAbstractMemoryMappedStructure {
		public String interfaceName;
		public int ioctlSemaId;
		public int ioctlOKSemaId;
		public pspNetMacAddress macAddress = new pspNetMacAddress();
		public int unknownCallbackAddr188;

		@Override
		protected void read() {
			readUnknown(20); // Offset 0
			interfaceName = readStringNZ(16); // Offset 20
			readUnknown(152); // Offset 36
			unknownCallbackAddr188 = read32(); // Offset 188
			readUnknown(44); // Offset 192
			macAddress = new pspNetMacAddress();
			read(macAddress); // Offset 236
			readUnknown(14); // Offset 242
			ioctlSemaId = read32(); // Offset 256
			ioctlOKSemaId = read32(); // Offset 260
		}

		@Override
		protected void write() {
			writeSkip(20); // Offset 0
			writeStringNZ(16, interfaceName); // Offset 20
			writeSkip(152); // Offset 36
			write32(unknownCallbackAddr188); // Offset 188
			writeSkip(44); // Offset 192
			write(macAddress); // Offset 236
			writeSkip(14); // Offset 242
			write32(ioctlSemaId); // Offset 256
			write32(ioctlOKSemaId); // Offset 260
		}

		@Override
		public int sizeof() {
			return 320;
		}

		@Override
		public String toString() {
			return String.format("interfaceName='%s', unknownCallbackAddr188=0x%08X, ioctlSemaId=0x%X, ioctlOKSemaId=0x%X, macAddress=%s", interfaceName, unknownCallbackAddr188, ioctlSemaId, ioctlOKSemaId, macAddress);
		}
	}

	@Override
	protected void read() {
		handleInternalAddr = read32(); // Offset 0
		if (handleInternalAddr != 0) {
			handleInternal = new SceNetIfHandleInternal();
			handleInternal.read(mem, handleInternalAddr);
		}
		callbackArg4 = read32(); // Offset 4
		upCallbackAddr = read32(); // Offset 8
		downCallbackAddr = read32(); // Offset 12
		sendCallbackAddr = read32(); // Offset 16
		ioctlCallbackAddr = read32(); // Offset 20
		addrFirstMessageToBeSent = read32(); // Offset 24
		addrLastMessageToBeSent = read32(); // Offset 28
		numberOfMessagesToBeSent = read32(); // Offset 32
		unknown36 = read32(); // Offset 36
		unknown40 = read32(); // Offset 40
	}

	@Override
	protected void write() {
		write32(handleInternalAddr); // Offset 0
		if (handleInternalAddr != 0 && handleInternal != null) {
			handleInternal.write(mem, handleInternalAddr);
		}
		write32(callbackArg4); // Offset 4
		write32(upCallbackAddr); // Offset 8
		write32(downCallbackAddr); // Offset 12
		write32(sendCallbackAddr); // Offset 16
		write32(ioctlCallbackAddr); // Offset 20
		write32(addrFirstMessageToBeSent); // Offset 24
		write32(addrLastMessageToBeSent); // Offset 28
		write32(numberOfMessagesToBeSent); // Offset 32
		write32(unknown36); // Offset 36
		write32(unknown40); // Offset 40
	}

	@Override
	public int sizeof() {
		return 44;
	}

	@Override
	public String toString() {
		return String.format("callbackArg4=0x%X, upCallbackAddr=0x%08X, downCallbackAddr=0x%08X, sendCallbackAddr=0x%08X, ioctlCallbackAddr=0x%08X, firstMessage=0x%08X, lastMessage=0x%08X, nbrMessages=0x%X, unknown36=0x%X, unknown40=0x%X, internalStructure: %s", callbackArg4, upCallbackAddr, downCallbackAddr, sendCallbackAddr, ioctlCallbackAddr, addrFirstMessageToBeSent, addrLastMessageToBeSent, numberOfMessagesToBeSent, unknown36, unknown40, handleInternal);
	}
}
