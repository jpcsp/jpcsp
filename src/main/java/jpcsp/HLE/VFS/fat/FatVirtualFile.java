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

import static jpcsp.HLE.VFS.AbstractVirtualFileSystem.IO_ERROR;
import static jpcsp.HLE.VFS.fat.FatBuilder.bootSectorNumber;
import static jpcsp.HLE.VFS.fat.FatBuilder.directoryTableEntrySize;
import static jpcsp.HLE.VFS.fat.FatBuilder.numberOfFats;
import static jpcsp.HLE.VFS.fat.FatBuilder.reservedSectors;
import static jpcsp.HLE.VFS.fat.FatUtils.getSectorNumber;
import static jpcsp.HLE.VFS.fat.FatUtils.getSectorOffset;
import static jpcsp.HLE.VFS.fat.FatUtils.readSectorString;
import static jpcsp.HLE.VFS.fat.FatUtils.storeSectorInt32;
import static jpcsp.HLE.VFS.fat.FatUtils.storeSectorInt8;
import static jpcsp.HLE.VFS.fat.FatUtils.storeSectorString;
import static jpcsp.HLE.kernel.types.pspAbstractMemoryMappedStructure.charset16;
import static jpcsp.util.Utilities.hasFlag;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import jpcsp.HLE.TPointer;
import jpcsp.HLE.VFS.IVirtualCache;
import jpcsp.HLE.VFS.IVirtualFile;
import jpcsp.HLE.VFS.IVirtualFileSystem;
import jpcsp.HLE.VFS.InvalidVirtualFile;
import jpcsp.HLE.VFS.ReadCacheVirtualFile;
import jpcsp.HLE.kernel.types.SceIoDirent;
import jpcsp.HLE.kernel.types.SceIoStat;
import jpcsp.HLE.modules.IoFileMgrForUser;
import jpcsp.HLE.modules.IoFileMgrForUser.IoOperation;
import jpcsp.HLE.modules.IoFileMgrForUser.IoOperationTiming;
import jpcsp.state.IState;
import jpcsp.state.StateInputStream;
import jpcsp.state.StateOutputStream;
import jpcsp.util.Utilities;

// See format description: https://en.wikipedia.org/wiki/Design_of_the_FAT_file_system
public abstract class FatVirtualFile implements IVirtualFile, IVirtualCache, IState {
	public static Logger log = Logger.getLogger("fat");
	private static final boolean useSynchronizeVFS = true;
	private static final int STATE_VERSION = 0;
    public final static int sectorSize = 512;
	protected final static int firstClusterNumber = 2;
    protected final byte[] currentSector = new byte[sectorSize];
    private static final byte[] emptySector = new byte[sectorSize];
    private String deviceName;
	private IVirtualFileSystem vfs;
	private long position;
	protected int totalSectors;
    protected int fatSectors;
    protected int[] fatClusterMap;
    private FatFileInfo[] fatFileInfoMap;
    private FatBuilder builder;
    private int fatSectorNumber = bootSectorNumber + reservedSectors;
    private int fsInfoSectorNumber = bootSectorNumber + 1;
    protected FatFileInfo rootDirectory;
    protected int rootDirectoryStartSectorNumber = -1;
    protected int rootDirectoryEndSectorNumber = -1;
    private final byte[] secondFat;
    private IVirtualFile baseVirtualFile;

	protected FatVirtualFile(String deviceName, IVirtualFileSystem vfs, int totalSectors) {
		this.deviceName = deviceName;
		this.vfs = vfs;
		baseVirtualFile = this;

		this.totalSectors = totalSectors;
		fatSectors = getFatSectors(totalSectors, getSectorsPerCluster());

		if (log.isDebugEnabled()) {
			log.debug(String.format("totalSectors=0x%X, fatSectors=0x%X", totalSectors, fatSectors));
		}

		secondFat = new byte[fatSectors * sectorSize];

		reset();
	}

	protected abstract int getClusterMask();
	protected abstract int getSectorsPerCluster();
	protected abstract int getFatEOC();
	protected abstract int getFatSectors(int totalSectors, int sectorsPerCluster);
	protected abstract void readBIOSParameterBlock();
	protected abstract int getFirstDataClusterOffset();
	protected abstract void setRootDirectory(FatFileInfo rootDirectory);

