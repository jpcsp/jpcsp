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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.HashSet;
import java.util.Set;

public class RemoveDuplicatePreDecrypt {
	public static void main(String[] args) {
		try {
			new RemoveDuplicatePreDecrypt().run();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void run() throws IOException {
		BufferedReader in = new BufferedReader(new FileReader("src/jpcsp/crypto/PreDecrypt.xml"));
		Writer out = new BufferedWriter(new FileWriter("PreDecrypt.xml"));

		Set<String> found = new HashSet<String>();
		StringBuilder current = new StringBuilder();
		StringBuilder currentTrimmed = new StringBuilder();
		boolean inside = false;
		String nl = System.lineSeparator();
		int countRemoved = 0;
		while (true) {
			String line = in.readLine();
			if (line == null) {
				break;
			}

			line += nl;

			if (line.contains("<PreDecryptInfo ")) {
				inside = true;
			}

			if (inside) {
				current.append(line);
				currentTrimmed.append(line.trim());
			} else {
				out.write(line);
			}

			if (line.contains("</PreDecryptInfo>")) {
				if (found.add(currentTrimmed.toString())) {
					out.write(current.toString());
				} else {
					countRemoved++;
				}

				current.setLength(0);
				currentTrimmed.setLength(0);
				inside = false;
			}
		}

		in.close();
		out.close();

		System.out.println(String.format("Removed %d duplicates", countRemoved));
	}
}
