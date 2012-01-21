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

import static jpcsp.util.Utilities.normalize3;
import static jpcsp.util.Utilities.round;
import static jpcsp.util.Utilities.vectorMult33;
import static jpcsp.util.Utilities.vectorMult34;

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
	public int secondaryColor;
	public float u;
	public float v;
	public float q;
	private final float[] V = new float[] { 0.f, 0.f, 0.f, 1.f };
	private final float[] Ve = new float[3];
	private final float[] N = new float[] { 0.f, 0.f, 1.f };
	private final float[] Ne = new float[] { 0.f, 0.f, 1.f };
	private final float[] normalizedNe = new float[] { 0.f, 0.f, 1.f };
	private boolean computedV;
	private boolean computedN;
	private boolean computedVe;
	private boolean computedNe;
	private boolean computedNormalizedNe;
	public boolean hasNormal;
	public float v1x, v1y, v1z;
	public float v2x, v2y, v2z;
	public float v3x, v3y, v3z;
	public float n1x, n1y, n1z;
	public float n2x, n2y, n2z;
	public float n3x, n3y, n3z;
	public final float[] viewMatrix = new float[16];
	public final float[] modelViewMatrix = new float[16];
	public final float[] modelViewProjectionMatrix = new float[16];

	public float triangleWeight1;
	public float triangleWeight2;
	public float triangleWeight3;

	public boolean filterPassed;

	public float getTriangleWeightedValue(float value1, float value2, float value3) {
		return triangleWeight1 * value1 + triangleWeight2 * value2 + triangleWeight3 * value3;
	}

	public int getTriangleWeightedValue(int value1, int value2, int value3) {
		return round(triangleWeight1 * value1 + triangleWeight2 * value2 + triangleWeight3 * value3);
	}

	public void getTriangleWeightedValue(float[] result, float[] values1, float[] values2, float[] values3) {
		result[0] = getTriangleWeightedValue(values1[0], values2[0], values3[0]);
		result[1] = getTriangleWeightedValue(values1[1], values2[1], values3[1]);
		result[2] = getTriangleWeightedValue(values1[2], values2[2], values3[2]);
	}

	public boolean isInsideTriangle() {
		return triangleWeight1 >= 0.f && triangleWeight2 >= 0.f && triangleWeight3 >= 0.f;
	}

	public void newPixel() {
		computedV = false;
		computedN = false;
		computedVe = false;
		computedNe = false;
		computedNormalizedNe = false;
	}

	private void computeV() {
		if (!computedV) {
			V[0] = getTriangleWeightedValue(v1x, v2x, v3x);
			V[1] = getTriangleWeightedValue(v1y, v2y, v3y);
			V[2] = getTriangleWeightedValue(v1z, v2z, v3z);
			computedV = true;
		}
	}

	public void getV(float[] V) {
		computeV();
		V[0] = this.V[0];
		V[1] = this.V[1];
		V[2] = this.V[2];
	}

	private void computeN() {
		if (!computedN && hasNormal) {
			N[0] = getTriangleWeightedValue(n1x, n2x, n3x);
			N[1] = getTriangleWeightedValue(n1y, n2y, n3y);
			N[2] = getTriangleWeightedValue(n1z, n2z, n3z);
			computedN = true;
		}
	}

	public void getN(float[] N) {
		computeN();
		N[0] = this.N[0];
		N[1] = this.N[1];
		N[2] = this.N[2];
	}

	private void computeVe() {
		if (!computedVe) {
			computeV();
			vectorMult34(Ve, modelViewMatrix, V);
			computedVe = true;
		}
	}

	public void getVe(float[] Ve) {
		computeVe();
		Ve[0] = this.Ve[0];
		Ve[1] = this.Ve[1];
		Ve[2] = this.Ve[2];
	}

	private void computeNe() {
		if (!computedNe && hasNormal) {
			computeN();
			// TODO We should use the proper gl_NormalMatrix?
			// Or is the PSP restricting the model-view matrix to an orthogonal matrix?
			// See http://www.lighthouse3d.com/tutorials/glsl-tutorial/the-normal-matrix/
			vectorMult33(Ne, modelViewMatrix, N);
			computedNe = true;
		}
	}

	public void getNe(float[] Ne) {
		computeNe();
		Ne[0] = this.Ne[0];
		Ne[1] = this.Ne[1];
		Ne[2] = this.Ne[2];
	}

	private void computeNormalizedNe() {
		if (!computedNormalizedNe && hasNormal) {
			computeNe();
			normalize3(normalizedNe, Ne);
			computedNormalizedNe = true;
		}
	}

	public void getNormalizedNe(float[] normalizedNe) {
		computeNormalizedNe();
		normalizedNe[0] = this.normalizedNe[0];
		normalizedNe[1] = this.normalizedNe[1];
		normalizedNe[2] = this.normalizedNe[2];
	}
}
