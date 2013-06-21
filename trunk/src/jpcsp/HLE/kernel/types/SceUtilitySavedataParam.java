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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import jpcsp.Memory;
import jpcsp.memory.IMemoryReader;
import jpcsp.memory.MemoryReader;
import jpcsp.memory.IMemoryWriter;
import jpcsp.memory.MemoryWriter;
import jpcsp.HLE.Modules;
import jpcsp.HLE.VFS.IVirtualFileSystem;
import jpcsp.crypto.CryptoEngine;
import jpcsp.filesystems.SeekableDataInput;
import jpcsp.filesystems.SeekableRandomFile;
import jpcsp.format.PSF;
import jpcsp.hardware.MemoryStick;
import jpcsp.util.Utilities;

public class SceUtilitySavedataParam extends pspAbstractMemoryMappedStructure {

    public final static String savedataPath = "ms0:/PSP/SAVEDATA/";
    public final static String savedataFilePath = "ms0/PSP/SAVEDATA/";
    public final static String icon0FileName = "ICON0.PNG";
    public final static String icon1PNGFileName = "ICON1.PNG";
    public final static String icon1PMFFileName = "ICON1.PMF";
    public final static String pic1FileName = "PIC1.PNG";
    public final static String snd0FileName = "SND0.AT3";
    public final static String paramSfoFileName = "PARAM.SFO";
    public final static String anyFileName = "<>";
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
    public final static int MODE_AUTODELETE = 9;
    public final static int MODE_SINGLEDELETE = 10;
    public final static int MODE_LIST = 11;
    public final static int MODE_FILES = 12;
    public final static int MODE_MAKEDATASECURE = 13;
    public final static int MODE_MAKEDATA = 14;
    public final static int MODE_READSECURE = 15;
    public final static int MODE_READ = 16;
    public final static int MODE_WRITESECURE = 17;
    public final static int MODE_WRITE = 18;
    public final static int MODE_ERASESECURE = 19;
    public final static int MODE_ERASE = 20;
    public final static int MODE_DELETEDATA = 21;
    public final static int MODE_GETSIZE = 22;
    public final static String[] modeNames = new String[]{
        "AUTOLOAD",
        "AUTOSAVE",
        "LOAD",
        "SAVE",
        "LISTLOAD",
        "LISTSAVE",
        "LISTDELETE",
        "DELETE",
        "SIZES",
        "AUTODELETE",
        "SINGLEDELETE",
        "LIST",
        "FILES",
        "MAKEDATASECURE",
        "MAKEDATA",
        "READSECURE",
        "READ",
        "WRITESECURE",
        "WRITE",
        "ERASESECURE",
        "ERASE",
        "DELETEDATA",
        "GETSIZE"
    };
    public int bind;   // Used by certain applications to detect if this save data was created on a different PSP.
    public final static int BIND_NOT_USED = 0;
    public final static int BIND_IS_OK = 1;
    public final static int BIND_IS_REJECTED = 2;
    public final static int BIND_IS_NOT_SUPPORTED = 3;
    public boolean overwrite;
    public String gameName; // name used from the game for saves, equal for all saves
    public String saveName; // name of the particular save, normally a number
    public String fileName; // name of the data file of the game for example DATA.BIN
    public String[] saveNameList; // used by multiple modes
    public int saveNameListAddr;
    public int dataBuf;
    public int dataBufSize;
    public int dataSize;
    public PspUtilitySavedataSFOParam sfoParam;
    public PspUtilitySavedataFileData icon0FileData;
    public PspUtilitySavedataFileData icon1FileData;
    public PspUtilitySavedataFileData pic1FileData;
    public PspUtilitySavedataFileData snd0FileData;
    int newDataAddr;
    public PspUtilitySavedataListSaveNewData newData;
    public int focus;
    public final static int FOCUS_UNKNOWN = 0;
    public final static int FOCUS_FIRSTLIST = 1;	// First in list
    public final static int FOCUS_LASTLIST = 2;		// Last in list
    public final static int FOCUS_LATEST = 3;		// Most recent one
    public final static int FOCUS_OLDEST = 4;		// Oldest one
    public final static int FOCUS_FIRSTEMPTY = 7;	// First empty slot
    public final static int FOCUS_LASTEMPTY = 8;	// Last empty slot
    public int abortStatus;     // Used by sceUtilityXXXAbort functions.
    public int msFreeAddr;      // Address of a buffer to hold MemoryStick free size data (used in MODE_SIZES only).
    public int msDataAddr;      // Address of a buffer to hold MemoryStick size data (used in MODE_SIZES only).
    public int utilityDataAddr; // Address of a buffer to hold utility size data (used in MODE_SIZES only).
    public byte[] key = new byte[0x10];                   // Encrypt/decrypt key for saves with firmware >= 2.00.
    public int secureVersion;   // 0 - Pre 2.00 (no encrypted files) / 1 - Post 2.00 (encrypted files are now used).
    public int multiStatus;     // After 2.00, several modes can be triggered at the same time using this for sync.
    public final static int MULTI_STATUS_SINGLE = 0;  // Save data is all generated in one call.
    public final static int MULTI_STATUS_INIT = 1;	  // Save data is generated in multiple calls and this is the first one.
    public final static int MULTI_STATUS_RELAY = 2;	  // Save data is generated in multiple calls and this is an intermediate call.
    public final static int MULTI_STATUS_FINISH = 3;  // Save data is generated in multiple calls and this is the last one.
    public int idListAddr;      // Address of a buffer to hold the file IDs generated by MODE_LIST.
    public int fileListAddr;    // Address of a buffer to hold the file names generated by MODE_FILES.
    public int sizeAddr;        // Address of a buffer to hold the sizes generated by MODE_GETSIZE.

