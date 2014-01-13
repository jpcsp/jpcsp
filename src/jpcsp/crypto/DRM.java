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
package jpcsp.crypto;

public class DRM {
    
    private static AMCTRL amctrl;
    private byte[] iv = {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
    
    public DRM() {
        amctrl = new AMCTRL();
    }
    
    /*
     * sceNpDrm - npdrm.prx
     */
    public byte[] hleNpDrmGetFixedKey(byte[] hash, byte[] data, int mode) {
        // Setup the crypto and keygen modes and initialize both context structs.   
        AMCTRL.BBMac_Ctx bbctx = new AMCTRL.BBMac_Ctx();
        AES128 aes = new AES128("AES/CBC/NoPadding");

        // Get the encryption key.
        byte[] encKey = new byte[0x10];
        if ((mode & 0x1) == 0x1) {
            for (int i = 0; i < 0x10; i++) {
                encKey[i] = (byte) (KeyVault.drmEncKey1[i] & 0xFF);
            }
        } else if ((mode & 0x2) == 0x2) {
            for (int i = 0; i < 0x10; i++) {
                encKey[i] = (byte) (KeyVault.drmEncKey2[i] & 0xFF);
            }
        } else if ((mode & 0x3) == 0x3) {
            for (int i = 0; i < 0x10; i++) {
                encKey[i] = (byte) (KeyVault.drmEncKey3[i] & 0xFF);
            }
        } else {
            return null;
        }

        // Get the fixed key.
        byte[] fixedKey = new byte[0x10];
        for (int i = 0; i < 0x10; i++) {
            fixedKey[i] = (byte) (KeyVault.drmFixedKey[i] & 0xFF);
        }

        // Call the BBMac functions.
        amctrl.hleDrmBBMacInit(bbctx, 1);
        amctrl.hleDrmBBMacUpdate(bbctx, data, data.length);
        amctrl.hleDrmBBMacFinal(bbctx, hash, fixedKey);

        // Encrypt and return the hash.
        return aes.encrypt(hash, encKey, iv);
    }
    
    public byte[] GetKeyFromRif(byte[] rifBuf, byte[] actdatBuf, byte[] openPSID) {
        AES128 aes = new AES128("AES/ECB/NoPadding");

        byte[] rifIndex = new byte[0x10];
        byte[] rifDatKey = new byte[0x10];
        byte[] encRifIndex = new byte[0x10];
        byte[] encRifDatKey = new byte[0x10];

        byte[] rifKey = new byte[KeyVault.drmRifKey.length];
        for (int i = 0; i < KeyVault.drmRifKey.length; i++) {
            rifKey[i] = (byte) (KeyVault.drmRifKey[i] & 0xFF);
        }

        System.arraycopy(rifBuf, 0x40, encRifIndex, 0x0, 0x10);
        System.arraycopy(rifBuf, 0x50, encRifDatKey, 0x0, 0x10);

        rifIndex = aes.decrypt(encRifIndex, rifKey, iv);

        long index = rifIndex[0xF];
        if (index < 0x80) {
            byte[] actDat = DecryptActdat(actdatBuf, openPSID);
            byte[] datKey = new byte[0x10];
            System.arraycopy(actDat, (int) index * 16, datKey, 0, 0x10);
            rifDatKey = aes.decrypt(encRifDatKey, datKey, iv);
        }

        return rifDatKey;
    }

    public byte[] DecryptActdat(byte[] actdatBuf, byte[] openPSID) {
        AES128 aes = new AES128("AES/ECB/NoPadding");

        byte[] actdat = new byte[0x800];
        byte[] consoleKey = GetConsoleKey(openPSID);
        System.arraycopy(actdatBuf, 0x10, actdat, 0x0, actdat.length - 0x10);

        return aes.decrypt(actdat, consoleKey, iv);
    }

    public byte[] GetConsoleKey(byte[] openPSID) {
        AES128 aes = new AES128("AES/ECB/NoPadding");

        byte[] actdatKey = new byte[KeyVault.drmActdatKey.length];
        for (int i = 0; i < KeyVault.drmActdatKey.length; i++) {
            actdatKey[i] = (byte) (KeyVault.drmActdatKey[i] & 0xFF);
        }

        return aes.encrypt(openPSID, actdatKey, iv);
    }
}