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

import static jpcsp.HLE.modules150.sceNet.convertMacAddressToString;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.HashMap;

import jpcsp.HLE.CanBeNull;
import jpcsp.HLE.CheckArgument;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.SceKernelErrorException;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer32;
import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.Processor;
import jpcsp.Allegrex.Common;
import jpcsp.Allegrex.CpuState;
import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.kernel.types.SceKernelThreadInfo;
import jpcsp.HLE.kernel.types.pspNetMacAddress;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.HLE.modules.ThreadManForUser;
import jpcsp.HLE.modules150.SysMemUserForUser.SysMemInfo;
import jpcsp.HLE.modules150.sceNetAdhoc.AdhocMessage;
import jpcsp.util.Utilities;

import org.apache.log4j.Logger;

public class sceNetAdhocMatching extends HLEModule {
    protected static Logger log = Modules.getLogger("sceNetAdhocMatching");
    private int currentMatchingId;
    protected HashMap<Integer, MatchingObject> matchingObjects;
    private static final int loopThreadRegisterArgument = Common._s0; // $s0 is presserved across calls

    /**
     * Matching events used in pspAdhocMatchingCallback
     */
    /** Hello event. optdata contains data if optlen > 0. */
    public static final int PSP_ADHOC_MATCHING_EVENT_HELLO = 1;
    /** Join request. optdata contains data if optlen > 0. */
    public static final int PSP_ADHOC_MATCHING_EVENT_JOIN = 2;
    /** Target left matching. */
    public static final int PSP_ADHOC_MATCHING_EVENT_LEFT = 3;
    /** Join request rejected. */
    public static final int PSP_ADHOC_MATCHING_EVENT_REJECT = 4;
    /** Join request cancelled. */
    public static final int PSP_ADHOC_MATCHING_EVENT_CANCEL = 5;
    /** Join request accepted. optdata contains data if optlen > 0. */
    public static final int PSP_ADHOC_MATCHING_EVENT_ACCEPT = 6;
    /** Matching is complete. */
    public static final int PSP_ADHOC_MATCHING_EVENT_COMPLETE = 7;
    /** Ping timeout event. */
    public static final int PSP_ADHOC_MATCHING_EVENT_TIMEOUT = 8;
    /** Error event. */
    public static final int PSP_ADHOC_MATCHING_EVENT_ERROR = 9;
    /** Peer disconnect event. */
    public static final int PSP_ADHOC_MATCHING_EVENT_DISCONNECT = 10;
    /** Data received event. optdata contains data if optlen > 0. */
    public static final int PSP_ADHOC_MATCHING_EVENT_DATA = 11;
    /** Data acknowledged event. */
    public static final int PSP_ADHOC_MATCHING_EVENT_DATA_CONFIRM = 12;
    /** Data timeout event. */
    public static final int PSP_ADHOC_MATCHING_EVENT_DATA_TIMEOUT = 13;

    /** Internal ping message. */
    private static final int PSP_ADHOC_MATCHING_EVENT_INTERNAL_PING = 100;

    /**
     * Matching modes used in sceNetAdhocMatchingCreate
     */
    /** Host */
    public static final int PSP_ADHOC_MATCHING_MODE_HOST = 1;
    /** Client */
    public static final int PSP_ADHOC_MATCHING_MODE_CLIENT = 2;
    /** Peer to peer */
    public static final int PSP_ADHOC_MATCHING_MODE_PTP = 3;

    /**
     * An AdhocMatchingEventMessage is consisting of:
     * - 6 bytes for the MAC address of the message sender
     * - 1 byte for the event PSP_ADHOC_MATCHING_EVENT_xxx
     * - n bytes for the message data
     */
    protected static class AdhocMatchingEventMessage extends AdhocMessage {
    	protected int event;
    	protected static final int HEADER_SIZE_MATCHING = HEADER_SIZE + 1;

    	public AdhocMatchingEventMessage(byte[] message, int length) {
    		if (length >= HEADER_SIZE_MATCHING) {
    			System.arraycopy(message, 0, fromMacAddress, 0, fromMacAddress.length);
    			System.arraycopy(message, fromMacAddress.length, toMacAddress, 0, toMacAddress.length);
    			event = message[HEADER_SIZE] & 0xFF;
    			data = new byte[length - HEADER_SIZE_MATCHING];
    			System.arraycopy(message, HEADER_SIZE_MATCHING, data, 0, data.length);
    		}
		}

