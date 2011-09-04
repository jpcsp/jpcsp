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
import java.io.IOException;
import java.nio.ByteBuffer;

import jpcsp.GeneralJpcspException;
import jpcsp.HLE.HLEFunction;
import jpcsp.Emulator;
import jpcsp.Loader;
import jpcsp.Memory;
import jpcsp.Processor;
import jpcsp.Allegrex.CpuState;
import jpcsp.connector.PGDFileConnector;
import jpcsp.crypto.CryptoEngine;
import jpcsp.filesystems.SeekableDataInput;
import jpcsp.filesystems.SeekableRandomFile;
import jpcsp.HLE.kernel.managers.IntrManager;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.kernel.types.SceKernelLMOption;
import jpcsp.HLE.kernel.types.SceModule;
import jpcsp.HLE.Modules;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.HLE.modules150.IoFileMgrForUser.IoInfo;
import jpcsp.util.Utilities;

import org.apache.log4j.Logger;

public class scePspNpDrm_user extends HLEModule {

    protected static Logger log = Modules.getLogger("scePspNpDrm_user");

    @Override
    public String getName() {
        return "scePspNpDrm_user";
    }

    public static final int PSP_NPDRM_KEY_LENGHT = 0x10;
    private byte npDrmKey[] = new byte[PSP_NPDRM_KEY_LENGHT];
    private PGDFileConnector edatFileConnector;

