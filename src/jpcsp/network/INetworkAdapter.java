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
package jpcsp.network;

import java.net.SocketAddress;
import java.net.UnknownHostException;

import jpcsp.HLE.modules150.sceNetAdhoc.GameModeArea;
import jpcsp.network.adhoc.AdhocMatchingEventMessage;
import jpcsp.network.adhoc.AdhocMessage;
import jpcsp.network.adhoc.MatchingObject;
import jpcsp.network.adhoc.PdpObject;
import jpcsp.network.adhoc.PtpObject;

/**
 * @author gid15
 *
 */
public interface INetworkAdapter {
	/**
	 * Called when starting the PSP sceNet module.
	 */
	public void start();

	/**
	 * Called when stopping the PSP sceNet module.
	 */
	public void stop();

	/**
	 * Create a new Pdp object
	 * @return a new Pdp object
	 */
	public PdpObject createPdpObject();

	/**
	 * Create a new Ptp object
	 * @return a new Ptp object
	 */
	public PtpObject createPtpObject();

	/**
	 * Create an Adhoc Pdp message from PSP memory data
	 * @param address        the address for the data part of the Pdp message
	 * @param length         the length of the data part of the Pdp message
	 * @param destMacAddress the destination MAC address
	 * @return               an AdhocMessage
	 */
	public AdhocMessage createAdhocPdpMessage(int address, int length, byte[] destMacAddress);

	/**
	 * Create an Adhoc Pdp message from a network packet
	 * @param message  the network packet received
	 * @param length   the length of the message
	 * @return         an AdhocMessage
	 */
	public AdhocMessage createAdhocPdpMessage(byte[] message, int length);

	/**
	 * Create an Adhoc Ptp message from PSP memory data
	 * @param address the address for the data part of the Pdp message
	 * @param length  the length of the data part of the Pdp message
	 * @return        an AdhocMessage
	 */
	public AdhocMessage createAdhocPtpMessage(int address, int length);

	/**
	 * Create an Adhoc Ptp message from a network packet
	 * @param message  the network packet received
	 * @param length   the length of the message
	 * @return         an AdhocMessage
	 */
	public AdhocMessage createAdhocPtpMessage(byte[] message, int length);

	/**
	 * Create an Adhoc GameMode message from a PSP GameModeArea
	 * @param gameModeArea the GameMode area
	 * @return             an AdhocMessage
	 */
	public AdhocMessage createAdhocGameModeMessage(GameModeArea gameModeArea);

	/**
	 * Create an Adhoc GameMode message from a network packet
	 * @param message  the network packet received
	 * @param length   the length of the message
	 * @return         an AdhocMessage
	 */
	public AdhocMessage createAdhocGameModeMessage(byte[] message, int length);

	/**
	 * Get the SocketAddress for the given MAC address and port.
	 * @param macAddress  the MAC address
	 * @param port        the real port number (i.e. the shifted port if port shifting is active)
	 * @return            the corresponding SocketAddress
	 */
	public SocketAddress getSocketAddress(byte[] macAddress, int realPort) throws UnknownHostException;

	/**
	 * Create a new Matching object
	 * @return a new Matching object
	 */
	public MatchingObject createMatchingObject();

	/**
	 * Create an Adhoc Matching message for an event.
	 * @param event the event
	 * @return      the Adhoc Matching message for the event
	 */
	public AdhocMatchingEventMessage createAdhocMatchingEventMessage(MatchingObject matchingObject, int event);

	/**
	 * Create an Adhoc Matching message for an event with additional data.
	 * @param event      the event
	 * @param data       the address of the additional data
	 * @param dataLength the length of the additional data
	 * @param macAddress the destination MAC address
	 * @return           the new Adhoc Matching message
	 */
	public AdhocMatchingEventMessage createAdhocMatchingEventMessage(MatchingObject matchingObject, int event, int data, int dataLength, byte[] macAddress);

	/**
	 * Create an Adhoc Matching message from a network packet
	 * @param message  the network packet received
	 * @param length   the length of the message
	 * @return         an Adhoc Matching
	 */
	public AdhocMatchingEventMessage createAdhocMatchingEventMessage(MatchingObject matchingObject, byte[] message, int length);

	/**
	 * Send a chat message to the network group
	 * @param message the chat message to send
	 */
	public void sendChatMessage(String message);

	/**
	 * Called when executing sceNetAdhocctlInit.
	 */
	public void sceNetAdhocctlInit();

	/**
	 * Called when executing sceNetAdhocctlTerm.
	 */
	public void sceNetAdhocctlTerm();

	/**
	 * Called when executing sceNetAdhocctlConnect.
	 */
	public void sceNetAdhocctlConnect();

	/**
	 * Called when executing sceNetAdhocctlCreate.
	 */
	public void sceNetAdhocctlCreate();

	/**
	 * Called when executing sceNetAdhocctlDisconnect.
	 */
	public void sceNetAdhocctlDisconnect();

	/**
	 * Called when executing sceNetAdhocctlScan.
	 */
	public void sceNetAdhocctlScan();
}
