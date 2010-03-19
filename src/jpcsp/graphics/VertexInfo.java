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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.media.opengl.GL;

import com.sun.opengl.util.BufferUtil;

import jpcsp.Memory;
import jpcsp.memory.IMemoryReader;
import jpcsp.memory.MemoryReader;
import jpcsp.util.Hash;

// Based on soywiz/pspemulator
public class VertexInfo {
    // vtype
    public boolean transform2D;
    public int skinningWeightCount;
    public int morphingVertexCount;
    public int texture;
    public int color;
    public int normal;
    public int position;
    public int weight;
    public int index;
    private int param;

    // vaddr, iaddr
    public int ptr_vertex;
    public int ptr_index;

    // other data
    public int vertexSize;
    public int oneVertexSize;
    public int textureOffset;
    public int colorOffset;
    public int normalOffset;
    public int positionOffset;
    public int alignmentSize;
    
    //temp value for memory addres 
    public int tempAddr;

    private static int[] size_mapping = new int[] { 0, 1, 2, 4 };
    private static int[] size_padding = new int[] { 0, 0, 1, 3 };
    private static int[] color_size_mapping = new int[] { 0, 1, 1, 1, 2, 2, 2, 4 };
    private static int[] color_size_padding = new int[] { 0, 0, 0, 0, 1, 1, 1, 3 };
    private float[] morph_weight = new float[8];

    private static String[] texture_info = new String[] {
        null, "GU_TEXTURE_8BIT", "GU_TEXTURE_16BIT", "GU_TEXTURE_32BITF"
    };
    private static String[] color_info = new String[] {
        null, "GU_COLOR_UNK2", "GU_COLOR_UNK3", "GU_COLOR_UNK4",
        "GU_COLOR_5650", "GU_COLOR_5551", "GU_COLOR_4444", "GU_COLOR_8888"
    };
    private static String[] normal_info = new String[] {
        null, "GU_NORMAL_8BIT", "GU_NORMAL_16BIT", "GU_NORMAL_32BITF"
    };
    private static String[] vertex_info = new String[] {
        null, "GU_VERTEX_8BIT", "GU_VERTEX_16BIT", "GU_VERTEX_32BITF"
    };
    private static String[] weight_info = new String[] {
        null, "GU_WEIGHT_8BIT", "GU_WEIGHT_16BIT", "GU_WEIGHT_32BITF"
    };
    private static String[] index_info = new String[] {
        null, "GU_INDEX_8BIT",  "GU_INDEX_16BIT", "GU_INDEX_UNK3"
    };
    private static String[] transform_info = new String[] {
        "GU_TRANSFORM_3D", "GU_TRANSFORM_2D"
    };

    // cache data
	private int glId = -1;	// id created by glGenBuffers
	private boolean hashCodeComputed = false;
	private int hashCode;
	private int cachedNumberOfVertex;
	private float[] cachedMorphWeights;
	private float[][] cachedBoneMatrix;
	private ByteBuffer cachedBuffer;
	private VertexCache vertexCache;

	public VertexInfo() {
	}

	public VertexInfo(VertexInfo vertexInfo) {
		this.param               = vertexInfo.param;
		this.transform2D         = vertexInfo.transform2D;
		this.skinningWeightCount = vertexInfo.skinningWeightCount;
		this.morphingVertexCount = vertexInfo.morphingVertexCount;
		this.texture             = vertexInfo.texture;
		this.color               = vertexInfo.color;
		this.normal              = vertexInfo.normal;
		this.position            = vertexInfo.position;
		this.weight              = vertexInfo.weight;
		this.index               = vertexInfo.index;
		this.ptr_vertex          = vertexInfo.ptr_vertex;
		this.ptr_index           = vertexInfo.ptr_index;
		this.vertexSize          = vertexInfo.vertexSize;
		this.oneVertexSize       = vertexInfo.oneVertexSize;
		this.textureOffset       = vertexInfo.textureOffset;
		this.colorOffset         = vertexInfo.colorOffset;
		this.normalOffset        = vertexInfo.normalOffset;
		this.positionOffset      = vertexInfo.positionOffset;
		this.alignmentSize       = vertexInfo.alignmentSize;
		this.morph_weight        = vertexInfo.morph_weight;
		this.hashCode            = vertexInfo.hashCode;
		this.hashCodeComputed    = vertexInfo.hashCodeComputed;
		this.vertexCache         = vertexInfo.vertexCache;
	}

