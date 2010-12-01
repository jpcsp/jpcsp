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
package jpcsp.media;

import java.io.IOException;
import java.io.InputStream;

public class StreamReader extends Thread {
	InputStream is;
	StringBuilder input;

	public StreamReader(InputStream is) {
		this.is = is;
		input = new StringBuilder();
	}

	@Override
	public void run() {
		try {
			byte[] buffer = new byte[1000];
			while (true) {
				int length = is.read(buffer);
				if (length < 0) {
					break;
				}
				if (length > 0) {
					input.append(new String(buffer, 0, length));
				}
			}
			is.close();
		} catch (IOException e) {
			// Ignore IOException
		}
	}

	public String getInput() {
		return input.toString();
	}
}
