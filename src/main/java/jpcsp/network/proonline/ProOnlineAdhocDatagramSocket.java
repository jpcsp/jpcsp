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

import java.io.IOException;
import java.net.SocketAddress;

import jpcsp.network.adhoc.AdhocDatagramSocket;
import jpcsp.network.adhoc.AdhocMessage;

/**
 * @author gid15
 *
 */
public class ProOnlineAdhocDatagramSocket extends AdhocDatagramSocket {
	private ProOnlineNetworkAdapter proOnline;

	public ProOnlineAdhocDatagramSocket(ProOnlineNetworkAdapter proOnline) {
		this.proOnline = proOnline;
	}

	@Override
	public void send(SocketAddress socketAddress, AdhocMessage adhocMessage) throws IOException {
		if (proOnline.isBroadcast(socketAddress)) {
			int port = proOnline.getBroadcastPort(socketAddress);
			// Broadcast to all MAC's/IP's
			int numberMacIps = proOnline.getNumberMacIps();
			for (int i = 0; i < numberMacIps; i++) {
				MacIp macIp = proOnline.getMacIp(i);
				if (macIp != null) {
					SocketAddress remoteSocketAddress = proOnline.getSocketAddress(macIp.mac, port);
					if (log.isDebugEnabled()) {
						log.debug(String.format("Sending broadcasted message to %s: %s", macIp, adhocMessage));
					}
					super.send(remoteSocketAddress, adhocMessage);
				}
			}
		} else {
			super.send(socketAddress, adhocMessage);
		}
	}
}
