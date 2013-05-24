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

public interface IVirtualFile {
	public int ioClose();
	public int ioRead(TPointer outputPointer, int outputLength);
	public int ioRead(byte[] outputBuffer, int outputOffset, int outputLength);
	public int ioWrite(TPointer inputPointer, int inputLength);
	public int ioWrite(byte[] inputBuffer, int inputOffset, int inputLength);
	public long ioLseek(long offset);
	public int ioIoctl(int command, TPointer inputPointer, int inputLength, TPointer outputPointer, int outputLength);
	public long length();
	public boolean isSectorBlockMode();
	public long getPosition();
	public IVirtualFile duplicate();
}