		public AdhocMatchingEventMessage(int address, int length, int event) {
			super(address, length);
			this.event = event;
		}

		public AdhocMatchingEventMessage(int address, int length, byte[] toMacAddress, int event) {
			super(address, length, toMacAddress);
			this.event = event;
		}

		public int getEvent() {
			return event;
		}

		@Override
		public byte[] getMessage() {
    		byte[] message = new byte[getMessageLength()];
    		System.arraycopy(fromMacAddress, 0, message, 0, fromMacAddress.length);
    		System.arraycopy(toMacAddress, 0, message, fromMacAddress.length, toMacAddress.length);
    		message[HEADER_SIZE] = (byte) event;
    		System.arraycopy(data, 0, message, HEADER_SIZE_MATCHING, data.length);

    		return message;
		}

		@Override
		public int getMessageLength() {
			return super.getMessageLength() + (HEADER_SIZE_MATCHING - HEADER_SIZE);
		}

		@Override
		public String toString() {
			return String.format("AdhocMatchingEventMessage[fromMacAddress=%s, toMacAddress=%d, event=%d, dataLength=%d]", convertMacAddressToString(fromMacAddress), convertMacAddressToString(toMacAddress), getEvent(), getDataLength());
		}
    }

    private class MatchingObject {
    	private final int id;
    	private int mode;
    	private int maxPeers;
    	private int port;
    	private int bufSize;
    	private SysMemInfo buf;
    	private int helloDelay;
    	private int pingDelay;
    	private int initCount;
    	private int msgDelay;
    	private int callback;
    	private DatagramSocket socket;
    	private boolean started;
    	private long lastHelloMicros;
    	private long lastPingMicros;
    	private int helloOptLen;
    	private int helloOptData;
    	private SceKernelThreadInfo eventThread;
    	private SceKernelThreadInfo inputThread;
    	private byte[] pendingJoinRequest;
    	private boolean inConnection;
    	private boolean connected;
    	private boolean pendingComplete;

    	public MatchingObject() {
    		id = currentMatchingId++;
    	}

		public int getMode() {
			return mode;
		}

		public void setMode(int mode) {
			this.mode = mode;
		}

		public int getMaxPeers() {
			return maxPeers;
		}

		public void setMaxPeers(int maxPeers) {
			this.maxPeers = maxPeers;
		}

		public int getPort() {
			return port;
		}

		public void setPort(int port) {
			this.port = port;
		}

		public int getBufSize() {
			return bufSize;
		}

		public void setBufSize(int bufSize) {
			this.bufSize = bufSize;
			if (buf != null) {
				Modules.SysMemUserForUserModule.free(buf);
				buf = null;
			}
			buf = Modules.SysMemUserForUserModule.malloc(SysMemUserForUser.USER_PARTITION_ID, getName(), SysMemUserForUser.PSP_SMEM_Low, bufSize, 0);
		}

		public int getHelloDelay() {
			return helloDelay;
		}

		public void setHelloDelay(int helloDelay) {
			this.helloDelay = helloDelay;
		}

		public int getPingDelay() {
			return pingDelay;
		}

		public void setPingDelay(int pingDelay) {
			this.pingDelay = pingDelay;
		}

		public int getInitCount() {
			return initCount;
		}

		public void setInitCount(int initCount) {
			this.initCount = initCount;
		}

		public int getMsgDelay() {
			return msgDelay;
		}

		public void setMsgDelay(int msgDelay) {
			this.msgDelay = msgDelay;
		}

		public int getCallback() {
			return callback;
		}

		public void setCallback(int callback) {
			this.callback = callback;
		}

		public int getId() {
			return id;
		}

