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

import static jpcsp.util.Utilities.readUnaligned16;
import static jpcsp.util.Utilities.readUnaligned32;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Map;

import jpcsp.HLE.TPointer;
import jpcsp.HLE.VFS.AbstractVirtualFile;
import jpcsp.HLE.VFS.IVirtualFile;
import jpcsp.HLE.VFS.IVirtualFileSystem;
import jpcsp.HLE.VFS.iso.UmdIsoVirtualFileSystem;
import jpcsp.HLE.kernel.types.SceIoStat;
import jpcsp.HLE.modules.IoFileMgrForUser;
import jpcsp.HLE.modules.IoFileMgrForUser.IoOperation;
import jpcsp.HLE.modules.IoFileMgrForUser.IoOperationTiming;
import jpcsp.filesystems.umdiso.UmdIsoReader;
import jpcsp.format.PBP;
import jpcsp.settings.Settings;
import jpcsp.util.Utilities;

public class XmbIsoVirtualFile extends AbstractVirtualFile {
	protected static class PbpSection {
		int index;
		int size;
		int offset;
		boolean availableInContents;
		String umdFilename;
		File cacheFile;
	}

	private String umdFilename;
	private String umdName;
	protected long filePointer;
	protected long totalLength;
	protected static final String[] umdFilenames = new String [] {
		"PSP_GAME/PARAM.SFO",
		"PSP_GAME/ICON0.PNG",
		"PSP_GAME/ICON1.PMF",
		"PSP_GAME/PIC0.PNG",
		"PSP_GAME/PIC1.PNG",
		"PSP_GAME/SND0.AT3"
	};
	protected byte[] contents;
	protected PbpSection[] sections;

	public XmbIsoVirtualFile(String umdFilename) {
		super(null);

		this.umdFilename = umdFilename;
		umdName = new File(umdFilename).getName();

		File cacheDirectory = new File(getCacheDirectory());
		boolean createCacheFiles = !cacheDirectory.isDirectory();
		if (createCacheFiles) {
			cacheDirectory.mkdirs();
		}

		try {
			UmdIsoReader iso = new UmdIsoReader(umdFilename);
			IVirtualFileSystem vfs = new UmdIsoVirtualFileSystem(iso);
			sections = new PbpSection[umdFilenames.length + 1];
			sections[0] = new PbpSection();
			sections[0].index = 0;
			sections[0].offset = 0;
			sections[0].size = 0x28;
			sections[0].availableInContents = true;
			int offset = 0x28;
			SceIoStat stat = new SceIoStat();
			for (int i = 0; i < umdFilenames.length; i++) {
				PbpSection section = new PbpSection();
				section.index = i + 1;
				section.offset = offset;
				section.umdFilename = umdFilenames[i];
				if (vfs.ioGetstat(section.umdFilename, stat) >= 0) {
					section.size = (int) stat.size;
					if (log.isTraceEnabled()) {
						log.trace(String.format("%s: mapping %s at offset 0x%X, size 0x%X", umdFilename, umdFilenames[i], section.offset, section.size));
					}
				}

				String cacheFileName = getCacheFileName(section);
				File cacheFile = new File(cacheFileName);

				// Create only cache files for PARAM.SFO and ICON0.PNG
				if (createCacheFiles && i < 2) {
					IVirtualFile vFile = vfs.ioOpen(section.umdFilename, IoFileMgrForUser.PSP_O_RDONLY, 0);
					if (vFile != null) {
						section.size = (int) vFile.length();
						byte[] buffer = new byte[section.size];
						int length = vFile.ioRead(buffer, 0, buffer.length);
						vFile.ioClose();

						OutputStream os = new FileOutputStream(cacheFile);
						os.write(buffer, 0, length);
						os.close();
					}
				}

				if (cacheFile.canRead()) {
					section.cacheFile = cacheFile;
				}

				sections[section.index] = section;
				offset += section.size;
			}
			totalLength = offset;

			contents = new byte[offset];
			ByteBuffer buffer = ByteBuffer.wrap(contents).order(ByteOrder.LITTLE_ENDIAN);
			buffer.putInt(PBP.PBP_MAGIC);
			buffer.putInt(0x10000); // version
			for (int i = 1; i < sections.length; i++) {
				buffer.putInt(sections[i].offset);
			}
			int endSectionOffset = sections[sections.length - 1].offset + sections[sections.length - 1].size;
			for (int i = sections.length; i <= 8; i++) {
				buffer.putInt(endSectionOffset);
			}

			if (log.isTraceEnabled()) {
				log.trace(String.format("%s: PBP header :%s", umdFilename, Utilities.getMemoryDump(contents, sections[0].offset, sections[0].size)));
			}
			vfs.ioExit();
		} catch (FileNotFoundException e) {
			log.debug("XmbIsoVirtualFile", e);
		} catch (IOException e) {
			log.debug("XmbIsoVirtualFile", e);
		}
	}

