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

import static jpcsp.graphics.RE.software.PixelColor.getColor;

/**
 * @author gid15
 *
 */
public class ColorSourceReader implements ISourceReader {
	private int color;

	public ColorSourceReader(int color) {
		this.color = color;
	}

	public ColorSourceReader(float[] color) {
		this.color = getColor(color);
	}

	@Override
	public int read(PixelState pixel) {
		return color;
	}
}