	public void processType(int param) {
    	this.param          = param;
        texture             = (param >>  0) & 0x3;
        color               = (param >>  2) & 0x7;
        normal              = (param >>  5) & 0x3;
        position            = (param >>  7) & 0x3;
        weight              = (param >>  9) & 0x3;
        index               = (param >> 11) & 0x3;
        skinningWeightCount = ((param >> 14) & 0x7) + 1;
        morphingVertexCount = ((param >> 18) & 0x7) + 1;
        transform2D         = ((param >> 23) & 0x1) != 0;

        vertexSize = 0;
        vertexSize += size_mapping[weight] * skinningWeightCount;
        vertexSize = (vertexSize + size_padding[texture]) & ~size_padding[texture];

        textureOffset = vertexSize;
        vertexSize += size_mapping[texture] * 2;
        vertexSize = (vertexSize + color_size_padding[color]) & ~color_size_padding[color];

        colorOffset = vertexSize;
        vertexSize += color_size_mapping[color];
        vertexSize = (vertexSize + size_padding[normal]) & ~size_padding[normal];

        normalOffset = vertexSize;
        vertexSize += size_mapping[normal] * 3;
        vertexSize = (vertexSize + size_padding[position]) & ~size_padding[position];

        positionOffset = vertexSize;
        vertexSize += size_mapping[position] * 3;
        
        oneVertexSize = vertexSize;
        vertexSize *= morphingVertexCount;

        alignmentSize = Math.max(size_mapping[weight],
                        Math.max(color_size_mapping[color],
                        Math.max(size_mapping[normal],
                        Math.max(size_mapping[texture],
                                 size_mapping[position]))));
        vertexSize = (vertexSize + alignmentSize - 1) & ~(alignmentSize - 1);
        oneVertexSize = (oneVertexSize + alignmentSize - 1) & ~(alignmentSize - 1);
    }

    public int getAddress(Memory mem, int i) {
        if (ptr_index != 0 && index != 0) {
            int addr = ptr_index + i * index;
            switch(index) {
                case 1: i = mem.read8 (addr); break; // GU_INDEX_8BIT
                case 2: i = mem.read16(addr); break; // GU_INDEX_16BIT
                case 3: i = mem.read32(addr); break; // GU_INDEX_UNK3 (assume 32bit)
            }
        }

        return ptr_vertex + i * vertexSize;
    }
    
    public void setMorphWeights(float[] mw)
    {
    	morph_weight = mw;
    	
        if(morphingVertexCount == 1)
        	morph_weight[0] = 1.f;
    }

