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
import static jpcsp.HLE.kernel.managers.SceUidManager.INVALID_ID;
import static jpcsp.HLE.kernel.types.SceNetWlanMessage.WLAN_PROTOCOL_SUBTYPE_CONTROL;
import static jpcsp.HLE.kernel.types.SceNetWlanMessage.WLAN_PROTOCOL_SUBTYPE_DATA;
import static jpcsp.HLE.kernel.types.SceNetWlanMessage.WLAN_PROTOCOL_TYPE_SONY;
import static jpcsp.HLE.modules.SysMemUserForUser.KERNEL_PARTITION_ID;

import java.io.IOException;
import java.net.BindException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

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
import jpcsp.HLE.kernel.Managers;
import jpcsp.HLE.kernel.types.IAction;
import jpcsp.HLE.kernel.types.SceKernelThreadInfo;
import jpcsp.HLE.kernel.types.SceKernelVplInfo;
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
    private static int wlanSocketPort = 30010;
    private static final int wlanThreadPollingDelayUs = 10000;
    static private final byte[] dummyOtherMacAddress = new byte[] { 0x10,  0x22, 0x33, 0x44, 0x55, 0x66 };
    private String joinedSSID;
    private int dummyMessageStep;
    private TPointer dummyMessageHandleAddr;
    private DatagramSocket wlanSocket;
    private TPointer wlanHandleAddr;
    private int wlanThreadUid;

	@Override
	public void start() {
		wlanThreadUid = INVALID_ID;
		dummyMessageStep = -1;

		super.start();
	}

    public void hleWlanThread() {
    	if (log.isTraceEnabled()) {
    		log.trace(String.format("hleWlanThread"));
    	}

    	if (wlanThreadMustExit()) {
    		if (log.isDebugEnabled()) {
    			log.debug(String.format("Exiting hleWlanThread %s", Modules.ThreadManForUserModule.getCurrentThread()));
    		}
    		Modules.ThreadManForUserModule.hleKernelExitDeleteThread(0);
    		return;
    	}

    	while (!wlanThreadMustExit() && hleWlanReceiveMessage()) {
    		// Receive all available messages
    	}

    	if (dummyMessageStep > 0) {
    		sendDummyMessage(dummyMessageStep, dummyMessageHandleAddr);
    		dummyMessageStep = 0;
    	}

    	Modules.ThreadManForUserModule.hleKernelDelayThread(wlanThreadPollingDelayUs, true);
    }

    private boolean wlanThreadMustExit() {
    	return wlanThreadUid != Modules.ThreadManForUserModule.getCurrentThreadID();
    }

    private boolean hleWlanReceiveMessage() {
    	boolean packetReceived = false;

    	if (!createWlanSocket()) {
    		return packetReceived;
    	}

    	byte[] bytes = new byte[10000];
		DatagramPacket packet = new DatagramPacket(bytes, bytes.length);
		try {
			wlanSocket.receive(packet);
			if (log.isDebugEnabled()) {
				log.debug(String.format("hleWlanReceiveMessage message: %s", Utilities.getMemoryDump(packet.getData(), packet.getOffset(), packet.getLength())));
			}

			packetReceived = true;

	    	int dataLength = packet.getLength();
	    	SceNetIfMessage message = new SceNetIfMessage();
	    	final int size = message.sizeof() + dataLength;
	    	int allocatedAddr;
	    	SceKernelVplInfo vplInfo = Managers.vpl.getVplInfoByName("SceNet");
	    	if (vplInfo != null) {
	    		allocatedAddr = Managers.vpl.tryAllocateVpl(vplInfo, size);
	    	} else {
	    		allocatedAddr = Modules.sceNetIfhandleModule.sceNetMallocInternal(size);
	    	}

	    	if (allocatedAddr > 0) {
	    		Memory mem = Memory.getInstance();
		    	mem.memset(allocatedAddr, (byte) 0, size);
		    	RuntimeContext.debugMemory(allocatedAddr, size);

		    	TPointer messageAddr = new TPointer(mem, allocatedAddr);
		    	TPointer data = new TPointer(mem, messageAddr.getAddress() + message.sizeof());

		    	// Write the received bytes to memory
		    	Utilities.writeBytes(data.getAddress(), dataLength, packet.getData(), packet.getOffset());

		    	// Write the message header
		    	message.dataAddr = data.getAddress();
				message.dataLength = dataLength;
				message.unknown16 = 1;
				message.unknown18 = 2;
				message.unknown24 = dataLength;
				message.write(messageAddr);

				if (dataLength > 0) {
					if (log.isDebugEnabled()) {
				    	SceNetWlanMessage wlanMessage = new SceNetWlanMessage();
				    	wlanMessage.read(data);

						log.debug(String.format("Notifying received message: %s", message));
						log.debug(String.format("Message WLAN: %s", wlanMessage));
						log.debug(String.format("Message data: %s", Utilities.getMemoryDump(data.getAddress(), dataLength)));
					}

					int sceNetIfEnqueue = NIDMapper.getInstance().getAddressByName("sceNetIfEnqueue");
					if (sceNetIfEnqueue != 0) {
						SceKernelThreadInfo thread = Modules.ThreadManForUserModule.getCurrentThread();
						Modules.ThreadManForUserModule.executeCallback(thread, sceNetIfEnqueue, null, true, wlanHandleAddr.getAddress(), messageAddr.getAddress());
					}
				}
	    	}
		} catch (SocketTimeoutException e) {
			// Timeout can be ignored as we are polling
		} catch (IOException e) {
			log.error("hleWlanReceiveMessage", e);
		}

		return packetReceived;
    }

    private boolean createWlanSocket() {
    	if (wlanSocket == null) {
			boolean retry;
			do {
				retry = false;
	    		try {
					wlanSocket = new DatagramSocket(wlanSocketPort);
		    		// For broadcast
					wlanSocket.setBroadcast(true);
		    		// Non-blocking (timeout = 0 would mean blocking)
					wlanSocket.setSoTimeout(1);
	    		} catch (BindException e) {
	    			if (log.isDebugEnabled()) {
	    				log.debug(String.format("createWlanSocket port %d already in use (%s) - retrying with port %d", wlanSocketPort, e, wlanSocketPort + 1));
	    			}
	    			// The port is already busy, retrying with another port
	    			wlanSocketPort++;
	    			retry = true;
				} catch (SocketException e) {
					log.error("createWlanSocket", e);
				}
			} while (retry);
    	}

    	return wlanSocket != null;
    }

    protected void hleWlanSendMessage(TPointer handleAddr, SceNetIfMessage message) {
    	Memory mem = handleAddr.getMemory();
    	SceNetWlanMessage wlanMessage = new SceNetWlanMessage();
    	wlanMessage.read(mem, message.dataAddr);

    	if (log.isDebugEnabled()) {
    		log.debug(String.format("hleWlanSendMessage message: %s: %s", message, Utilities.getMemoryDump(message.getBaseAddress(), message.sizeof())));
    		log.debug(String.format("hleWlanSendMessage WLAN message : %s", wlanMessage));
    		log.debug(String.format("hleWlanSendMessage message data: %s", Utilities.getMemoryDump(message.dataAddr + wlanMessage.sizeof(), message.dataLength - wlanMessage.sizeof())));
    	}

    	if (!createWlanSocket()) {
    		return;
    	}

    	byte[] messageBytes = new byte[message.dataLength];
    	Utilities.readBytes(message.dataAddr, message.dataLength, messageBytes, 0);
    	try {
			InetSocketAddress broadcastAddress[] = sceNetInet.getBroadcastInetSocketAddress(wlanSocketPort ^ 1);
			if (broadcastAddress != null) {
				for (int i = 0; i < broadcastAddress.length; i++) {
					DatagramPacket packet = new DatagramPacket(messageBytes, message.dataLength, broadcastAddress[i]);
					wlanSocket.send(packet);
				}
			}
		} catch (UnknownHostException e) {
			log.error("hleWlanSendMessage", e);
		} catch (IOException e) {
			log.error("hleWlanSendMessage", e);
		}

    	if (false) {
    		sendDummyMessage(handleAddr, message, wlanMessage);
    	}
    }

    public int hleWlanSendCallback(TPointer handleAddr) {
    	SceNetIfHandle handle = new SceNetIfHandle();
    	handle.read(handleAddr);

    	Memory mem = handleAddr.getMemory();
    	TPointer firstMessageAddr = new TPointer(mem, handle.addrFirstMessageToBeSent);
    	SceNetIfMessage message = new SceNetIfMessage();
    	message.read(firstMessageAddr);
    	RuntimeContext.debugMemory(firstMessageAddr.getAddress(), message.sizeof());
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("hleWlanSendCallback handleAddr=%s: %s", handleAddr, handle));
    	}

    	hleWlanSendMessage(handleAddr, message);

    	// Call sceNetMFreem to free the received message
    	int sceNetMFreem = NIDMapper.getInstance().getAddressByName("sceNetMFreem");
    	if (sceNetMFreem != 0) {
    		Modules.ThreadManForUserModule.executeCallback(null, sceNetMFreem, null, true, firstMessageAddr.getAddress());
    	} else {
    		Modules.sceNetIfhandleModule.sceNetMFreem(firstMessageAddr);
    	}

    	// Unlink the message from the handle
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

    // Called by sceNetIfhandleIfUp
    public int hleWlanUpCallback(TPointer handleAddr) {
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("hleWlanUpCallback handleAddr: %s", Utilities.getMemoryDump(handleAddr.getAddress(), 44)));
    		int handleInternalAddr = handleAddr.getValue32();
    		if (handleInternalAddr != 0) {
        		log.debug(String.format("hleWlanUpCallback handleInternalAddr: %s", Utilities.getMemoryDump(handleInternalAddr, 320)));
    		}
    	}

    	SceNetIfHandle handle = new SceNetIfHandle();
    	handle.read(handleAddr);

    	if (log.isDebugEnabled()) {
    		log.debug(String.format("hleWlanUpCallback handleAddr=%s: %s", handleAddr, handle));
    	}

    	wlanHandleAddr = handleAddr;

    	// This thread will call hleWlanThread() in a loop
    	SceKernelThreadInfo thread = Modules.ThreadManForUserModule.hleKernelCreateThread("SceWlanHal", ThreadManForUser.WLAN_LOOP_ADDRESS, 39, 2048, 0, 0, KERNEL_PARTITION_ID);
    	if (thread != null) {
    		wlanThreadUid = thread.uid;
    		Modules.ThreadManForUserModule.hleKernelStartThread(thread, 0, 0, 0);
    	}

    	Modules.ThreadManForUserModule.sceKernelSignalSema(handle.handleInternal.ioctlSemaId, 1);

    	return 0;
    }

    // Called by sceNetIfhandleIfDown
    public int hleWlanDownCallback(TPointer handleAddr) {
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("hleWlanDownCallback handleAddr: %s", Utilities.getMemoryDump(handleAddr.getAddress(), 44)));
    		int handleInternalAddr = handleAddr.getValue32();
    		if (handleInternalAddr != 0) {
        		log.debug(String.format("hleWlanDownCallback handleInternalAddr: %s", Utilities.getMemoryDump(handleInternalAddr, 320)));
    		}
    	}

    	SceNetIfHandle handle = new SceNetIfHandle();
    	handle.read(handleAddr);

    	if (log.isDebugEnabled()) {
    		log.debug(String.format("hleWlanDownCallback handleAddr=%s: %s", handleAddr, handle));
    	}

    	// This will force the current wlan thread to exit
    	wlanThreadUid = INVALID_ID;

    	Modules.ThreadManForUserModule.sceKernelSignalSema(handle.handleInternal.ioctlSemaId, 1);

    	return 0;
    }

    public int hleWlanIoctlCallback(TPointer handleAddr, int cmd, TPointer unknown1, TPointer32 unknown2) {
    	SceNetIfHandle handle = new SceNetIfHandle();
    	handle.read(handleAddr);

    	Memory mem = Memory.getInstance();
		int inputAddr = unknown2.getValue(0);
		int outputAddr = unknown2.getValue(4);

		if (log.isDebugEnabled()) {
    		int inputLength = 0x80;
    		int outputLength = 0x80;
    		switch (cmd) {
    			case 0x34:
    				inputLength = 0x4C;
    				outputLength = 0x600;
    				break;
    			case 0x35:
    				inputLength = 0x70;
    				break;
    			case 0x36:
    				inputLength = 0x70;
    				break;
    			case 0x37:
    				inputLength = 0x60;
    				break;
    		}
    		log.debug(String.format("hleWlanIoctlCallback handleAddr=%s: %s", handleAddr, handle));
    		if (inputAddr != 0 && Memory.isAddressGood(inputAddr) && inputLength > 0) {
    			log.debug(String.format("hleWlanIoctlCallback inputAddr: %s", Utilities.getMemoryDump(inputAddr, inputLength)));
    			RuntimeContext.debugMemory(inputAddr, inputLength);
    		}
    		if (outputAddr != 0 && Memory.isAddressGood(outputAddr) && outputLength > 0) {
    			log.debug(String.format("hleWlanIoctlCallback outputAddr: %s", Utilities.getMemoryDump(outputAddr, outputLength)));
    			RuntimeContext.debugMemory(outputAddr, outputLength);
    		}
    		RuntimeContext.debugMemory(unknown1.getAddress(), 32);
    	}

		String ssid;
		int ssidLength;
		int errorCode = 0;
		int type;
		int unknownValue012;
		int unknownValue345;
    	switch (cmd) {
    		case 0x34: // Start scanning?
    			ssidLength = mem.read8(inputAddr + 24);
    			ssid = Utilities.readStringNZ(mem, inputAddr + 28, ssidLength);

    			mem.write32(outputAddr, 0); // Link to next SSID
    			int ssidAddr = outputAddr + 4;
    			mem.memset(ssidAddr, (byte) 0, 92);
    			unknownValue012 = 0;
    			mem.write8(ssidAddr + 0, (byte) (unknownValue012 >> 16));
    			mem.write8(ssidAddr + 1, (byte) (unknownValue012 >>  8));
    			mem.write8(ssidAddr + 2, (byte) (unknownValue012 >>  0));
    			unknownValue345 = 0;
    			mem.write8(ssidAddr + 3, (byte) (unknownValue345 >> 16));
    			mem.write8(ssidAddr + 4, (byte) (unknownValue345 >>  8));
    			mem.write8(ssidAddr + 5, (byte) (unknownValue345 >>  0));
    			Utilities.writeStringNZ(mem, ssidAddr + 8, 32, ssid);
    			type = 2;
    			mem.write32(ssidAddr + 40, type);
    			break;
    		case 0x35: // Start joining?
    			ssidLength = mem.read8(inputAddr + 7);
    			ssid = Utilities.readStringNZ(mem, inputAddr + 8, ssidLength);
    			type = mem.read32(inputAddr + 40);
    			int unknown6 = mem.read8(inputAddr + 6); // 0xB
    			int unknown44 = mem.read32(inputAddr + 44); // 0x64
    			int unknown62 = mem.read16(inputAddr + 62); // 0x22
    			if (log.isDebugEnabled()) {
    				log.debug(String.format("hleWlanIoctlCallback cmd=0x%X, ssid='%s', type=0x%X, unknown6=0x%X, unknown44=0x%X, unknown62=0x%X", cmd, ssid, type, unknown6, unknown44, unknown62));
    			}
    			break;
    		case 0x36: // Join
    			// Receiving as input the SSID structure returned by cmd=0x34
    			ssid = Utilities.readStringNZ(mem, inputAddr + 8, 32);
    			if (log.isDebugEnabled()) {
    				log.debug(String.format("hleWlanIoctlCallback cmd=0x%X, ssid='%s'", cmd, ssid));
    			}
    			joinedSSID = ssid;
    			break;
    		case 0x37: // Get joined SSID
    			// Remark: returning the joined SSID in the inputAddr!
    			mem.memset(inputAddr, (byte) 0, 40);
    			unknownValue012 = 0;
    			mem.write8(inputAddr + 0, (byte) (unknownValue012 >> 16));
    			mem.write8(inputAddr + 1, (byte) (unknownValue012 >>  8));
    			mem.write8(inputAddr + 2, (byte) (unknownValue012 >>  0));
    			unknownValue345 = 0;
    			mem.write8(inputAddr + 3, (byte) (unknownValue345 >> 16));
    			mem.write8(inputAddr + 4, (byte) (unknownValue345 >>  8));
    			mem.write8(inputAddr + 5, (byte) (unknownValue345 >>  0));
    			Utilities.writeStringNZ(mem, inputAddr + 8, 32, joinedSSID);
    			break;
    		case 0x38: // Disconnect
    			break;
    	}
    	handle.handleInternal.errorCode = errorCode;
    	handle.write(handleAddr);
    	Modules.ThreadManForUserModule.sceKernelSignalSema(handle.handleInternal.ioctlSemaId, 1);

    	return 0;
    }

    static private void sendDummyMessage(int step, TPointer handleAddr) {
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("sendDummyMessage step=%d", step));
    	}
    	Memory mem = Memory.getInstance();
    	SceNetIfMessage message = new SceNetIfMessage();
    	SceNetWlanMessage wlanMessage = new SceNetWlanMessage();

    	final int size = message.sizeof() + wlanMessage.sizeof() + SceNetWlanMessage.maxContentLength + 0x12;
    	int allocatedAddr;
    	SceKernelVplInfo vplInfo = Managers.vpl.getVplInfoByName("SceNet");
    	if (vplInfo != null) {
    		allocatedAddr = Managers.vpl.tryAllocateVpl(vplInfo, size);
    	} else {
    		allocatedAddr = Modules.sceNetIfhandleModule.sceNetMallocInternal(size);
    	}

    	if (allocatedAddr <= 0) {
    		return;
    	}
    	RuntimeContext.debugMemory(allocatedAddr, size);
    	mem.memset(allocatedAddr, (byte) 0, size);

    	TPointer messageAddr = new TPointer(mem, allocatedAddr);
    	TPointer data = new TPointer(mem, messageAddr.getAddress() + message.sizeof());
    	TPointer header = new TPointer(mem, data.getAddress());
    	TPointer content = new TPointer(mem, header.getAddress() + wlanMessage.sizeof());

    	int dataLength;
    	int controlType;
    	int contentLength;
    	switch (step) {
    		case 1:
	        	controlType = 2; // possible values: [1..8]
		    	contentLength = SceNetWlanMessage.contentLengthFromMessageType[controlType];
		    	dataLength = wlanMessage.sizeof() + contentLength;
	
		    	wlanMessage.dstMacAddress = new pspNetMacAddress(Wlan.getMacAddress());
		    	wlanMessage.srcMacAddress = new pspNetMacAddress(dummyOtherMacAddress);
		    	wlanMessage.protocolType = WLAN_PROTOCOL_TYPE_SONY;
		    	wlanMessage.protocolSubType = WLAN_PROTOCOL_SUBTYPE_CONTROL; // 1 or 2 -> 1 will trigger sceNetIfhandleModule.unknownCallback3, 2 will trigger sceNetIfhandleModule.unknownCallback1
		    	wlanMessage.unknown16 = 1; // possible value: only 1
		    	wlanMessage.controlType = controlType;
		    	wlanMessage.contentLength = SceNetWlanMessage.contentLengthFromMessageType[controlType];
	
		    	content.clear(contentLength);
		    	break;
    		case 2:
	        	controlType = 0;
		    	contentLength = 0x4C;
		    	dataLength = wlanMessage.sizeof() + contentLength;

		    	wlanMessage.dstMacAddress = new pspNetMacAddress(new byte[] { -1, -1, -1, -1, -1, -1}); // Broadcast MAC address
		    	wlanMessage.srcMacAddress = new pspNetMacAddress(dummyOtherMacAddress);
		    	wlanMessage.protocolType = WLAN_PROTOCOL_TYPE_SONY;
		    	wlanMessage.protocolSubType = WLAN_PROTOCOL_SUBTYPE_DATA; // 1 or 2 -> 1 will trigger sceNetIfhandleModule.unknownCallback3, 2 will trigger sceNetIfhandleModule.unknownCallback1
		    	wlanMessage.unknown16 = 0;
		    	wlanMessage.controlType = controlType;
		    	wlanMessage.contentLength = contentLength;

		    	content.clear(contentLength);
		    	content.setStringNZ(0x34, 5, "Jpcsp");
		    	break;
    		case 3:
    			controlType = 2; // possible values: [1..8]
    			contentLength = SceNetWlanMessage.contentLengthFromMessageType[controlType];
    			dataLength = wlanMessage.sizeof() + contentLength + 0x12;

		    	wlanMessage.dstMacAddress = new pspNetMacAddress(new byte[] { -1, -1, -1, -1, -1, -1});
		    	wlanMessage.srcMacAddress = new pspNetMacAddress(dummyOtherMacAddress);
		    	wlanMessage.protocolType = WLAN_PROTOCOL_TYPE_SONY;
		    	wlanMessage.protocolSubType = WLAN_PROTOCOL_SUBTYPE_CONTROL; // 1 or 2 -> 1 will trigger sceNetIfhandleModule.unknownCallback3, 2 will trigger sceNetIfhandleModule.unknownCallback1
		    	wlanMessage.unknown16 = 1; // possible value: only 1
		    	wlanMessage.controlType = controlType;
		    	wlanMessage.contentLength = SceNetWlanMessage.contentLengthFromMessageType[controlType];

		    	content.setStringNZ(0, 0x80, "JpcspOther");
		    	content.setValue8(0x80, (byte) 1);
		    	content.setValue8(0x81, (byte) 4);
		    	content.setUnalignedValue32(0x82, Modules.SysMemUserForUserModule.sceKernelDevkitVersion());
		    	content.setValue8(0x86, (byte) 2);
		    	content.setValue8(0x87, (byte) 4);
		    	content.setUnalignedValue32(0x88, Modules.SysMemUserForUserModule.sceKernelGetCompiledSdkVersion());
		    	content.setValue8(0x8C, (byte) 3);
		    	content.setValue8(0x8D, (byte) 4);
		    	content.setUnalignedValue32(0x8E, Modules.SysMemForKernelModule.sceKernelGetModel());
		    	break;
    		case 4:
    			controlType = 3; // possible values: [1..8]
    			contentLength = SceNetWlanMessage.contentLengthFromMessageType[controlType];
    			dataLength = wlanMessage.sizeof() + contentLength + 0x12;

		    	wlanMessage.dstMacAddress = new pspNetMacAddress(Wlan.getMacAddress());
		    	wlanMessage.srcMacAddress = new pspNetMacAddress(dummyOtherMacAddress);
		    	wlanMessage.protocolType = WLAN_PROTOCOL_TYPE_SONY;
		    	wlanMessage.protocolSubType = WLAN_PROTOCOL_SUBTYPE_CONTROL; // 1 or 2 -> 1 will trigger sceNetIfhandleModule.unknownCallback3, 2 will trigger sceNetIfhandleModule.unknownCallback1
		    	wlanMessage.unknown16 = 1; // possible value: only 1
		    	wlanMessage.controlType = controlType;
		    	wlanMessage.contentLength = SceNetWlanMessage.contentLengthFromMessageType[controlType];

		    	content.clear(contentLength);
		    	content.setStringNZ(0xA0, 0x80, "JpcspOther");
		    	content.setValue8(0x120, (byte) 1);
		    	content.setValue8(0x121, (byte) 4);
		    	content.setUnalignedValue32(0x82, Modules.SysMemUserForUserModule.sceKernelDevkitVersion());
		    	content.setValue8(0x126, (byte) 2);
		    	content.setValue8(0x127, (byte) 4);
		    	content.setUnalignedValue32(0x88, Modules.SysMemUserForUserModule.sceKernelGetCompiledSdkVersion());
		    	content.setValue8(0x12C, (byte) 3);
		    	content.setValue8(0x12D, (byte) 4);
		    	content.setUnalignedValue32(0x12E, Modules.SysMemForKernelModule.sceKernelGetModel());
		    	break;
    		case 5:
    			controlType = 4; // possible values: [1..8]
    			contentLength = SceNetWlanMessage.contentLengthFromMessageType[controlType];
    			dataLength = wlanMessage.sizeof() + contentLength;

		    	wlanMessage.dstMacAddress = new pspNetMacAddress(Wlan.getMacAddress());
		    	wlanMessage.srcMacAddress = new pspNetMacAddress(dummyOtherMacAddress);
		    	wlanMessage.protocolType = WLAN_PROTOCOL_TYPE_SONY;
		    	wlanMessage.protocolSubType = WLAN_PROTOCOL_SUBTYPE_CONTROL; // 1 or 2 -> 1 will trigger sceNetIfhandleModule.unknownCallback3, 2 will trigger sceNetIfhandleModule.unknownCallback1
		    	wlanMessage.unknown16 = 1; // possible value: only 1
		    	wlanMessage.controlType = controlType;
		    	wlanMessage.contentLength = SceNetWlanMessage.contentLengthFromMessageType[controlType];

		    	content.clear(contentLength);
		    	break;
    		case 6:
    			controlType = 5; // possible values: [1..8]
    			contentLength = SceNetWlanMessage.contentLengthFromMessageType[controlType];
    			dataLength = wlanMessage.sizeof() + contentLength;

		    	wlanMessage.dstMacAddress = new pspNetMacAddress(Wlan.getMacAddress());
		    	wlanMessage.srcMacAddress = new pspNetMacAddress(dummyOtherMacAddress);
		    	wlanMessage.protocolType = WLAN_PROTOCOL_TYPE_SONY;
		    	wlanMessage.protocolSubType = WLAN_PROTOCOL_SUBTYPE_CONTROL; // 1 or 2 -> 1 will trigger sceNetIfhandleModule.unknownCallback3, 2 will trigger sceNetIfhandleModule.unknownCallback1
		    	wlanMessage.unknown16 = 1; // possible value: only 1
		    	wlanMessage.controlType = controlType;
		    	wlanMessage.contentLength = SceNetWlanMessage.contentLengthFromMessageType[controlType];

		    	content.clear(contentLength);
		    	break;
    		case 7:
    			controlType = 6; // possible values: [1..8]
    			contentLength = SceNetWlanMessage.contentLengthFromMessageType[controlType];
    			dataLength = wlanMessage.sizeof() + contentLength;

		    	wlanMessage.dstMacAddress = new pspNetMacAddress(Wlan.getMacAddress());
		    	wlanMessage.srcMacAddress = new pspNetMacAddress(dummyOtherMacAddress);
		    	wlanMessage.protocolType = WLAN_PROTOCOL_TYPE_SONY;
		    	wlanMessage.protocolSubType = WLAN_PROTOCOL_SUBTYPE_CONTROL; // 1 or 2 -> 1 will trigger sceNetIfhandleModule.unknownCallback3, 2 will trigger sceNetIfhandleModule.unknownCallback1
		    	wlanMessage.unknown16 = 1; // possible value: only 1
		    	wlanMessage.controlType = controlType;
		    	wlanMessage.contentLength = SceNetWlanMessage.contentLengthFromMessageType[controlType];

		    	content.clear(contentLength);
		    	break;
    		case 8:
    			controlType = 8; // possible values: [1..8]
    			contentLength = SceNetWlanMessage.contentLengthFromMessageType[controlType];
    			dataLength = wlanMessage.sizeof() + contentLength;

		    	wlanMessage.dstMacAddress = new pspNetMacAddress(Wlan.getMacAddress());
		    	wlanMessage.srcMacAddress = new pspNetMacAddress(dummyOtherMacAddress);
		    	wlanMessage.protocolType = WLAN_PROTOCOL_TYPE_SONY;
		    	wlanMessage.protocolSubType = WLAN_PROTOCOL_SUBTYPE_CONTROL; // 1 or 2 -> 1 will trigger sceNetIfhandleModule.unknownCallback3, 2 will trigger sceNetIfhandleModule.unknownCallback1
		    	wlanMessage.unknown16 = 1; // possible value: only 1
		    	wlanMessage.controlType = controlType;
		    	wlanMessage.contentLength = SceNetWlanMessage.contentLengthFromMessageType[controlType];

		    	content.clear(contentLength);
		    	break;
    		default:
    			dataLength = 0;
    			break;
		}

    	wlanMessage.write(header);

    	message.dataAddr = data.getAddress();
		message.dataLength = dataLength;
		message.unknown18 = 0;
		message.unknown24 = dataLength;
		message.write(messageAddr);

		if (dataLength > 0) {
			if (log.isDebugEnabled()) {
				log.debug(String.format("Sending dummy message: %s", message));
				log.debug(String.format("Dummy message data: %s", Utilities.getMemoryDump(data.getAddress(), dataLength)));
			}

			int sceNetIfEnqueue = NIDMapper.getInstance().getAddressByName("sceNetIfEnqueue");
			if (sceNetIfEnqueue != 0) {
				SceKernelThreadInfo thread = Modules.ThreadManForUserModule.getCurrentThread();
				Modules.ThreadManForUserModule.executeCallback(thread, sceNetIfEnqueue, null, true, handleAddr.getAddress(), messageAddr.getAddress());
			}
		}
    }

    private void sendDummyMessage(TPointer handleAddr, SceNetIfMessage sentMessage, SceNetWlanMessage sentWlanMessage) {
    	int step = 0;
    	if (false) {
    		step = 1;
    	} else if (false) {
    		step = 2;
    	} else if (dummyMessageStep < 0 && !sentWlanMessage.dstMacAddress.equals(dummyOtherMacAddress)) {
    		step = 3;
		} else if (sentWlanMessage.controlType == 3) {
			step = 5;
		} else if (sentWlanMessage.controlType == 4) {
			step = 5;
		} else if (sentWlanMessage.controlType == 5) {
			step = 7;
		} else if (sentWlanMessage.controlType == 7) {
			step = 8;
		} else {
			step = 0;
		}

    	if (log.isDebugEnabled()) {
    		log.debug(String.format("Adding action step=%d for sending dummy message", step));
    	}
    	dummyMessageStep = step;
    	dummyMessageHandleAddr = handleAddr;
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

    	int sceNetAttachIfhandleEther = NIDMapper.getInstance().getAddressByName("sceNetAttachIfhandleEther");
    	if (sceNetAttachIfhandleEther == 0) {
    		return;
    	}

    	Modules.ThreadManForUserModule.executeCallback(thread, sceNetAttachIfhandleEther, null, true, handleAddr.getAddress(), macAddressAddr, interfaceNameAddr);
    }

    private int createWlanInterface() {
		SceNetIfHandle handle = new SceNetIfHandle();
		handle.callbackArg4 = 0x11040404; // dummy callback value
		handle.upCallbackAddr = ThreadManForUser.WLAN_UP_CALLBACK_ADDRESS;
		handle.downCallbackAddr = ThreadManForUser.WLAN_DOWN_CALLBACK_ADDRESS;
		handle.sendCallbackAddr = ThreadManForUser.WLAN_SEND_CALLBACK_ADDRESS;
		handle.ioctlCallbackAddr = ThreadManForUser.WLAN_IOCTL_CALLBACK_ADDRESS;
		int handleMem = sceNetIfhandleModule.sceNetMallocInternal(handle.sizeof());
		if (handleMem < 0) {
			return handleMem;
		}
		TPointer handleAddr = new TPointer(Memory.getInstance(), handleMem);
		handle.write(handleAddr);
		RuntimeContext.debugMemory(handleAddr.getAddress(), handle.sizeof());

		int sceNetCreateIfhandleEther = NIDMapper.getInstance().getAddressByName("sceNetCreateIfhandleEther");
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

    @HLEUnimplemented
    @HLEFunction(nid = 0x2519EAA7, version = 150)
    public int sceWlanDrv_lib_2519EAA7() {
    	// Has no parameters
        return 0;
    }
}