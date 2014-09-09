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
package jpcsp.media.codec.aac;

import jpcsp.util.Utilities;

public class Pulse {
	public int numPulse;
	public int start;
	public int pos[] = new int[4];
	public int amp[] = new int[4];

	public void copy(Pulse that) {
		numPulse = that.numPulse;
		start    = that.start;
		Utilities.copy(pos, that.pos);
		Utilities.copy(amp, that.amp);
	}
}
