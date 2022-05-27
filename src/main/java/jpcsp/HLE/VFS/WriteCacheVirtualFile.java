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
import jpcsp.util.Utilities;

public class WriteCacheVirtualFile extends BaseCacheVirtualFile implements IState {
	private static final int STATE_VERSION = 0;
	private boolean compareAtWrite;

	public WriteCacheVirtualFile(Logger log, IVirtualFile vFile) {
		super(log, vFile);

		if (log.isTraceEnabled()) {
			log.trace(String.format("Creating WriteCacheVirtualFile 0x%X, vFile=%s", hashCode(), vFile));
		}
	}

	public WriteCacheVirtualFile(Logger log, IVirtualFile vFile, boolean compareAtWrite) {
		super(log, vFile);

		this.compareAtWrite = compareAtWrite;
	}

	@Override
	public synchronized int ioRead(TPointer outputPointer, int outputLength) {
		if (log.isTraceEnabled()) {
			log.trace(String.format("ioRead position=0x%X, length=0x%X", vFile.getPosition(), outputLength));
		}

		// Optimized the most common case
		if (blocks.isEmpty()) {
			return vFile.ioRead(outputPointer, outputLength);
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
			totalReadLength += readLength;
		}

		return totalReadLength;
	}

	@Override
	public synchronized int ioRead(byte[] outputBuffer, int outputOffset, int outputLength) {
		if (log.isTraceEnabled()) {
			log.trace(String.format("ioRead position=0x%X, length=0x%X", vFile.getPosition(), outputLength));
		}

		// Optimized the most common case
		if (blocks.isEmpty()) {
			return vFile.ioRead(outputBuffer, outputOffset, outputLength);
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
			totalReadLength += readLength;
		}

		return totalReadLength;
	}

	@Override
	public synchronized int ioWrite(TPointer inputPointer, int inputLength) {
		long position = vFile.getPosition();
		byte[] data = inputPointer.getArray8(inputLength);

		if (log.isTraceEnabled()) {
			log.trace(String.format("ioWrite position=0x%X, length=0x%X, %s", position, inputLength, Utilities.getMemoryDump(data)));
		}

		boolean ignoreWrite = false;
		if (compareAtWrite) {
			// If the written data is matching the data already present on vFile,
			// ignore the write operation.
			byte[] targetData = new byte[inputLength];
			vFile.ioRead(targetData, 0, inputLength);

			ignoreWrite = Utilities.equals(data, 0, targetData, 0, inputLength);
		} else {
			vFile.ioLseek(position + inputLength);
		}

		if (!ignoreWrite) {
			Block block = new Block(position, position + inputLength, data);

			addBlock(block);
		}

		return inputLength;
	}

	@Override
	public synchronized int ioWrite(byte[] inputBuffer, int inputOffset, int inputLength) {
		long position = vFile.getPosition();

		if (log.isTraceEnabled()) {
			log.trace(String.format("ioWrite position=0x%X, length=0x%X, %s", position, inputLength, Utilities.getMemoryDump(inputBuffer, inputOffset, inputLength)));
		}

		boolean ignoreWrite = false;
		if (compareAtWrite) {
			// If the written data is matching the data already present on vFile,
			// ignore the write operation.
			byte[] targetData = new byte[inputLength];
			vFile.ioRead(targetData, 0, inputLength);

			ignoreWrite = Utilities.equals(inputBuffer, inputOffset, targetData, 0, inputLength);
		} else {
			vFile.ioLseek(position + inputLength);
		}

		if (!ignoreWrite) {
			byte[] data = new byte[inputLength];
			System.arraycopy(inputBuffer, inputOffset, data, 0, inputLength);
			Block block = new Block(position, position + inputLength, data);

			addBlock(block);
		}

		return inputLength;
	}

	private void writeBlocks() {
		for (Block block : blocks) {
			if (log.isTraceEnabled()) {
				log.trace(String.format("WriteCacheVirtualFile.flushCachedData %s", block));
			}

			int result = block.ioWrite(vFile);
			if (result < 0) {
				log.error(String.format("WriteCacheVirtualFile.flushCachedData failed 0x%08X for %s", result, block));
			}
		}
	}

	@Override
	public synchronized void flushCachedData() {
		super.flushCachedData();

		// Write the pending blocks after rebuilding the fat,
		// so that any blocks related to file contents are written
		// to the correct files.
		writeBlocks();
	}

	@Override
	public void read(StateInputStream stream) throws IOException {
    	stream.readVersion(STATE_VERSION);
    	compareAtWrite = stream.readBoolean();

    	super.read(stream);
	}

	@Override
	public void write(StateOutputStream stream) throws IOException {
    	stream.writeVersion(STATE_VERSION);
    	stream.writeBoolean(compareAtWrite);

    	super.write(stream);
	}

	@Override
	public String toString() {
		return String.format("WriteCacheVirtualFile %s", vFile);
	}
}
