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
package jpcsp.filesystems.umdiso;

import static jpcsp.util.Utilities.add;
import static libchdr.Cdrom.CD_FRAMES_PER_HUNK;
import static libchdr.Cdrom.CD_MAX_SECTOR_DATA;
import static libchdr.Cdrom.CD_MAX_SUBCODE_DATA;
import static libchdr.ChdHeader.CDROM_TRACK_METADATA2_TAG;
import static libchdr.ChdHeader.CHD_OPEN_READ;
import static libchdr.ChdHeader.ChdError.CHDERR_NONE;
import static libchdr.ChdHeader.ChdError.CHDERR_REQUIRES_PARENT;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import libchdr.Chd;
import libchdr.ChdHeader;
import libchdr.ChdHeader.ChdError;
import libchdr.Chd.ChdFile;

public class CHDFileSectorDevice extends AbstractFileSectorDevice {
	private final Chd chd;
	private final ChdFile chdFile;
	private final ChdHeader header;
	private int numberFrames;
	private int frameSize;
	private final byte[] buffer;
	private int hunkNumberInBuffer;

	public CHDFileSectorDevice(RandomAccessFile fileAccess, String fileName) throws IOException {
		super(fileAccess);

		chd = new Chd();

		ChdFile[] res = new ChdFile[1];
		ChdError err = openChdFile(fileName, res);
		if (err != CHDERR_NONE) {
	        throw new IOException(String.format("Invalid CHD file '%s': %s", fileName, err));
		}
		chdFile = res[0];

		header = chd.chd_get_header(chdFile);
		if (header == null) {
	        throw new IOException(String.format("Could not access the CHD file header '%s'", fileAccess));
		}

		numberFrames = header.totalhunks * CD_FRAMES_PER_HUNK;
		frameSize = CD_MAX_SECTOR_DATA + CD_MAX_SUBCODE_DATA;

		final byte[] metadata = new byte[512];
		int[] resultLength = new int[1];
		err = chd.chd_get_metadata(chdFile, CDROM_TRACK_METADATA2_TAG, 0, metadata, metadata.length, resultLength, null, null);
		if (err == CHDERR_NONE) {
			int metadataLength = resultLength[0];
			String metadataString = new String(metadata, 0, metadataLength);
			Pattern p = Pattern.compile("TRACK:(\\d+) TYPE:(.*) SUBTYPE:(.*) FRAMES:(\\d+) PREGAP:(\\d+)");
			Matcher m = p.matcher(metadataString);
			if (m.find()) {
				int track = Integer.parseInt(m.group(1));
				String type = m.group(2);
				String subtype = m.group(3);
				int frames = Integer.parseInt(m.group(4));
				int pregap = Integer.parseInt(m.group(5));
				log.info(String.format("Track %d, type %s, subtype %s, frames %d, pregap %d", track, type, subtype, frames, pregap));

				numberFrames = frames;
				if ("MODE1".equals(type)) {
					frameSize = sectorLength;
				} else {
			        throw new IOException(String.format("Unsupported CHD file format METADATA2='%s'", metadataString));
				}
			} else {
		        throw new IOException(String.format("Unsupported CHD file format METADATA2='%s'", metadataString));
			}
		} else {
	        throw new IOException(String.format("Unsupported CHD file format, could not find METADATA2 tag"));
		}

		// The current readSector() implementation is only supporting a frameSize of 2048 bytes
		if (frameSize != sectorLength) {
	        throw new IOException(String.format("Unsupported CHD file format having frameSize=%d, only %d is currently supported", frameSize, sectorLength));
		}

		buffer = new byte[header.hunkbytes];
		hunkNumberInBuffer = -1;
	}

	private ChdError openChdFile(String fileName, ChdFile[] res) {
		 // First try to open without giving a parent
		ChdError err = chd.chd_open_file(fileName, CHD_OPEN_READ, null, res);
		if (err == CHDERR_REQUIRES_PARENT) {
			// Search for a parent file based on the child file name
			String[] parentFileNames = searchParentFileNames(fileName);
			if (parentFileNames != null) {
				// Try to find the matching parent
				for (int i = 0; i < parentFileNames.length; i++) {
					err = openChdFile(parentFileNames[i], res);
					if (err == CHDERR_NONE) {
						ChdFile parent = res[0];
						// Try to reopen the child CHD with the potential parent CHD
						err = chd.chd_open_file(fileName, CHD_OPEN_READ, parent, res);
						if (err == CHDERR_NONE) {
							// The child could be opened, we have found the matching parent
							break;
						}

						// This is not the matching parent, close it and try for the next one
						chd.chd_close(parent);
					}
				}
			}
		}

		return err;
	}

