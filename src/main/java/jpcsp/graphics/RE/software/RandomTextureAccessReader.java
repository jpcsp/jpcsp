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

import jpcsp.memory.IMemoryReader;

/**
 * @author gid15
 *
 * Provide a random access for a texture provided having only a sequential access (IMemoryReader).
 */
public class RandomTextureAccessReader implements IRandomTextureAccess {
	protected int width;
	protected int height;
	protected final int[] pixels;

	public RandomTextureAccessReader(IMemoryReader imageReader, int width, int height) {
		this.width = width;
		this.height = height;
		// Read the whole texture into the "pixels" array
		final int length = width * height;
		pixels = new int[length];
		for (int i = 0; i < length; i++) {
			pixels[i] = imageReader.readNext();
		}
	}

	@Override
	public int readPixel(int u, int v) {
		return pixels[v * width + u];
	}

	@Override
	public int getWidth() {
		return width;
	}

	@Override
	public int getHeight() {
		return height;
	}
}
