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

import static jpcsp.memory.mmio.MMIOHandlerBaseMemoryStick.PAGE_SIZE;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;

import jpcsp.util.LWJGLFixer;

public class ExtractMemoryStickIPL {
	static {
		LWJGLFixer.fixOnce();
	}

	public static void main(String[] args) {
		String fileName = "ipl.bin";
		if (args != null && args.length >= 1) {
			fileName = args[0];
		}
		new ExtractMemoryStickIPL().extractMemoryStickIPL(fileName);
	}

	private static boolean isEmpty(byte[] buffer, int length) {
		for (int i = 0; i < length; i++) {
			if (buffer[i] != (byte) 0) {
				return false;
			}
		}

		return true;
	}

	private void extractMemoryStickIPL(String fileName) {
		try {
			RandomAccessFile msIpl = new RandomAccessFile("ms.ipl.bin", "r");
			msIpl.seek(16 * PAGE_SIZE);

			OutputStream os = new FileOutputStream(fileName);

			byte[] buffer = new byte[PAGE_SIZE];
			while (true) {
				int length = msIpl.read(buffer);
				if (length <= 0 || isEmpty(buffer, length)) {
					break;
				}
				os.write(buffer, 0, length);
			}
			msIpl.close();
			os.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
