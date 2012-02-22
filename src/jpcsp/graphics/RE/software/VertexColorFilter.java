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
import static jpcsp.util.Utilities.sameColor;

import org.apache.log4j.Logger;

import jpcsp.graphics.VideoEngine;

/**
 * @author gid15
 *
 */
public class VertexColorFilter {
	protected static final Logger log = VideoEngine.log;
	protected static IPixelFilter vertexTriangleTextureFilter = new VertexTriangleTextureFilter();

	public static IPixelFilter getVertexColorFilter(float[] c1, float c2[], float[] c3) {
		IPixelFilter filter;

		if (sameColor(c1, c2, c3)) {
        	if (log.isTraceEnabled()) {
        		log.trace(String.format("Using ColorTextureFilter color=0x%08X", getColor(c1)));
        	}
			filter = new ColorTextureFilter(c1);
		} else  {
        	if (log.isTraceEnabled()) {
        		log.trace(String.format("Using VertexTriangleTextureFilter color1=0x%08X, color2=0x%08X, color3=0x%08X", getColor(c1), getColor(c2), getColor(c3)));
        	}
			filter = vertexTriangleTextureFilter;
		}

		return filter;
	}

	private static final class VertexTriangleTextureFilter implements IPixelFilter {
		@Override
		public void filter(PixelState pixel) {
			pixel.primaryColor = pixel.getTriangleColorWeightedValue();
		}

		@Override
		public int getCompilationId() {
			return 493722550;
		}

		@Override
		public int getFlags() {
			return 0;
		}
	}

	private static final class ColorTextureFilter implements IPixelFilter {
		private int color;

		public ColorTextureFilter(float[] color) {
			this.color = getColor(color);
		}

		@Override
		public void filter(PixelState pixel) {
			pixel.primaryColor = color;
		}

		@Override
		public int getCompilationId() {
			return 903177108;
		}

		@Override
		public int getFlags() {
			return 0;
		}
	}
}
