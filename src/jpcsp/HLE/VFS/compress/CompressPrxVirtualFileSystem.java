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
package jpcsp.HLE.VFS.compress;

import static jpcsp.HLE.modules.IoFileMgrForUser.PSP_O_RDONLY;
import static jpcsp.format.PSP.AES_KEY_SIZE;
import static jpcsp.format.PSP.CHECK_SIZE;
import static jpcsp.format.PSP.CMAC_DATA_HASH_SIZE;
import static jpcsp.format.PSP.CMAC_HEADER_HASH_SIZE;
import static jpcsp.format.PSP.CMAC_KEY_SIZE;
import static jpcsp.format.PSP.KEY_DATA_SIZE;
import static jpcsp.format.PSP.PSP_HEADER_SIZE;
import static jpcsp.format.PSP.PSP_MAGIC;
import static jpcsp.format.PSP.SCE_KERNEL_MAX_MODULE_SEGMENT;
import static jpcsp.format.PSP.SHA1_HASH_SIZE;
import static jpcsp.util.Utilities.endianSwap32;
import static jpcsp.util.Utilities.readUnaligned16;
import static jpcsp.util.Utilities.readUnaligned32;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;

import jpcsp.HLE.VFS.AbstractProxyVirtualFileSystem;
import jpcsp.HLE.VFS.ByteArrayVirtualFile;
import jpcsp.HLE.VFS.IVirtualFile;
import jpcsp.HLE.VFS.IVirtualFileSystem;
import jpcsp.HLE.kernel.types.SceIoDirent;
import jpcsp.format.Elf32Header;
import jpcsp.util.Utilities;

/**
 * Virtual file system showing all the PRX files as compressed PRX's.
 * 
 * @author gid15
 *
 */
public class CompressPrxVirtualFileSystem extends AbstractProxyVirtualFileSystem {
	public CompressPrxVirtualFileSystem(IVirtualFileSystem vfs) {
		super(vfs);
	}

	private boolean isPrx(String fileName) {
		return fileName.toLowerCase().endsWith(".prx");
	}

	private boolean isKl4eCompression(String fileName) {
		if ("loadcore.prx".equalsIgnoreCase(fileName) || "kd/loadcore.prx".equalsIgnoreCase(fileName)) {
			return true;
		}
		if ("sysmem.prx".equalsIgnoreCase(fileName) || "kd/sysmem.prx".equalsIgnoreCase(fileName)) {
			return true;
		}

		return false;
	}

	private static void write8(OutputStream os, int value) throws IOException {
		os.write(value & 0xFF);
	}

	private static void write16(OutputStream os, int value) throws IOException {
		write8(os, value);
		write8(os, value >> 8);
	}

	private static void write32(OutputStream os, int value) throws IOException {
		write8(os, value);
		write8(os, value >> 8);
		write8(os, value >> 16);
		write8(os, value >> 24);
	}

	private static void writeString(OutputStream os, String value, int length) throws IOException {
		if (value != null) {
			int n = Math.min(value.length(), length);
			for (int i = 0; i < n; i++) {
				char c = value.charAt(i);
				write8(os, c);
			}
			length -= n;
		}

		while (length > 0) {
			length--;
			write8(os, 0);
		}
	}

	private static void writeBytes(OutputStream os, byte[] bytes, int length) throws IOException {
		if (bytes != null) {
			int n = Math.min(bytes.length, length);
			for (int i = 0; i < n; i++) {
				write8(os, bytes[i]);
			}
			length -= n;
		}

		while (length > 0) {
			length--;
			write8(os, 0);
		}
	}