		public int start(int evthPri, int evthStack, int inthPri, int inthStack, int optLen, int optData) {
			try {
				openSocket();
				helloOptLen = optLen;
				helloOptData = optData;

	            ThreadManForUser threadMan = Modules.ThreadManForUserModule;
				if (eventThread == null) {
		            eventThread = threadMan.hleKernelCreateThread("SceNetAdhocMatchingEvent",
		                          ThreadManForUser.NET_ADHOC_MATCHING_EVENT_LOOP_ADDRESS,
					              evthPri, evthStack, threadMan.getCurrentThread().attr, 0);
		            eventThread.cpuContext.gpr[loopThreadRegisterArgument] = getId();
					threadMan.hleKernelStartThread(eventThread, 0, 0, eventThread.gpReg_addr);
				}
				if (inputThread == null) {
					inputThread = threadMan.hleKernelCreateThread("SceNetAdhocMatchingInput",
					              ThreadManForUser.NET_ADHOC_MATCHING_INPUT_LOOP_ADDRESS,
					              inthPri, inthStack, threadMan.getCurrentThread().attr, 0);
					inputThread.cpuContext.gpr[loopThreadRegisterArgument] = getId();
					threadMan.hleKernelStartThread(inputThread, 0, 0, inputThread.gpReg_addr);
				}

				started = true;
			} catch (SocketException e) {
				log.error("start", e);
			} catch (IOException e) {
				log.error("start", e);
			}

			return 0;
		}

		public int delete() {
			closeSocket();

			if (buf != null) {
				Modules.SysMemUserForUserModule.free(buf);
				buf = null;
			}

			return 0;
		}

		public int stop() {
			closeSocket();

			return 0;
		}

		private void openSocket() throws SocketException, UnknownHostException {
			if (socket == null) {
				int realPort = port + Modules.sceNetAdhocModule.netServerPortShift;
				socket = new DatagramSocket(realPort);
				if (log.isDebugEnabled()) {
					log.debug(String.format("Listening on port %d(%d)", port, realPort));
				}
				socket.setBroadcast(true);
				socket.setSoTimeout(1);
			}
		}

		private void sendHello(int optLen, int optData) throws IOException {
			int realPort = port + Modules.sceNetAdhocModule.netClientPortShift;
			InetSocketAddress broadcastAddress = sceNetInet.getBroadcastInetSocketAddress(realPort);
			if (log.isDebugEnabled()) {
				log.debug(String.format("Sending hello to port %d(%d)", port, realPort));
			}
			AdhocMatchingEventMessage adhocMatchingEventMessage = new AdhocMatchingEventMessage(optData, optLen, PSP_ADHOC_MATCHING_EVENT_HELLO);
			DatagramPacket packet = new DatagramPacket(adhocMatchingEventMessage.getMessage(), adhocMatchingEventMessage.getMessageLength(), broadcastAddress);
			socket.send(packet);

			lastHelloMicros = Emulator.getClock().microTime();
		}

		private void sendPing() throws IOException {
			int realPort = port + Modules.sceNetAdhocModule.netClientPortShift;
			InetSocketAddress broadcastAddress = sceNetInet.getBroadcastInetSocketAddress(realPort);
			if (log.isDebugEnabled()) {
				log.debug(String.format("Sending ping to port %d(%d)", port, realPort));
			}
			AdhocMatchingEventMessage adhocMatchingEventMessage = new AdhocMatchingEventMessage(0, 0, PSP_ADHOC_MATCHING_EVENT_INTERNAL_PING);
			DatagramPacket packet = new DatagramPacket(adhocMatchingEventMessage.getMessage(), adhocMatchingEventMessage.getMessageLength(), broadcastAddress);
			socket.send(packet);

			lastPingMicros = Emulator.getClock().microTime();
		}

		private void closeSocket() {
			started = false;
			if (socket != null) {
				socket.close();
				socket = null;
			}
		}

		public int send(pspNetMacAddress macAddress, int dataLen, int data) {
			// Faked, all data sent
			return dataLen;
		}

