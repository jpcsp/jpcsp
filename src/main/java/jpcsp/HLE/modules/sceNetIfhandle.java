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

import static jpcsp.HLE.kernel.types.SceNetIfMessage.TYPE_SHORT_MESSAGE;
import static jpcsp.util.Utilities.hasFlag;

import java.util.HashMap;

import jpcsp.Memory;
import jpcsp.Allegrex.compiler.RuntimeContext;
import jpcsp.HLE.BufferInfo;
import jpcsp.HLE.BufferInfo.LengthInfo;
import jpcsp.HLE.BufferInfo.Usage;
import jpcsp.HLE.CanBeNull;
import jpcsp.HLE.CheckArgument;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.PspString;
import jpcsp.HLE.SceKernelErrorException;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer32;
import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointer8;
import jpcsp.HLE.kernel.Managers;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.kernel.types.SceKernelVplInfo;
import jpcsp.HLE.kernel.types.SceNetIfHandle;
import jpcsp.HLE.kernel.types.SceNetIfHandle.SceNetIfHandleInternal;
import jpcsp.HLE.kernel.types.SceNetIfMessage;
import jpcsp.HLE.kernel.types.pspNetMacAddress;
import jpcsp.HLE.modules.SysMemUserForUser.SysMemInfo;

import org.apache.log4j.Logger;

public class sceNetIfhandle extends HLEModule {
    public static Logger log = Modules.getLogger("sceNetIfhandle");
    protected HashMap<Integer, SysMemInfo> allocatedMemory;
    protected int unknownCallback1;
    protected int unknownCallback2;
    protected int unknownCallback3;
    protected boolean callbacksDefined;
    protected TPointer[] handles;
    protected int unknownValue1;
    protected int unknownValue2;
    protected int unknownValue3;
    protected int unknownValue4;

	@Override
	public void start() {
		allocatedMemory = new HashMap<Integer, SysMemUserForUser.SysMemInfo>();
		unknownCallback1 = 0;
		unknownCallback2 = 0;
		unknownCallback3 = 0;
		callbacksDefined = false;
		handles = new TPointer[8];
		for (int i = 0; i < handles.length; i++) {
			handles[i] = TPointer.NULL;
		}

		super.start();
	}

	public TPointer checkHandleAddr(TPointer handleAddr) {
		for (int i = 0; i < handles.length; i++) {
			if (handles[i].getAddress() == handleAddr.getAddress()) {
				return handleAddr;
			}
		}

		throw new SceKernelErrorException(SceKernelErrors.ERROR_NOT_FOUND);
	}

	public TPointer checkHandleInternalAddr(TPointer handleInternalAddr) {
		for (int i = 0; i < handles.length; i++) {
			if (handles[i].getValue32() == handleInternalAddr.getAddress()) {
				return handleInternalAddr;
			}
		}

		throw new SceKernelErrorException(SceKernelErrors.ERROR_NOT_FOUND);
	}

	protected int hleNetCreateIfhandleEther(TPointer handleAddr) {
    	int handleIndex = -1;
    	for (int i = 0; i < handles.length; i++) {
    		if (handles[i].isNull()) {
    			handleIndex = i;
    			break;
    		}
    	}

    	if (handleIndex < 0) {
    		return SceKernelErrors.ERROR_OUT_OF_MEMORY;
    	}

    	SceNetIfHandleInternal handleInternal = new SceNetIfHandleInternal();
    	int allocatedMem = hleNetMallocInternal(handleInternal.sizeof());
    	if (allocatedMem < 0) {
    		return SceKernelErrors.ERROR_OUT_OF_MEMORY;
    	}
    	handleAddr.setValue32(0, allocatedMem);
    	handles[handleIndex] = handleAddr;

    	handleInternal.write(new TPointer(Memory.getInstance(), allocatedMem));

    	RuntimeContext.debugMemory(allocatedMem, handleInternal.sizeof());

    	return 0;
	}

    protected int hleNetAttachIfhandleEther(TPointer handleAddr, pspNetMacAddress macAddress, String interfaceName) {
    	SceNetIfHandle handle = new SceNetIfHandle();
    	handle.read(handleAddr);
    	handle.handleInternal.macAddress = macAddress;
    	handle.handleInternal.interfaceName = interfaceName;
    	handle.addrFirstMessageToBeSent = 0;
    	handle.addrLastMessageToBeSent = 0;
    	handle.numberOfMessagesToBeSent = 0;
    	handle.unknown36 = 0;
    	handle.unknown40 = 0;
    	handle.write(handleAddr);

    	return 0;
    }

