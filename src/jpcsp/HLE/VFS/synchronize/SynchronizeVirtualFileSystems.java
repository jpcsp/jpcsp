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
import java.util.HashSet;
import java.util.Set;

import jpcsp.HLE.VFS.IVirtualCache;
import jpcsp.HLE.VFS.IVirtualFile;
import jpcsp.HLE.VFS.IVirtualFileSystem;
import jpcsp.HLE.kernel.types.SceIoDirent;
import jpcsp.HLE.kernel.types.SceIoStat;
import jpcsp.HLE.kernel.types.ScePspDateTime;
import jpcsp.HLE.modules.IoFileMgrForUser;
import jpcsp.state.StateInputStream;
import jpcsp.state.StateOutputStream;
import jpcsp.util.Utilities;

public class SynchronizeVirtualFileSystems extends BaseSynchronize {
	private static final int STATE_VERSION = 0;
	private IVirtualFileSystem input;
	private IVirtualFileSystem output;
	private ScePspDateTime lastSyncDate;

	public SynchronizeVirtualFileSystems(String name, IVirtualFileSystem input, IVirtualFileSystem output, Object lock) {
		super(name, lock);
		this.input = input;
		this.output = output;

		lastSyncDate = nowDate();
	}

	@Override
	public void read(StateInputStream stream) throws IOException {
    	stream.readVersion(STATE_VERSION);
		lastSyncDate.read(stream);
    	super.read(stream);
	}

	@Override
	public void write(StateOutputStream stream) throws IOException {
    	stream.writeVersion(STATE_VERSION);
    	lastSyncDate.write(stream);
    	super.write(stream);
	}

	@Override
	protected void invalidateCachedData() {
		if (input instanceof IVirtualCache) {
			((IVirtualCache) input).invalidateCachedData();
		}
		if (output instanceof IVirtualCache) {
			((IVirtualCache) output).invalidateCachedData();
		}
	}

	private void closeCachedFiles() {
		if (input instanceof IVirtualCache) {
			((IVirtualCache) input).closeCachedFiles();
		}
		if (output instanceof IVirtualCache) {
			((IVirtualCache) output).closeCachedFiles();
		}
	}

	@Override
	protected int deltaSynchronize() {
		invalidateCachedData();
		closeCachedFiles();

		ScePspDateTime newLastSync = nowDate();

		if (log.isTraceEnabled()) {
			log.trace(String.format("deltaSynchronize %s start", name));
		}

		int result = deltaSynchronize("");

		if (log.isTraceEnabled()) {
			log.trace(String.format("deltaSynchronize %s end", name));
		}

		// The new lastSync is the time when this sync has been started
		// (and not when it finished).
		lastSyncDate = newLastSync;

		return result;
	}

	private boolean isModifiedSinceLastSync(SceIoDirent dirent) {
		if (dirent.stat.mtime.after(lastSyncDate)) {
			return true;
		}

		// When modifying an entry, the PSP is sometimes
		// writing no time at all in the directory entry.
		if (dirent.stat.mtime.toMSDOSTime() == 0) {
			return true;
		}

		return false;
	}

	private boolean isDirectory(SceIoDirent entry) {
		return (entry.stat.attr & 0x10) != 0;
	}

	private boolean sameEntryNameAndAttributes(SceIoDirent dirent1, SceIoDirent dirent2) {
		if (dirent1 == null) {
			return dirent2 == null;
		}
		if (dirent2 == null) {
			return dirent1 == null;
		}

		if (!dirent1.filename.equalsIgnoreCase(dirent2.filename)) {
			return false;
		}

		if (dirent1.stat.attr != dirent2.stat.attr) {
			return false;
		}

		if (dirent1.stat.mode != dirent2.stat.mode) {
			return false;
		}

		if (dirent1.stat.size != dirent2.stat.size) {
			return false;
		}

		return true;
	}

	private SceIoDirent[] getDirectoryEntries(IVirtualFileSystem vfs, String dirName) {
		String[] fileNames = vfs.ioDopen(dirName);

		SceIoDirent entries[] = new SceIoDirent[0];
		if (fileNames != null) {
			for (String fileName : fileNames) {
				if (!".".equals(fileName) && !"..".equals(fileName)) {
					SceIoDirent entry = new SceIoDirent(new SceIoStat(), fileName);
					int result = vfs.ioDread(dirName, entry);
					if (result != 1) {
						return null;
					}
					entries = Utilities.add(entries, entry);
				}
			}
		}

		return entries;
	}

