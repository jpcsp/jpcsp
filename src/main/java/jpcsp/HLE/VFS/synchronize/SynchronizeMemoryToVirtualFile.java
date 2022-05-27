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
package jpcsp.HLE.VFS.synchronize;

import static jpcsp.HLE.VFS.AbstractVirtualFileSystem.IO_ERROR;

import java.io.IOException;

import jpcsp.HLE.TPointer;
import jpcsp.HLE.VFS.IVirtualCache;
import jpcsp.HLE.VFS.IVirtualFile;
import jpcsp.state.StateInputStream;
import jpcsp.state.StateOutputStream;
import jpcsp.util.Utilities;

public class SynchronizeMemoryToVirtualFile extends BaseSynchronize {
	private static final int STATE_VERSION = 0;
	private TPointer input;
	private int inputSize;
	private IVirtualFile output;
	private long outputOffset;

	public SynchronizeMemoryToVirtualFile(String name, TPointer input, int inputSize, IVirtualFile output, Object lock) {
		super(name, lock);

		this.input = input;
		this.inputSize = inputSize;
		this.output = output;
		outputOffset = output.getPosition();
	}

	@Override
	public void read(StateInputStream stream) throws IOException {
    	stream.readVersion(STATE_VERSION);
    	super.read(stream);
	}

	@Override
	public void write(StateOutputStream stream) throws IOException {
    	stream.writeVersion(STATE_VERSION);
    	super.write(stream);
	}

	@Override
	protected int deltaSynchronize() {
		int result = 0;

		long seekPosition = output.ioLseek(outputOffset);
		if (seekPosition != outputOffset) {
			log.error(String.format("Error while seeking in %s to 0x%X", output, outputOffset));
			result = IO_ERROR;
		} else {
			int writeSize = output.ioWrite(input, inputSize);
			if (writeSize < 0) {
				log.error(String.format("Error while writing to %s: 0x%08X", output, writeSize));
				result = writeSize;
			} else if (writeSize != inputSize) {
				log.error(String.format("Error while writing to %s: could not write 0x%X bytes, but only 0x%X", output, inputSize, writeSize));
				result = IO_ERROR;
			} else {
				if (log.isDebugEnabled()) {
					log.debug(String.format("deltaSynchronize successfully written %s", Utilities.getMemoryDump(input, 0x1000)));
				}
			}
		}

		return result;
	}

	@Override
	protected void invalidateCachedData() {
		if (output instanceof IVirtualCache) {
			((IVirtualCache) output).invalidateCachedData();
		}
	}

	@Override
	protected void flushCachedData() {
		if (output instanceof IVirtualCache) {
			((IVirtualCache) output).flushCachedData();
		}
	}
}
