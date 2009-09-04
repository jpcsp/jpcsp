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

package jpcsp.util;

import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;

public class MetaInformation {
    public static String NAME = "Jpcsp";
    public static String VERSION = "v0.3";
    public static String FULL_NAME = NAME + " " + VERSION;
    public static String FULL_CUSTOM_NAME = NAME + " " + VERSION;
    public static String OFFICIAL_SITE = "http://jpcsp.org/";
    public static String OFFICIAL_FORUM = "http://jpcsp.org/forum/";
    public static String OFFICIAL_REPOSITORY = "http://code.google.com/p/jpcsp/";
    public static String TEAM = "shadow, mad, dreampeppers99, wrayal, fiveofhearts, hlide, Nutzje<br />aisesal, shashClp, spip2, mozvip, Orphis, gigaherz, gid15";

    private static MetaInformation singleton;

    private MetaInformation() {
        try {
            File f = new File(getClass().getResource("/jpcsp/title.txt").toURI());
            FileReader fr = new FileReader(f);
            BufferedReader br = new BufferedReader(fr);
            String customName = br.readLine();

            if (customName != null)
                FULL_CUSTOM_NAME = NAME + " " + VERSION + " " + customName;

			br.close();
			fr.close();
		} catch(Exception e) {
			// just ignore it, custom title is optional
		}
    }

    static {
        singleton = new MetaInformation();
    }
}
