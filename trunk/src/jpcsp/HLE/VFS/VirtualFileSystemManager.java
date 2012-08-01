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
package jpcsp.HLE.VFS;

import java.util.HashMap;

public class VirtualFileSystemManager {
	protected HashMap<String, IVirtualFileSystem> virtualFileSystems = new HashMap<String, IVirtualFileSystem>();

	public void register(String name, IVirtualFileSystem vfs) {
		name = name.toLowerCase();
		virtualFileSystems.put(name, vfs);
	}

	public void unregister(String name) {
		name = name.toLowerCase();
		virtualFileSystems.remove(name);
	}

	public IVirtualFileSystem getVirtualFileSystem(String absoluteFileName, StringBuilder localFileName) {
		int colon = absoluteFileName.indexOf(':');
		if (colon < 0) {
			return null;
		}

		String name = absoluteFileName.substring(0, colon);
		name = name.toLowerCase();

		if (localFileName != null) {
			localFileName.setLength(0);
			localFileName.append(absoluteFileName.substring(colon + 1));

			// Delete any leading "/"
			if (localFileName.length() > 0 && localFileName.charAt(0) == '/') {
				localFileName.deleteCharAt(0);
			}

			// Delete any trailing "/"
			if (localFileName.length() > 0 && localFileName.charAt(localFileName.length() - 1) == '/') {
				localFileName.setLength(localFileName.length() - 1);
			}
		}

		return virtualFileSystems.get(name);
	}
}
