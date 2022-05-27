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
package jpcsp.Allegrex.compiler;

public class StackPopException extends Exception {
	private static final long serialVersionUID = -5573324070282200237L;
	private int ra;

	public StackPopException(int ra) {
		this.ra = ra;
	}

	public int getRa() {
		return ra;
	}

	@Override
	public String toString() {
		return String.format("StackPopException(0x%08X)", ra);
	}
}
