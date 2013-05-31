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
package jpcsp.network.jpcsp;

import static jpcsp.HLE.Modules.sceNetAdhocModule;
import static jpcsp.HLE.Modules.sceNetAdhocctlModule;
import static jpcsp.HLE.modules150.sceNetAdhocctl.PSP_ADHOCCTL_MODE_GAMEMODE;
import static jpcsp.network.jpcsp.JpcspAdhocPtpMessage.PTP_MESSAGE_TYPE_DATA;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import jpcsp.HLE.kernel.types.pspNetMacAddress;
import jpcsp.HLE.modules.sceNetAdhocctl;
import jpcsp.HLE.modules150.sceNetAdhoc;
import jpcsp.HLE.modules150.sceNetInet;
import jpcsp.HLE.modules150.sceUtility;
import jpcsp.HLE.modules150.sceNetAdhoc.GameModeArea;
import jpcsp.hardware.Wlan;
import jpcsp.network.BaseNetworkAdapter;
import jpcsp.network.adhoc.AdhocMatchingEventMessage;
import jpcsp.network.adhoc.AdhocMessage;
import jpcsp.network.adhoc.MatchingObject;
import jpcsp.network.adhoc.PdpObject;
import jpcsp.network.adhoc.PtpObject;

/**
 * @author gid15
 *
 */
public class JpcspNetworkAdapter extends BaseNetworkAdapter {
    private DatagramSocket adhocctlSocket;
    private static final int adhocctlBroadcastPort = 30000;

    @Override
	public void sceNetAdhocctlInit() {
	}

	@Override
	public void sceNetAdhocctlTerm() {
	}

	@Override
	public void sceNetAdhocctlConnect() {
	}

	@Override
	public void sceNetAdhocctlCreate() {
	}

	@Override
	public void sceNetAdhocctlJoin() {
	}

	@Override
	public void sceNetAdhocctlDisconnect() {
	}

	@Override
	public void sceNetAdhocctlScan() {
	}

	@Override
	public AdhocMessage createAdhocPdpMessage(int address, int length, byte[] destMacAddress) {
		return new JpcspAdhocPdpMessage(address, length, destMacAddress);
	}

	@Override
	public AdhocMessage createAdhocPdpMessage(byte[] message, int length) {
		return new JpcspAdhocPdpMessage(message, length);
	}

	@Override
	public PdpObject createPdpObject() {
		return new JpcspPdpObject(this);
	}

	@Override
	public PtpObject createPtpObject() {
		return new JpcspPtpObject(this);
	}

	@Override
	public AdhocMessage createAdhocPtpMessage(int address, int length) {
		return new JpcspAdhocPtpMessage(address, length, PTP_MESSAGE_TYPE_DATA);
	}

	@Override
	public AdhocMessage createAdhocPtpMessage(byte[] message, int length) {
		return new JpcspAdhocPtpMessage(message, length);
	}

	@Override
	public AdhocMessage createAdhocGameModeMessage(GameModeArea gameModeArea) {
		return new JpcspAdhocGameModeMessage(gameModeArea);
	}

	@Override
	public AdhocMessage createAdhocGameModeMessage(byte[] message, int length) {
		return new JpcspAdhocGameModeMessage(message, length);
	}

	@Override
	public SocketAddress getSocketAddress(byte[] macAddress, int realPort) throws UnknownHostException {
		if (sceNetAdhocModule.hasNetPortShiftActive()) {
			return new InetSocketAddress(InetAddress.getLocalHost(), realPort);
		}
		return sceNetInet.getBroadcastInetSocketAddress(realPort)[0];
	}

	@Override
	public SocketAddress[] getMultiSocketAddress(byte[] macAddress, int realPort) throws UnknownHostException {
		if (sceNetAdhocModule.hasNetPortShiftActive()) {
			return super.getMultiSocketAddress(macAddress, realPort);
		}
		return sceNetInet.getBroadcastInetSocketAddress(realPort);
	}

	@Override
	public MatchingObject createMatchingObject() {
		return new JpcspMatchingObject(this);
	}

	@Override
	public AdhocMatchingEventMessage createAdhocMatchingEventMessage(MatchingObject matchingObject, int event) {
		return new JpcspAdhocMatchingEventMessage(matchingObject, event);
	}

	@Override
	public AdhocMatchingEventMessage createAdhocMatchingEventMessage(MatchingObject matchingObject, int event, int data, int dataLength, byte[] macAddress) {
		return new JpcspAdhocMatchingEventMessage(matchingObject, event, data, dataLength, macAddress);
	}

	@Override
	public AdhocMatchingEventMessage createAdhocMatchingEventMessage(MatchingObject matchingObject, byte[] message, int length) {
		return new JpcspAdhocMatchingEventMessage(matchingObject, message, length);
	}

	@Override
	public void sendChatMessage(String message) {
		// TODO Implement Chat
		log.warn(String.format("Chat functionality not supported: %s", message));
	}