	/**
	 * Test if a given file name is a potential parent CHD for a child CHD, based on the
	 * file names.
	 * It is assumed that the parent has the same file name extension (e.g. ".chd") as the child,
	 * and that either the parent file name is the prefix of the child file name or vice versa.
	 * E.g.:
	 *     - Child prefix: "Application-patched", child suffix: "chd"
	 *     - Parent prefix: "Application", parent suffix: "chd"
	 * or
	 *     - Child prefix: "Application", child suffix: "chd"
	 *     - Parent prefix: "Application-original", parent suffix: "chd"
	 * 
	 * @param childPrefix   The part of the child CHD file name up to the last "."
	 * @param childSuffix   The suffix of the child CHD file name (e.g. "chd")
	 * @param parentPrefix  The part of the parent CHD file name up to the last "."
	 * @param parentSuffix  The suffix of the parent CHD file name (e.g. "chd")
	 * @return              true if the parentPrefix/parentSuffix is a potential file name for a parent
	 */
	private boolean isParentFileNameMatching(String childPrefix, String childSuffix, String parentPrefix, String parentSuffix) {
		// Child and parent must have the same suffix
		if (!childSuffix.equalsIgnoreCase(parentSuffix)) {
			return false;
		}

		// The parent must be different than the child itself
		if (childPrefix.equals(parentPrefix)) {
			return false;
		}

		// Either the parent file name is the prefix of the child file name or vice versa.
		return childPrefix.startsWith(parentPrefix) || parentPrefix.startsWith(childPrefix);
	}

	/**
	 * Try to find a list of potential parent CHD file names based on the child CHD file name.
	 * It is assumed that the parent resides in the same directory as the child,
	 * that it has the same file name extension (e.g. ".chd") and that either the parent file name
	 * is the prefix of the child file name or vice versa.
	 * E.g.:
	 *     - Child: Application-patched.chd
	 *     - Parent: Application.chd
	 * or
	 *     - Child: Application.chd
	 *     - Parent: Application-original.chd
	 *
	 * @param childFileName the file name of the child CHD
	 * @return              an array of potential parent CHD file names
	 */
	private String[] searchParentFileNames(String childFileName) {
		if (childFileName == null) {
			return null;
		}

		int lastDot = childFileName.lastIndexOf('.');
		if (lastDot < 0) {
			return null;
		}
		int lastDir = childFileName.lastIndexOf(File.separatorChar);
		if (lastDir < 0 || lastDir > lastDot) {
			return null;
		}

		String dirName = childFileName.substring(0, lastDir);
		File[] dirEntries = new File(dirName).listFiles();
		if (dirEntries == null) {
			return null;
		}

		String childPrefix = childFileName.substring(lastDir + 1, lastDot);
		String childSuffix = childFileName.substring(lastDot);
		String[] parentFileNames = null;
		for (int i = 0; i < dirEntries.length; i++) {
			if (dirEntries[i].isFile()) {
				String parentFileName = dirEntries[i].getName();
				lastDot = parentFileName.lastIndexOf('.');
				if (lastDot >= 0) {
					String parentPrefix = parentFileName.substring(0, lastDot);
					String parentSuffix = parentFileName.substring(lastDot);
					if (isParentFileNameMatching(childPrefix, childSuffix, parentPrefix, parentSuffix)) {
						parentFileNames = add(parentFileNames, dirEntries[i].getPath());
					}
				}
			}
		}

		return parentFileNames;
	}

	@Override
	public int getNumSectors() throws IOException {
		return numberFrames;
	}

	@Override
	public void readSector(int sectorNumber, byte[] data, int offset) throws IOException {
		int hunkNumber = sectorNumber / CD_FRAMES_PER_HUNK;

		// Reading the same hunk as the one already available in the buffer?
		if (hunkNumberInBuffer != hunkNumber) {
			ChdError err = chd.chd_read(chdFile, hunkNumber, buffer, 0);
			if (err != CHDERR_NONE) {
		        throw new IOException(String.format("Error reading CHD file sectorNumber=%d: %s", sectorNumber, err));
			}
			// Remember which hunk we currently have stored in the buffer
			hunkNumberInBuffer = hunkNumber;
		}

		int frameIndex = sectorNumber % CD_FRAMES_PER_HUNK;
		System.arraycopy(buffer, frameIndex * (CD_MAX_SECTOR_DATA + CD_MAX_SUBCODE_DATA), data, offset, sectorLength);
	}
}
