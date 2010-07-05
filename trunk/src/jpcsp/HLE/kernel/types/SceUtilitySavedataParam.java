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
package jpcsp.HLE.kernel.types;

import java.io.IOException;
import java.io.FileNotFoundException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel.MapMode;
import java.util.ArrayList;
import java.util.List;

import jpcsp.Memory;
import jpcsp.HLE.pspiofilemgr;
import jpcsp.filesystems.SeekableDataInput;
import jpcsp.filesystems.SeekableRandomFile;
import jpcsp.format.PSF;
import jpcsp.util.Utilities;

/*
 * TODO list:
 * 1. Check writePsf:
 *   -> sfoParam.parentalLevel & 0x3ff ? FF1 -> 1027 -> 3.
 *   -> Need to add SAVEDATA_FILE_LIST, SAVEDATA_PARAMS and SAVEDATA_DIRECTORY?
     -> Delete original file, size "map" cannot resize it smaller.
 */

public class SceUtilitySavedataParam extends pspAbstractMemoryMappedStructure {
	public final static String savedataPath     = "ms0:/PSP/SAVEDATA/";
	public final static String icon0FileName    = "ICON0.PNG";
	public final static String icon1FileName    = "ICON1.PNG";
	public final static String pic1FileName     = "PIC1.PNG";
	public final static String snd0FileName     = "SND0.AT3";
	public final static String paramSfoFileName = "PARAM.SFO";

	public int baseAddress;

	public pspUtilityDialogCommon base;
	public int mode;
		public final static int MODE_AUTOLOAD = 0;
		public final static int MODE_AUTOSAVE = 1;
		public final static int MODE_LOAD = 2;
		public final static int MODE_SAVE = 3;
		public final static int MODE_LISTLOAD = 4;
		public final static int MODE_LISTSAVE = 5;
		public final static int MODE_LISTDELETE = 6;
		public final static int MODE_DELETE = 7;
		public final static int MODE_SIZES = 8;
        public final static int MODE_LIST = 11;
        public final static int MODE_FILES = 12;
		public final static int MODE_TEST = 15;
        public final static int MODE_GETSIZE = 22;
	public int focus;
		public final static int FOCUS_UNKNOWN = 0;
		public final static int FOCUS_FIRSTLIST = 1;	// First in list
		public final static int FOCUS_LASTLIST = 2;		// Last in list
		public final static int FOCUS_LATEST = 3;		// Most recent one
		public final static int FOCUS_OLDEST = 4;		// Oldest one
		public final static int FOCUS_FIRSTEMPTY = 7;	// First empty slot
		public final static int FOCUS_LASTEMPTY = 8;	// Last empty slot
	public boolean overwrite;
	public String gameName; // name used from the game for saves, equal for all saves
	public String saveName; // name of the particular save, normally a number
	public String fileName; // name of the data file of the game for example DATA.BIN
	public String[] saveNameList; // used by multiple modes
	public int saveNameListAddr;
	int dataBuf;
	int dataBufSize;
	public int dataSize;
	public PspUtilitySavedataSFOParam sfoParam;
	public PspUtilitySavedataFileData icon0FileData;
	public PspUtilitySavedataFileData icon1FileData;
	public PspUtilitySavedataFileData pic1FileData;
	public PspUtilitySavedataFileData snd0FileData;
	int newDataAddr;
	public PspUtilitySavedataListSaveNewData newData;
	public int buffer1Addr;
	public int buffer2Addr;
	public int buffer3Addr;
	public int buffer4Addr;
    public int buffer5Addr;
    public int buffer6Addr;
	public String key;		// encrypt/decrypt key for save with firmware >= 2.00
    public int secureVersion;
    public int errorStatus;
    public int secureStatus;

	public static class PspUtilitySavedataSFOParam extends pspAbstractMemoryMappedStructure {
        public String title;
		public String savedataTitle;
		public String detail;
		public int parentalLevel;

		protected void read() {
			title = readStringNZ(0x80);
			savedataTitle = readStringNZ(0x80);
			detail = readStringNZ(0x400);
			parentalLevel = read32();
		}