    public VertexState lastVertex = new VertexState();
    public VertexState readVertex(Memory mem, int addr) {
        VertexState v = new VertexState();

        // testing
        if (false) {
            int u0 = mem.read8(addr);
            int u1 = mem.read8(addr + 1);
            int u2 = mem.read8(addr + 2);
            int u3 = mem.read8(addr + 3);
            int u4 = mem.read8(addr + 4);
            int u5 = mem.read8(addr + 5);
            int u6 = mem.read8(addr + 6);
            int u7 = mem.read8(addr + 7);
            int u8 = mem.read8(addr + 8);
            int u9 = mem.read8(addr + 9);
            int u10 = mem.read8(addr + 10);
            int u11 = mem.read8(addr + 11);
            int u12 = mem.read8(addr + 12);
            VideoEngine.log.debug("vertex " + String.format("%02x %02x %02x %02x %02x %02x %02x %02x %02x %02x %02x %02x ",
                    u0, u1, u2, u3, u4, u5, u6, u7, u8, u9, u10, u11, u12));
        }
        
        tempAddr = addr;

        //VideoEngine.log.debug("skinning " + String.format("0x%08x", addr));
        if (weight != 0) {
	        for (int i = 0; i < skinningWeightCount; ++i) {
	            switch (weight) {
	            case 1:
	            	// Unsigned 8 bit, mapped to [0..2]
	                v.boneWeights[i] = mem.read8(addr); addr += 1;
	                v.boneWeights[i] /= 0x80;
	                break;
	            case 2:
	            	addr = (addr + 1) & ~1;
	            	// Unsigned 16 bit, mapped to [0..2]
	                v.boneWeights[i] = mem.read16(addr); addr += 2;
	                v.boneWeights[i] /= 0x8000;
	                break;
	            case 3:
	            	addr = (addr + 3) & ~3;
	                v.boneWeights[i] = Float.intBitsToFloat(mem.read32(addr)); addr += 4;
	                break;
	            }
	        }
	        if (VideoEngine.log.isTraceEnabled()) {
	        	VideoEngine.log.trace(String.format("Weight(%d) %.1f %.1f %.1f %.1f %.1f %.1f %.1f %.1f", skinningWeightCount, v.boneWeights[0], v.boneWeights[1], v.boneWeights[2], v.boneWeights[3], v.boneWeights[4], v.boneWeights[5], v.boneWeights[6], v.boneWeights[7]));
	        }
        }

        //VideoEngine.log.debug("texture " + String.format("0x%08x", addr));
        switch(texture) {
            case 1:
            	for(int morphCounter = 0;morphCounter < morphingVertexCount;morphCounter++){
            		addr = tempAddr + (morphCounter * oneVertexSize) + textureOffset;
            		float tu,tv;
            		
	            	// Unsigned 8 bit
	        		tu = mem.read8(addr) & 0xFF; addr += 1;
	        		tv = mem.read8(addr) & 0xFF; addr += 1;
	            	if (!transform2D) {
	            		// To be mapped to [0..2] for 3D
	            		tu /= 0x80;
	            		tv /= 0x80;
	            		
	            		v.t[0] += tu * morph_weight[morphCounter];
		                v.t[1] += tv * morph_weight[morphCounter];
	            	}else{
	            		v.t[0] = tu;
		                v.t[1] = tv;
	            	}
            	}
            	if (VideoEngine.log.isTraceEnabled()) {
            		VideoEngine.log.trace("texture type 1 " + v.t[0] + ", " + v.t[1] + " transform2D=" + transform2D);
            	}
                break;
            case 2:
            	for(int morphCounter = 0;morphCounter < morphingVertexCount;morphCounter++){
            		addr = tempAddr + (morphCounter * oneVertexSize) + textureOffset;
            		float tu,tv;
	            	
            		addr = (addr + 1) & ~1;
	            	// Unsigned 16 bit
	        		tu = mem.read16(addr) & 0xFFFF; addr += 2;
	        		tv = mem.read16(addr) & 0xFFFF; addr += 2;
	            	if (!transform2D) {
	            		// To be mapped to [0..2] for 3D
	            		tu /= 0x8000;
	            		tv /= 0x8000;
	            		
	            		v.t[0] += tu * morph_weight[morphCounter];
	 	                v.t[1] += tv * morph_weight[morphCounter];
	            	}else {
	            		v.t[0] = tu;
		                v.t[1] = tv;
	            	}
            	}
            	if (VideoEngine.log.isTraceEnabled()) {
            		VideoEngine.log.trace("texture type 2 " + v.t[0] + ", " + v.t[1] + " transform2D=" + transform2D);
            	}
                break;
            case 3:
            	for(int morphCounter = 0;morphCounter < morphingVertexCount;morphCounter++){
            		addr = tempAddr + (morphCounter * oneVertexSize) + textureOffset;
            		float tu,tv;
            		
	            	addr = (addr + 3) & ~3;
	                tu = Float.intBitsToFloat(mem.read32(addr)); addr += 4;
	                tv = Float.intBitsToFloat(mem.read32(addr)); addr += 4;
	                
	                v.t[0] += tu * morph_weight[morphCounter];
	                v.t[1] += tv * morph_weight[morphCounter];
            	}
            	if (VideoEngine.log.isTraceEnabled()) {
            		VideoEngine.log.trace("texture type 3 " + v.t[0] + ", " + v.t[1] + " transform2D=" + transform2D);
            	}
                break;
        }

        //VideoEngine.log.debug("color " + String.format("0x%08x", addr));
        switch(color) {
            case 1: case 2: case 3:
                VideoEngine.log.warn("unimplemented color type " + color);
                addr += 1;
                break;

            case 4: { // GU_COLOR_5650
            	for(int morphCounter = 0;morphCounter < morphingVertexCount;morphCounter++){
            		addr = tempAddr + (morphCounter * oneVertexSize) + colorOffset;
            		float a,r,g,b; //??
            		
	            	addr = (addr + 1) & ~1;
	            	int packed = mem.read16(addr); addr += 2;
	                // All components checked on PSP
	            	//
	            	//    5650 format: BBBBBGGGGGGRRRRR
	            	//                 4321054321043210
	            	// transformed into
	            	//    8888 format: 11111111BBBBBBBBGGGGGGGGRRRRRRRR
	            	//                         432104325432105443210432
	            	//
	            	int rBits = (packed      ) & 0x1F;
	            	int gBits = (packed >>  5) & 0x3F;
	            	int bBits = (packed >> 11) & 0x1F;
	            	r = ((rBits << 3) | (rBits >> 2)) / 255.0f;
	            	g = ((gBits << 2) | (gBits >> 4)) / 255.0f;
	            	b = ((bBits << 3) | (bBits >> 2)) / 255.0f;
	                a = 1.0f;
	                
	                v.c[0] += r * morph_weight[morphCounter];
	                v.c[1] += g * morph_weight[morphCounter];
	                v.c[2] += b * morph_weight[morphCounter];
	                v.c[3] += a * morph_weight[morphCounter];
                
            	}
            	break;
            }

            case 5: { // GU_COLOR_5551
            	for(int morphCounter = 0;morphCounter < morphingVertexCount;morphCounter++){
            		addr = tempAddr + (morphCounter * oneVertexSize) + colorOffset;
            		float a,r,g,b; //??
            		
	            	addr = (addr + 1) & ~1;
	            	int packed = mem.read16(addr); addr += 2;
	                // All components checked on PSP
	            	//
	            	//    5551 format: ABBBBBGGGGGRRRRR
	            	//                  432104321043210
	            	// transformed into
	            	//    8888 format: AAAAAAAABBBBBBBBGGGGGGGGRRRRRRRR
	            	//                         432104324321043243210432
	            	//
	            	int rBits = (packed      ) & 0x1F;
	            	int gBits = (packed >>  5) & 0x1F;
	            	int bBits = (packed >> 10) & 0x1F;
	            	r = ((rBits << 3) | (rBits >> 2)) / 255.0f;
	            	g = ((gBits << 3) | (gBits >> 2)) / 255.0f;
	            	b = ((bBits << 3) | (bBits >> 2)) / 255.0f;
	                a = ((packed >> 15) & 0x01) /  1.0f;

	                v.c[0] += r * morph_weight[morphCounter];
	                v.c[1] += g * morph_weight[morphCounter];
	                v.c[2] += b * morph_weight[morphCounter];
	                v.c[3] += a * morph_weight[morphCounter];
            	}
            	break;
            }

            case 6: { // GU_COLOR_4444
            	for(int morphCounter = 0;morphCounter < morphingVertexCount;morphCounter++){
            		addr = tempAddr + (morphCounter * oneVertexSize) + colorOffset;
            		float a,r,g,b; //??
            		
	            	addr = (addr + 1) & ~1;
	                int packed = mem.read16(addr); addr += 2;
	                // All components checked on PSP
	            	//
	            	//    4444 format: AAAABBBBGGGGRRRR
	            	//                 3210321032103210
	            	// transformed into
	            	//    8888 format: AAAAAAAABBBBBBBBGGGGGGGGRRRRRRRR
	            	//                 32103210321032103210321032103210
	            	//
	            	int rBits = (packed      ) & 0x0F;
	            	int gBits = (packed >>  4) & 0x0F;
	            	int bBits = (packed >>  8) & 0x0F;
	            	int aBits = (packed >> 12) & 0x0F;
	            	r = ((rBits << 4) | rBits) / 255.0f;
	            	g = ((gBits << 4) | gBits) / 255.0f;
	            	b = ((bBits << 4) | bBits) / 255.0f;
	            	a = ((aBits << 4) | aBits) / 255.0f;

	                v.c[0] += r * morph_weight[morphCounter];
	                v.c[1] += g * morph_weight[morphCounter];
	                v.c[2] += b * morph_weight[morphCounter];
	                v.c[3] += a * morph_weight[morphCounter];
                
            	}
                if (VideoEngine.log.isTraceEnabled()) {
                	VideoEngine.log.trace("color type 6 " + String.format("r=%.1f g=%.1f b=%.1f a=%.1f ", v.c[0], v.c[1], v.c[2], v.c[3]));
                }
                break;
            }

            case 7: { // GU_COLOR_8888
                // 32-bit align here instead of on vertexSize, from actarus/sam
            	for(int morphCounter = 0;morphCounter < morphingVertexCount;morphCounter++){
            		addr = tempAddr + (morphCounter * oneVertexSize) + colorOffset;
            		float a,r,g,b; //??143695396
            		
	                addr = (addr + 3) & ~3;
	                int packed = mem.read32(addr); addr += 4;
	                r = ((packed      ) & 0xff) / 255.0f;
	                g = ((packed >>  8) & 0xff) / 255.0f;
	                b = ((packed >> 16) & 0xff) / 255.0f;
	                a = ((packed >> 24) & 0xff) / 255.0f;
	                
	                v.c[0] += r * morph_weight[morphCounter];
	                v.c[1] += g * morph_weight[morphCounter];
	                v.c[2] += b * morph_weight[morphCounter];
	                v.c[3] += a * morph_weight[morphCounter];
            	}
                if (VideoEngine.log.isTraceEnabled()) {
                	VideoEngine.log.trace("color type 7 " + String.format("r=%.1f g=%.1f b=%.1f a=%.1f", v.c[0], v.c[1], v.c[2], v.c[3]));
                }
                break;
            }
        }

        //VideoEngine.log.debug("normal " + String.format("0x%08x", addr));
        switch(normal) {
            case 1:
            	// TODO Check if this value is signed like position or unsigned like texture
            	// Signed 8 bit
            	for(int morphCounter = 0;morphCounter < morphingVertexCount;morphCounter++){
            		addr = tempAddr + (morphCounter * oneVertexSize) + normalOffset;
            		float x,y,z;
            		
	                x = (byte)mem.read8(addr); addr += 1;
	                y = (byte)mem.read8(addr); addr += 1;
	                z = (byte)mem.read8(addr); addr += 1;
	            	if (!transform2D) {
	            		// To be mapped to [-1..1] for 3D
	            		x /= 0x7f;
	            		y /= 0x7f;
	            		z /= 0x7f;
	            		
	            		v.n[0] += x * morph_weight[morphCounter];
		                v.n[1] += y * morph_weight[morphCounter];
		                v.n[2] += z * morph_weight[morphCounter];
	            	}else
	            	{
	            		v.n[0] = x;
		                v.n[1] = y;
		                v.n[2] = z;
	            	}
            	}
            	if (VideoEngine.log.isTraceEnabled()) {
            		VideoEngine.log.trace("normal type 1 " + v.n[0] + ", " + v.n[1] + ", " + v.n[2] + " transform2D=" + transform2D);
            	}
                break;
            case 2:
            	for(int morphCounter = 0;morphCounter < morphingVertexCount;morphCounter++){
            		addr = tempAddr + (morphCounter * oneVertexSize) + normalOffset;
            		float x,y,z;
            	
	            	addr = (addr + 1) & ~1;
	            	// TODO Check if this value is signed like position or unsigned like texture
	            	// Signed 16 bit
	                x = (short)mem.read16(addr); addr += 2;
	                y = (short)mem.read16(addr); addr += 2;
	                z = (short)mem.read16(addr); addr += 2;
	            	if (!transform2D) {
	            		// To be mapped to [-1..1] for 3D
	            		x /= 0x7fff;
	            		y /= 0x7fff;
	            		z /= 0x7fff;
	            		
	            		v.n[0] += x * morph_weight[morphCounter];
		                v.n[1] += y * morph_weight[morphCounter];
		                v.n[2] += z * morph_weight[morphCounter];
	            	}else
	            	{
	            		v.n[0] = x;
		                v.n[1] = y;
		                v.n[2] = z;
	            	}
            	}
            	if (VideoEngine.log.isTraceEnabled()) {
            		VideoEngine.log.trace("normal type 2 " + v.n[0] + ", " + v.n[1] + ", " + v.n[2] + " transform2D=" + transform2D);
            	}
                break;
            case 3:
            	for(int morphCounter = 0;morphCounter < morphingVertexCount;morphCounter++){
            		addr = tempAddr + (morphCounter * oneVertexSize) + normalOffset;
            		float x,y,z;
            		
	            	addr = (addr + 3) & ~3;
	                x = Float.intBitsToFloat(mem.read32(addr)); addr += 4;
	                y = Float.intBitsToFloat(mem.read32(addr)); addr += 4;
	                z = Float.intBitsToFloat(mem.read32(addr)); addr += 4;
	                
	                v.n[0] += x * morph_weight[morphCounter];
	                v.n[1] += y * morph_weight[morphCounter];
	                v.n[2] += z * morph_weight[morphCounter];
            	}
                break;
        }

        //VideoEngine.log.debug("position " + String.format("0x%08x", addr));
        switch (position) {
            case 1:
            	for(int morphCounter = 0;morphCounter < morphingVertexCount;morphCounter++){
            		addr = tempAddr + (morphCounter * oneVertexSize) + positionOffset;
            		float x,y,z;
            		
	            	if (transform2D) {
	            		// X and Y are signed 8 bit, Z is unsigned 8 bit
	            		v.p[0] = (byte) mem.read8(addr); addr += 1;
	            		v.p[1] = (byte) mem.read8(addr); addr += 1;
	            		v.p[2] =        mem.read8(addr); addr += 1;
	                    
	            	} else {
		            	// Signed 8 bit, to be mapped to [-1..1] for 3D
		                x = ((byte)mem.read8(addr)) / 127f; addr += 1;
		                y = ((byte)mem.read8(addr)) / 127f; addr += 1;
		                z = ((byte)mem.read8(addr)) / 127f; addr += 1;
		                
		                v.p[0] += x * morph_weight[morphCounter];
	                    v.p[1] += y * morph_weight[morphCounter];
	                    v.p[2] += z * morph_weight[morphCounter];
	            	}
            	}
            	if (VideoEngine.log.isTraceEnabled()) {
            		VideoEngine.log.trace("vertex type 1 " + v.p[0] + ", " + v.p[1] + ", " + v.p[2] + " transform2D=" + transform2D);
            	}
                break;
            case 2:
            	for(int morphCounter = 0;morphCounter < morphingVertexCount;morphCounter++){
            		addr = tempAddr + (morphCounter * oneVertexSize) + positionOffset;
            		float x,y,z;
	            	addr = (addr + 1) & ~1;
	            	if (transform2D) {
	            		// X and Y are signed 16 bit, Z is unsigned 16 bit
	            		v.p[0] = (short)mem.read16(addr); addr += 2;
	            		v.p[1] = (short)mem.read16(addr); addr += 2;
	            		v.p[2] =        mem.read16(addr); addr += 2;

	            	} else {
		            	// Signed 16 bit, to be mapped to [-1..1] for 3D
		        		x = ((short)mem.read16(addr)) / 32767f; addr += 2;
		        		y = ((short)mem.read16(addr)) / 32767f; addr += 2;
		        		z = ((short)mem.read16(addr)) / 32767f; addr += 2;
		        		
		        		v.p[0] += x * morph_weight[morphCounter];
	                    v.p[1] += y * morph_weight[morphCounter];
	                    v.p[2] += z * morph_weight[morphCounter];
	            	}
            	}
            	if (VideoEngine.log.isTraceEnabled()) {
            		VideoEngine.log.trace("vertex type 2 " + v.p[0] + ", " + v.p[1] + ", " + v.p[2] + " transform2D=" + transform2D + ", addr=0x" + Integer.toHexString(addr - 6));
            	}
                break;
            case 3: // GU_VERTEX_32BITF
            	for(int morphCounter = 0;morphCounter < morphingVertexCount;morphCounter++){
            		addr = tempAddr + (morphCounter * oneVertexSize) + positionOffset;
            		float x,y,z;
            		
	            	addr = (addr + 3) & ~3;
	                x = Float.intBitsToFloat(mem.read32(addr)); addr += 4;
	                y = Float.intBitsToFloat(mem.read32(addr)); addr += 4;
	                z = Float.intBitsToFloat(mem.read32(addr)); addr += 4;
	                
	                if (transform2D) {
	                	// Negative Z are interpreted as 0
	                	if (z < 0) {
	                		z = 0;
	                	} else {
	                		z = (int) z;	// 2D positions are always integer values
	                	}
	                	
	                	v.p[0] = x ;
	                    v.p[1] = y ;
	                    v.p[2] = z ;
	                }else {
	                	v.p[0] += x * morph_weight[morphCounter];
	                    v.p[1] += y * morph_weight[morphCounter];
	                    v.p[2] += z * morph_weight[morphCounter];
	                }
            	}
            	if (VideoEngine.log.isTraceEnabled()) {
            		VideoEngine.log.trace("vertex type 3 " + v.p[0] + ", " + v.p[1] + ", " + v.p[2] + " transform2D=" + transform2D + ", addr=0x" + Integer.toHexString(addr - 12));
            	}
                break;
        }

        //VideoEngine.log.debug("end " + String.format("0x%08x", addr) + " size=" + vertexSize);

        lastVertex = v;
        return v;
    }