    public static class PspUtilitySavedataSFOParam extends pspAbstractMemoryMappedStructure {

        public String title;
        public String savedataTitle;
        public String detail;
        public int parentalLevel;

        @Override
        protected void read() {
            title = readStringNZ(0x80);
            savedataTitle = readStringNZ(0x80);
            detail = readStringNZ(0x400);
            parentalLevel = read32();
        }

        @Override
        protected void write() {
            writeStringNZ(0x80, title);
            writeStringNZ(0x80, savedataTitle);
            writeStringNZ(0x400, detail);
            write32(parentalLevel);
        }

        @Override
        public int sizeof() {
            return 0x80 + 0x80 + 0x400 + 4;
        }
    };

    public static class PspUtilitySavedataFileData extends pspAbstractMemoryMappedStructure {

        public int buf;
        public int bufSize;
        public int size;

        @Override
        protected void read() {
            buf = read32();
            bufSize = read32();
            size = read32();
            readUnknown(4);
        }

        @Override
        protected void write() {
            write32(buf);
            write32(bufSize);
            write32(size);
            writeUnknown(4);
        }

        @Override
        public int sizeof() {
            return 4 * 4;
        }
    }

    public static class PspUtilitySavedataListSaveNewData extends pspAbstractMemoryMappedStructure {

        public PspUtilitySavedataFileData icon0;
        public int titleAddr;
        public String title;

        @Override
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

        @Override
        protected void write() {
            write(icon0);
            write32(titleAddr);
            if (titleAddr != 0) {
                Utilities.writeStringZ(mem, titleAddr, title);
            }
        }

        @Override
        public int sizeof() {
            return icon0.sizeof() + 4;
        }
    }

