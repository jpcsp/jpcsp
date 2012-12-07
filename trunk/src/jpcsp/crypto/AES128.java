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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.security.Key;
import java.security.Security;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import jpcsp.HLE.Modules;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class AES128 {

    private static byte[] const_Zero = {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
    private static byte[] const_Rb = {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, (byte) 0x87};
    private byte[] contentKey;
    private ByteArrayOutputStream barros;
    private static Cipher cipher;

    private static void init() {
    	if (cipher == null) {
            Security.addProvider(new BouncyCastleProvider());
    		try {
				cipher = Cipher.getInstance("AES/CBC/NoPadding", "BC");
			} catch (Exception e) {
				Modules.log.error("AES128 Cipher", e);
			}
    	}
    }

    public AES128() {
    	init();
    }

    // Private encrypting method for CMAC (IV == 0).
    private static byte[] encrypt(byte[] in, byte[] encKey) {
        Key keySpec = new SecretKeySpec(encKey, "AES");
        byte[] iv = {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
        IvParameterSpec ivec = new IvParameterSpec(iv);
        try {
            Cipher c = cipher;
            c.init(Cipher.ENCRYPT_MODE, keySpec, ivec);
            ByteArrayInputStream inStream = new ByteArrayInputStream(in);
            CipherInputStream cIn = new CipherInputStream(inStream, c);
            DataInputStream dIn = new DataInputStream(cIn);
            byte[] bytes = new byte[in.length];
            for (int i = 0; i < bytes.length; i++) {
                bytes[i] = (byte) dIn.read();
            }
            dIn.close();
            return bytes;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // Public encrypting/decrypting methods (for CryptoEngine calls).
    public byte[] encryptCBC(byte[] in, byte[] encKey, byte[] iv) {
        Key keySpec = new SecretKeySpec(encKey, "AES");
        IvParameterSpec ivec = new IvParameterSpec(iv);
        try {
            Cipher c = cipher;
            c.init(Cipher.ENCRYPT_MODE, keySpec, ivec);
            ByteArrayInputStream inStream = new ByteArrayInputStream(in);
            CipherInputStream cIn = new CipherInputStream(inStream, c);
            DataInputStream dIn = new DataInputStream(cIn);
            byte[] bytes = new byte[in.length];
            for (int i = 0; i < bytes.length; i++) {
                bytes[i] = (byte) dIn.read();
            }
            dIn.close();
            return bytes;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public byte[] decryptCBC(byte[] in, byte[] decKey, byte[] iv) {
        Key keySpec = new SecretKeySpec(decKey, "AES");
        IvParameterSpec ivec = new IvParameterSpec(iv);
        try {
            Cipher c = cipher;
            c.init(Cipher.DECRYPT_MODE, keySpec, ivec);
            return c.doFinal(in);
        } catch (Exception e) {
        	Modules.log.error("decryptCBC", e);
            return null;
        }
    }

    public void doInitCMAC(byte[] contentKey) {
        this.contentKey = contentKey;
        barros = new ByteArrayOutputStream();
    }

    public void doUpdateCMAC(byte[] input, int offset, int len) {
        barros.write(input, offset, len);
    }

    public void doUpdateCMAC(byte[] input) {
        barros.write(input, 0, input.length);
    }

    public byte[] doFinalCMAC() {
        Object[] keys = generateSubKey(contentKey);
        byte[] K1 = (byte[]) keys[0];
        byte[] K2 = (byte[]) keys[1];

        byte[] input = barros.toByteArray();

        int numberOfRounds = (input.length + 15) / 16;
        boolean lastBlockComplete;

        if (numberOfRounds == 0) {
            numberOfRounds = 1;
            lastBlockComplete = false;
        } else {
            if (input.length % 16 == 0) {
                lastBlockComplete = true;
            } else {
                lastBlockComplete = false;
            }
        }

        byte[] M_last;
        int srcPos = 16 * (numberOfRounds - 1);

        if (lastBlockComplete) {
            byte[] partInput = new byte[16];

            System.arraycopy(input, srcPos, partInput, 0, 16);
            M_last = xor128(partInput, K1);
        } else {
            byte[] partInput = new byte[input.length % 16];

            System.arraycopy(input, srcPos, partInput, 0, input.length % 16);
            byte[] padded = doPaddingCMAC(partInput);
            M_last = xor128(padded, K2);
        }

        byte[] X = const_Zero.clone();
        byte[] partInput = new byte[16];
        byte[] Y;

        for (int i = 0; i < numberOfRounds - 1; i++) {
            srcPos = 16 * i;
            System.arraycopy(input, srcPos, partInput, 0, 16);

            Y = xor128(partInput, X); /* Y := Mi (+) X */
            X = encrypt(Y, contentKey);
        }

        Y = xor128(X, M_last);
        X = encrypt(Y, contentKey);

        return X;
    }

    public boolean doVerifyCMAC(byte[] verificationCMAC) {
        byte[] cmac = doFinalCMAC();

        if (verificationCMAC == null || verificationCMAC.length != cmac.length) {
            return false;
        }

        for (int i = 0; i < cmac.length; i++) {
            if (cmac[i] != verificationCMAC[i]) {
                return false;
            }
        }

        return true;
    }

    private byte[] doPaddingCMAC(byte[] input) {
        byte[] padded = new byte[16];

        for (int j = 0; j < 16; j++) {
            if (j < input.length) {
                padded[j] = input[j];
            } else if (j == input.length) {
                padded[j] = (byte) 0x80;
            } else {
                padded[j] = (byte) 0x00;
            }
        }

        return padded;
    }

    public static Object[] generateSubKey(byte[] key) {
        byte[] L = encrypt(const_Zero, key);

        byte[] K1 = null;
        if ((L[0] & 0x80) == 0) { /* If MSB(L) = 0, then K1 = L << 1 */
            K1 = doLeftShiftOneBit(L);
        } else {    /* Else K1 = ( L << 1 ) (+) Rb */
            byte[] tmp = doLeftShiftOneBit(L);
            K1 = xor128(tmp, const_Rb);
        }

        byte[] K2 = null;
        if ((K1[0] & 0x80) == 0) {
            K2 = doLeftShiftOneBit(K1);
        } else {
            byte[] tmp = doLeftShiftOneBit(K1);
            K2 = xor128(tmp, const_Rb);
        }

        Object[] result = new Object[2];
        result[0] = K1;
        result[1] = K2;
        return result;
    }

    private static byte[] xor128(byte[] input1, byte[] input2) {
        byte[] output = new byte[input1.length];
        for (int i = 0; i < input1.length; i++) {
            output[i] = (byte) (((int) input1[i] ^ (int) input2[i]) & 0xFF);
        }
        return output;
    }

    private static byte[] doLeftShiftOneBit(byte[] input) {
        byte[] output = new byte[input.length];
        byte overflow = 0;

        for (int i = (input.length - 1); i >= 0; i--) {
            output[i] = (byte) ((int) input[i] << 1 & 0xFF);
            output[i] |= overflow;
            overflow = ((input[i] & 0x80) != 0) ? (byte) 1 : (byte) 0;
        }

        return output;
    }
}