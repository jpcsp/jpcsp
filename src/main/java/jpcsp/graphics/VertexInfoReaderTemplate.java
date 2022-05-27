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

import static jpcsp.util.Utilities.round;
import jpcsp.Memory;
import jpcsp.graphics.RE.software.PixelColor;

/**
 * @author gid15
 *
 */
public class VertexInfoReaderTemplate {
	public static boolean isLogTraceEnabled;
	public static boolean transform2D;
    public static int skinningWeightCount;
    public static int morphingVertexCount;
    public static int texture;
    public static int color;
    public static int normal;
    public static int position;
    public static int weight;
    public static int index;
    public static int vtype;
    public static boolean readTexture;
    public static int vertexSize;
    public static int oneVertexSize;
    public static int textureOffset;
    public static int colorOffset;
    public static int normalOffset;
    public static int positionOffset;
    public static int alignmentSize;

    public void processType(VertexInfo vinfo) {
    	vinfo.transform2D = transform2D;
    	vinfo.skinningWeightCount = skinningWeightCount;
    	vinfo.morphingVertexCount = morphingVertexCount;
    	vinfo.texture = texture;
    	vinfo.color = color;
    	vinfo.normal = normal;
    	vinfo.position = position;
    	vinfo.weight = weight;
    	vinfo.index = index;
    	vinfo.vtype = vtype;
    	vinfo.readTexture = readTexture;
    	vinfo.vertexSize = vertexSize;
    	vinfo.oneVertexSize = oneVertexSize;
    	vinfo.textureOffset = textureOffset;
    	vinfo.colorOffset = colorOffset;
    	vinfo.normalOffset = normalOffset;
    	vinfo.positionOffset = positionOffset;
    	vinfo.alignmentSize = alignmentSize;
    }

    public void readVertex(Memory mem, int addr, VertexState v, float[] morphWeights) {
    	doReadVertex(mem, addr, v, morphWeights);
    }

