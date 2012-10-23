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
package jpcsp.memory;

import static jpcsp.util.Utilities.round4;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;

import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.MemoryMap;
import jpcsp.HLE.Modules;

public class FastMemory extends Memory {
	//
	// In a typical application, the following read/write operations are performed:
	//   - read8  :  1,45% of total read/write,  1,54% of total read operations
	//   - read16 : 13,90% of total read/write, 14,76% of total read operations
	//   - read32 : 78,80% of total read/write, 83,70% of total read operations
	//   - write8 :  1,81% of total read/write, 30,96% of total write operations
	//   - write16:  0,02% of total read/write,  0,38% of total write operations
	//   - write32:  4,02% of total read/write, 68,67% of total write operations
	//
	// This is why this Memory implementation is optimized for fast read32 operations.
	// Drawback is the higher memory requirements.
	//
	// This implementation is performing very few checks for the validity of
	// memory address references to achieve the highest performance.
	// Use SafeFastMemory for complete address checks.
	//
	private int[] all;

	// Enable/disable read & write tracing.
	// Use final variables to not reduce the performance
	// (the code is removed/inserted at Java compile time)
	private static final boolean traceRead  = false;
	private static final boolean traceWrite = false;

	@Override
	public boolean allocate() {
		// Free previously allocated memory
		all = null;

		int allSize = (MemoryMap.END_RAM + 1) >> 2;
		try {
			all = new int[allSize];
		} catch (OutOfMemoryError e) {
			// Not enough memory provided for this VM, cannot use FastMemory model
			Memory.log.warn("Cannot allocate FastMemory: add the option '-Xmx256m' to the Java Virtual Machine startup command to improve Performance");
			Memory.log.info("The current Java Virtual Machine has been started using '-Xmx" + (Runtime.getRuntime().maxMemory() / (1024 * 1024)) + "m'");
			return false;
		}

		return super.allocate();
	}

	@Override
	public void Initialise() {
		Arrays.fill(all, 0);
	}

	@Override
	public int read8(int address) {
		try {
			address &= addressMask;
			int data = all[address >> 2];
			switch (address & 0x03) {
				case 1: data >>=  8; break;
				case 2: data >>= 16; break;
				case 3: data >>= 24; break;
			}

			if (traceRead) {
				if (log.isTraceEnabled()) {
					log.trace("read8(0x" + Integer.toHexString(address).toUpperCase() + ")=0x" + Integer.toHexString(data & 0xFF).toUpperCase());
				}
			}
	
			return data & 0xFF;
		} catch (Exception e) {
            invalidMemoryAddress(address, "read8", Emulator.EMU_STATUS_MEM_READ);
			return 0;
		}
	}

	@Override
	public int read16(int address) {
		try {
			address &= addressMask;
			int data = all[address >> 2];
			if ((address & 0x02) != 0) {
				data >>= 16;
			}

			if (traceRead) {
				if (log.isTraceEnabled()) {
					log.trace("read16(0x" + Integer.toHexString(address).toUpperCase() + ")=0x" + Integer.toHexString(data & 0xFFFF).toUpperCase());
				}
			}

			return data & 0xFFFF;
		} catch (Exception e) {
            invalidMemoryAddress(address, "read16", Emulator.EMU_STATUS_MEM_READ);
			return 0;
		}
	}

	@Override
	public int read32(int address) {
		try {
			address &= addressMask;

			if (traceRead) {
				if (log.isTraceEnabled()) {
					log.trace("read32(0x" + Integer.toHexString(address).toUpperCase() + ")=0x" + Integer.toHexString(all[address >> 2]).toUpperCase() + " (" + Float.intBitsToFloat(all[address >> 2]) + ")");
				}
			}

			return all[address >> 2];
		} catch (Exception e) {
			if (read32AllowedInvalidAddress(address)) {
				return 0;
			}

            invalidMemoryAddress(address, "read32", Emulator.EMU_STATUS_MEM_READ);
			return 0;
		}
	}

	@Override
	public long read64(int address) {
		try {
			address &= addressMask;
			long data = (((long) all[(address >> 2) + 1]) << 32) | (((long) all[address >> 2]) & 0xFFFFFFFFL);

			if (traceRead) {
				if (log.isTraceEnabled()) {
					log.trace("read64(0x" + Integer.toHexString(address).toUpperCase() + ")=0x" + Long.toHexString(data).toUpperCase());
				}
			}

			return data;
		} catch (Exception e) {
            invalidMemoryAddress(address, "read64", Emulator.EMU_STATUS_MEM_READ);
			return 0;
		}
	}

