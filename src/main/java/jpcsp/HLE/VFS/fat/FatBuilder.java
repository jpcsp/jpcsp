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

import static jpcsp.HLE.VFS.fat.FatUtils.storeSectorInt16;
import static jpcsp.HLE.VFS.fat.FatUtils.storeSectorInt32;
import static jpcsp.HLE.VFS.fat.FatUtils.storeSectorInt8;
import static jpcsp.HLE.VFS.fat.FatUtils.storeSectorString;
import static jpcsp.HLE.kernel.types.pspAbstractMemoryMappedStructure.charset16;
import static jpcsp.util.Utilities.hasFlag;
import static jpcsp.util.Utilities.notHasFlag;

import java.util.List;

import org.apache.log4j.Logger;

import jpcsp.HLE.VFS.IVirtualFileSystem;
import jpcsp.HLE.kernel.types.SceIoDirent;
import jpcsp.HLE.kernel.types.SceIoStat;
import jpcsp.HLE.kernel.types.ScePspDateTime;
import jpcsp.util.Utilities;

public class FatBuilder {
	private static Logger log = FatVirtualFile.log;
    public final static int bootSectorNumber = 0;
    public final static int numberOfFats = 2;
    public final static int reservedSectors = 32;
    public final static int directoryTableEntrySize = 32;
	private final FatVirtualFile vFile;
	private final IVirtualFileSystem vfs;
	private final int maxNumberClusters;
    private int firstFreeCluster;

	public FatBuilder(FatVirtualFile vFile, IVirtualFileSystem vfs, int maxNumberClusters) {
		this.vFile = vFile;
		this.vfs = vfs;
		this.maxNumberClusters = maxNumberClusters;
	}

	public FatFileInfo scan(String deviceName) {
		firstFreeCluster = vFile.getFirstFreeCluster();

		FatFileInfo rootDirectory = new FatFileInfo(vFile.getDeviceName(), null, null, true, false, null, 0);
		rootDirectory.setParentDirectory(rootDirectory);

		scan(null, rootDirectory);

		vFile.setRootDirectory(rootDirectory);

		if (log.isDebugEnabled()) {
			log.debug(String.format("%s: Using 0x%X clusters out of 0x%X", deviceName, firstFreeCluster, maxNumberClusters));
			debugScan(rootDirectory);
		}

		if (firstFreeCluster > maxNumberClusters) {
			log.error(String.format("Too many files in the Fat partition '%s': required clusters=0x%X, max clusters=0x%X", deviceName, firstFreeCluster, maxNumberClusters));
		}

		return rootDirectory;
	}

	private void debugScan(FatFileInfo fileInfo) {
		log.debug(String.format("scan %s", fileInfo));
		List<FatFileInfo> children = fileInfo.getChildren();
		if (children != null) {
			for (FatFileInfo child: children) {
				debugScan(child);
			}
		}
	}

	public void setClusters(FatFileInfo fileInfo, int[] clusters) {
		fileInfo.setClusters(clusters);
		vFile.setFatFileInfoMap(fileInfo);
	}

	private int allocateCluster() {
		return firstFreeCluster++;
	}

	private int[] allocateClusters(long size) {
		int clusterSize = vFile.getClusterSize();
		int numberClusters = (int) ((size + clusterSize - 1) / clusterSize);
		if (numberClusters <= 0) {
			return null;
		}

		int[] clusters = new int[numberClusters];
		for (int i = 0; i < numberClusters; i++) {
			clusters[i] = allocateCluster();
		}

		// Fill the cluster chain in the cluster map
		for (int i = 0; i < numberClusters - 1; i++) {
			// Pointing to the next cluster
			vFile.setFatClusterMap(clusters[i], clusters[i + 1]);
		}
		if (numberClusters > 0) {
			// Last cluster in file (EOC)
			vFile.setFatClusterMap(clusters[numberClusters - 1], vFile.getFatEOC());
		}

		return clusters;
	}

	private void allocateClusters(FatFileInfo fileInfo) {
		long dataSize = fileInfo.getFileSize();
		if (fileInfo.isDirectory()) {
			// Two child entries for "." and ".."
			int directoryTableEntries = 2;

			List<FatFileInfo> children = fileInfo.getChildren();
			if (children != null) {
				// TODO: take fake entries into account to support "long filename"
				directoryTableEntries += children.size();
			} 

			dataSize = directoryTableEntrySize * directoryTableEntries;
		}

		int[] clusters = allocateClusters(dataSize);
		setClusters(fileInfo, clusters);
	}

