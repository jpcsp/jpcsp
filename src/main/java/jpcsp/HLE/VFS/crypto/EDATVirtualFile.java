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
package jpcsp.HLE.VFS.crypto;

import jpcsp.HLE.VFS.IVirtualFile;

public class EDATVirtualFile extends PGDVirtualFile {
	private static final int edatHeaderSize = 0x90;

	public EDATVirtualFile(IVirtualFile pgdFile) {
		super(null, pgdFile, edatHeaderSize);
	}

	@Override
	protected boolean isHeaderValid(IVirtualFile pgdFile) {
		byte[] header = new byte[edatHeaderSize];
		long position = pgdFile.getPosition();
		int length = pgdFile.ioRead(header, 0, edatHeaderSize);
		pgdFile.ioLseek(position);

		if (length != edatHeaderSize) {
			return false;
		}

		if (header[0] != 0 || header[1] != 'P' || header[2] != 'S' || header[3] != 'P' || header[4] != 'E' || header[5] != 'D' || header[6] != 'A' || header[7] != 'T') {
            // No "EDAT" found in the header,
            log.warn("PSPEDAT header not found!");
            return false;
        }

		return super.isHeaderValid(pgdFile);
	}
}
