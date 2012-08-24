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
package jpcsp.HLE;

public interface ITPointerBase {
	/**
	 * Equivalent to
	 *    Memory.isAddressGood(getAddress()).
	 * 
	 * @return true  if the pointer address is good/valid.
	 *         false if the pointer address is not good/valid.
	 */
	public boolean isAddressGood();

	/**
	 * Tests if the pointer address is aligned on a given size.
	 * 
	 * @param offset  size of the alignment in bytes (e.g. 2 or 4)
	 * @return true  if the pointer address is aligned on offset.
	 *         false if the pointer address is not aligned on offset.
	 */
	public boolean isAlignedTo(int offset);

	/**
	 * @return the pointer address
	 */
	public int getAddress();

	/**
	 * Tests if the pointer address is NULL.
	 * Equivalent to
	 *    getAddress() == 0
	 * 
	 * @return true  if the pointer address is NULL.
	 *         false if the pointer address is not NULL.
	 */
	public boolean isNull();

	/**
	 * Tests if the pointer address is not NULL.
	 * Equivalent to
	 *    getAddress() != 0
	 * 
	 * @return true  if the pointer address is not NULL.
	 *         false if the pointer address is NULL.
	 */
	public boolean isNotNull();
}