		protected void write() {
		    writeStringNZ(0x80, title);
            writeStringNZ(0x80, savedataTitle);
            writeStringNZ(0x400, detail);
            write32(parentalLevel);
		}

		public int sizeof() {
			return 0x80 + 0x80 + 0x400 + 4;
		}
	};

	public static class PspUtilitySavedataFileData extends pspAbstractMemoryMappedStructure {
		public int buf;
		public int bufSize;
		public int size;

		protected void read() {
			buf     = read32();
			bufSize = read32();
			size    = read32();
			readUnknown(4);
		}

		protected void write() {
			write32(buf);
			write32(bufSize);
			write32(size);
			writeUnknown(4);
		}

		public int sizeof() {
			return 4 * 4;
		}
	}

	public static class PspUtilitySavedataListSaveNewData extends pspAbstractMemoryMappedStructure {
		public PspUtilitySavedataFileData icon0;
		public int titleAddr;
		public String title;

		protected void read() {
			icon0 = new PspUtilitySavedataFileData();
			read(icon0);
			titleAddr = read32();
			if (titleAddr != 0) {
				title = Utilities.readStringZ(mem, titleAddr);
			} else {
				title = null;
			}
		}

		protected void write() {
			write(icon0);
			write32(titleAddr);
			if (titleAddr != 0) {
				Utilities.writeStringZ(mem, titleAddr, title);
			}
		}

		public int sizeof() {
			return icon0.sizeof() + 4;
		}
	}

	protected void read() {
		base = new pspUtilityDialogCommon();
		read(base);
		setMaxSize(base.size);

		mode        = read32();
		readUnknown(4);
		overwrite   = read32() == 0 ? false : true;
		gameName    = readStringNZ(13);
		readUnknown(3);
		saveName    = readStringNZ(20);
		saveNameListAddr = read32();
		if (saveNameListAddr != 0) {
			List<String> saveNameList = new ArrayList<String>();
			boolean endOfList = false;
			for (int i = 0; !endOfList; i += 20) {
				String saveNameItem = Utilities.readStringNZ(mem, saveNameListAddr + i, 20);
				if (saveNameItem == null || saveNameItem.length() == 0) {
					endOfList = true;
				} else {
					saveNameList.add(saveNameItem);
				}
			}
			this.saveNameList = saveNameList.toArray(new String[saveNameList.size()]);
		}
		fileName    = readStringNZ(13);
		readUnknown(3);
		dataBuf     = read32();
		dataBufSize = read32();
		dataSize    = read32();

		sfoParam = new PspUtilitySavedataSFOParam();
		read(sfoParam);
		icon0FileData = new PspUtilitySavedataFileData();
		read(icon0FileData);
		icon1FileData = new PspUtilitySavedataFileData();
		read(icon1FileData);
		pic1FileData = new PspUtilitySavedataFileData();
		read(pic1FileData);
		snd0FileData = new PspUtilitySavedataFileData();
		read(snd0FileData);

		newDataAddr = read32();
		if (newDataAddr != 0) {
			newData = new PspUtilitySavedataListSaveNewData();
			newData.read(mem, newDataAddr);
		} else {
			newData = null;
		}
        focus = read32();
		errorStatus = read32();
		buffer1Addr = read32();
        buffer2Addr = read32();
        buffer3Addr = read32();
		key = readStringNZ(16);
		secureVersion = read32();
        secureStatus = read32();
		buffer4Addr = read32();
        buffer5Addr = read32();
        buffer6Addr = read32();
	}

	protected void write() {
        setMaxSize(base.size);
	    write(base);

		write32(mode);
		writeUnknown(4);
		write32(overwrite ? 1 : 0);
		writeStringNZ(13, gameName);
		writeUnknown(3);
		writeStringNZ(20, saveName);
		write32(saveNameListAddr);
		writeStringNZ(13, fileName);
		writeUnknown(3);
		write32(dataBuf);
		write32(dataBufSize);
		write32(dataSize);

		write(sfoParam);
		write(icon0FileData);
        write(icon1FileData);
        write(pic1FileData);
        write(snd0FileData);

		write32(newDataAddr);
		if (newDataAddr != 0) {
			newData.write(mem, newDataAddr);
		}
		write32(focus);
		write32(errorStatus);
		write32(buffer1Addr);
        write32(buffer2Addr);
        write32(buffer3Addr);
		writeStringNZ(16, key);
		write32(secureVersion);
        write32(secureStatus);
		write32(buffer4Addr);
        write32(buffer5Addr);
        write32(buffer6Addr);
	}

