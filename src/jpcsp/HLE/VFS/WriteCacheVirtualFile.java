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

import static jpcsp.HLE.VFS.AbstractVirtualFile.IO_ERROR;

import java.io.IOException;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import jpcsp.HLE.TPointer;
import jpcsp.state.IState;
import jpcsp.state.StateInputStream;
import jpcsp.state.StateOutputStream;
import jpcsp.util.Utilities;

public class WriteCacheVirtualFile extends AbstractProxyVirtualFile implements IState {
	private static final int STATE_VERSION = 0;
	private final Logger log;
	private final SortedSet<Block> blocks = new TreeSet<Block>();
	private boolean compareAtWrite;

	private static class Block implements Comparable<Block>, IState {
		private static final int STATE_VERSION = 0;
		private long start;
		private long end;
		private byte[] data;

		public Block() {
		}

		public Block(long start, long end, byte[] data) {
			this.start = start;
			this.end = end;
			this.data = data;
		}

		public boolean isContaining(long position) {
			return start <= position && position < end;
		}

		public boolean isBefore(long position) {
			return end <= position;
		}

		public boolean isAfter(long position) {
			return position < start;
		}

		public boolean isBefore(Block block) {
			return isBefore(block.start);
		}

		public boolean isAfter(Block block) {
			return isAfter(block.end);
		}

		public boolean isOverlapping(Block block)  {
			return !isBefore(block) && !isAfter(block);
		}

		public boolean isFollowing(Block block) {
			return start == block.end;
		}

		public int getLength() {
			return (int) (end - start);
		}

		public int ioWrite(IVirtualFile vFile) {
			synchronized (vFile) {
				vFile.ioLseek(start);
				return vFile.ioWrite(data, 0, getLength());
			}
		}

		@Override
		public int compareTo(Block block) {
			return Long.compare(start, block.start);
		}

		@Override
		public void read(StateInputStream stream) throws IOException {
	    	stream.readVersion(STATE_VERSION);
			start = stream.readLong();
			end = stream.readLong();
			data = stream.readBytesWithLength();
		}

		@Override
		public void write(StateOutputStream stream) throws IOException {
	    	stream.writeVersion(STATE_VERSION);
			stream.writeLong(start);
			stream.writeLong(end);
			stream.writeBytesWithLength(data);
		}

		@Override
		public String toString() {
			return String.format("Block 0x%X-0x%X(length=0x%X)", start, end, getLength());
		}
	}

	public WriteCacheVirtualFile(Logger log, IVirtualFile vFile) {
		super(vFile);
		this.log = log;
	}

	public WriteCacheVirtualFile(Logger log, IVirtualFile vFile, boolean compareAtWrite) {
		super(vFile);
		this.log = log;

		this.compareAtWrite = compareAtWrite;
	}

	private void mergeBlocks(Block block1, Block block2) {
		long mergedStart = Math.min(block1.start, block2.start);
		long mergedEnd = Math.max(block1.end, block2.end);

		byte[] mergedData = new byte[(int) (mergedEnd - mergedStart)];
		System.arraycopy(block1.data, 0, mergedData, (int) (block1.start - mergedStart), block1.getLength());
		System.arraycopy(block2.data, 0, mergedData, (int) (block2.start - mergedStart), block2.getLength());

		Block mergedBlock = new Block(mergedStart, mergedEnd, mergedData);

		blocks.remove(block1);
		blocks.remove(block2);
		blocks.add(mergedBlock);
	}

	private void addBlock(Block blockToBeAdded) {
		if (blockToBeAdded.getLength() <= 0) {
			return;
		}

		for (Block block : blocks) {
			if (block.isFollowing(blockToBeAdded) || blockToBeAdded.isFollowing(block)) {
				mergeBlocks(block, blockToBeAdded);
				return;
			}

			if (block.isAfter(blockToBeAdded)) {
				break;
			}

			if (block.isOverlapping(blockToBeAdded)) {
				mergeBlocks(block, blockToBeAdded);
				return;
			}
		}

		blocks.add(blockToBeAdded);
	}

	@Override
	public int ioRead(TPointer outputPointer, int outputLength) {
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
	public int ioRead(byte[] outputBuffer, int outputOffset, int outputLength) {
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
					return IO_ERROR;
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
	public int ioWrite(TPointer inputPointer, int inputLength) {
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
	public int ioWrite(byte[] inputBuffer, int inputOffset, int inputLength) {
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

	@Override
	public void flushCachedData() {
		while (!blocks.isEmpty()) {
			Block block = blocks.first();
			blocks.remove(block);

			if (log.isTraceEnabled()) {
				log.trace(String.format("WriteCacheVirtualFile.flushCachedData %s", block));
			}

			int length = block.ioWrite(vFile);
			if (length < 0) {
				log.error(String.format("WriteCacheVirtualFile.flushCachedData failed 0x%08X for %s", length, block));
			}
		}

		super.flushCachedData();
	}

	@Override
	public void read(StateInputStream stream) throws IOException {
    	stream.readVersion(STATE_VERSION);
    	compareAtWrite = stream.readBoolean();
    	int numberBlocks = stream.readInt();
    	blocks.clear();
    	for (int i = 0; i < numberBlocks; i++) {
    		Block block = new Block();
    		block.read(stream);
    		blocks.add(block);
    	}

    	super.read(stream);
	}

	@Override
	public void write(StateOutputStream stream) throws IOException {
    	stream.writeVersion(STATE_VERSION);
    	stream.writeBoolean(compareAtWrite);
    	int numberBlocks = blocks.size();
    	stream.writeInt(numberBlocks);
    	for (Block block : blocks) {
    		block.write(stream);
    	}

    	super.write(stream);
	}

	@Override
	public String toString() {
		return String.format("WriteCacheVirtualFile %s", vFile);
	}
}
