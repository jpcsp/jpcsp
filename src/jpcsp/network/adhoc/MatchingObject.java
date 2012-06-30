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
package jpcsp.network.adhoc;

import static jpcsp.HLE.modules150.sceNetAdhocMatching.PSP_ADHOC_MATCHING_EVENT_ACCEPT;
import static jpcsp.HLE.modules150.sceNetAdhocMatching.PSP_ADHOC_MATCHING_EVENT_CANCEL;
import static jpcsp.HLE.modules150.sceNetAdhocMatching.PSP_ADHOC_MATCHING_EVENT_COMPLETE;
import static jpcsp.HLE.modules150.sceNetAdhocMatching.PSP_ADHOC_MATCHING_EVENT_DATA;
import static jpcsp.HLE.modules150.sceNetAdhocMatching.PSP_ADHOC_MATCHING_EVENT_DATA_CONFIRM;
import static jpcsp.HLE.modules150.sceNetAdhocMatching.PSP_ADHOC_MATCHING_EVENT_DISCONNECT;
import static jpcsp.HLE.modules150.sceNetAdhocMatching.PSP_ADHOC_MATCHING_EVENT_HELLO;
import static jpcsp.HLE.modules150.sceNetAdhocMatching.PSP_ADHOC_MATCHING_EVENT_INTERNAL_PING;
import static jpcsp.HLE.modules150.sceNetAdhocMatching.PSP_ADHOC_MATCHING_EVENT_JOIN;
import static jpcsp.HLE.modules150.sceNetAdhocMatching.PSP_ADHOC_MATCHING_EVENT_LEFT;
import static jpcsp.HLE.modules150.sceNetAdhocMatching.PSP_ADHOC_MATCHING_MODE_CLIENT;

import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.List;

import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.types.SceKernelThreadInfo;
import jpcsp.HLE.kernel.types.pspNetMacAddress;
import jpcsp.HLE.modules.ThreadManForUser;
import jpcsp.HLE.modules.sceNetAdhocMatching;
import jpcsp.HLE.modules150.sceNetAdhoc;
import jpcsp.hardware.Wlan;
import jpcsp.memory.IMemoryReader;
import jpcsp.memory.MemoryReader;
import jpcsp.network.INetworkAdapter;
import jpcsp.util.Utilities;

/**
 * @author gid15
 *
 */
public abstract class MatchingObject extends AdhocObject {
	private int mode;
	private int maxPeers;
	private int helloDelay;
	private int pingDelay;
	private int initCount;
	private int msgDelay;
	private int callback;
	private boolean started;
	private long lastHelloMicros;
	private long lastPingMicros;
	private byte[] helloOptData;
	private SceKernelThreadInfo eventThread;
	private SceKernelThreadInfo inputThread;
	private byte[] pendingJoinRequest;
	private boolean inConnection;
	private boolean connected;
	private boolean pendingComplete;
	private LinkedList<pspNetMacAddress> members = new LinkedList<pspNetMacAddress>();

