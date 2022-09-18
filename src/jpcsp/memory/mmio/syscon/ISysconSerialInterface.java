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
package jpcsp.memory.mmio.syscon;

import jpcsp.state.IState;

/**
 * Interface for the sub-system connected to the syscon serial interface.
 * 
 * @author gid15
 *
 */
public interface ISysconSerialInterface extends IState {
	public void reset();
	public void startTransmission();
	public void transmit(int value);
	public void startReception();
	public int receive();
}
