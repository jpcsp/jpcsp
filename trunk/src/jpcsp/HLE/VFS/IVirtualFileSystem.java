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

import jpcsp.HLE.TPointer;
import jpcsp.HLE.kernel.types.SceIoDirent;
import jpcsp.HLE.kernel.types.SceIoStat;

public interface IVirtualFileSystem {
	public void ioInit();
	public void ioExit(); 
	public IVirtualFile ioOpen(String fileName, int flags, int mode);
	public int ioClose(IVirtualFile file);
	public int ioRead(IVirtualFile file, TPointer outputPointer, int outputLength);
	public int ioWrite(IVirtualFile file, TPointer inputPointer, int inputLength);
	public long ioLseek(IVirtualFile file, long offset);
	public int ioIoctl(IVirtualFile file, int command, TPointer inputPointer, int inputLength, TPointer outputPointer, int outputLength);
	public int ioRemove(String name);
	public int ioMkdir(String name, int mode);
	public int ioRmdir(String name);
	public String[] ioDopen(String dirName);
	public int ioDclose(String dirName);
	public int ioDread(String dirName, SceIoDirent dir);
	public int ioGetstat(String fileName, SceIoStat stat);
	public int ioChstat(String fileName, SceIoStat stat, int bits);
	public int ioRename(String oldFileName, String newFileName);
	public int ioChdir(String directoryName);
	public int ioMount();
	public int ioUmount();
	public int ioDevctl(String deviceName, int command, TPointer inputPointer, int inputLength, TPointer outputPointer, int outputLength);
}
