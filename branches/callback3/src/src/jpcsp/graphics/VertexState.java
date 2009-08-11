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
    float[] boneWeights = new float[8];
    float[] c = new float[4]; // R, G, B, A
    float[] p = new float[3]; // X, Y, Z
    float[] n = new float[3]; // NX, NY, NZ
    float[] t = new float[2]; // U, V
}