    public int hleNetMallocInternal(int size) {
    	int allocatedAddr;
    	// When flash0:/kd/ifhandle.prx is in use, allocate the memory through this
    	// implementation instead of using the HLE implementation.
    	// ifhandle.prx is creating a VPL name "SceNet" and allocating from it.
    	SceKernelVplInfo vplInfo = Managers.vpl.getVplInfoByName("SceNet");
    	if (vplInfo != null) {
    		allocatedAddr = Managers.vpl.tryAllocateVpl(vplInfo, size);
    	} else {
    		allocatedAddr = sceNetMallocInternal(size);
    	}

    	return allocatedAddr;
    }

    @HLEFunction(nid = 0xC80181A2, version = 150, checkInsideInterrupt = true)
    public int sceNetGetDropRate(@CanBeNull TPointer32 dropRateAddr, @CanBeNull TPointer32 dropDurationAddr) {
    	return Modules.sceWlanModule.sceWlanGetDropRate(dropRateAddr, dropDurationAddr);
    }

    @HLEFunction(nid = 0xFD8585E1, version = 150, checkInsideInterrupt = true)
    public int sceNetSetDropRate(int dropRate, int dropDuration) {
    	return Modules.sceWlanModule.sceWlanSetDropRate(dropRate, dropDuration);
    }

    @HLEFunction(nid = 0x15CFE3C0, version = 150)
    public int sceNetMallocInternal(int size) {
    	SysMemInfo info = Modules.SysMemUserForUserModule.malloc(SysMemUserForUser.USER_PARTITION_ID, "sceNetMallocInternal", SysMemUserForUser.PSP_SMEM_Low, size, 0);

    	if (info == null) {
    		return 0;
    	}

    	allocatedMemory.put(info.addr, info);

    	return info.addr;
    }

