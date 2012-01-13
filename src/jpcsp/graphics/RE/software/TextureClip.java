/*

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

/**
 * @author gid15
 *
 */
public class TextureClip implements IRandomTextureAccess {
	protected IRandomTextureAccess textureAccess;
	protected int width;
	protected int height;

	public TextureClip(IRandomTextureAccess textureAccess, int width, int height) {
		this.textureAccess = textureAccess;
		this.width = width;
		this.height = height;
	}

	@Override
	public int readPixel(int u, int v) {
		if (u < 0 || u >= width || v < 0 || v >= height) {
			return 0;
		}
		return textureAccess.readPixel(u, v);
	}
}
