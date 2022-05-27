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
package jpcsp.HLE.VFS;

import java.io.IOException;

import org.apache.log4j.Logger;

import jpcsp.HLE.TPointer;
import jpcsp.state.IState;
import jpcsp.state.StateInputStream;
import jpcsp.state.StateOutputStream;

public class ReadCacheVirtualFile extends BaseCacheVirtualFile implements IState {
	private static final int STATE_VERSION = 0;

	public ReadCacheVirtualFile(Logger log, IVirtualFile vFile) {
		super(log, vFile);
	}

	private void addToCache(TPointer ptr, int offset, int length, long position) {
		Block block = new Block(position, position + length, ptr.getArray8(offset, length));

		addBlock(block);
	}

	private void addToCache(byte[] buffer, int offset, int length, long position) {
		byte[] data = new byte[length];
		System.arraycopy(buffer, offset, data, 0, length);
		Block block = new Block(position, position + length, data);

		addBlock(block);
	}

	@Override
	public synchronized int ioRead(TPointer outputPointer, int outputLength) {
		if (log.isTraceEnabled()) {
			log.trace(String.format("ioRead position=0x%X, length=0x%X", vFile.getPosition(), outputLength));
		}

		long position = vFile.getPosition();
		int outputOffset = 0;
		int totalReadLength = 0;
		for (Block block : blocks) {
			if (block.isAfter(position)) {
				int length = Math.min(outputLength, (int) (block.start - position));
				int readLength = vFile.ioRead(new TPointer(outputPointer, outputOffset), length);
				if (readLength < 0) {
					return readLength;
				}

				addToCache(outputPointer, outputOffset, readLength, position);

				outputOffset += readLength;
				outputLength -= readLength;
				totalReadLength += readLength;
				position = vFile.getPosition();
			}

			if (block.isContaining(position)) {
				int before = (int) (position - block.start);
				int length = Math.min(outputLength, block.getLength() - before);
				outputPointer.setArray(outputOffset, block.data, before, length);
				outputOffset += length;
				outputLength -= length;
				totalReadLength += length;
				position = vFile.ioLseek(position + length);
			} else if (block.isAfter(position)) {
				break;
			}

			if (outputLength == 0) {
				break;
			}
		}

		if (outputLength > 0) {
			int readLength = vFile.ioRead(new TPointer(outputPointer, outputOffset), outputLength);
			if (readLength < 0) {
				return readLength;
			}

			addToCache(outputPointer, outputOffset, readLength, position);

			totalReadLength += readLength;
		}

		return totalReadLength;
	}

	@Override
	public synchronized int ioRead(byte[] outputBuffer, int outputOffset, int outputLength) {
		if (log.isTraceEnabled()) {
			log.trace(String.format("ioRead position=0x%X, length=0x%X", vFile.getPosition(), outputLength));
		}

		long position = vFile.getPosition();
		int totalReadLength = 0;
		for (Block block : blocks) {
			if (block.isAfter(position)) {
				int length = Math.min(outputLength, (int) (block.start - position));
				int readLength = vFile.ioRead(outputBuffer, outputOffset, length);
				if (readLength < 0) {
					return readLength;
				}

				addToCache(outputBuffer, outputOffset, readLength, position);

				outputOffset += readLength;
				outputLength -= readLength;
				totalReadLength += readLength;
				position = vFile.getPosition();
			}

			if (block.isContaining(position)) {
				int before = (int) (position - block.start);
				int length = Math.min(outputLength, block.getLength() - before);
				System.arraycopy(block.data, before, outputBuffer, outputOffset, length);
				outputOffset += length;
				outputLength -= length;
				totalReadLength += length;
				position = vFile.ioLseek(position + length);
			} else if (block.isAfter(position)) {
				break;
			}

			if (outputLength == 0) {
				break;
			}
		}

		if (outputLength > 0) {
			int readLength = vFile.ioRead(outputBuffer, outputOffset, outputLength);
			if (readLength < 0) {
				return readLength;
			}

			addToCache(outputBuffer, outputOffset, readLength, position);

			totalReadLength += readLength;
		}

		return totalReadLength;
	}

	@Override
	public void read(StateInputStream stream) throws IOException {
    	stream.readVersion(STATE_VERSION);

    	super.read(stream);
	}

	@Override
	public void write(StateOutputStream stream) throws IOException {
    	stream.writeVersion(STATE_VERSION);

    	super.write(stream);
	}

	@Override
	public String toString() {
		return String.format("ReadCacheVirtualFile %s", vFile);
	}
}
