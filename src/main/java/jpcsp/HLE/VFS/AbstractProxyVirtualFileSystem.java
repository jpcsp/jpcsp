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

import java.io.IOException;
import java.util.Map;

import org.apache.log4j.Logger;

import jpcsp.HLE.TPointer;
import jpcsp.HLE.kernel.types.SceIoDirent;
import jpcsp.HLE.kernel.types.SceIoStat;
import jpcsp.HLE.modules.IoFileMgrForUser.IoOperation;
import jpcsp.HLE.modules.IoFileMgrForUser.IoOperationTiming;
import jpcsp.state.IState;
import jpcsp.state.StateInputStream;
import jpcsp.state.StateOutputStream;

/**
 * Proxy all the IVirtualFileSystem interface calls to another virtual file system.
 * 
 * @author gid15
 *
 */
public class AbstractProxyVirtualFileSystem implements IVirtualFileSystem, IState {
	protected static Logger log = AbstractVirtualFileSystem.log;
	protected IVirtualFileSystem vfs;

	protected AbstractProxyVirtualFileSystem() {
	}

	protected AbstractProxyVirtualFileSystem(IVirtualFileSystem vfs) {
		this.vfs = vfs;
	}

	protected void setProxyVirtualFileSystem(IVirtualFileSystem vfs) {
		this.vfs = vfs;
	}

	@Override
	public void ioInit() {
		vfs.ioInit();
	}

	@Override
	public void ioExit() {
		vfs.ioExit();
	}

	@Override
	public IVirtualFile ioOpen(String fileName, int flags, int mode) {
		return vfs.ioOpen(fileName, flags, mode);
	}

	@Override
	public int ioRemove(String name) {
		return vfs.ioRemove(name);
	}

	@Override
	public int ioMkdir(String name, int mode) {
		return vfs.ioMkdir(name, mode);
	}

	@Override
	public int ioRmdir(String name) {
		return vfs.ioRmdir(name);
	}

	@Override
	public String[] ioDopen(String dirName) {
		return vfs.ioDopen(dirName);
	}

	@Override
	public int ioDclose(String dirName) {
		return vfs.ioDclose(dirName);
	}

	@Override
	public int ioDread(String dirName, SceIoDirent dir) {
		return vfs.ioDread(dirName, dir);
	}

	@Override
	public int ioGetstat(String fileName, SceIoStat stat) {
		return vfs.ioGetstat(fileName, stat);
	}

	@Override
	public int ioChstat(String fileName, SceIoStat stat, int bits) {
		return vfs.ioChstat(fileName, stat, bits);
	}

	@Override
	public int ioRename(String oldFileName, String newFileName) {
		return vfs.ioRename(oldFileName, newFileName);
	}

	@Override
	public int ioChdir(String directoryName) {
		return vfs.ioChdir(directoryName);
	}

	@Override
	public int ioMount() {
		return vfs.ioMount();
	}

	@Override
	public int ioUmount() {
		return vfs.ioUmount();
	}

	@Override
	public int ioDevctl(String deviceName, int command, TPointer inputPointer, int inputLength, TPointer outputPointer, int outputLength) {
		return vfs.ioDevctl(deviceName, command, inputPointer, inputLength, outputPointer, outputLength);
	}

	@Override
	public Map<IoOperation, IoOperationTiming> getTimings() {
		return vfs.getTimings();
	}

	@Override
	public void read(StateInputStream stream) throws IOException {
		if (vfs instanceof IState) {
			((IState) vfs).read(stream);
		}
	}

	@Override
	public void write(StateOutputStream stream) throws IOException {
		if (vfs instanceof IState) {
			((IState) vfs).write(stream);
		}
	}
}
