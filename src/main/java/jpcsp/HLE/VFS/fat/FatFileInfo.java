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
package jpcsp.HLE.VFS.fat;

import static jpcsp.HLE.modules.IoFileMgrForUser.PSP_O_RDWR;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import jpcsp.HLE.VFS.IVirtualFile;
import jpcsp.HLE.VFS.IVirtualFileSystem;
import jpcsp.HLE.kernel.types.ScePspDateTime;
import jpcsp.state.StateInputStream;
import jpcsp.state.StateOutputStream;
import jpcsp.util.Utilities;

public class FatFileInfo {
	private static final int STATE_VERSION = 0;
	private String deviceName;
	private String dirName;
	private String fileName;
	private String fileName83;
	private boolean directory;
	private boolean readOnly;
	private ScePspDateTime lastModified;
	private long fileSize;
	private int[] clusters;
	private List<FatFileInfo> children;
	private IVirtualFile vFile;
	private boolean vFileOpen;
	private byte[] fileData;
	private FatFileInfo parentDirectory;

	public FatFileInfo() {
	}

	public FatFileInfo(String deviceName, String dirName, String fileName, boolean directory, boolean readOnly, ScePspDateTime lastModified, long fileSize) {
		this.deviceName = deviceName;
		this.dirName = dirName;
		this.fileName = fileName;
		this.directory = directory;
		this.readOnly = readOnly;
		this.lastModified = lastModified;
		this.fileSize = fileSize;
	}

	public String getDirName() {
		return dirName;
	}

	public void setDirName(String dirName) {
		this.dirName = dirName;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getFullFileName() {
		if (dirName == null) {
			return fileName;
		}
		return dirName + '/' + fileName;
	}

	public boolean isDirectory() {
		return directory;
	}

	public void setDirectory(boolean directory) {
		this.directory = directory;
	}

	public boolean isReadOnly() {
		return readOnly;
	}

	public void setReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
	}

	public ScePspDateTime getLastModified() {
		return lastModified;
	}

	public void setLastModified(ScePspDateTime lastModified) {
		this.lastModified = lastModified;
	}

	public long getFileSize() {
		return fileSize;
	}

	public void setFileSize(long fileSize) {
		this.fileSize = fileSize;
	}

	public int[] getClusters() {
		return clusters;
	}

	public void setClusters(int[] clusters) {
		this.clusters = clusters;
	}

	public void addChild(FatFileInfo fileInfo) {
		if (children == null) {
			children = new LinkedList<FatFileInfo>();
		}

		children.add(fileInfo);
		fileInfo.setParentDirectory(this);
	}

	public List<FatFileInfo> getChildren() {
		return children;
	}

	public IVirtualFile getVirtualFile(IVirtualFileSystem vfs) {
		if (!vFileOpen) {
			vFile = vfs.ioOpen(getFullFileName(), PSP_O_RDWR, 0);
			vFileOpen = true;
		}

		return vFile;
	}

	public void closeVirtualFile() {
		if (vFileOpen) {
			if (vFile != null) {
				vFile.ioClose();
				vFile = null;
			}
			vFileOpen = false;
		}
	}

	public byte[] getFileData() {
		return fileData;
	}

	public void setFileData(byte[] fileData) {
		this.fileData = fileData;
	}

	public FatFileInfo getParentDirectory() {
		return parentDirectory;
	}

	public void setParentDirectory(FatFileInfo parentDirectory) {
		this.parentDirectory = parentDirectory;
	}

	public boolean isRootDirectory() {
		return isDirectory() && dirName == null && fileName == null;
	}

	public String getFileName83() {
		return fileName83;
	}

	public void setFileName83(String fileName83) {
		this.fileName83 = fileName83;
	}

	public FatFileInfo getChildByFileName83(String fileName83) {
		if (children != null) {
			for (FatFileInfo child : children) {
				String childFileName83 = child.getFileName83();
				if (childFileName83 != null && fileName83.equals(childFileName83)) {
					return child;
				}
			}
		}

		return null;
	}

