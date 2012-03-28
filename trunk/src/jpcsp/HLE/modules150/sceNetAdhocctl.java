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

import jpcsp.HLE.CanBeNull;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer32;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.LinkedList;

import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.Processor;
import jpcsp.Allegrex.Common;
import jpcsp.Allegrex.CpuState;
import jpcsp.HLE.kernel.managers.IntrManager;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.kernel.types.SceKernelThreadInfo;
import jpcsp.HLE.kernel.types.pspNetMacAddress;
import jpcsp.HLE.Modules;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.HLE.modules.ThreadManForUser;
import jpcsp.hardware.Wlan;
import jpcsp.util.Utilities;

import org.apache.log4j.Logger;

public class sceNetAdhocctl extends HLEModule {
    protected static Logger log = Modules.getLogger("sceNetAdhocctl");

    @Override
    public String getName() {
        return "sceNetAdhocctl";
    }

    public static final int PSP_ADHOCCTL_EVENT_ERROR = 0;
    public static final int PSP_ADHOCCTL_EVENT_CONNECTED = 1;
    public static final int PSP_ADHOCCTL_EVENT_DISCONNECTED = 2;
    public static final int PSP_ADHOCCTL_EVENT_SCAN = 3;
    public static final int PSP_ADHOCCTL_EVENT_GAME = 4;
    public static final int PSP_ADHOCCTL_EVENT_DISCOVER = 5;
    public static final int PSP_ADHOCCTL_EVENT_WOL = 6;
    public static final int PSP_ADHOCCTL_EVENT_WOL_INTERRUPTED = 7;

    public static final int PSP_ADHOCCTL_STATE_DISCONNECTED = 0;
    public static final int PSP_ADHOCCTL_STATE_CONNECTED = 1;
    public static final int PSP_ADHOCCTL_STATE_SCAN = 2;
    public static final int PSP_ADHOCCTL_STATE_GAME = 3;
    public static final int PSP_ADHOCCTL_STATE_DISCOVER = 4;
    public static final int PSP_ADHOCCTL_STATE_WOL = 5;

    public static final int NICK_NAME_LENGTH = 128;
    public static final int GROUP_NAME_LENGTH = 8;

	protected int adhocctlCurrentState;
    protected String adhocctlCurrentGroup;
    protected boolean doTerminate;
    protected SceKernelThreadInfo adhocctlThread;
    private boolean doScan;
    private boolean doDisconnect;
    private DatagramSocket adhocctlSocket;
    private static final int adhocctlBroadcastPort = 30000;
    protected LinkedList<AdhocctlPeer> peers;

    private HashMap<Integer, AdhocctlHandler> adhocctlHandlerMap = new HashMap<Integer, AdhocctlHandler>();
    private int adhocctlHandlerCount = 0;

    protected class AdhocctlHandler {

        private int entryAddr;
        private int currentEvent;
        private int currentError;
        private int currentArg;
        private int handle;

        private AdhocctlHandler(int num, int addr, int arg) {
            entryAddr = addr;
            currentArg = arg;
            handle = makeFakeAdhocctHandle(num);
        }

        protected void triggerAdhocctlHandler() {
            SceKernelThreadInfo thread = Modules.ThreadManForUserModule.getCurrentThread();
            if (thread != null) {
                Modules.ThreadManForUserModule.executeCallback(thread, entryAddr, null, true, currentEvent, currentError, currentArg);
            }
        }

        protected int makeFakeAdhocctHandle(int num) {
            return 0x0000AD00 | (num & 0xFFFF);
        }

        protected int getHandle() {
            return handle;
        }

        protected void setEvent(int event) {
            currentEvent = event;
        }

        protected void setError(int error) {
            currentError = error;
        }
    }

    protected static class AdhocctlPeer {
    	public String nickName;
    	public byte[] macAddress;
    	public long timestamp;

    	public boolean equals(String nickName, byte[] macAddress) {
    		return nickName.equals(this.nickName) && sceNetAdhoc.isSameMacAddress(macAddress, this.macAddress);
    	}

