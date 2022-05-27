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
package jpcsp.network.protocols;

import java.io.EOFException;
import java.util.HashMap;
import java.util.Map;

public class SSDP {
	public String search;
	private Map<String, String> headers = new HashMap<String, String>();

	public void read(NetPacket packet) throws EOFException {
		search = packet.readLine();

		while (!packet.isEmpty()) {
			String line = packet.readLine();
			if (line == null || line.length() == 0) {
				break;
			}

			int colon = line.indexOf(':');
			if (colon > 0) {
				String name = line.substring(0, colon).trim();
				String value = line.substring(colon + 1).trim();
				headers.put(name.toLowerCase(), value);
			}
		}
	}

	public String getHeaderValue(String name) {
		return headers.get(name.toLowerCase());
	}

	@Override
	public String toString() {
		StringBuilder s = new StringBuilder();
		s.append(String.format("%s", search));
		for (String name : headers.keySet()) {
			s.append(String.format("\n%s: %s", name, headers.get(name)));
		}

		return s.toString();
	}
}
