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

public class PGD {
    
    private static AMCTRL amctrl;
    private static DRM drm;
    private static AMCTRL.BBCipher_Ctx pgdCipherContext;
    private static AMCTRL.BBMac_Ctx pgdMacContext;
    
    public PGD() {
        amctrl = new AMCTRL();
        drm = new DRM();
    }
    
    // Plain PGD handling functions.
    public byte[] DecryptPGD(byte[] inbuf, int size, byte[] key) {
        // Setup the crypto and keygen modes and initialize both context structs.
        int sdEncMode = 1;
        int sdGenMode = 2;
        pgdMacContext = new AMCTRL.BBMac_Ctx();
        pgdCipherContext = new AMCTRL.BBCipher_Ctx();

        // Align the buffers to 16-bytes.
        int alignedSize = ((size + 0xF) >> 4) << 4;
        byte[] outbuf = new byte[alignedSize - 0x10];
        byte[] dataBuf = new byte[alignedSize];

        // Fully copy the contents of the encrypted file.
        System.arraycopy(inbuf, 0, dataBuf, 0, size);

        amctrl.hleDrmBBMacInit(pgdMacContext, sdEncMode);
        amctrl.hleDrmBBCipherInit(pgdCipherContext, sdEncMode, sdGenMode, dataBuf, key);
        amctrl.hleDrmBBMacUpdate(pgdMacContext, dataBuf, 0x10);
        System.arraycopy(dataBuf, 0x10, outbuf, 0, alignedSize - 0x10);
        amctrl.hleDrmBBMacUpdate(pgdMacContext, outbuf, alignedSize - 0x10);
        amctrl.hleDrmBBCipherUpdate(pgdCipherContext, outbuf, alignedSize - 0x10);

        return outbuf;
    }

    public byte[] UpdatePGDCipher(byte[] inbuf, int size) {
        // Align the buffers to 16-bytes.
        int alignedSize = ((size + 0xF) >> 4) << 4;
        byte[] outbuf = new byte[alignedSize - 0x10];
        byte[] dataBuf = new byte[alignedSize];
        
        System.arraycopy(inbuf, 0, dataBuf, 0, size);
        System.arraycopy(dataBuf, 0x10, outbuf, 0, alignedSize - 0x10);
        amctrl.hleDrmBBCipherUpdate(pgdCipherContext, outbuf, alignedSize - 0x10);

        return outbuf;
    }

    public void FinishPGDCipher() {
        amctrl.hleDrmBBCipherFinal(pgdCipherContext);
    }
    
    // PGD with EDAT header handling functions.
    public byte[] DecryptEDATPGD(byte[] inbuf, int size, int hashOffset, int chunkSize, byte[] key) {
        // Setup the crypto and keygen modes and initialize both context structs.
        int macEncMode;
        int cipherEncMode;
        int genMode = 2;
        pgdMacContext = new AMCTRL.BBMac_Ctx();
        pgdCipherContext = new AMCTRL.BBCipher_Ctx();

        // Align the buffers to 16-bytes and calculate offsets.
        int alignedSize = ((size + 0xF) >> 4) << 4;
        int hashTableOffset = hashOffset + alignedSize;
        int chunkNum = ((alignedSize + chunkSize - 1) & ~(chunkSize - 1)) / chunkSize;

        int keyIndex = inbuf[0x4];
        int drmType = inbuf[0x8];

        if ((drmType & 0x1) == 0x1) {
            macEncMode = 1;
            if (keyIndex > 0x1) {
                macEncMode = 3;
            }
            cipherEncMode = 1;
        } else {
            macEncMode = 2;
            cipherEncMode = 2;
        }

        byte[] hashTable = new byte[chunkNum * 16];
        System.arraycopy(inbuf, hashTableOffset, hashTable, 0, hashTable.length);

        byte[] macKey60 = new byte[0x10];
        System.arraycopy(inbuf, 0x60, macKey60, 0, 0x10);

        // MAC_0x60
        amctrl.hleDrmBBMacInit(pgdMacContext, macEncMode);
        amctrl.hleDrmBBMacUpdate(pgdMacContext, hashTable, hashTable.length);
        amctrl.hleDrmBBMacFinal2(pgdMacContext, macKey60, key);

        // Decrypt all the data.
        byte[] decDataBuf = new byte[inbuf.length - 0x30];
        System.arraycopy(inbuf, 0x30, decDataBuf, 0, decDataBuf.length);

        byte[] updateDecDataBuf = new byte[inbuf.length - 0x90];
        System.arraycopy(inbuf, 0x90, updateDecDataBuf, 0, updateDecDataBuf.length);

        amctrl.hleDrmBBCipherInit(pgdCipherContext, cipherEncMode, genMode, decDataBuf, key);
        amctrl.hleDrmBBCipherUpdate(pgdCipherContext, updateDecDataBuf, alignedSize);
        amctrl.hleDrmBBCipherFinal(pgdCipherContext);

        byte[] outbuf = new byte[size];
        System.arraycopy(updateDecDataBuf, 0, outbuf, 0, outbuf.length);

        return outbuf;
    }

