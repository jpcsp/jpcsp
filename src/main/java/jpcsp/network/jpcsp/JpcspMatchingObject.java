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

import java.io.IOException;
import java.net.UnknownHostException;

import jpcsp.network.INetworkAdapter;
import jpcsp.network.adhoc.AdhocDatagramSocket;
import jpcsp.network.adhoc.AdhocSocket;
import jpcsp.network.adhoc.MatchingObject;

/**
 * @author gid15
 *
 */
public class JpcspMatchingObject extends MatchingObject {
	public JpcspMatchingObject(INetworkAdapter networkAdapter) {
		super(networkAdapter);
	}

	@Override
	protected AdhocSocket createSocket() throws UnknownHostException, IOException {
		return new AdhocDatagramSocket();
	}
}