	public void setBaseVirtualFile(IVirtualFile baseVirtualFile) {
		this.baseVirtualFile = baseVirtualFile;
	}

	protected int getFirstFreeCluster() {
		return firstClusterNumber + getFirstDataClusterOffset() / getSectorsPerCluster();
	}

	protected int getClusterSize() {
		return sectorSize * getSectorsPerCluster();
	}

	public int getFsInfoSectorNumber() {
		return fsInfoSectorNumber;
	}

	public void setFsInfoSectorNumber(int fsInfoSectorNumber) {
		this.fsInfoSectorNumber = fsInfoSectorNumber;
	}

	public int getFatSectorNumber() {
		return fatSectorNumber;
	}

	public void setFatSectorNumber(int fatSectorNumber) {
		this.fatSectorNumber = fatSectorNumber;
	}

	private void reset() {
		int usedSectors = reservedSectors + fatSectors * numberOfFats;
		usedSectors += getFirstDataClusterOffset();
		int maxNumberClusters = (totalSectors - usedSectors) / getSectorsPerCluster();

		// Allocate the FAT cluster map
		fatClusterMap = new int[maxNumberClusters];
		// First 2 special entries in the cluster map
		fatClusterMap[0] = 0xFFFFFFF8 & getClusterMask(); // 0xF8 is matching the boot sector Media type field
		fatClusterMap[1] = 0xFFFFFFFF & getClusterMask();

		// Allocate the FAT file info map
		fatFileInfoMap = new FatFileInfo[maxNumberClusters];

		// Reset the second FAT
		Arrays.fill(secondFat, (byte) 0);

		builder = new FatBuilder(this, vfs, maxNumberClusters);
	}

	public void scan() {
		builder.scan(deviceName);
	}

	private void extendClusterMap(int clusterNumber) {
		int extend = clusterNumber + 1 - fatClusterMap.length;
		if (extend > 0) {
			if (log.isDebugEnabled()) {
				log.debug(String.format("extendClusterMap clusterNumber=0x%X, extend=0x%X", clusterNumber, extend));
			}
			fatClusterMap = Utilities.extendArray(fatClusterMap, extend);
			fatFileInfoMap = FatUtils.extendArray(fatFileInfoMap, extend);
		}
	}

	public void setFatFileInfoMap(FatFileInfo fileInfo) {
		if (log.isTraceEnabled()) {
			log.trace(String.format("setFatFileInfoMap %s", fileInfo));
		}

		int[] clusters = fileInfo.getClusters();
		if (clusters != null) {
			for (int i = 0; i < clusters.length; i++) {
				setFatFileInfoMap(clusters[i], fileInfo);
			}
		}
	}

	public void setFatFileInfoMap(int clusterNumber, FatFileInfo fileInfo) {
		if (clusterNumber >= fatFileInfoMap.length) {
			extendClusterMap(clusterNumber);
		}
		fatFileInfoMap[clusterNumber] = fileInfo;
	}

	public void setFatClusterMap(int clusterNumber, int value) {
		if (clusterNumber >= fatClusterMap.length) {
			extendClusterMap(clusterNumber);
		}
		fatClusterMap[clusterNumber] = value;
	}

	private int getClusterNumber(int sectorNumber) {
		sectorNumber -= fatSectorNumber;
		sectorNumber -= numberOfFats * fatSectors;
		sectorNumber -= getFirstDataClusterOffset();
		return firstClusterNumber + (sectorNumber / getSectorsPerCluster());
	}

	private int getSectorOffsetInCluster(int sectorNumber) {
		sectorNumber -= fatSectorNumber;
		sectorNumber -= numberOfFats * fatSectors;
		sectorNumber -= getFirstDataClusterOffset();
		return sectorNumber % getSectorsPerCluster();
	}

	protected String getOEMName() {
		return "";
	}

