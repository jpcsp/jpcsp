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
package jpcsp.HLE.modules500;

import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.zip.CRC32;

import jpcsp.crypto.CryptoEngine;
import jpcsp.crypto.KeyVault;
import jpcsp.filesystems.SeekableRandomFile;
import jpcsp.HLE.CanBeNull;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLELogging;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer32;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.settings.Settings;

@HLELogging
public class scePauth extends HLEModule {
    public static Logger log = Modules.getLogger("scePauth");

    @Override
	public String getName() {
		return "scePauth";
	}

    @HLEUnimplemented
    @HLEFunction(nid = 0xF7AA47F6, version = 500)
    public int scePauth_F7AA47F6(TPointer inputAddr, int inputLength, @CanBeNull TPointer32 resultLengthAddr, TPointer keyAddr) {
    	CryptoEngine crypto = new CryptoEngine();
        byte[] in = inputAddr.getArray8(inputLength);
        byte[] key = keyAddr.getArray8(0x10);
        byte[] xor = new byte[0x10];
        for(int i = 0; i < 0x10; i++) {
            xor[i] = (byte)(KeyVault.pauthXorKey[i] & 0xFF);
        }

        // Try to read/write PAUTH data for external decryption.
        try {
            // Calculate CRC32 for PAUTH data.
            CRC32 crc = new CRC32();
            crc.update(in, 0, inputLength);
            int tag = (int) crc.getValue();

            // Set PAUTH file name and PAUTH dir name.
            String pauthDirName = String.format("%sPAUTH%c", Settings.getInstance().getDiscTmpDirectory(), File.separatorChar);
            String pauthFileName = pauthDirName + String.format("pauth-%s.bin", Integer.toHexString(tag));
            String pauthDecFileName = pauthDirName + String.format("pauth-%s.bin.decrypt", Integer.toHexString(tag));
            String pauthKeyFileName = pauthDirName + String.format("pauth-%s.key", Integer.toHexString(tag));

            // Check for an already decrypted file.
            File dec = new File(pauthDecFileName);
            if (dec.exists()) {
                log.info("Reading PAUTH data file from " + pauthDecFileName);

                // Read the externally decrypted file.
                SeekableRandomFile pauthPRXDec = new SeekableRandomFile(pauthDecFileName, "rw");
                int pauthSize = (int) pauthPRXDec.length();
                byte[] pauthDec = new byte[pauthSize];
                pauthPRXDec.read(pauthDec);
                pauthPRXDec.close();

                inputAddr.setArray(pauthDec, pauthSize);
                resultLengthAddr.setValue(pauthSize);
            } else {
                // Create PAUTH dir under tmp.
                File f = new File(pauthDirName);
                f.mkdirs();

                log.info("Writting PAUTH data file to " + pauthFileName);
                log.info("Writting PAUTH key file to " + pauthKeyFileName);

                // Write the PAUTH file and key for external decryption.
                SeekableRandomFile pauthPRX = new SeekableRandomFile(pauthFileName, "rw");
                SeekableRandomFile pauthKey = new SeekableRandomFile(pauthKeyFileName, "rw");
                pauthPRX.write(in);
                pauthKey.write(key);
                pauthPRX.close();
                pauthKey.close();
                
                // Decryption is not working properly due to a missing KIRK key.
                int reslength = crypto.getPRXEngine().DecryptPRX(in, inputLength, null, 0, 5, key, xor);
                
                // Fake the result.
                inputAddr.clear(reslength);
                resultLengthAddr.setValue(reslength);
            }
        } catch (IOException ioe) {
            log.error(ioe);
        }
        
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x98B83B5D, version = 500)
    public int scePauth_98B83B5D(TPointer inputAddr, int inputLength,  @CanBeNull TPointer32 resultLengthAddr, TPointer keyAddr) {
        CryptoEngine crypto = new CryptoEngine();
        byte[] in = inputAddr.getArray8(inputLength);
        byte[] key = keyAddr.getArray8(0x10);
        byte[] xor = new byte[0x10];
        for(int i = 0; i < 0x10; i++) {
            xor[i] = (byte)(KeyVault.pauthXorKey[i] & 0xFF);
        }

        // Try to read/write PAUTH data for external decryption.
        try {
            // Calculate CRC32 for PAUTH data.
            CRC32 crc = new CRC32();
            crc.update(in, 0, inputLength);
            int tag = (int) crc.getValue();

            // Set PAUTH file name and PAUTH dir name.
            String pauthDirName = String.format("%sPAUTH%c", Settings.getInstance().getDiscTmpDirectory(), File.separatorChar);
            String pauthFileName = pauthDirName + String.format("pauth_%s.bin", Integer.toHexString(tag));
            String pauthDecFileName = pauthDirName + String.format("pauth_%s.bin.decrypt", Integer.toHexString(tag));
            String pauthKeyFileName = pauthDirName + String.format("pauth_%s.key", Integer.toHexString(tag));

            // Check for an already decrypted file.
            File dec = new File(pauthDecFileName);
            if (dec.exists()) {
                log.info("Reading PAUTH data file from " + pauthDecFileName);

                // Read the externally decrypted file.
                SeekableRandomFile pauthPRXDec = new SeekableRandomFile(pauthDecFileName, "rw");
                int pauthSize = (int) pauthPRXDec.length();
                byte[] pauthDec = new byte[pauthSize];
                pauthPRXDec.read(pauthDec);
                pauthPRXDec.close();

                inputAddr.setArray(pauthDec, pauthSize);
                resultLengthAddr.setValue(pauthSize);
            } else {
                // Create PAUTH dir under tmp.
                File f = new File(pauthDirName);
                f.mkdirs();

                log.info("Writting PAUTH data file to " + pauthFileName);
                log.info("Writting PAUTH key file to " + pauthKeyFileName);

                // Write the PAUTH file and key for external decryption.
                SeekableRandomFile pauthPRX = new SeekableRandomFile(pauthFileName, "rw");
                SeekableRandomFile pauthKey = new SeekableRandomFile(pauthKeyFileName, "rw");
                pauthPRX.write(in);
                pauthKey.write(key);
                pauthPRX.close();
                pauthKey.close();
                
                // Decryption is not working properly due to a missing KIRK key.
                int reslength = crypto.getPRXEngine().DecryptPRX(in, inputLength, null, 0, 5, key, xor);
                
                // Fake the result.
                inputAddr.clear(reslength);
                resultLengthAddr.setValue(reslength);
            }
        } catch (IOException ioe) {
            log.error(ioe);
        }
        
    	return 0;
    }
}
