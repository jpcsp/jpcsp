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
public class ScissorDepthFilter {
	public static IPixelFilter getScissorDepthFilter(GeContext context, int nearZ, int farZ) {
		IPixelFilter filter = null;

		if (nearZ != 0x0000 || farZ != 0xFFFF) {
			filter = new ScissorDepth(nearZ, farZ);
		}

		return filter;
	}

	private static final class ScissorDepth implements IPixelFilter {
		private int nearZ;
		private int farZ;

		public ScissorDepth(int nearZ, int farZ) {
			this.nearZ = nearZ;
			this.farZ = farZ;
		}

		@Override
		public void filter(PixelState pixel) {
			pixel.filterPassed = pixel.sourceDepth >= nearZ && pixel.sourceDepth <= farZ;
		}

		@Override
		public int getCompilationId() {
			return 889156152;
		}

		@Override
		public int getFlags() {
			return REQUIRES_SOURCE_DEPTH;
		}
	}
}
