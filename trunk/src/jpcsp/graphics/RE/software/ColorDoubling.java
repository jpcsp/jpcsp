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

import static jpcsp.graphics.GeCommands.LMODE_SEPARATE_SPECULAR_COLOR;
import static jpcsp.graphics.RE.software.PixelColor.addBGR;

import org.apache.log4j.Logger;

import jpcsp.graphics.GeContext;
import jpcsp.graphics.VideoEngine;

/**
 * @author gid15
 *
 */
public class ColorDoubling {
	protected static final Logger log = VideoEngine.log;

	public static IPixelFilter getColorDoubling(GeContext context, boolean usePrimaryColor, boolean primaryColorSetGlobally) {
		IPixelFilter filter = null;

		if (context.textureColorDoubled) {
			if (!context.vinfo.transform2D && context.lightingFlag.isEnabled() && context.lightMode == LMODE_SEPARATE_SPECULAR_COLOR) {
				if (usePrimaryColor) {
					if (log.isTraceEnabled()) {
						log.trace(String.format("Using PrimarySecondaryColorDoubling"));
					}
					filter = new PrimarySecondaryColorDoubling();
				} else {
					if (log.isTraceEnabled()) {
						log.trace(String.format("Using SourceSecondaryColorDoubling"));
					}
					filter = new SourceSecondaryColorDoubling();
				}
			} else {
				if (primaryColorSetGlobally) {
					// Nothing to do, the primary color is already set and doubled.
				} else if (usePrimaryColor) {
					if (log.isTraceEnabled()) {
						log.trace(String.format("Using PrimaryColorDoubling"));
					}
					filter = new PrimaryColorDoubling();
				} else {
					if (log.isTraceEnabled()) {
						log.trace(String.format("Using SourceColorDoubling"));
					}
					filter = new SourceColorDoubling();
				}
			}
		}

		return filter;
	}

	public static final int doubleColor(int color) {
		return addBGR(color, color) | (color & 0xFF000000);
	}

	private static final class SourceColorDoubling implements IPixelFilter {
		@Override
		public void filter(PixelState pixel) {
			pixel.source = doubleColor(pixel.source);
		}

		@Override
		public int getCompilationId() {
			return 798941819;
		}

		@Override
		public int getFlags() {
			return 0;
		}
	}

	private static final class SourceSecondaryColorDoubling implements IPixelFilter {
		@Override
		public void filter(PixelState pixel) {
			pixel.source = doubleColor(pixel.source);
			pixel.secondaryColor = doubleColor(pixel.secondaryColor);
		}

		@Override
		public int getCompilationId() {
			return 811210335;
		}

		@Override
		public int getFlags() {
			return 0;
		}
	}

	private static final class PrimaryColorDoubling implements IPixelFilter {
		@Override
		public void filter(PixelState pixel) {
			pixel.primaryColor = doubleColor(pixel.primaryColor);
		}

		@Override
		public int getCompilationId() {
			return 435080808;
		}

		@Override
		public int getFlags() {
			return 0;
		}
	}

	private static final class PrimarySecondaryColorDoubling implements IPixelFilter {
		@Override
		public void filter(PixelState pixel) {
			pixel.primaryColor = doubleColor(pixel.primaryColor);
			pixel.secondaryColor = doubleColor(pixel.secondaryColor);
		}

		@Override
		public int getCompilationId() {
			return 49715063;
		}

		@Override
		public int getFlags() {
			return 0;
		}
	}
}