	private void readBootSector() {
		readEmptySector();

		// Jump Code
    	storeSectorInt8(currentSector, 0, 0xEB);
    	storeSectorInt8(currentSector, 1, 0x58);
    	storeSectorInt8(currentSector, 2, 0x90);

    	// OEM Name
    	storeSectorString(currentSector, 3, getOEMName(), 8);

    	// The format of BIOS Parameter Block is depending on the
    	// fat format (FAT12, FAT16 or FAT32)
    	readBIOSParameterBlock();

    	// Signature
    	storeSectorInt8(currentSector, 510, 0x55);
    	storeSectorInt8(currentSector, 511, 0xAA);
	}

	private void readFsInfoSector() {
		readEmptySector();

    	// FS Information sector signature
		storeSectorInt8(currentSector, 0, 0x52);
		storeSectorInt8(currentSector, 1, 0x52);
		storeSectorInt8(currentSector, 2, 0x61);
		storeSectorInt8(currentSector, 3, 0x41);

    	// FS Information sector signature
		storeSectorInt8(currentSector, 484, 0x72);
		storeSectorInt8(currentSector, 485, 0x72);
		storeSectorInt8(currentSector, 486, 0x41);
		storeSectorInt8(currentSector, 487, 0x61);

    	// Last known number of free data clusters, or 0xFFFFFFFF if unknown
    	storeSectorInt32(currentSector, 488, 0xFFFFFFFF);

    	// Number of the most recently known to be allocated data cluster.
    	// Should be set to 0xFFFFFFFF during format.
    	storeSectorInt32(currentSector, 492, 0xFFFFFFFF);

    	// FS Information sector signature
    	storeSectorInt8(currentSector, 510, 0x55);
    	storeSectorInt8(currentSector, 511, 0xAA);
	}

	protected abstract void readFatSector(int fatIndex);

	private void readDataSector(int sectorNumber, int clusterNumber, int sectorOffsetInCluster, FatFileInfo fileInfo) {
		readEmptySector();

		if (fileInfo == null) {
			if (log.isDebugEnabled()) {
				log.debug(String.format("readDataSector unknown sectorNumber=0x%X, clusterNumber=0x%X", sectorNumber, clusterNumber));
			}
			return;
		}
		if (log.isDebugEnabled()) {
			log.debug(String.format("readDataSector clusterNumber=0x%X(sector=0x%X), fileInfo=%s", clusterNumber, sectorOffsetInCluster, fileInfo));
		}

		if (fileInfo.isDirectory()) {
			byte[] directoryData = fileInfo.getFileData();
			if (directoryData == null) {
				directoryData = builder.buildDirectoryData(fileInfo);
				fileInfo.setFileData(directoryData);
			}

			int byteOffset = sectorOffsetInCluster * sectorSize;
			if (byteOffset < directoryData.length) {
				int length = Math.min(directoryData.length - byteOffset, sectorSize);
				System.arraycopy(directoryData, byteOffset, currentSector, 0, length);
			}
		} else {
			IVirtualFile vFile = fileInfo.getVirtualFile(vfs);
			if (vFile == null) {
				log.error(String.format("readDataSector cannot read file %s", fileInfo));
				return;
			}

			long byteOffset = sectorOffsetInCluster * (long) sectorSize;
			int[] clusters = fileInfo.getClusters();
			if (clusters != null) {
				for (int i = 0; i < clusters.length; i++) {
					if (clusters[i] == clusterNumber) {
						break;
					}
					byteOffset += getSectorsPerCluster() * sectorSize;
				}
			}

			if (byteOffset < fileInfo.getFileSize()) {
				int length = (int) Math.min(fileInfo.getFileSize() - byteOffset, (long) sectorSize);
				int readLength;
				// synchronize the vFile to make sure that the ioLseek/ioRead combination is atomic
				synchronized (vFile) {
					if (vFile.ioLseek(byteOffset) != byteOffset) {
						log.error(String.format("readDataSector cannot seek file '%s' to 0x%X", fileInfo, byteOffset));
						return;
					}

					readLength = vFile.ioRead(currentSector, 0, length);
				}
				if (readLength != length) {
					log.error(String.format("readDataSector cannot read sector readLength=0x%X(max length=0x%X, byteOffset=0x%X, fileSize=0x%X)", readLength, length, byteOffset, fileInfo.getFileSize()));
				}
				if (log.isDebugEnabled()) {
					log.debug(String.format("readDataSector readLength=0x%X(max length=0x%X, byteOffset=0x%X, fileSize=0x%X)", readLength, length, byteOffset, fileInfo.getFileSize()));
				}
			} else {
				if (log.isDebugEnabled()) {
					log.debug(String.format("readDataSector trying to read at offset 0x%X past end of file", byteOffset, fileInfo));
				}
			}
		}
	}

