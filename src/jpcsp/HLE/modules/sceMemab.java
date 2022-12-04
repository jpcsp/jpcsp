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

import static jpcsp.crypto.KIRK.PSP_KIRK_CMD_DECRYPT;
import static jpcsp.crypto.KIRK.PSP_KIRK_CMD_ECDSA_GEN_KEYS;
import static jpcsp.crypto.KIRK.PSP_KIRK_CMD_ECDSA_MULTIPLY_POINT;
import static jpcsp.crypto.KIRK.PSP_KIRK_CMD_ECDSA_SIGN;
import static jpcsp.crypto.KIRK.PSP_KIRK_CMD_ECDSA_VERIFY;
import static jpcsp.crypto.KIRK.PSP_KIRK_CMD_MODE_DECRYPT_CBC;
import static jpcsp.crypto.KIRK.PSP_KIRK_CMD_MODE_ENCRYPT_CBC;
import static jpcsp.crypto.KIRK.PSP_KIRK_CMD_PRNG;
import static jpcsp.crypto.KIRK.PSP_KIRK_CMD_SHA1_HASH;
import static jpcsp.util.Utilities.writeUnaligned32;
import static jpcsp.util.Utilities.writeUnaligned64;

import java.util.Arrays;
import java.util.Random;

import org.apache.log4j.Logger;

import jpcsp.Allegrex.compiler.RuntimeContext;
import jpcsp.HLE.BufferInfo;
import jpcsp.HLE.BufferInfo.LengthInfo;
import jpcsp.HLE.BufferInfo.Usage;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointer;
import jpcsp.hardware.Nand;
import jpcsp.memory.ByteArrayMemory;

/**
 * Crypto library used to encrypt/decrypt the sceNetAdhoc packets.
 */
public class sceMemab extends HLEModule {
    public static Logger log = Modules.getLogger("sceMemab");
    private Random random = new Random();
    private long scrambleCounter;
    private byte[] randomKey;

    private byte[] readCertificate() {
    	byte[] bufferBytes = new byte[Nand.pageSize];
    	ByteArrayMemory bufferMemory = new ByteArrayMemory(bufferBytes);
    	TPointer buffer = new TPointer(bufferMemory, 0);
    	int result = Modules.sceIdStorageModule.hleIdStorageReadLeaf(0x0100, buffer);
    	if (result != 0) {
    		return null;
    	}

    	byte[] certificate = new byte[0xB8];
    	System.arraycopy(bufferBytes, 0xF0, certificate, 0, certificate.length);

    	return certificate;
    }

    private int decrypt(byte[] output, byte[] input, int keyseed) {
    	byte[] buffer = new byte[input.length + 20];
    	writeUnaligned32(buffer, 0, PSP_KIRK_CMD_MODE_DECRYPT_CBC);
    	writeUnaligned32(buffer, 4, 0);
    	writeUnaligned32(buffer, 8, 0);
    	writeUnaligned32(buffer, 12, keyseed);
    	writeUnaligned32(buffer, 16, input.length);
    	System.arraycopy(input, 0, buffer, 20, input.length);

    	int result = Modules.semaphoreModule.hleUtilsBufferCopyWithRange(output, output.length, buffer, buffer.length, PSP_KIRK_CMD_DECRYPT);

    	return result;
    }

    private int encrypt(byte[] output, byte[] input, int keyseed) {
    	byte[] buffer = new byte[input.length + 20];
    	writeUnaligned32(buffer, 0, PSP_KIRK_CMD_MODE_ENCRYPT_CBC);
    	writeUnaligned32(buffer, 4, 0);
    	writeUnaligned32(buffer, 8, 0);
    	writeUnaligned32(buffer, 12, keyseed);
    	writeUnaligned32(buffer, 16, input.length);
    	System.arraycopy(input, 0, buffer, 20, input.length);

    	byte[] outputBuffer = new byte[output.length + 20];
    	int result = Modules.semaphoreModule.hleUtilsBufferCopyWithRange(outputBuffer, outputBuffer.length, buffer, buffer.length, PSP_KIRK_CMD_DECRYPT);

    	System.arraycopy(outputBuffer, 20, output, 0, output.length);

    	return result;
    }