	@Override
	public void write8(int address, byte data) {
		try {
			address &= addressMask;
			int memData = all[address >> 2];
			switch (address & 0x03) {
			case 0: memData = (memData & 0xFFFFFF00) | ((data & 0xFF)      ); break;
			case 1: memData = (memData & 0xFFFF00FF) | ((data & 0xFF) <<  8); break;
			case 2: memData = (memData & 0xFF00FFFF) | ((data & 0xFF) << 16); break;
			case 3: memData = (memData & 0x00FFFFFF) | ((data & 0xFF) << 24); break;
			}

			if (traceWrite) {
				if (log.isTraceEnabled()) {
					log.trace("write8(0x" + Integer.toHexString(address).toUpperCase() + ", 0x" + Integer.toHexString(data & 0xFF).toUpperCase() + ")");
				}
			}

			all[address >> 2] = memData;
            Modules.sceDisplayModule.write8(address);
		} catch (Exception e) {
            invalidMemoryAddress(address, "write8", Emulator.EMU_STATUS_MEM_WRITE);
		}
	}

	@Override
	public void write16(int address, short data) {
		try {
			address &= addressMask;
			int memData = all[address >> 2];
			if ((address & 0x02) == 0) {
				memData = (memData & 0xFFFF0000) | (data & 0xFFFF);
			} else {
				memData = (memData & 0x0000FFFF) | (data << 16);
			}

			if (traceWrite) {
				if (log.isTraceEnabled()) {
					log.trace("write16(0x" + Integer.toHexString(address).toUpperCase() + ", 0x" + Integer.toHexString(data & 0xFFFF).toUpperCase() + ")");
				}
			}

			all[address >> 2] = memData;
            Modules.sceDisplayModule.write16(address);
		} catch (Exception e) {
            invalidMemoryAddress(address, "write16", Emulator.EMU_STATUS_MEM_WRITE);
		}
	}

	@Override
	public void write32(int address, int data) {
		try {
			address &= addressMask;
			all[address >> 2] = data;

			if (traceWrite) {
				if (log.isTraceEnabled()) {
					log.trace("write32(0x" + Integer.toHexString(address).toUpperCase() + ", 0x" + Integer.toHexString(data).toUpperCase() + " (" + Float.intBitsToFloat(data) + "))");
				}
			}

			Modules.sceDisplayModule.write32(address);
		} catch (Exception e) {
            invalidMemoryAddress(address, "write32", Emulator.EMU_STATUS_MEM_WRITE);
		}
	}

	@Override
	public void write64(int address, long data) {
		try {
			address &= addressMask;

			if (traceWrite) {
				if (log.isTraceEnabled()) {
					log.trace("write64(0x" + Integer.toHexString(address).toUpperCase() + ", 0x" + Long.toHexString(data).toUpperCase() + ")");
				}
			}

			all[address >> 2] = (int) data;
			all[(address >> 2) + 1] = (int) (data >> 32);
		} catch (Exception e) {
            invalidMemoryAddress(address, "write64", Emulator.EMU_STATUS_MEM_WRITE);
		}
	}

	@Override
	public IntBuffer getMainMemoryByteBuffer() {
		return IntBuffer.wrap(all, MemoryMap.START_RAM >> 2, MemoryMap.SIZE_RAM >> 2);
	}

	@Override
	public IntBuffer getBuffer(int address, int length) {
		address = normalizeAddress(address);

		IntBuffer buffer = getMainMemoryByteBuffer();
		buffer.position(address >> 2);
		buffer.limit(round4(address + length) >> 2);

		return buffer.slice();
	}

	private boolean isIntAligned(int n) {
		return (n & 0x03) == 0;
	}

	@Override
	public void memset(int address, byte data, int length) {
		address = normalizeAddress(address);

		for (; !isIntAligned(address) && length > 0; address++, length--) {
			write8(address, data);
		}

		int count4 = length >> 2;
		if (count4 > 0) {
			int data1 = data & 0xFF;
			int data4 = (data1 << 24) | (data1 << 16) | (data1 << 8) | data1;
			Arrays.fill(all, address >> 2, (address >> 2) + count4, data4);
			address += count4 << 2;
			length -= count4 << 2;
		}

		for (; length > 0; address++, length--) {
			write8(address, data);
		}
	}

