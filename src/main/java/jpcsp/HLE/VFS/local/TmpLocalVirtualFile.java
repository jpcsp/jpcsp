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
package jpcsp.HLE.VFS.local;

import java.util.Map;

import jpcsp.HLE.TPointer;
import jpcsp.HLE.VFS.AbstractProxyVirtualFile;
import jpcsp.HLE.VFS.IVirtualFile;
import jpcsp.HLE.modules.IoFileMgrForUser;
import jpcsp.HLE.modules.IoFileMgrForUser.IoOperation;
import jpcsp.HLE.modules.IoFileMgrForUser.IoOperationTiming;

public class TmpLocalVirtualFile extends AbstractProxyVirtualFile {
	protected IVirtualFile ioctl;

	public TmpLocalVirtualFile(IVirtualFile vFile, IVirtualFile ioctl) {
		super(vFile);
		this.ioctl = ioctl;
	}

	@Override
	public int ioIoctl(int command, TPointer inputPointer, int inputLength, TPointer outputPointer, int outputLength) {
		if (ioctl != null) {
			return ioctl.ioIoctl(command, inputPointer, inputLength, outputPointer, outputLength);
		}
		return super.ioIoctl(command, inputPointer, inputLength, outputPointer, outputLength);
	}

	@Override
	public int ioClose() {
		if (ioctl != null) {
			ioctl.ioClose();
		}
		return super.ioClose();
	}

	@Override
	public Map<IoOperation, IoOperationTiming> getTimings() {
		return IoFileMgrForUser.noDelayTimings;
	}
}
