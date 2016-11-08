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
package jpcsp.HLE.VFS.xmb;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Map;

import jpcsp.HLE.TPointer;
import jpcsp.HLE.VFS.AbstractVirtualFile;
import jpcsp.HLE.VFS.IVirtualFile;
import jpcsp.HLE.VFS.IVirtualFileSystem;
import jpcsp.HLE.VFS.iso.UmdIsoVirtualFileSystem;
import jpcsp.HLE.modules.IoFileMgrForUser;
import jpcsp.HLE.modules.IoFileMgrForUser.IoOperation;
import jpcsp.HLE.modules.IoFileMgrForUser.IoOperationTiming;
import jpcsp.filesystems.umdiso.UmdIsoReader;
import jpcsp.format.PBP;
import jpcsp.format.PSF;
import jpcsp.util.Utilities;

public class XmbIsoVirtualFile extends AbstractVirtualFile {
	protected static class PbpSection {
		IVirtualFile vFile;
		int size;
	}
	private UmdIsoReader iso;
	protected IVirtualFileSystem vfs;
	protected PbpSection[] sections;
	protected int[] header;
	protected byte[] headerBytes;
	protected long filePointer;
	protected long totalLength;
	protected static final String[] isoFileNames = new String [] {
		"PSP_GAME/PARAM.SFO",
		"PSP_GAME/ICON0.PNG",
		"PSP_GAME/ICON1.PMF",
		"PSP_GAME/PIC0.PNG",
		"PSP_GAME/PIC1.PNG",
		"PSP_GAME/SND0.AT3"
	};
	protected byte[] virtualSfo = {
        (byte) 0x00, (byte) 0x50, (byte) 0x53, (byte) 0x46, (byte) 0x01, (byte) 0x01, (byte) 0x00, (byte) 0x00,
        (byte) 0x94, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xE8, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x08, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x04, (byte) 0x04,
        (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x09, (byte) 0x00, (byte) 0x04, (byte) 0x02,
        (byte) 0x03, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x12, (byte) 0x00, (byte) 0x04, (byte) 0x02,
        (byte) 0x0A, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x10, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x08, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x1A, (byte) 0x00, (byte) 0x04, (byte) 0x02,
        (byte) 0x05, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x08, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x18, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x27, (byte) 0x00, (byte) 0x04, (byte) 0x04,
        (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x20, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x36, (byte) 0x00, (byte) 0x04, (byte) 0x02,
        (byte) 0x05, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x08, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x24, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x45, (byte) 0x00, (byte) 0x04, (byte) 0x04,
        (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x04, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x2C, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x4C, (byte) 0x00, (byte) 0x04, (byte) 0x02,
        (byte) 0x40, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x80, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x30, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x42, (byte) 0x4F, (byte) 0x4F, (byte) 0x54,
        (byte) 0x41, (byte) 0x42, (byte) 0x4C, (byte) 0x45, (byte) 0x00, (byte) 0x43, (byte) 0x41, (byte) 0x54,
        (byte) 0x45, (byte) 0x47, (byte) 0x4F, (byte) 0x52, (byte) 0x59, (byte) 0x00, (byte) 0x44, (byte) 0x49,
        (byte) 0x53, (byte) 0x43, (byte) 0x5F, (byte) 0x49, (byte) 0x44, (byte) 0x00, (byte) 0x44, (byte) 0x49,
        (byte) 0x53, (byte) 0x43, (byte) 0x5F, (byte) 0x56, (byte) 0x45, (byte) 0x52, (byte) 0x53, (byte) 0x49,
        (byte) 0x4F, (byte) 0x4E, (byte) 0x00, (byte) 0x50, (byte) 0x41, (byte) 0x52, (byte) 0x45, (byte) 0x4E,
        (byte) 0x54, (byte) 0x41, (byte) 0x4C, (byte) 0x5F, (byte) 0x4C, (byte) 0x45, (byte) 0x56, (byte) 0x45,
        (byte) 0x4C, (byte) 0x00, (byte) 0x50, (byte) 0x53, (byte) 0x50, (byte) 0x5F, (byte) 0x53, (byte) 0x59,
        (byte) 0x53, (byte) 0x54, (byte) 0x45, (byte) 0x4D, (byte) 0x5F, (byte) 0x56, (byte) 0x45, (byte) 0x52,
        (byte) 0x00, (byte) 0x52, (byte) 0x45, (byte) 0x47, (byte) 0x49, (byte) 0x4F, (byte) 0x4E, (byte) 0x00,
        (byte) 0x54, (byte) 0x49, (byte) 0x54, (byte) 0x4C, (byte) 0x45, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x4D, (byte) 0x47, (byte) 0x00, (byte) 0x00,
        (byte) 0x55, (byte) 0x43, (byte) 0x4A, (byte) 0x53, (byte) 0x31, (byte) 0x30, (byte) 0x30, (byte) 0x34,
        (byte) 0x31, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x31, (byte) 0x2E, (byte) 0x30, (byte) 0x30, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x31, (byte) 0x2E, (byte) 0x30, (byte) 0x30,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x80, (byte) 0x00, (byte) 0x00,
        (byte) 0x31, (byte) 0x32, (byte) 0x33, (byte) 0x34, (byte) 0x35, (byte) 0x36, (byte) 0x37, (byte) 0x38,
        (byte) 0x39, (byte) 0x30, (byte) 0x31, (byte) 0x32, (byte) 0x33, (byte) 0x34, (byte) 0x35, (byte) 0x36,
        (byte) 0x37, (byte) 0x38, (byte) 0x39, (byte) 0x30, (byte) 0x31, (byte) 0x32, (byte) 0x33, (byte) 0x34,
        (byte) 0x35, (byte) 0x36, (byte) 0x37, (byte) 0x38, (byte) 0x39, (byte) 0x30, (byte) 0x31, (byte) 0x32,
        (byte) 0x33, (byte) 0x34, (byte) 0x35, (byte) 0x36, (byte) 0x37, (byte) 0x38, (byte) 0x39, (byte) 0x30,
        (byte) 0x31, (byte) 0x32, (byte) 0x33, (byte) 0x34, (byte) 0x35, (byte) 0x36, (byte) 0x37, (byte) 0x38,
        (byte) 0x39, (byte) 0x30, (byte) 0x31, (byte) 0x32, (byte) 0x33, (byte) 0x34, (byte) 0x35, (byte) 0x36,
        (byte) 0x37, (byte) 0x38, (byte) 0x39, (byte) 0x30, (byte) 0x31, (byte) 0x32, (byte) 0x33, (byte) 0x34,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00
	};

	public XmbIsoVirtualFile(String umdFilename) {
		super(null);

		try {
			iso = new UmdIsoReader(umdFilename);
			vfs = new UmdIsoVirtualFileSystem(iso);
			sections = new PbpSection[isoFileNames.length];
			header = new int[10];
			header[0] = PBP.PBP_MAGIC;
			header[1] = 0x10000; // version
			int offset = 0x28;
			for (int i = 0; i < isoFileNames.length; i++) {
				PbpSection section = new PbpSection();
				section.vFile = vfs.ioOpen(isoFileNames[i], IoFileMgrForUser.PSP_O_RDONLY, 0);
				if (section.vFile != null) {
					section.size = (int) section.vFile.length();
				}

				sections[i] = section;
				header[i + 2] = offset;
				if (i == 0) {
					offset += virtualSfo.length;
				} else {
					offset += section.size;
				}
			}
			totalLength = offset;

			headerBytes = new byte[header.length * 4];
			ByteBuffer.wrap(headerBytes).order(ByteOrder.LITTLE_ENDIAN).asIntBuffer().put(header);
		} catch (FileNotFoundException e) {
			log.debug("XmbIsoVirtualFile", e);
		} catch (IOException e) {
			log.debug("XmbIsoVirtualFile", e);
		}
	}

	protected int getHeaderSection(int remaining, TPointer outputPointer, int offset) {
		int length = (int) Math.min(remaining, header[2] - filePointer);

		outputPointer.setArray(offset, headerBytes, (int) filePointer, length);

		return length;
	}

	protected int getSfoSection(int remaining, TPointer outputPointer, int offset) {
		if (sections[0].vFile != null) {
			byte[] buffer = new byte[sections[0].size];
			sections[0].vFile.ioLseek(0L);
			int length = sections[0].vFile.ioRead(buffer, 0, buffer.length);

			PSF psf = new PSF();
			try {
				psf.read(ByteBuffer.wrap(buffer, 0, length));
			} catch (IOException e) {
				return IO_ERROR;
			}

			String title = psf.getString("TITLE");
			String discId = psf.getString("DISC_ID");
			int parentalLevel = psf.getNumeric("PARENTAL_LEVEL");
			ByteBuffer sfoBuffer = ByteBuffer.wrap(virtualSfo);
			sfoBuffer.position(0x118);
			Utilities.writeStringZ(sfoBuffer, title);
			sfoBuffer.position(0xF0);
			Utilities.writeStringZ(sfoBuffer, discId);
			Utilities.writeUnaligned32(virtualSfo, 0x108, parentalLevel);
		}

		int virtualSfoOffset = (int) (filePointer - header[2]);
		int length = (int) Math.min(remaining, virtualSfo.length - virtualSfoOffset);
		outputPointer.setArray(offset, virtualSfo, virtualSfoOffset, length);

		return length;
	}

	protected int getPbpSection(int remaining, int idx, TPointer outputPointer, int offset) {
		int length = 0;

		if (remaining <= 0) {
			return length;
		}

		if (idx < 0) {
			if (filePointer < header[2]) {
				return getHeaderSection(remaining, outputPointer, offset);
			}
			return length;
		}

		if (filePointer < header[3]) {
			return getSfoSection(remaining, outputPointer, offset);
		}

		if (filePointer < header[2 + idx] || filePointer >= header[2 + idx + 1]) {
			return length;
		}

		long sectionPointer = filePointer - header[2 + idx];
		remaining = (int) Math.min(remaining, sections[idx].size - sectionPointer);

		while (remaining > 0) {
			TPointer data = new TPointer(outputPointer.getMemory(), outputPointer.getAddress() + offset);
			sections[idx].vFile.ioLseek(sectionPointer);
			int result = sections[idx].vFile.ioRead(data, remaining);
			if (result < 0) {
				return result;
			}

			remaining -= result;
			length += result;
			offset += result;
			sectionPointer += result;
		}

		return length;
	}

	@Override
	public int ioRead(TPointer outputPointer, int outputLength) {
		int remaining = outputLength;

		int offset = 0;
		for (int i = -1; i < sections.length && remaining > 0; i++) {
			int length = getPbpSection(remaining, i, outputPointer, offset);

			if (length < 0) {
				return length;
			}

			offset += length;
			filePointer += length;
			remaining -= length;
		}

		return offset;
	}

	@Override
	public int ioRead(byte[] outputBuffer, int outputOffset, int outputLength) {
		return super.ioRead(outputBuffer, outputOffset, outputLength);
	}

	@Override
	public long length() {
		return totalLength;
	}

	@Override
	public long ioLseek(long offset) {
		filePointer = offset;

		return filePointer;
	}

	@Override
	public int ioClose() {
		if (sections != null) {
			for (int i = 0; i < sections.length; i++) {
				if (sections[i].vFile != null) {
					sections[i].vFile.ioClose();
					sections[i].vFile = null;
				}
			}
			sections = null;
		}

		totalLength = 0L;

		return 0;
	}

	@Override
	public Map<IoOperation, IoOperationTiming> getTimings() {
		// Do not delay IO operations on faked EBOOT.PBP files
		return IoFileMgrForUser.noDelayTimings;
	}

	public IVirtualFile ioReadForLoadExec() {
		return vfs.ioOpen("PSP_GAME/SYSDIR/EBOOT.BIN", IoFileMgrForUser.PSP_O_RDONLY, 0);
	}

	public UmdIsoReader getIsoReader() {
		return iso;
	}
}