	@Override
	public void copyToMemory(int address, ByteBuffer source, int length) {
		// copy in 1 byte steps until address is "int"-aligned
		while (!isIntAligned(address) && length > 0 && source.hasRemaining()) {
			byte b = source.get();
			write8(address, b);
			address++;
			length--;
		}

		// copy 1 int at each loop
		int countInt = Math.min(length, source.remaining()) >> 2;
		IMemoryWriter memoryWriter = MemoryWriter.getMemoryWriter(address, countInt << 2, 4);
		for (int i = 0; i < countInt; i++) {
			int data1 = source.get() & 0xFF;
			int data2 = source.get() & 0xFF;
			int data3 = source.get() & 0xFF;
			int data4 = source.get() & 0xFF;
			int data = (data4 << 24) | (data3 << 16) | (data2 << 8) | data1;
			memoryWriter.writeNext(data);
		}
		memoryWriter.flush();
		int copyLength = countInt << 2;
		length -= copyLength;
		address += copyLength;

		// copy rest length in 1 byte steps (rest length <= 3)
		while (length > 0 && source.hasRemaining()) {
			byte b = source.get();
			write8(address, b);
			address++;
			length--;
		}
	}

	public int[] getAll() {
	    return all;
	}

	// Source, destination and length are "int"-aligned
	private void memcpyAligned4(int destination, int source, int length, boolean checkOverlap) {
		if (checkOverlap || !areOverlapping(destination, source, length)) {
			// Direct copy, System.arraycopy is handling correctly overlapping arrays
			System.arraycopy(all, source >> 2, all, destination >> 2, length >> 2);
		} else {
			// Buffers are overlapping, but we have to copy as they would not overlap.
			// Unfortunately, IntBuffer operation are always checking for overlapping buffers,
			// so we have to copy manually...
			IMemoryReader sourceReader = MemoryReader.getMemoryReader(source, length, 4);
			IMemoryWriter destinationWriter = MemoryWriter.getMemoryWriter(destination, length, 4);
			for (int i = 0; i < length; i += 4) {
				destinationWriter.writeNext(sourceReader.readNext());
			}
			destinationWriter.flush();
		}
	}

	@Override
    protected void memcpy(int destination, int source, int length, boolean checkOverlap) {
    	if (length <= 0) {
    		return;
    	}

    	destination = normalizeAddress(destination);
		source = normalizeAddress(source);

		if (isIntAligned(source) && isIntAligned(destination) && isIntAligned(length)) {
			// Source, destination and length are "int"-aligned
			memcpyAligned4(destination, source, length, checkOverlap);
		} else if ((source & 0x03) == (destination & 0x03) && (!checkOverlap || !areOverlapping(destination, source, length))) {
			// Source and destination have the same alignment and are not overlapping
			while (!isIntAligned(source) && length > 0) {
				write8(destination, (byte) read8(source));
				source++;
				destination++;
				length--;
			}

			int length4 = length & ~0x03;
			if (length4 > 0) {
				memcpyAligned4(destination, source, length4, checkOverlap);
				source += length4;
				destination += length4;
				length -= length4;
			}

			while (length > 0) {
				write8(destination, (byte) read8(source));
				destination++;
				source++;
				length--;
			}
		} else {
			//
			// Buffers are not "int"-aligned, copy in 1 byte steps.
			// Overlapping address ranges must be correctly handled:
			//   If source >= destination:
			//                 [---source---]
			//       [---destination---]
			//      => Copy from the head
			//   If source < destination:
			//       [---source---]
			//                 [---destination---]
			//      => Copy from the tail
			//
			if (!checkOverlap || source >= destination || !areOverlapping(destination, source, length)) {
				if (areOverlapping(destination, source, 4)) {
					// Cannot use MemoryReader if source and destination are overlapping in less than 4 bytes
					for (int i = 0; i < length; i++) {
						write8(destination + i, (byte) read8(source + i));
					}
				} else {
					IMemoryReader sourceReader = MemoryReader.getMemoryReader(source, length, 1);
					for (int i = 0; i < length; i++) {
						write8(destination + i, (byte) sourceReader.readNext());
					}
				}
			} else {
				for (int i = length - 1; i >= 0; i--) {
					write8(destination + i, (byte) read8(source + i));
				}
			}
		}
	}
}