	protected String getCacheDirectory() {
        return String.format("%1$s%2$cUmdBrowserCache%2$c%3$s", Settings.getInstance().readString("emu.tmppath"), File.separatorChar, umdName);
	}

	protected String getCacheFileName(PbpSection section) {
        return String.format("%s%c%s", getCacheDirectory(), File.separatorChar, section.umdFilename.substring(9));
	}

	protected void readSection(PbpSection section) {
		if (section.size > 0) {
			try {
				if (section.cacheFile != null) {
					if (log.isDebugEnabled()) {
						log.debug(String.format("XmbIsoVirtualFile.readSection from Cache %s", section.cacheFile));
					}

					InputStream is = new FileInputStream(section.cacheFile);
					is.read(contents, section.offset, section.size);
					is.close();
				} else {
					if (log.isDebugEnabled()) {
						log.debug(String.format("XmbIsoVirtualFile.readSection from UMD %s", section.umdFilename));
					}

					UmdIsoReader iso = new UmdIsoReader(umdFilename);
					IVirtualFileSystem vfs = new UmdIsoVirtualFileSystem(iso);
					IVirtualFile vFile = vfs.ioOpen(section.umdFilename, IoFileMgrForUser.PSP_O_RDONLY, 0);
					if (vFile != null) {
						vFile.ioRead(contents, section.offset, section.size);
						vFile.ioClose();
					}
					vfs.ioExit();
				}
			} catch (IOException e) {
				log.debug("readSection", e);
			}

			// PARAM.SFO?
			if (section.index == 1) {
				// Patch the CATEGORY in the PARAM.SFO:
				// the VSH is checking that the CATEGORY value is starting
				// with 'M' (meaning MemoryStick) and not 'U' (UMD).
				// Change the first letter 'U' into 'M'.
				int offset = section.offset;
				int keyTableOffset = readUnaligned32(contents, offset + 8) + offset;
				int valueTableOffset = readUnaligned32(contents, offset + 12) + offset;
				int numberKeys = readUnaligned32(contents, offset + 16);
				for (int i = 0; i < numberKeys; i++) {
					int keyOffset = readUnaligned16(contents, offset + 20 + i * 16);
					String key = Utilities.readStringZ(contents, keyTableOffset + keyOffset);
					if ("CATEGORY".equals(key)) {
						int valueOffset = readUnaligned32(contents, offset + 20 + i * 16 + 12);
						char valueFirstChar = (char) contents[valueTableOffset + valueOffset];

						// Change the first letter 'U' into 'M'.
						if (valueFirstChar == 'U') {
							contents[valueTableOffset + valueOffset] = 'M';
						}
						break;
					}
				}
			}
		}

		section.availableInContents = true;
	}

	protected int ioRead(PbpSection section, TPointer outputPointer, int offset, int length) {
		if (filePointer < section.offset || filePointer >= section.offset + section.size) {
			return 0;
		}

		length = Math.min(length, section.size - (int) (filePointer - section.offset));
		if (length > 0) {
			if (!section.availableInContents) {
				readSection(section);
			}

			outputPointer.setArray(offset, contents, (int) filePointer, length);
		}

		return length;
	}

	@Override
	public int ioRead(TPointer outputPointer, int outputLength) {
		int remaining = (int) Math.min(outputLength, contents.length - filePointer);
		int offset = 0;
		for (int i = 0; remaining > 0 && i < sections.length; i++) {
			int length = ioRead(sections[i], outputPointer, offset, remaining);
			filePointer += length;
			offset += length;
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
		filePointer = 0L;

		return 0;
	}

	@Override
	public Map<IoOperation, IoOperationTiming> getTimings() {
		// Do not delay IO operations on faked EBOOT.PBP files
		return IoFileMgrForUser.noDelayTimings;
	}

	public IVirtualFile ioReadForLoadExec() throws FileNotFoundException, IOException {
		UmdIsoReader iso = getIsoReader();
		IVirtualFileSystem vfs = new UmdIsoVirtualFileSystem(iso);
		return vfs.ioOpen("PSP_GAME/SYSDIR/EBOOT.BIN", IoFileMgrForUser.PSP_O_RDONLY, 0);
	}

	public UmdIsoReader getIsoReader() throws FileNotFoundException, IOException {
		return new UmdIsoReader(umdFilename);
	}
}