    @Override
    protected void read() {
        base = new pspUtilityDialogCommon();
        read(base);
        setMaxSize(base.totalSizeof());

        mode = read32(); // Offset 48
        bind = read32(); // Offset 52
        overwrite = read32() == 0 ? false : true; // Offset 56
        gameName = readStringNZ(13); // Offset 60
        readUnknown(3);
        saveName = readStringNZ(20); // Offset 76
        saveNameListAddr = read32(); // Offset 96
        if (Memory.isAddressGood(saveNameListAddr)) {
            List<String> newSaveNameList = new ArrayList<String>();
            boolean endOfList = false;
            for (int i = 0; !endOfList; i += 20) {
                String saveNameItem = Utilities.readStringNZ(mem, saveNameListAddr + i, 20);
                if (saveNameItem == null || saveNameItem.length() == 0) {
                    endOfList = true;
                } else {
                    newSaveNameList.add(saveNameItem);
                }
            }
            saveNameList = newSaveNameList.toArray(new String[newSaveNameList.size()]);
        }
        fileName = readStringNZ(13); // Offset 100
        readUnknown(3);
        dataBuf = read32(); // Offset 116
        dataBufSize = read32(); // Offset 120
        dataSize = read32(); // Offset 124

        sfoParam = new PspUtilitySavedataSFOParam();
        read(sfoParam); // Offset 128
        icon0FileData = new PspUtilitySavedataFileData();
        read(icon0FileData); // Offset 1412
        icon1FileData = new PspUtilitySavedataFileData();
        read(icon1FileData); // Offset 1428
        pic1FileData = new PspUtilitySavedataFileData();
        read(pic1FileData); // Offset 1444
        snd0FileData = new PspUtilitySavedataFileData();
        read(snd0FileData); // Offset 1460

        newDataAddr = read32(); // Offset 1476
        if (newDataAddr != 0) {
            newData = new PspUtilitySavedataListSaveNewData();
            newData.read(mem, newDataAddr);
        } else {
            newData = null;
        }
        focus = read32(); // Offset 1480
        abortStatus = read32(); // Offset 1484
        msFreeAddr = read32(); // Offset 1488
        msDataAddr = read32(); // Offset 1492
        utilityDataAddr = read32(); // Offset 1496
        read8Array(key); // Offset 1500
        secureVersion = read32(); // Offset 1516
        multiStatus = read32(); // Offset 1520
        idListAddr = read32(); // Offset 1524
        fileListAddr = read32(); // Offset 1528
        sizeAddr = read32(); // Offset 1532
    }

    @Override
    protected void write() {
        write(base);
        setMaxSize(base.totalSizeof());

        write32(mode);
        write32(bind);
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
        write32(abortStatus);
        write32(msFreeAddr);
        write32(msDataAddr);
        write32(utilityDataAddr);
        write8Array(key);
        write32(secureVersion);
        write32(multiStatus);
        write32(idListAddr);
        write32(fileListAddr);
        write32(sizeAddr);
    }

    public String getBasePath() {
        return getBasePath(gameName, saveName);
    }

    public String getBasePath(String saveName) {
    	return getBasePath(gameName, saveName);
    }

    public String getBasePath(String gameName, String saveName) {
        String path = savedataPath + gameName;
        if (saveName != null && !anyFileName.equals(saveName)) {
            path += saveName;
        }
        path += "/";
        return path;
    }

    public String getFileName(String saveName, String fileName) {
    	return getFileName(gameName, saveName, fileName);
    }

    public String getFileName(String gameName, String saveName, String fileName) {
        return getBasePath(gameName, saveName) + fileName;
    }

    private static int computeMemoryStickRequiredSpaceKb(int sizeByte) {
        int sizeKb = Utilities.getSizeKb(sizeByte);
        int sectorSizeKb = MemoryStick.getSectorSizeKb();
        int numberSectors = (sizeKb + sectorSizeKb - 1) / sectorSizeKb;

        return numberSectors * sectorSizeKb;
    }