    @HLEFunction(nid = 0x76BAD213, version = 150)
    public int sceNetFreeInternal(int memory) {
    	SysMemInfo info = allocatedMemory.remove(memory);
    	if (info != null) {
    		Modules.SysMemUserForUserModule.free(info);
    	}

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x0542835F, version = 150)
    public int sceNetIfhandle_driver_0542835F(int unknownCallback1, int unknownCallback2, int unknownCallback3) {
    	this.unknownCallback1 = unknownCallback1;
    	this.unknownCallback2 = unknownCallback2;
    	this.unknownCallback3 = unknownCallback3;
    	callbacksDefined = true;

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x773FC77C, version = 150)
    public int sceNetIfhandle_driver_773FC77C() {
    	// Has no parameters
    	unknownCallback1 = 0;
    	unknownCallback2 = 0;
    	unknownCallback3 = 0;
    	callbacksDefined = false;

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x9CBA24D4, version = 150)
    public int sceNetIfhandle_driver_9CBA24D4(PspString interfaceName) {
    	int handleAddr = 0;
    	for (int i = 0; i < handles.length; i++) {
    		if (handles[i].isNotNull()) {
	    		SceNetIfHandle handle = new SceNetIfHandle();
	    		handle.read(handles[i]);
	    		if (interfaceName.equals(handle.handleInternal.interfaceName)) {
	    			handleAddr = handles[i].getAddress();
	    			break;
	    		}
    		}
    	}

    	return handleAddr;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xC5623112, version = 150)
    public int sceNetIfhandle_driver_C5623112(@CheckArgument(value="checkHandleAddr") @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=44, usage=Usage.in) TPointer handleAddr, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=6, usage=Usage.out) TPointer8 macAddress) {
    	SceNetIfHandle handle = new SceNetIfHandle();
    	handle.read(handleAddr);
    	handle.handleInternal.macAddress.write(macAddress);

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x16042084, version = 150)
    public int sceNetCreateIfhandleEther(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=44, usage=Usage.out) TPointer handleAddr) {
    	return hleNetCreateIfhandleEther(handleAddr);
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xAE81C0CB, version = 150)
    public int sceNetAttachIfhandleEther(@CheckArgument("checkHandleAddr") TPointer handleAddr, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=6, usage=Usage.in) TPointer8 macAddress, PspString interfaceName) {
    	pspNetMacAddress netMacAddress = new pspNetMacAddress();
    	netMacAddress.read(macAddress);

    	return hleNetAttachIfhandleEther(handleAddr, netMacAddress, interfaceName.getString());
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x07505747, version = 150)
    public int sceNetIfhandle_07505747(int unknown) {
    	return 0; // Current thread delay in ms (used only for some gameIds)
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x0B258B5E, version = 150)
    public int sceNetIfhandle_0B258B5E() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x0C391E9F, version = 150)
    public int sceNetIfhandle_0C391E9F(int partitionId, int memorySize) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x0FB8AE0D, version = 150)
    public void sceNetIfhandle_0FB8AE0D() {
    	// Has no parameters
    	unknownValue1 = 0;
    	unknownValue2 = 0;
    	unknownValue3 = 0;
    	unknownValue4 = 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x29ED84C5, version = 150)
    public int sceNetIfhandle_29ED84C5() {
    	// Has no parameters
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x35FAB6A2, version = 150)
    public void sceNetIfhandle_35FAB6A2() {
    	// Has no parameters
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x5FB31C72, version = 150)
    public int sceNetIfhandle_5FB31C72(@CanBeNull @BufferInfo(usage=Usage.out) TPointer32 unknownValue1Addr, @CanBeNull @BufferInfo(usage=Usage.out) TPointer32 unknownValue2Addr, @CanBeNull @BufferInfo(usage=Usage.out) TPointer32 unknownValue3Addr, @CanBeNull @BufferInfo(usage=Usage.out) TPointer32 unknownValue4Addr) {
    	unknownValue1Addr.setValue(unknownValue1);
    	unknownValue2Addr.setValue(unknownValue2);
    	unknownValue3Addr.setValue(unknownValue3);
    	unknownValue4Addr.setValue(unknownValue4);

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x62B20015, version = 150)
    public int sceNetIfhandle_62B20015(int unknownValue1, int unknownValue2, int unknownValue3, int unknownValue4) {
    	this.unknownValue1 = unknownValue1;
    	this.unknownValue2 = unknownValue2;
    	this.unknownValue3 = unknownValue3;
    	this.unknownValue4 = unknownValue4;

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x955F2924, version = 150)
    public int sceNetIfhandle_955F2924() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xE9BF5332, version = 150)
    public int sceNetIfhandle_E9BF5332() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x0296C7D6, version = 150)
    public void sceNetIfhandleIfIoctl(@CanBeNull @CheckArgument("checkHandleInternalAddr") @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=320, usage=Usage.inout) TPointer handleInternalAddr, int cmd, @CanBeNull @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=32, usage=Usage.in) TPointer unknown) {
    	if (log.isDebugEnabled()) {
    		String interfaceName = unknown.getStringNZ(16);
    		int flags = unknown.getValue16(16);
    		log.debug(String.format("sceNetIfhandleIfIoctl interfaceName='%s' flags=0x%X", interfaceName, flags));
    	}
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x1560F143, version = 150)
    public int sceNetMCopyback(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=76, usage=Usage.in) TPointer messageAddr, int dataOffset, int length, @CanBeNull @BufferInfo(lengthInfo=LengthInfo.previousParameter, usage=Usage.in) TPointer sourceAddr) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x16246B99, version = 150)
    public int sceNetIfPrepend() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x2162EE67, version = 150)
    public int sceNetIfhandlePollSema() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x263767F6, version = 150)
    public int sceNetFlagIfEvent(@CanBeNull TPointer handleAddr, int unknown1, int unknown2) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x30602CE9, version = 150)
    public int sceNetIfhandleSignalSema() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x30F69334, version = 150)
    public int sceNetIfhandleInit(int eventFlagId) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x3E8DD3F8, version = 150)
    public int sceNetMCat() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x456E3146, version = 150)
    public int sceNetMCopym() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x49EDBB18, version = 150)
    public int sceNetMPullup() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x4C2886CB, version = 150)
    public int sceNetGetMallocStatInternal() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x4CF15C43, version = 150)
    public int sceNetMGethdr() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x4FB43BCE, version = 150)
    public int sceNetIfhandleGetDetachEther() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x54D1AEA1, version = 150)
    public int sceNetDetachIfhandleEther() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x59F0D619, version = 150)
    public int sceNetMGetclr() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x6AB53C27, version = 150)
    public int sceNetMDup() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x8FCB05A1, version = 150)
    public int sceNetIfhandleIfUp(@CheckArgument("checkHandleInternalAddr") @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=320, usage=Usage.inout) TPointer handleInternalAddr) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x9A6261EC, version = 150)
    public int sceNetMCopydata(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=76, usage=Usage.in) TPointer messageAddr, int dataOffset, int length, @CanBeNull @BufferInfo(lengthInfo=LengthInfo.previousParameter, usage=Usage.out) TPointer destinationAddr) {
    	if (destinationAddr.isNotNull()) {
        	SceNetIfMessage message = new SceNetIfMessage();
    		while (messageAddr.isNotNull()) {
            	message.read(messageAddr);

            	if (dataOffset < message.dataLength) {
            		break;
            	}
        		dataOffset -= message.dataLength;
        		messageAddr.setAddress(message.nextDataAddr);
    		}

    		while (length > 0 && messageAddr.isNotNull()) {
    			message.read(messageAddr);
    			int copyLength = Math.min(length, message.dataLength - dataOffset);
            	destinationAddr.memcpy(message.dataAddr + dataOffset, copyLength);
            	length -= copyLength;
            	destinationAddr.add(copyLength);
            	dataOffset = 0;
    		}
    	}

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xA493AA5F, version = 150)
    public int sceNetMGet(int unknown1, int unknown2) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xB1F5BB87, version = 150)
    public void sceNetIfhandleIfStart(@CanBeNull TPointer handleInternalAddr, @CanBeNull SceNetIfMessage messageToBeSent) {
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xB8188F96, version = 150)
    public int sceNetIfhandleGetAttachEther(@BufferInfo(usage=Usage.out) TPointer32 handleInternalAddrAddr, @BufferInfo(usage=Usage.out) TPointer32 unknown) {
    	// returns the address of handleInternal into handleInternalAddrAddr
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xB9096E48, version = 150)
    public int sceNetIfhandleTerm() {
    	// Has no parameters
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xBFF3CEA5, version = 150)
    public void sceNetMAdj(@CanBeNull TPointer messageAddr, int sizeAdj) {
    	if (messageAddr.isNull()) {
    		return;
    	}

    	SceNetIfMessage message = new SceNetIfMessage();

    	if (sizeAdj < 0) {
    		sizeAdj = -sizeAdj;
    		int totalSize = 0;
	    	int currentMessageAddr = messageAddr.getAddress();
    		do {
    			message.read(messageAddr.getMemory(), currentMessageAddr);
    			totalSize += message.dataLength;
    			currentMessageAddr = message.nextDataAddr;
    		} while (currentMessageAddr != 0);

    		if (message.dataLength < sizeAdj) {
    			totalSize -= sizeAdj;
    			message.read(messageAddr);
    			totalSize = Math.max(totalSize - sizeAdj, 0);
    	    	if (hasFlag(message.type, TYPE_SHORT_MESSAGE)) {
    	    		message.totalDataLength = totalSize;
    	    		message.write(messageAddr);
    	    	}

    	    	currentMessageAddr = messageAddr.getAddress();
    	    	while (currentMessageAddr != 0) {
    	    		message.read(messageAddr.getMemory(), currentMessageAddr);
    	    		if (message.dataLength < totalSize) {
    	    			currentMessageAddr = message.nextDataAddr;
    	    			totalSize -= message.dataLength;
    	    		} else {
    	    			message.dataLength = totalSize;
    	    			message.write(messageAddr.getMemory(), currentMessageAddr);
    	    			break;
    	    		}
    	    	}

    	    	currentMessageAddr = message.nextDataAddr;
    	    	while (currentMessageAddr != 0) {
    	    		message.read(messageAddr.getMemory(), currentMessageAddr);
    	    		message.dataLength = 0;
    	    		message.write(messageAddr.getMemory(), currentMessageAddr);
    	    		currentMessageAddr = message.nextDataAddr;
    	    	}
    		} else {
    	    	message.read(messageAddr);
    	    	if (hasFlag(message.type, TYPE_SHORT_MESSAGE)) {
    	    		message.totalDataLength -= sizeAdj;
    	    		message.write(messageAddr);
    	    	}
    		}
    	} else {
	    	int totalSizeAdj = sizeAdj;
	    	int currentMessageAddr = messageAddr.getAddress();
	    	do {
	    		message.read(messageAddr.getMemory(), currentMessageAddr);
		    	if (sizeAdj < message.dataLength) {
		    		message.dataLength -= sizeAdj;
		    		message.dataAddr += sizeAdj;
		    		message.write(messageAddr);
		    		sizeAdj = 0;
		    	} else {
		    		sizeAdj -= message.dataLength;
		    		message.dataLength = 0;
		    		message.write(messageAddr);
		    		currentMessageAddr = message.nextDataAddr;
		    	}
	    	} while (messageAddr.isNotNull() && sizeAdj > 0);

	    	message.read(messageAddr);
	    	if (hasFlag(message.type, TYPE_SHORT_MESSAGE)) {
	    		message.totalDataLength -= totalSizeAdj - sizeAdj;
	    		message.write(messageAddr);
	    	}
    	}
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xC28F6FF2, version = 150)
    public int sceNetIfEnqueue(@CanBeNull TPointer handleAddr, @BufferInfo(lengthInfo = LengthInfo.fixedLength, length = 48, usage = Usage.in) TPointer messageAddr) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xC3325FDC, version = 150)
    public int sceNetMPrepend() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xC6D14282, version = 150)
    public int sceNetIfhandle_driver_C6D14282(TPointer handleAddr, int callbackArg4) {
    	SceNetIfHandle handle = new SceNetIfHandle();
    	handle.read(handleAddr);

    	handle.callbackArg4 = callbackArg4;
    	handle.write(handleAddr);

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xC9344A59, version = 150)
    public int sceNetDestroyIfhandleEther() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xD5AD6DEA, version = 150)
    public int sceNetIfhandle_driver_D5AD6DEA(@CanBeNull TPointer handleAddr, int dummyDataAddr, int dummyDataLength) {
    	if (handleAddr.isNull()) {
    		return 0;
    	}
    	SceNetIfHandle handle = new SceNetIfHandle();
    	handle.read(handleAddr);

    	return handle.callbackArg4;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xD5DA7B3C, version = 150)
    public int sceNetIfhandleWaitSema() {
    	return 0;
    }

    @HLEFunction(nid = 0xE2F4F1C9, version = 150)
    public int sceNetIfDequeue(@CanBeNull TPointer handleAddr) {
    	if (handleAddr.isNull()) {
    		return 0;
    	}

    	SceNetIfHandle handle = new SceNetIfHandle();
		handle.read(handleAddr);

    	Memory mem = handleAddr.getMemory();
    	TPointer firstMessageAddr = new TPointer(mem, handle.addrFirstMessageToBeSent);
    	SceNetIfMessage message = new SceNetIfMessage();
    	message.read(firstMessageAddr);

    	// Unlink the message from the handle
    	handle.addrFirstMessageToBeSent = message.nextMessageAddr;
    	handle.numberOfMessagesToBeSent--;
    	if (handle.addrFirstMessageToBeSent == 0) {
    		handle.addrLastMessageToBeSent = 0;
    	}
    	handle.write(handleAddr);

    	return firstMessageAddr.getAddress();
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xE440A7D8, version = 150)
    public int sceNetIfhandleIfDequeue() {
    	// Has no parameters
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xE80F00A4, version = 150)
    public int sceNetMPulldown() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xEAD3A759, version = 150)
    public int sceNetIfhandleIfDown() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xF56FAC82, version = 150)
    public int sceNetMFreem(@CanBeNull TPointer messageAddr) {
		SceNetIfMessage message = new SceNetIfMessage();
    	while (messageAddr.isNotNull()) {
    		message.read(messageAddr);
    		int nextMessage = message.nextDataAddr;
    		sceNetFreeInternal(messageAddr.getAddress());
    		messageAddr = new TPointer(messageAddr.getMemory(), nextMessage);
    	}

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xF8825DC4, version = 150)
    public int sceNetMFree() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xF94BAF52, version = 150)
    public int sceNetSendIfEvent(TPointer handleAddr) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x9173FD47, version = 150)
    public int sceNetIfhandle_9173FD47() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xB40A882F, version = 150)
    public int sceNetIfhandle_B40A882F() {
    	return 0;
    }
}