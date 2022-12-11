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
package jpcsp.mediaengine;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import jpcsp.HLE.kernel.types.MemoryChunk;
import jpcsp.HLE.kernel.types.MemoryChunkList;

/**
 * Emulating the Media Engine when LLE is not active.
 *
 * @author gid15
 *
 */
public class MEEmulator {
	public static Logger log = MEProcessor.log;
	private static MEEmulator instance;
	private static final int sizeAlignment = 64;
	private final MemoryChunkList freeMemoryChunks;
	private final Map<Integer, MemoryChunk> allocatedMemoryChunks;

	public static MEEmulator getInstance() {
		if (instance == null) {
			instance = new MEEmulator();
		}

		return instance;
	}

	private MEEmulator() {
		MemoryChunk initialMemory = new MemoryChunk(MEMemory.START_ME_RAM, MEMemory.SIZE_ME_RAM);
		freeMemoryChunks = new MemoryChunkList(initialMemory);

		// Allocate something at the lower end
		freeMemoryChunks.allocLow(0x10000, sizeAlignment);

		allocatedMemoryChunks = new HashMap<Integer, MemoryChunk>();
	}

	public MEProcessor getMEProcessor() {
		return MEProcessor.getInstance();
	}

	public MEMemory getMEMemory() {
		return getMEProcessor().getMEMemory();
	}

	public int malloc(int size) {
		MemoryChunk memoryChunk = freeMemoryChunks.allocLow(size, sizeAlignment);
		if (memoryChunk == null) {
			log.warn(String.format("MEEmulator.malloc cannot allocate size=0x%X", size));
			return 0;
		}

		int addr = memoryChunk.addr;
		allocatedMemoryChunks.put(addr, memoryChunk);

		return addr;
	}

	public void free(int addr) {
		MemoryChunk memoryChunk = allocatedMemoryChunks.remove(addr);
		if (memoryChunk == null) {
			log.warn(String.format("MEEmulator.free cannot free addr=0x%08X", addr));
			return;
		}

		freeMemoryChunks.add(memoryChunk);
	}

	public boolean isAllocated(int addr) {
		return allocatedMemoryChunks.containsKey(addr);
	}
}
