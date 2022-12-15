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
package jpcsp.HLE.modules;

import static jpcsp.HLE.Modules.IoFileMgrForUserModule;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_NPDRM_INVALID_FILE;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_NPDRM_NO_FILENAME_MATCH;
import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_NPDRM_NO_K_LICENSEE_SET;
import static jpcsp.HLE.modules.IoFileMgrForUser.PSP_O_RDONLY;
import static jpcsp.util.Utilities.notHasBit;
import static jpcsp.util.Utilities.readUnaligned32;

import java.io.IOException;
import java.util.Arrays;

import jpcsp.HLE.BufferInfo;
import jpcsp.HLE.BufferInfo.LengthInfo;
import jpcsp.HLE.CanBeNull;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLELogging;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.PspString;
import jpcsp.HLE.TPointer;
import jpcsp.crypto.CryptoEngine;
import jpcsp.HLE.BufferInfo.Usage;
import jpcsp.HLE.VFS.IVirtualFile;
import jpcsp.HLE.VFS.SeekableDataInputVirtualFile;
import jpcsp.HLE.VFS.crypto.EDATVirtualFile;
import jpcsp.HLE.VFS.crypto.PGDVirtualFile;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.Modules;
import jpcsp.HLE.modules.IoFileMgrForUser.IoInfo;
import jpcsp.settings.AbstractBoolSettingsListener;

import org.apache.log4j.Logger;

public class scePspNpDrm_user extends HLEModule {
    public static Logger log = Modules.getLogger("scePspNpDrm_user");

    public static final int PSP_NPDRM_LICENSEE_KEY_LENGTH = 0x10;
    private byte licenseeKey[] = new byte[PSP_NPDRM_LICENSEE_KEY_LENGTH];
    private boolean isLicenseeKeySet = false;
    private boolean isDLCDecryptionDisabled;

    private class DisableDLCSettingsListerner extends AbstractBoolSettingsListener {
        @Override
        protected void settingsValueChanged(boolean value) {
        	setDLCDecryptionDisabled(value);
        }
    }

    @Override
    public void start() {
        setSettingsListener("emu.disableDLC", new DisableDLCSettingsListerner());
        super.start();
    }

	public boolean isLicenseeKeySet() {
		return isLicenseeKeySet;
	}

	public void setLicenseeKeySet(boolean isLicenseeKeySet) {
		this.isLicenseeKeySet = isLicenseeKeySet;
	}

	public boolean isDLCDecryptionDisabled() {
		return isDLCDecryptionDisabled;
	}

	public boolean isDLCDecryptionEnabled() {
		return !isDLCDecryptionDisabled;
	}

	public void setDLCDecryptionDisabled(boolean isDLCDecryptionDisabled) {
		this.isDLCDecryptionDisabled = isDLCDecryptionDisabled;
	}

    public byte[] getLicenseeKey() {
    	return licenseeKey;
    }

    public static boolean isEncrypted(byte[] header) {
    	// "\0PSPEDAT"
    	if (readUnaligned32(header, 0) == 0x50535000 && readUnaligned32(header, 4) == 0x54414445) {
    		return true;
    	}

    	return false;
    }

    private boolean isEncrypted(IVirtualFile vFile) {
    	if (vFile == null) {
    		return false;
    	}

    	long position = vFile.getPosition();
    	byte[] header = new byte[8];
    	int length = vFile.ioRead(header, 0, header.length);
    	vFile.ioLseek(position);

    	if (length != header.length) {
    		return false;
    	}

    	return isEncrypted(header);
    }

    @HLELogging(level = "info")
    @HLEFunction(nid = 0xA1336091, version = 150, checkInsideInterrupt = true)
    public int sceNpDrmSetLicenseeKey(@BufferInfo(lengthInfo = LengthInfo.fixedLength, length = PSP_NPDRM_LICENSEE_KEY_LENGTH, usage = Usage.in) TPointer licenseeKeyAddr) {
    	licenseeKeyAddr.getArray8(licenseeKey);
        setLicenseeKeySet(true);

        return 0;
    }

