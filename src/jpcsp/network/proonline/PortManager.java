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

import java.util.LinkedList;
import java.util.List;

import jpcsp.network.upnp.UPnP;

/**
 * @author gid15
 *
 */
public class PortManager {
	private List<String> hosts = new LinkedList<String>();
	private List<PortInfo> portInfos = new LinkedList<PortInfo>();
	private UPnP upnp;
	private String externalIPAddress;
	private final static int portLeaseDuration = 0;
	private final static String portDescription = "Jpcsp ProOnline Network";

	private static class PortInfo {
		int port;
		String protocol;

		public PortInfo(int port, String protocol) {
			this.port = port;
			this.protocol = protocol;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof PortInfo) {
				PortInfo portInfo = (PortInfo) obj;
				return port == portInfo.port && protocol.equals(portInfo.protocol);
			}
			return super.equals(obj);
		}
	}

	public PortManager(UPnP upnp) {
		this.upnp = upnp;
	}

	protected String getExternalIPAddress() {
		if (externalIPAddress == null) {
			externalIPAddress = upnp.getIGD().getExternalIPAddress(upnp);
		}

		return externalIPAddress;
	}

	public void addHost(String host) {
		if (hosts.contains(host)) {
			return;
		}

		// Open all the ports to this new host
		for (PortInfo portInfo : portInfos) {
			upnp.getIGD().addPortMapping(upnp, host, portInfo.port, portInfo.protocol, portInfo.port, getExternalIPAddress(), portDescription, portLeaseDuration);
		}

		hosts.add(host);
	}

	public void removeHost(String host) {
		if (!hosts.contains(host)) {
			return;
		}

		// Remove all the port mappings from this host
		for (PortInfo portInfo : portInfos) {
			upnp.getIGD().deletePortMapping(upnp, host, portInfo.port, portInfo.protocol);
		}

		hosts.remove(host);
	}

	public void addPort(int port, String protocol) {
		PortInfo portInfo = new PortInfo(port, protocol);
		if (portInfos.contains(portInfo)) {
			return;
		}

		// All the new port mapping for all the hosts
		for (String host : hosts) {
			upnp.getIGD().addPortMapping(upnp, host, port, protocol, port, getExternalIPAddress(), portDescription, portLeaseDuration);
		}

		portInfos.add(portInfo);
	}

	public void removePort(int port, String protocol) {
		PortInfo portInfo = new PortInfo(port, protocol);
		if (!portInfos.contains(portInfo)) {
			return;
		}

		// Remove the port mapping for all the hosts
		for (String host : hosts) {
			upnp.getIGD().deletePortMapping(upnp, host, port, protocol);
		}

		portInfos.remove(portInfo);
	}

	public void clear() {
		// Remove all the hosts
		while (!hosts.isEmpty()) {
			String host = hosts.get(0);
			removeHost(host);
		}

		// ...and remove all the ports
		while (!portInfos.isEmpty()) {
			PortInfo portInfo = portInfos.get(0);
			removePort(portInfo.port, portInfo.protocol);
		}
	}
}
