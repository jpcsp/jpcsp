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
package jpcsp.HLE.VFS.xmb;

import static jpcsp.MainGUI.getUmdPaths;
import static jpcsp.util.Utilities.add;
import static jpcsp.util.Utilities.merge;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import jpcsp.GUI.UmdBrowser;
import jpcsp.HLE.VFS.AbstractVirtualFileSystem;
import jpcsp.HLE.VFS.IVirtualFile;
import jpcsp.HLE.VFS.IVirtualFileSystem;
import jpcsp.HLE.VFS.local.LocalVirtualFileSystem;
import jpcsp.HLE.kernel.types.SceIoDirent;
import jpcsp.HLE.kernel.types.SceIoStat;
import jpcsp.HLE.modules.IoFileMgrForUser;
import jpcsp.HLE.modules.IoFileMgrForUser.IoOperation;
import jpcsp.HLE.modules.IoFileMgrForUser.IoOperationTiming;

public class XmbVirtualFileSystem extends AbstractVirtualFileSystem {
	public static final String PSP_GAME = "PSP/GAME";
	private static final String EBOOT_PBP = "EBOOT.PBP";
	private static final String ISO_DIR = "ms0/ISO";
	private IVirtualFileSystem vfs;
	private File[] umdPaths;
	private Map<String, IVirtualFileSystem> umdVfs;
	private String[] umdFiles;

	public XmbVirtualFileSystem(IVirtualFileSystem vfs) {
		this.vfs = vfs;

		umdPaths = getUmdPaths(true);
		File isoDir = new File(ISO_DIR);
		if (isoDir.isDirectory()) {
			umdPaths = add(umdPaths, isoDir);
		}

		umdVfs = new HashMap<String, IVirtualFileSystem>();
		for (int i = 0; i < umdPaths.length; i++) {
			IVirtualFileSystem localVfs = new LocalVirtualFileSystem(umdPaths[i].getAbsolutePath() + "/", false);
			umdVfs.put(umdPaths[i].getAbsolutePath(), localVfs);
		}
	}

	private String[] addUmdFileNames(String dirName, File[] files) {
		if (files == null) {
			return null;
		}

		String[] fileNames = new String[files.length];
		for (int i = 0; i < files.length; i++) {
			umdFiles = add(umdFiles, files[i].getAbsolutePath());
			int umdIndex = umdFiles.length - 1;
			fileNames[i] = String.format("@UMD%d", umdIndex);

			if (log.isDebugEnabled()) {
				log.debug(String.format("%s=%s", fileNames[i], files[i].getAbsolutePath()));
			}
		}

		return fileNames;
	}

	private String getUmdFileName(String fileName, StringBuilder restFileName) {
		if (fileName != null) {
			int umdMarkerIndex = fileName.indexOf("@UMD");
			if (umdMarkerIndex >= 0) {
				String umdIndexString = fileName.substring(umdMarkerIndex + 4);
				int sepIndex = umdIndexString.indexOf("/");
				if (sepIndex >= 0) {
					if (restFileName != null) {
						restFileName.append(umdIndexString.substring(sepIndex + 1));
					}
					umdIndexString = umdIndexString.substring(0, sepIndex);
				}

				int umdIndex = Integer.parseInt(umdIndexString);
				if (umdIndex >= 0 && umdIndex < umdFiles.length) {
					return umdFiles[umdIndex];
				}
			}
		}

		return null;
	}

	private IVirtualFileSystem getUmdVfs(String umdFileName, StringBuilder localFileName) {
		for (String path : umdVfs.keySet()) {
			if (umdFileName.startsWith(path)) {
				if (localFileName != null) {
					localFileName.append(umdFileName.substring(path.length() + 1));
				}
				return umdVfs.get(path);
			}
		}

		return null;
	}