	private int deltaSynchronize(String dirName) {
		if (log.isTraceEnabled()) {
			log.trace(String.format("deltaSynchronize '%s'", dirName));
		}

		SceIoDirent inputEntries[] = getDirectoryEntries(input, dirName);
		if (inputEntries == null) {
			return IO_ERROR;
		}
		SceIoDirent outputEntries[] = getDirectoryEntries(output, dirName);
		if (outputEntries == null) {
			return IO_ERROR;
		}

		Set<SceIoDirent> toBeUpdated = new HashSet<SceIoDirent>();
		Set<SceIoDirent> toBeCreated = new HashSet<SceIoDirent>();
		for (int i = 0; i < inputEntries.length; i++) {
			SceIoDirent inputEntry = inputEntries[i];
			if (log.isTraceEnabled()) {
				log.trace(String.format("deltaSynchronize '%s', entry=%s", dirName, inputEntry));
			}

			boolean found = false;
			for (int j = 0; j < outputEntries.length; j++) {
				if (sameEntryNameAndAttributes(inputEntry, outputEntries[j])) {
					if (!isDirectory(inputEntry) && isModifiedSinceLastSync(inputEntry)) {
						if (log.isDebugEnabled()) {
							log.debug(String.format("deltaSynchronize: entry to be updated inputEntry=%s, lastSyncDate=%s", inputEntry, lastSyncDate));
						}
						toBeUpdated.add(inputEntry);
					}
					outputEntries[j] = null;
					found = true;
					break;
				}
			}

			if (!found) {
				if (log.isDebugEnabled()) {
					log.debug(String.format("deltaSynchronize: entry to be created inputEntry=%s", inputEntry));
				}
				toBeCreated.add(inputEntry);
			}
		}

		for (int i = 0; i < outputEntries.length; i++) {
			if (outputEntries[i] != null) {
				if (log.isDebugEnabled()) {
					log.debug(String.format("deltaSynchronize: entry to be deleted outputEntry=%s", outputEntries[i]));
				}
				int result = deleteEntry(dirName, outputEntries[i]);
				if (result != 0) {
					return result;
				}
			}
		}

		for (SceIoDirent entry : toBeCreated) {
			int result = createEntry(dirName, entry);
			if (result != 0) {
				return result;
			}
		}

		for (SceIoDirent entry : toBeUpdated) {
			int result = updateEntry(dirName, entry);
			if (result != 0) {
				return result;
			}
		}

		for (int i = 0; i < inputEntries.length; i++) {
			// Directory entry?
			if (isDirectory(inputEntries[i])) {
				String subDirName = inputEntries[i].filename;
				if (dirName.length() > 0) {
					subDirName = dirName + '/' + subDirName;
				}
				int result = deltaSynchronize(subDirName);
				if (result != 0) {
					return result;
				}
			}
		}

		return 0;
	}

	private String getFileName(String dirName, SceIoDirent entry) {
		if (dirName.length() == 0) {
			return entry.filename;
		}

		return String.format("%s/%s", dirName, entry.filename);
	}

	private int updateEntry(String dirName, SceIoDirent entry) {
		String fileName = getFileName(dirName, entry);

		if (log.isDebugEnabled()) {
			log.debug(String.format("updateEntry %s", fileName));
		}

		IVirtualFile inputFile = input.ioOpen(fileName, IoFileMgrForUser.PSP_O_RDONLY, 0);
		if (inputFile == null) {
			log.error(String.format("updateEntry cannot read file '%s'", fileName));
			return IO_ERROR;
		}

		IVirtualFile outputFile = output.ioOpen(fileName, IoFileMgrForUser.PSP_O_WRONLY, 0);
		if (outputFile == null) {
			inputFile.ioClose();
			log.error(String.format("updateEntry cannot write file '%s'", fileName));
			return IO_ERROR;
		}

		byte buffer[] = new byte[32 * 1024];
		long inputLength = inputFile.length();
		while (inputLength > 0L) {
			int length = Math.min(buffer.length, (int) inputLength);
			int readLength = inputFile.ioRead(buffer, 0, length);
			if (readLength != length) {
				log.error(String.format("updateEntry error reading file '%s': 0x%X", fileName, readLength));
				return IO_ERROR;
			}

			int writeLength = outputFile.ioWrite(buffer, 0, readLength);
			if (writeLength != readLength) {
				log.error(String.format("updateEntry error writing file '%s': 0x%X", fileName, writeLength));
				return IO_ERROR;
			}

			inputLength -= readLength;
		}

		inputFile.ioClose();
		outputFile.ioClose();

		return 0;
	}

	private int createEntry(String dirName, SceIoDirent entry) {
		int result;

		// Directory entry?
		if (isDirectory(entry)) {
			String fileName = getFileName(dirName, entry);
			if (log.isDebugEnabled()) {
				log.debug(String.format("createEntry %s", fileName));
			}

			result = output.ioMkdir(fileName, 0777);

			if (result != 0) {
				log.error(String.format("createEntry could not create directory '%s'", fileName));
			}
		} else {
			// Creating a file is the same as updating it
			result = updateEntry(dirName, entry);
		}

		return result;
	}

	private int deleteEntry(String dirName, SceIoDirent entry) {
		int result;
		String fileName = getFileName(dirName, entry);

		if (log.isDebugEnabled()) {
			log.debug(String.format("deleteEntry %s", fileName));
		}

		// Directory entry?
		if (isDirectory(entry)) {
			result = deleteDirectoryEntryRecursive(fileName);
		} else {
			result = output.ioRemove(fileName);

			if (result != 0) {
				log.error(String.format("deleteEntry could not delete '%s'", fileName));
			}
		}

		return result;
	}

	private int deleteDirectoryEntryRecursive(String dirName) {
		SceIoDirent [] entries = getDirectoryEntries(output, dirName);
		if (entries == null) {
			log.error(String.format("deleteDirectoryEntryRecursive could not delete '%s'", dirName));
			return IO_ERROR;
		}

		int result = 0;
		for (SceIoDirent entry : entries) {
			String fileName = getFileName(dirName, entry);
			if (isDirectory(entry)) {
				result = deleteDirectoryEntryRecursive(fileName);
			} else {
				if (log.isDebugEnabled()) {
					log.debug(String.format("deleteDirectoryEntryRecursive: delete file %s", fileName));
				}
				result = output.ioRemove(fileName);

				if (result != 0) {
					log.error(String.format("deleteDirectoryEntryRecursive could not delete '%s'", dirName));
				}
			}

			if (result != 0) {
				return result;
			}
		}

		if (log.isDebugEnabled()) {
			log.debug(String.format("deleteDirectoryEntryRecursive: delete directory %s", dirName));
		}
		result = output.ioRmdir(dirName);

		if (result != 0) {
			log.error(String.format("deleteDirectoryEntryRecursive could not delete '%s'", dirName));
		}

		return result;
	}
}