		public int selectTarget(pspNetMacAddress macAddress, int optLen, int optData) {
			int result = 0;

			try {
				openSocket();
				int realPort = port + Modules.sceNetAdhocModule.netClientPortShift;
				InetSocketAddress broadcastAddress = sceNetInet.getBroadcastInetSocketAddress(realPort);
				int event;
				if (pendingJoinRequest != null && sceNetAdhoc.isSameMacAddress(pendingJoinRequest, macAddress.macAddress)) {
					event = PSP_ADHOC_MATCHING_EVENT_ACCEPT;
					if (log.isDebugEnabled()) {
						log.debug(String.format("Sending accept to port %d(%d)", port, realPort));
					}
				} else {
					event = PSP_ADHOC_MATCHING_EVENT_JOIN;
					if (log.isDebugEnabled()) {
						log.debug(String.format("Sending join to port %d(%d)", port, realPort));
					}
				}
				AdhocMatchingEventMessage adhocMatchingEventMessage = new AdhocMatchingEventMessage(optData, optLen, macAddress.macAddress, event);
				DatagramPacket packet = new DatagramPacket(adhocMatchingEventMessage.getMessage(), adhocMatchingEventMessage.getMessageLength(), broadcastAddress);
				socket.send(packet);

				inConnection = true;
			} catch (SocketException e) {
				log.error("selectTarget", e);
			} catch (UnknownHostException e) {
				log.error("selectTarget", e);
			} catch (IOException e) {
				log.error("selectTarget", e);
			}

			return result;
		}

		public int cancelTarget(pspNetMacAddress macAddress) {
			return cancelTarget(macAddress, 0, 0);
		}

		public int cancelTarget(pspNetMacAddress macAddress, int optLen, int optData) {
			return 0;
		}

	    private void notifyCallbackEvent(int event, int macAddr, int optLen, int optData) {
	    	if (getCallback() == 0) {
	    		return;
	    	}

	    	if (log.isDebugEnabled()) {
	    		log.debug(String.format("Notify callback 0x%08X, event=%d, macAddr=0x%08X, optLen=%d, optData=0x%08X", getCallback(), event, macAddr, optLen, optData));
	    	}
	    	Modules.ThreadManForUserModule.executeCallback(eventThread, getCallback(), null, true, getId(), event, macAddr, optLen, optData);
	    }

		public boolean eventLoop() {
			if (socket == null || !started) {
				return false;
			}

			if (!inConnection) {
				if (connected) {
					if (Emulator.getClock().microTime() - lastPingMicros >= getPingDelay()) {
						try {
							sendPing();
						} catch (IOException e) {
							log.error("eventLoop ping", e);
						}
					}
				} else {
					if (Emulator.getClock().microTime() - lastHelloMicros >= getHelloDelay()) {
						try {
							sendHello(helloOptLen, helloOptData);
						} catch (IOException e) {
							log.error("eventLoop hello", e);
						}
					}
				}
			}

			return true;
		}

		public boolean inputLoop() {
			if (socket == null || !started) {
				return false;
			}

			try {
				byte[] bytes = new byte[AdhocMatchingEventMessage.getMessageLength(getBufSize())];
				DatagramPacket packet = new DatagramPacket(bytes, bytes.length);
				socket.receive(packet);
				AdhocMatchingEventMessage adhocMatchingEventMessage = new AdhocMatchingEventMessage(packet.getData(), packet.getLength());
				if (adhocMatchingEventMessage.isForMe()) {
					int event = adhocMatchingEventMessage.getEvent();
					int macAddr = buf.addr;
					int optData = buf.addr + 8;
					int optLen = adhocMatchingEventMessage.getDataLength();
					adhocMatchingEventMessage.writeDataToMemory(optData);
					pspNetMacAddress macAddress = new pspNetMacAddress();
					macAddress.setMacAddress(adhocMatchingEventMessage.getFromMacAddress());
					macAddress.write(Memory.getInstance(), macAddr);
					if (log.isDebugEnabled()) {
						log.debug(String.format("Received message length=%d, type=%d, fromMac=%s, port=%d: %s", adhocMatchingEventMessage.getDataLength(), event, macAddress, packet.getPort(), Utilities.getMemoryDump(optData, optLen, 4, 16)));
					}
					if (event == PSP_ADHOC_MATCHING_EVENT_JOIN) {
						pendingJoinRequest = adhocMatchingEventMessage.getFromMacAddress();
						inConnection = true;
					}
					notifyCallbackEvent(event, macAddr, optLen, optData);

					if (event == PSP_ADHOC_MATCHING_EVENT_ACCEPT) {
						int realPort = port + Modules.sceNetAdhocModule.netClientPortShift;
						InetSocketAddress broadcastAddress = sceNetInet.getBroadcastInetSocketAddress(realPort);
						if (log.isDebugEnabled()) {
							log.debug(String.format("Sending complete to port %d(%d)", port, realPort));
						}
						adhocMatchingEventMessage = new AdhocMatchingEventMessage(optData, optLen, macAddress.macAddress, PSP_ADHOC_MATCHING_EVENT_COMPLETE);
						packet = new DatagramPacket(adhocMatchingEventMessage.getMessage(), adhocMatchingEventMessage.getMessageLength(), broadcastAddress);
						socket.send(packet);

						notifyCallbackEvent(PSP_ADHOC_MATCHING_EVENT_COMPLETE, macAddr, optLen, optData);

						pendingComplete = true;
						connected = true;
						inConnection = false;
					} else if (event == PSP_ADHOC_MATCHING_EVENT_COMPLETE) {
						if (!pendingComplete) {
							int realPort = port + Modules.sceNetAdhocModule.netClientPortShift;
							InetSocketAddress broadcastAddress = sceNetInet.getBroadcastInetSocketAddress(realPort);
							if (log.isDebugEnabled()) {
								log.debug(String.format("Sending complete to port %d(%d)", port, realPort));
							}
							adhocMatchingEventMessage = new AdhocMatchingEventMessage(optData, optLen, macAddress.macAddress, PSP_ADHOC_MATCHING_EVENT_COMPLETE);
							packet = new DatagramPacket(adhocMatchingEventMessage.getMessage(), adhocMatchingEventMessage.getMessageLength(), broadcastAddress);
							socket.send(packet);
						}
						connected = true;
						inConnection = false;
					}
				}
			} catch (SocketTimeoutException e) {
				// Nothing available
				if (log.isTraceEnabled()) {
					log.trace(String.format("Sync: nothing available on port %d", socket.getPort()));
				}
			} catch (IOException e) {
				log.error("inputLoop", e);
			}

			return true;
		}