	private int umdIoGetstat(String umdFileName, SceIoStat stat) {
		StringBuilder localFileName = new StringBuilder();
		IVirtualFileSystem vfs = getUmdVfs(umdFileName, localFileName);
		if (vfs != null) {
			int result = vfs.ioGetstat(localFileName.toString(), stat);
			if (result == 0) {
				// Change attribute from "file" to "directory"
				stat.attr = (stat.attr & ~0x20) | 0x10;
				stat.mode = (stat.mode & ~0x2000) | 0x1000;
			}
			return result;
		}

		return IO_ERROR;
	}

	@Override
	public String[] ioDopen(String dirName) {
		String[] entries = null;

		StringBuilder restFileName = new StringBuilder();
		String umdFileName = getUmdFileName(dirName, restFileName);
		if (umdFileName != null && restFileName.length() == 0) {
			entries = new String[] { EBOOT_PBP };
		} else if (PSP_GAME.equals(dirName)) {
			for (int i = 0; i < umdPaths.length; i++) {
				File umdPath = umdPaths[i];
				if (umdPath.isDirectory()) {
					File[] umdFiles = umdPath.listFiles(new UmdBrowser.UmdFileFilter());
					entries = merge(entries, addUmdFileNames(dirName, umdFiles));
				}
			}

			entries = merge(entries, vfs.ioDopen(dirName));
		} else {
			entries = vfs.ioDopen(dirName);
		}

		return entries;
	}

	@Override
	public int ioDread(String dirName, SceIoDirent dir) {
		StringBuilder restFileName = new StringBuilder();
		String umdFileName = getUmdFileName(dirName, restFileName);
		if (umdFileName != null && restFileName.length() == 0 && EBOOT_PBP.equals(dir.filename)) {
			int result = umdIoGetstat(umdFileName, dir.stat);
			if (result < 0) {
				return result;
			}

			return 1;
		}

		restFileName = new StringBuilder();
		umdFileName = getUmdFileName(dir.filename, restFileName);
		if (umdFileName != null && restFileName.length() == 0) {
			int result = umdIoGetstat(umdFileName, dir.stat);
			if (result < 0) {
				return result;
			}

			return 1;
		}

		return vfs.ioDread(dirName, dir);
	}

	@Override
	public int ioGetstat(String fileName, SceIoStat stat) {
		StringBuilder restFileName = new StringBuilder();
		String umdFileName = getUmdFileName(fileName, restFileName);
		if (umdFileName != null && EBOOT_PBP.equals(restFileName.toString())) {
			return umdIoGetstat(umdFileName, stat);
		}

		return vfs.ioGetstat(fileName, stat);
	}

	@Override
	public IVirtualFile ioOpen(String fileName, int flags, int mode) {
		StringBuilder restFileName = new StringBuilder();
		String umdFileName = getUmdFileName(fileName, restFileName);
		if (umdFileName != null && EBOOT_PBP.equals(restFileName.toString())) {
			File umdFile = new File(umdFileName);

			// Is it a directory containing an EBOOT.PBP file?
			if (umdFile.isDirectory()) {
				StringBuilder localFileName = new StringBuilder();
				IVirtualFileSystem vfs = getUmdVfs(umdFileName, localFileName);
				if (vfs != null) {
					// Open the EBOOT.PBP file inside the directory
					return vfs.ioOpen(localFileName.toString() + "/" + restFileName.toString(), flags, mode);
				}
			}

			// Map the ISO/CSO file into a virtual PBP file
			IVirtualFile vFile = new XmbIsoVirtualFile(umdFileName);
			if (vFile.length() > 0) {
				return vFile;
			}

			if (log.isDebugEnabled()) {
				log.debug(String.format("XmbVirtualFileSystem.ioOpen could not open UMD file '%s'", umdFileName));
			}
			vFile.ioClose();
		}

		return vfs.ioOpen(fileName, flags, mode);
	}

	@Override
	public Map<IoOperation, IoOperationTiming> getTimings() {
		// Do not delay IO operations on faked EBOOT.PBP files
		return IoFileMgrForUser.noDelayTimings;
	}
}
