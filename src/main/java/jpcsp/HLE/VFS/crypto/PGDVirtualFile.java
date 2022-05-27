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

import jpcsp.HLE.VFS.BufferedVirtualFile;
import jpcsp.HLE.VFS.IVirtualFile;

public class PGDVirtualFile extends BufferedVirtualFile {
	private boolean isValid;
	private boolean isHeaderPresent;

	public PGDVirtualFile(byte[] key, IVirtualFile pgdFile) {
		init(key, pgdFile, 0);
	}

	public PGDVirtualFile(byte[] key, IVirtualFile pgdFile, int dataOffset) {
		init(key, pgdFile, dataOffset);
	}

	private void init(byte[] key, IVirtualFile pgdFile, int dataOffset) {
		isValid = false;

		long position = pgdFile.getPosition();
		if (isHeaderValid(pgdFile)) {
			PGDBlockVirtualFile pgdBlockFile = new PGDBlockVirtualFile(pgdFile, key, dataOffset);

			isHeaderPresent = pgdBlockFile.isHeaderPresent();
			if (pgdBlockFile.isHeaderValid()) {
				setBufferedVirtualFile(pgdBlockFile, pgdBlockFile.getBlockSize());
				isValid = true;
			}
		}

		if (!isValid) {
			pgdFile.ioLseek(position);
			setBufferedVirtualFile(pgdFile, 0x1000);
		}
	}

	protected boolean isHeaderValid(IVirtualFile pgdFile) {
		return true;
	}

	public boolean isValid() {
		return isValid;
	}

	public boolean isHeaderPresent() {
		return isHeaderPresent;
	}
}
