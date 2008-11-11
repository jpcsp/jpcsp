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
import java.nio.ByteBuffer;
import java.util.Vector;

import jpcsp.Memory;
import jpcsp.HLE.Modules;
import jpcsp.HLE.pspiofilemgr;
import jpcsp.filesystems.SeekableDataInput;
import jpcsp.filesystems.SeekableRandomFile;
import jpcsp.format.PSF;
import jpcsp.util.Utilities;

public class SceUtilitySavedataParam {
	public final static String savedataPath     = "ms0:/PSP/SAVEDATA/";
	public final static String icon0FileName    = "ICON0.PNG";
	public final static String icon1FileName    = "ICON1.PNG";
	public final static String pic1FileName     = "PIC1.PNG";
	public final static String snd0FileName     = "SND0.PNG";
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
	int dataSize;
	public PspUtilitySavedataSFOParam sfoParam;
	public PspUtilitySavedataFileData icon0FileData;
	public PspUtilitySavedataFileData icon1FileData;
	public PspUtilitySavedataFileData pic1FileData;
	public PspUtilitySavedataFileData snd0FileData;
	int newDataAddr;
	public PspUtilitySavedataListSaveNewData newData;
	public String key;		// encrypt/decrypt key for save with firmware >= 2.00

	public class PspUtilitySavedataSFOParam {
		public String title;
		public String savedataTitle;
		public String detail;
		public int parentalLevel;

		public void read(Memory mem, int address) {
			title = Utilities.readStringNZ(mem, address + 0, 0x80);
			savedataTitle = Utilities.readStringNZ(mem, address + 0x80, 0x80);
			detail = Utilities.readStringNZ(mem, address + 0x100, 0x400);
			parentalLevel = mem.read8(address + 0x500);
		}

		public void write(Memory mem, int address) {
			Utilities.writeStringNZ(mem, address + 0, 0x80, title);
			Utilities.writeStringNZ(mem, address + 0x80, 0x80, savedataTitle);
			Utilities.writeStringNZ(mem, address + 0x100, 0x400, detail);
			mem.write8(address + 0x500, (byte) parentalLevel);
			mem.write8(address + 0x501, (byte) pspUtilityDialogCommon.unknown);
			mem.write8(address + 0x502, (byte) pspUtilityDialogCommon.unknown);
			mem.write8(address + 0x503, (byte) pspUtilityDialogCommon.unknown);
		}

		public int sizeof() {
			return 0x80 + 0x80 + 0x400 + 4;
		}
	};

	public class PspUtilitySavedataFileData {
		public int buf;
		public int bufSize;
		public int size;

		public void read(Memory mem, int address) {
			buf     = mem.read32(address + 0);
			bufSize = mem.read32(address + 4);
			size    = mem.read32(address + 8);
		}

		public void write(Memory mem, int address) {
			mem.write32(address + 0, buf);
			mem.write32(address + 4, bufSize);
			mem.write32(address + 8, size);
			mem.write32(address + 12, pspUtilityDialogCommon.unknown);
		}

		public int sizeof() {
			return 4 * 4;
		}
	}

	public class PspUtilitySavedataListSaveNewData {
		public PspUtilitySavedataFileData icon0;
		public int titleAddr;
		public String title;

		public void read(Memory mem, int address) {
			icon0 = new PspUtilitySavedataFileData();
			icon0.read(mem, address);
			titleAddr = mem.read32(address + icon0.sizeof());
			if (titleAddr != 0) {
				title = Utilities.readStringZ(mem, titleAddr);
			} else {
				title = null;
			}
		}

		public void write(Memory mem, int address) {
			icon0.write(mem, address);
			mem.write32(address + icon0.sizeof(), titleAddr);
			if (titleAddr != 0) {
				Utilities.writeStringZ(mem, titleAddr, title);
			}
		}

		public int sizeof() {
			return icon0.sizeof() + 4;
		}
	}

