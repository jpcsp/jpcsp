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

/**
 * Same as StopThreadException, but not requiring a "throws" for the function throwing it.
 *
 */
public class StopThreadRuntimeException extends RuntimeException {
	private static final long serialVersionUID = -4061175846715802650L;

	public StopThreadRuntimeException(String message) {
		super(message);
	}
}
