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
package jpcsp.HLE.kernel.types;

import static jpcsp.util.Utilities.min;

/**
 * Implements a circular buffer in PSP memory that can
 * be fed from a file.
 * Used by sceAtrac3plus and sceMp3 modules.
 * 
 * @author gid15
 *
 */
public class pspFileBuffer {
	private int addr;
	private int maxSize;
	private int currentSize;
	private int readPosition;
	private int writePosition;
	private int filePosition;
	private int fileMaxSize;

	public pspFileBuffer() {
	}

	public pspFileBuffer(int addr, int maxSize) {
		this.addr = addr;
		this.maxSize = maxSize;
	}

	public pspFileBuffer(int addr, int maxSize, int readSize) {
		this.addr = addr;
		this.maxSize = maxSize;
		notifyWrite(readSize);
	}

	public pspFileBuffer(int addr, int maxSize, int readSize, int filePosition) {
		this.addr = addr;
		this.maxSize = maxSize;
		notifyWrite(readSize);
		this.filePosition = filePosition;
	}

	public void setFileMaxSize(int fileMaxSize) {
		this.fileMaxSize = fileMaxSize;
	}

	public boolean isFileEnd() {
		return filePosition >= fileMaxSize;
	}

	public int getWriteAddr() {
		return addr + writePosition;
	}

	public int getWriteSize() {
		return min(maxSize - currentSize, maxSize - writePosition, fileMaxSize - filePosition);
	}

	public int getFileWriteSize() {
		return fileMaxSize - filePosition;
	}

	public int getFilePosition() {
		return filePosition;
	}

	public void setFilePosition(int filePosition) {
		this.filePosition = filePosition;
	}

	public int getReadAddr() {
		return addr + readPosition;
	}

	public int getReadSize() {
		return min(currentSize, maxSize - readPosition);
	}

	public int getCurrentSize() {
		return currentSize;
	}

	public void reset(int readSize, int filePosition) {
		currentSize = 0;
		readPosition = 0;
		writePosition = 0;
		notifyWrite(readSize);
		this.filePosition = filePosition;
	}

	public void notifyRead(int size) {
		if (size > 0) {
			size = min(size, currentSize);
			readPosition = incrementPosition(readPosition, size);
			currentSize -= size;
		}
	}

	public void notifyReadAll() {
		notifyRead(currentSize);
	}

	public void notifyWrite(int size) {
		if (size > 0) {
			size = min(size, getMaxSize() - currentSize);
			writePosition = incrementPosition(writePosition, size);
			filePosition += size;
			currentSize += size;
		}
	}

	private int incrementPosition(int position, int size) {
		position += size;
		if (position >= maxSize) {
			position -= maxSize;
		}

		return position;
	}

	public int getMaxSize() {
		return maxSize;
	}

	public int getAddr() {
		return addr;
	}

	public void setAddr(int addr) {
		this.addr = addr;
	}

	public boolean isEmpty() {
		return currentSize == 0;
	}

	@Override
	public String toString() {
		return String.format("pspFileBuffer(addr=0x%08X, maxSize=0x%X, currentSize=0x%X, readPosition=0x%X, writePosition=0x%X, filePosition=0x%X, fileMaxSize=0x%X)", getAddr(), getMaxSize(), currentSize, readPosition, writePosition, getFilePosition(), fileMaxSize);
	}
}
