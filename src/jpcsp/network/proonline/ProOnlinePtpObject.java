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
import java.net.InetAddress;
import java.net.UnknownHostException;

import jpcsp.HLE.kernel.types.SceKernelThreadInfo;
import jpcsp.HLE.kernel.types.pspNetMacAddress;
import jpcsp.network.INetworkAdapter;
import jpcsp.network.adhoc.AdhocMessage;
import jpcsp.network.adhoc.AdhocSocket;
import jpcsp.network.adhoc.AdhocStreamSocket;
import jpcsp.network.adhoc.PtpObject;

/**
 * @author gid15
 *
 */
public class ProOnlinePtpObject extends PtpObject {
	final protected ProOnlineNetworkAdapter proOnline;

	public ProOnlinePtpObject(INetworkAdapter networkAdapter) {
		super(networkAdapter);
		proOnline = (ProOnlineNetworkAdapter) networkAdapter;
	}

	@Override
	protected boolean pollAccept(int peerMacAddr, int peerPortAddr, SceKernelThreadInfo thread) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected boolean pollConnect(SceKernelThreadInfo thread) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean canAccept() {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public boolean canConnect() {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	protected AdhocSocket createSocket() throws UnknownHostException, IOException {
		return new AdhocStreamSocket();
	}

	@Override
	public int create(pspNetMacAddress macAddress, int port, int bufSize) {
		// Open the UDP port in the router
		proOnline.sceNetPortOpen("UDP", port);

		return super.create(macAddress, port, bufSize);
	}

	@Override
	public void delete() {
		// Close the UDP port in the router
		proOnline.sceNetPortClose("UDP", getPort());

		super.delete();
	}

	@Override
	protected boolean isForMe(AdhocMessage adhocMessage, int port, InetAddress address) {
		// Always for me on stream sockets
		return true;
	}
}