    private static int hashCode(VertexInfo vertexInfo, int numberOfVertex) {
    	int hashCode = vertexInfo.param;

    	int vertexArraySize;
    	if (vertexInfo.ptr_index != 0 && vertexInfo.index != 0) {
    		IMemoryReader memoryReader = null;
    		switch (vertexInfo.index) {
    			case 1: { // GU_INDEX_8BIT
    				memoryReader = MemoryReader.getMemoryReader(vertexInfo.ptr_index, 1 * numberOfVertex, 1);
    				break;
    			}
    			case 2: { // GU_INDEX_16BIT
    				memoryReader = MemoryReader.getMemoryReader(vertexInfo.ptr_index, 2 * numberOfVertex, 2);
    				break;
    			}
    			case 3: { // GU_INDEX_UNK3 (assume 32bit)
    				memoryReader = MemoryReader.getMemoryReader(vertexInfo.ptr_index, 4 * numberOfVertex, 4);
    				break;
    			}
    		}

    		// Compute hashCode on index array and remember the largest index
    		int maxIndex = -1;
    		if (memoryReader != null) {
				for (int i = 0; i < numberOfVertex; i++) {
					int index = memoryReader.readNext();
					if (index > maxIndex) {
						maxIndex = index;
					}
					hashCode ^= index + i;
					hashCode += i;
				}
    		}

			// The vertex array extends only up to the largest index
			vertexArraySize = vertexInfo.vertexSize * (maxIndex + 1);
    	} else {
    		vertexArraySize = vertexInfo.vertexSize * numberOfVertex;
    	}

    	if (vertexInfo.ptr_vertex != 0) {
    		hashCode = Hash.getHashCodeComplex(hashCode, vertexInfo.ptr_vertex, vertexArraySize);
    	}

    	return hashCode;
    }