		@Override
		public String toString() {
			return String.format("MatchingObject[id=%d, mode=%d, maxPeers=%d, port=%d, callback=0x%08X]", id, mode, maxPeers, port, callback);
		}
    }

    @Override
    public String getName() {
        return "sceNetAdhocMatching";
    }

	@Override
	public void start() {
		matchingObjects = new HashMap<Integer, sceNetAdhocMatching.MatchingObject>();
		currentMatchingId = 1;

		super.start();
	}

	public int checkMatchingId(int matchingId) {
		if (!matchingObjects.containsKey(matchingId)) {
			throw new SceKernelErrorException(SceKernelErrors.ERROR_NET_ADHOC_INVALID_MATCHING_ID);
		}

		return matchingId;
	}

	public void hleNetAdhocMatchingEventThread(Processor processor) {
		if (log.isTraceEnabled()) {
			log.trace(String.format("hleNetAdhocMatchingEventThread"));
		}

		int matchingId = processor.cpu.gpr[loopThreadRegisterArgument];
		MatchingObject matchingObject = matchingObjects.get(matchingId);
		if (matchingObject != null && matchingObject.eventLoop()) {
			Modules.ThreadManForUserModule.hleKernelDelayThread(10000, false);
		} else {
			// Exit thread with status 0
			processor.cpu.gpr[Common._v0] = 0;
			Modules.ThreadManForUserModule.hleKernelExitDeleteThread();
		}
	}

	public void hleNetAdhocMatchingInputThread(Processor processor) {
		if (log.isTraceEnabled()) {
			log.trace(String.format("hleNetAdhocMatchingInputThread"));
		}

		int matchingId = processor.cpu.gpr[loopThreadRegisterArgument];
		MatchingObject matchingObject = matchingObjects.get(matchingId);
		if (matchingObject != null && matchingObject.inputLoop()) {
			Modules.ThreadManForUserModule.hleKernelDelayThread(10000, false);
		} else {
			// Exit thread with status 0
			processor.cpu.gpr[Common._v0] = 0;
			Modules.ThreadManForUserModule.hleKernelExitDeleteThread();
		}
	}

	/**
     * Initialise the Adhoc matching library
     *
     * @param memsize - Internal memory pool size. Lumines uses 0x20000
     *
     * @return 0 on success, < 0 on error
     */
    @HLEFunction(nid = 0x2A2A1E07, version = 150)
    public int sceNetAdhocMatchingInit(int memsize) {
        log.warn(String.format("IGNORING: sceNetAdhocMatchingInit: memsize=0x%X", memsize));

		return 0;
    }