	private void readRootDirectory(int sectorNumber) {
		readDataSector(rootDirectoryStartSectorNumber, 0, sectorNumber, rootDirectory);
	}

	private void readDataSector(int sectorNumber) {
		readEmptySector();

		int clusterNumber = getClusterNumber(sectorNumber);
		if (clusterNumber >= fatFileInfoMap.length) {
			// Reading out of the allocated fat files
			return;
		}
		FatFileInfo fileInfo = fatFileInfoMap[clusterNumber];
		int sectorOffsetInCluster = getSectorOffsetInCluster(sectorNumber);

		readDataSector(sectorNumber, clusterNumber, sectorOffsetInCluster, fileInfo);
	}

	private void readSecondFatSector(int fatIndex) {
		System.arraycopy(secondFat, fatIndex * sectorSize, currentSector, 0, sectorSize);
	}

	protected void readEmptySector() {
		System.arraycopy(emptySector, 0, currentSector, 0, sectorSize);
	}

	private void readSector(int sectorNumber) {
		if (sectorNumber == bootSectorNumber) {
			readBootSector();
		} else if (sectorNumber == fsInfoSectorNumber) {
			readFsInfoSector();
		} else if (sectorNumber < fatSectorNumber) {
			readEmptySector();
		} else if (sectorNumber >= fatSectorNumber && sectorNumber < fatSectorNumber + fatSectors) {
			readFatSector(sectorNumber - fatSectorNumber);
		} else if (sectorNumber >= fatSectorNumber + fatSectors && sectorNumber < fatSectorNumber + numberOfFats * fatSectors) {
			// Reading from the second FAT table
			readSecondFatSector(sectorNumber - fatSectorNumber - fatSectors);
		} else if (sectorNumber >= rootDirectoryStartSectorNumber && sectorNumber <= rootDirectoryEndSectorNumber) {
			readRootDirectory(sectorNumber - rootDirectoryStartSectorNumber);
		} else {
			readDataSector(sectorNumber);
		}
	}

	private void writeBootSector() {
		log.warn(String.format("Writing to the MemoryStick boot sector!"));
		writeEmptySector();
	}

	private void writeFsInfoSector() {
		log.warn(String.format("Writing to the MemoryStick FsInfo sector!"));
		writeEmptySector();
	}

	protected void writeFatSectorEntry(int clusterNumber, int value) {
		// One entry of the FAT has been updated
		if (log.isDebugEnabled()) {
			log.debug(String.format("writeFatSectorEntry[0x%X]=0x%08X", clusterNumber, value));
		}

		fatClusterMap[clusterNumber] = value;
	}

	protected abstract void writeFatSector(int fatIndex);

	private static String getFileNameLFN(byte[] lfn) {
		boolean last = false;
		byte[] fileNameBytes = null;
		for (int sequenceNumber = 1; !last; sequenceNumber++) {
			for (int i = 0; i < lfn.length; i += directoryTableEntrySize) {
				if ((lfn[i + 0] & 0x1F) == sequenceNumber) {
					if ((lfn[i + 0] & 0x40) != 0) {
						last = true;
					}
					fileNameBytes = Utilities.extendArray(fileNameBytes, lfn, i + 1, 10);
					fileNameBytes = Utilities.extendArray(fileNameBytes, lfn, i + 14, 12);
					fileNameBytes = Utilities.extendArray(fileNameBytes, lfn, i + 28, 4);
					break;
				}
			}
		}

		if (fileNameBytes == null) {
			return "";
		}

		for (int i = 0; i < fileNameBytes.length; i += 2) {
			if (fileNameBytes[i] == ((byte) 0) && fileNameBytes[i + 1] == (byte) 0) {
				return new String(fileNameBytes, 0, i, charset16);
			}
		}

		return new String(fileNameBytes, charset16);
	}