    public void setDirty() {
    	hashCodeComputed = false;
    }

    public int hashCode(int numberOfVertex) {
    	if (!hashCodeComputed) {
    		hashCode = hashCode(this, numberOfVertex);
    		hashCodeComputed = true;
    	}

    	return hashCode;
    }

    public boolean equals(VertexInfo vertexInfo, int numberOfVertex, float[][] boneMatrix, int numberOfWeightsForShader) {
		if (param != vertexInfo.param ||
		    this.cachedNumberOfVertex != numberOfVertex ||
		    ptr_index != vertexInfo.ptr_index) {
			return false;
		}

		for (int i = 0; i < morphingVertexCount; i++) {
			if (cachedMorphWeights[i] != vertexInfo.morph_weight[i]) {
				return false;
			}
		}

		// Check if the bone matrix has changed, only if not using Skinning Shaders
		if (weight != 0 && numberOfWeightsForShader == 0) {
			for (int i = 0; i < skinningWeightCount; i++) {
				for (int j = 0; j < 12; j++) {
					if (cachedBoneMatrix[i][j] != boneMatrix[i][j]) {
						return false;
					}
				}
			}
		}

		// Do not compute the hashCode of the new vertex if it has already
		// been checked during this display cycle
		if (!vertexCache.vertexAlreadyHashed(vertexInfo)) {
			int hashCode = vertexInfo.hashCode(numberOfVertex);
			if (hashCode != hashCode(numberOfVertex)) {
				return false;
			}
			vertexCache.setVertexAlreadyHashed(vertexInfo);
		}

		return true;
	}

