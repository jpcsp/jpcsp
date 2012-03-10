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
package jpcsp.connector;

import java.io.File;

import jpcsp.settings.Settings;

/**
 * @author gid15
 *
 */
public class Connector {
	public static final String baseDirectory = Settings.getInstance().readString("emu.tmppath") + File.separatorChar;
	public static final String basePSPDirectory = "ms0:/tmp/";
	public static final String jpcspConnectorName = "Jpcsp Connector 3xx";
	public static final String commandFileName = "command.txt";
}
