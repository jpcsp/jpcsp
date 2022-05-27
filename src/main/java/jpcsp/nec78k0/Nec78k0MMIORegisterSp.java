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
package jpcsp.nec78k0;

import static jpcsp.nec78k0.Nec78k0Processor.SP_ADDRESS;
import static jpcsp.util.Utilities.u16;

/**
 * @author gid15
 *
 */
public class Nec78k0MMIORegisterSp extends Nec78k0MMIOHandlerBase {
	public Nec78k0MMIORegisterSp() {
		super(SP_ADDRESS);
	}

	@Override
	public int read16(int address) {
		return processor.getSp();
	}

	@Override
	public void write16(int address, short value) {
		processor.setSp(u16(value));
	}
}
