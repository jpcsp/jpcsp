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

import jpcsp.HLE.BufferInfo;
import jpcsp.HLE.BufferInfo.LengthInfo;
import jpcsp.HLE.BufferInfo.Usage;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.TPointer;
import libkirk.KirkEngine;
import jpcsp.HLE.Modules;

import static jpcsp.HLE.kernel.types.SceKernelErrors.SCE_DNAS_ERROR_OPERATION_FAILED;
import static jpcsp.util.Utilities.writeUnaligned32;
import static libkirk.KirkEngine.KIRK_CMD_ECDSA_VERIFY;
import static libkirk.KirkEngine.KIRK_CMD_SHA1_HASH;

import org.apache.log4j.Logger;

public class sceOpenPSID extends HLEModule {
    public static Logger log = Modules.getLogger("sceOpenPSID");

    protected int[] dummyOpenPSID = {0x10, 0x02, 0xA3, 0x44, 0x13, 0xF5, 0x93, 0xB0, 0xCC, 0x6E, 0xD1, 0x32, 0x27, 0x85, 0x0F, 0x9D};
    protected int[] dummyPSID     = {0x10, 0x02, 0xA3, 0x44, 0x13, 0xF5, 0x93, 0xB0, 0xCC, 0x6E, 0xD1, 0x32, 0x27, 0x85, 0x0F, 0x9D};

    @HLEFunction(nid = 0xC69BEBCE, version = 150, checkInsideInterrupt = true)
    public int sceOpenPSIDGetOpenPSID(TPointer openPSIDAddr) {
        for (int i = 0; i < dummyOpenPSID.length; i++) {
        	openPSIDAddr.setValue8(i, (byte) dummyOpenPSID[i]);
        }

        return 0;
    }

    @HLEFunction(nid = 0x19D579F0, version = 150)
    public int  sceOpenPSIDGetPSID(TPointer PSIDAddr, int unknown) {
        for (int i = 0; i < dummyPSID.length; i++) {
        	PSIDAddr.setValue8(i, (byte) dummyPSID[i]);
        }

        return 0;
    }

    /**
     * Encrypt the provided data. It will be encrypted using AES.
     *
     * @note The used key is provided by the PSP.
     * 
     * @param pSrcData Pointer to data to encrypt. The encrypted data will be written 
     *                 back into this buffer.
     * @param size The size of the data to encrypt. The size needs to be a multiple of ::KIRK_AES_BLOCK_LEN.
                   Max size: ::SCE_DNAS_USER_DATA_MAX_LEN.
     *
     * @return 0 on success, otherwise < 0.
     */
    @HLEUnimplemented
    @HLEFunction(nid = 0x05D50F41, version = 150)
    public int sceDdrdbEncrypt(@BufferInfo(lengthInfo=LengthInfo.nextParameter, usage=Usage.inout) TPointer srcData, int size) {
    	return 0;
    }

