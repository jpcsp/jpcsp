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
package jpcsp.HLE.modules150;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import jpcsp.GeneralJpcspException;
import jpcsp.HLE.CanBeNull;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLELogging;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.PspString;
import jpcsp.HLE.TPointer;
import jpcsp.Emulator;
import jpcsp.Loader;
import jpcsp.connector.PGDFileConnector;
import jpcsp.crypto.CryptoEngine;
import jpcsp.filesystems.SeekableDataInput;
import jpcsp.filesystems.SeekableRandomFile;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.kernel.types.SceKernelLMOption;
import jpcsp.HLE.kernel.types.SceModule;
import jpcsp.HLE.Modules;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.HLE.modules150.IoFileMgrForUser.IoInfo;

import org.apache.log4j.Logger;

@HLELogging
public class scePspNpDrm_user extends HLEModule {
    public static Logger log = Modules.getLogger("scePspNpDrm_user");

    @Override
    public String getName() {
        return "scePspNpDrm_user";
    }

    public static final int PSP_NPDRM_KEY_LENGHT = 0x10;
    private byte npDrmKey[] = new byte[PSP_NPDRM_KEY_LENGHT];
    private PGDFileConnector edatFileConnector;

    protected boolean isEmptyDrmKey() {
    	for (int i = 0; i < npDrmKey.length; i++) {
    		if (npDrmKey[i] != 0) {
    			return false;
    		}
    	}

    	return true;
    }

    @HLEFunction(nid = 0xA1336091, version = 150, checkInsideInterrupt = true)
    public int sceNpDrmSetLicenseeKey(TPointer npDrmKeyAddr) {
        StringBuilder key = new StringBuilder();
        for (int i = 0; i < PSP_NPDRM_KEY_LENGHT; i++) {
            npDrmKey[i] = (byte) npDrmKeyAddr.getValue8(i);
            key.append(String.format("%02X", npDrmKey[i] & 0xFF));
        }
        log.info(String.format("NPDRM Encryption key detected: 0x%s", key.toString()));

        return 0;
    }

    @HLEFunction(nid = 0x9B745542, version = 150, checkInsideInterrupt = true)
    public int sceNpDrmClearLicenseeKey() {
    	Arrays.fill(npDrmKey, (byte) 0);

    	return 0;
    }

    @HLELogging(level="warn")
    @HLEFunction(nid = 0x275987D1, version = 150, checkInsideInterrupt = true)
    public int sceNpDrmRenameCheck(PspString fileName) {
        CryptoEngine crypto = new CryptoEngine(); 
		boolean renamed = false;
		int result = 0;

        try {
            String pcfilename = Modules.IoFileMgrForUserModule.getDeviceFilePath(fileName.getString());
            SeekableRandomFile file = new SeekableRandomFile(pcfilename, "rw");

            String[] name = pcfilename.split("/");
            String fName = "";
            for (int i = 0; i < name.length; i++) {
                if (name[i].contains("EDAT")) {
                    fName = name[i].toLowerCase();
                }
            }

            // Setup the buffers.
            byte[] inBuf = new byte[0x80];
            byte[] dataBuf = new byte[0x30];
            byte[] nameHashBuf = new byte[0x10];

            // Read the encrypted PSPEDATA header.
            file.readFully(inBuf);
            file.close();

            // Generate a new name hash for this file and compare with the one stored in it's header.
            System.arraycopy(inBuf, 0x10, dataBuf, 0, 0x30);
            System.arraycopy(inBuf, 0x40, nameHashBuf, 0, 0x10);

            // If the CryptoEngine fails to find a match, then the file has been renamed.
            if (crypto.CheckEDATANameKey(nameHashBuf, dataBuf, fName.getBytes(), fName.getBytes().length) != 0) {
                renamed = true;
            }

            if (log.isDebugEnabled()) {
            	log.debug(String.format("sceNpDrmRenameCheck renamed=%b", renamed));
            }
        } catch (FileNotFoundException e) {
        	result = SceKernelErrors.ERROR_ERRNO_FILE_NOT_FOUND;
        	if (log.isDebugEnabled()) {
        		log.debug(String.format("sceNpDrmRenameCheck: file '%s' not found: %s", fileName.getString(), e.toString()));
        	}
        } catch (Exception e) {
        	log.error("sceNpDrmRenameCheck", e);
        }

        return result;  // Faking.
    }