	public static String getFileName(byte[] sector, int offset, byte[] lfn) {
		String fileName;

		if (lfn != null) {
			fileName = getFileNameLFN(lfn);
		} else {
			String name = readSectorString(sector, offset + 0, 8);
			String ext = readSectorString(sector, offset + 8, 3);
			if (ext.length() == 0) {
				fileName = name;
			} else {
				fileName = name + '.' + ext;
			}
		}

		return fileName;
	}

	public static boolean isLongFileNameDirectoryEntry(byte[] directoryData, int offset) {
		// Attributes (always 0x0F for LFN)
		return directoryData[offset + 11] == 0x0F;
	}

	private long getByteOffset(FatFileInfo fileInfo, int sectorNumber) {
		int clusterNumber = getClusterNumber(sectorNumber);
		int sectorOffsetInCluster = getSectorOffsetInCluster(sectorNumber);

		long byteOffset = sectorOffsetInCluster * (long) sectorSize;
		int[] clusters = fileInfo.getClusters();
		if (clusters != null) {
			for (int i = 0; i < clusters.length; i++) {
				if (clusters[i] == clusterNumber) {
					break;
				}
				byteOffset += getClusterSize();
			}
		}

		return byteOffset;
	}

	private static boolean isEmpty(byte[] sector, int offset) {
		for (int i = offset; i < sectorSize; i++) {
			if (sector[i] != (byte) 0) {
				return false;
			}
		}

		return true;
	}

	private boolean writeFileSector(FatFileInfo fileInfo, long byteOffset, byte[] sector) {
		IVirtualFile vFile = fileInfo.getVirtualFile(vfs);
		if (vFile == null) {
			log.warn(String.format("writeFileSector cannot write file '%s'", fileInfo));
			return false;
		}

		if (log.isDebugEnabled()) {
			log.debug(String.format("writeFileSector byteOffset=0x%X for %s", byteOffset, fileInfo));
		}

		int length = (int) Math.min(fileInfo.getFileSize() - byteOffset, (long) sectorSize);
		if (length < 0 || !isEmpty(sector, length)) {
			log.warn(String.format("writeFileSector writing past end of file '%s'", fileInfo));
			return false;
		}

		// synchronize the vFile to make sure that the ioLseek/ioWrite combination is atomic
		synchronized (vFile) {
			if (vFile.ioLseek(byteOffset) != byteOffset) {
				log.warn(String.format("writeFileSector cannot seek file '%s' to 0x%X", fileInfo, byteOffset));
				return false;
			}

			int writeLength = vFile.ioWrite(sector, 0, length);
			if (writeLength != length) {
				log.warn(String.format("writeFileSector cannot write file 0x%X bytes to file '%s': result 0x%X", length, fileInfo, writeLength));
				return false;
			}
		}

		return true;
	}

