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
package jpcsp.HLE.VFS.local;

import java.io.IOException;

import jpcsp.HLE.TPointer;
import jpcsp.HLE.VFS.AbstractVirtualFile;
import jpcsp.filesystems.SeekableRandomFile;
import jpcsp.util.Utilities;

public class LocalVirtualFile extends AbstractVirtualFile {
	protected SeekableRandomFile file;
	protected boolean truncateAtNextWrite;

	public LocalVirtualFile(SeekableRandomFile file) {
		super(file);
		this.file = file;
	}

	@Override
	public int ioWrite(TPointer inputPointer, int inputLength) {
		try {
			Utilities.write(file, inputPointer.getAddress(), inputLength);
		} catch (IOException e) {
			log.error("ioWrite", e);
			return IO_ERROR;
		}

		return inputLength;
	}

	@Override
	public int ioWrite(byte[] inputBuffer, int inputOffset, int inputLength) {
		try {
			if (isTruncateAtNextWrite()) {
            	// The file was open with PSP_O_TRUNC: truncate the file at the first write
				long position = getPosition();
				if (position < file.length()) {
					file.setLength(getPosition());
				}
				setTruncateAtNextWrite(false);
			}

			file.write(inputBuffer, inputOffset, inputLength);
		} catch (IOException e) {
			log.error("ioWrite", e);
			return IO_ERROR;
		}

		return inputLength;
	}

	public boolean isTruncateAtNextWrite() {
		return truncateAtNextWrite;
	}

	public void setTruncateAtNextWrite(boolean truncateAtNextWrite) {
		this.truncateAtNextWrite = truncateAtNextWrite;
	}

	@Override
	public String toString() {
		return String.format("LocalVirtualFile %s", file);
	}
}
