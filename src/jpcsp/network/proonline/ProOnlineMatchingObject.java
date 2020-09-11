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
package jpcsp.network.proonline;

import static jpcsp.HLE.modules.sceNetAdhoc.isMyMacAddress;
import static jpcsp.HLE.modules.sceNetAdhocMatching.PSP_ADHOC_MATCHING_EVENT_DATA_CONFIRM;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import jpcsp.HLE.kernel.types.pspNetMacAddress;
import jpcsp.network.INetworkAdapter;
import jpcsp.network.adhoc.AdhocMessage;
import jpcsp.network.adhoc.AdhocSocket;
import jpcsp.network.adhoc.MatchingObject;

/**
 * @author gid15
 *
 */
public class ProOnlineMatchingObject extends MatchingObject {
	ProOnlineNetworkAdapter proOnline;

	public ProOnlineMatchingObject(INetworkAdapter networkAdapter) {
		super(networkAdapter);
		proOnline = (ProOnlineNetworkAdapter) networkAdapter;
	}

	@Override
	protected AdhocSocket createSocket() throws UnknownHostException, IOException {
		return new ProOnlineAdhocDatagramSocket(proOnline);
	}

	@Override
	public void create() {
		// Open the UDP port in the router
		proOnline.sceNetPortOpen("UDP", getPort());

		super.create();
	}

	@Override
	protected boolean isForMe(AdhocMessage adhocMessage, int port, InetAddress address) {
		return proOnline.isForMe(adhocMessage, port, address);
	}

	@Override
	public int selectTarget(pspNetMacAddress macAddress, int optLen, int optData) {
		boolean sendBirth = isPendingJoinRequest(macAddress);

		int result = super.selectTarget(macAddress, optLen, optData);

		if (sendBirth) {
			// Inform the other members of the new member
			for (pspNetMacAddress member : getMembers()) {
				if (!macAddress.equals(member) && !isMyMacAddress(member.macAddress)) {
					ProOnlineAdhocMatchingEventMessage birthMessageToOtherMembers = MatchingPacketFactory.createBirthPacket(proOnline, this, member.macAddress, macAddress.macAddress);
					try {
						if (log.isDebugEnabled()) {
							log.debug(String.format("selectTarget sending birth message for new member %s to %s", macAddress, member));
						}

						send(birthMessageToOtherMembers);
					} catch (IOException e) {
						log.error("selectTarget", e);
					}
				}
			}
		}

		return result;
	}

	@Override
	public int send(pspNetMacAddress macAddress, int dataLen, int data) {
		int result = super.send(macAddress, dataLen, data);

		// Trigger the PSP_ADHOC_MATCHING_EVENT_DATA_CONFIRM event immediately
		notifyCallbackEvent(PSP_ADHOC_MATCHING_EVENT_DATA_CONFIRM, macAddress.getBaseAddress(), dataLen, data);

		return result;
	}
}