	private void writePspHeader(OutputStream os, byte[] elfFile, String fileName) throws IOException {
		int bssSize = 0;
		int modInfoOffset = -1;
		int nSegments = 0;
		int type = readUnaligned16(elfFile, 16);
		int bootEntry = readUnaligned32(elfFile, 24);
		int phOffset = readUnaligned32(elfFile, 28);
		int phEntSize = readUnaligned16(elfFile, 42);
		int phNum = readUnaligned16(elfFile, 44);
		int[] segAlign = new int[SCE_KERNEL_MAX_MODULE_SEGMENT];
		int[] segAddress = new int[SCE_KERNEL_MAX_MODULE_SEGMENT];
		int[] segSize = new int[SCE_KERNEL_MAX_MODULE_SEGMENT];

		// Scan all the ELF program headers
		for (int i = 0; i < phNum; i++) {
			int offset = phOffset + i * phEntSize;
			int phType = readUnaligned32(elfFile, offset + 0);

			// Compute the BSS size
			int phEntFileSize = readUnaligned32(elfFile, offset + 16);
			int phEntMemSize = readUnaligned32(elfFile, offset + 20);
			if (phEntMemSize > phEntFileSize) {
				bssSize += phEntMemSize - phEntFileSize;
			}

			if (phType == 1 && nSegments < SCE_KERNEL_MAX_MODULE_SEGMENT) {
				segAlign[nSegments] = readUnaligned32(elfFile, offset + 28);
				segAddress[nSegments] = readUnaligned32(elfFile, offset + 8);
				segSize[nSegments] = phEntMemSize;

				if (type == Elf32Header.ET_SCE_PRX && nSegments == 0) {
					modInfoOffset = readUnaligned32(elfFile, offset + 12);
				}

				nSegments++;
			}
		}

		int compAttribute = 0x001; // SCE_EXEC_FILE_COMPRESSED
		boolean isKl4eCompressed = isKl4eCompression(fileName);
		if (isKl4eCompressed) {
			compAttribute |= 0x200; // SCE_EXEC_FILE_KL4E_COMPRESSED
		}

		write32(os, PSP_MAGIC); // Offset 0
		write16(os, 0x1007); // Offset 4, modAttribute (SCE_MODULE_KERNEL)
		write16(os, compAttribute); // Offset 6, compAttribute
		write8(os, 0); // Offset 8, moduleVerLo
		write8(os, 0); // Offset 9, moduleVerHi
		writeString(os, fileName, 28); // Offset 10, modName
		write8(os, 0); // Offset 38, modVersion
		write8(os, nSegments); // Offset 39, nSegments
		write32(os, elfFile.length); // Offset 40, elfSize
		write32(os, 0); // Offset 44, pspSize
		write32(os, bootEntry); // Offset 48, bootEntry
		write32(os, modInfoOffset); // Offset 52, modInfoOffset (must be < 0)
		write32(os, bssSize); // Offset 56, bssSize
		for (int i = 0; i < SCE_KERNEL_MAX_MODULE_SEGMENT; i++) {
			write16(os, segAlign[i]); // Offset 60-66, segAlign
		}
		for (int i = 0; i < SCE_KERNEL_MAX_MODULE_SEGMENT; i++) {
			write32(os, segAddress[i]); // Offset 68-80, segAddress
		}
		for (int i = 0; i < SCE_KERNEL_MAX_MODULE_SEGMENT; i++) {
			write32(os, segSize[i]); // Offset 84-96, segSize
		}
		for (int i = 0; i < 5; i++) {
			write32(os, 0); // Offset 100-116, reserved
		}
		write32(os, 0); // Offset 120, devkitVersion
		write8(os, 0); // Offset 124, decryptMode (DECRYPT_MODE_KERNEL_MODULE)
		write8(os, 0); // Offset 125, padding
		write16(os, 0); // Offset 126, overlapSize

		// Non-encrypted but compressed files having a short 128 bytes PSP header.
		// Excepted for sysmem.prx and loadcore.prx which are assumed to always be
		// KL4E compressed and encrypted. In those cases, the full 336 bytes
		// PSP header is present.
		if (isKl4eCompressed) {
			writeBytes(os, null, AES_KEY_SIZE); // Offset 128-143, aeskey
			writeBytes(os, null, CMAC_KEY_SIZE); // Offset 144-159, cmacKey
			writeBytes(os, null, CMAC_HEADER_HASH_SIZE); // Offset 160-175, cmacHeaderHash
			write32(os, 0); // Offset 176, compSize
			write32(os, 0); // Offset 180, unk180
			write32(os, 0); // Offset 184, unk184
			write32(os, 0); // Offset 188, unk188
			writeBytes(os, null, CMAC_DATA_HASH_SIZE); // Offset 192-207, cmacDataHash
			write32(os, 0); // Offset 208, tag
			writeBytes(os, null, CHECK_SIZE); // Offset 212-299, sCheck
			writeBytes(os, null, SHA1_HASH_SIZE); // Offset 300-319, sha1Hash
			writeBytes(os, null, KEY_DATA_SIZE); // Offset 320-335, keyData
		}
	}

	private void fixPspSizeInHeader(byte[] bytes) {
		int pspSize = bytes.length;
		Utilities.writeUnaligned32(bytes, 44, pspSize);
	}

	private byte[] getCompressedPrxFile(String dirName, String fileName) {
		String proxyFileName;
		if (dirName == null || dirName.length() == 0) {
			proxyFileName = fileName;
		} else {
			proxyFileName = dirName + "/" + fileName;
		}

		IVirtualFile vFileUncompressed = super.ioOpen(proxyFileName, PSP_O_RDONLY, 0);
		if (vFileUncompressed == null) {
			return null;
		}
		byte[] bufferUncompressed = Utilities.readCompleteFile(vFileUncompressed);
		vFileUncompressed.ioClose();
		if (bufferUncompressed == null) {
			return null;
		}
		int lengthUncompressed = bufferUncompressed.length;

		ByteArrayOutputStream osCompressed = new ByteArrayOutputStream(PSP_HEADER_SIZE + 9 + lengthUncompressed);
		try {
			writePspHeader(osCompressed, bufferUncompressed, fileName);
			// loadcore.prx and sysmem.prx need to be compressed using KL4E.
			// KL4E supports a version where the data is not compressed.
			// Use this simple version as we have no real KL4E compressor.
			if (isKl4eCompression(fileName)) {
				writeString(osCompressed, "KL4E", 4);
				write8(osCompressed, 0x80); // Flag indicating that the rest of the data is uncompressed
				write32(osCompressed, endianSwap32(lengthUncompressed));
				writeBytes(osCompressed, bufferUncompressed, lengthUncompressed);
			} else {
				GZIPOutputStream os = new GZIPOutputStream(osCompressed);
				os.write(bufferUncompressed, 0, lengthUncompressed);
				os.close();
			}
		} catch (IOException e) {
		}

		byte[] bytes = osCompressed.toByteArray();
		fixPspSizeInHeader(bytes);

		return bytes;
	}

	private long getCompressedPrxSize(String dirName, String fileName) {
		byte[] compressedFile = getCompressedPrxFile(dirName, fileName);
		if (compressedFile == null) {
			return 0L;
		}
		return (long) compressedFile.length;
	}

	@Override
	public int ioDread(String dirName, SceIoDirent dir) {
		int result = super.ioDread(dirName, dir);
		if (isPrx(dir.filename)) {
			dir.stat.size = getCompressedPrxSize(dirName, dir.filename);
		}

		return result;
	}

	@Override
	public IVirtualFile ioOpen(String fileName, int flags, int mode) {
		if (isPrx(fileName)) {
			return new ByteArrayVirtualFile(getCompressedPrxFile(null, fileName));
		}

		return super.ioOpen(fileName, flags, mode);
	}
}