		@Override
		public String toString() {
			return String.format("nickName='%s', macAddress=%s", nickName, sceNet.convertMacAddressToString(macAddress));
		}
    }

    private static class AdhocctlMessage {
    	private String nickName;
    	private byte[] macAddress = new byte[Wlan.MAC_ADDRESS_LENGTH];
    	private String groupName;

    	public AdhocctlMessage(String nickName, byte[] macAddress, String groupName) {
    		this.nickName = nickName;
    		System.arraycopy(macAddress, 0, this.macAddress, 0, this.macAddress.length);
    		this.groupName = groupName;
    	}

    	public AdhocctlMessage(byte[] message, int length) {
    		nickName = copyFromMessage(message, 0, NICK_NAME_LENGTH);
    		copyFromMessage(message, NICK_NAME_LENGTH, macAddress);
    		groupName = copyFromMessage(message, NICK_NAME_LENGTH + macAddress.length, GROUP_NAME_LENGTH);
    	}

    	private String copyFromMessage(byte[] message, int offset, int length) {
    		StringBuilder s = new StringBuilder();
    		for (int i = 0; i < length; i++) {
    			byte b = message[offset + i];
    			if (b == 0) {
    				break;
    			}
    			s.append((char) b);
    		}

    		return s.toString();
    	}

    	private void copyFromMessage(byte[] message, int offset, byte[] bytes) {
    		System.arraycopy(message, offset, bytes, 0, bytes.length);
    	}

    	private void copyToMessage(byte[] message, int offset, String s) {
    		int length = s.length();
    		for (int i = 0; i < length; i++) {
    			message[offset + i] = (byte) s.charAt(i);
    		}
    	}

    	private void copyToMessage(byte[] message, int offset, byte[] bytes) {
    		for (int i = 0; i < bytes.length; i++) {
    			message[offset + i] = bytes[i];
    		}
    	}

    	public byte[] getMessage() {
    		byte[] message = new byte[getMessageLength()];

    		copyToMessage(message, 0, nickName);
    		copyToMessage(message, NICK_NAME_LENGTH, macAddress);
    		copyToMessage(message, NICK_NAME_LENGTH + macAddress.length, groupName);

    		return message;
    	}

    	public static int getMessageLength() {
    		return NICK_NAME_LENGTH + Wlan.MAC_ADDRESS_LENGTH + GROUP_NAME_LENGTH;
    	}

		@Override
		public String toString() {
			return String.format("AdhocctlMessage[nickName='%s', macAddress=%s, groupName='%s']", nickName, sceNet.convertMacAddressToString(macAddress), groupName);
		}
    }


	@Override
	public void start() {
		peers = new LinkedList<sceNetAdhocctl.AdhocctlPeer>();

		super.start();
	}

    public void hleNetAdhocctlThread(Processor processor) {
    	ThreadManForUser threadMan = Modules.ThreadManForUserModule;

    	if (log.isDebugEnabled()) {
    		log.debug("hleNetAdhocctlThread");
    	}

    	if (doTerminate) {
    		setState(PSP_ADHOCCTL_STATE_DISCONNECTED);
    		notifyAdhocctlHandler(PSP_ADHOCCTL_EVENT_DISCONNECTED, 0);
    	} else if (doDisconnect) {
    		setState(PSP_ADHOCCTL_STATE_DISCONNECTED);
    		notifyAdhocctlHandler(PSP_ADHOCCTL_EVENT_DISCONNECTED, 0);
    		doDisconnect = false;
    	} else if (doScan) {
    		setState(PSP_ADHOCCTL_STATE_SCAN);
            notifyAdhocctlHandler(PSP_ADHOCCTL_EVENT_SCAN, 0);
            doScan = false;
    	} else if (adhocctlCurrentState == PSP_ADHOCCTL_STATE_DISCONNECTED) {
    		if (adhocctlCurrentGroup != null) {
    			setState(PSP_ADHOCCTL_STATE_CONNECTED);
    			notifyAdhocctlHandler(PSP_ADHOCCTL_EVENT_CONNECTED, 0);
    		}
    	}

    	if (adhocctlCurrentState == PSP_ADHOCCTL_STATE_CONNECTED) {
    		broadcastPeers();
    		pollPeers();
    	}

    	if (doTerminate) {
			// Exit thread with status 0
			processor.cpu.gpr[Common._v0] = 0;
			threadMan.hleKernelExitDeleteThread();
			adhocctlThread = null;
			doTerminate = false;
    	} else {
    		// Wait for the next event
    		threadMan.hleKernelSleepThread(false);
    	}
    }

