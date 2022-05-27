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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class DumpRawFile {

	private static int read8(FileInputStream fis) throws IOException {
		byte[] buffer = new byte[1];
		int length = fis.read(buffer);
		if (length <= 0) {
			throw new IOException("EOF");
		}

		return ((int) buffer[0]) & 0xFF;
	}

	private static int read32(FileInputStream fis) throws IOException {
		int n1 = read8(fis);
		int n2 = read8(fis);
		int n3 = read8(fis);
		int n4 = read8(fis);

		return n1 | (n2 << 8) | (n3 << 16) | (n4 << 24);
	}

	private static void skip(FileInputStream fis, int length) throws IOException {
		fis.skip(length);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			FileInputStream fis = new FileInputStream("tmp/xxDISCIDxx/Mpeg-nnnn/VideoStream-0.raw");
			boolean isVideo = true;
			int version = read32(fis);
			System.out.println(String.format("RawFile version %d", version));

			for (int frameCount = 0; true; frameCount++) {
				int fileSize = read32(fis);
				int timeStamp = read32(fis);
				if (isVideo) {
					int packetsConsumed = read32(fis);
					int totalBytes = read32(fis);
					skip(fis, fileSize - 16);
					System.out.println(String.format("VideoFrame %d, pts %d", frameCount, timeStamp, fileSize, packetsConsumed, totalBytes));
				} else {
					skip(fis, fileSize - 8);
					System.out.println(String.format("AudioFrame %d, pts %d", frameCount, timeStamp, fileSize));
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
		}
	}
}
