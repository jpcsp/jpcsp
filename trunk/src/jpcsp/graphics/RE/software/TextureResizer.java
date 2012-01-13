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

import org.apache.log4j.Logger;

import jpcsp.graphics.VideoEngine;

/**
 * @author gid15
 *
 */
public class TextureResizer {
	protected static final Logger log = VideoEngine.log;

	public static IRandomTextureAccess getImageResizer(IRandomTextureAccess textureAccess, int fromWidth, int fromHeight, int toWidth, int toHeight) {
		if (fromWidth != toWidth || fromHeight != toHeight) {
        	if (log.isTraceEnabled()) {
            	log.trace(String.format("Using ImageResizer: fromWidth=%d, fromHeight=%d, toWidth=%d, toHeight=%d", fromWidth, fromHeight, toWidth, toHeight));
            }
        	textureAccess = new TextureExtender(textureAccess, fromWidth, fromHeight, toWidth, toHeight);
		}

		return textureAccess;
	}

	private static final class TextureExtender implements IRandomTextureAccess {
		private IRandomTextureAccess textureAccess;
		private float stepX;
		private float stepY;

		public TextureExtender(IRandomTextureAccess textureAccess, int fromWidth, int fromHeight, int toWidth, int toHeight) {
			this.textureAccess = textureAccess;

			stepX = ((float) fromWidth) / toWidth;
			stepY = ((float) fromHeight) / toHeight;
		}

		@Override
		public int readPixel(int u, int v) {
			return textureAccess.readPixel((int) (u * stepX), (int) (v * stepY));
		}
	}
}