    private void openSocket() throws SocketException {
    	if (adhocctlSocket == null) {
    		adhocctlSocket = new DatagramSocket(adhocctlBroadcastPort + Modules.sceNetAdhocModule.netServerPortShift);
    		// For broadcast
    		adhocctlSocket.setBroadcast(true);
    		// Non-blocking (timeout = 0 would mean blocking)
    		adhocctlSocket.setSoTimeout(1);
    	}
    }

    private void broadcastPeers() {
		try {
			openSocket();

			AdhocctlMessage adhocctlMessage = new AdhocctlMessage(sceUtility.getSystemParamNickname(), Wlan.getMacAddress(), adhocctlCurrentGroup);
	    	SocketAddress socketAddress = Modules.sceNetAdhocModule.getSocketAddress(sceNetAdhoc.ANY_MAC_ADDRESS, adhocctlBroadcastPort + Modules.sceNetAdhocModule.netClientPortShift);
	    	DatagramPacket packet = new DatagramPacket(adhocctlMessage.getMessage(), AdhocctlMessage.getMessageLength(), socketAddress);
	    	adhocctlSocket.send(packet);

	    	if (log.isDebugEnabled()) {
	    		log.debug(String.format("broadcast sent to peers: %s", adhocctlMessage));
	    	}
		} catch (SocketException e) {
			log.error("broadcastPeers", e);
		} catch (IOException e) {
			log.error("broadcastPeers", e);
		}
    }

    private void pollPeers() {
		try {
			openSocket();

	    	byte[] bytes = new byte[AdhocctlMessage.getMessageLength()];
	    	DatagramPacket packet = new DatagramPacket(bytes, bytes.length);
	    	adhocctlSocket.receive(packet);
	    	AdhocctlMessage adhocctlMessage = new AdhocctlMessage(packet.getData(), packet.getLength());

	    	if (log.isDebugEnabled()) {
	    		log.debug(String.format("broadcast received from peer: %s", adhocctlMessage));
	    	}

	    	if (adhocctlMessage.groupName.equals(adhocctlCurrentGroup) && !sceNetAdhoc.isSameMacAddress(Wlan.getMacAddress(), adhocctlMessage.macAddress)) {
		    	boolean found = false;
		    	for (AdhocctlPeer peer : peers) {
		    		if (peer.equals(adhocctlMessage.nickName, adhocctlMessage.macAddress)) {
		    			// Update the timestamp
		    			peer.timestamp = Emulator.getClock().microTime();

		    			found = true;
		    			break;
		    		}
		    	}

		    	if (!found) {
		    		AdhocctlPeer peer = new AdhocctlPeer();
		    		peer.nickName = adhocctlMessage.nickName;
		    		peer.macAddress = adhocctlMessage.macAddress;
		    		peers.add(peer);

		    		if (log.isDebugEnabled()) {
		    			log.debug(String.format("New peer discovered %s", peer));
		    		}
		    	}
	    	}
		} catch (SocketException e) {
			log.error("broadcastPeers", e);
		} catch (SocketTimeoutException e) {
			// Nothing available
		} catch (IOException e) {
			log.error("broadcastPeers", e);
		}
    }

    protected void triggerAdhocctlThread() {
    	if (adhocctlThread != null) {
    		Modules.ThreadManForUserModule.hleKernelWakeupThread(adhocctlThread);
    	}
    }

