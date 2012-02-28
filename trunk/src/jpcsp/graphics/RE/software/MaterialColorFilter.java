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

import static jpcsp.graphics.GeCommands.CMAT_FLAG_AMBIENT;
import static jpcsp.graphics.GeCommands.CMAT_FLAG_DIFFUSE;
import static jpcsp.graphics.GeCommands.CMAT_FLAG_SPECULAR;
import jpcsp.graphics.GeContext;

/**
 * @author gid15
 *
 */
public class MaterialColorFilter {
	public static IPixelFilter getMaterialColorFilter(GeContext context) {
		IPixelFilter filter = null;

		int flags = context.mat_flags;
		if (flags != 0) {
			filter = new MaterialColor((flags & CMAT_FLAG_AMBIENT) != 0, (flags & CMAT_FLAG_DIFFUSE) != 0, (flags & CMAT_FLAG_SPECULAR) != 0);
		}

		return filter;
	}

	private static class MaterialColor implements IPixelFilter {
		private final boolean ambient;
		private final boolean diffuse;
		private final boolean specular;

		public MaterialColor(boolean ambient, boolean diffuse, boolean specular) {
			this.ambient = ambient;
			this.diffuse = diffuse;
			this.specular = specular;
		}

		@Override
		public void filter(PixelState pixel) {
			if (ambient) {
				pixel.materialAmbient = pixel.primaryColor;
			}
			if (diffuse) {
				pixel.materialDiffuse = pixel.primaryColor;
			}
			if (specular) {
				pixel.materialSpecular = pixel.primaryColor;
			}
		}

		@Override
		public int getCompilationId() {
			return 414669041;
		}

		@Override
		public int getFlags() {
			return 0;
		}
	}
}
