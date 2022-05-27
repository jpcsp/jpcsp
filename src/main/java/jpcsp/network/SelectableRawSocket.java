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

import org.apache.log4j.Logger;

import jpcsp.HLE.modules.sceNetInet;

import com.savarese.rocksaw.net.RawSocket;

public class SelectableRawSocket extends RawSocket {
	protected static Logger log = sceNetInet.log;

	public boolean isSelectedForRead() {
		int result = __select(__socket, true, 0, 0);
		if (log.isDebugEnabled()) {
			log.debug(String.format("SelectableRawSocket.isSelectedForRead: %d", result));
		}
		return result >= 0;
	}

	public boolean isSelectedForWrite() {
		int result = __select(__socket, false, 0, 0);
		if (log.isDebugEnabled()) {
			log.debug(String.format("SelectableRawSocket.isSelectedForWrite: %d", result));
		}
		return result >= 0;
	}
}