	@Override
	public boolean isConnectComplete() {
		return sceNetAdhocctlModule.getNumberPeers() > 0 || sceNetAdhocctlModule.hleNetAdhocctlGetState() == sceNetAdhocctl.PSP_ADHOCCTL_STATE_DISCONNECTED;
	}

    private void openSocket() throws SocketException {
    	if (adhocctlSocket == null) {
    		adhocctlSocket = new DatagramSocket(sceNetAdhocModule.getRealPortFromServerPort(adhocctlBroadcastPort));
    		// For broadcast
    		adhocctlSocket.setBroadcast(true);
    		// Non-blocking (timeout = 0 would mean blocking)
    		adhocctlSocket.setSoTimeout(1);
    	}
    }

    private void broadcastPeers() {
    	if (sceNetAdhocctlModule.hleNetAdhocctlGetGroupName() == null) {
    		return;
    	}

    	try {
			openSocket();

			JpcspAdhocctlMessage adhocctlMessage = new JpcspAdhocctlMessage(sceUtility.getSystemParamNickname(), Wlan.getMacAddress(), sceNetAdhocctlModule.hleNetAdhocctlGetGroupName());
			if (sceNetAdhocctlModule.hleNetAdhocctlGetMode() == PSP_ADHOCCTL_MODE_GAMEMODE && sceNetAdhocctlModule.hleNetAdhocctlGetRequiredGameModeMacs().size() > 0) {
				boolean gameModeComplete = sceNetAdhocctlModule.isGameModeComplete();
				adhocctlMessage.setGameModeComplete(gameModeComplete, sceNetAdhocctlModule.hleNetAdhocctlGetRequiredGameModeMacs());
			}
	    	SocketAddress[] socketAddress = sceNetAdhocModule.getMultiSocketAddress(sceNetAdhoc.ANY_MAC_ADDRESS, sceNetAdhocModule.getRealPortFromClientPort(sceNetAdhoc.ANY_MAC_ADDRESS, adhocctlBroadcastPort));
	    	for (int i = 0; i < socketAddress.length; i++) {
		    	DatagramPacket packet = new DatagramPacket(adhocctlMessage.getMessage(), JpcspAdhocctlMessage.getMessageLength(), socketAddress[i]);
		    	adhocctlSocket.send(packet);

		    	if (log.isDebugEnabled()) {
		    		log.debug(String.format("broadcast sent to peer[%s]: %s", socketAddress[i], adhocctlMessage));
		    	}
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

			// Poll all the available messages.
			// Exiting the loop only when no more messages are available (SocketTimeoutException)
			while (true) {
		    	byte[] bytes = new byte[JpcspAdhocctlMessage.getMessageLength()];
		    	DatagramPacket packet = new DatagramPacket(bytes, bytes.length);
		    	adhocctlSocket.receive(packet);
		    	JpcspAdhocctlMessage adhocctlMessage = new JpcspAdhocctlMessage(packet.getData(), packet.getLength());

		    	if (log.isDebugEnabled()) {
		    		log.debug(String.format("broadcast received from peer: %s", adhocctlMessage));
		    	}

		    	// Ignore messages coming from myself
		    	if (!sceNetAdhoc.isSameMacAddress(Wlan.getMacAddress(), adhocctlMessage.macAddress)) {
			    	if (adhocctlMessage.groupName.equals(sceNetAdhocctlModule.hleNetAdhocctlGetGroupName())) {
			    		sceNetAdhocctlModule.hleNetAdhocctlAddPeer(adhocctlMessage.nickName, new pspNetMacAddress(adhocctlMessage.macAddress));
			    	}

			    	if (adhocctlMessage.ibss.equals(sceNetAdhocctlModule.hleNetAdhocctlGetIBSS())) {
			    		sceNetAdhocctlModule.hleNetAdhocctlAddNetwork(adhocctlMessage.groupName, new pspNetMacAddress(adhocctlMessage.macAddress), adhocctlMessage.channel, adhocctlMessage.ibss, adhocctlMessage.mode);

			    		if (adhocctlMessage.mode == PSP_ADHOCCTL_MODE_GAMEMODE) {
			    			sceNetAdhocctlModule.hleNetAdhocctlAddGameModeMac(adhocctlMessage.macAddress);
			    			if (sceNetAdhocctlModule.hleNetAdhocctlGetRequiredGameModeMacs().size() <= 0) {
			    				sceNetAdhocctlModule.hleNetAdhocctlSetGameModeJoinComplete(adhocctlMessage.gameModeComplete);
			    				if (adhocctlMessage.gameModeComplete) {
			    					byte[][] macs = adhocctlMessage.gameModeMacs;
			    					if (macs != null) {
			    						sceNetAdhocctlModule.hleNetAdhocctlSetGameModeMacs(macs);
			    					}
		    					}
			    			}
			    		}
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

	@Override
	public void updatePeers() {
		broadcastPeers();
		pollPeers();
	}
}