    @HLELogging(level="warn")
    @HLEFunction(nid = 0x08D98894, version = 150, checkInsideInterrupt = true)
    public int sceNpDrmEdataSetupKey(int edataFd) {
    	// Nothing to do if the DRM Key is all 0's
    	if (isEmptyDrmKey()) {
    		return 0;
    	}

    	IoInfo info = Modules.IoFileMgrForUserModule.getFileIoInfo(edataFd);    
    	if (info == null) {
    		return 0;
    	}
        CryptoEngine crypto = new CryptoEngine();
        
        if (edatFileConnector == null) {
            edatFileConnector = new PGDFileConnector();
        }
        
        // Check for an already decrypted file.
        SeekableDataInput decInput = edatFileConnector.loadDecryptedPGDFile(info.filename);
        
        if (decInput != null) {
            info.readOnlyFile = decInput;
        } else {
            // Try to decrypt the data with the Crypto Engine.
            try {
                // Generate the necessary directories and files.
                String edataPath = edatFileConnector.getBaseDirectory(edatFileConnector.id);
                new File(edataPath).mkdirs();
                String decFileName = edatFileConnector.getCompleteFileName(PGDFileConnector.decryptedFileName);
                SeekableRandomFile decFile = new SeekableRandomFile(decFileName, "rw");

                // Maximum 16-byte aligned block size to use during stream read/write.
                int maxAlignedChunkSize = 0x4EF0;

                // PGD hash header size.
                int pgdHeaderSize = 0xA0;
                
                // PGD data offset in EDAT file.
                int edatPGDOffset = 0x90;

                // Setup the buffers.
                byte[] inBuf = new byte[maxAlignedChunkSize + pgdHeaderSize];
                byte[] outBuf = new byte[maxAlignedChunkSize + 0x10];
                byte[] headerBuf = new byte[0x30 + 0x10];
                byte[] hashBuf = new byte[0x10];
   
                // Read the encrypted PGD header.
                info.readOnlyFile.readFully(inBuf, 0, pgdHeaderSize);
                
                // Generate the decryption key for this file.
                byte[] dBuf = new byte[0x30];
                byte[] kBuf = new byte[0x10];
                System.arraycopy(inBuf, 0, dBuf, 0, 0x30);
                System.arraycopy(inBuf, 0xA0, kBuf, 0, 0x10);
                byte[] newKey = crypto.MakeEDATAFixedKey(dBuf, kBuf);

                // Decrypt 0x30 bytes at offset edatPGDOffset + 0x30 to expose the first header.
                System.arraycopy(inBuf, edatPGDOffset + 0x10, headerBuf, 0, 0x10);
                System.arraycopy(inBuf, edatPGDOffset + 0x30, headerBuf, 0x10, 0x30);
                byte headerBufDec[] = crypto.DecryptPGD(headerBuf, 0x30 + 0x10, newKey);
              
                // Extract the decrypting parameters.
                System.arraycopy(headerBufDec, 0, hashBuf, 0, 0x10);
                int dataSize = (headerBufDec[0x14] & 0xFF) | ((headerBufDec[0x15] & 0xFF) << 8) |
                        ((headerBufDec[0x16] & 0xFF) << 16) | ((headerBufDec[0x17] & 0xFF) << 24);
                int chunkSize = (headerBufDec[0x18] & 0xFF) | ((headerBufDec[0x19] & 0xFF) << 8) |
                        ((headerBufDec[0x1A] & 0xFF) << 16) | ((headerBufDec[0x1B] & 0xFF) << 24);
                int hashOffset = (headerBufDec[0x1C] & 0xFF) | ((headerBufDec[0x1D] & 0xFF) << 8) |
                        ((headerBufDec[0x1E] & 0xFF) << 16) | ((headerBufDec[0x1F] & 0xFF) << 24);
                if (log.isDebugEnabled()) {
                    log.debug(String.format("PGD dataSize=%d, chunkSize=%d, hashOffset=%d", dataSize, chunkSize, hashOffset));
                }

                // Write the newly extracted hash at the top of the output buffer,
                // locate the data hash at hashOffset and start decrypting until
                // dataSize is reached.

                // If the data is smaller than maxAlignedChunkSize, decrypt it right away.
                if (dataSize <= maxAlignedChunkSize) {
                    info.readOnlyFile.seek(hashOffset);
                    info.readOnlyFile.readFully(inBuf, 0xA0, dataSize);
                    System.arraycopy(hashBuf, 0, outBuf, 0, 0x10);
                    System.arraycopy(inBuf, 0xA0, outBuf, 0x10, dataSize);
                    decFile.write(crypto.DecryptPGD(outBuf, dataSize + 0x10, npDrmKey));
                } else {
                    // Read and decrypt the first chunk of data.
                    info.readOnlyFile.seek(hashOffset);
                    info.readOnlyFile.readFully(inBuf, 0xA0, maxAlignedChunkSize);
                    System.arraycopy(hashBuf, 0, outBuf, 0, 0x10);
                    System.arraycopy(inBuf, 0xA0, outBuf, 0x10, maxAlignedChunkSize);
                    decFile.write(crypto.DecryptPGD(outBuf, maxAlignedChunkSize + 0x10, npDrmKey));

                    // Keep reading and decrypting data by updating the PGD cipher.
                    for (int i = 0; i < dataSize; i += maxAlignedChunkSize) {
                        info.readOnlyFile.readFully(inBuf, 0xA0, maxAlignedChunkSize);
                        System.arraycopy(hashBuf, 0, outBuf, 0, 0x10);
                        System.arraycopy(inBuf, 0xA0, outBuf, 0x10, maxAlignedChunkSize);
                        decFile.write(crypto.UpdatePGDCipher(outBuf, maxAlignedChunkSize + 0x10));
                    }
                }

                // Finish the PGD cipher operations, set the real file length and close it.
                crypto.FinishPGDCipher();
                decFile.setLength(dataSize);
                decFile.close();
            } catch (Exception e) {
                // Ignore.
            }

            try {
                info.readOnlyFile.seek(info.position);
            } catch (Exception e) {
                // Ignore.
            }

            // Load the manually decrypted file generated just now.
            info.readOnlyFile = edatFileConnector.loadDecryptedPGDFile(info.filename);
        }

        return 0;  // Faking.
    }

