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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

import jpcsp.util.LWJGLFixer;

public class InstallMemoryStickIPL {
	static {
		LWJGLFixer.fixOnce();
	}

	public static void main(String[] args) {
		String fileName = "multiloader_ipl.bin";
		if (args != null && args.length >= 1) {
			fileName = args[0];
		}
		new InstallMemoryStickIPL().installMemoryStickIPL(fileName);
	}

	private void installMemoryStickIPL(String fileName) {
		try {
			File inputFile = new File(fileName);
			byte[] inputIpl = new byte[(int) inputFile.length()];
			InputStream is = new FileInputStream(inputFile);
			int length = is.read(inputIpl);
			is.close();

			RandomAccessFile msIpl = new RandomAccessFile("ms.ipl.bin", "rw");
			msIpl.seek(16 * PAGE_SIZE);
			msIpl.write(inputIpl, 0, length);
			msIpl.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
