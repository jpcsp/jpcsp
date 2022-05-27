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
package jpcsp.memory.mmio;

import jpcsp.state.IState;

public interface IMMIOHandler extends IState {
	public void reset();
	public int read8(int address);
	public int read16(int address);
	public int read32(int address);
	public void write8(int address, byte value);
	public void write16(int address, short value);
	public void write32(int address, int value);
	public int internalRead8(int address);
	public int internalRead16(int address);
	public int internalRead32(int address);
}