	private void scan(String dirName, FatFileInfo parent) {
		String[] names = vfs.ioDopen(dirName);
		if (names == null || names.length == 0) {
			return;
		}

		SceIoStat stat = new SceIoStat();
		SceIoDirent dir = new SceIoDirent(stat, null);
		for (int i = 0; i < names.length; i++) {
			if (!".".equals(names[i]) && !"..".equals(names[i])) {
				dir.filename = names[i];
				if (vfs.ioDread(dirName, dir) >= 0) {
					boolean directory = hasFlag(dir.stat.attr, 0x10);
					boolean readOnly = notHasFlag(dir.stat.mode, 0x2);
					FatFileInfo fileInfo = new FatFileInfo(vFile.getDeviceName(), dirName, dir.filename, directory, readOnly, dir.stat.mtime, dir.stat.size);

					parent.addChild(fileInfo);

					if (directory) {
						if (dirName == null) {
							scan(dir.filename, fileInfo);
						} else {
							scan(dirName + "/" + dir.filename, fileInfo);
						}
					}

					// Allocate the clusters after having scanned the children
					allocateClusters(fileInfo);
				}
			}
		}

		List<FatFileInfo> children = parent.getChildren();
		if (children != null) {
			for (FatFileInfo child: children) {
				computeFileName83(child, children);
			}
		}
	}

	private void computeFileName83(FatFileInfo fileInfo, List<FatFileInfo> siblings) {
		int collisionIndex = 0;
		String fileName = fileInfo.getFileName();
		String fileName83 = convertFileNameTo83(fileName, collisionIndex);

		// Check if the 8.3 file name is not colliding
		// with other 8.3 file names in the same directory.
		boolean hasCollision;
		do {
			hasCollision = false;
			for (FatFileInfo sibling: siblings) {
				String siblingFileName83 = sibling.getFileName83();
				if (siblingFileName83 != null) {
					if (fileName83.equals(siblingFileName83)) {
						// 8.3 file name collision
						collisionIndex++;
						hasCollision = true;
						fileName83 = convertFileNameTo83(fileName, collisionIndex);
						break;
					}
				}
			}
		} while (hasCollision);

		fileInfo.setFileName83(fileName83);
	}

	public static String convertFileName8_3To83(String fileName8_3) {
		String name = fileName8_3;
		String extension = "";
		int dotIndex = name.indexOf('.');
		if (dotIndex >= 0) {
			extension = name.substring(dotIndex + 1);
			name = name.substring(0, dotIndex);
		}

		name = (name + "        ").substring(0, 8);
		extension = (extension + "   ").substring(0, 3);

		return name + extension;
	}

	// Convert a "long" file name into a 8.3 file name.
	private static String convertFileNameTo8_3(String fileName, int collisionIndex) {
		if (fileName == null) {
			return null;
		}

		// Special character '+' is turned into '_'
		fileName = fileName.replace("+", "_");
		// File name is upper-cased
		fileName = fileName.toUpperCase();

		// Split into the name and extension parts
		int lastDot = fileName.lastIndexOf(".");
		String name;
		String ext;
		if (lastDot < 0) {
			name = fileName;
			ext = "";
		} else {
			name = fileName.substring(0, lastDot);
			ext = fileName.substring(lastDot + 1);
		}

		// All dots in name part are dropped
		name = name.replace(".", "");

		// The file extension is truncated to 3 characters
		if (ext.length() > 3) {
			ext = ext.substring(0, 3);
		}

		if (collisionIndex >= 1) {
			if (collisionIndex <= 4) {
				// The name is truncated to 6 characters, followed by "~N"
				if (name.length() > 6) {
					name = name.substring(0, 6);
				}
				name += "~" + collisionIndex;
			} else {
				// The name is truncated to 2 characters,
				// followed by 4 hexadecimal digits derived
				// from an undocumented hash of the filename,
				// followed by a tilde, followed by a single digit.
				if (name.length() > 2) {
					name = name.substring(0, 2);
				}
				name += String.format("%04X", collisionIndex) + "~1";
			}
		} else if (name.length() > 8) {
			// The name is truncated to 6 characters (if longer than 8 characters)
			// followed by "~1"
			name = name.substring(0, 6) + "~1";
		}

		if (ext.length() == 0) {
			return name;
		}
		return name + "." + ext;
	}