	public MatchingObject(INetworkAdapter networkAdapter) {
		super(networkAdapter);
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

	public List<pspNetMacAddress> getMembers() {
		return members;
	}

	public void create() {
	}

	public int start(int evthPri, int evthStack, int inthPri, int inthStack, int optLen, int optData) {
		try {
			setHelloOpt(optLen, optData);
			openSocket();

            ThreadManForUser threadMan = Modules.ThreadManForUserModule;
			if (eventThread == null) {
	            eventThread = threadMan.hleKernelCreateThread("SceNetAdhocMatchingEvent",
	                          ThreadManForUser.NET_ADHOC_MATCHING_EVENT_LOOP_ADDRESS,
				              evthPri, evthStack, threadMan.getCurrentThread().attr, 0);
	            eventThread.cpuContext.gpr[sceNetAdhocMatching.loopThreadRegisterArgument] = getId();
				threadMan.hleKernelStartThread(eventThread, 0, 0, eventThread.gpReg_addr);
			}
			if (inputThread == null) {
				inputThread = threadMan.hleKernelCreateThread("SceNetAdhocMatchingInput",
				              ThreadManForUser.NET_ADHOC_MATCHING_INPUT_LOOP_ADDRESS,
				              inthPri, inthStack, threadMan.getCurrentThread().attr, 0);
				inputThread.cpuContext.gpr[sceNetAdhocMatching.loopThreadRegisterArgument] = getId();
				threadMan.hleKernelStartThread(inputThread, 0, 0, inputThread.gpReg_addr);
			}

			// Add myself as the first member
			addMember(Wlan.getMacAddress());

			started = true;
		} catch (SocketException e) {
			log.error("start", e);
		} catch (UnknownHostException e) {
			log.error("start", e);
		} catch (IOException e) {
			log.error("start", e);
		}

		return 0;
	}

	public int stop() {
		if (connected) {
			if (log.isDebugEnabled()) {
				log.debug(String.format("Sending disconnect to port %d", getPort()));
			}

			try {
				AdhocMatchingEventMessage adhocMatchingEventMessage = createMessage(PSP_ADHOC_MATCHING_EVENT_DISCONNECT);
				send(adhocMatchingEventMessage);
			} catch (IOException e) {
				log.error("stop", e);
			}
		}

		closeSocket();
		removeMember(Wlan.getMacAddress());

		return 0;
	}

	private void sendHello() throws IOException {
		if (log.isDebugEnabled()) {
			log.debug(String.format("Sending hello to port %d", getPort()));
		}
		AdhocMatchingEventMessage adhocMatchingEventMessage = createMessage(PSP_ADHOC_MATCHING_EVENT_HELLO);
		if (getHelloOptLen() > 0) {
			adhocMatchingEventMessage.setData(helloOptData);
		}
		send(adhocMatchingEventMessage);

		lastHelloMicros = Emulator.getClock().microTime();
	}

	private void sendPing() throws IOException {
		if (log.isDebugEnabled()) {
			log.debug(String.format("Sending ping to port %d", getPort()));
		}
		AdhocMatchingEventMessage adhocMatchingEventMessage = createMessage(PSP_ADHOC_MATCHING_EVENT_INTERNAL_PING);
		send(adhocMatchingEventMessage);

		lastPingMicros = Emulator.getClock().microTime();
	}

	@Override
	protected void closeSocket() {
		super.closeSocket();
		started = false;
		connected = false;
		inConnection = false;
		pendingComplete = false;
	}

	public int send(pspNetMacAddress macAddress, int dataLen, int data) {
		int result = 0;

		try {
			AdhocMatchingEventMessage adhocMatchingEventMessage = createMessage(PSP_ADHOC_MATCHING_EVENT_DATA, data, dataLen, macAddress.macAddress);
			send(adhocMatchingEventMessage);
			result = dataLen;
		} catch (SocketException e) {
			log.error("send", e);
		} catch (UnknownHostException e) {
			log.error("send", e);
		} catch (IOException e) {
			log.error("send", e);
		}

		return result;
	}

	public int selectTarget(pspNetMacAddress macAddress, int optLen, int optData) {
		int result = 0;

		try {
			int event;
			if (pendingJoinRequest != null && sceNetAdhoc.isSameMacAddress(pendingJoinRequest, macAddress.macAddress)) {
				event = PSP_ADHOC_MATCHING_EVENT_ACCEPT;
				if (log.isDebugEnabled()) {
					log.debug(String.format("Sending accept to port %d", getPort()));
				}
			} else {
				event = PSP_ADHOC_MATCHING_EVENT_JOIN;
				if (log.isDebugEnabled()) {
					log.debug(String.format("Sending join to port %d", getPort()));
				}
			}
			AdhocMatchingEventMessage adhocMatchingEventMessage = createMessage(event, optData, optLen, macAddress.macAddress);
			send(adhocMatchingEventMessage);

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
		int result = 0;

		try {
			int event;
			if (connected) {
				event = PSP_ADHOC_MATCHING_EVENT_LEFT;
				if (log.isDebugEnabled()) {
					log.debug(String.format("Sending leave to port %d", getPort()));
				}
			} else {
				event = PSP_ADHOC_MATCHING_EVENT_CANCEL;
				if (log.isDebugEnabled()) {
					log.debug(String.format("Sending cancel to port %d", getPort()));
				}
			}
			AdhocMatchingEventMessage adhocMatchingEventMessage = createMessage(event, optData, optLen, macAddress.macAddress);
			send(adhocMatchingEventMessage);
		} catch (SocketException e) {
			log.error("cancelTarget", e);
		} catch (UnknownHostException e) {
			log.error("cancelTarget", e);
		} catch (IOException e) {
			log.error("cancelTarget", e);
		}
		removeMember(macAddress.macAddress);
		connected = false;
		inConnection = false;

		return result;
	}

	public int getHelloOptLen() {
		return helloOptData == null ? 0 : helloOptData.length;
	}

	public byte[] getHelloOptData() {
		return helloOptData;
	}

	public void setHelloOpt(int optLen, int optData) {
		if (optLen <= 0 || optData == 0) {
			this.helloOptData = null;
			return;
		}

		// Copy the HelloOpt into an internal buffer, the user memory can be overwritten
		// after this call.
		IMemoryReader memoryReader = MemoryReader.getMemoryReader(optData, optLen, 1);
		helloOptData = new byte[optLen];
		for (int i = 0; i < optLen; i++) {
			helloOptData[i] = (byte) memoryReader.readNext();
		}
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
			} else if (getMode() != PSP_ADHOC_MATCHING_MODE_CLIENT) {
				if (Emulator.getClock().microTime() - lastHelloMicros >= getHelloDelay()) {
					try {
						sendHello();
					} catch (IOException e) {
						log.error("eventLoop hello", e);
					}
				}
			}
		}

		return true;
	}

	private void addMember(byte[] macAddr) {
		for (pspNetMacAddress member : members) {
			if (sceNetAdhoc.isSameMacAddress(macAddr, member.macAddress)) {
				// Already in the members list
				return;
			}
		}

		pspNetMacAddress member = new pspNetMacAddress();
		member.setMacAddress(macAddr);
		members.add(member);

		if (log.isDebugEnabled()) {
			log.debug(String.format("Adding member %s", member));
		}
	}

