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

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import jpcsp.GUI.UmdBrowser;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.VFS.AbstractVirtualFileSystem;
import jpcsp.HLE.VFS.IVirtualFile;
import jpcsp.HLE.VFS.IVirtualFileSystem;
import jpcsp.HLE.VFS.local.LocalVirtualFileSystem;
import jpcsp.HLE.kernel.types.SceIoDirent;
import jpcsp.HLE.kernel.types.SceIoStat;
import jpcsp.HLE.modules.IoFileMgrForUser;
import jpcsp.HLE.modules.IoFileMgrForUser.IoOperation;
import jpcsp.HLE.modules.IoFileMgrForUser.IoOperationTiming;
import jpcsp.settings.Settings;

public class XmbVirtualFileSystem extends AbstractVirtualFileSystem {
	public static final String PSP_GAME = "PSP/GAME";
	private static final String EBOOT_PBP = "EBOOT.PBP";
	private static final String DOCUMENT_DAT = "DOCUMENT.DAT";
	private static final String ISO_DIR = Settings.getInstance().getDirectoryMapping("ms0") + "ISO";
	private IVirtualFileSystem vfs;
	private File[] umdPaths;
	private Map<String, IVirtualFileSystem> umdVfs;
	private List<VirtualPBP> umdFiles;

	private static class VirtualPBP {
		String umdFile;
		IVirtualFile vFile;
	}

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

		umdFiles = new LinkedList<XmbVirtualFileSystem.VirtualPBP>();
	}

	private boolean isVirtualFile(String name) {
		return EBOOT_PBP.equals(name) || DOCUMENT_DAT.equals(name);
	}

	private String[] addUmdFileNames(String dirName, File[] files) {
		if (files == null) {
			return null;
		}

		String[] fileNames = new String[files.length];
		for (int i = 0; i < files.length; i++) {
			VirtualPBP virtualPBP = new VirtualPBP();
			virtualPBP.umdFile = files[i].getAbsolutePath();

			int umdIndex = umdFiles.size();
			umdFiles.add(virtualPBP);
			fileNames[i] = String.format("@UMD%d", umdIndex);

			if (log.isDebugEnabled()) {
				log.debug(String.format("%s=%s", fileNames[i], files[i].getAbsolutePath()));
			}
		}

		return fileNames;
	}

	private VirtualPBP getVirtualPBP(String fileName, StringBuilder restFileName) {
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
				if (umdIndex >= 0 && umdIndex < umdFiles.size()) {
					return umdFiles.get(umdIndex);
				}
			}
		}

		return null;
	}

	private String getUmdFileName(String fileName, StringBuilder restFileName) {
		VirtualPBP virtualPBP = getVirtualPBP(fileName, restFileName);
		if (virtualPBP == null) {
			return null;
		}

		return virtualPBP.umdFile;
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

	private int umdIoGetstat(String umdFileName, String restFileName, SceIoStat stat) {
		StringBuilder localFileName = new StringBuilder();
		IVirtualFileSystem vfs = getUmdVfs(umdFileName, localFileName);
		if (vfs != null) {
			int result = vfs.ioGetstat(localFileName.toString(), stat);
			// If the UMD file is actually a directory
			// (e.g. containing the EBOOT.PBP),
			// then stat the real file (EBOOT.PBP or DOCUMENT.DAT).
			if (restFileName != null && restFileName.length() > 0 && result == 0) {
				if ((stat.attr & 0x10) != 0) {
					result = vfs.ioGetstat(localFileName.toString() + "/" + restFileName, stat);
				}
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
			entries = new String[] { "..", ".", EBOOT_PBP };
		} else if (PSP_GAME.equals(dirName)) {
			for (int i = 0; i < umdPaths.length; i++) {
				File umdPath = umdPaths[i];
				if (umdPath.isDirectory()) {
					File[] umdFiles = umdPath.listFiles(new UmdBrowser.UmdFileFilter());
					entries = add(entries, addUmdFileNames(dirName, umdFiles));
				}
			}

			entries = add(vfs.ioDopen(dirName), entries);
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
			int result = umdIoGetstat(umdFileName, restFileName.toString(), dir.stat);
			if (result < 0) {
				return result;
			}

			return 1;
		}

		restFileName = new StringBuilder();
		umdFileName = getUmdFileName(dir.filename, restFileName);
		if (umdFileName != null && restFileName.length() == 0) {
			int result = umdIoGetstat(umdFileName, restFileName.toString(), dir.stat);
			if (result < 0) {
				return result;
			}

			// Change attribute from "file" to "directory"
			dir.stat.attr = (dir.stat.attr & ~0x20) | 0x10;
			dir.stat.mode = (dir.stat.mode & ~0x2000) | 0x1000;

			return 1;
		}

		return vfs.ioDread(dirName, dir);
	}

	@Override
	public int ioGetstat(String fileName, SceIoStat stat) {
		StringBuilder restFileName = new StringBuilder();
		String umdFileName = getUmdFileName(fileName, restFileName);
		if (umdFileName != null && isVirtualFile(restFileName.toString())) {
			return umdIoGetstat(umdFileName, restFileName.toString(), stat);
		}

		return vfs.ioGetstat(fileName, stat);
	}

	@Override
	public IVirtualFile ioOpen(String fileName, int flags, int mode) {
		StringBuilder restFileName = new StringBuilder();
		VirtualPBP virtualPBP = getVirtualPBP(fileName, restFileName);
		if (virtualPBP != null && isVirtualFile(restFileName.toString())) {
			String umdFileName = virtualPBP.umdFile;
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
			if (virtualPBP.vFile == null) {
				virtualPBP.vFile = new XmbIsoVirtualFile(umdFileName);
			}
			if (virtualPBP.vFile.length() > 0) {
				return virtualPBP.vFile;
			}

			if (log.isDebugEnabled()) {
				log.debug(String.format("XmbVirtualFileSystem.ioOpen could not open UMD file '%s'", umdFileName));
			}
		}

		return vfs.ioOpen(fileName, flags, mode);
	}

	@Override
	public Map<IoOperation, IoOperationTiming> getTimings() {
		// Do not delay IO operations on faked EBOOT.PBP files
		return IoFileMgrForUser.noDelayTimings;
	}

	@Override
	public int ioRename(String oldFileName, String newFileName) {
		return vfs.ioRename(oldFileName, newFileName);
	}

	@Override
	public int ioChstat(String fileName, SceIoStat stat, int bits) {
		return vfs.ioChstat(fileName, stat, bits);
	}

	@Override
	public int ioRemove(String name) {
		return vfs.ioRemove(name);
	}

	@Override
	public int ioMkdir(String name, int mode) {
		return vfs.ioMkdir(name, mode);
	}

	@Override
	public int ioRmdir(String name) {
		return vfs.ioRmdir(name);
	}

	@Override
	public int ioChdir(String directoryName) {
		return vfs.ioChdir(directoryName);
	}

	@Override
	public int ioMount() {
		return vfs.ioMount();
	}

	@Override
	public int ioUmount() {
		return vfs.ioUmount();
	}

	@Override
	public int ioDevctl(String deviceName, int command, TPointer inputPointer, int inputLength, TPointer outputPointer, int outputLength) {
		return vfs.ioDevctl(deviceName, command, inputPointer, inputLength, outputPointer, outputLength);
	}
}
