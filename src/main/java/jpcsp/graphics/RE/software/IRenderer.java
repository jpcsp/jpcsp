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
package jpcsp.graphics.RE.software;

import jpcsp.graphics.GeContext;

/**
 * @author gid15
 *
 */
public interface IRenderer {
	/**
	 * Prepare the renderer so that the rendering can be
	 * performed asynchronously, possibly in a different thread.
	 * After the preparation, the context cannot be accessed any more.
	 * 
	 * @return            true if something has to be rendered
	 *                    false if nothing has to be rendered. It is not
	 *                          valid to call render() when this prepare
	 *                          method has returned false.
	 */
	public boolean prepare(GeContext context);

	/**
	 * Render the primitive. This method is only allowed to access class
	 * variables. The GeContext cannot be accessed.
	 * This method can be called asynchronously and in a different thread.
	 */
	public void render();

	public IRenderer duplicate();
}