	public void read(Memory mem, int address) {
		int offset = 0;
		baseAddress = address;

		base = new pspUtilityDialogCommon();
		base.read(mem, address + offset);
		offset += base.sizeof();

		mode        = mem.read32(address + offset);
		offset += 4 + 4;	// 4 bytes unknown
		overwrite   = mem.read32(address + offset) == 0 ? false : true;
		offset += 4;
		gameName    = Utilities.readStringNZ(mem, address + offset, 13);
		offset += 16;
		saveName    = Utilities.readStringNZ(mem, address + offset, 20);
		offset += 20;
		saveNameListAddr = mem.read32(address + offset);
		offset += 4;
		if (saveNameListAddr != 0) {
			Vector<String> saveNameList = new Vector<String>();
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
		fileName    = Utilities.readStringNZ(mem, address + offset, 13);
		offset += 16;
		dataBuf     = mem.read32(address + offset);
		offset += 4;
		dataBufSize = mem.read32(address + offset);
		offset += 4;
		dataSize    = mem.read32(address + offset);
		offset += 4;

		sfoParam = new PspUtilitySavedataSFOParam();
		sfoParam.read(mem, address + offset);
		offset += sfoParam.sizeof();

		icon0FileData = new PspUtilitySavedataFileData();
		icon0FileData.read(mem, address + offset);
		offset += icon0FileData.sizeof();

		icon1FileData = new PspUtilitySavedataFileData();
		icon1FileData.read(mem, address + offset);
		offset += icon1FileData.sizeof();

		pic1FileData = new PspUtilitySavedataFileData();
		pic1FileData.read(mem, address + offset);
		offset += pic1FileData.sizeof();

		snd0FileData = new PspUtilitySavedataFileData();
		snd0FileData.read(mem, address + offset);
		offset += snd0FileData.sizeof();

		newDataAddr = mem.read32(address + offset);
		if (newDataAddr != 0) {
			newData = new PspUtilitySavedataListSaveNewData();
			newData.read(mem, newDataAddr);
		} else {
			newData = null;
		}
		offset += 4;
		focus = mem.read32(address + offset);
		offset += 4 + 16;	// 16 bytes unknown
		key = Utilities.readStringNZ(mem, address + offset + 24, 16);
	}

	public void write(Memory mem) {
		write(mem, baseAddress);
	}

	public void write(Memory mem, int address) {
		int offset = 0;
		baseAddress = address;

		base.write(mem, address + offset);
		offset += base.sizeof();

		mem.write32(address + offset, mode);
		offset += 4;
		mem.write32(address + offset, pspUtilityDialogCommon.unknown);
		offset += 4;
		mem.write32(address + offset, overwrite ? 1 : 0);
		offset += 4;
		Utilities.writeStringNZ(mem, address + offset, 13, gameName);
		offset += 16;
		Utilities.writeStringNZ(mem, address + offset, 20, saveName);
		offset += 20;
		mem.write32(address + offset, saveNameListAddr);
		offset += 4;
		Utilities.writeStringNZ(mem, address + offset, 13, fileName);
		offset += 16;
		mem.write32(address + offset, dataBuf);
		offset += 4;
		mem.write32(address + offset, dataBufSize);
		offset += 4;
		mem.write32(address + offset, dataSize);
		offset += 4;

		sfoParam.write(mem, address + offset);
		offset += sfoParam.sizeof();

		icon0FileData.write(mem, address + offset);
		offset += icon0FileData.sizeof();

		icon1FileData.write(mem, address + offset);
		offset += icon1FileData.sizeof();

		pic1FileData.write(mem, address + offset);
		offset += pic1FileData.sizeof();

		snd0FileData.write(mem, address + offset);
		offset += snd0FileData.sizeof();

		mem.write32(address + offset, newDataAddr);
		offset += 4;
		if (newDataAddr != 0) {
			newData.write(mem, newDataAddr);
		}
		mem.write32(address + offset, focus);
		offset += 4;
		mem.write32(address + offset, pspUtilityDialogCommon.unknown);
		offset += 4;
		mem.write32(address + offset, pspUtilityDialogCommon.unknown);
		offset += 4;
		mem.write32(address + offset, pspUtilityDialogCommon.unknown);
		offset += 4;
		mem.write32(address + offset, pspUtilityDialogCommon.unknown);
		offset += 4;
		Utilities.writeStringNZ(mem, address + offset, 16, key);
	}

	public void load(Memory mem, pspiofilemgr fileManager) throws IOException {
		String path = savedataPath + gameName + saveName + "/";

		// Firmware 1.5 stores data file non-encrypted.
		// From Firmware 2.0, the data file is encrypted using the kirk chip.
		// Encrypted files cannot be loaded.
		// TODO Detect and reject an encrypted data file.
		dataSize           = loadFile(mem, fileManager, path, fileName,      dataBuf,           dataBufSize);
		icon0FileData.size = loadFile(mem, fileManager, path, icon0FileName, icon0FileData.buf, icon0FileData.bufSize);
		icon1FileData.size = loadFile(mem, fileManager, path, icon1FileName, icon1FileData.buf, icon1FileData.bufSize);
		pic1FileData.size  = loadFile(mem, fileManager, path, pic1FileName,  pic1FileData.buf,  pic1FileData.bufSize);
		snd0FileData.size  = loadFile(mem, fileManager, path, snd0FileName,  snd0FileData.buf,  snd0FileData.bufSize);
		loadPsf(mem, fileManager, path, paramSfoFileName, sfoParam);
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
		byte[] buffer = new byte[(int) fileInput.length()];
		fileInput.readFully(buffer);
		fileInput.close();

		PSF psf = new PSF(0);
		psf.read(ByteBuffer.wrap(buffer));

		sfoParam.parentalLevel = (int) psf.getNumeric("PARENTAL_LEVEL");
		sfoParam.title = psf.getString("TITLE");
		sfoParam.detail = psf.getString("SAVEDATA_DETAIL");
		sfoParam.savedataTitle = psf.getString("SAVEDATA_TITLE");
	}

	private int loadFile(Memory mem, pspiofilemgr fileManager, String path, String name, int address, int maxLength) throws IOException {
		if (name == null || name.length() <= 0 || address == 0 || maxLength <= 0) {
			return 0;
		}

		SeekableDataInput fileInput = getDataInput(fileManager, path, name);
		if (fileInput == null) {
			return 0;
		}

		int fileSize = (int) fileInput.length();
		if (fileSize > maxLength) {
			fileSize = maxLength;
		}

		for (int i = 0; i < fileSize; i++) {
			byte b = fileInput.readByte();
			mem.write8(address + i, b);
		}
		fileInput.close();

		return fileSize;
	}

	public void save(Memory mem, pspiofilemgr fileManager) throws IOException {
		String path = savedataPath + gameName + saveName + "/";

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

		for (int i = 0; i < length; i++) {
			int b = (byte) mem.read8(address + i);
			fileOutput.writeByte(b);
		}
		fileOutput.close();
	}

	private void writePsf(Memory mem, pspiofilemgr fileManager, String path, String name, PspUtilitySavedataSFOParam sfoParam) throws IOException {
		// TODO Implement writing of PSF file
		Modules.log.error("Unimplemented - saving of " + name);
	}
}
