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
import java.util.Map;

import jpcsp.HLE.TPointer;
import jpcsp.HLE.modules.IoFileMgrForUser.IoOperation;
import jpcsp.HLE.modules.IoFileMgrForUser.IoOperationTiming;
import jpcsp.state.IState;
import jpcsp.state.StateInputStream;
import jpcsp.state.StateOutputStream;

/**
 * All reads and writes are failing.
 * Only seek and close operations are successful.
 *
 */
public class InvalidVirtualFile implements IVirtualFile, IState {
	private static final int STATE_VERSION = 0;
	private long position;

	public InvalidVirtualFile() {
	}

	@Override
	public void read(StateInputStream stream) throws IOException {
    	stream.readVersion(STATE_VERSION);
    	position = stream.readLong();
	}

	@Override
	public void write(StateOutputStream stream) throws IOException {
    	stream.writeVersion(STATE_VERSION);
    	stream.writeLong(position);
	}

	@Override
	public int ioClose() {
		return 0;
	}

	@Override
	public int ioRead(TPointer outputPointer, int outputLength) {
		return IO_ERROR;
	}

	@Override
	public int ioRead(byte[] outputBuffer, int outputOffset, int outputLength) {
		return IO_ERROR;
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
		if (offset < 0L) {
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
		return 0L;
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