    @HLEFunction(nid = 0xA1336091, version = 150)
    public void sceNpDrmSetLicenseeKey(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Memory.getInstance();

        int npDrmKeyAddr = cpu.gpr[4];

        if (Modules.log.isDebugEnabled()) {
            log.debug("sceNpDrmSetLicenseeKey (npDrmKeyAddr=0x" + Integer.toHexString(npDrmKeyAddr) + ")");
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (Memory.isAddressGood(npDrmKeyAddr)) {
            String key = "";
            for(int i = 0; i < PSP_NPDRM_KEY_LENGHT; i++) {
                npDrmKey[i] = (byte)mem.read8(npDrmKeyAddr + i);
                key += Integer.toHexString(npDrmKey[i] & 0xFF);
            }
            log.info("NPDRM Encryption key detected: 0x" + key);
        }
        cpu.gpr[2] = 0;
    }

    @HLEFunction(nid = 0x9B745542, version = 150)
    public void sceNpDrmClearLicenseeKey(Processor processor) {
        CpuState cpu = processor.cpu;

        if (Modules.log.isDebugEnabled()) {
            log.debug("sceNpDrmClearLicenseeKey");
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        for(int i = 0; i < PSP_NPDRM_KEY_LENGHT; i++) {
            npDrmKey[i] = 0;
        }
        cpu.gpr[2] = 0;
    }

    @HLEFunction(nid = 0x275987D1, version = 150)
    public void sceNpDrmRenameCheck(Processor processor) {
        CpuState cpu = processor.cpu;
        
        int nameAddr = cpu.gpr[4];

        log.warn("PARTIAL: sceNpDrmRenameCheck (nameAddr=0x" + Integer.toHexString(nameAddr) + ")");

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        String fileName = Utilities.readStringZ(nameAddr);        
        CryptoEngine crypto = new CryptoEngine(); 
        boolean renamed = false;

        try {
            String pcfilename = Modules.IoFileMgrForUserModule.getDeviceFilePath(fileName);
            SeekableRandomFile file = new SeekableRandomFile(pcfilename, "rw");

            // EDAT header size.
            int edatHeaderSize = 0x90;

            // Setup the buffers.
            byte[] inBuf = new byte[edatHeaderSize];
            byte[] dataBuf = new byte[0x30];
            byte[] hashBuf = new byte[0x10];

            // Read the encrypted PSPEDATA header.
            file.readFully(inBuf);

            // Generate a new name hash for this file and compare with the one stored in it's header.
            System.arraycopy(inBuf, 0x10, dataBuf, 0, 0x30);
            System.arraycopy(inBuf, 0x40, hashBuf, 0, 0x10);
            
            // If the CryptoEngine fails to find a match, then the file has been renamed.
            if (crypto.CheckEDATANameKey(dataBuf, hashBuf, fileName.getBytes(), fileName.getBytes().length) != 0) {
                renamed = true;
            }

        } catch (Exception e) {
            // Ignore.
        }
        
        cpu.gpr[2] = 0;  // Faking.
    }

    @HLEFunction(nid = 0x08D98894, version = 150)
    public void sceNpDrmEdataSetupKey(Processor processor) {
        CpuState cpu = processor.cpu;

        int edataFd = cpu.gpr[4];

        log.warn("PARTIAL: (sceNpDrmEdataSetupKey edataFd=0x" + Integer.toHexString(edataFd) + ")");
    
        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        IoInfo info = Modules.IoFileMgrForUserModule.getFileIoInfo(edataFd);    
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
        
        cpu.gpr[2] = 0;  // Faking.
    }

    @HLEFunction(nid = 0x219EF5CC, version = 150)
    public void sceNpDrmEdataGetDataSize(Processor processor) {
        CpuState cpu = processor.cpu;
        
        int edataFd = cpu.gpr[4];

        log.warn("PARTIAL: sceNpDrmEdataGetDataSize edataFd=0x" + Integer.toHexString(edataFd));
        
        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        IoInfo info = Modules.IoFileMgrForUserModule.getFileIoInfo(edataFd); 
        int size = 0;
        try {
            size = (int) info.readOnlyFile.length();
        } catch (Exception e) {
            log.error(e);
        }
        cpu.gpr[2] = size;
    }

    @HLEFunction(nid = 0x2BAA4294, version = 150)
    public void sceNpDrmOpen(Processor processor) {
        CpuState cpu = processor.cpu;
        
        int nameAddr = cpu.gpr[4];
        int flags = cpu.gpr[5];
        int permissions = cpu.gpr[6];

        log.warn("IGNORING: sceNpDrmOpen (nameAddr=0x" + Integer.toHexString(nameAddr) 
                + ", flags=0x" + Integer.toHexString(flags) + ", permissions=0" + Integer.toOctalString(permissions));

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        cpu.gpr[2] = 0;
    }

    @HLEFunction(nid = 0xC618D0B1, version = 150)
    public void sceKernelLoadModuleNpDrm(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Memory.getInstance();
        
        int path_addr = cpu.gpr[4];
        int flags = cpu.gpr[5];
        int option_addr = cpu.gpr[6];

        String name = Utilities.readStringZ(path_addr);

        if (log.isDebugEnabled()) {
            log.debug("sceKernelLoadModuleNpDrm (path='" + name 
                    + "', flags=0x" + Integer.toHexString(flags) 
                    + ", option_addr=0x" + Integer.toHexString(option_addr) + ")");
        }

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        SceKernelLMOption lmOption = null;
        if (option_addr != 0) {
            lmOption = new SceKernelLMOption();
            lmOption.read(mem, option_addr);
            log.info("sceKernelLoadModuleNpDrm: partition=" + lmOption.mpidText + ", position=" + lmOption.position);
        }

        Modules.ModuleMgrForUserModule.hleKernelLoadModule(processor, name, flags, 0, false);
    }

    @HLEFunction(nid = 0xAA5FC85B, version = 150)
    public void sceKernelLoadExecNpDrm(Processor processor) {
        CpuState cpu = processor.cpu;
        Memory mem = Memory.getInstance();
        
        int filename_addr = cpu.gpr[4];
        int option_addr = cpu.gpr[5];

        String name = Utilities.readStringZ(filename_addr);

        if (IntrManager.getInstance().isInsideInterrupt()) {
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_CANNOT_BE_CALLED_FROM_INTERRUPT;
            return;
        }
        if (log.isDebugEnabled()) {
            log.debug(String.format("sceKernelLoadExecNpDrm file='%s' option_addr=0x%08X", name, option_addr));
        }

        // Flush system memory to mimic a real PSP reset.
        Modules.SysMemUserForUserModule.reset();

        if (option_addr != 0) {
            int optSize = mem.read32(option_addr);       // Size of the option struct.
            int argSize = mem.read32(option_addr + 4);   // Number of args (strings).
            int argAddr = mem.read32(option_addr + 8);   // Pointer to a list of strings.
            int keyAddr = mem.read32(option_addr + 12);  // Pointer to an encryption key (may not be used).

            if (log.isDebugEnabled()) {
            	log.debug(String.format("sceKernelLoadExecNpDrm params: optSize=%d, argSize=%d, argAddr=0x%08X, keyAddr=0x%08X", optSize, argSize, argAddr, keyAddr));
            }
        }

        try {
            SeekableDataInput moduleInput = Modules.IoFileMgrForUserModule.getFile(name, IoFileMgrForUser.PSP_O_RDONLY);
            if (moduleInput != null) {
                byte[] moduleBytes = new byte[(int) moduleInput.length()];
                moduleInput.readFully(moduleBytes);
                ByteBuffer moduleBuffer = ByteBuffer.wrap(moduleBytes);

                SceModule module = Emulator.getInstance().load(name, moduleBuffer, true);
                Emulator.getClock().resume();

                if ((module.fileFormat & Loader.FORMAT_ELF) == Loader.FORMAT_ELF) {
                    cpu.gpr[2] = 0;
                } else {
                    log.warn("sceKernelLoadExecNpDrm - failed, target is not an ELF");
                    cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_ILLEGAL_LOADEXEC_FILENAME;
                }
                moduleInput.close();
            }
        } catch (GeneralJpcspException e) {
            log.error("General Error : " + e.getMessage());
            Emulator.PauseEmu();
        } catch (IOException e) {
            log.error("sceKernelLoadExecNpDrm - Error while loading module " + name + ": " + e.getMessage());
            cpu.gpr[2] = SceKernelErrors.ERROR_KERNEL_PROHIBIT_LOADEXEC_DEVICE;
        }
    }

}