    protected void setState(int state) {
    	adhocctlCurrentState = state;
    }

    protected void setGroupName(String groupName) {
    	adhocctlCurrentGroup = groupName;
    	triggerAdhocctlThread();
    }

    protected void notifyAdhocctlHandler(int event, int error) {
        for (AdhocctlHandler handler : adhocctlHandlerMap.values()) {
            handler.setEvent(event);
            handler.setError(error);
            handler.triggerAdhocctlHandler();
        }
    }

    /**
     * Initialise the Adhoc control library
     *
     * @param stacksize - Stack size of the adhocctl thread. Set to 0x2000
     * @param priority - Priority of the adhocctl thread. Set to 0x30
     * @param product - Pass a filled in ::productStruct
     *
     * @return 0 on success, < 0 on error
     */
    @HLEFunction(nid = 0xE26F226E, version = 150, checkInsideInterrupt = true)
    public int sceNetAdhocctlInit(int stackSize, int priority, @CanBeNull TPointer product) {
        Memory mem = Memory.getInstance();

        log.warn(String.format("PARTIAL: sceNetAdhocctlInit stackSize=0x%X, priority=%d, product=%s", stackSize, priority, product));

        if (Memory.isAddressGood(product.getAddress())) {
            int adhocType = mem.read32(product.getAddress()); // 0 - Commercial type / 1 - Debug type.
            String adhocParams = Utilities.readStringNZ(mem, product.getAddress() + 4, 9);
            if (log.isDebugEnabled()) {
            	log.debug(String.format("Found product data: type=%d, params='%s'", adhocType, adhocParams));
            }
        }

        setState(PSP_ADHOCCTL_STATE_DISCONNECTED);
        doTerminate = false;
        doScan = false;
        doDisconnect = false;
        ThreadManForUser threadMan = Modules.ThreadManForUserModule;
        adhocctlThread = threadMan.hleKernelCreateThread("SceNetAdhocctl", ThreadManForUser.NET_ADHOC_CTL_LOOP_ADDRESS, priority, stackSize, 0, 0);
        threadMan.hleKernelStartThread(adhocctlThread, 0, 0, adhocctlThread.gpReg_addr);

        return 0;
    }

    /**
     * Terminate the Adhoc control library
     *
     * @return 0 on success, < on error.
     */
    @HLEFunction(nid = 0x9D689E13, version = 150, checkInsideInterrupt = true)
    public int sceNetAdhocctlTerm() {
        log.warn("PARTIAL: sceNetAdhocctlTerm");

        doTerminate = true;
        triggerAdhocctlThread();

        return 0;
    }

    /**
     * Connect to the Adhoc control
     *
     * @param name - The name of the connection (maximum 8 alphanumeric characters).
     *
     * @return 0 on success, < 0 on error.
     */
    @HLEFunction(nid = 0x0AD043ED, version = 150, checkInsideInterrupt = true)
    public int sceNetAdhocctlConnect(TPointer groupNameAddr) {
        Memory mem = Memory.getInstance();

        String groupName = Utilities.readStringNZ(mem, groupNameAddr.getAddress(), GROUP_NAME_LENGTH);

        log.warn(String.format("PARTIAL: sceNetAdhocctlConnect groupNameAddr=%s('%s')", groupNameAddr, groupName));

        setGroupName(groupName);

        return 0;
    }

    /**
     * Connect to the Adhoc control (as a host)
     *
     * @param name - The name of the connection (maximum 8 alphanumeric characters).
     *
     * @return 0 on success, < 0 on error.
     */
    @HLEFunction(nid = 0xEC0635C1, version = 150, checkInsideInterrupt = true)
    public int sceNetAdhocctlCreate(@CanBeNull TPointer groupNameAddr) {
    	String groupName = "";
    	if (groupNameAddr.isAddressGood()) {
    		groupName = Utilities.readStringNZ(groupNameAddr.getAddress(), GROUP_NAME_LENGTH);
    	}
        log.warn(String.format("PARTIAL: sceNetAdhocctlCreate groupNameAddr=%s('%s')", groupNameAddr, groupName));

        setGroupName(groupName);

        return 0;
    }