    private int random(byte[] output, int outputLength) {
    	int result = Modules.semaphoreModule.hleUtilsBufferCopyWithRange(output, outputLength, null, 0, PSP_KIRK_CMD_PRNG);

    	return result;
    }

    private int encryptWithRandomData(byte[] output, byte[] input, int keyseed) {
    	byte[] randomData = new byte[32];
    	int result = random(randomData, randomData.length);
    	if (result != 0) {
    		return result;
    	}

    	byte[] buffer = new byte[input.length + 16];
    	for (int i = 0; i < 8; i++) {
    		buffer[i] = randomData[i * 4];
    	}
    	for (int i = 8; i < 16; i++) {
    		buffer[i] = (byte) 0;
    	}
    	System.arraycopy(input, 0, buffer, 16, input.length);

    	result = encrypt(output, buffer, keyseed);

    	return result;
    }

    private int hash(byte[] output, byte[] input, int offset, int length) {
    	byte[] buffer = new byte[length + 4];
    	writeUnaligned32(buffer, 0, length);

    	int result = Modules.semaphoreModule.hleUtilsBufferCopyWithRange(output, 20, buffer, buffer.length, PSP_KIRK_CMD_SHA1_HASH);

    	return result;
    }

    private int verify(byte[] publicKey, int publicKeyOffset, byte[] messageHash, int messageHashOffset, byte[] signature, int signatureOffset) {
    	byte[] buffer = new byte[40 + 20 + 40];
    	System.arraycopy(publicKey, publicKeyOffset, buffer, 0, 40);
    	System.arraycopy(messageHash, messageHashOffset, buffer, 40, 20);
    	System.arraycopy(signature, signatureOffset, buffer, 40 + 20, 40);

    	int result = Modules.semaphoreModule.hleUtilsBufferCopyWithRange(null, 0, buffer, buffer.length, PSP_KIRK_CMD_ECDSA_VERIFY);

    	return result;
    }

    private int genKeys(byte[] privateKey, byte[] publicKey) {
    	byte[] buffer = new byte[60];
    	int result = Modules.semaphoreModule.hleUtilsBufferCopyWithRange(buffer, buffer.length, null, 0, PSP_KIRK_CMD_ECDSA_GEN_KEYS);

    	System.arraycopy(buffer, 0, privateKey, 0, 20);
    	System.arraycopy(buffer, 20, publicKey, 0, 40);

    	return result;
    }

    private int multiply(byte[] output, byte[] multiplier, byte[] publicKey, int publicKeyOffset) {
    	byte[] buffer = new byte[60];
    	System.arraycopy(multiplier, 0, buffer, 0, 20);
    	System.arraycopy(publicKey, publicKeyOffset, buffer, 20, 40);
    	int result = Modules.semaphoreModule.hleUtilsBufferCopyWithRange(output, 40, buffer, buffer.length, PSP_KIRK_CMD_ECDSA_MULTIPLY_POINT);

    	return result;
    }

    private int sign(byte[] output, byte[] encPrivate, int encPrivateOffset, byte[] messageHash, int messageHashOffset) {
    	byte[] buffer = new byte[32 + 20];
    	System.arraycopy(encPrivate, encPrivateOffset, buffer, 0, 32);
    	System.arraycopy(messageHash, messageHashOffset, buffer, 32, 20);
    	int result = Modules.semaphoreModule.hleUtilsBufferCopyWithRange(output, 40, buffer, buffer.length, PSP_KIRK_CMD_ECDSA_SIGN);

    	return result;
    }

    private int scrambleKey(byte[] output, long[] counter, byte[] key) {
    	final int keyseed = 68;

    	byte[] input = new byte[output.length];
    	for (int i = 0; i < output.length; i += 16) {
			writeUnaligned64(input, i, counter[0]);
    		if ((counter[0] & 1L) != 0) {
    			System.arraycopy(key, 0, input, i + 8, 8);
    		} else {
    			System.arraycopy(key, 8, input, i + 8, 8);
    		}
    		counter[0] += 1L;
    	}

    	int result = decrypt(output, input, keyseed);

    	return result;
    }

