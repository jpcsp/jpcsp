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
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class CreateJpcspTraceConfig {
	public static void main(String[] args) {
		boolean outputUnimplemented = false;
		String fileName = "src/jpcsp/HLE/modules150/scePsmfPlayer.java";
		try {
			BufferedReader reader = new BufferedReader(new FileReader(fileName));
			String nid = null;
			boolean isUnimplemented = false;
			while (true) {
				String line = reader.readLine();
				if (line == null) {
					break;
				}
				line = line.trim();
				if (line.startsWith("@HLEUnimplemented")) {
					isUnimplemented = true;
				} else if (line.startsWith("@HLEFunction")) {
					nid = line.replaceFirst("^.*nid *= *(0x[0-9a-fA-F]+).*$", "$1");
				} else if (nid != null) {
					if (outputUnimplemented || !isUnimplemented) {
						String functionName = line.replaceFirst("^.*public\\s+\\w+\\s+(\\w+)\\s*\\(.*$", "$1");
						int numberOfParameters = 0;
						// if the function has some parameters, e.g. not matching "public xxx sceXxxx()"...
						if (!line.matches("^.*public\\s+\\w+\\s+(\\w+)\\s*\\(\\s*\\).*")) {
							// ...the number of parameters is the number of "," + 1
							numberOfParameters = line.replaceAll("[^,]", "").length() + 1;
						}
						System.out.println(functionName + " " + nid + " " + numberOfParameters);
					}

					// Reset for the next function
					nid = null;
					isUnimplemented = false;
				}
			}
			reader.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
