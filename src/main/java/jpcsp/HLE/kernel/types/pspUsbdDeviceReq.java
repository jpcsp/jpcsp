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
package jpcsp.HLE.kernel.types;

import jpcsp.HLE.TPointer;

public class pspUsbdDeviceReq extends pspAbstractMemoryMappedStructure {
	public TPointer endp;
	public TPointer data;
	public int size;
	public int unkC;
	public TPointer func; // function to call on completion
	public int recvsize;
	public int retcode;
	public int nextRequest;
	public int arg;
	public TPointer link;

	@Override
	protected void read() {
		endp = readPointer();
		data = readPointer();
		size = read32();
		unkC = read32();
		func = readPointer();
		recvsize = read32();
		retcode = read32();
		nextRequest = read32();
		arg = read32();
		link = readPointer();
	}

	@Override
	protected void write() {
		writePointer(endp);
		writePointer(data);
		write32(size);
		write32(unkC);
		writePointer(func);
		write32(recvsize);
		write32(retcode);
		write32(nextRequest);
		write32(arg);
		writePointer(link);
	}

	@Override
	public int sizeof() {
		return 40;
	}
}