    /**
     * Terminate the Adhoc matching library
     *
     * @return 0 on success, < 0 on error
     */
    @HLEFunction(nid = 0x7945ECDA, version = 150)
    public int sceNetAdhocMatchingTerm() {
        log.warn("IGNORING: sceNetAdhocMatchingTerm");

        return 0;
    }

    /**
     * Create an Adhoc matching object
     *
     * @param mode - One of ::pspAdhocMatchingModes
     * @param maxpeers - Maximum number of peers to match (only used when mode is PSP_ADHOC_MATCHING_MODE_HOST)
     * @param port - Port. Lumines uses 0x22B
     * @param bufsize - Receiving buffer size
     * @param hellodelay - Hello message send delay in microseconds (only used when mode is PSP_ADHOC_MATCHING_MODE_HOST or PSP_ADHOC_MATCHING_MODE_PTP)
     * @param pingdelay - Ping send delay in microseconds. Lumines uses 0x5B8D80 (only used when mode is PSP_ADHOC_MATCHING_MODE_HOST or PSP_ADHOC_MATCHING_MODE_PTP)
     * @param initcount - Initial count of the of the resend counter. Lumines uses 3
     * @param msgdelay - Message send delay in microseconds
     * @param callback - Callback to be called for matching
     *
     * @return ID of object on success, < 0 on error.
     */
    @HLEFunction(nid = 0xCA5EDA6F, version = 150)
    public int sceNetAdhocMatchingCreate(int mode, int maxPeers, int port, int bufSize, int helloDelay, int pingDelay, int initCount, int msgDelay, @CanBeNull TPointer callback) {
        log.warn(String.format("UNIMPLEMENTED: sceNetAdhocMatchingCreate mode=%d, maxPeers=%d, port=%d, bufSize=%d, helloDelay=%d, pingDelay=%d, initCount=%d, msgDelay=%d, callback=%s", mode, maxPeers, port, bufSize, helloDelay, pingDelay, initCount, msgDelay, callback));

        MatchingObject matchingObject = new MatchingObject();
        matchingObject.setMode(mode);
        matchingObject.setMaxPeers(maxPeers);
        matchingObject.setPort(port);
        matchingObject.setBufSize(bufSize);
        matchingObject.setHelloDelay(helloDelay);
        matchingObject.setPingDelay(pingDelay);
        matchingObject.setInitCount(initCount);
        matchingObject.setMsgDelay(msgDelay);
        matchingObject.setCallback(callback.getAddress());
        matchingObjects.put(matchingObject.getId(), matchingObject);

        return matchingObject.getId();
    }

    /**
     * Start a matching object
     *
     * @param matchingid - The ID returned from ::sceNetAdhocMatchingCreate
     * @param evthpri - Priority of the event handler thread. Lumines uses 0x10
     * @param evthstack - Stack size of the event handler thread. Lumines uses 0x2000
     * @param inthpri - Priority of the input handler thread. Lumines uses 0x10
     * @param inthstack - Stack size of the input handler thread. Lumines uses 0x2000
     * @param optlen - Size of hellodata
     * @param optdata - Pointer to block of data passed to callback
     *
     * @return 0 on success, < 0 on error
     */
    @HLEFunction(nid = 0x93EF3843, version = 150)
    public int sceNetAdhocMatchingStart(@CheckArgument("checkMatchingId") int matchingId, int evthPri, int evthStack, int inthPri, int inthStack, int optLen, @CanBeNull TPointer optData) {
    	log.warn(String.format("UNIMPLEMENTED: sceNetAdhocMatchingStart matchingId=%d, evthPri=%d, evthStack=0x%X, inthPri=%d, inthStack=0x%X, optLen=%d, optData=%s: %s", matchingId, evthPri, evthStack, inthPri, inthStack, optLen, optData, Utilities.getMemoryDump(optData.getAddress(), optLen, 4, 16)));

        return matchingObjects.get(matchingId).start(evthPri, evthStack, inthPri, inthStack, optLen, optData.getAddress());
    }

