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
import java.util.Map;

import org.apache.log4j.Logger;

import jpcsp.HLE.TPointer;
import jpcsp.HLE.modules.IoFileMgrForUser.IoOperation;
import jpcsp.HLE.modules.IoFileMgrForUser.IoOperationTiming;
import jpcsp.state.IState;
import jpcsp.state.StateInputStream;
import jpcsp.state.StateOutputStream;

/**
 * Proxy all the IVirtualFile interface calls to another virtual file.
 * 
 * @author gid15
 *
 */
public abstract class AbstractProxyVirtualFile implements IVirtualFile, IVirtualCache, IState {
	protected static Logger log = AbstractVirtualFileSystem.log;
	protected IVirtualFile vFile;

	protected AbstractProxyVirtualFile() {
	}

	protected AbstractProxyVirtualFile(IVirtualFile vFile) {
		this.vFile = vFile;
	}

	protected void setProxyVirtualFile(IVirtualFile vFile) {
		this.vFile = vFile;
	}

	@Override
	public int ioClose() {
		return vFile.ioClose();
	}

	@Override
	public int ioRead(TPointer outputPointer, int outputLength) {
		return vFile.ioRead(outputPointer, outputLength);
	}

	/*
	 * Perform the ioRead in PSP memory using the ioRead for a byte array.
	 */
	protected int ioReadBuf(TPointer outputPointer, int outputLength) {
		if (outputLength <= 0) {
			return 0;
		}

		byte[] outputBuffer = new byte[outputLength];
		int readLength = ioRead(outputBuffer, 0, outputLength);
		if (readLength > 0) {
			outputPointer.setArray(outputBuffer, readLength);
		}

		return readLength;
	}

	@Override
	public int ioRead(byte[] outputBuffer, int outputOffset, int outputLength) {
		return vFile.ioRead(outputBuffer, outputOffset, outputLength);
	}

	@Override
	public int ioWrite(TPointer inputPointer, int inputLength) {
		return vFile.ioWrite(inputPointer, inputLength);
	}

	@Override
	public int ioWrite(byte[] inputBuffer, int inputOffset, int inputLength) {
		return vFile.ioWrite(inputBuffer, inputOffset, inputLength);
	}

	@Override
	public long ioLseek(long offset) {
		return vFile.ioLseek(offset);
	}

	@Override
	public int ioIoctl(int command, TPointer inputPointer, int inputLength, TPointer outputPointer, int outputLength) {
		return vFile.ioIoctl(command, inputPointer, inputLength, outputPointer, outputLength);
	}

	@Override
	public long length() {
		return vFile.length();
	}

	@Override
	public boolean isSectorBlockMode() {
		return vFile.isSectorBlockMode();
	}

	@Override
	public long getPosition() {
		return vFile.getPosition();
	}

	@Override
	public IVirtualFile duplicate() {
		return vFile.duplicate();
	}

	@Override
	public Map<IoOperation, IoOperationTiming> getTimings() {
		return vFile.getTimings();
	}

	@Override
	public void invalidateCachedData() {
		if (vFile instanceof IVirtualCache) {
			((IVirtualCache) vFile).invalidateCachedData();
		}
	}

	@Override
	public void flushCachedData() {
		if (vFile instanceof IVirtualCache) {
			((IVirtualCache) vFile).flushCachedData();
		}
	}

	@Override
	public void closeCachedFiles() {
		if (vFile instanceof IVirtualCache) {
			((IVirtualCache) vFile).closeCachedFiles();
		}
	}

	@Override
	public void read(StateInputStream stream) throws IOException {
		if (vFile instanceof IState) {
			((IState) vFile).read(stream);
		}
	}

	@Override
	public void write(StateOutputStream stream) throws IOException {
		if (vFile instanceof IState) {
			((IState) vFile).write(stream);
		}
	}

	@Override
	public String toString() {
		return String.format("ProxyVirtualFile %s", vFile);
	}
}