    /**
     * Connect to the Adhoc control (as a client)
     *
     * @param scaninfo - A valid ::SceNetAdhocctlScanInfo struct that has been filled by sceNetAchocctlGetScanInfo
     *
     * @return 0 on success, < 0 on error.
     */
    @HLEFunction(nid = 0x5E7F79C9, version = 150, checkInsideInterrupt = true)
    public int sceNetAdhocctlJoin(TPointer scanInfoAddr) {
        log.warn(String.format("PARTIAL: sceNetAdhocctlJoin scanInfoAddr=%s", scanInfoAddr));

        if (scanInfoAddr.isAddressGood()) {
        	Memory mem = Memory.getInstance();
        	int addr = scanInfoAddr.getAddress();
            // IBSS Data field.
            int nextAddr = mem.read32(addr);  // Next group data.
            int ch = mem.read32(addr + 4);
            String groupName = Utilities.readStringNZ(mem, addr + 8, GROUP_NAME_LENGTH);
            String bssID = Utilities.readStringNZ(mem, addr + 16, 6);
            int mode = mem.read32(addr + 24);

            if (log.isDebugEnabled()) {
            	log.debug(String.format("sceNetAdhocctlJoin nextAddr 0x%08X, ch %d, groupName '%s', bssID '%s', mode %d", nextAddr, ch, groupName, bssID, mode));
            }
            setGroupName(groupName);
        }

        return 0;
    }

    /**
     * Scan the adhoc channels
     *
     * @return 0 on success, < 0 on error.
     */
    @HLEFunction(nid = 0x08FFF7A0, version = 150, checkInsideInterrupt = true)
    public int sceNetAdhocctlScan() {
        log.warn("PARTIAL: sceNetAdhocctlScan");

        doScan = true;
        triggerAdhocctlThread();

        return 0;
    }

    /**
     * Disconnect from the Adhoc control
     *
     * @return 0 on success, < 0 on error
     */
    @HLEFunction(nid = 0x34401D65, version = 150, checkInsideInterrupt = true)
    public int sceNetAdhocctlDisconnect() {
        log.warn("PARTIAL: sceNetAdhocctlDisconnect");

        doDisconnect = true;
        triggerAdhocctlThread();

        return 0;
    }

    /**
     * Register an adhoc event handler
     *
     * @param handler - The event handler.
     * @param unknown - Pass NULL.
     *
     * @return Handler id on success, < 0 on error.
     */
    @HLEFunction(nid = 0x20B317A0, version = 150)
    public void sceNetAdhocctlAddHandler(Processor processor) {
        CpuState cpu = processor.cpu;

        int adhocctlHandlerAddr = cpu.gpr[4];
        int adhocctlHandlerArg = cpu.gpr[5];

        log.warn("PARTIAL: sceNetAdhocctlAddHandler (adhocctlHandlerAddr=0x" + Integer.toHexString(adhocctlHandlerAddr) + ", adhocctlHandlerArg=0x" + Integer.toHexString(adhocctlHandlerArg) + ")");

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        AdhocctlHandler adhocctlHandler = new AdhocctlHandler(adhocctlHandlerCount++, adhocctlHandlerAddr, adhocctlHandlerArg);
        int handle = adhocctlHandler.getHandle();
        adhocctlHandlerMap.put(handle, adhocctlHandler);
        cpu.gpr[2] = handle;
    }

    /**
     * Delete an adhoc event handler
     *
     * @param id - The handler id as returned by sceNetAdhocctlAddHandler.
     *
     * @return 0 on success, < 0 on error.
     */
    @HLEFunction(nid = 0x6402490B, version = 150)
    public void sceNetAdhocctlDelHandler(Processor processor) {
       CpuState cpu = processor.cpu;

        int adhocctlHandler = cpu.gpr[4];

        log.warn("PARTIAL: sceNetAdhocctlDelHandler (adhocctlHandler=0x" + Integer.toHexString(adhocctlHandler) + ")");

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        adhocctlHandlerMap.remove(adhocctlHandler);
        cpu.gpr[2] = 0;
    }

