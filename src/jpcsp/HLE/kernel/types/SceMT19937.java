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

import jpcsp.HLE.modules150.sceMt19937;

public class SceMT19937 extends pspAbstractMemoryMappedStructure {
	public int mti = sceMt19937.MT19937.N + 1;
	public int[] mt = new int[sceMt19937.MT19937.N];

	@Override
	protected void read() {
		mti = read32();
		read32Array(mt);
	}

	@Override
	protected void write() {
		write32(mti);
		write32Array(mt);
	}

	@Override
	public int sizeof() {
		return sceMt19937.MT19937.N * 4 + 4;
	}
}