    @HLEFunction(nid = 0x9B745542, version = 150, checkInsideInterrupt = true)
    public int sceNpDrmClearLicenseeKey() {
        Arrays.fill(licenseeKey, (byte) 0);
        setLicenseeKeySet(false);

        return 0;
    }

    @HLEFunction(nid = 0x275987D1, version = 150, checkInsideInterrupt = true)
    public int sceNpDrmRenameCheck(PspString fileName) {
        CryptoEngine crypto = new CryptoEngine();
        int result = 0;

        if (!isLicenseeKeySet()) {
            result = ERROR_NPDRM_NO_K_LICENSEE_SET;
        } else {
        	IVirtualFile vFile = IoFileMgrForUserModule.getVirtualFile(fileName.getString(), PSP_O_RDONLY, 0777);
        	if (vFile == null) {
                result = ERROR_NPDRM_INVALID_FILE;
                if (log.isDebugEnabled()) {
                    log.debug(String.format("sceNpDrmRenameCheck: file '%s' not found", fileName.getString()));
                }
        	} else {
                String fName = fileName.getString();
                int lastFileNamePart = fName.lastIndexOf('/');
                if (lastFileNamePart > 0) {
                	fName = fName.substring(lastFileNamePart + 1);
                }

                // Setup the buffers.
                byte[] inBuf = new byte[0x80];
                byte[] srcData = new byte[0x30];
                byte[] srcHash = new byte[0x10];

                int length = vFile.ioRead(inBuf, 0, inBuf.length);
                vFile.ioClose();

                if (length == inBuf.length && isEncrypted(inBuf)) {
                    // The data seed is stored at offset 0x10 of the PSPEDAT header.
                    System.arraycopy(inBuf, 0x10, srcData, 0, 0x30);

                    // The hash to compare is stored at offset 0x40 of the PSPEDAT header.
                    System.arraycopy(inBuf, 0x40, srcHash, 0, 0x10);

                    // If the CryptoEngine fails to find a match, then the file has been renamed.
                    if (!crypto.getPGDEngine().CheckEDATRenameKey(fName.getBytes(), srcHash, srcData)) {
                        if (isDLCDecryptionEnabled()) {
                            result = ERROR_NPDRM_NO_FILENAME_MATCH;
                            log.warn("sceNpDrmRenameCheck: the file has been renamed");
                        }
                    }
                } else {
                	// File is not encrypted
                	result = 0;
                }
        	}
        }

        return result;
    }

    @HLELogging(level = "info")
    @HLEFunction(nid = 0x08D98894, version = 150, checkInsideInterrupt = true)
    public int sceNpDrmEdataSetupKey(int edataFd) {
        // Return an error if the key has not been set.
        // Note: An empty key is valid, as long as it was set with sceNpDrmSetLicenseeKey.
        if (!isLicenseeKeySet()) {
            return SceKernelErrors.ERROR_NPDRM_NO_K_LICENSEE_SET;
        }

        IoInfo info = Modules.IoFileMgrForUserModule.getFileIoInfo(edataFd);
        if (info == null) {
            return -1;
        }

        int result = 0;

    	IVirtualFile vFile = info.vFile;
    	if (vFile == null && info.readOnlyFile != null) {
    		vFile = new SeekableDataInputVirtualFile(info.readOnlyFile);
    	}

    	if (isEncrypted(vFile)) {
    		PGDVirtualFile pgdFile = new EDATVirtualFile(vFile);
    		if (pgdFile.isValid()) {
    			info.vFile = pgdFile;
    		}
        }

        return result;
    }

