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

import static jpcsp.memory.FastMemory.memory16Mask;
import static jpcsp.memory.FastMemory.memory16Shift;
import static jpcsp.memory.FastMemory.memory8Mask;
import static jpcsp.memory.FastMemory.memory8Shift;

import java.nio.Buffer;
import java.nio.ByteBuffer;

import jpcsp.Memory;
import jpcsp.HLE.TPointer;

public class IntArrayMemory extends Memory {
	private int[] memory;
	private int offset;

	public IntArrayMemory(int[] memory) {
		this.memory = memory;
		offset = 0;
	}

	public IntArrayMemory(int[] memory, int offset) {
		this.memory = memory;
		this.offset = offset;
	}

	public TPointer getPointer(int address) {
		return new TPointer(this, address).forceNonNull();
	}

	public TPointer getPointer() {
		return getPointer(0);
	}

	@Override
	public void Initialise() {
	}

	private int getOffset(int address) {
		return (address >> 2) + offset;
	}

	@Override
	public int read32(int address) {
		return memory[getOffset(address)];
	}

	@Override
	public int read16(int address) {
		int data = (memory[getOffset(address)] >> memory16Shift[address & 0x02]) & 0xFFFF;
		return data;
	}

	@Override
	public int read8(int address) {
		int data = (memory[getOffset(address)] >> memory8Shift[address & 0x03]) & 0xFF;
		return data;
	}

	@Override
	public void write32(int address, int value) {
		memory[getOffset(address)] = value;
	}

	@Override
	public void write16(int address, short value) {
		int index = address & 0x02;
		int memData = (memory[getOffset(address)] & memory16Mask[index]) | ((value & 0xFFFF) << memory16Shift[index]);
		memory[getOffset(address)] = memData;
	}

	@Override
	public void write8(int address, byte value) {
		int index = address & 0x03;
		int memData = (memory[getOffset(address)] & memory8Mask[index]) | ((value & 0xFF) << memory8Shift[index]);
		memory[getOffset(address)] = memData;
	}

	@Override
	public void memset(int address, byte data, int length) {
		for (int i = 0; i < length; i++) {
			write8(address + i, data);
		}
	}

	@Override
	public Buffer getMainMemoryByteBuffer() {
		return null;
	}

	@Override
	public Buffer getBuffer(int address, int length) {
		return null;
	}

	@Override
	public void copyToMemory(int address, ByteBuffer source, int length) {
		log.error(String.format("Unimplemented copyToMemory address=0x%08X, source=%s, length=0x%X", address, source, length));
	}

	@Override
	protected void memcpy(int destination, int source, int length, boolean checkOverlap) {
		log.error(String.format("Unimplemented memcpy destination=0x%08X, source=0x%08X, length=0x%X, checkOverlap=%b", destination, source, length, checkOverlap));
	}
}