    public int getRequiredSizeKb() {
        int requiredSpaceKb = 0;
        requiredSpaceKb += MemoryStick.getSectorSizeKb(); // Assume 1 sector for SFO-Params
        // Add the dataSize only if a fileName has been provided
        if (fileName != null && fileName.length() > 0) {
            requiredSpaceKb += computeMemoryStickRequiredSpaceKb(dataSize + 15);
        }
        requiredSpaceKb += computeMemoryStickRequiredSpaceKb(icon0FileData.size);
        requiredSpaceKb += computeMemoryStickRequiredSpaceKb(icon1FileData.size);
        requiredSpaceKb += computeMemoryStickRequiredSpaceKb(pic1FileData.size);
        requiredSpaceKb += computeMemoryStickRequiredSpaceKb(snd0FileData.size);

        return requiredSpaceKb;
    }

    public int getSizeKb(String gameName, String saveName) {
        int sizeKb = 0;

        String path = getBasePath(gameName, saveName);
        StringBuilder localFileName = new StringBuilder();
        IVirtualFileSystem vfs = Modules.IoFileMgrForUserModule.getVirtualFileSystem(path, localFileName);
        if (vfs != null) {
        	String[] fileNames = vfs.ioDopen(localFileName.toString());
        	if (fileNames != null) {
        		for (int i = 0; i < fileNames.length; i++) {
            		SceIoStat stat = new SceIoStat();
            		SceIoDirent dirent = new SceIoDirent(stat, fileNames[i]);
            		int result = vfs.ioDread(localFileName.toString(), dirent);
            		if (result > 0) {
            			sizeKb += Utilities.getSizeKb((int) stat.size);
            		}
        		}
        		vfs.ioDclose(localFileName.toString());
        	}
        }

        return sizeKb;
    }

    private SeekableDataInput getDataInput(String path, String name) {
        SeekableDataInput fileInput = Modules.IoFileMgrForUserModule.getFile(path + name, jpcsp.HLE.modules150.IoFileMgrForUser.PSP_O_RDONLY);

        return fileInput;
    }

    private SeekableRandomFile getDataOutput(String path, String name) {
        SeekableDataInput fileInput = Modules.IoFileMgrForUserModule.getFile(path + name, jpcsp.HLE.modules150.IoFileMgrForUser.PSP_O_RDWR | jpcsp.HLE.modules150.IoFileMgrForUser.PSP_O_CREAT);

        if (fileInput instanceof SeekableRandomFile) {
            return (SeekableRandomFile) fileInput;
        }

        return null;
    }

    public void load(Memory mem) throws IOException {
        String path = getBasePath();

        // Read main data.
        if (CryptoEngine.getSavedataCryptoStatus()) {
            dataSize = loadEncryptedFile(mem, path, fileName, dataBuf, dataBufSize, key, secureVersion);
        } else {
            dataSize = loadFile(mem, path, fileName, dataBuf, dataBufSize);
        }
        
        // Read ICON0.PNG
        safeLoad(mem, icon0FileName, icon0FileData);
        
        // Check and read ICON1.PMF or ICON1.PNG
        if (icon1FileData.buf == 0) {
        	icon1FileData.size = 0;
        } else {
	        String icon1FileName;
	        if (mem.read8(icon1FileData.buf) != 0x89) {
	            icon1FileName = icon1PMFFileName;
	        } else {
	            icon1FileName = icon1PNGFileName; 
	        }
	        safeLoad(mem, icon1FileName, icon1FileData);
        }
        
        // Read PIC1.PNG
        safeLoad(mem, pic1FileName, pic1FileData);
        
        // Read SND0.AT3
        safeLoad(mem, snd0FileName, snd0FileData);
        
        // Read PARAM.SFO
        loadPsf(mem, path, paramSfoFileName, sfoParam);
        
        bind = BIND_IS_OK;
        abortStatus = 0;
    }

    private void safeLoad(Memory mem, String filename, PspUtilitySavedataFileData fileData) throws IOException {
        String path = getBasePath();

        try {
            fileData.size = loadFile(mem, path, filename, fileData.buf, fileData.bufSize);
        } catch (FileNotFoundException e) {
            // ignore
        }
    }

