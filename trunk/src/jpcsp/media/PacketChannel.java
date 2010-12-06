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

import java.nio.ByteBuffer;

import com.xuggle.xuggler.io.IURLProtocolHandler;

import jpcsp.util.FIFOByteBuffer;

/*
 * Common interface for PSMF/MPEG -> Media Engine communication.
 *
 */
public class PacketChannel extends FIFOByteBuffer implements IURLProtocolHandler {
	private int readLength;

    public PacketChannel() {
		super();
	}

	public PacketChannel(byte[] buffer) {
		super(buffer);
	}

    @Override
	public int close() {
    	delete();
    	return 0;
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

	@Override
	public boolean isStreamed(String url, int flags) {
		// We support seeking
		return false;
	}

	@Override
	public int open(String url, int flags) {
		// Nothing to do
		return 0;
	}

	@Override
	public int read(byte[] buf, int size) {
		if (size > 0) {
			size = readByteBuffer(ByteBuffer.wrap(buf, 0, size));
			if (size > 0) {
				readLength += size;
			}
		}

		return size;
	}

	@Override
	public long seek(long offset, int whence) {
		// Seek not supported
		return -1;
	}

	@Override
	public int write(byte[] buf, int size) {
		// Write not supported
		return -1;
	}
}