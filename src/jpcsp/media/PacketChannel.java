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

import org.apache.log4j.Logger;

import com.xuggle.xuggler.io.IURLProtocolHandler;

import jpcsp.util.FIFOByteBuffer;
import jpcsp.util.Utilities;

/*
 * Common interface for PSMF/MPEG -> Media Engine communication.
 *
 */
public class PacketChannel extends FIFOByteBuffer implements IMediaChannel {
	private static Logger log = Logger.getLogger("PacketChannel");
	private int readLength;
	private int totalStreamSize;
	private long position;
	private boolean farRewindAllowed;

    public PacketChannel() {
		super();
		totalStreamSize = -1;
	}

	public PacketChannel(byte[] buffer) {
		super(buffer);
		totalStreamSize = buffer.length;
	}

    @Override
	public int close() {
    	delete();
    	return 0;
	}

    @Override
	public int getReadLength() {
		return readLength;
	}

    @Override
	public void setReadLength(int readLength) {
		this.readLength = readLength;
	}

	@Override
	public void clear() {
		super.clear();
		setReadLength(0);
		position = 0;
		totalStreamSize = -1;
	}

	public void reset() {
		super.clear();
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
		int readSize = 0;

		if (size > 0) {
			readSize = readByteBuffer(ByteBuffer.wrap(buf, 0, size));
			if (readSize > 0) {
				if (log.isDebugEnabled()) {
					log.debug(String.format("PacketChannel: read %d/%d bytes, position=%d", readSize, size, position));
					if (log.isTraceEnabled()) {
						log.trace(Utilities.getMemoryDump(buf, 0, readSize));
					}
				}
				readLength += readSize;
				position += readSize;
			} else {
				log.debug("PacketChannel: End of data");
			}
		}

		return readSize;
	}

	@Override
	public long seek(long offset, int whence) {
		long result = offset;

		if (totalStreamSize < 0) {
			result = -1;
		} else {
			switch (whence) {
				case IURLProtocolHandler.SEEK_CUR:
					if (!setPosition(position + offset)) {
						result = -1;
					}
					break;
				case IURLProtocolHandler.SEEK_SET:
					if (!setPosition(offset)) {
						result = -1;
					}
					break;
				case IURLProtocolHandler.SEEK_END:
					if (!setPosition(totalStreamSize + offset)) {
						result = -1;
					}
					break;
				case IURLProtocolHandler.SEEK_SIZE:
					result = totalStreamSize;
					break;
				default:
					result = -1;
					break;
			}
		}
		if (log.isDebugEnabled()) {
			log.debug(String.format("PacketChannel: seek offset=%d, whence=%d, result=%d, new position=%d", offset, whence, result, getPosition()));
		}

		return result;
	}

	@Override
	public int write(byte[] buf, int size) {
		log.warn(String.format("PacketChannel: unsupported write size=%d", size));

		// Write not supported
		return -1;
	}

	public int getTotalStreamSize() {
		return totalStreamSize;
	}

	public void setTotalStreamSize(int totalStreamSize) {
		this.totalStreamSize = totalStreamSize;
	}

	public long getPosition() {
		return position;
	}

	public boolean setPosition(long position) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("setPosition %d", position));
		}
		if (position > this.position) {
			int forwardLength = (int) (position - this.position);
			if (!forward(forwardLength)) {
				if (log.isDebugEnabled()) {
					log.debug(String.format("setPosition %d forward failed", position));
				}
				return false;
			}
		} else if (position < this.position) {
			int rewindLength = (int) (this.position - position);
			if (!rewind(rewindLength) && !farRewindAllowed) {
				if (log.isDebugEnabled()) {
					log.debug(String.format("setPosition %d rewind failed", position));
				}
				return false;
			}
		}

		this.position = position;
		return true;
	}

	public boolean isFarRewindAllowed() {
		return farRewindAllowed;
	}

	public void setFarRewindAllowed(boolean farRewindAllowed) {
		this.farRewindAllowed = farRewindAllowed;
	}
}