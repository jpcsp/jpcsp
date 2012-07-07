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

import java.net.InetAddress;

import jpcsp.HLE.kernel.types.pspNetMacAddress;
import jpcsp.network.INetworkAdapter;
import jpcsp.network.adhoc.AdhocMessage;
import jpcsp.network.adhoc.AdhocSocket;
import jpcsp.network.adhoc.PdpObject;

/**
 * @author gid15
 *
 */
public class ProOnlinePdpObject extends PdpObject {
	final protected ProOnlineNetworkAdapter proOnline;
	final private static String socketProtocol = "UCP";

	public ProOnlinePdpObject(INetworkAdapter networkAdapter) {
		super(networkAdapter);
		proOnline = (ProOnlineNetworkAdapter) networkAdapter;
	}

	@Override
	protected AdhocSocket createSocket() {
		return new ProOnlineAdhocDatagramSocket(proOnline);
	}

	@Override
	public int create(pspNetMacAddress macAddress, int port, int bufSize) {
		// Open the UDP port in the router
		proOnline.sceNetPortOpen(socketProtocol, port);

		return super.create(macAddress, port, bufSize);
	}

	@Override
	public void delete() {
		// Close the UDP port in the router
		proOnline.sceNetPortClose(socketProtocol, getPort());

		super.delete();
	}

	@Override
	protected boolean isForMe(AdhocMessage adhocMessage, int port, InetAddress address) {
		return proOnline.isForMe(adhocMessage, port, address);
	}
}