    @HLEFunction(nid = 0x219EF5CC, version = 150, checkInsideInterrupt = true)
    public int sceNpDrmEdataGetDataSize(int edataFd) {
        IoInfo info = Modules.IoFileMgrForUserModule.getFileIoInfo(edataFd); 
        int size = 0;
        if (info != null) {
	        try {
	            size = (int) info.readOnlyFile.length();
	        } catch (IOException e) {
	        	log.error("sceNpDrmEdataGetDataSize", e);
	        }
        }

        return size;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x2BAA4294, version = 150, checkInsideInterrupt = true)
    public int sceNpDrmOpen(PspString name, int flags, int permissions) {
        return 0;
    }

    @HLEFunction(nid = 0xC618D0B1, version = 150, checkInsideInterrupt = true)
    public int sceKernelLoadModuleNpDrm(PspString path, int flags, @CanBeNull TPointer optionAddr) {
        SceKernelLMOption lmOption = null;
        if (optionAddr.isNotNull()) {
            lmOption = new SceKernelLMOption();
            lmOption.read(optionAddr);
            if (log.isInfoEnabled()) {
            	log.info(String.format("sceKernelLoadModuleNpDrm partition=%d, position=%d", lmOption.mpidText, lmOption.position));
            }
        }

        return Modules.ModuleMgrForUserModule.hleKernelLoadModule(path.getString(), flags, 0, false);
    }

    @HLEFunction(nid = 0xAA5FC85B, version = 150, checkInsideInterrupt = true)
    public int sceKernelLoadExecNpDrm(PspString fileName, @CanBeNull TPointer optionAddr) {
        // Flush system memory to mimic a real PSP reset.
        Modules.SysMemUserForUserModule.reset();

        if (optionAddr.isNotNull()) {
            int optSize = optionAddr.getValue32(0);  // Size of the option struct.
            int argSize = optionAddr.getValue32(4);  // Number of args (strings).
            int argAddr = optionAddr.getValue32(8);  // Pointer to a list of strings.
            int keyAddr = optionAddr.getValue32(12); // Pointer to an encryption key (may not be used).

            if (log.isDebugEnabled()) {
            	log.debug(String.format("sceKernelLoadExecNpDrm (params: optSize=%d, argSize=%d, argAddr=0x%08X, keyAddr=0x%08X)", optSize, argSize, argAddr, keyAddr));
            }
        }

        int result;
        try {
            SeekableDataInput moduleInput = Modules.IoFileMgrForUserModule.getFile(fileName.getString(), IoFileMgrForUser.PSP_O_RDONLY);
            if (moduleInput != null) {
                byte[] moduleBytes = new byte[(int) moduleInput.length()];
                moduleInput.readFully(moduleBytes);
                moduleInput.close();
                ByteBuffer moduleBuffer = ByteBuffer.wrap(moduleBytes);

                SceModule module = Emulator.getInstance().load(fileName.getString(), moduleBuffer, true);
                Emulator.getClock().resume();

                if ((module.fileFormat & Loader.FORMAT_ELF) == Loader.FORMAT_ELF) {
                    result = 0;
                } else {
                    log.warn("sceKernelLoadExecNpDrm - failed, target is not an ELF");
                    result = SceKernelErrors.ERROR_KERNEL_ILLEGAL_LOADEXEC_FILENAME;
                }
            } else {
                result = SceKernelErrors.ERROR_KERNEL_PROHIBIT_LOADEXEC_DEVICE;
            }
        } catch (GeneralJpcspException e) {
            log.error("sceKernelLoadExecNpDrm", e);
            result = SceKernelErrors.ERROR_KERNEL_PROHIBIT_LOADEXEC_DEVICE;
        } catch (IOException e) {
            log.error(String.format("sceKernelLoadExecNpDrm - Error while loading module '%s'", fileName), e);
            result = SceKernelErrors.ERROR_KERNEL_PROHIBIT_LOADEXEC_DEVICE;
        }

        return result;
    }
}