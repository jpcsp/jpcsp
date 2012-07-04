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

import static jpcsp.network.jpcsp.JpcspAdhocPtpMessage.PTP_MESSAGE_TYPE_DATA;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;

import jpcsp.HLE.Modules;
import jpcsp.HLE.modules150.sceNetInet;
import jpcsp.HLE.modules150.sceNetAdhoc.GameModeArea;
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
		if (Modules.sceNetAdhocModule.hasNetPortShiftActive()) {
			return new InetSocketAddress(InetAddress.getLocalHost(), realPort);
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
		return Modules.sceNetAdhocctlModule.getNumberPeers() > 0;
	}
}
