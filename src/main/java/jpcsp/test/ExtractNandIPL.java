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

import static jpcsp.HLE.modules.sceNand.iplTablePpnStart;
import static jpcsp.hardware.Nand.pageSize;
import static jpcsp.hardware.Nand.pagesPerBlock;
import static jpcsp.util.Utilities.readUnaligned16;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;

import jpcsp.util.LWJGLFixer;

public class ExtractNandIPL {
	static {
		LWJGLFixer.fixOnce();
	}

	public static void main(String[] args) {
		String fileName = "ipl.bin";
		if (args != null && args.length >= 1) {
			fileName = args[0];
		}
		new ExtractNandIPL().extractNandIPL(fileName);
	}

	private void extractNandIPL(String fileName) {
		try {
			RandomAccessFile nandIpl = new RandomAccessFile("nand.ipl.bin", "r");

			byte[] buffer = new byte[pageSize];
			nandIpl.read(buffer);

			int[] iplTable = new int[pageSize >> 1];
			int iplTableSize = 0;
			for (int i = 0; i < pageSize; i += 2) {
				int iplEntry = readUnaligned16(buffer, i);
				if (iplEntry == 0) {
					break;
				}
				iplTable[iplTableSize++] = iplEntry;
			}

			OutputStream os = new FileOutputStream(fileName);
			for (int i = 0; i < iplTableSize; i++) {
				nandIpl.seek(((iplTable[i] * pagesPerBlock) - iplTablePpnStart) * pageSize);
				for (int j = 0; j < pagesPerBlock; j++) {
					int length = nandIpl.read(buffer);
					if (length != pageSize) {
						break;
					}
					os.write(buffer, 0, length);
				}
			}

			nandIpl.close();
			os.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
