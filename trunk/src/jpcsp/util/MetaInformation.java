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

import java.io.IOException;
import java.io.InputStream;

public class MetaInformation {
    public static String NAME = "Jpcsp";
    public static String VERSION = "v0.5";
    public static String FULL_NAME = NAME + " " + VERSION;
    public static String FULL_CUSTOM_NAME = NAME + " " + VERSION;
    public static String OFFICIAL_SITE = "http://jpcsp.org/";
    public static String OFFICIAL_FORUM = "http://jpcsp.org/forum/";
    public static String OFFICIAL_REPOSITORY = "http://code.google.com/p/jpcsp/";
    public static String TEAM = "shadow, mad, dreampeppers99, wrayal, fiveofhearts, hlide, Nutzje,<br/>aisesal, shashClp, spip2, mozvip, Orphis, gigaherz, gid15, hykem,<br/>Drakon, raziel1000, theball, J_BYYX";

    private static MetaInformation singleton;

    private MetaInformation() {
        //System.err.println("MetaInformation loading...");
        try {
            InputStream is = getClass().getResourceAsStream("/jpcsp/title.txt");
            String customName = Utilities.toString(is, true);
            if (customName.isEmpty()) {
                System.err.println("first line of title.txt is blank or file is empty");
            } else {
                //System.err.println("found title '" + customName + "'");
                FULL_CUSTOM_NAME = NAME + " " + VERSION + " " + customName;
            }
        } catch (IOException e) {
            System.err.println("something went wrong: " + e.getMessage());
            e.printStackTrace();
        } catch (NullPointerException ex) {
            // optional file ...
            // System.err.println("title.txt is missing");
        }
    }

    static {
        singleton = new MetaInformation();
    }
}