    public void save(Memory mem) throws IOException {
        String path = getBasePath();

        Modules.IoFileMgrForUserModule.mkdirs(path);
        
        // Write main data.
        if (CryptoEngine.getSavedataCryptoStatus()) {
            writeEncryptedFile(mem, path, fileName, dataBuf, dataSize, key, secureVersion);
        } else {
            writeFile(mem, path, fileName, dataBuf, dataSize);
        }
        
        // Write ICON0.PNG
        writeFile(mem, path, icon0FileName, icon0FileData.buf, icon0FileData.size);
        
        // Check and write ICON1.PMF or ICON1.PNG
        IMemoryReader memoryReader = MemoryReader.getMemoryReader(icon1FileData.buf, 1);
        String icon1FileName;
        if ((byte) memoryReader.readNext() != (byte) 0x89) {
            icon1FileName = icon1PMFFileName;
        } else {
            icon1FileName = icon1PNGFileName; 
        }
        writeFile(mem, path, icon1FileName, icon1FileData.buf, icon1FileData.size);
        
        // Write PIC1.PNG
        writeFile(mem, path, pic1FileName, pic1FileData.buf, pic1FileData.size);
        
        // Write SND0.AT3
        writeFile(mem, path, snd0FileName, snd0FileData.buf, snd0FileData.size);
        
        // Write PARAM.SFO
        if (CryptoEngine.getSavedataCryptoStatus()) {
            writeEncryptedPsf(mem, path, paramSfoFileName, sfoParam, fileName, dataSize, key, secureVersion);
        } else {
            writePsf(mem, path, paramSfoFileName, sfoParam);
        }
    }

    private int loadFile(Memory mem, String path, String name, int address, int maxLength) throws IOException {
        if (name == null || name.length() <= 0 || address == 0) {
            return 0;
        }

        SeekableDataInput fileInput = getDataInput(path, name);
        if (fileInput == null) {
            throw new FileNotFoundException("File not found '" + path + "' '" + name + "'");
        }

        // Some applications set dataBufSize to -1 on purpose. The reason behind this
        // is still unknown, but, for these cases, ignore maxLength.
        int fileSize = (int) fileInput.length();
        if ((fileSize > maxLength) && (maxLength > 0)) {
            fileSize = maxLength;
        }

        Utilities.readFully(fileInput, address, fileSize);
        fileInput.close();

        return fileSize;
    }

    private void writeEncryptedFile(Memory mem, String path, String name, int address, int length, byte[] key, int mode) throws IOException {
        if (name == null || name.length() <= 0 || address == 0) {
            return;
        }

        SeekableRandomFile fileOutput = getDataOutput(path, name);
        CryptoEngine crypto = new CryptoEngine();
        if (fileOutput == null) {
            return;
        }

        byte[] inBuf = new byte[length];

        IMemoryReader memoryReader = MemoryReader.getMemoryReader(address, 1);
        for (int i = 0; i < length; i++) {
            inBuf[i] = (byte) memoryReader.readNext();
        }

        byte[] outBuf = crypto.EncryptSavedata(inBuf, length, key, mode);

        fileOutput.write(outBuf, 0, outBuf.length);
        fileOutput.close();
    }

    private int loadEncryptedFile(Memory mem, String path, String name, int address, int maxLength, byte[] key, int mode) throws IOException {
        if (name == null || name.length() <= 0 || address == 0) {
            return 0;
        }

        SeekableDataInput fileInput = getDataInput(path, name);
        CryptoEngine crypto = new CryptoEngine();
        if (fileInput == null) {
            throw new FileNotFoundException("File not found '" + path + "' '" + name + "'");
        }

        int fileSize = (int) fileInput.length();
        byte[] inBuf = new byte[fileSize];
        fileInput.readFully(inBuf);

        byte[] outBuf = crypto.DecryptSavedata(inBuf, fileSize, key, mode);

        IMemoryWriter memoryWriter = MemoryWriter.getMemoryWriter(address, 1);
        int length = Math.min(outBuf.length, maxLength);
        for (int i = 0; i < length; i++) {
            memoryWriter.writeNext(outBuf[i]);
        }
        memoryWriter.flush();

        return length;
    }

