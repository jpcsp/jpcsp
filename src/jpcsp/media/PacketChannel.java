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
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

import jpcsp.util.FIFOByteBuffer;

/*
 * Common interface for PSMF/MPEG -> Media Engine communication.
 *
 */
public class PacketChannel extends FIFOByteBuffer implements ReadableByteChannel {
	private int readLength;

    public PacketChannel() {
		super();
	}

	public PacketChannel(byte[] buffer) {
		super(buffer);
	}

	@Override
	public int read(ByteBuffer dst) throws IOException {
		int length = readByteBuffer(dst);

		if (length > 0) {
			readLength += length;
		}

		return length;
	}

    @Override
	public void close() throws IOException {
    	delete();
	}

    @Override
	public boolean isOpen() {
		return true;
	}

	public int getReadLength() {
		return readLength;
	}

	public void setReadLength(int readLength) {
		this.readLength = readLength;
	}

	@Override
	public void clear() {
		super.clear();
		setReadLength(0);
	}
}