	public boolean hasCluster(int cluster) {
		if (clusters != null) {
			for (int i = 0; i < clusters.length; i++) {
				if (clusters[i] == cluster) {
					return true;
				}
			}
		}

		return false;
	}

	public void addCluster(int cluster) {
		clusters = Utilities.extendArray(clusters, 1);
		clusters[clusters.length - 1] = cluster;
	}

	public int getFirstCluster() {
		if (clusters == null || clusters.length == 0) {
			return 0;
		}

		return clusters[0];
	}

	public void read(StateInputStream stream) throws IOException {
    	stream.readVersion(STATE_VERSION);
    	deviceName = stream.readString();
    	dirName = stream.readString();
    	fileName = stream.readString();
    	fileName83 = stream.readString();
    	directory = stream.readBoolean();
    	readOnly = stream.readBoolean();
    	int time = stream.readInt();
    	if (time == 0) {
    		lastModified = null;
    	} else {
    		lastModified = ScePspDateTime.fromMSDOSTime(time);
    	}
    	fileSize = stream.readLong();
    	clusters = stream.readIntsWithLength();
    	fileData = stream.readBytesWithLength();
    	closeVirtualFile();
    }

    public void read(StateInputStream stream, FatVirtualFile fatVirtualFile) throws IOException {
    	parentDirectory = fatVirtualFile.readFatFileInfo(stream);

    	// Read the children
    	children = null;
    	int countChildren = stream.readInt();
    	for (int i = 0; i < countChildren; i++) {
    		FatFileInfo child = fatVirtualFile.readFatFileInfo(stream);
    		if (child != null) {
    			addChild(child);
    		}
    	}
    }

    public void write(StateOutputStream stream) throws IOException {
    	stream.writeVersion(STATE_VERSION);
    	stream.writeString(deviceName);
    	stream.writeString(dirName);
    	stream.writeString(fileName);
    	stream.writeString(fileName83);
    	stream.writeBoolean(directory);
    	stream.writeBoolean(readOnly);
    	if (lastModified == null) {
    		stream.writeInt(0);
    	} else {
    		stream.writeInt(lastModified.toMSDOSTime());
    	}
    	stream.writeLong(fileSize);
    	stream.writeIntsWithLength(clusters);
    	stream.writeBytesWithLength(fileData);
    }

    public void write(StateOutputStream stream, FatVirtualFile fatVirtualFile) throws IOException {
    	fatVirtualFile.writeFatFileInfo(stream, parentDirectory);

    	// Write the children
    	if (children == null) {
    		stream.writeInt(0);
    	} else {
	    	stream.writeInt(children.size());
	    	for (FatFileInfo child : children) {
	    		fatVirtualFile.writeFatFileInfo(stream, child);
	    	}
    	}
    }

    @Override
	public String toString() {
		StringBuilder s = new StringBuilder();

		if (deviceName != null) {
			s.append(deviceName);
		}

		if (getFullFileName() == null) {
			s.append("[ROOT]");
		} else {
			s.append("/");
			s.append(getFullFileName());
		}

		if (fileName83 != null) {
			s.append(String.format("('%s')", fileName83));
		}

		if (directory) {
			s.append(", directory");
		}

		if (readOnly) {
			s.append(", readOnly");
		}

		s.append(String.format(", size=0x%X", fileSize));

		if (clusters != null) {
			s.append(", clusters=[");
			// Display maximum 20 cluster entries
			int length = Math.min(clusters.length, 20);
			for (int i = 0; i < length; i++) {
				if (i > 0) {
					s.append(", ");
				}
				s.append(String.format("0x%X", clusters[i]));
			}
			if (length < clusters.length) {
				s.append(String.format(", ..., 0x%X", clusters[clusters.length - 1]));
			}
			s.append("]");
		}

		return s.toString();
	}
}