	private void writeDataSector(int sectorNumber, int clusterNumber, int sectorOffsetInCluster, FatFileInfo fileInfo) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("writeDataSector clusterNumber=0x%X(sector=0x%X), fileInfo=%s", clusterNumber, sectorOffsetInCluster, fileInfo));
		}

		if (fileInfo.isDirectory()) {
			byte[] directoryData = fileInfo.getFileData();
			if (directoryData == null) {
				directoryData = builder.buildDirectoryData(fileInfo);
				fileInfo.setFileData(directoryData);
			}

			int byteOffset = sectorOffsetInCluster * sectorSize;
			int sectorLength = sectorSize;
			directoryData = Utilities.copyToArrayAndExtend(directoryData, byteOffset, currentSector, 0, sectorLength);
			fileInfo.setFileData(directoryData);
		} else {
			writeFileSector(fileInfo, getByteOffset(fileInfo, sectorNumber), currentSector);
		}
	}

	private void writeRootDirectory(int sectorNumber) {
		writeDataSector(rootDirectoryStartSectorNumber, 0, sectorNumber, rootDirectory);
	}

	private void writeDataSector(int sectorNumber) {
		int clusterNumber = getClusterNumber(sectorNumber);
		int sectorOffsetInCluster = getSectorOffsetInCluster(sectorNumber);
		FatFileInfo fileInfo = fatFileInfoMap[clusterNumber];

		writeDataSector(sectorNumber, clusterNumber, sectorOffsetInCluster, fileInfo);
	}

	private void writeSecondFatSector(int fatIndex) {
		System.arraycopy(currentSector, 0, secondFat, fatIndex * sectorSize, sectorSize);
	}

	private void writeEmptySector() {
	}

	public String getDeviceName() {
		return deviceName;
	}

	private void writeSector(int sectorNumber) {
		if (log.isDebugEnabled()) {
			log.debug(String.format("writeSector 0x%X", sectorNumber));
		}

		if (sectorNumber == bootSectorNumber) {
			writeBootSector();
		} else if (sectorNumber == fsInfoSectorNumber) {
			writeFsInfoSector();
		} else if (sectorNumber < fatSectorNumber) {
			writeEmptySector();
		} else if (sectorNumber >= fatSectorNumber && sectorNumber < fatSectorNumber + fatSectors) {
			writeFatSector(sectorNumber - fatSectorNumber);
		} else if (sectorNumber >= fatSectorNumber + fatSectors && sectorNumber < fatSectorNumber + numberOfFats * fatSectors) {
			// Writing to the second FAT table
			writeSecondFatSector(sectorNumber - fatSectorNumber - fatSectors);
		} else if (sectorNumber >= rootDirectoryStartSectorNumber && sectorNumber <= rootDirectoryEndSectorNumber) {
			writeRootDirectory(sectorNumber - rootDirectoryStartSectorNumber);
		} else {
			writeDataSector(sectorNumber);
		} 
	}

	@Override
	public synchronized int ioRead(TPointer outputPointer, int outputLength) {
		int readLength = 0;
		int outputOffset = 0;
		while (outputLength > 0) {
			int sectorNumber = getSectorNumber(position);
			readSector(sectorNumber);
			int sectorOffset = getSectorOffset(position);
			int sectorLength = sectorSize - sectorOffset;
			int length = Math.min(sectorLength, outputLength);

			outputPointer.setArray(outputOffset, currentSector, sectorOffset, length);

			outputLength -= length;
			outputOffset += length;
			position += length;
			readLength += length;
		}

		return readLength;
	}

	@Override
	public synchronized int ioRead(byte[] outputBuffer, int outputOffset, int outputLength) {
		int readLength = 0;
		while (outputLength > 0) {
			int sectorNumber = getSectorNumber(position);
			readSector(sectorNumber);
			int sectorOffset = getSectorOffset(position);
			int sectorLength = sectorSize - sectorOffset;
			int length = Math.min(sectorLength, outputLength);

			System.arraycopy(currentSector, sectorOffset, outputBuffer, outputOffset, length);

			outputLength -= length;
			outputOffset += length;
			position += length;
			readLength += length;
		}

		return readLength;
	}

	@Override
	public synchronized int ioWrite(TPointer inputPointer, int inputLength) {
		if (useSynchronizeVFS) {
			return inputLength;
		}

		int writeLength = 0;
		int inputOffset = 0;
		while (inputLength > 0) {
			int sectorOffset = getSectorOffset(position);
			int sectorLength = sectorSize - sectorOffset;
			int length = Math.min(sectorLength, inputLength);

			if (length != sectorSize) {
				// Not writing a complete sector, read the current sector
				int sectorNumber = getSectorNumber(position);
				readSector(sectorNumber);
			}

			System.arraycopy(inputPointer.getArray8(inputOffset, length), 0, currentSector, sectorOffset, length);

			int sectorNumber = getSectorNumber(position);
			writeSector(sectorNumber);

			inputLength -= length;
			inputOffset += length;
			position += length;
			writeLength += length;
		}

		return writeLength;
	}

	@Override
	public synchronized int ioWrite(byte[] inputBuffer, int inputOffset, int inputLength) {
		if (useSynchronizeVFS) {
			return inputLength;
		}

		int writeLength = 0;
		while (inputLength > 0) {
			int sectorOffset = getSectorOffset(position);
			int sectorLength = sectorSize - sectorOffset;
			int length = Math.min(sectorLength, inputLength);

			if (length != sectorSize) {
				// Not writing a complete sector, read the current sector
				int sectorNumber = getSectorNumber(position);
				readSector(sectorNumber);
			}

			System.arraycopy(inputBuffer, inputOffset, currentSector, sectorOffset, length);

			int sectorNumber = getSectorNumber(position);
			writeSector(sectorNumber);

			inputLength -= length;
			inputOffset += length;
			position += length;
			writeLength += length;
		}

		return writeLength;
	}

	@Override
	public synchronized long ioLseek(long offset) {
		position = offset;

		return position;
	}

	@Override
	public int ioIoctl(int command, TPointer inputPointer, int inputLength, TPointer outputPointer, int outputLength) {
		return IO_ERROR;
	}

	@Override
	public long length() {
		return IO_ERROR;
	}

	@Override
	public boolean isSectorBlockMode() {
		return false;
	}

	@Override
	public long getPosition() {
		return position;
	}

	@Override
	public IVirtualFile duplicate() {
		return null;
	}

	@Override
	public Map<IoOperation, IoOperationTiming> getTimings() {
		return IoFileMgrForUser.noDelayTimings;
	}

	@Override
	public synchronized int ioClose() {
		closeCachedFiles();
		vfs.ioExit();
		vfs = null;

		return 0;
	}

	@Override
    public synchronized void read(StateInputStream stream) throws IOException {
    	stream.readVersion(STATE_VERSION);
    	deviceName = stream.readString();
    	position = stream.readLong();
    	totalSectors = stream.readInt();
    	fatSectors = stream.readInt();

    	fatClusterMap = stream.readIntsWithLength();

    	// Read the fatFileInfoMap in the format: index, alreadyReadIndex, [object,] index, alreadyReadIndex, [object]..., -1
    	fatFileInfoMap = new FatFileInfo[fatClusterMap.length];
    	List<FatFileInfo> fatFileInfoList = new LinkedList<FatFileInfo>();
    	while (true) {
    		int i = stream.readInt();
    		if (i < 0) {
    			// End of the fatFileInfoMap
    			break;
    		}

    		int alreadyReadIndex = stream.readInt();
    		if (alreadyReadIndex < 0) {
    			FatFileInfo fatFileInfo = new FatFileInfo();
				fatFileInfoMap[i] = fatFileInfo;
				fatFileInfo.read(stream);
				fatFileInfoList.add(fatFileInfo);
    		} else {
    			fatFileInfoMap[i] = fatFileInfoMap[alreadyReadIndex];
    		}
    	}

    	// Read the parent-children relations
    	for (FatFileInfo fatFileInfo : fatFileInfoList) {
			fatFileInfo.read(stream, this);
    	}
    }

	@Override
    public synchronized void write(StateOutputStream stream) throws IOException {
    	stream.writeVersion(STATE_VERSION);
    	stream.writeString(deviceName);
    	stream.writeLong(position);
    	stream.writeInt(totalSectors);
    	stream.writeInt(fatSectors);

    	stream.writeIntsWithLength(fatClusterMap);

    	// Write the fatFileInfoMap in the format: index, alreadyWrittenIndex, [object,] index, alreadyWrittenIndex, [object]..., -1
    	HashMap<FatFileInfo, Integer> alreadyWritten = new HashMap<FatFileInfo, Integer>();
    	List<FatFileInfo> fatFileInfoList = new LinkedList<FatFileInfo>();
    	for (int i = 0; i < fatFileInfoMap.length; i++) {
    		FatFileInfo fatFileInfo = fatFileInfoMap[i];
    		if (fatFileInfo != null) {
    			stream.writeInt(i);
    			Integer alreadyWrittenIndex = alreadyWritten.get(fatFileInfo);
    			if (alreadyWrittenIndex != null) {
    				stream.writeInt(alreadyWrittenIndex.intValue());
    			} else {
    				stream.writeInt(-1);
    				fatFileInfo.write(stream);
        			alreadyWritten.put(fatFileInfo, i);
        			fatFileInfoList.add(fatFileInfo);
    			}
    		}
    	}
    	// End of the fatFileInfoMap
    	stream.writeInt(-1);

    	// Write the parent-children relations
    	for (FatFileInfo fatFileInfo : fatFileInfoList) {
			fatFileInfo.write(stream, this);
    	}
    }

    private int getFatFileInfoMapIndex(FatFileInfo info) {
		if (info != null) {
			for (int i = 0; i < fatFileInfoMap.length; i++) {
				if (info == fatFileInfoMap[i]) {
					return i;
				}
			}
		}

		return -1;
	}

	private FatFileInfo getFatFileInfoFromMapIndex(int index) {
		if (index < 0 || index >= fatFileInfoMap.length) {
			return null;
		}
		return fatFileInfoMap[index];
	}

    public FatFileInfo readFatFileInfo(StateInputStream stream) throws IOException {
    	int mapIndex = stream.readInt();
    	return getFatFileInfoFromMapIndex(mapIndex);
    }

    public void writeFatFileInfo(StateOutputStream stream, FatFileInfo info) throws IOException {
    	int mapIndex = getFatFileInfoMapIndex(info);
    	stream.writeInt(mapIndex);
    }

    @Override
	public void invalidateCachedData() {
    	// Nothing to do
	}

    private void readAllDirectories(String dirName, IVirtualFileSystem vfs) {
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

					if (directory) {
						if (dirName == null) {
							readAllDirectories(dir.filename, vfs);
						} else {
							readAllDirectories(dirName + "/" + dir.filename, vfs);
						}
					}
				}
			}
		}
    }

    private void setFatFileInfoMapRecursive(FatVirtualFileSystem fatVirtualFileSystem, int clusterNumber, FatFileInfo parentDirectory) {
    	FatFileInfo[] fatFileInfos = fatVirtualFileSystem.getDirectoryEntries(clusterNumber);
    	if (fatFileInfos != null) {
    		String dirName = null;
    		if (parentDirectory != null) {
    			dirName = parentDirectory.getFullFileName();
    		}
	    	for (FatFileInfo fatFileInfo : fatFileInfos) {
	    		fatFileInfo.setParentDirectory(parentDirectory);
    			fatFileInfo.setDirName(dirName);
				parentDirectory.addChild(fatFileInfo);
	    		setFatFileInfoMap(fatFileInfo);

	    		if (fatFileInfo.isDirectory()) {
	    			int directoryClusterNumber = fatFileInfo.getFirstCluster();
	    			if (directoryClusterNumber != 0) {
	    				setFatFileInfoMapRecursive(fatVirtualFileSystem, directoryClusterNumber, fatFileInfo);
	    			}
	    		}
	    	}
    	}
    }

    @Override
	public synchronized void flushCachedData() {
    	if (log.isDebugEnabled()) {
    		log.debug(String.format("flushCachedData: rebuilding the FAT"));
    	}

    	// Read the whole directory structure into a cache before we reset().
    	ReadCacheVirtualFile directoriesVirtualFile = new ReadCacheVirtualFile(log, baseVirtualFile);
    	FatVirtualFileSystem fatVirtualFileSystem = new FatVirtualFileSystem(deviceName, directoriesVirtualFile);
    	readAllDirectories(null, fatVirtualFileSystem);

    	// For debugging purpose, make sure we are going to only read from the cache
    	directoriesVirtualFile.setProxyVirtualFile(new InvalidVirtualFile());

    	// Close all the files before we reset
    	closeCachedFiles();

    	reset();

    	// Copy the fatClusterMap from the VFS
    	System.arraycopy(fatVirtualFileSystem.getFatClusterMap(), 0, fatClusterMap, 0, fatClusterMap.length);
    	int rootDirectoryClusterNumber = fatVirtualFileSystem.getRootDirectoryClusterNumber();

    	// Rebuild all the FatFileInfo's
		FatFileInfo rootDirectory = new FatFileInfo(getDeviceName(), null, null, true, false, null, 0);
		rootDirectory.addCluster(rootDirectoryClusterNumber);
    	setFatFileInfoMapRecursive(fatVirtualFileSystem, rootDirectoryClusterNumber, rootDirectory);
    	setFatFileInfoMap(rootDirectory);
	}

	@Override
	public synchronized void closeCachedFiles() {
		for (FatFileInfo fatFileInfo : fatFileInfoMap) {
			if (fatFileInfo != null) {
				fatFileInfo.closeVirtualFile();
			}
		}
	}

    @Override
	public String toString() {
    	return String.format("%s%s", deviceName == null ? "" : deviceName, vfs);
	}
}
