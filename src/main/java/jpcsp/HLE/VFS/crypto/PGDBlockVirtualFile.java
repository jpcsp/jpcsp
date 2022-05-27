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
package jpcsp.HLE.VFS.crypto;

import static jpcsp.crypto.PGD.PGD_MAGIC;
import static jpcsp.util.Utilities.readUnaligned32;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

import jpcsp.HLE.TPointer;
import jpcsp.HLE.VFS.AbstractProxyVirtualFile;
import jpcsp.HLE.VFS.IVirtualFile;
import jpcsp.crypto.CryptoEngine;
import jpcsp.crypto.PGD;
import jpcsp.util.Utilities;

public class PGDBlockVirtualFile extends AbstractProxyVirtualFile {
	private static final int pgdHeaderSize = 0x90;
	private byte[] key;
	private int dataOffset;
	private int dataSize;
	private int blockSize;
	private boolean headerValid;
	private boolean headerPresent;
	private byte[] buffer;
	private byte[] header;
	private PGD pgd;
	private boolean sequentialRead;
	private int headerMode;

	public PGDBlockVirtualFile(IVirtualFile pgdFile, byte[] key, int dataOffset) {
		super(pgdFile);

		if (key != null) {
			this.key = key.clone();
		}
		this.dataOffset = dataOffset;

		super.ioLseek(dataOffset);

		pgd = new CryptoEngine().getPGDEngine();

		readHeader();

		this.dataOffset += dataOffset;
	}

	private void readHeader() {
		headerValid = false;
		headerPresent = false;
		byte[] inBuf = new byte[pgdHeaderSize];
		super.ioRead(inBuf, 0, pgdHeaderSize);

		// Check if the "PGD" header is present
		int magic = readUnaligned32(inBuf, 0);
        if (magic != PGD_MAGIC) {
            // No "PGD" found in the header,
            log.warn(String.format("No PGD header detected 0x%08X ('%c%c%c%c') detected in file '%s'", magic, (char) inBuf[0], (char) inBuf[1], (char) inBuf[2], (char) inBuf[3], vFile));
            return;
        }
        headerPresent = true;

        // Decrypt 0x30 bytes at offset 0x30 to expose the first header.
        byte[] headerBuf = new byte[0x30 + 0x10];
        System.arraycopy(inBuf, 0x10, headerBuf, 0, 0x10);
        System.arraycopy(inBuf, 0x30, headerBuf, 0x10, 0x30);
        if (key == null) {
        	key = pgd.GetEDATPGDKey(inBuf, pgdHeaderSize);
        	if (log.isDebugEnabled()) {
        		log.debug(String.format("Using PGD key from EDAT: %s", Utilities.getMemoryDump(key)));
        	}
        }
        int version = readUnaligned32(inBuf, 4);
        if (version != 1) {
        	log.error(String.format("Unimplemented PGD version=%d", version));
        }
        headerMode = readUnaligned32(inBuf, 8);
        header = pgd.DecryptPGD(headerBuf, headerBuf.length, key, 0, headerMode);

        // Extract the decryption parameters.
        IntBuffer decryptedHeader = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN).asIntBuffer();
        dataSize = decryptedHeader.get(5);
        blockSize = decryptedHeader.get(6);
        dataOffset = decryptedHeader.get(7);
        if (log.isDebugEnabled()) {
            log.debug(String.format("PGD dataSize=%d, blockSize=%d, dataOffset=%d", dataSize, blockSize, dataOffset));
            if (log.isTraceEnabled()) {
                log.trace(String.format("PGD Header: %s", Utilities.getMemoryDump(inBuf, 0, pgdHeaderSize)));
                log.trace(String.format("Decrypted PGD Header: %s", Utilities.getMemoryDump(header, 0, header.length)));
            }
        }

        if (dataOffset < 0 || dataOffset > super.length() || dataSize < 0) {
            // The decrypted PGD header is incorrect...
            log.warn(String.format("Incorrect PGD header: dataSize=%d, chunkSize=%d, hashOffset=%d", dataSize, blockSize, dataOffset));
            return;
        }

        buffer = new byte[blockSize + 0x10];

        headerValid = true;
        sequentialRead = false;
	}

	public int getBlockSize() {
		return blockSize;
	}

	public boolean isHeaderValid() {
		return headerValid;
	}

	public boolean isHeaderPresent() {
		return headerPresent;
	}

	@Override
	public int ioRead(byte[] outputBuffer, int outputOffset, int outputLength) {
		int seed = 0;
		if (!sequentialRead) {
			seed = (int) (getPosition() >> 4);
		}

		int readLength = blockSize;
		super.ioRead(buffer, 0x10, readLength);
		System.arraycopy(header, 0, buffer, 0, 0x10);

        byte[] decryptedBytes;
        if (sequentialRead) {
        	// This operation is more efficient than a complete DecryptPGD() call
        	// but can only be used when decrypting sequential packets.
        	decryptedBytes = pgd.UpdatePGDCipher(buffer, readLength + 0x10);
        } else {
        	pgd.FinishPGDCipher();
        	decryptedBytes = pgd.DecryptPGD(buffer, readLength + 0x10, key, seed, headerMode);
        	sequentialRead = true;
        }
        int length = Math.min(outputLength, decryptedBytes.length);
        System.arraycopy(decryptedBytes, 0, outputBuffer, outputOffset, length);

        if (log.isTraceEnabled()) {
        	log.trace(String.format("PGDBlockVirtualFile.ioRead length=0x%X: %s", length, Utilities.getMemoryDump(decryptedBytes, 0, length)));
        }

        return length;
	}

	@Override
	public int ioRead(TPointer outputPointer, int outputLength) {
		return ioReadBuf(outputPointer, outputLength);
	}

	@Override
	public long ioLseek(long offset) {
		long result = super.ioLseek(dataOffset + offset);
		if (result >= 0 && result >= dataOffset) {
			result -= dataOffset;
		}

		if (log.isTraceEnabled()) {
			log.trace(String.format("PGDBlockVirtualFile.ioLseek offset=0x%X, result=0x%X", offset, result));
		}

		sequentialRead = false;

		return result;
	}

	@Override
	public long getPosition() {
		long position = super.getPosition();
		if (position >= 0 && position >= dataOffset) {
			position -= dataOffset;
		}
		return position;
	}

	@Override
	public long length() {
		return dataSize;
	}
}
