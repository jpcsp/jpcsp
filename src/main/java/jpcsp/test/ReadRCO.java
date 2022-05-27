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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import jpcsp.format.RCO;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

public class ReadRCO {
	public static void main(String[] args) {
        DOMConfigurator.configure("LogSettings.xml");
        Logger log = Logger.getLogger("rco");
		try {
			File file = new File(args[0]);
			InputStream is = new FileInputStream(file);
			byte[] buffer = new byte[(int) file.length()];
			is.read(buffer);
			is.close();
			RCO rco = new RCO(buffer);
			log.info(String.format("Read RCO: %s", rco));
			rco.execute(null, "UNKNOWN");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