	public void bindVertex(GL gl) {
		VideoEngine.getInstance().glBindBuffer(glId);
	}

	public void loadVertex(GL gl, FloatBuffer buffer, int size) {
		if (glId == -1) {
			int[] glIds = new int[1];
			VideoEngine.getInstance().glGenBuffers(gl, glIds.length, glIds, 0);
            glId = glIds[0];
		}

		bindVertex(gl);

		cachedBuffer = ByteBuffer.allocateDirect(size * BufferUtil.SIZEOF_FLOAT).order(ByteOrder.LITTLE_ENDIAN);
		int oldLimit = buffer.limit();
		buffer.limit(size);
		cachedBuffer.asFloatBuffer().put(buffer);
		buffer.limit(oldLimit);
		buffer.rewind();
		cachedBuffer.rewind();

		VideoEngine.getInstance().glBufferData(gl, size * BufferUtil.SIZEOF_FLOAT, cachedBuffer, GL.GL_STATIC_DRAW);
	}

	public void deleteVertex(GL gl) {
		if (glId != -1) {
			int[] glIds = new int[1];
			glIds[0] = glId;
			VideoEngine.getInstance().glDeleteBuffers(gl, glIds.length, glIds, 0);
            glId = -1;
		}
		cachedMorphWeights = null;
		cachedBoneMatrix = null;
		cachedBuffer = null;
    }

