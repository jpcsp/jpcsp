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

import static jpcsp.util.Utilities.maxInt;
import static jpcsp.util.Utilities.minInt;

/**
 * @author gid15
 *
 */
public class Rasterizer {
	// There is no advantage of using a Rasterizer when rendering an area smaller
	// than the given width and height.
	// The overhead of the Rasterizer would be too high.
	public static final int MINIMUM_WIDTH = 4;
	public static final int MINIMUM_HEIGHT = 4;
	private Edge[] edges = new Edge[3];
	private int longEdge;
	private int shortEdge1;
	private int shortEdge2;
	private int currentEdge;
	float xdiff1;
	float xdiff2;
	float factor1;
	float factorStep1;
	float factor2;
	float factorStep2;
	float y2;

	public Rasterizer(float x1, float y1, float x2, float y2, float x3, float y3, int yMin, int yMax) {
		edges[0] = new Edge(x1, y1, x2, y2);
		edges[1] = new Edge(x2, y2, x3, y3);
		edges[2] = new Edge(x3, y3, x1, y1);

		longEdge = 0;
		float maxLength = edges[longEdge].getLengthY();
		for (int i = 1; i < edges.length; i++) {
			float length = edges[i].getLengthY();
			if (length > maxLength) {
				maxLength = length;
				longEdge = i;
			}
		}

		shortEdge1 = (longEdge + 1) % edges.length;
		shortEdge2 = (longEdge + 2) % edges.length;

		if (edges[shortEdge1].y1 > edges[shortEdge2].y1) {
			// Switch short edges to start with the one with the lowest "y"
			int tmp = shortEdge1;
			shortEdge1 = shortEdge2;
			shortEdge2 = tmp;
		}

		currentEdge = shortEdge1;
		if (!init(longEdge, currentEdge)) {
			currentEdge = shortEdge2;
			init(longEdge, currentEdge);
		}
	}

	public void setY(float y) {
		if (y == y2) {
			return;
		}
		if (currentEdge == shortEdge1 && y > edges[currentEdge].y2) {
			currentEdge = shortEdge2;
			init(longEdge, currentEdge);
			if (y2 >= y) {
				return;
			}
		}
		float diff = y - y2;
		factor1 += diff * factorStep1;
		factor2 += diff * factorStep2;
		y2 = y;
	}

	private boolean init(int edge1, int edge2) {
		if (edges[edge2].getLengthY() <= 0) {
			y2 = edges[edge2].y2;
			return false;
		}
		float ydiff1 = edges[edge1].y2 - edges[edge1].y1;
		float ydiff2 = edges[edge2].y2 - edges[edge2].y1;
		xdiff1 = edges[edge1].x2 - edges[edge1].x1;
		xdiff2 = edges[edge2].x2 - edges[edge2].x1;
		factor1 = (edges[edge2].y1 - edges[edge1].y1) / ydiff1;
		factorStep1 = 1.f / ydiff1;
		factor2 = 0.f;
		factorStep2 = 1.f / ydiff2;
		y2 = edges[edge2].y1;

		return true;
	}

	public void getNextRange(Range range) {
		if (y2 >= edges[currentEdge].y2) {
			if (currentEdge == shortEdge2) {
				range.clear();
				return;
			}
			currentEdge = shortEdge2;
			if (!init(longEdge, currentEdge)) {
				range.clear();
				return;
			}
		}

		final int edge1 = longEdge;
		final int edge2 = currentEdge;
		float x1 = edges[edge1].x1 + xdiff1 * factor1;
		float x2 = edges[edge2].x1 + xdiff2 * factor2;
		factor1 += factorStep1;
		factor2 += factorStep2;

		range.setRange(x1, x2);
		y2++;
	}

	public static class Range {
		public int xMin;
		public int xMax;

		public void clear() {
			xMin = 0;
			xMax = 0;
		}

		public void setRange(float x1, float x2) {
			xMin = minInt(x1, x2); // minimum value rounded down
			xMax = maxInt(x1, x2); // maximum value rounded up
		}

		@Override
		public String toString() {
			return String.format("[%d-%d]", xMin, xMax);
		}
	}

	private static class Edge {
		protected float x1, y1;
		protected float x2, y2;

		public Edge(float x1, float y1, float x2, float y2) {
			if (y1 <= y2) {
				this.x1 = x1;
				this.y1 = y1;
				this.x2 = x2;
				this.y2 = y2;
			} else {
				this.x1 = x2;
				this.y1 = y2;
				this.x2 = x1;
				this.y2 = y1;
			}
		}

		public float getLengthY() {
			return y2 - y1;
		}

		@Override
		public String toString() {
			return String.format("(%d,%d)-(%d,%d)", x1, y1, x2, y2);
		}
	}
}
