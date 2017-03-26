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

import static jpcsp.HLE.Modules.sceNetIfhandleModule;
import jpcsp.Memory;
import jpcsp.NIDMapper;
import jpcsp.Allegrex.compiler.RuntimeContext;
import jpcsp.HLE.BufferInfo;
import jpcsp.HLE.BufferInfo.LengthInfo;
import jpcsp.HLE.BufferInfo.Usage;
import jpcsp.HLE.CanBeNull;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer32;
import jpcsp.HLE.kernel.types.IAction;
import jpcsp.HLE.kernel.types.SceKernelThreadInfo;
import jpcsp.HLE.kernel.types.SceNetIfHandle;
import jpcsp.HLE.kernel.types.SceNetIfMessage;
import jpcsp.HLE.kernel.types.SceNetWlanMessage;
import jpcsp.HLE.kernel.types.pspNetMacAddress;
import jpcsp.HLE.Modules;
import jpcsp.hardware.Wlan;
import jpcsp.util.Utilities;

import org.apache.log4j.Logger;

public class sceWlan extends HLEModule {
    public static Logger log = Modules.getLogger("sceWlan");

    public int hleWlanSendCallback(TPointer handleAddr) {
    	SceNetIfHandle handle = new SceNetIfHandle();
    	handle.read(handleAddr);

    	Memory mem = handleAddr.getMemory();
    	TPointer firstMessageAddr = new TPointer(mem, handle.addrFirstMessageToBeSent);
    	SceNetIfMessage message = new SceNetIfMessage();
    	message.read(firstMessageAddr);
    	SceNetWlanMessage wlanMessage = new SceNetWlanMessage();
    	wlanMessage.read(mem, message.dataAddr);
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("hleWlanSendCallback handleAddr=%s: %s", handleAddr, handle));
    		log.debug(String.format("hleWlanSendCallback message: %s: %s", message, Utilities.getMemoryDump(firstMessageAddr.getAddress(), message.sizeof())));
    		log.debug(String.format("hleWlanSendCallback WLAN message : %s", wlanMessage));
    		log.debug(String.format("hleWlanSendCallback message data: %s", Utilities.getMemoryDump(message.dataAddr + wlanMessage.sizeof(), message.dataLength - wlanMessage.sizeof())));
    	}

    	// Unlink the message
		TPointer nextMessageAddr = new TPointer(mem, message.nextMessageAddr);
    	handle.addrFirstMessageToBeSent = nextMessageAddr.getAddress();
    	handle.numberOfMessagesToBeSent--;
    	if (nextMessageAddr.isNull()) {
    		handle.addrLastMessageToBeSent = 0;
    	} else {
    		SceNetIfMessage nextMessage = new SceNetIfMessage();
    		nextMessage.read(nextMessageAddr);
    		nextMessage.previousMessageAddr = 0;
    		nextMessage.write(nextMessageAddr);
    	}
    	handle.write(handleAddr);

    	return 0;
    }

    public int hleWlanUpCallback(TPointer handleAddr) {
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("hleWlanUpCallback handleAddr: %s", Utilities.getMemoryDump(handleAddr.getAddress(), 44)));
    		int handleInternalAddr = handleAddr.getValue32();
    		if (handleInternalAddr != 0) {
        		log.debug(String.format("afterNetCreateIfhandleEtherAction handleInternalAddr: %s", Utilities.getMemoryDump(handleInternalAddr, 320)));
    		}
    	}

    	SceNetIfHandle handle = new SceNetIfHandle();
    	handle.read(handleAddr);

    	if (log.isDebugEnabled()) {
    		log.debug(String.format("hleWlanUpCallback handleAddr=%s: %s", handleAddr, handle));
    	}

    	Modules.ThreadManForUserModule.sceKernelSignalSema(handle.handleInternal.ioctlSemaId, 1);

    	return 0;
    }

    public int hleWlanIoctlCallback(TPointer handleAddr, int cmd, TPointer unknown1, TPointer32 unknown2) {
    	SceNetIfHandle handle = new SceNetIfHandle();
    	handle.read(handleAddr);

    	if (log.isDebugEnabled()) {
    		int length1 = 0x80;
    		int length2 = 0x80;
    		switch (cmd) {
    			case 0x34:
    				length1 = 0x4C;
    				length2 = 0x600;
    				break;
    			case 0x35:
    				length1 = 0x70;
    				break;
    			case 0x37:
    				length1 = 0x60;
    				break;
    		}
    		log.debug(String.format("hleWlanIoctlCallback handleAddr=%s: %s", handleAddr, handle));
    		int addr1 = unknown2.getValue(0);
    		if (addr1 != 0 && Memory.isAddressGood(addr1) && length1 > 0) {
    			log.debug(String.format("hleWlanIoctlCallback addr1: %s", Utilities.getMemoryDump(addr1, length1)));
    			RuntimeContext.debugMemory(addr1, length1);
    		}
    		int addr2 = unknown2.getValue(4);
    		if (addr2 != 0 && Memory.isAddressGood(addr2) && length2 > 0) {
    			log.debug(String.format("hleWlanIoctlCallback addr2: %s", Utilities.getMemoryDump(addr2, length2)));
    			RuntimeContext.debugMemory(addr2, length2);
    		}
    	}

    	Modules.ThreadManForUserModule.sceKernelSignalSema(handle.handleInternal.ioctlSemaId, 1);

    	return 0;
    }

    private class AfterNetCreateIfhandleEtherAction implements IAction {
    	private SceKernelThreadInfo thread;
    	private TPointer handleAddr;

		public AfterNetCreateIfhandleEtherAction(SceKernelThreadInfo thread, TPointer handleAddr) {
			this.thread = thread;
			this.handleAddr = handleAddr;
		}

		@Override
		public void execute() {
			afterNetCreateIfhandleEtherAction(thread, handleAddr);
		}
    }

    private void afterNetCreateIfhandleEtherAction(SceKernelThreadInfo thread, TPointer handleAddr) {
    	int tempMem = sceNetIfhandleModule.sceNetMallocInternal(32);
    	if (tempMem <= 0) {
    		return;
    	}

    	int macAddressAddr = tempMem;
    	int interfaceNameAddr = tempMem + 8;

    	pspNetMacAddress macAddress = new pspNetMacAddress(Wlan.getMacAddress());
    	macAddress.write(handleAddr.getMemory(), macAddressAddr);

    	Utilities.writeStringZ(handleAddr.getMemory(), interfaceNameAddr, "wlan");

    	int sceNetAttachIfhandleEther = NIDMapper.getInstance().overwrittenNidToAddress(0xAE81C0CB);
    	if (sceNetAttachIfhandleEther == 0) {
    		return;
    	}

    	Modules.ThreadManForUserModule.executeCallback(thread, sceNetAttachIfhandleEther, null, true, handleAddr.getAddress(), macAddressAddr, interfaceNameAddr);
    }

    private int createWlanInterface() {
		SceNetIfHandle handle = new SceNetIfHandle();
		handle.callbackArg4 = 0x11040404; // dummy callback addr
		handle.upCallbackAddr = ThreadManForUser.WLAN_UP_CALLBACK_ADDRESS;
		handle.callbackAddr12 = 0x110C0C0C; // dummy callback addr
		handle.sendCallbackAddr = ThreadManForUser.WLAN_SEND_CALLBACK_ADDRESS;
		handle.ioctlCallbackAddr = ThreadManForUser.WLAN_IOCTL_CALLBACK_ADDRESS;
		int handleMem = sceNetIfhandleModule.sceNetMallocInternal(handle.sizeof());
		if (handleMem < 0) {
			return handleMem;
		}
		TPointer handleAddr = new TPointer(Memory.getInstance(), handleMem);
		handle.write(handleAddr);
		RuntimeContext.debugMemory(handleAddr.getAddress(), handle.sizeof());

		int sceNetCreateIfhandleEther = NIDMapper.getInstance().overwrittenNidToAddress(0x16042084);
		if (sceNetCreateIfhandleEther == 0) {
			int result = sceNetIfhandleModule.hleNetCreateIfhandleEther(handleAddr);
			if (result < 0) {
				return result;
			}

			result = sceNetIfhandleModule.hleNetAttachIfhandleEther(handleAddr, new pspNetMacAddress(Wlan.getMacAddress()), "wlan");
			if (result < 0) {
				return result;
			}
		} else {
			SceKernelThreadInfo thread = Modules.ThreadManForUserModule.getCurrentThread();
			Modules.ThreadManForUserModule.executeCallback(thread, sceNetCreateIfhandleEther, new AfterNetCreateIfhandleEtherAction(thread, handleAddr), false, handleAddr.getAddress());
		}

		return 0;
	}

	/**
     * Get the Ethernet Address of the wlan controller
     *
     * @param etherAddr - pointer to a buffer of u8 (NOTE: it only writes to 6 bytes, but
     * requests 8 so pass it 8 bytes just in case)
     * @return 0 on success, < 0 on error
     */
    @HLEFunction(nid = 0x0C622081, version = 150, checkInsideInterrupt = true)
    public int sceWlanGetEtherAddr(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=6, usage=Usage.out) TPointer etherAddr) {
    	pspNetMacAddress macAddress = new pspNetMacAddress();
    	macAddress.setMacAddress(Wlan.getMacAddress());
    	macAddress.write(etherAddr);

    	if (log.isDebugEnabled()) {
    		log.debug(String.format("sceWlanGetEtherAddr returning %s", macAddress));
    	}

    	return 0;
    }

    /**
     * Determine the state of the Wlan power switch
     *
     * @return 0 if off, 1 if on
     */
    @HLEFunction(nid = 0xD7763699, version = 150)
    public int sceWlanGetSwitchState() {
        return Wlan.getSwitchState();
    }

    /**
     * Determine if the wlan device is currently powered on
     *
     * @return 0 if off, 1 if on
     */
    @HLEFunction(nid = 0x93440B11, version = 150)
    public int sceWlanDevIsPowerOn() {
        return Wlan.getSwitchState();
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x482CAE9A, version = 150)
    public int sceWlanDevAttach() {
    	// Has no parameters
    	int result = createWlanInterface();
    	if (result < 0) {
			log.error(String.format("Cannot create the WLAN Interface: 0x%08X", result));
			return result;
		}

        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xC9A8CAB7, version = 150)
    public int sceWlanDevDetach() {
    	// Has no parameters
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x8D5F551B, version = 150)
    public int sceWlanDrv_8D5F551B() {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x749B813A, version = 150)
    public int sceWlanSetHostDiscover(int unknown1, @CanBeNull @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=40, usage=Usage.in) TPointer unknown2) {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xFE8A0B46, version = 150)
    public int sceWlanSetWakeUp(int unknown1, @CanBeNull @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=40, usage=Usage.in) TPointer unknown2) {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x5E7C8D94, version = 150)
    public int sceWlanDevIsGameMode() {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x5ED4049A, version = 150)
    public int sceWlanGPPrevEstablishActive() {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xA447103A, version = 150)
    public int sceWlanGPRecv() {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xB4D7CB74, version = 150)
    public int sceWlanGPSend() {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x2D0FAE4E, version = 150)
    public int sceWlanDrv_lib_2D0FAE4E() {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x56F467CA, version = 150)
    public int sceWlanDrv_lib_56F467CA() {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x5BAA1FE5, version = 150)
    public int sceWlanDrv_lib_5BAA1FE5() {
        return 0;
    }
}