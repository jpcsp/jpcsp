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

import static java.lang.System.arraycopy;
import static libkirk.Utilities.log;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class SHA1 {
	public static final int SHS_DIGESTSIZE = 20;

	public static class SHA_CTX {
		public final int[] digest = new int[5]; /* Message digest */
		public int countLo, countHi;            /* 64-bit bit count */
		public final int[] data = new int[16];  /* SHS data buffer */
		public int Endianness;
        private MessageDigest md;
	}

	public static void SHAInit(SHA_CTX shsInfo) {
		try {
			shsInfo.md = MessageDigest.getInstance("SHA-1");
		} catch (NoSuchAlgorithmException e) {
			log.error("SHAInit", e);
		}
	}

	public static void SHAUpdate(SHA_CTX shsInfo, byte[] buffer, int count) {
        SHAUpdate(shsInfo, buffer, 0, count);
	}

	public static void SHAUpdate(SHA_CTX shsInfo, byte[] buffer, int offset, int count) {
        shsInfo.md.update(buffer, offset, count);
	}

	public static void SHAFinal(byte[] output, SHA_CTX shsInfo) {
		SHAFinal(output, 0, shsInfo);
	}

	public static void SHAFinal(byte[] output, int offset, SHA_CTX shsInfo) {
		byte[] digest = shsInfo.md.digest();
		arraycopy(digest, 0, output, offset, SHS_DIGESTSIZE);
	}
}
