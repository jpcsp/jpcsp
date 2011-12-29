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
package jpcsp.graphics;

// Based on soywiz/pspemulator
public class VertexState {
    public float[] boneWeights = new float[8];
    public float[] c = new float[4]; // R, G, B, A
    public float[] p = new float[3]; // X, Y, Z
    public float[] n = new float[3]; // NX, NY, NZ
    public float[] t = new float[2]; // U, V

    public void copy(VertexState from) {
    	if (from != this) {
	    	System.arraycopy(from.boneWeights, 0, boneWeights, 0, boneWeights.length);
	    	System.arraycopy(from.c, 0, c, 0, c.length);
	    	System.arraycopy(from.p, 0, p, 0, p.length);
	    	System.arraycopy(from.n, 0, n, 0, n.length);
	    	System.arraycopy(from.t, 0, t, 0, t.length);
    	}
    }

    public void clear() {
    	c[0] = c[1] = c[2] = c[3] = 0.f;
    	p[0] = p[1] = p[2] = 0.f;
    	n[0] = n[1] = n[2] = 0.f;
    	t[0] = t[1] = 0.f;
    }
}
