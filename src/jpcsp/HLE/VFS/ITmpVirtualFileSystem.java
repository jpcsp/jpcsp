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

import static java.io.File.separatorChar;
import static java.lang.Math.abs;

import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.types.SceIoStat;
import jpcsp.settings.Settings;

public interface ITmpVirtualFileSystem extends IVirtualFileSystem {
	public static final IPurpose tmpPurposePGD = new PurposePGD();
	public static final IPurpose tmpPurposeAtrac = new PurposeAtrac();
	public IVirtualFile ioOpen(String fileName, int flags, int mode, IPurpose purpose);

	public static interface IPurpose {
		public String getFileName(String fileName);
	}

	public static abstract class AbstractPurpose implements IPurpose {
		protected String getFileName(String dir, String fileName) {
			return String.format("%s%s%c%s", Settings.getInstance().getDiscDirectory(), dir, separatorChar, fileName);
		}

		protected String getFileName(String dir1, String dir2, String fileName) {
			return String.format("%s%s%c%s%c%s", Settings.getInstance().getDiscDirectory(), dir1, separatorChar, dir2, separatorChar, fileName);
		}

		protected String getFileNameById(String fileName) {
			int fileId = 0;

			SceIoStat stat = Modules.IoFileMgrForUserModule.statFile(fileName);
			if (stat != null) {
				// Use the UMD start sector as file ID.
				fileId = stat.getStartSector();
			}

			if (fileId == 0 && fileName != null) {
				// If the file is not stored on the UMD (e.g. stored on ms0:),
				// use a unique ID based on the file name as file ID.
				fileId = abs(VirtualFileSystemManager.getFileNameLastPart(fileName).hashCode());
			}

			return String.format("File-%d", fileId);
		}
	}

	public static class PurposePGD extends AbstractPurpose {
		@Override
		public String getFileName(String fileName) {
			return getFileName("PGD", getFileNameById(fileName), "PGDfile.raw.decrypted");
		}
	}

	public static class PurposeAtrac extends AbstractPurpose {
		@Override
		public String getFileName(String fileName) {
			return getFileName("Atrac", fileName);
		}
	}
}