    private static void doReadVertex(Memory mem, int addr, VertexState v, float[] morphWeights) {
        int startAddr = addr;

		//
		// Weight
		//
        if (weight != 0) {
        	// Align first weight
        	if (weight == 2) {
                addr = (addr + 1) & ~1;
        	} else if (weight == 3) {
                addr = (addr + 3) & ~3;
        	}

        	for (int i = 0; i < skinningWeightCount; i++) {
                switch (weight) {
                    case 1:
                        // Unsigned 8 bit, mapped to [0..2]
                        v.boneWeights[i] = mem.read8(addr++);
                        v.boneWeights[i] /= 0x80;
                        break;
                    case 2:
                        // Unsigned 16 bit, mapped to [0..2]
                        v.boneWeights[i] = mem.read16(addr);
                        v.boneWeights[i] /= 0x8000;
                        addr += 2;
                        break;
                    case 3:
                        v.boneWeights[i] = Float.intBitsToFloat(mem.read32(addr));
                        addr += 4;
                        break;
                }
            }
        }

        if (morphingVertexCount > 1 && !transform2D) {
        	//
        	// Read vertex with morphing
        	//
        	if (readTexture) {
        		//
        		// Texture with morphing
        		//
	        	v.t[0] = v.t[1] = 0f;
    	        switch (texture) {
    	            case 1:
    	                for (int morphCounter = 0; morphCounter < morphingVertexCount; morphCounter++) {
    	                    addr = startAddr + (morphCounter * oneVertexSize) + textureOffset;

    	                    // Unsigned 8 bit
    	                    float tu = mem.read8(addr);
    	                    float tv = mem.read8(addr + 1);
	                        // To be mapped to [0..2] for 3D
	                        tu /= 0x80;
	                        tv /= 0x80;

	                        v.t[0] += tu * morphWeights[morphCounter];
	                        v.t[1] += tv * morphWeights[morphCounter];
    	                }
    	                break;
    	            case 2:
    	                for (int morphCounter = 0; morphCounter < morphingVertexCount; morphCounter++) {
    	                    addr = startAddr + (morphCounter * oneVertexSize) + textureOffset;

    	                    // Unsigned 16 bit
    	                    float tu = mem.read16(addr);
    	                    float tv = mem.read16(addr + 2);
	                        // To be mapped to [0..2] for 3D
	                        tu /= 0x8000;
	                        tv /= 0x8000;

	                        v.t[0] += tu * morphWeights[morphCounter];
	                        v.t[1] += tv * morphWeights[morphCounter];
    	                }
    	                break;
    	            case 3:
    	                for (int morphCounter = 0; morphCounter < morphingVertexCount; morphCounter++) {
    	                    addr = startAddr + (morphCounter * oneVertexSize) + textureOffset;

    	                    float tu = Float.intBitsToFloat(mem.read32(addr));
    	                    float tv = Float.intBitsToFloat(mem.read32(addr + 4));

    	                    v.t[0] += tu * morphWeights[morphCounter];
    	                    v.t[1] += tv * morphWeights[morphCounter];
    	                }
    	                break;
    	        }
            }

            if (color != 0) {
        		//
        		// Color with morphing
        		//
	        	v.c[0] = v.c[1] = v.c[2] = v.c[3] = 0f;

	        	switch (color) {
		            case 1:
		            case 2:
		            case 3:
		                VideoEngine.log.error(String.format("Unknown color type %d", color));
		                break;

		            case 4: { // GU_COLOR_5650
		                for (int morphCounter = 0; morphCounter < morphingVertexCount; morphCounter++) {
		                    addr = startAddr + (morphCounter * oneVertexSize) + colorOffset;

		                    int packed = mem.read16(addr);
		                    // All components checked on PSP
		                    //
		                    //    5650 format: BBBBBGGGGGGRRRRR
		                    //                 4321054321043210
		                    // transformed into
		                    //    8888 format: 11111111BBBBBBBBGGGGGGGGRRRRRRRR
		                    //                         432104325432105443210432
		                    //
		                    int rBits = (packed) & 0x1F;
		                    int gBits = (packed >> 5) & 0x3F;
		                    int bBits = (packed >> 11) & 0x1F;
		                    float r = ((rBits << 3) | (rBits >> 2)) / 255.0f;
		                    float g = ((gBits << 2) | (gBits >> 4)) / 255.0f;
		                    float b = ((bBits << 3) | (bBits >> 2)) / 255.0f;

		                    v.c[0] += r * morphWeights[morphCounter];
		                    v.c[1] += g * morphWeights[morphCounter];
		                    v.c[2] += b * morphWeights[morphCounter];
		                    v.c[3] += morphWeights[morphCounter];
		                }
		                break;
		            }

		            case 5: { // GU_COLOR_5551
		                for (int morphCounter = 0; morphCounter < morphingVertexCount; morphCounter++) {
		                    addr = startAddr + (morphCounter * oneVertexSize) + colorOffset;

		                    int packed = mem.read16(addr);
		                    // All components checked on PSP
		                    //
		                    //    5551 format: ABBBBBGGGGGRRRRR
		                    //                  432104321043210
		                    // transformed into
		                    //    8888 format: AAAAAAAABBBBBBBBGGGGGGGGRRRRRRRR
		                    //                         432104324321043243210432
		                    //
		                    int rBits = (packed) & 0x1F;
		                    int gBits = (packed >> 5) & 0x1F;
		                    int bBits = (packed >> 10) & 0x1F;
		                    float r = ((rBits << 3) | (rBits >> 2)) / 255.0f;
		                    float g = ((gBits << 3) | (gBits >> 2)) / 255.0f;
		                    float b = ((bBits << 3) | (bBits >> 2)) / 255.0f;
		                    float a = ((packed >> 15) & 0x01) / 1.0f;

		                    v.c[0] += r * morphWeights[morphCounter];
		                    v.c[1] += g * morphWeights[morphCounter];
		                    v.c[2] += b * morphWeights[morphCounter];
		                    v.c[3] += a * morphWeights[morphCounter];
		                }
		                break;
		            }

		            case 6: { // GU_COLOR_4444
		                for (int morphCounter = 0; morphCounter < morphingVertexCount; morphCounter++) {
		                    addr = startAddr + (morphCounter * oneVertexSize) + colorOffset;

		                    int packed = mem.read16(addr);
		                    // All components checked on PSP
		                    //
		                    //    4444 format: AAAABBBBGGGGRRRR
		                    //                 3210321032103210
		                    // transformed into
		                    //    8888 format: AAAAAAAABBBBBBBBGGGGGGGGRRRRRRRR
		                    //                 32103210321032103210321032103210
		                    //
		                    int rBits = (packed) & 0x0F;
		                    int gBits = (packed >> 4) & 0x0F;
		                    int bBits = (packed >> 8) & 0x0F;
		                    int aBits = (packed >> 12) & 0x0F;
		                    float r = ((rBits << 4) | rBits) / 255.0f;
		                    float g = ((gBits << 4) | gBits) / 255.0f;
		                    float b = ((bBits << 4) | bBits) / 255.0f;
		                    float a = ((aBits << 4) | aBits) / 255.0f;

		                    v.c[0] += r * morphWeights[morphCounter];
		                    v.c[1] += g * morphWeights[morphCounter];
		                    v.c[2] += b * morphWeights[morphCounter];
		                    v.c[3] += a * morphWeights[morphCounter];
		                }
		                break;
		            }

		            case 7: { // GU_COLOR_8888
		                for (int morphCounter = 0; morphCounter < morphingVertexCount; morphCounter++) {
		                    addr = startAddr + (morphCounter * oneVertexSize) + colorOffset;

		                    int packed = mem.read32(addr);
		                    float r = ((packed) & 0xff) / 255.0f;
		                    float g = ((packed >> 8) & 0xff) / 255.0f;
		                    float b = ((packed >> 16) & 0xff) / 255.0f;
		                    float a = ((packed >> 24) & 0xff) / 255.0f;

		                    v.c[0] += r * morphWeights[morphCounter];
		                    v.c[1] += g * morphWeights[morphCounter];
		                    v.c[2] += b * morphWeights[morphCounter];
		                    v.c[3] += a * morphWeights[morphCounter];
		                }
		                break;
		            }
		        }
	        }

	        if (normal != 0) {
        		//
        		// Normal with morphing
        		//
	        	v.n[0] = v.n[1] = v.n[2] = 0f;

	        	switch (normal) {
		            case 1:
		                // TODO Check if this value is signed like position or unsigned like texture
		                // Signed 8 bit
		                for (int morphCounter = 0; morphCounter < morphingVertexCount; morphCounter++) {
		                    addr = startAddr + (morphCounter * oneVertexSize) + normalOffset;

		                    float x = (byte) mem.read8(addr);
		                    float y = (byte) mem.read8(addr + 1);
		                    float z = (byte) mem.read8(addr + 2);
	                        // To be mapped to [-1..1] for 3D
	                        x /= 0x7f;
	                        y /= 0x7f;
	                        z /= 0x7f;

	                        v.n[0] += x * morphWeights[morphCounter];
	                        v.n[1] += y * morphWeights[morphCounter];
	                        v.n[2] += z * morphWeights[morphCounter];
		                }
		                break;
		            case 2:
		                for (int morphCounter = 0; morphCounter < morphingVertexCount; morphCounter++) {
		                    addr = startAddr + (morphCounter * oneVertexSize) + normalOffset;

		                    // TODO Check if this value is signed like position or unsigned like texture
		                    // Signed 16 bit
		                    float x = (short) mem.read16(addr);
		                    float y = (short) mem.read16(addr + 2);
		                    float z = (short) mem.read16(addr + 4);
	                        // To be mapped to [-1..1] for 3D
	                        x /= 0x7fff;
	                        y /= 0x7fff;
	                        z /= 0x7fff;

	                        v.n[0] += x * morphWeights[morphCounter];
	                        v.n[1] += y * morphWeights[morphCounter];
	                        v.n[2] += z * morphWeights[morphCounter];
		                }
		                break;
		            case 3:
		                for (int morphCounter = 0; morphCounter < morphingVertexCount; morphCounter++) {
		                    addr = startAddr + (morphCounter * oneVertexSize) + normalOffset;

		                    float x = Float.intBitsToFloat(mem.read32(addr));
		                    float y = Float.intBitsToFloat(mem.read32(addr + 4));
		                    float z = Float.intBitsToFloat(mem.read32(addr + 8));

		                    v.n[0] += x * morphWeights[morphCounter];
		                    v.n[1] += y * morphWeights[morphCounter];
		                    v.n[2] += z * morphWeights[morphCounter];
		                }
		                break;
		        }
	        }

	        if (position != 0) {
        		//
        		// Position with morphing
        		//
	        	v.p[0] = v.p[1] = v.p[2] = 0f;

	        	switch (position) {
		            case 1:
		                for (int morphCounter = 0; morphCounter < morphingVertexCount; morphCounter++) {
		                    addr = startAddr + (morphCounter * oneVertexSize) + positionOffset;

	                        // Signed 8 bit, to be mapped to [-1..1] for 3D
	                        float x = ((byte) mem.read8(addr)) / 127f;
	                        float y = ((byte) mem.read8(addr + 1)) / 127f;
	                        float z = ((byte) mem.read8(addr + 2)) / 127f;

	                        v.p[0] += x * morphWeights[morphCounter];
	                        v.p[1] += y * morphWeights[morphCounter];
	                        v.p[2] += z * morphWeights[morphCounter];
		                }
		                break;
		            case 2:
		                for (int morphCounter = 0; morphCounter < morphingVertexCount; morphCounter++) {
		                    addr = startAddr + (morphCounter * oneVertexSize) + positionOffset;

	                        // Signed 16 bit, to be mapped to [-1..1] for 3D
	                        float x = ((short) mem.read16(addr)) / 32767f;
	                        float y = ((short) mem.read16(addr + 2)) / 32767f;
	                        float z = ((short) mem.read16(addr + 4)) / 32767f;

	                        v.p[0] += x * morphWeights[morphCounter];
	                        v.p[1] += y * morphWeights[morphCounter];
	                        v.p[2] += z * morphWeights[morphCounter];
		                }
		                break;
		            case 3: // GU_VERTEX_32BITF
		                for (int morphCounter = 0; morphCounter < morphingVertexCount; morphCounter++) {
		                    addr = startAddr + (morphCounter * oneVertexSize) + positionOffset;

		                    float x = Float.intBitsToFloat(mem.read32(addr));
		                    float y = Float.intBitsToFloat(mem.read32(addr + 4));
		                    float z = Float.intBitsToFloat(mem.read32(addr + 8));

	                        v.p[0] += x * morphWeights[morphCounter];
	                        v.p[1] += y * morphWeights[morphCounter];
	                        v.p[2] += z * morphWeights[morphCounter];
		                }
		                break;
	        	}
	        }
        } else {
        	//
        	// Read Vertex without morphing
        	//
	        if (readTexture) {
        		//
        		// Texture
        		//
		        switch (texture) {
		            case 1: {
	                    // Unsigned 8 bit
	                    float tu = mem.read8(addr++);
	                    float tv = mem.read8(addr++);
	                    if (!transform2D) {
	                        // To be mapped to [0..2] for 3D
	                        tu /= 0x80;
	                        tv /= 0x80;
	                    }
                        v.t[0] = tu;
                        v.t[1] = tv;
		                break;
		            }
		            case 2: {
                        addr = (addr + 1) & ~1;
	                    // Unsigned 16 bit
	                    float tu = mem.read16(addr);
	                    float tv = mem.read16(addr + 2);
	                    addr += 4;
	                    if (!transform2D) {
	                        // To be mapped to [0..2] for 3D
	                        tu /= 0x8000;
	                        tv /= 0x8000;
	                    }
                        v.t[0] = tu;
                        v.t[1] = tv;
                        break;
                    }
		            case 3: {
                        addr = (addr + 3) & ~3;
                        v.t[0] = Float.intBitsToFloat(mem.read32(addr));
	                    v.t[1] = Float.intBitsToFloat(mem.read32(addr + 4));
	                    addr += 8;
	                    break;
	                }
		        }
	        } else if (texture != 0) {
	        	// Skip unread texture
	        	addr += colorOffset - textureOffset;
	        }

	        if (color != 0) {
        		//
        		// Color
        		//
		        switch (color) {
		            case 1:
		            case 2:
		            case 3:
		                VideoEngine.log.error(String.format("Unknown color type %d", color));
		                break;

		            case 4: { // GU_COLOR_5650
		                addr = (addr + 1) & ~1;
	                    int packed = mem.read16(addr);
	                    addr += 2;
	                    // All components checked on PSP
	                    //
	                    //    5650 format: BBBBBGGGGGGRRRRR
	                    //                 4321054321043210
	                    // transformed into
	                    //    8888 format: 11111111BBBBBBBBGGGGGGGGRRRRRRRR
	                    //                         432104325432105443210432
	                    //
	                    int rBits = packed & 0x1F;
	                    int gBits = (packed >> 5) & 0x3F;
	                    int bBits = (packed >> 11) & 0x1F;
	                    v.c[0] = ((rBits << 3) | (rBits >> 2)) / 255f;
	                    v.c[1] = ((gBits << 2) | (gBits >> 4)) / 255f;
	                    v.c[2] = ((bBits << 3) | (bBits >> 2)) / 255f;
	                    v.c[3] = 1f;
		                break;
	                }

		            case 5: { // GU_COLOR_5551
		                addr = (addr + 1) & ~1;
	                    int packed = mem.read16(addr);
	                    addr += 2;
	                    // All components checked on PSP
	                    //
	                    //    5551 format: ABBBBBGGGGGRRRRR
	                    //                  432104321043210
	                    // transformed into
	                    //    8888 format: AAAAAAAABBBBBBBBGGGGGGGGRRRRRRRR
	                    //                         432104324321043243210432
	                    //
	                    int rBits = (packed) & 0x1F;
	                    int gBits = (packed >> 5) & 0x1F;
	                    int bBits = (packed >> 10) & 0x1F;
	                    v.c[0] = ((rBits << 3) | (rBits >> 2)) / 255f;
	                    v.c[1] = ((gBits << 3) | (gBits >> 2)) / 255f;
	                    v.c[2] = ((bBits << 3) | (bBits >> 2)) / 255f;
	                    v.c[3] = ((packed >> 15) & 0x01) / 1f;
	                    break;
		            }

		            case 6: { // GU_COLOR_4444
		                addr = (addr + 1) & ~1;
		                int packed = mem.read16(addr);
		                addr += 2;
	                    // All components checked on PSP
	                    //
	                    //    4444 format: AAAABBBBGGGGRRRR
	                    //                 3210321032103210
	                    // transformed into
	                    //    8888 format: AAAAAAAABBBBBBBBGGGGGGGGRRRRRRRR
	                    //                 32103210321032103210321032103210
	                    //
	                    int rBits = (packed) & 0x0F;
	                    int gBits = (packed >> 4) & 0x0F;
	                    int bBits = (packed >> 8) & 0x0F;
	                    int aBits = (packed >> 12) & 0x0F;
	                    v.c[0] = ((rBits << 4) | rBits) / 255f;
	                    v.c[1] = ((gBits << 4) | gBits) / 255f;
	                    v.c[2] = ((bBits << 4) | bBits) / 255f;
	                    v.c[3] = ((aBits << 4) | aBits) / 255f;
	                    break;
	                }

		            case 7: { // GU_COLOR_8888
                        addr = (addr + 3) & ~3;
                        int packed = mem.read32(addr);
                        addr += 4;
                        v.c[0] = ((packed) & 0xff) / 255f;
                        v.c[1] = ((packed >> 8) & 0xff) / 255f;
                        v.c[2] = ((packed >> 16) & 0xff) / 255f;
                        v.c[3] = ((packed >> 24) & 0xff) / 255f;
                        break;
		            }
		        }
            }

	        if (normal != 0) {
	    		//
	    		// Normal
	    		//
		        switch (normal) {
		            case 1: {
	                    float x = (byte) mem.read8(addr++);
	                    float y = (byte) mem.read8(addr++);
	                    float z = (byte) mem.read8(addr++);
	                    if (!transform2D) {
	                        // To be mapped to [-1..1] for 3D
	                        x /= 0x7f;
	                        y /= 0x7f;
	                        z /= 0x7f;
	                    }
	                    v.n[0] = x;
	                    v.n[1] = y;
	                    v.n[2] = z;
	                    break;
	                }
		            case 2: {
		                addr = (addr + 1) & ~1;
	                    // TODO Check if this value is signed like position or unsigned like texture
	                    // Signed 16 bit
	                    float x = (short) mem.read16(addr);
	                    float y = (short) mem.read16(addr + 2);
	                    float z = (short) mem.read16(addr + 4);
	                    addr += 6;
	                    if (!transform2D) {
	                        // To be mapped to [-1..1] for 3D
	                        x /= 0x7fff;
	                        y /= 0x7fff;
	                        z /= 0x7fff;
	                    }
	                    v.n[0] = x;
	                    v.n[1] = y;
	                    v.n[2] = z;
	                    break;
		            }
		            case 3: {
	                    addr = (addr + 3) & ~3;
	                    v.n[0] = Float.intBitsToFloat(mem.read32(addr));
	                    v.n[1] = Float.intBitsToFloat(mem.read32(addr + 4));
	                    v.n[2] = Float.intBitsToFloat(mem.read32(addr + 8));
	                    addr += 12;
	                    break;
		            }
		        }
	        }

	        if (position != 0) {
	    		//
	    		// Position
	    		//
		        switch (position) {
		            case 1: {
	                    if (transform2D) {
	                        // X and Y are signed 8 bit, Z is unsigned 8 bit
	                        v.p[0] = (byte) mem.read8(addr++);
	                        v.p[1] = (byte) mem.read8(addr++);
	                        v.p[2] = mem.read8(addr++);
	                    } else {
	                        // Signed 8 bit, to be mapped to [-1..1] for 3D
	                    	v.p[0] = ((byte) mem.read8(addr++)) / 127f;
	                    	v.p[1] = ((byte) mem.read8(addr++)) / 127f;
	                    	v.p[2] = ((byte) mem.read8(addr++)) / 127f;
	                    }
	                    break;
	                }
		            case 2: {
		                addr = (addr + 1) & ~1;
	                    if (transform2D) {
	                        // X and Y are signed 16 bit, Z is unsigned 16 bit
	                        v.p[0] = (short) mem.read16(addr);
	                        v.p[1] = (short) mem.read16(addr + 2);
	                        v.p[2] = mem.read16(addr + 4);
	                    } else {
	                        // Signed 16 bit, to be mapped to [-1..1] for 3D
	                    	v.p[0] = ((short) mem.read16(addr)) / 32767f;
	                    	v.p[1] = ((short) mem.read16(addr + 2)) / 32767f;
	                    	v.p[2] = ((short) mem.read16(addr + 4)) / 32767f;
	                    }
	                    addr += 6;
	                    break;
	                }
		            case 3: { // GU_VERTEX_32BITF
	                    addr = (addr + 3) & ~3;
	                    float x = Float.intBitsToFloat(mem.read32(addr));
	                    float y = Float.intBitsToFloat(mem.read32(addr + 4));
	                    float z = Float.intBitsToFloat(mem.read32(addr + 8));
	                    addr += 12;
	                    if (transform2D) {
	        				// Z is an integer value clamped between 0 and 65535
	                        if (z < 0f) {
	                            z = 0f;
	                        } else if (z > 65535f) {
	                        	z = 65535f;
	                        } else {
	                        	// 2D positions are always integer values
	                            z = (int) z;
	                        }
	                    }
	                    v.p[0] = x;
	                    v.p[1] = y;
	                    v.p[2] = z;
	                    break;
	                }
		        }
	        }
        }

		//
		// Tracing
		//
        if (isLogTraceEnabled) {
        	VideoEngine.log.trace(String.format("Reading vertex at 0x%08X %s", startAddr, VertexInfo.toString(texture, color, normal, position, weight, skinningWeightCount, morphingVertexCount, index, transform2D, vertexSize)));
            if (weight != 0) {
                VideoEngine.log.trace(String.format("Weight(%d) %.2f %.2f %.2f %.2f %.2f %.2f %.2f %.2f", skinningWeightCount, v.boneWeights[0], v.boneWeights[1], v.boneWeights[2], v.boneWeights[3], v.boneWeights[4], v.boneWeights[5], v.boneWeights[6], v.boneWeights[7]));
            }
            if (texture != 0) {
            	if (transform2D) {
            		VideoEngine.log.trace(String.format("texture type %d %d, %d", texture, round(v.t[0]), round(v.t[1])));
            	} else {
            		VideoEngine.log.trace(String.format("texture type %d %f, %f", texture, v.t[0], v.t[1]));
            	}
            }
            if (color != 0) {
                VideoEngine.log.trace(String.format("color type %d 0x%08X", color, PixelColor.getColor(v.c)));
            }
            if (normal != 0) {
                VideoEngine.log.trace(String.format("normal type %d %f, %f, %f", normal, v.n[0], v.n[1], v.n[2]));
            }
            if (position != 0) {
        		VideoEngine.log.trace(String.format("vertex type %d %f, %f, %f", position, v.p[0], v.p[1], v.p[2]));
            }
            if (morphingVertexCount > 1) {
            	VideoEngine.log.trace(String.format("Morphing oneVertexSize=%d, textureOffset=%d, colorOffset=%d, normalOffset=%d, positionOffset=%d", oneVertexSize, textureOffset, colorOffset, normalOffset, positionOffset));
            }
        }
    }
}
