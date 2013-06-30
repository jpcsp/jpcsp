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

import java.nio.charset.Charset;
import javax.swing.filechooser.FileNameExtensionFilter;

public class Constants {

    public static final FileNameExtensionFilter fltTextFiles = new FileNameExtensionFilter("Text files", "txt");
    public static final FileNameExtensionFilter fltMemoryBreakpointFiles = new FileNameExtensionFilter("Memory breakpoint files", "mbrk");
    public static final FileNameExtensionFilter fltBreakpointFiles = new FileNameExtensionFilter("Breakpoint files", "brk");
    public static final FileNameExtensionFilter fltXMLFiles = new FileNameExtensionFilter("XML files", "xml");
    public static final int ICON0_WIDTH = 144;
    public static final int ICON0_HEIGHT = 80;
    public static final int PSPSCREEN_WIDTH = 480;
    public static final int PSPSCREEN_HEIGHT = 272;
    public static final Charset charset = Charset.forName("UTF-8");
}
