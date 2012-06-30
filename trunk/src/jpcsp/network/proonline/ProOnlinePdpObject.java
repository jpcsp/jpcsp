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

import static jpcsp.HLE.modules150.sceNetAdhoc.isAnyMacAddress;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.kernel.types.pspNetMacAddress;
import jpcsp.network.INetworkAdapter;
import jpcsp.network.adhoc.AdhocDatagramSocket;
import jpcsp.network.adhoc.AdhocSocket;
import jpcsp.network.adhoc.PdpObject;

/**
 * @author gid15
 *
 */
public class ProOnlinePdpObject extends PdpObject {
	ProOnlineNetworkAdapter proOnline;

	public ProOnlinePdpObject(INetworkAdapter networkAdapter) {
		super(networkAdapter);
		proOnline = (ProOnlineNetworkAdapter) networkAdapter;
	}

	@Override
	public int send(pspNetMacAddress destMacAddress, int destPort, TPointer data, int length, int timeout, int nonblock) {
		if (isAnyMacAddress(destMacAddress.macAddress)) {
			// Broadcast to all MAC's/IP's
			for (MacIp macIp : proOnline.getMacIps()) {
				int result = super.send(macIp.macAddress, destPort, data, length, timeout, nonblock);
				if (result != 0) {
					return result;
				}
			}

			return 0;
		}

		return super.send(destMacAddress, destPort, data, length, timeout, nonblock);
	}

	@Override
	protected AdhocSocket createSocket() {
		return new AdhocDatagramSocket();
	}
}
