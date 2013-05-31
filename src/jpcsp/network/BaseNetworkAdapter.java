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

import org.apache.log4j.Logger;

/**
 * @author gid15
 *
 */
public abstract class BaseNetworkAdapter implements INetworkAdapter {
	protected static Logger log = Logger.getLogger("network");

	@Override
	public void start() {
	}

	@Override
	public void stop() {
	}

	@Override
	public SocketAddress[] getMultiSocketAddress(byte[] macAddress, int realPort) throws UnknownHostException {
		SocketAddress[] socketAddresses = new SocketAddress[1];
		socketAddresses[0] = getSocketAddress(macAddress, realPort);

		return socketAddresses;
	}
}