	private void removeMember(byte[] macAddr) {
		for (pspNetMacAddress member : members) {
			if (sceNetAdhoc.isSameMacAddress(macAddr, member.macAddress)) {
				if (log.isDebugEnabled()) {
					log.debug(String.format("Removing member %s", member));
				}
				members.remove(member);
				break;
			}
		}
	}

	public boolean inputLoop() {
		if (socket == null || !started) {
			return false;
		}

		try {
			byte[] bytes = new byte[getBufSize() + AdhocMessage.MAX_HEADER_SIZE];
			int length = socket.receive(bytes, bytes.length);
			AdhocMatchingEventMessage adhocMatchingEventMessage = createMessage(bytes, length);
			if (adhocMatchingEventMessage.isForMe()) {
				int event = adhocMatchingEventMessage.getEvent();
				int macAddr = buffer.addr;
				int optData = buffer.addr + 8;
				int optLen = adhocMatchingEventMessage.getDataLength();
				adhocMatchingEventMessage.writeDataToMemory(optData);
				pspNetMacAddress macAddress = new pspNetMacAddress();
				macAddress.setMacAddress(adhocMatchingEventMessage.getFromMacAddress());
				macAddress.write(Memory.getInstance(), macAddr);
				if (log.isDebugEnabled()) {
					log.debug(String.format("Received message length=%d, type=%d, fromMac=%s, port=%d: %s", adhocMatchingEventMessage.getDataLength(), event, macAddress, socket.getReceivedPort(), Utilities.getMemoryDump(optData, optLen, 4, 16)));
				}
				if (event == PSP_ADHOC_MATCHING_EVENT_JOIN) {
					pendingJoinRequest = adhocMatchingEventMessage.getFromMacAddress();
					inConnection = true;
				}
				if (event != PSP_ADHOC_MATCHING_EVENT_INTERNAL_PING) {
					notifyCallbackEvent(event, macAddr, optLen, optData);
				}

				if (event == PSP_ADHOC_MATCHING_EVENT_ACCEPT) {
					addMember(adhocMatchingEventMessage.getFromMacAddress());
					if (log.isDebugEnabled()) {
						log.debug(String.format("Sending complete to port %d", getPort()));
					}
					adhocMatchingEventMessage = createMessage(PSP_ADHOC_MATCHING_EVENT_COMPLETE, optData, optLen, macAddress.macAddress);
					send(adhocMatchingEventMessage);

					pendingComplete = true;
					connected = true;
					inConnection = false;
				} else if (event == PSP_ADHOC_MATCHING_EVENT_COMPLETE) {
					addMember(adhocMatchingEventMessage.getFromMacAddress());
					if (!pendingComplete) {
						if (log.isDebugEnabled()) {
							log.debug(String.format("Sending complete to port %d", getPort()));
						}
						adhocMatchingEventMessage = createMessage(PSP_ADHOC_MATCHING_EVENT_COMPLETE, optData, optLen, macAddress.macAddress);
						send(adhocMatchingEventMessage);
					}
					connected = true;
					inConnection = false;
				} else if (event == PSP_ADHOC_MATCHING_EVENT_DATA) {
					if (log.isDebugEnabled()) {
						log.debug(String.format("Sending data confirm to port %d", getPort()));
					}
					adhocMatchingEventMessage = createMessage(PSP_ADHOC_MATCHING_EVENT_DATA_CONFIRM, 0, 0, macAddress.macAddress);
					send(adhocMatchingEventMessage);
				} else if (event == PSP_ADHOC_MATCHING_EVENT_DISCONNECT || event == PSP_ADHOC_MATCHING_EVENT_LEFT) {
					if (log.isDebugEnabled()) {
						log.debug(String.format("Received disconnect/leave from %s", macAddress));
					}
					removeMember(adhocMatchingEventMessage.getFromMacAddress());
					if (members.size() <= 1) {
						connected = false;
						inConnection = false;
					}
				}
			}
		} catch (SocketTimeoutException e) {
			// Nothing available
			if (log.isTraceEnabled()) {
				log.trace(String.format("Sync: nothing available on port %d", getPort()));
			}
		} catch (IOException e) {
			log.error("inputLoop", e);
		}

		return true;
	}

	protected INetworkAdapter getNetworkAdapter() {
		return Modules.sceNetModule.getNetworkAdapter();
	}

	protected AdhocMatchingEventMessage createMessage(int event) {
		return getNetworkAdapter().createAdhocMatchingEventMessage(this, event);
	}

	protected AdhocMatchingEventMessage createMessage(int event, int data, int dataLength, byte[] macAddress) {
		return getNetworkAdapter().createAdhocMatchingEventMessage(this, event, data, dataLength, macAddress);
	}

	protected AdhocMatchingEventMessage createMessage(byte[] message, int length) {
		return getNetworkAdapter().createAdhocMatchingEventMessage(this, message, length);
	}

	@Override
	public String toString() {
		return String.format("MatchingObject[id=%d, mode=%d, maxPeers=%d, port=%d, callback=0x%08X]", getId(), mode, maxPeers, getPort(), callback);
	}
}