    private void safeLoad(Memory mem, pspiofilemgr fileManager, String filename, PspUtilitySavedataFileData fileData) throws IOException {
		String path = getBasePath();

        try {
            fileData.size = loadFile(mem, fileManager, path, filename, fileData.buf, fileData.bufSize);
        } catch(FileNotFoundException e) {
            // ignore
        }
    }

	public boolean test(Memory mem, pspiofilemgr fileManager) throws IOException {
		String path = getBasePath();

		boolean result = testFile(mem, fileManager, path, fileName);

		return result;
	}

	public void load(Memory mem, pspiofilemgr fileManager) throws IOException {
		String path = getBasePath();

		// Firmware 1.5 stores data file non-encrypted.
		// From Firmware 2.0, the data file is encrypted using the kirk chip.
		// Encrypted files cannot be loaded.
		// TODO Detect and reject an encrypted data file.
		dataSize = loadFile(mem, fileManager, path, fileName, dataBuf, dataBufSize);
        safeLoad(mem, fileManager, icon0FileName, icon0FileData);
        safeLoad(mem, fileManager, icon1FileName, icon1FileData);
        safeLoad(mem, fileManager, pic1FileName, pic1FileData);
        safeLoad(mem, fileManager, snd0FileName, snd0FileData);
		loadPsf(mem, fileManager, path, paramSfoFileName, sfoParam);
	}

    private String getBasePath() {
        return getBasePath(saveName);
    }

    public String getBasePath(String saveName) {
        String path = savedataPath + gameName;
        if (!saveName.equals("<>")) {
            path += saveName;
        }
        path += "/";
        return path;
    }

	public String getFileName(String saveName, String fileName) {
		return getBasePath(saveName) + fileName;
	}

	public boolean isPresent(pspiofilemgr fileManager, String gameName, String saveName) {
	    if (fileName == null || fileName.length() <= 0) {
	        return false;
	    }

	    String path = getBasePath();
	    try {
            SeekableDataInput fileInput = getDataInput(fileManager, path, fileName);
            if (fileInput != null) {
                fileInput.close();
                return true;
            }
	    } catch (IOException e) {
	    }

        return false;
	}

	public boolean isPresent(pspiofilemgr fileManager) {
		return isPresent(fileManager, gameName, saveName);
	}

	private int getFileSize(pspiofilemgr fileManager, String fileName) {
		int size = 0;

		if (fileName != null && fileName.length() > 0) {
		    SceIoStat fileStat = fileManager.statFile(getFileName(saveName, fileName));
		    if (fileStat != null) {
		    	size = (int) fileStat.size;
		    }
		}

        return size;
	}

	public int getSize(pspiofilemgr fileManager, String gameName, String saveName) {
		int size;

		size  = getFileSize(fileManager, fileName);
		size += getFileSize(fileManager, icon0FileName);
		size += getFileSize(fileManager, icon1FileName);
		size += getFileSize(fileManager, pic1FileName);
		size += getFileSize(fileManager, snd0FileName);
		size += getFileSize(fileManager, paramSfoFileName);

        return size;
	}

	private SeekableDataInput getDataInput(pspiofilemgr fileManager, String path, String name) {
		SeekableDataInput fileInput = fileManager.getFile(path + name, pspiofilemgr.PSP_O_RDONLY);

		return fileInput;
	}

	private SeekableRandomFile getDataOutput(pspiofilemgr fileManager, String path, String name) {
		SeekableDataInput fileInput = fileManager.getFile(path + name, pspiofilemgr.PSP_O_RDWR | pspiofilemgr.PSP_O_CREAT);

		if (fileInput instanceof SeekableRandomFile) {
			return (SeekableRandomFile) fileInput;
		}

		return null;
	}

