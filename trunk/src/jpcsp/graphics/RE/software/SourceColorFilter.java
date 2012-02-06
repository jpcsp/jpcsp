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
import static jpcsp.graphics.RE.software.PixelColor.add;

import org.apache.log4j.Logger;

import jpcsp.graphics.GeContext;
import jpcsp.graphics.VideoEngine;

/**
 * @author gid15
 *
 */
public class SourceColorFilter {
	protected static final Logger log = VideoEngine.log;

	public static IPixelFilter getSourceColorFilter(GeContext context, boolean usePrimaryColor) {
		IPixelFilter filter = null;

		if (!context.vinfo.transform2D && context.lightingFlag.isEnabled() && context.lightMode == LMODE_SEPARATE_SPECULAR_COLOR) {
			if (usePrimaryColor) {
		    	if (log.isTraceEnabled()) {
		        	log.trace(String.format("Using SourceColorFilter source = primary + secondary"));
		        }
				filter = new SourcePrimarySecondary();
			} else {
		    	if (log.isTraceEnabled()) {
		        	log.trace(String.format("Using SourceColorFilter source = source + secondary"));
		        }
				filter = new SourceSecondary();
			}
		} else {
			if (usePrimaryColor) {
		    	if (log.isTraceEnabled()) {
		        	log.trace(String.format("Using SourceColorFilter source = primary"));
		        }
				filter = new SourcePrimary();
			}
		}

		return filter;
	}

	private static final class SourcePrimary implements IPixelFilter {
		@Override
		public void filter(PixelState pixel) {
			pixel.source = pixel.primaryColor;
		}

		@Override
		public int getCompilationId() {
			return 218776433;
		}

		@Override
		public int getFlags() {
			return 0;
		}
	}

	private static final class SourcePrimarySecondary implements IPixelFilter {
		@Override
		public void filter(PixelState pixel) {
			pixel.source = add(pixel.primaryColor, pixel.secondaryColor);
		}

		@Override
		public int getCompilationId() {
			return 521641523;
		}

		@Override
		public int getFlags() {
			return 0;
		}
	}

	private static final class SourceSecondary implements IPixelFilter {
		@Override
		public void filter(PixelState pixel) {
			pixel.source = add(pixel.source, pixel.secondaryColor);
		}

		@Override
		public int getCompilationId() {
			return 766222180;
		}

		@Override
		public int getFlags() {
			return 0;
		}
	}
}