	private static String convertFileNameTo83(String fileName, int collisionIndex) {
		String fileName8_3 = convertFileNameTo8_3(fileName, collisionIndex);
		return convertFileName8_3To83(fileName8_3);
	}

	private static String convertFileName83To8_3(String fileName83) {
		int endName = 8;
		for (; endName > 0; endName--) {
			if (fileName83.charAt(endName - 1) != ' ') {
				break;
			}
		}
		String name = fileName83.substring(0, endName);

		int endExt = 8 + 3;
		for (; endExt > 8; endExt--) {
			if (fileName83.charAt(endExt - 1) != ' ') {
				break;
			}
		}
		String ext = fileName83.substring(8, endExt);

		if (ext.length() == 0) {
			return name;
		}
		return name + "." + ext;
	}

	private byte[] addLongFileNameDirectoryEntries(byte[] directoryData, String fileName, int fileNameChecksum) {
		byte[] fileNameBytes = fileName.getBytes(charset16);
		int numberEntries = Math.max((fileNameBytes.length + 25) / 26, 1);

		byte[] extend = new byte[numberEntries * 26 - fileNameBytes.length];
		if (extend.length >= 2) {
			extend[0] = (byte) 0;
			extend[1] = (byte) 0;
			for (int i = 2; i < extend.length; i++) {
				extend[i] = (byte) 0xFF;
			}
			fileNameBytes = Utilities.extendArray(fileNameBytes, extend);
		}

		int offset = directoryData.length;
		directoryData = Utilities.extendArray(directoryData, directoryTableEntrySize * numberEntries);

		for (int i = numberEntries; i > 0; i--) {
			int sequenceNumber = i;
			if (i == numberEntries) {
				sequenceNumber |= 0x40; // Last LFN entry
			}
			storeSectorInt8(directoryData, offset + 0, sequenceNumber);

			// Name characters (five UCS-2 characters)
			int fileNameBytesOffset = (i - 1) * 26;
			System.arraycopy(fileNameBytes, fileNameBytesOffset, directoryData, offset + 1, 10);
			fileNameBytesOffset += 10;

			// Attributes (always 0x0F)
			storeSectorInt8(directoryData, offset + 11, 0x0F);

			// Type (always 0x00 for VFAT LFN)
			storeSectorInt8(directoryData, offset + 12, 0x00);

			// Checksum of DOS file name
			storeSectorInt8(directoryData, offset + 13, fileNameChecksum);

			// Name characters (six UCS-2 characters)
			System.arraycopy(fileNameBytes, fileNameBytesOffset, directoryData, offset + 14, 12);
			fileNameBytesOffset += 12;

			// First cluster (always 0)
			storeSectorInt16(directoryData, offset + 26, 0);

			// Name characters (two UCS-2 characters)
			System.arraycopy(fileNameBytes, fileNameBytesOffset, directoryData, offset + 28, 4);

			offset += directoryTableEntrySize;
		}

		return directoryData;
	}

	private int getFileNameChecksum(String fileName) {
		int checksum = 0;

		for (int i = 0; i < fileName.length(); i++) {
			int c = fileName.charAt(i) & 0xFF;
			checksum = (((checksum & 1) << 7) + (checksum >> 1) + c) & 0xFF;
		}

		return checksum;
	}

	private boolean isLongFileName(String fileName8_3, String fileName) {
		return !fileName8_3.equalsIgnoreCase(fileName);
	}