    private int decryptAndHash(byte[] dataBuffer, int dataLength, long[] counter, byte[] key, byte[] output, boolean flag) {
    	writeUnaligned64(output, 0, counter[0]);
    	Arrays.fill(output, 8, 16, (byte) 0);

    	if (flag) {
    		if (dataLength == 0) {
    			return -1;
    		}

    		byte[] buffer = new byte[dataLength];
    		int result = scrambleKey(buffer, counter, key);
    		if (result != 0) {
    			return result;
    		}

			for (int i = 0; i < dataLength; i++) {
				dataBuffer[i] ^= buffer[i];
			}
    	}

		byte[] hashInput = new byte[20 + 16 + dataLength];
		byte[] hashOutput = new byte[20];
		System.arraycopy(key, 0, hashInput, 0, 20);
		System.arraycopy(output, 0, hashInput, 20, 16);
		System.arraycopy(dataBuffer, 0, hashInput, 20 + 16, dataLength);
		int result = hash(hashOutput, hashInput, 0, hashInput.length);
		if (result != 0) {
			return result;
		}

		System.arraycopy(hashOutput, 0, output, 16, 16);

		return 0;
    }

    /**
     * sceMemab initialization.
     * Generates a random number if not yet generated.
     * Called by sceNetAdhocAuthInit().
     * 
     * @return
     */
    @HLEUnimplemented
    @HLEFunction(nid = 0x6DD7339A, version = 150)
    public int sceAdhocBBInit() {
    	// Has no parameters
    	if (randomKey == null) {
    		scrambleCounter = 0L;

    		byte[] buffer = new byte[20];
    		int result = random(buffer, buffer.length);
    		if (result != 0) {
    			return result;
    		}

    		randomKey = new byte[20];
    	}

    	return 0;
    }

    /**
     * sceMemab termination.
     * Called by sceNetAdhocAuthTerm or if sceNetAdhocAuthInit is failing internally.
     * 
     * @return
     */
    @HLEUnimplemented
    @HLEFunction(nid = 0xD57856A7, version = 150)
    public int sceAdhocBBShutdown() {
    	// Has no parameters
    	return 0;
    }

