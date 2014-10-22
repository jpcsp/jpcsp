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
package jpcsp.format.psmf;

import static jpcsp.HLE.VFS.AbstractVirtualFileSystem.IO_ERROR;

import org.apache.log4j.Logger;

import jpcsp.Emulator;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.VFS.AbstractProxyVirtualFile;
import jpcsp.HLE.VFS.IVirtualFile;

/**
 * Provides a IVirtualFile interface to convert the audio from an Mpeg to OMA.
 * 
 * @author gid15
 *
 */
public class PsmfAudioOMAVirtualFile extends AbstractProxyVirtualFile {
	private static Logger log = Emulator.log;
	private int remainingFrameLength;
	private final byte[] header = new byte[8];

	public PsmfAudioOMAVirtualFile(IVirtualFile vFile) {
		super(vFile);
	}

	private boolean readHeader() {
		int length = vFile.ioRead(header, 0, header.length);
		if (length < header.length) {
			return false;
		}

		if (header[0] != (byte) 0x0F || header[1] != (byte) 0xD0) {
			log.warn(String.format("Invalid header 0x%02X 0x%02X", header[0] & 0xFF, header[1] & 0xFF));
			return false;
		}

		remainingFrameLength = (((header[2] & 0x03) << 8) | ((header[3] & 0xFF) << 3)) + 8;

		return true;
	}

	@Override
	public int ioRead(TPointer outputPointer, int outputLength) {
		return super.ioRead(outputPointer, outputLength);
	}

	@Override
	public int ioRead(byte[] outputBuffer, int outputOffset, int outputLength) {
		int readLength = 0;
		boolean error = false;

		while (outputLength > 0) {
			if (remainingFrameLength == 0) {
				if (!readHeader()) {
					error = true;
					break;
				}
			}

			int length = vFile.ioRead(outputBuffer, outputOffset, Math.min(outputLength, remainingFrameLength));
			if (length < 0) {
				error = true;
				break;
			}

			readLength += length;
			outputOffset += length;
			outputLength -= length;
			remainingFrameLength -= length;
		}

		if (error && readLength == 0) {
			return IO_ERROR;
		}

		return readLength;
	}
}
