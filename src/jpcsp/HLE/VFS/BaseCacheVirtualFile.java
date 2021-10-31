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
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import jpcsp.state.IState;
import jpcsp.state.StateInputStream;
import jpcsp.state.StateOutputStream;

public class BaseCacheVirtualFile extends AbstractProxyVirtualFile implements IState {
	private static final int STATE_VERSION = 0;
	protected final Logger log;
	protected final SortedSet<Block> blocks = new TreeSet<Block>();

	protected static class Block implements Comparable<Block>, IState {
		private static final int STATE_VERSION = 0;
		protected long start;
		protected long end;
		protected byte[] data;

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

	public BaseCacheVirtualFile(Logger log, IVirtualFile vFile) {
		super(vFile);
		this.log = log;
	}

	public void setProxyVirtualFile(IVirtualFile vFile) {
		super.setProxyVirtualFile(vFile);
	}

	protected Block mergeBlocks(Block block1, Block block2) {
		long mergedStart = Math.min(block1.start, block2.start);
		long mergedEnd = Math.max(block1.end, block2.end);

		byte[] mergedData = new byte[(int) (mergedEnd - mergedStart)];
		System.arraycopy(block1.data, 0, mergedData, (int) (block1.start - mergedStart), block1.getLength());
		System.arraycopy(block2.data, 0, mergedData, (int) (block2.start - mergedStart), block2.getLength());

		Block mergedBlock = new Block(mergedStart, mergedEnd, mergedData);

		blocks.remove(block1);
		blocks.remove(block2);
		blocks.add(mergedBlock);

		return mergedBlock;
	}

	protected void addBlock(Block blockToBeAdded) {
		if (blockToBeAdded.getLength() <= 0) {
			return;
		}

		for (Iterator<Block> it = blocks.iterator(); it.hasNext(); ) {
			Block block = it.next();

			if (block.isFollowing(blockToBeAdded)) {
				mergeBlocks(block, blockToBeAdded);
				return;
			}
			if (blockToBeAdded.isFollowing(block)) {
				if (it.hasNext()) {
					// The next block has to be retrieved first, before the block list is being modified
					Block nextBlock = it.next();
					// Merge with the previous block
					Block mergedBlock = mergeBlocks(block, blockToBeAdded);
					// Verify if the next block has to be merged as well
					if (nextBlock.isFollowing(mergedBlock)) {
						mergeBlocks(nextBlock, mergedBlock);
					}
				} else {
					mergeBlocks(block, blockToBeAdded);
				}
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
	public void read(StateInputStream stream) throws IOException {
    	stream.readVersion(STATE_VERSION);
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
    	int numberBlocks = blocks.size();
    	stream.writeInt(numberBlocks);
    	for (Block block : blocks) {
    		block.write(stream);
    	}

    	super.write(stream);
	}

	@Override
	public String toString() {
		return String.format("BaseCacheVirtualFile %s", vFile);
	}
}
