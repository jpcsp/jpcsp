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

import org.apache.log4j.Logger;

import jpcsp.HLE.VFS.ITmpVirtualFileSystem;
import jpcsp.HLE.VFS.IVirtualFile;
import jpcsp.settings.Settings;

public class TmpLocalVirtualFileSystem extends LocalVirtualFileSystem implements ITmpVirtualFileSystem {
	protected static Logger log = Logger.getLogger("vfs");

	public TmpLocalVirtualFileSystem() {
		super(Settings.getInstance().getTmpDirectory());
	}

	@Override
	public IVirtualFile ioOpen(String fileName, int flags, int mode, IPurpose purpose) {
		String purposeFileName = purpose.getFileName(fileName);

		if (log.isDebugEnabled()) {
			log.debug(String.format("TmpLocalVirtualFileSystem ioOpen %s -> %s", fileName, purposeFileName));
		}

		return ioOpen(purposeFileName, flags, mode);
	}

	@Override
	public IVirtualFile ioOpen(String fileName, int flags, int mode, IPurpose purpose, IVirtualFile originalFile) {
		if (originalFile == null) {
			return ioOpen(fileName, flags, mode, purpose);
		}

		String purposeFileName = purpose.getFileName(fileName);

		if (log.isDebugEnabled()) {
			log.debug(String.format("TmpLocalVirtualFileSystem ioOpen %s -> %s", fileName, purposeFileName));
		}

		IVirtualFile vFile = ioOpen(purposeFileName, flags, mode);
		if (vFile == null) {
			return null;
		}

		return new TmpLocalVirtualFile(vFile, originalFile);
	}
}
