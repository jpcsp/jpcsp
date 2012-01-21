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

import static jpcsp.graphics.RE.software.PixelColor.ZERO;
import jpcsp.graphics.GeCommands;
import jpcsp.graphics.GeContext;

/**
 * @author gid15
 *
 */
public class LogicalOperationFilter {
	public static IPixelFilter getLogicalOperationFilter(GeContext context) {
		IPixelFilter filter = null;

		switch (context.logicOp) {
			case GeCommands.LOP_CLEAR:
				filter = new LogicalOperationClear();
				break;
			case GeCommands.LOP_AND:
				filter = new LogicalOperationAnd();
				break;
			case GeCommands.LOP_REVERSE_AND:
				filter = new LogicalOperationReverseAnd();
				break;
			case GeCommands.LOP_COPY:
				// This is a NOP
				break;
			case GeCommands.LOP_INVERTED_AND:
				filter = new LogicalOperationInvertedAnd();
				break;
			case GeCommands.LOP_NO_OPERATION:
				filter = new LogicalOperationNoOperation();
				break;
			case GeCommands.LOP_EXLUSIVE_OR:
				filter = new LogicalOperationExclusiveOr();
				break;
			case GeCommands.LOP_OR:
				filter = new LogicalOperationOr();
				break;
			case GeCommands.LOP_NEGATED_OR:
				filter = new LogicalOperationNegatedOr();
				break;
			case GeCommands.LOP_EQUIVALENCE:
				filter = new LogicalOperationEquivalence();
				break;
			case GeCommands.LOP_INVERTED:
				filter = new LogicalOperationInverted();
				break;
			case GeCommands.LOP_REVERSE_OR:
				filter = new LogicalOperationReverseOr();
				break;
			case GeCommands.LOP_INVERTED_COPY:
				filter = new LogicalOperationInvertedCopy();
				break;
			case GeCommands.LOP_INVERTED_OR:
				filter = new LogicalOperationInvertedOr();
				break;
			case GeCommands.LOP_NEGATED_AND:
				filter = new LogicalOperationNegatedAnd();
				break;
			case GeCommands.LOP_SET:
				filter = new LogicalOperationSet();
				break;
		}

		return filter;
	}

	private static final class LogicalOperationClear implements IPixelFilter {
		@Override
		public void filter(PixelState pixel) {
			pixel.source = ZERO;
		}
	}

	private static final class LogicalOperationAnd implements IPixelFilter {
		@Override
		public void filter(PixelState pixel) {
			pixel.source &= pixel.destination;
		}
	}

	private static final class LogicalOperationReverseAnd implements IPixelFilter {
		@Override
		public void filter(PixelState pixel) {
			pixel.source &= (~pixel.destination);
		}
	}

	private static final class LogicalOperationInvertedAnd implements IPixelFilter {
		@Override
		public void filter(PixelState pixel) {
			pixel.source = (~pixel.source) & pixel.destination;
		}
	}

	private static final class LogicalOperationNoOperation implements IPixelFilter {
		@Override
		public void filter(PixelState pixel) {
			pixel.source = pixel.destination;
		}
	}

	private static final class LogicalOperationExclusiveOr implements IPixelFilter {
		@Override
		public void filter(PixelState pixel) {
			pixel.source ^= pixel.destination;
		}
	}

	private static final class LogicalOperationOr implements IPixelFilter {
		@Override
		public void filter(PixelState pixel) {
			pixel.source |= pixel.destination;
		}
	}

	private static final class LogicalOperationNegatedOr implements IPixelFilter {
		@Override
		public void filter(PixelState pixel) {
			pixel.source = ~(pixel.source | pixel.destination);
		}
	}

	private static final class LogicalOperationEquivalence implements IPixelFilter {
		@Override
		public void filter(PixelState pixel) {
			pixel.source = ~(pixel.source ^ pixel.destination);
		}
	}

	private static final class LogicalOperationInverted implements IPixelFilter {
		@Override
		public void filter(PixelState pixel) {
			pixel.source = ~pixel.destination;
		}
	}

	private static final class LogicalOperationReverseOr implements IPixelFilter {
		@Override
		public void filter(PixelState pixel) {
			pixel.source |= (~pixel.destination);
		}
	}

	private static final class LogicalOperationInvertedCopy implements IPixelFilter {
		@Override
		public void filter(PixelState pixel) {
			pixel.source = ~pixel.source;
		}
	}

	private static final class LogicalOperationInvertedOr implements IPixelFilter {
		@Override
		public void filter(PixelState pixel) {
			pixel.source = (~pixel.source) | pixel.destination;
		}
	}

	private static final class LogicalOperationNegatedAnd implements IPixelFilter {
		@Override
		public void filter(PixelState pixel) {
			pixel.source = ~(pixel.source & pixel.destination);
		}
	}

	private static final class LogicalOperationSet implements IPixelFilter {
		@Override
		public void filter(PixelState pixel) {
			pixel.source = 0xFFFFFFFF;
		}
	}
}
