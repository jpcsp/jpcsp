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

import jpcsp.HLE.TPointerFunction;

// See https://github.com/uofw/uofw/blob/master/include/interruptman.h
public class SceIntrCb extends pspAbstractMemoryMappedStructureVariableLength {
	public int unknown4;
	public TPointerFunction cbRegBefore;
	public TPointerFunction cbRegAfter;
	public TPointerFunction cbRelBefore;
	public TPointerFunction cbRelAfter;
	public TPointerFunction cbEnable;
	public TPointerFunction cbDisable;
	public TPointerFunction cbSuspend;
	public TPointerFunction cbResume;
	public TPointerFunction cbIsOccured;

	@Override
	protected void read() {
		super.read();
		unknown4 = read32();
		cbRegBefore = readPointerFunction();
		cbRegAfter = readPointerFunction();
		cbRelBefore = readPointerFunction();
		cbRelAfter = readPointerFunction();
		cbEnable = readPointerFunction();
		cbDisable = readPointerFunction();
		cbSuspend = readPointerFunction();
		cbResume = readPointerFunction();
		cbIsOccured = readPointerFunction();
	}

	@Override
	protected void write() {
		super.write();
		write32(unknown4);
		writePointerFunction(cbRegBefore);
		writePointerFunction(cbRegAfter);
		writePointerFunction(cbRelBefore);
		writePointerFunction(cbRelAfter);
		writePointerFunction(cbEnable);
		writePointerFunction(cbDisable);
		writePointerFunction(cbSuspend);
		writePointerFunction(cbResume);
		writePointerFunction(cbIsOccured);
	}
}
