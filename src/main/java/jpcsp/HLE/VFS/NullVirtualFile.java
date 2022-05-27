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
import java.util.Arrays;
import java.util.Map;

import jpcsp.HLE.TPointer;
import jpcsp.HLE.modules.IoFileMgrForUser.IoOperation;
import jpcsp.HLE.modules.IoFileMgrForUser.IoOperationTiming;
import jpcsp.state.IState;
import jpcsp.state.StateInputStream;
import jpcsp.state.StateOutputStream;

/**
 * All reads return 0-bytes.
 * Fails on writes.
 *
 */
public class NullVirtualFile implements IVirtualFile, IState {
	private static final int STATE_VERSION = 0;
	private long size;
	private long position;

	public NullVirtualFile(long size) {
		this.size = size;
	}

	@Override
	public void read(StateInputStream stream) throws IOException {
    	stream.readVersion(STATE_VERSION);
    	size = stream.readLong();
    	position = stream.readLong();
	}

	@Override
	public void write(StateOutputStream stream) throws IOException {
    	stream.writeVersion(STATE_VERSION);
    	stream.writeLong(size);
    	stream.writeLong(position);
	}

	@Override
	public int ioClose() {
		return 0;
	}

	private int getMaxOutputLength(int outputLength) {
		if (outputLength <= 0) {
			return 0;
		}

		if (position + outputLength > size) {
			return (int) (size - position);
		}

		return outputLength;
	}

	@Override
	public int ioRead(TPointer outputPointer, int outputLength) {
		outputLength = getMaxOutputLength(outputLength);
		outputPointer.clear(outputLength);

		position += outputLength;

		return outputLength;
	}

	@Override
	public int ioRead(byte[] outputBuffer, int outputOffset, int outputLength) {
		outputLength = getMaxOutputLength(outputLength);
		Arrays.fill(outputBuffer, outputOffset, outputOffset + outputLength, (byte) 0);

		position += outputLength;

		return outputLength;
	}

	@Override
	public int ioWrite(TPointer inputPointer, int inputLength) {
		return IO_ERROR;
	}

	@Override
	public int ioWrite(byte[] inputBuffer, int inputOffset, int inputLength) {
		return IO_ERROR;
	}

	@Override
	public long ioLseek(long offset) {
		if (offset < 0L || offset > size) {
			return IO_ERROR;
		}

		position = offset;

		return offset;
	}

	@Override
	public int ioIoctl(int command, TPointer inputPointer, int inputLength, TPointer outputPointer, int outputLength) {
		return IO_ERROR;
	}

	@Override
	public long length() {
		return size;
	}

	@Override
	public boolean isSectorBlockMode() {
		return false;
	}

	@Override
	public long getPosition() {
		return position;
	}

	@Override
	public IVirtualFile duplicate() {
		return null;
	}

	@Override
	public Map<IoOperation, IoOperationTiming> getTimings() {
		return null;
	}
}
