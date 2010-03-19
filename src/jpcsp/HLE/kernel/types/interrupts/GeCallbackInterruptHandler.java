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
package jpcsp.HLE.kernel.types.interrupts;

public class GeCallbackInterruptHandler extends AbstractAllegrexInterruptHandler {
	public GeCallbackInterruptHandler(int address, int argument) {
		// call: PspGeCallback(int id, void *argument)
		super(address, 0, argument);
	}

	public int getId() {
		return getArgument(0);
	}

	public void setId(int id) {
		setArgument(0, id);
	}

	public int getArgument() {
		return getArgument(1);
	}

	public void setArgument(int argument) {
		setArgument(1, argument);
	}
}