    @HLEFunction(nid = 0x219EF5CC, version = 150, checkInsideInterrupt = true)
    public int sceNpDrmEdataGetDataSize(int edataFd) {
        IoInfo info = Modules.IoFileMgrForUserModule.getFileIoInfo(edataFd);
        int size = 0;
        if (info != null) {
        	if (info.vFile != null) {
        		size = (int) info.vFile.length();
        	} else if (info.readOnlyFile != null) {
                try {
                    size = (int) info.readOnlyFile.length();
                } catch (IOException e) {
                    log.error("sceNpDrmEdataGetDataSize", e);
                }
            }
        }

        return size;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x2BAA4294, version = 150, checkInsideInterrupt = true)
    public int sceNpDrmOpen(PspString name, int flags, int permissions) {
        if (!isLicenseeKeySet()) {
            return SceKernelErrors.ERROR_NPDRM_NO_K_LICENSEE_SET;
        }
        // Open the file with flags ORed with PSP_O_FGAMEDATA and send it to the IoFileMgr.
        int fd = Modules.IoFileMgrForUserModule.hleIoOpen(name, flags | 0x40000000, permissions, true);
        return sceNpDrmEdataSetupKey(fd);
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xEBB198ED, version = 150)
    public int sceNpDrmDecActivation(TPointer unknown1, TPointer unknown2) {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x17E3F4BB, version = 150)
    public int sceNpDrmVerifyAct(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=4152, usage=Usage.in) TPointer actDatAddr) {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x37B9B10D, version = 150)
    public int sceNpDrmVerifyRif(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=152, usage=Usage.in) TPointer rifAddr) {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x4478C033, version = 150)
    public int sceNpDrmVerifyRifById(int id) {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x9A34AC9F, version = 150)
    public int sceNpDrmCheckRifTimeLimit(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=152, usage=Usage.in) TPointer rifAddr) {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xD19E6E28, version = 150)
    public int sceNpDrmCheckRifTimeLimitById(int id) {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x0F9547E6, version = 150)
    public int sceNpDrmGetVersionKey(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=32, usage=Usage.out) TPointer keyAddr, @CanBeNull @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=4152, usage=Usage.in) TPointer actDatAddr, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=152, usage=Usage.in) TPointer rifAddr, int type) {
    	byte[] ebootKey = Modules.scePopsManModule.getEbootKey();
    	if (ebootKey != null) {
    		keyAddr.setArray(ebootKey);
    	}
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x5667B7B9, version = 150)
    public int sceNpDrmGetContentKey(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=32, usage=Usage.out) TPointer keyAddr, @CanBeNull @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=4152, usage=Usage.in) TPointer actDatAddr, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=152, usage=Usage.in) TPointer rifAddr) {
        return sceNpDrmGetVersionKey(keyAddr, actDatAddr, rifAddr, 0);
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x2D88879A, version = 150)
    public int sceNpDrmSetDebugMode(int debugMode) {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x3554E328, version = 150)
    public int sceNpDrmSetRifDevice(int rifDevice) {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xD36B4E6D, version = 150)
    public int sceNpDrmGetModuleKey(int fileId, @BufferInfo(lengthInfo = LengthInfo.fixedLength, length = 16, usage = Usage.out) TPointer key) {
        return 0;
    }

    @HLEFunction(nid = 0x00AD67F8, version = 150)
    public int sceNpDrmGetFixedKey(@BufferInfo(lengthInfo = LengthInfo.fixedLength, length = 16, usage = Usage.out) TPointer key, PspString data, int type) {
    	if (notHasBit(type, 24)) {
    		return ERROR_NPDRM_INVALID_FILE;
    	}

    	byte[] hash = new byte[0x10];
    	byte[] dataBuffer = new byte[0x30];
    	data.getPointer().getArray8(0, dataBuffer, 0, data.getString().length());

    	hash = new CryptoEngine().getDRMEngine().hleNpDrmGetFixedKey(hash, dataBuffer, type & 0xFF);
    	key.setArray(hash);

    	return 0;
    }
}
