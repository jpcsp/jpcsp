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
import jpcsp.HLE.TPointerFunction;

public class pspUsbDriver extends pspAbstractMemoryMappedStructure {
	public TPointer namePtr;
	public String name;
	public int endpoints;
	public TPointer endp;
	public TPointer intp;
	public TPointer devp_hi;
	public TPointer confp_hi;
	public TPointer devp;
	public TPointer confp;
	public TPointer stringDescriptor;
	public TPointerFunction recvctl;
	public TPointerFunction func28;
	public TPointerFunction attach;
	public TPointerFunction detach;
	public int unk34;
	public TPointerFunction start_func;
	public TPointerFunction stop_func;
	public TPointer link;

	@Override
	protected void read() {
		namePtr = readPointer();
		name = namePtr.getStringZ();
		endpoints = read32();
		endp = readPointer();
		intp = readPointer();
		devp_hi = readPointer();
		confp_hi = readPointer();
		devp = readPointer();
		confp = readPointer();
		stringDescriptor = readPointer();
		recvctl = readPointerFunction();
		func28 = readPointerFunction();
		attach = readPointerFunction();
		detach = readPointerFunction();
		unk34 = read32();
		start_func = readPointerFunction();
		stop_func = readPointerFunction();
		link = readPointer();
	}

	@Override
	protected void write() {
		writePointer(namePtr);
		write32(endpoints);
		writePointer(endp);
		writePointer(intp);
		writePointer(devp_hi);
		writePointer(confp_hi);
		writePointer(devp);
		writePointer(confp);
		writePointer(stringDescriptor);
		writePointerFunction(recvctl);
		writePointerFunction(func28);
		writePointerFunction(attach);
		writePointerFunction(detach);
		write32(unk34);
		writePointerFunction(start_func);
		writePointerFunction(stop_func);
		writePointer(link);
	}

	@Override
	public int sizeof() {
		return 68;
	}

	@Override
	public String toString() {
		return String.format("pspUsbDriver name='%s', endpoints=%d", name, endpoints);
	}
}
