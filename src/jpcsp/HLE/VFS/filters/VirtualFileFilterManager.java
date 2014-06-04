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
package jpcsp.HLE.VFS.filters;

import jpcsp.State;
import jpcsp.HLE.VFS.IVirtualFile;

/**
 * @author gid15
 *
 */
public class VirtualFileFilterManager {
	private static VirtualFileFilterManager instance;

	public static VirtualFileFilterManager getInstance() {
		if (instance == null) {
			instance = new VirtualFileFilterManager();
		}

		return instance;
	}

	private VirtualFileFilterManager() {
	}

	public IVirtualFileFilter getFilter() {
		// This should be configurable through the game specific patch files.
		if ("NPJH50676".equals(State.discId)) {
			return new XorVirtualFileFilter((byte) 0x7B);
		}

		return null;
	}

	public IVirtualFile getFilteredVirtualFile(IVirtualFile vFile) {
		IVirtualFileFilter filter = getFilter();
		if (filter == null) {
			return vFile;
		}

		filter.setVirtualFile(vFile);
		return filter;
	}

	public void filter(byte[] data, int offset, int length) {
		IVirtualFileFilter filter = getFilter();
		if (filter != null) {
			filter.filter(data, offset, length);
		}
	}
}