    public byte[] DecryptEDATPGDHeader(byte[] inbuf, int size, byte[] key) {
        // Setup the crypto and keygen modes and initialize both context structs.
        int cipherEncMode;
        int genMode = 2;
        pgdCipherContext = new AMCTRL.BBCipher_Ctx();

        // Align the buffers to 16-bytes.
        int alignedSize = ((size + 0xF) >> 4) << 4;
        byte[] dataBuf = new byte[alignedSize];

        // Fully copy the contents of the encrypted file.
        System.arraycopy(inbuf, 0, dataBuf, 0, size);

        int drmType = dataBuf[0x8];

        if ((drmType & 0x1) == 0x1) {
            cipherEncMode = 1;
        } else {
            cipherEncMode = 2;
        }

        // Decrypt the PGD data with the given key.
        byte[] cipherBuf = new byte[dataBuf.length - 0x10];
        byte[] pgdDataBuf = new byte[0x30];

        System.arraycopy(dataBuf, 0x10, cipherBuf, 0, dataBuf.length - 0x10);
        System.arraycopy(dataBuf, 0x30, pgdDataBuf, 0, 0x30);

        amctrl.hleDrmBBCipherInit(pgdCipherContext, cipherEncMode, genMode, cipherBuf, key);
        amctrl.hleDrmBBCipherUpdate(pgdCipherContext, pgdDataBuf, 0x30);
        amctrl.hleDrmBBCipherFinal(pgdCipherContext);

        return pgdDataBuf;
    }

    public byte[] GetEDATPGDKey(byte[] inbuf, int size) {
        // Setup the crypto and keygen modes and initialize both context structs.
        int macEncMode;
        int pgdFlag = 2;
        pgdMacContext = new AMCTRL.BBMac_Ctx();

        // Align the buffer to 16-bytes.
        int alignedSize = ((size + 0xF) >> 4) << 4;
        byte[] dataBuf = new byte[alignedSize];

        // Fully copy the contents of the encrypted file.
        System.arraycopy(inbuf, 0, dataBuf, 0, size);

        int keyIndex = dataBuf[0x4];
        int drmType = dataBuf[0x8];

        if ((drmType & 0x1) == 0x1) {
            macEncMode = 1;
            pgdFlag |= 4;
            if (keyIndex > 0x1) {
                macEncMode = 3;
                pgdFlag |= 8;
            }
        } else {
            macEncMode = 2;
        }

        // Get fixed DNAS keys.
        byte[] dnasKey = new byte[0x10];
        if ((pgdFlag & 0x2) == 0x2) {
            for (int i = 0; i < KeyVault.drmDNASKey1.length; i++) {
                dnasKey[i] = (byte) (KeyVault.drmDNASKey1[i] & 0xFF);
            }
        } else if ((pgdFlag & 0x1) == 0x1) {
            for (int i = 0; i < KeyVault.drmDNASKey2.length; i++) {
                dnasKey[i] = (byte) (KeyVault.drmDNASKey2[i] & 0xFF);
            }
        } else {
            return null;
        }

        // Get mac80 from input.
        byte[] macKey80 = new byte[0x10];
        System.arraycopy(dataBuf, 0x80, macKey80, 0, 0x10);

        // Get mac70 from input.
        byte[] macKey70 = new byte[0x10];
        System.arraycopy(dataBuf, 0x70, macKey70, 0, 0x10);

        // MAC_0x80
        amctrl.hleDrmBBMacInit(pgdMacContext, macEncMode);
        amctrl.hleDrmBBMacUpdate(pgdMacContext, dataBuf, 0x80);
        amctrl.hleDrmBBMacFinal2(pgdMacContext, macKey80, dnasKey);

        // MAC_0x70
        amctrl.hleDrmBBMacInit(pgdMacContext, macEncMode);
        amctrl.hleDrmBBMacUpdate(pgdMacContext, dataBuf, 0x70);

        // Get the decryption key from BBMAC.
        return amctrl.GetKeyFromBBMac(pgdMacContext, macKey70);
    }
    
    public boolean CheckEDATRenameKey(byte[] fileName, byte[] hash, byte[] data) {
        // Set up MAC context.
        pgdMacContext = new AMCTRL.BBMac_Ctx();
        
        // Perform hash check.
        amctrl.hleDrmBBMacInit(pgdMacContext, 3);
        amctrl.hleDrmBBMacUpdate(pgdMacContext, data, 0x30);
        amctrl.hleDrmBBMacUpdate(pgdMacContext, fileName, fileName.length);     
        
        // Get the fixed rename key.
        byte[] renameKey = new byte[0x10];
        for (int i = 0; i < 0x10; i++) {
            renameKey[i] = (byte) (KeyVault.drmRenameKey[i] & 0xFF);
        }
        
        // Compare and return.
        return (amctrl.hleDrmBBMacFinal2(pgdMacContext, hash, renameKey) == 0);
    }
}