    private void writeFile(Memory mem, String path, String name, int address, int length) throws IOException {
        if (name == null || name.length() <= 0 || address == 0) {
            return;
        }

        SeekableRandomFile fileOutput = getDataOutput(path, name);
        if (fileOutput == null) {
            return;
        }

        Utilities.write(fileOutput, address, length);
        fileOutput.close();
    }

    private void loadPsf(Memory mem, String path, String name, PspUtilitySavedataSFOParam sfoParam) throws IOException {
        SeekableDataInput fileInput = getDataInput(path, name);
        if (fileInput != null) {
            byte[] buffer = new byte[(int) fileInput.length()];
            fileInput.readFully(buffer);
            fileInput.close();

            PSF psf = new PSF();
            psf.read(ByteBuffer.wrap(buffer));

            sfoParam.parentalLevel = psf.getNumeric("PARENTAL_LEVEL");
            sfoParam.title = psf.getString("TITLE");
            sfoParam.detail = psf.getString("SAVEDATA_DETAIL");
            sfoParam.savedataTitle = psf.getString("SAVEDATA_TITLE");
        }
    }

    private void writePsf(Memory mem, String path, String name, PspUtilitySavedataSFOParam sfoParam) throws IOException {
        SeekableRandomFile fileOutput = getDataOutput(path, name);
        if (fileOutput == null) {
            return;
        }

        PSF psf = new PSF();
        psf.put("PARENTAL_LEVEL", sfoParam.parentalLevel);
        psf.put("TITLE", sfoParam.title, 128);
        psf.put("SAVEDATA_DETAIL", sfoParam.detail, 1024);
        psf.put("SAVEDATA_TITLE", sfoParam.savedataTitle, 128);

        psf.write(fileOutput);
        fileOutput.close();
    }

    private void writeEncryptedPsf(Memory mem, String path, String psfName, PspUtilitySavedataSFOParam sfoParam, String dataName, int dataLength, byte[] key, int mode) throws IOException {
        SeekableRandomFile psfOutput = getDataOutput(path, psfName);
        SeekableRandomFile dataOutput = getDataOutput(path, dataName);
        if ((psfOutput == null) || (dataOutput == null)) {
            return;
        }

        CryptoEngine crypto = new CryptoEngine();
        byte[] dataBuffer = new byte[dataLength];
        dataOutput.readFully(dataBuffer);

        PSF psf = new PSF();
        psf.put("CATEGORY", "MS", 4);
        psf.put("PARENTAL_LEVEL", sfoParam.parentalLevel);     
        psf.put("SAVEDATA_DETAIL", sfoParam.detail, 1024);
        psf.put("SAVEDATA_DIRECTORY", gameName + saveName, 64);
        crypto.UpdateSavedataHashes(psf, dataBuffer, dataLength, key, dataName, mode);
        psf.put("SAVEDATA_TITLE", sfoParam.savedataTitle, 128);
        psf.put("TITLE", sfoParam.title, 128);

        psf.write(psfOutput);
        psfOutput.close();
    }

    public boolean test(Memory mem) throws IOException {
        String path = getBasePath();

        boolean result = testFile(mem, path, fileName);

        return result;
    }

    private boolean testFile(Memory mem, String path, String name) throws IOException {
        if (name == null || name.length() <= 0) {
            return false;
        }

        SeekableDataInput fileInput = getDataInput(path, name);
        if (fileInput == null) {
            throw new FileNotFoundException("File not found '" + path + "' '" + name + "'");
        }

        fileInput.close();

        return true;
    }