    /**
     * Stop a matching object
     *
     * @param matchingid - The ID returned from ::sceNetAdhocMatchingCreate
     *
     * @return 0 on success, < 0 on error.
     */
    @HLEFunction(nid = 0x32B156B3, version = 150)
    public int sceNetAdhocMatchingStop(@CheckArgument("checkMatchingId") int matchingId) {
        log.warn(String.format("UNIMPLEMENTED: sceNetAdhocMatchingStop matchingId=%d", matchingId));

        return matchingObjects.get(matchingId).stop();
    }

    /**
     * Delete an Adhoc matching object
     *
     * @param matchingid - The ID returned from ::sceNetAdhocMatchingCreate
     *
     * @return 0 on success, < 0 on error.
     */
    @HLEFunction(nid = 0xF16EAF4F, version = 150)
    public int sceNetAdhocMatchingDelete(@CheckArgument("checkMatchingId") int matchingId) {
        log.warn(String.format("PARTIAL: sceNetAdhocMatchingDelete matchingId=%d", matchingId));

        return matchingObjects.remove(matchingId).delete();
    }

    /**
     * Send data to a matching target
     *
     * @param matchingid - The ID returned from ::sceNetAdhocMatchingCreate
     * @param mac - The MAC address to send the data to
     * @param datalen - Length of the data
     * @param data - Pointer to the data
     *
     * @return 0 on success, < 0 on error.
     */
    @HLEFunction(nid = 0xF79472D7, version = 150)
    public int sceNetAdhocMatchingSendData(@CheckArgument("checkMatchingId") int matchingId, TPointer macAddr, int dataLen, TPointer data) {
    	pspNetMacAddress macAddress = new pspNetMacAddress();
    	macAddress.read(Memory.getInstance(), macAddr.getAddress());
        log.warn(String.format("UNIMPLEMENTED: sceNetAdhocMatchingSendData matchingId=%d, macAddr=%s(%s), dataLen=%d, data=%s", matchingId, macAddr, macAddress, dataLen, data));

        return matchingObjects.get(matchingId).send(macAddress, dataLen, data.getAddress());
    }

    /**
     * Abort a data send to a matching target
     *
     * @param matchingid - The ID returned from ::sceNetAdhocMatchingCreate
     * @param mac - The MAC address to send the data to
     *
     * @return 0 on success, < 0 on error.
     */
    @HLEFunction(nid = 0xEC19337D, version = 150)
    public int sceNetAdhocMatchingAbortSendData(@CheckArgument("checkMatchingId") int matchingId, TPointer macAddr) {
    	pspNetMacAddress macAddress = new pspNetMacAddress();
    	macAddress.read(Memory.getInstance(), macAddr.getAddress());
        log.warn(String.format("UNIMPLEMENTED: sceNetAdhocMatchingAbortSendData matchingId=%d, macAddr=%s(%s)", matchingId, macAddr, macAddress));

        return 0;
    }

    /**
     * Select a matching target
     *
     * @param matchingid - The ID returned from ::sceNetAdhocMatchingCreate
     * @param mac - MAC address to select
     * @param optlen - Optional data length
     * @param optdata - Pointer to the optional data
     *
     * @return 0 on success, < 0 on error.
     */
    @HLEFunction(nid = 0x5E3D4B79, version = 150)
    public int sceNetAdhocMatchingSelectTarget(@CheckArgument("checkMatchingId") int matchingId, TPointer macAddr, int optLen, @CanBeNull TPointer optData) {
    	pspNetMacAddress macAddress = new pspNetMacAddress();
    	macAddress.read(Memory.getInstance(), macAddr.getAddress());
        log.warn(String.format("UNIMPLEMENTED: sceNetAdhocMatchingSelectTarget matchingId=%d, macAddr=%s(%s), optLen=%d, optData=%s", matchingId, macAddr, macAddress, optLen, optData));

        return matchingObjects.get(matchingId).selectTarget(macAddress, optLen, optData.getAddress());
    }

