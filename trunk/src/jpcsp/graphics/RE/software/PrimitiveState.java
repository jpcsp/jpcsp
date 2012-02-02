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
public class PrimitiveState {
	public float p1x, p1y, p1z, p1w, p1wInverted;
	public float p2x, p2y, p2z, p2w, p2wInverted;
	public float p3x, p3y, p3z, p3w, p3wInverted;
	public int pxMin, pxMax, pyMin, pyMax, pzMin, pzMax;
	public float t1u, t1v;
	public float t2u, t2v;
	public float t3u, t3v;
	public int tuMin, tuMax, tvMin, tvMax;
	public float uStart, uStep;
	public float vStart, vStep;
	public int destinationWidth;
	public int destinationHeight;

	// Pre-computed values for triangle weights
	public float diff13x;
	public float diff13y;
	public float diff32x;
	public float diff23y;
	public float denomInverted;

	public void preComputeTriangleWeights() {
		diff13x = p1x - p3x;
		diff13y = p1y - p3y;
		diff32x = p3x - p2x;
		diff23y = p2y - p3y;

		float denom = diff23y * diff13x + diff32x * diff13y;
		denomInverted = 1.0f / denom;
	}

	public void computeTriangleWeights(PixelState pixel) {
		// Based on http://en.wikipedia.org/wiki/Barycentric_coordinates_%28mathematics%29
		//
		// All the values independent of the current pixel haven been pre-computed
		// in preComputeTriangleWeights().
		//
		float diff03x = pixel.x - p3x;
		float diff03y = pixel.y - p3y;
		pixel.triangleWeight1 = (diff23y * diff03x + diff32x * diff03y) * denomInverted;
		pixel.triangleWeight2 = (diff13x * diff03y - diff13y * diff03x) * denomInverted;
		pixel.triangleWeight3 = 1.f - (pixel.triangleWeight1 + pixel.triangleWeight2);
	}

	/**
	 * Update the triangle weights (Barycentric coordinates) by knowing that
	 *    pixel.x
	 * has been incremented by 1.
	 */
	public void deltaXTriangleWeigths(PixelState pixel) {
		// When pixel.x is incremented by 1, diff03x is also incremented by 1.
		// Which leads to simple increments of the triangle weights.
		pixel.triangleWeight1 += diff23y * denomInverted;
		pixel.triangleWeight2 -= diff13y * denomInverted;
		pixel.triangleWeight3 = 1.f - (pixel.triangleWeight1 + pixel.triangleWeight2);
	}

	public boolean isClockwise() {
		float crossProduct = (p2x - p1x) * (p3y - p1y) - (p2y - p1y) * (p3x - p1x);
		return crossProduct >= 0.f;
	}
}
