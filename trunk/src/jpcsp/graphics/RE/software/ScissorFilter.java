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

/**
 * @author gid15
 *
 */
public class ScissorFilter {
	public static IPixelFilter getScissorFilter(int scissorX1, int scissorY1, int scissorX2, int scissorY2, boolean scissorX, boolean scissorY) {
		IPixelFilter filter;

		if (scissorX) {
			if (scissorY) {
				filter = new ScissorXY(scissorX1, scissorY1, scissorX2, scissorY2);
			} else {
				filter = new ScissorX(scissorX1, scissorX2);
			}
		} else {
			if (scissorY) {
				filter = new ScissorY(scissorY1, scissorY2);
			} else {
				filter = NopFilter.NOP;
			}
		}

		return filter;
	}

	private static final class ScissorXY implements IPixelFilter {
		private int x1, y1;
		private int x2, y2;

		public ScissorXY(int x1, int y1, int x2, int y2) {
			this.x1 = x1;
			this.y1 = y1;
			this.x2 = x2;
			this.y2 = y2;
		}

		@Override
		public void filter(PixelState pixel) {
			pixel.filterPassed = (pixel.x >= x1 && pixel.x <= x2 && pixel.y >= y1 && pixel.y <= y2);
		}

		@Override
		public int getCompilationId() {
			return 954625645;
		}

		@Override
		public int getFlags() {
			return 0;
		}
	}

	private static final class ScissorX implements IPixelFilter {
		private int x1, x2;

		public ScissorX(int x1, int x2) {
			this.x1 = x1;
			this.x2 = x2;
		}

		@Override
		public void filter(PixelState pixel) {
			pixel.filterPassed = (pixel.x >= x1 && pixel.x <= x2);
		}

		@Override
		public int getCompilationId() {
			return 897312919;
		}

		@Override
		public int getFlags() {
			return 0;
		}
	}

	private static final class ScissorY implements IPixelFilter {
		private int y1, y2;

		public ScissorY(int y1, int y2) {
			this.y1 = y1;
			this.y2 = y2;
		}

		@Override
		public void filter(PixelState pixel) {
			pixel.filterPassed = (pixel.y >= y1 && pixel.y <= y2);
		}

		@Override
		public int getCompilationId() {
			return 433974911;
		}

		@Override
		public int getFlags() {
			return 0;
		}
	}
}