    private void loadPsf(Memory mem, pspiofilemgr fileManager, String path, String name, PspUtilitySavedataSFOParam sfoParam) throws IOException {
        SeekableDataInput fileInput = getDataInput(fileManager, path, name);
        if (fileInput != null) {
            byte[] buffer = new byte[(int) fileInput.length()];
            fileInput.readFully(buffer);
            fileInput.close();

            PSF psf = new PSF();
            psf.read(ByteBuffer.wrap(buffer));

            sfoParam.parentalLevel = (int) psf.getNumeric("PARENTAL_LEVEL");
            sfoParam.title = psf.getString("TITLE");
            sfoParam.detail = psf.getString("SAVEDATA_DETAIL");
            sfoParam.savedataTitle = psf.getString("SAVEDATA_TITLE");
        }
    }

	private boolean testFile(Memory mem, pspiofilemgr fileManager, String path, String name) throws IOException {
		if (name == null || name.length() <= 0) {
			return false;
		}

		SeekableDataInput fileInput = getDataInput(fileManager, path, name);
		if (fileInput == null) {
			throw new FileNotFoundException("File not found '" + path + "' '" + name + "'");
		}

		fileInput.close();

		return true;
	}

	private int loadFile(Memory mem, pspiofilemgr fileManager, String path, String name, int address, int maxLength) throws IOException {
		if (name == null || name.length() <= 0 || address == 0 || maxLength <= 0) {
			return 0;
		}

		SeekableDataInput fileInput = getDataInput(fileManager, path, name);
		if (fileInput == null) {
			throw new FileNotFoundException("File not found '" + path + "' '" + name + "'");
		}

		int fileSize = (int) fileInput.length();
		if (fileSize > maxLength) {
			fileSize = maxLength;
		}

		Utilities.readFully(fileInput, address, fileSize);
		fileInput.close();

		return fileSize;
	}

	public void save(Memory mem, pspiofilemgr fileManager) throws IOException {
		String path = getBasePath();

		fileManager.mkdirs(path);
		writeFile(mem, fileManager, path, fileName,      dataBuf,           dataSize);
		writeFile(mem, fileManager, path, icon0FileName, icon0FileData.buf, icon0FileData.size);
		writeFile(mem, fileManager, path, icon1FileName, icon1FileData.buf, icon1FileData.size);
		writeFile(mem, fileManager, path, pic1FileName,  pic1FileData.buf,  pic1FileData.size);
		writeFile(mem, fileManager, path, snd0FileName,  snd0FileData.buf,  snd0FileData.size);
		writePsf(mem, fileManager, path, paramSfoFileName, sfoParam);
	}

	private void writeFile(Memory mem, pspiofilemgr fileManager, String path, String name, int address, int length) throws IOException {
		if (name == null || name.length() <= 0 || address == 0) {
			return;
		}

		SeekableRandomFile fileOutput = getDataOutput(fileManager, path, name);
		if (fileOutput == null) {
			return;
		}

        Utilities.write(fileOutput, address, length);
		fileOutput.close();
	}

	private void writePsf(Memory mem, pspiofilemgr fileManager, String path, String name, PspUtilitySavedataSFOParam sfoParam) throws IOException {
        SeekableRandomFile fileOutput = getDataOutput(fileManager, path, name);
		if (fileOutput == null) {
			return;
		}

		PSF psf = new PSF();
        psf.put("PARENTAL_LEVEL", sfoParam.parentalLevel);
        psf.put("TITLE", sfoParam.title, 128);
        psf.put("SAVEDATA_DETAIL", sfoParam.detail, 1024);
        psf.put("SAVEDATA_TITLE", sfoParam.savedataTitle, 128);

        psf.write(fileOutput.getChannel().map(MapMode.READ_WRITE, 0, psf.size()));
	}

    public int sizeof() {
        return base.size;
    }

	@Override
	public String toString() {
		return String.format("Address 0x%08X, mode=%d, gameName=%s, saveName=%s, fileName=%s",
				getBaseAddress(), mode, gameName, saveName, fileName);
	}
}