	private byte[] addDirectoryEntry(byte[] directoryData, FatFileInfo fileInfo) {
		String fileName = fileInfo.getFileName();
		String fileName83 = fileInfo.getFileName83();
		String fileName8_3 = convertFileName83To8_3(fileName83);
		if (isLongFileName(fileName8_3, fileName)) {
			int checksum = getFileNameChecksum(fileName83);
			directoryData = addLongFileNameDirectoryEntries(directoryData, fileName, checksum);
		}

		int offset = directoryData.length;
		directoryData = Utilities.extendArray(directoryData, directoryTableEntrySize);

		storeSectorString(directoryData, offset + 0, fileName83, 8 + 3);

		int fileAttributes = 0x20; // Archive attribute
		if (fileInfo.isReadOnly()) {
			fileAttributes |= 0x01; // Read Only attribute
		}
		if (fileInfo.isDirectory()) {
			fileAttributes |= 0x10; // Sub-directory attribute
		}
		storeSectorInt8(directoryData, offset + 11, fileAttributes);

		// Has extended attributes?
		storeSectorInt8(directoryData, offset + 12, 0);

		ScePspDateTime lastModified = fileInfo.getLastModified();
		storeSectorInt8(directoryData, offset + 13, 0); // Milliseconds always set to 0 by the PSP

		int createTime = lastModified.hour << 11;
		createTime |= lastModified.minute << 5;
		createTime |= lastModified.second >> 1;
		storeSectorInt16(directoryData, offset + 14, createTime);

		int createDate = (lastModified.year - 1980) << 9;
		createDate |= lastModified.month << 5;
		createDate |= lastModified.day;
		storeSectorInt16(directoryData, offset + 16, createDate);

		storeSectorInt16(directoryData, offset + 18, createDate);

		int[] clusters = fileInfo.getClusters();
		int entryClusterNumber = 0; // Empty file
		if (clusters != null && clusters.length > 0) {
			entryClusterNumber = clusters[0];
		}

		storeSectorInt16(directoryData, offset + 20, entryClusterNumber >>> 16);

		storeSectorInt16(directoryData, offset + 22, createTime);
		storeSectorInt16(directoryData, offset + 24, createDate);

		storeSectorInt16(directoryData, offset + 26, entryClusterNumber & 0xFFFF);

		int fileSize = (int) fileInfo.getFileSize();
		if (fileInfo.isDirectory()) {
			fileSize = 0;
		}
		storeSectorInt32(directoryData, offset + 28, fileSize);

		return directoryData;
	}

	private void buildDotDirectoryEntry(byte[] directoryData, int offset, FatFileInfo fileInfo, String dotName, ScePspDateTime alternateLastModified) {
		storeSectorString(directoryData, offset + 0, dotName, 8 + 3);

		// File attributes: directory
		storeSectorInt8(directoryData, offset + 11, 0x10);

		// Has extended attributes?
		storeSectorInt8(directoryData, offset + 12, 0);

		ScePspDateTime lastModified = fileInfo.getLastModified();
		if (lastModified == null) {
			// The root directory has no lastModified date/time,
			// rather use the date/time of the sub-directory.
			lastModified = alternateLastModified;
		}
		storeSectorInt8(directoryData, offset + 13, 0); // Milliseconds, always set to 0 by the PSP

		int createTime = lastModified.hour << 11;
		createTime |= lastModified.minute << 5;
		createTime |= lastModified.second >> 1;
		storeSectorInt16(directoryData, offset + 14, createTime);

		int createDate = (lastModified.year - 1980) << 9;
		createDate |= lastModified.month << 5;
		createDate |= lastModified.day;
		storeSectorInt16(directoryData, offset + 16, createDate);

		storeSectorInt16(directoryData, offset + 18, createDate);

		int[] clusters = fileInfo.getClusters();
		int entryClusterNumber = 0; // Empty file
		if (clusters != null && clusters.length > 0) {
			entryClusterNumber = clusters[0];
		}

		storeSectorInt16(directoryData, offset + 20, entryClusterNumber >>> 16);

		storeSectorInt16(directoryData, offset + 22, createTime);
		storeSectorInt16(directoryData, offset + 24, createDate);

		storeSectorInt16(directoryData, offset + 26, entryClusterNumber & 0xFFFF);

		// File size
		storeSectorInt32(directoryData, offset + 28, 0);
	}

	public byte[] buildDirectoryData(FatFileInfo fileInfo) {
		byte[] directoryData;

		// Is this the root directory?
		if (fileInfo.isRootDirectory()) {
			// The root directory has no "." nor ".." directory entries
			directoryData = new byte[0];
		} else {
			// Non-root directories have "." and ".." directory entries
			directoryData = new byte[directoryTableEntrySize * 2];

			buildDotDirectoryEntry(directoryData, 0, fileInfo, ".", fileInfo.getLastModified());
			buildDotDirectoryEntry(directoryData, directoryTableEntrySize, fileInfo.getParentDirectory(), "..", fileInfo.getLastModified());
		}

		List<FatFileInfo> children = fileInfo.getChildren();
		if (children != null) {
			for (FatFileInfo child : children) {
				directoryData = addDirectoryEntry(directoryData, child);
			}
		}

		return directoryData;
	}
}