    /**
     * Verify a certificate.
     *
     * @param pCert Pointer to the certificate to verify. Certificate length: ::KIRK_CERT_LEN.
     *
     * @return 0 on success, otherwise < 0.
     */
    @HLEUnimplemented
    @HLEFunction(nid = 0x370F456A, version = 150)
    public int sceDdrdbCertvry(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=184, usage=Usage.in) TPointer cert) {
    	return 0;
    }

    /** 
     * Generate a SHA-1 hash value of the provided data.
     * 
     * @param pSrcData Pointer to data to generate the hash for.
     * @param size The size of the source data. Max size: ::SCE_DNAS_USER_DATA_MAX_LEN.
     * @param pDigest Pointer to buffer receiving the hash. Size: ::KIRK_SHA1_DIGEST_LEN.
     *
     * @return 0 on success, otherwise < 0.
     */
    @HLEFunction(nid = 0x40CB752A, version = 150)
    public int sceDdrdbHash(@BufferInfo(lengthInfo=LengthInfo.nextParameter, usage=Usage.in) TPointer srcData, int size, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=20, usage=Usage.out) TPointer digest) {
    	byte[] kirkBuffer = new byte[KirkEngine.KIRK_SHA1_HEADER.SIZEOF + size];
    	byte[] digestBuffer = new byte[20];
    	writeUnaligned32(kirkBuffer, 0, size);
    	srcData.getArray8(0, kirkBuffer, 4, size);
    	int result = Modules.semaphoreModule.hleUtilsBufferCopyWithRange(digestBuffer, digestBuffer.length, kirkBuffer, kirkBuffer.length, KIRK_CMD_SHA1_HASH);
    	if (result != 0) {
    		return SCE_DNAS_ERROR_OPERATION_FAILED;
    	}

    	digest.setArray(digestBuffer);

    	return 0;
    }

    /**
     * Generate a valid signature for the specified data using the specified private key.
     *
     * @note The ECDSA algorithm is used to generate a signature.
     *
     * @param pPrivKey Pointer to the private key used to generate the signature.
     *                 CONFIRM: The key has to be AES encrypted before.
     * @param pData Pointer to data a signature has to be computed for. Data length: ::KIRK_ECDSA_SRC_DATA_LEN
     * @param pSig Pointer to a buffer receiving the signature. Signature length: ::KIRK_ECDSA_SIG_LEN
     *
     * @return 0 on success, otherwise < 0.
     */
    @HLEUnimplemented
    @HLEFunction(nid = 0xB24E1391, version = 150)
    public int sceDdrdbSiggen(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=20, usage=Usage.in) TPointer privKey, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=20, usage=Usage.in) TPointer srcData, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=40, usage=Usage.out) TPointer sig) {
    	return 0;
    }

    /**
     * Decrypt the provided data. The data has to be AES encrypted. 
     *
     * @note The used key is provided by the PSP.
     * 
     * @param pSrcData Pointer to data to decrypt. The decrypted data will be written
     *                 back into this buffer.
     * @param size The size of the data to decrypt. The size needs to be a multiple of ::KIRK_AES_BLOCK_LEN.
                   Max size: ::SCE_DNAS_USER_DATA_MAX_LEN.
     *
     * @return 0 on success, otherwise < 0.
     */
    @HLEUnimplemented
    @HLEFunction(nid = 0xB33ACB44, version = 150)
    public int sceDdrdbDecrypt(@BufferInfo(lengthInfo=LengthInfo.nextParameter, usage=Usage.inout) TPointer srcData, int size) {
    	return 0;
    }

    /**
     * Generate a ::KIRK_PRN_LEN large pseudorandom number (PRN). 
     * 
     * @note The seed is automatically set by the system software.
     * 
     * @param pDstData Pointer to buffer receiving the PRN. Size has to be ::KIRK_PRN_LEN.
     *
     * @return 0 on success, otherwise < 0.
     */
    @HLEUnimplemented
    @HLEFunction(nid = 0xB8218473, version = 150)
    public int sceDdrdbPrngen(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=20, usage=Usage.out) TPointer dstData) {
    	return 0;
    }

    /**
     * Verify if the provided signature is valid for the specified data given the public key. 
     *
     * @note The ECDSA algorithm is used to verify a signature.
     *
     * @param pPubKey The public key used for validating the (data,signature) pair.
     *                Size has to be ::KIRK_ECDSA_PUBLIC_KEY_LEN.
     * @param pData Pointer to data the signature has to be verified for.
                    Data length: ::KIRK_ECDSA_SRC_DATA_LEN
     * @param pSig Pointer to the signature to verify. Signature length: ::KIRK_ECDSA_SIG_LEN
     *
     * @return 0 on success, otherwise < 0.
     */
    @HLEFunction(nid = 0xE27CE4CB, version = 150)
    public int sceDdrdbSigvry(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=KirkEngine.ECDSA_POINT.SIZEOF, usage=Usage.in) TPointer pubKey, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=20, usage=Usage.in) TPointer data, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=KirkEngine.ECDSA_SIG.SIZEOF, usage=Usage.in) TPointer sig) {
    	byte[] kirkBuffer = new byte[KirkEngine.KIRK_CMD17_BUFFER.SIZEOF];
    	pubKey.getArray8(0, kirkBuffer, 0, KirkEngine.ECDSA_POINT.SIZEOF);
    	data.getArray8(0, kirkBuffer, KirkEngine.ECDSA_POINT.SIZEOF, 0x14);
    	sig.getArray8(0, kirkBuffer, KirkEngine.ECDSA_POINT.SIZEOF + 0x14, KirkEngine.ECDSA_SIG.SIZEOF);
    	int result = Modules.semaphoreModule.hleUtilsBufferCopyWithRange(null, 0, kirkBuffer, kirkBuffer.length, KIRK_CMD_ECDSA_VERIFY);
    	if (result != 0) {
    		return SCE_DNAS_ERROR_OPERATION_FAILED;
    	}

    	return 0;
    }

    /**
    *
    * Compute a new elliptic curve point by multiplying the provided private key with the
    * provided base point of the elliptic curve.
    *
    * @param pPrivKey Pointer to the private key of a (public,private) key pair usable for ECDSA.
    *                 
    * @param pBasePoint Pointer to a base point of the elliptic curve. Point size: ::KIRK_ECDSA_POINT_LEN
    * @param pNewPoint Pointer to a buffer receiving the new curve point. Buffer size: ::KIRK_ECDSA_POINT_LEN
    *
    * @return 0 on success, otherwise < 0.
    */
    @HLEUnimplemented
    @HLEFunction(nid = 0xEC05300A, version = 150)
    public int sceDdrdbMul2(TPointer privKey, TPointer basePoint, TPointer newPoint) {
    	return 0;
    }

    /**
     * Generate a new (public,private) key pair to use with ECDSA.
     *
     * @param pKeyData Pointer to buffer receiving the computed key pair.
     *                 The first ::KIRK_ECDSA_PRIVATE_KEY_LEN byte will contain the private key.
     *                 The rest of the bytes will contain the public key (elliptic curve) point p = (x,y),
     *                 with the x-value being first. Both coordinates have size ::KIRK_ECDSA_POINT_LEN / 2.
     *
     * @return 0 on success, otherwise < 0.
     */
    @HLEUnimplemented
    @HLEFunction(nid = 0xF970D54E, version = 150)
    public int sceDdrdbMul1(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=60, usage=Usage.out) TPointer keyData) {
    	return 0;
    }

    /**
     * Verify if the provided signature is valid for the specified data. The public key
     * is provided by the system software.
     *
     * @note The ECDSA algorithm is used to verify a signature.
     *
     * @param pData Pointer to data the signature has to be verified for.
     *              Data length: ::KIRK_ECDSA_SRC_DATA_LEN.
     * @param pSig Pointer to the signature to verify. Signature length: ::KIRK_ECDSA_SIG_LEN.
     *
     * @return 0 on success, otherwise < 0.
     */
    @HLEUnimplemented
    @HLEFunction(nid = 0xF013F8BF, version = 150)
    public int sceDdrdb_F013F8BF(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=20, usage=Usage.in) TPointer data, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=40, usage=Usage.in) TPointer sig) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x8523E178, version = 150)
    public int sceMlnpsnlAuth1BB(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=32, usage=Usage.in) TPointer unknown1, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=8, usage=Usage.in) TPointer unknown2, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=128, usage=Usage.out) TPointer unknown3, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=64, usage=Usage.out) TPointer unknown4) {
    	unknown3.clear(128);
    	unknown4.clear(64);

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x6885F392, version = 150)
    public int sceMlnpsnlAuth2BB() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xF9ECFDDD, version = 150)
    public int scePcactAuth1BB() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x08BB9677, version = 150)
    public int scePcactAuth2BB() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xB29330DE, version = 150)
    public int sceOpenPSIDGetProductCode() {
    	return 0;
    }
}