    public String getAnySaveName(String gameName, String saveName) {
        // NULL can also be sent in saveName (seen in MODE_SIZES).
        // It means any save from the current game, since all saves share a common
        // save data file.
        if (saveName == null || saveName.length() <= 0 || anyFileName.equals(saveName)) {
            File f = new File(savedataFilePath);
            String[] entries = f.list();
            if (entries != null) {
	            for (int i = 0; i < f.list().length; i++) {
	                if (entries[i].startsWith(gameName)) {
	                    saveName = entries[i].replace(gameName, "");
	                    break;
	                }
	            }
            }
        }

        return saveName;
    }

    public boolean isDirectoryPresent(String gameName, String saveName) {
    	saveName = getAnySaveName(gameName, saveName);
        String path = getBasePath(gameName, saveName);
    	SceIoStat stat = Modules.IoFileMgrForUserModule.statFile(path);
    	if (stat != null && (stat.attr & 0x20) == 0) {
    		return true;
    	}

    	return false;
    }

    public boolean isPresent(String gameName, String saveName) {
    	saveName = getAnySaveName(gameName, saveName);

        // When NULL is sent in fileName, it means any file inside the savedata folder.
        if (fileName == null || fileName.length() <= 0) {
            File f = new File(savedataFilePath + gameName + saveName);
            if (f.list() == null) {
                return false;
            }
            return true;
        }

        String path = getBasePath(gameName, saveName);
        try {
            SeekableDataInput fileInput = getDataInput(path, fileName);
            if (fileInput != null) {
                fileInput.close();
                return true;
            }
        } catch (IOException e) {
        }

        return false;
    }

    public boolean isPresent() {
        return isPresent(gameName, saveName);
    }

    public boolean isGameDirectoryPresent() {
    	String path = getBasePath();
    	SceIoStat gameDirectoryStat = Modules.IoFileMgrForUserModule.statFile(path);

    	return gameDirectoryStat != null;
    }

    public long getTimestamp(String gameName, String saveName) {
        String sfoFileName = getFileName(gameName, saveName, paramSfoFileName);
        SceIoStat sfoStat = Modules.IoFileMgrForUserModule.statFile(sfoFileName);
        if (sfoStat != null) {
            Calendar cal = Calendar.getInstance();
            ScePspDateTime pspTime = sfoStat.mtime;
            cal.set(pspTime.year, pspTime.month, pspTime.day, pspTime.hour, pspTime.minute, pspTime.second);
            return cal.getTimeInMillis();
        }

        return 0;
    }

    public Calendar getSavedTime() {
    	return getSavedTime(saveName);
    }

    public Calendar getSavedTime(String saveName) {
		String sfoFileName = getFileName(saveName, SceUtilitySavedataParam.paramSfoFileName);
        SceIoStat sfoStat = Modules.IoFileMgrForUserModule.statFile(sfoFileName);
        ScePspDateTime pspTime = sfoStat.mtime;

		Calendar savedTime = Calendar.getInstance();
        // pspTime.month has a value in range [1..12], Calendar requires a value in range [0..11]
        savedTime.set(pspTime.year, pspTime.month - 1, pspTime.day, pspTime.hour, pspTime.minute, pspTime.second);

        return savedTime;
    }

    @Override
    public int sizeof() {
        return base.totalSizeof();
    }

    public String getModeName() {
        if (mode < 0 || mode >= modeNames.length) {
            return String.format("UNKNOWN_MODE%d", mode);
        }
        return modeNames[mode];
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append(String.format("Address 0x%08X, mode=%d(%s), gameName=%s, saveName=%s, fileName=%s, secureVersion=%d", getBaseAddress(), mode, getModeName(), gameName, saveName, fileName, secureVersion));
        for (int i = 0; saveNameList != null && i < saveNameList.length; i++) {
            if (i == 0) {
                s.append(", saveNameList=[");
            } else {
                s.append(", ");
            }
            s.append(saveNameList[i]);
            if (i == saveNameList.length - 1) {
                s.append("]");
            }
        }

        return s.toString();
    }
}