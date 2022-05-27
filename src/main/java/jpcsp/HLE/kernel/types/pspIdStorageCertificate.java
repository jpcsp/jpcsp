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
package jpcsp.HLE.kernel.types;

import libkirk.KirkEngine;

public class pspIdStorageCertificate extends pspAbstractMemoryMappedStructure {
	// [0x00..0x0F]: unknown, size 16 bytes, returned by sceMemab_4B54EAAD
	// [0x10..0x37]: ??? (0x00..0x37 are being hashed and the output is used as message_hash for KIRK_CMD_ECDSA_VERIFY)
	// [0x38..0x5F]: signature, size 40 bytes, see KIRK_CMD_ECDSA_VERIFY
	// [0x60..0x87]: public_key, size 40 bytes, see KIRK_CMD_ECDSA_VERIFY
	// [0x88..0xA7]: enc_private, size 32 bytes, see KIRK_CMD_ECDSA_SIGN
	// [0xA8..0xB7]: ???
	public final byte[] hash = new byte[0x38];
	public final byte[] signature = new byte[0x28];
	public final byte[] publicKey = new byte[0x28];
	public final byte[] encryptedPrivateKey = new byte[0x20];
	public final byte[] unknown = new byte[0x10];

	@Override
	protected void read() {
		read8Array(hash);
		read8Array(signature);
		read8Array(publicKey);
		read8Array(encryptedPrivateKey);
		read8Array(unknown);
	}

	@Override
	protected void write() {
		write8Array(hash);
		write8Array(signature);
		write8Array(publicKey);
		write8Array(encryptedPrivateKey);
		write8Array(unknown);
	}

	public void setHash(byte[] hash) {
		System.arraycopy(hash, 0, this.hash, 0, this.hash.length);
	}

	public void setSignature(byte[] signature) {
		System.arraycopy(signature, 0, this.signature, 0, this.signature.length);
	}

	public void setPublicKey(byte[] publicKey) {
		System.arraycopy(publicKey, 0, this.publicKey, 0, this.publicKey.length);
	}

	public void encryptPrivateKey(byte[] decryptedPrivateKey) {
		// The decrypted private key needs to be encrypted using our own Fuse ID
		KirkEngine.encrypt_kirk16_private(encryptedPrivateKey, decryptedPrivateKey);
	}

	public void setUnknown(byte[] unknown) {
		System.arraycopy(unknown, 0, this.unknown, 0, this.unknown.length);
	}

	@Override
	public int sizeof() {
		return 0xB8;
	}
}