    /**
     * Cancel a matching target
     *
     * @param matchingid - The ID returned from ::sceNetAdhocMatchingCreate
     * @param mac - The MAC address to cancel
     *
     * @return 0 on success, < 0 on error.
     */
    @HLEFunction(nid = 0xEA3C6108, version = 150)
    public int sceNetAdhocMatchingCancelTarget(@CheckArgument("checkMatchingId") int matchingId, TPointer macAddr) {
    	pspNetMacAddress macAddress = new pspNetMacAddress();
    	macAddress.read(Memory.getInstance(), macAddr.getAddress());
        log.warn(String.format("UNIMPLEMENTED: sceNetAdhocMatchingCancelTarget matchingId=%d, macAddr=%s(%s)", matchingId, macAddr, macAddress));

        return matchingObjects.get(matchingId).cancelTarget(macAddress);
    }

    /**
     * Cancel a matching target (with optional data)
     *
     * @param matchingid - The ID returned from ::sceNetAdhocMatchingCreate
     * @param mac - The MAC address to cancel
     * @param optlen - Optional data length
     * @param optdata - Pointer to the optional data
     *
     * @return 0 on success, < 0 on error.
     */
    @HLEFunction(nid = 0x8F58BEDF, version = 150)
    public int sceNetAdhocMatchingCancelTargetWithOpt(@CheckArgument("checkMatchingId") int matchingId, TPointer macAddr, int optLen, @CanBeNull TPointer optData) {
    	pspNetMacAddress macAddress = new pspNetMacAddress();
    	macAddress.read(Memory.getInstance(), macAddr.getAddress());
        log.warn(String.format("UNIMPLEMENTED: sceNetAdhocMatchingCancelTargetWithOpt matchingId=%d, macAddr=%s(%s), optLen=%d, optData=%s", matchingId, macAddr, macAddress, optLen, optData));

        return matchingObjects.get(matchingId).cancelTarget(macAddress, optLen, optData.getAddress());
    }

    /**
     * Get the optional hello message
     *
     * @param matchingid - The ID returned from ::sceNetAdhocMatchingCreate
     * @param optlen - Length of the hello data
     * @param optdata - Pointer to the hello data
     *
     * @return 0 on success, < 0 on error.
     */
    @HLEFunction(nid = 0xB5D96C2A, version = 150)
    public void sceNetAdhocMatchingGetHelloOpt(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNetAdhocMatchingGetHelloOpt");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    /**
     * Set the optional hello message
     *
     * @param matchingid - The ID returned from ::sceNetAdhocMatchingCreate
     * @param optlen - Length of the hello data
     * @param optdata - Pointer to the hello data
     *
     * @return 0 on success, < 0 on error.
     */
    @HLEFunction(nid = 0xB58E61B7, version = 150)
    public void sceNetAdhocMatchingSetHelloOpt(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNetAdhocMatchingSetHelloOpt");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    /**
     * Get a list of matching members
     *
     * @param matchingid - The ID returned from ::sceNetAdhocMatchingCreate
     * @param length - The length of the list.
     * @param buf - An allocated area of size length.
     *
     * @return 0 on success, < 0 on error.
     */
    @HLEFunction(nid = 0xC58BCD9E, version = 150)
    public int sceNetAdhocMatchingGetMembers(@CheckArgument("checkMatchingId") int matchingId, TPointer32 lengthAddr, @CanBeNull TPointer buf) {
        log.warn(String.format("UNIMPLEMENTED: sceNetAdhocMatchingGetMembers matchingId=%d, lengthAddr=%s(%d), buf=%s", matchingId, lengthAddr.toString(), lengthAddr.getValue(), buf.toString()));

        lengthAddr.setValue(0);

        return 0;
    }

    /**
     * Get the status of the memory pool used by the matching library
     *
     * @param poolstat - A ::pspAdhocPoolStat.
     *
     * @return 0 on success, < 0 on error.
     */
    @HLEFunction(nid = 0x9C5CFB7D, version = 150)
    public void sceNetAdhocMatchingGetPoolStat(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNetAdhocMatchingGetPoolStat");

        cpu.gpr[2] = 0xDEADC0DE;
    }

    /**
     * Get the maximum memory usage by the matching library
     *
     * @return The memory usage on success, < 0 on error.
     */
    @HLEFunction(nid = 0x40F8F435, version = 150)
    public void sceNetAdhocMatchingGetPoolMaxAlloc(Processor processor) {
        CpuState cpu = processor.cpu;

        log.warn("UNIMPLEMENTED: sceNetAdhocMatchingGetPoolMaxAlloc");

        cpu.gpr[2] = 0xDEADC0DE;
    }
}