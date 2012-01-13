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
public class PixelState {
	public int source;
	public int destination;
	public int sourceDepth;
	public int destinationDepth;
	public int x;
	public int y;
	public int primaryColor;
	public float u;
	public float v;

	public float triangleWeight1;
	public float triangleWeight2;
	public float triangleWeight3;

	public boolean filterPassed;

	public float getTriangleWeightedValue(float value1, float value2, float value3) {
		return triangleWeight1 * value1 + triangleWeight2 * value2 + triangleWeight3 * value3;
	}

	public int getTriangleWeightedValue(int value1, int value2, int value3) {
		return Math.round(triangleWeight1 * value1 + triangleWeight2 * value2 + triangleWeight3 * value3);
	}
}