	public void prepareForCache(VertexCache vertexCache, int numberOfVertex, float[][] boneMatrix, int numberOfWeightsForShader) {
		this.vertexCache = vertexCache;
		cachedNumberOfVertex = numberOfVertex;

		cachedMorphWeights = new float[morphingVertexCount];
		for (int i = 0; i < morphingVertexCount; i++) {
			cachedMorphWeights[i] = morph_weight[i];
		}

		if (weight != 0 && numberOfWeightsForShader == 0) {
			cachedBoneMatrix = new float[skinningWeightCount][];
			for (int i = 0; i < skinningWeightCount; i++) {
				cachedBoneMatrix[i] = new float[12];
				System.arraycopy(boneMatrix[i], 0, cachedBoneMatrix[i], 0, 12);
			}
		} else {
			cachedBoneMatrix = null;
		}

		// Force hashCode computation
		hashCode(numberOfVertex);
	}

	@Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        if (texture_info[texture] != null)
            sb.append(texture_info[texture] + "|");
        if (color_info[color] != null)
            sb.append(color_info[color] + "|");
        if (normal_info[normal] != null)
            sb.append(normal_info[normal] + "|");
        if (vertex_info[position] != null)
            sb.append(vertex_info[position] + "|");
        if (weight_info[weight] != null) {
            sb.append(weight_info[weight] + "|");
            sb.append("GU_WEIGHTS(" + skinningWeightCount + ")|");
        }
        if (index_info[index] != null)
            sb.append(index_info[index] + "|");
        if (transform_info[transform2D ? 1 : 0] != null)
            sb.append(transform_info[transform2D ? 1 : 0]);

        sb.append(" size=" + vertexSize);
        return sb.toString();
    }
}