    /**
     * Get the state of the Adhoc control
     *
     * @param event - Pointer to an integer to receive the status. Can continue when it becomes 1.
     *
     * @return 0 on success, < 0 on error
     */
    @HLEFunction(nid = 0x75ECD386, version = 150, checkInsideInterrupt = true)
    public int sceNetAdhocctlGetState(TPointer32 stateAddr) {
        log.warn(String.format("PARTIAL: sceNetAdhocctlGetState stateAddr=%s returning %d", stateAddr, adhocctlCurrentState));

        stateAddr.setValue(adhocctlCurrentState);

        // Update peer list
        triggerAdhocctlThread();

        return 0;
    }

    /**
     * Get the adhoc ID
     *
     * @param product - A pointer to a  ::productStruct
     *
     * @return 0 on success, < 0 on error.
     */
    @HLEFunction(nid = 0x362CBE8F, version = 150)
    public void sceNetAdhocctlGetAdhocId(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNetAdhocctlGetAdhocId");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    /**
     * Get a list of peers
     *
     * @param length - The length of the list.
     * @param buf - An allocated area of size length.
     *
     * @return 0 on success, < 0 on error.
     */
    @HLEFunction(nid = 0xE162CB14, version = 150)
    public int sceNetAdhocctlGetPeerList(TPointer32 sizeAddr, @CanBeNull TPointer buf) {
    	final int peerInfoSize = 152;
        log.warn(String.format("UNIMPLEMENTED: sceNetAdhocctlGetPeerList sizeAddr=%s(%d), buf=%s", sizeAddr, sizeAddr.getValue(), buf.toString()));

        if (buf.getAddress() == 0) {
        	// Return size required
        	sizeAddr.setValue(peerInfoSize * peers.size());
        	if (log.isDebugEnabled()) {
        		log.debug(String.format("sceNetAdhocctlGetPeerList returning size=%d", sizeAddr.getValue()));
        	}
        } else {
        	Memory mem = Memory.getInstance();
        	int addr = buf.getAddress();
        	int endAddr = addr + sizeAddr.getValue();
    		pspNetMacAddress macAddress = new pspNetMacAddress();
        	for (AdhocctlPeer peer : peers) {
        		// Check if enough space available to write the next structure
        		if (addr + peerInfoSize > endAddr || peer == null) {
        			break;
        		}

        		if (log.isDebugEnabled()) {
        			log.debug(String.format("sceNetAdhocctlGetPeerList returning %s at 0x%08X", peer, addr));
        		}

        		/** Pointer to next Peer structure in list: will be written later */
        		addr += 4;

        		/** NickName */
        		Utilities.writeStringNZ(mem, addr, NICK_NAME_LENGTH, peer.nickName);
        		addr += NICK_NAME_LENGTH;

        		/** MAC address */
        		macAddress.setMacAddress(peer.macAddress);
        		macAddress.write(mem, addr);
        		addr += macAddress.sizeof();

        		/** Padding */
        		mem.memset(addr, (byte) 0, 6);
        		addr += 6;

        		/** Timestamp */
        		mem.write64(addr, peer.timestamp);
        		addr += 8;
        	}

        	for (int nextAddr = buf.getAddress(); nextAddr < addr; nextAddr += peerInfoSize) {
        		if (nextAddr + peerInfoSize >= addr) {
        			// Last one
        			mem.write32(nextAddr, 0);
        		} else {
        			// Pointer to next one
        			mem.write32(nextAddr, nextAddr + peerInfoSize);
        		}
        	}
        }

        // Update peer list
        triggerAdhocctlThread();

        return 0;
    }

    /**
     * Get peer information
     *
     * @param mac - The mac address of the peer.
     * @param size - Size of peerinfo.
     * @param peerinfo - Pointer to store the information.
     *
     * @return 0 on success, < 0 on error.
     */
    @HLEFunction(nid = 0x8DB83FDC, version = 150)
    public void sceNetAdhocctlGetPeerInfo(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNetAdhocctlGetPeerInfo");

        // Update peer list
        triggerAdhocctlThread();

        cpu.gpr[2] = 0;
    }

    /**
     * Get mac address from nickname
     *
     * @param nickname - The nickname.
     * @param length - The length of the list.
     * @param buf - An allocated area of size length.
     *
     * @return 0 on success, < 0 on error.
     */
    @HLEFunction(nid = 0x99560ABE, version = 150)
    public void sceNetAdhocctlGetAddrByName(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNetAdhocctlGetAddrByName");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    /**
     * Get nickname from a mac address
     *
     * @param mac - The mac address.
     * @param nickname - Pointer to a char buffer where the nickname will be stored.
     *
     * @return 0 on success, < 0 on error.
     */
    @HLEFunction(nid = 0x8916C003, version = 150)
    public void sceNetAdhocctlGetNameByAddr(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNetAdhocctlGetNameByAddr");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    /**
     * Get Adhocctl parameter
     *
     * @param params - Pointer to a ::SceNetAdhocctlParams
     *
     * @return 0 on success, < 0 on error.
     */
    @HLEFunction(nid = 0xDED9D28E, version = 150)
    public void sceNetAdhocctlGetParameter(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNetAdhocctlGetParameter");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    /**
     * Get the results of a scan
     *
     * @param length - The length of the list.
     * @param buf - An allocated area of size length.
     *
     * @return 0 on success, < 0 on error.
     */
    @HLEFunction(nid = 0x81AEE1BE, version = 150)
    public void sceNetAdhocctlGetScanInfo(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNetAdhocctlGetScanInfo");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    /**
     * Connect to the Adhoc control game mode (as a host)
     *
     * @param name - The name of the connection (maximum 8 alphanumeric characters).
     * @param unknown - Pass 1.
     * @param num - The total number of players (including the host).
     * @param macs - A pointer to a list of the participating mac addresses, host first, then clients.
     * @param timeout - Timeout in microseconds.
     * @param unknown2 - pass 0.
     *
     * @return 0 on success, < 0 on error.
     */
    @HLEFunction(nid = 0xA5C055CE, version = 150)
    public void sceNetAdhocctlCreateEnterGameMode(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNetAdhocctlCreateEnterGameMode");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    @HLEFunction(nid = 0xB0B80E80, version = 150)
    public void sceNetAdhocctlCreateEnterGameModeMin(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNetAdhocctlCreateEnterGameModeMin");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    /**
     * Connect to the Adhoc control game mode (as a client)
     *
     * @param name - The name of the connection (maximum 8 alphanumeric characters).
     * @param hostmac - The mac address of the host.
     * @param timeout - Timeout in microseconds.
     * @param unknown - pass 0.
     *
     * @return 0 on success, < 0 on error.
     */
    @HLEFunction(nid = 0x1FF89745, version = 150)
    public void sceNetAdhocctlJoinEnterGameMode(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNetAdhocctlJoinEnterGameMode");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    /**
     * Exit game mode.
     *
     * @return 0 on success, < 0 on error.
     */
    @HLEFunction(nid = 0xCF8E084D, version = 150)
    public void sceNetAdhocctlExitGameMode(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNetAdhocctlExitGameMode");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    /**
     * Get game mode information
     *
     * @param gamemodeinfo - Pointer to store the info.
     *
     * @return 0 on success, < 0 on error.
     */
    @HLEFunction(nid = 0x5A014CE0, version = 150)
    public void sceNetAdhocctlGetGameModeInfo(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNetAdhocctlGetGameModeInfo");

        cpu.gpr[2] = 0xDEADC0DE;
    }
}