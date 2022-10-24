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
package jpcsp.test;

import static libchdr.Cdrom.CD_FRAMES_PER_HUNK;
import static libchdr.Cdrom.CD_MAX_SECTOR_DATA;
import static libchdr.Cdrom.CD_MAX_SUBCODE_DATA;
import static libchdr.ChdHeader.CDROM_TRACK_METADATA2_TAG;
import static libchdr.ChdHeader.CHD_OPEN_READ;
import static libchdr.ChdHeader.ChdError.CHDERR_NONE;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

import jpcsp.HLE.VFS.IVirtualFile;
import jpcsp.HLE.VFS.local.LocalVirtualFile;
import jpcsp.filesystems.SeekableRandomFile;
import libchdr.Chd;
import libchdr.ChdHeader;
import libchdr.ChdHeader.ChdError;

public class TestChdr {
	public static Logger log = Chd.log;

	public static void main(String[] args) {
        DOMConfigurator.configure("LogSettings.xml");
		try {
			new TestChdr().test("umdimages/cube.chd", "tmp/cube.iso", null);
//			new TestChdr().test("tmp/test.chd", "tmp/test.iso", "tmp/parent.chd");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	public void test(String fileName, String outputFileName, String parentFileName) throws FileNotFoundException {
		ChdError err;

		long start = System.currentTimeMillis();

		new File(outputFileName).delete();
		IVirtualFile vFileOut = new LocalVirtualFile(new SeekableRandomFile(outputFileName, "rw"));

		Chd chd = new Chd();
		Chd.ChdFile parent = null;
		if (parentFileName != null) {
			Chd.ChdFile[] res = new Chd.ChdFile[1];
			err = chd.chd_open_file(parentFileName, CHD_OPEN_READ, null, res);
			parent = res[0];
			log.info(String.format("parent=%s, err=%s", parent, err));
		}

		Chd.ChdFile[] res = new Chd.ChdFile[1];
		err = chd.chd_open_file(fileName, CHD_OPEN_READ, parent, res);
		Chd.ChdFile chdFile = res[0];
		log.info(String.format("chdFile=%s, err=%s", chdFile, err));

		ChdHeader header = chd.chd_get_header(chdFile);
		int numberFrames = header.totalhunks * CD_FRAMES_PER_HUNK;
		int frameSize = CD_MAX_SECTOR_DATA + CD_MAX_SUBCODE_DATA;

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
					frameSize = 2048;
				}
			} else {
				log.info(metadataString);
			}
		}

		int totalBytes = header.hunkbytes * header.totalhunks;
		final byte[] buffer = new byte[header.hunkbytes];
		for (int i = 0, frameCount = 0; i < header.totalhunks; i++) {
			log.info(String.format("Reading hunk#%d/%d", i + 1, header.totalhunks));
			err = chd.chd_read(chdFile, i, buffer, 0);
			if (err != CHDERR_NONE) {
				log.error(String.format("chdRead hunknum=%d, err=%s", i, err));
			} else {
				for (int frame = 0; frame < CD_FRAMES_PER_HUNK && frameCount < numberFrames; frame++, frameCount++) {
					vFileOut.ioWrite(buffer, frame * (CD_MAX_SECTOR_DATA + CD_MAX_SUBCODE_DATA), frameSize);
				}
			}
		}
		chd.chd_close(chdFile);
		vFileOut.ioClose();

		long end = System.currentTimeMillis();
		double timeTaken = (end - start) / 1000.0;
		log.info(String.format("Read %d bytes in %f seconds", totalBytes, timeTaken));
	}
}