    /**
     * Method called to encrypt a data message sent to the MAC address FF:FF:FF:FF:FF:FF.
     * This method is called when receiving a message having
     * - protocolType == WLAN_PROTOCOL_TYPE_SONY
     * - protocolSubType == WLAN_PROTOCOL_SUBTYPE_DATA
     * - destMacAddress == FF:FF:FF:FF:FF:FF
     * 
     * @param dataAddr	    pointer to the message dataAddr + 48
     * @param dataLength	initial totalDataLength - 48 (i.e. length of data pointed by dataAddr48)
     * @param outputAddr	pointer to the message dataAddr + 16
     * @param unknownFlag
     * @return
     */
    @HLEUnimplemented
    @HLEFunction(nid = 0xF1D2B9B0, version = 271)
    @HLEFunction(nid = 0xF742F283, version = 661)
    public int sceMemab_driver_F742F283(TPointer dataAddr, int dataLength, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=32, usage=Usage.out) TPointer outputAddr, boolean unknownFlag) {
    	if ((dataLength & 0xF) != 0 || dataLength < 0 || dataLength > 2320) {
    		return -1;
    	}

    	byte[] dataBuffer = dataAddr.getArray8(dataLength);
    	long[] counter = new long[1];
    	counter[0] = scrambleCounter;
    	byte[] output = new byte[32];
    	int result = decryptAndHash(dataBuffer, dataLength, counter, randomKey, output, unknownFlag);

    	outputAddr.setArray(output);
    	scrambleCounter = counter[0];

    	return result;
    }

    /**
     * Method called to process a keep-alive control message.
     * This method is called when receiving a message having
     * - protocolType == WLAN_PROTOCOL_TYPE_SONY
     * - protocolSubType == WLAN_PROTOCOL_SUBTYPE_CONTROL
     * - controlType == 2
     * 
     * @param unknownOutput1
     * @param unknownOutput2
     * @return
     */
    @HLEUnimplemented
    @HLEFunction(nid = 0x4B54EAAD, version = 150)
    public int sceAdhocBBAuth1(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=192, usage=Usage.out) TPointer unknownOutput1, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=160, usage=Usage.out) TPointer unknownOutput2) {
    	int offset1 = 0;
    	unknownOutput1.clear(offset1, 60); // random private & public keys as generated by Kirk cmd=12
    	offset1 += 60;
    	unknownOutput1.clear(offset1, 16); // certificate read from IdStorage (key=0x100, offset=0xF0)
    	offset1 += 16;
    	unknownOutput1.clear(192 - offset1); // clearing the remaining buffer
    	RuntimeContext.debugMemory(unknownOutput1.getAddress(), 192);

    	unknownOutput2.clear(160);
    	RuntimeContext.debugMemory(unknownOutput2.getAddress(), 160);

    	return 0;
    }

    /**
     * Method called to process a control message during initial negotiation done by sceNetAdhocAuth.
     * This method is called when receiving a message having
     * - protocolType == WLAN_PROTOCOL_TYPE_SONY
     * - protocolSubType == WLAN_PROTOCOL_SUBTYPE_CONTROL
     * - controlType == 3
     * It generates a new message that will be sent with controlType = 4.
     * Basically, it is receiving a certificate and public key from another PSP
     * and is generating random private & public keys which will be used for the
     * peer-to-peer communication with that PSP.
     * 
     * @param keysOutput
     * @param encryptedInputMessage
     * @param encryptedResponseMessage
     * @return
     */
    @HLEUnimplemented
    @HLEFunction(nid = 0x9BF0C95D, version = 150)
    public int sceAdhocBBAuth2(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=192, usage=Usage.out) TPointer keysOutput, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=160, usage=Usage.in) TPointer encryptedInputMessage, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=272, usage=Usage.out) TPointer encryptedResponseMessage) {
    	// Receiving certificate & public key from other PSP, generating random private & public keys and
    	// generating a response message
    	final int keyseed = 2;

    	byte[] encryptedMessage = encryptedInputMessage.getArray8(160);
    	byte[] decryptedMessage = new byte[encryptedMessage.length];
    	int result = decrypt(decryptedMessage, encryptedMessage, keyseed);
    	if (result != 0) {
    		return result;
    	}

    	byte[] hash1 = new byte[20];
    	result = hash(hash1, decryptedMessage, 56, 56);

    	byte[] certificate = readCertificate();
    	if (certificate == null) {
    		return -1;
    	}

    	result = verify(certificate, 96, hash1, 0, decryptedMessage, 112);
    	if (result != 0) {
    		return result;
    	}

    	byte[] generatedPrivateKey = new byte[20];
    	byte[] generatedPublicKey = new byte[40];
    	result = genKeys(generatedPrivateKey, generatedPublicKey);
    	if (result != 0) {
    		return result;
    	}

    	byte[] multipliedPoint = new byte[40];
    	result = multiply(multipliedPoint, generatedPrivateKey, decryptedMessage, 16);
    	if (result != 0) {
    		return result;
    	}

    	byte[] inputHash2 = new byte[40 + 40 + 16];
    	System.arraycopy(generatedPublicKey, 0, inputHash2, 0, 40);
    	System.arraycopy(decryptedMessage, 16, inputHash2, 40, 40);
    	System.arraycopy(decryptedMessage, 56, inputHash2, 40 + 40, 16);
    	byte[] hash2 = new byte[20];
    	result = hash(hash2, inputHash2, 0, inputHash2.length);
    	if (result != 0) {
    		return result;
    	}

    	byte[] signature = new byte[40];
    	result = sign(signature, certificate, 0x88, hash2, 0);
    	if (result != 0) {
    		return result;
    	}

    	byte[] inputHash3 = new byte[20 + inputHash2.length];
    	System.arraycopy(multipliedPoint, 0, inputHash3, 0, 20);
    	System.arraycopy(inputHash2, 0, inputHash3, 20, inputHash2.length);
    	byte[] hash3 = new byte[20];
    	result = hash(hash3, inputHash3, 0, inputHash3.length);
    	if (result != 0) {
    		return result;
    	}

    	int offset = 0;
    	keysOutput.setArray(offset, decryptedMessage, 0x48, 40);
    	offset += 40;
    	keysOutput.setArray(offset, generatedPublicKey, 0, 40);
    	offset += 40;
    	keysOutput.setArray(offset, decryptedMessage, 0x10, 40);
    	offset += 40;
    	keysOutput.setArray(offset, certificate, 0, 16);
    	offset += 16;
    	keysOutput.setArray(offset, multipliedPoint, 0, 20);
    	offset += 20;
    	keysOutput.clear(offset, 192 - offset);

    	byte[] encryptInput = new byte[256];
    	offset = 0;
    	System.arraycopy(inputHash2, 0, encryptInput, offset, inputHash2.length);
    	offset += inputHash2.length;
    	System.arraycopy(certificate, 0x88, encryptInput, offset, 32);
    	offset += 32;
    	System.arraycopy(hash2, 0, encryptInput, offset, 8);
    	offset += 8;
    	System.arraycopy(hash2, 0, encryptInput, offset, 20);
    	offset += 20;
    	System.arraycopy(certificate, 0, encryptInput, offset, 96);
    	offset += 96;
    	// There are 4 bytes left
    	Arrays.fill(encryptInput, offset, encryptInput.length, (byte) 0);

    	byte[] encryptOutput = new byte[272];
    	result = encryptWithRandomData(encryptOutput, encryptInput, keyseed);
    	if (result != 0) {
    		return result;
    	}

    	encryptedResponseMessage.setArray(encryptOutput);

    	return 0;
    }

    /**
     * Method called to process a control message during initial negotiation done by sceNetAdhocAuth.
     * This method is called when receiving a message having
     * - protocolType == WLAN_PROTOCOL_TYPE_SONY
     * - protocolSubType == WLAN_PROTOCOL_SUBTYPE_CONTROL
     * - controlType == 4
     * It generates a new message that will be sent with controlType = 5.
     * 
     * @param unknownInputOuput
     * @param unknownInput
     * @param unknownOutput
     * @return
     */
    @HLEUnimplemented
    @HLEFunction(nid = 0xC3981EE1, version = 150)
    public int sceAdhocBBAuth3(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=192, usage=Usage.inout) TPointer unknownInputOuput, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=272, usage=Usage.in) TPointer unknownInput, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=256, usage=Usage.out) TPointer unknownOutput) {
    	unknownInputOuput.clear(192);
    	RuntimeContext.debugMemory(unknownInputOuput.getAddress(), 192);
    	RuntimeContext.debugMemory(unknownInput.getAddress(), 272);
    	unknownOutput.clear(256);
    	RuntimeContext.debugMemory(unknownOutput.getAddress(), 256);

    	return 0;
    }

    /**
     * Method called to process a control message during initial negotiation done by sceNetAdhocAuth.
     * This method is called when receiving a message having
     * - protocolType == WLAN_PROTOCOL_TYPE_SONY
     * - protocolSubType == WLAN_PROTOCOL_SUBTYPE_CONTROL
     * - controlType == 6
     * This is the final message being sent during the negotiation.
     * 
     * @param unknownOutput
     * @param unknownInput
     * @return
     */
    @HLEUnimplemented
    @HLEFunction(nid = 0x8ABE3445, version = 150)
    public int sceAdhocBBAuth5(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=192, usage=Usage.out) TPointer unknownOutput, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=96, usage=Usage.in) TPointer unknownInput) {
    	unknownOutput.clear(192);
    	RuntimeContext.debugMemory(unknownOutput.getAddress(), 192);
    	RuntimeContext.debugMemory(unknownInput.getAddress(), 96);

    	return 0;
    }

    /**
     * Method called to process a control message during initial negotiation done by sceNetAdhocAuth.
     * This method is called when receiving a message having
     * - protocolType == WLAN_PROTOCOL_TYPE_SONY
     * - protocolSubType == WLAN_PROTOCOL_SUBTYPE_CONTROL
     * - controlType == 5
     * It generates a new message that will be sent with controlType = 6.
     * 
     * @param unknownInputOutput1
     * @param unknownInputOutput2
     * @param unknownOutput
     * @return
     */
    @HLEUnimplemented
    @HLEFunction(nid = 0x23E4659B, version = 150)
    public int sceAdhocBBAuth4(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=192, usage=Usage.inout) TPointer unknownInputOutput1, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=256, usage=Usage.inout) TPointer unknownInputOutput2, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=96, usage=Usage.out) TPointer unknownOutput) {
    	unknownInputOutput1.clear(192);
    	RuntimeContext.debugMemory(unknownInputOutput1.getAddress(), 192);
    	unknownInputOutput2.clear(256);
    	RuntimeContext.debugMemory(unknownInputOutput2.getAddress(), 256);
    	unknownOutput.clear(96);
    	RuntimeContext.debugMemory(unknownOutput.getAddress(), 96);

    	return 0;
    }

    /**
     * Method called to decrypt a control or data message sent to the MAC address FF:FF:FF:FF:FF:FF.
     * This method is called when receiving a message in one of the following 2 cases:
     * - protocolType == WLAN_PROTOCOL_TYPE_SONY
     * - protocolSubType == WLAN_PROTOCOL_SUBTYPE_DATA
     * - destMacAddress == FF:FF:FF:FF:FF:FF
     * or
     * - protocolType == WLAN_PROTOCOL_TYPE_SONY
     * - protocolSubType == WLAN_PROTOCOL_SUBTYPE_CONTROL
     * - controlType == 7
     * - destMacAddress == FF:FF:FF:FF:FF:FF
     * 
     * @param unknownInputOutput
     * @param unknownInput
     * @param inputLength
     * @param unknownOutput
     * @param unknown1
     * @param unknown2
     * @return
     */
    @HLEUnimplemented
    @HLEFunction(nid = 0x4DF566D3, version = 271)
    @HLEFunction(nid = 0xCB5D3916, version = 661)
    public int sceMemab_driver_CB5D3916(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=60, usage=Usage.inout) TPointer unknownInputOutput, @BufferInfo(lengthInfo=LengthInfo.nextParameter, usage=Usage.in) TPointer unknownInput, int inputLength, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=32, usage=Usage.out) TPointer unknownOutput, int unknown1, int unknown2) {
    	unknownInputOutput.clear(60);
    	RuntimeContext.debugMemory(unknownInputOutput.getAddress(), 60);
    	RuntimeContext.debugMemory(unknownInput.getAddress(), inputLength);
    	unknownOutput.clear(32);
    	RuntimeContext.debugMemory(unknownOutput.getAddress(), 32);

    	return 0;
    }

    /**
     * Method called to decrypt a control or data message sent to a single MAC address
     * (i.e. not sent to a multicast MAC address).
     * This method is called when receiving a message in one of the following 3 cases:
     * - protocolType == WLAN_PROTOCOL_TYPE_SONY
     * - protocolSubType == WLAN_PROTOCOL_SUBTYPE_DATA
     * - destMacAddress == non-multicast MAC address
     * or
     * - protocolType == WLAN_PROTOCOL_TYPE_SONY
     * - protocolSubType == WLAN_PROTOCOL_SUBTYPE_CONTROL
     * - controlType == 8
     * or
     * - protocolType == WLAN_PROTOCOL_TYPE_SONY
     * - protocolSubType == WLAN_PROTOCOL_SUBTYPE_CONTROL
     * - controlType == 7
     * - destMacAddress != FF:FF:FF:FF:FF:FF
     * 
     * @param unknownInputOutput
     * @param unknownInput
     * @param inputLength
     * @param unknownOutput
     * @param unknown1
     * @param unknown2
     * @return
     */
    @HLEUnimplemented
    @HLEFunction(nid = 0xB54E1AA7, version = 271)
    @HLEFunction(nid = 0xD47A50B1, version = 661)
    public int sceMemab_driver_D47A50B1(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=76, usage=Usage.inout) TPointer unknownInputOutput, @BufferInfo(lengthInfo=LengthInfo.nextParameter, usage=Usage.in) TPointer unknownInput, int inputLength, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=32, usage=Usage.out) TPointer unknownOutput, int unknown1, int unknown2) {
    	unknownInputOutput.clear(76);
    	RuntimeContext.debugMemory(unknownInputOutput.getAddress(), 76);
    	RuntimeContext.debugMemory(unknownInput.getAddress(), inputLength);
    	unknownOutput.clear(32);
    	RuntimeContext.debugMemory(unknownOutput.getAddress(), 32);

    	return 0;
    }

    /**
     * Method called to encrypt a control or data message sent to a single MAC address.
     * (i.e. not sent to a multicast MAC address).
     * This method is called when sending a message having
     * - protocolType == WLAN_PROTOCOL_TYPE_SONY
     * - protocolSubType == WLAN_PROTOCOL_SUBTYPE_DATA
     * - destMacAddress == non-multicast MAC address
     * or
     * - protocolType == WLAN_PROTOCOL_TYPE_SONY
     * - protocolSubType == WLAN_PROTOCOL_SUBTYPE_CONTROL
     * - controlType == 8
     * or
     * - protocolType == WLAN_PROTOCOL_TYPE_SONY
     * - protocolSubType == WLAN_PROTOCOL_SUBTYPE_CONTROL
     * - controlType == 7
     * - destMacAddress != FF:FF:FF:FF:FF:FF
     * 
     * @param unknownInputOutput
     * @param unknownInput
     * @param inputLength
     * @param unknownInput2
     * @param unknown2
     * @return
     */
    @HLEUnimplemented
    @HLEFunction(nid = 0xE6D3ACE2, version = 271)
    @HLEFunction(nid = 0x3C15BC8C, version = 661)
    public int sceMemab_driver_3C15BC8C(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=68, usage=Usage.inout) TPointer unknownInputOutput, @BufferInfo(lengthInfo=LengthInfo.nextParameter, usage=Usage.in) TPointer unknownInput, int inputLength, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=16, usage=Usage.in) TPointer unknownInput2, int unknown2) {
    	unknownInputOutput.clear(68);
    	RuntimeContext.debugMemory(unknownInputOutput.getAddress(), 68);
    	RuntimeContext.debugMemory(unknownInput.getAddress(), inputLength);
    	RuntimeContext.debugMemory(unknownInput2.getAddress(), 16);

    	return 0;
    }

    /**
     * Generates 4 pseudo-random numbers (PSP_KIRK_CMD_PRNG).
     * Called by sceNetAdhocAuth_lib_312BD812.
     * 
     * @param buffer
     * @return
     */
    @HLEUnimplemented
    @HLEFunction(nid = 0x16594684, version = 150)
    public int sceAdhocBBGMCreateSessionKey(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=16, usage=Usage.out) TPointer buffer) {
    	for (int i = 0; i < 4; i++) {
    		buffer.setValue32(i << 2, random.nextInt());
    	}

    	return 0;
    }

    /**
     * Encrypting GameMode data.
     * Called by sceNetAdhocAuth_lib_AAB06250.
     * 
     * @param xorKey
     * @param buffer
     * @param bufferLength
     * @return
     */
    @HLEUnimplemented
    @HLEFunction(nid = 0x9DE8C8CD, version = 150)
    public int sceAdhocBBGMEncrypt(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=16, usage=Usage.in) TPointer xorKey, @BufferInfo(lengthInfo=LengthInfo.nextParameter, usage=Usage.inout) TPointer buffer, int bufferLength) {
    	RuntimeContext.debugMemory(buffer.getAddress(), bufferLength);

    	return 0;
    }

    /**
     * Decrypting GameMode data.
     * Called by sceNetAdhocAuth_lib_015A8A64.
     * 
     * @param xorKey
     * @param buffer
     * @param bufferLength
     * @return
     */
    @HLEUnimplemented
    @HLEFunction(nid = 0x9BF1A0A4, version = 150)
    public int sceAdhocBBGMDecrypt(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=16, usage=Usage.in) TPointer xorKey, @BufferInfo(lengthInfo=LengthInfo.nextParameter, usage=Usage.inout) TPointer buffer, int bufferLength) {
    	RuntimeContext.debugMemory(buffer.getAddress(), bufferLength);

    	return 0;
    }
}
