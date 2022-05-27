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
package libkirk;

import static libkirk.Utilities.alignUp;
import static libkirk.Utilities.log;
import static libkirk.Utilities.memcpy;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import libkirk.KirkEngine.header_keys;

public class AES {
	/* for 256-bit keys, fewer for less */
	public static final int AES_MAXROUNDS = 14;
	private static boolean inInit = false;
    private static volatile Cipher cipher;

    //CMAC GLOBS
    private static final byte[] const_Rb = {
    	(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
    	(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x87
    };
    //END

	public static class rijndael_ctx {
		public int enc_only;		/* context contains only encrypt schedule */
		public int Nr;			/* key-length-dependent number of rounds */
		public final int[] ek = new int[4*(AES_MAXROUNDS + 1)];	/* encrypt key schedule */
		public final int[] dk = new int[4*(AES_MAXROUNDS + 1)];	/* decrypt key schedule */
        protected Key keySpec;
	}

	public static class AES_ctx	extends rijndael_ctx {
	}

	public static void init() {
    	if (cipher == null && !inInit) {
    		inInit = true;
	    	// Run in a background thread as the initialization is taking around 300 milliseconds
	    	Thread staticInit = new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						cipher = Cipher.getInstance("AES/CBC/NoPadding");
						inInit = false;
					} catch (NoSuchAlgorithmException e) {
						log.error("AES Cipher init", e);
					} catch (NoSuchPaddingException e) {
						log.error("AES Cipher init", e);
					}
				}
			});
	    	staticInit.start();
    	}
    }

    public static int rijndael_set_key(rijndael_ctx ctx, byte[] key, int bits) {
    	return rijndael_set_key(ctx, key, 0, bits);
    }

    public static int rijndael_set_key(rijndael_ctx ctx, byte[] key, int keyoffset, int bits) {
		ctx.keySpec = new SecretKeySpec(key, keyoffset, bits / 8, "AES");

		return 0;
    }

    public static int AES_set_key(AES_ctx ctx, byte[] key, int bits) {
    	return rijndael_set_key(ctx, key, bits);
	}

    private static void xor_128(byte[] a, byte[] b, byte[] out) {
    	xor_128(a, 0, b, 0, out, 0);
    }

    private static void xor_128(byte[] a, int offset1, byte[] b, byte[] out) {
    	xor_128(a, offset1, b, 0, out, 0);
    }

    private static void xor_128(byte[] a, int offset1, byte[] b, int offset2, byte[] out, int outOffset) {
    	for (int i = 0; i < 16; i++) {
    		out[i + outOffset] = (byte) (a[i + offset1] ^ b[i + offset2]);
    	}
    }

    private static void doCipher(rijndael_ctx ctx, int mode, byte[] src, int srcOffset, byte[] dst, int dstOffset, int size) {
    	while (cipher == null) {
    		//
    	}

    	byte[] iv = new byte[0x10];
        IvParameterSpec ivec = new IvParameterSpec(iv);

        try {
            cipher.init(mode, ctx.keySpec, ivec);
            // If the size is not a multiple of 16 bytes,
            // padding bytes are read in src past the given size.
            // It is required to really read the padding data present in src
            // and not reading padding 0's instead.
            cipher.doFinal(src, srcOffset, alignUp(size, 15), dst, dstOffset);
        } catch (InvalidKeyException e) {
        	log.error("doCipher", e);
        } catch (InvalidAlgorithmParameterException e) {
        	log.error("doCipher", e);
		} catch (IllegalBlockSizeException e) {
			log.error("doCipher", e);
		} catch (BadPaddingException e) {
			log.error("doCipher", e);
		} catch (ShortBufferException e) {
			log.error("doCipher", e);
		} catch (IllegalArgumentException e) {
			log.error("doCipher", e);
		}
    }

	public static void rijndael_encrypt(rijndael_ctx ctx, byte[] src, byte[] dst) {
		rijndael_encrypt(ctx, src, 0, dst, 0);
	}

    public static void rijndael_encrypt(rijndael_ctx ctx, byte[] src, int srcOffset, byte[] dst, int dstOffset) {
    	rijndael_encrypt(ctx, src, srcOffset, dst, dstOffset, 16);
    }

    private static void rijndael_encrypt(rijndael_ctx ctx, byte[] src, int srcOffset, byte[] dst, int dstOffset, int size) {
    	doCipher(ctx, Cipher.ENCRYPT_MODE, src, srcOffset, dst, dstOffset, size);
	}

	//No IV support!
    public static void AES_cbc_encrypt(AES_ctx ctx, byte[] src, byte[] dst, int size) {
		AES_cbc_encrypt(ctx, src, 0, dst, 0, size);
	}

	public static void AES_cbc_encrypt(AES_ctx ctx, byte[] src, int srcOffset, byte[] dst, int dstOffset, int size) {
		rijndael_encrypt(ctx, src, srcOffset, dst, dstOffset, size);
	}

	public static void AES_cbc_decrypt(AES_ctx ctx, byte[] src, byte[] dst, int size) {
		AES_cbc_decrypt(ctx, src, 0, dst, 0, size);
	}

	public static void AES_cbc_decrypt(AES_ctx ctx, byte[] src, int srcOffset, header_keys dst, int size) {
		final byte[] buffer = new byte[header_keys.SIZEOF];
		AES_cbc_decrypt(ctx, src, srcOffset, buffer, 0, size);
		dst.read(buffer, 0);
	}

	public static void rijndael_decrypt(rijndael_ctx ctx, byte[] src, byte[] dst) {
		rijndael_decrypt(ctx, src, 0, dst, 0);
	}

	private static void rijndael_decrypt(rijndael_ctx ctx, byte[] src, int srcOffset, byte[] dst, int dstOffset) {
		rijndael_decrypt(ctx, src, srcOffset, dst, dstOffset, 16);
	}

	private static void rijndael_decrypt(rijndael_ctx ctx, byte[] src, int srcOffset, byte[] dst, int dstOffset, int size) {
		doCipher(ctx, Cipher.DECRYPT_MODE, src, srcOffset, dst, dstOffset, size);
    }

	public static void AES_cbc_decrypt(AES_ctx ctx, byte[] src, int srcOffset, byte[] dst, int dstOffset, int size) {
		rijndael_decrypt(ctx, src, srcOffset, dst, dstOffset, size);
	}

	public static void AES_encrypt(AES_ctx ctx, byte[] src, byte[] dst) {
		AES_encrypt(ctx, src, 0, dst, 0);
	}

	public static void AES_encrypt(AES_ctx ctx, byte[] src, int srcOffset, byte[] dst, int dstOffset) {
		AES_cbc_encrypt(ctx, src, srcOffset, dst, dstOffset, 16);
	}

	/* AES-CMAC Generation Function */

	private static void leftshift_onebit(byte[] input, byte[] output) {
		leftshift_onebit(input, 0, output, 0);
	}

	private static void leftshift_onebit(byte[] input, int inputOffset, byte[] output, int outputOffset) {
	    byte overflow = 0;

		for (int i = 15; i >= 0; i--) {
			output[i + outputOffset] = (byte) (input[i + inputOffset] << 1);
			output[i + outputOffset] |= overflow;
			overflow = ((input[i + inputOffset] & 0x80) != 0) ? (byte) 1 : (byte) 0;
		}
	}

	private static void generate_subkey(AES_ctx ctx, byte[] K1, byte[] K2) {
	    final byte[] L = new byte[16];
	    final byte[] Z = new byte[16];
	    final byte[] tmp = new byte[16];

		//for ( i=0; i<16; i++ ) Z[i] = 0;
		
		AES_encrypt(ctx, Z, L);

		if ((L[0] & 0x80) == 0) { /* If MSB(L) = 0, then K1 = L << 1 */
			leftshift_onebit(L, K1);
		} else {    /* Else K1 = ( L << 1 ) (+) Rb */
	        leftshift_onebit(L, tmp);
	        xor_128(tmp, const_Rb, K1);
	    }

		if ((K1[0] & 0x80) == 0) {
	        leftshift_onebit(K1, K2);
	    } else {
	        leftshift_onebit(K1, tmp);
	        xor_128(tmp, const_Rb, K2);
	    }
	}

	private static void padding(byte[] lastb, int lastbOffset, byte[] pad, int length) {
		padding(lastb, lastbOffset, pad, 0, length);
	}

	private static void padding(byte[] lastb, int lastbOffset, byte[] pad, int padOffset, int length) {
		/* original last block */
		for (int j= 0 ; j < 16; j++) {
			if (j < length) {
	            pad[j + padOffset] = lastb[j + lastbOffset];
	        } else if (j == length) {
	            pad[j + padOffset] = (byte) 0x80;
	        } else {
	            pad[j + padOffset] = (byte) 0x00;
	        }
		}
	}

	public static void AES_CMAC(AES_ctx ctx, byte[] input, int inputOffset, int length, byte[] mac) {
	    final byte[] X = new byte[16];
	    final byte[] Y = new byte[16];
	    final byte[] M_last = new byte[16];
	    final byte[] padded = new byte[16];
	    final byte[] K1 = new byte[16];
	    final byte[] K2 = new byte[16];
	    boolean flag;
	    generate_subkey(ctx,K1,K2);

	    int n = (length + 15) / 16;       /* n is number of rounds */

	    if (n == 0) {
	        n = 1;
	        flag = false;
	    } else {
	    	/* last block is a complete block? */
	    	flag = (length % 16) == 0;
	    }

	    if (flag) { /* last block is complete block */
	        xor_128(input, inputOffset + 16 * (n - 1), K1, M_last);
	    } else {
	        padding(input, inputOffset + 16 * (n - 1), padded, length % 16);
	        xor_128(padded, K2, M_last);
	    }

	    //for ( i=0; i<16; i++ ) X[i] = 0;
	    for (int i = 0; i < n - 1; i++) {
	        xor_128(input, inputOffset + 16*i, X, Y); /* Y := Mi (+) X  */
			AES_encrypt(ctx, Y, X); /* X := AES-128(KEY, Y); */
	    }

	    xor_128(X, M_last, Y);
	    AES_encrypt(ctx, Y, X);

	    memcpy(mac, X, 16);
	}
}
