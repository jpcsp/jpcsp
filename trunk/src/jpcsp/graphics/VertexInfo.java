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

import jpcsp.Memory;

// Based on soywiz/pspemulator
public class VertexInfo {
    // vtype
    private int transform2D; // for logging purposes (got moved into VideoEngine.java)
    public int skinningWeightCount;
    public int morphingVertexCount;
    public int texture;
    public int color;
    public int normal;
    public int position;
    public int weight;
    public int index;

    // vaddr, iaddr
    public int ptr_vertex;
    public int ptr_index;

    // other data
    public int vertexSize;

    private static int[] size_mapping = new int[] { 0, 1, 2, 4 };
    private static int[] size_padding = new int[] { 0, 0, 1, 3 };

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

    public void processType(int param) {
        texture             = (param >>  0) & 0x3;
        color               = (param >>  2) & 0x7;
        normal              = (param >>  5) & 0x3;
        position            = (param >>  7) & 0x3;
        weight              = (param >>  9) & 0x3;
        index               = (param >> 11) & 0x3;
        skinningWeightCount = ((param >> 14) & 0x7) + 1;
        morphingVertexCount = ((param >> 18) & 0x7) + 1;
        transform2D         = (param >> 23) & 0x1;

        vertexSize = 0;
        vertexSize += size_mapping[weight] * skinningWeightCount;
        vertexSize = (vertexSize + ((color != 0) ? ((color == 7) ? 3 : 1) : 0)) & ~((color != 0) ? ((color == 7) ? 3 : 1) : 0);
        vertexSize += (color != 0) ? ((color == 7) ? 4 : 2) : 0;
        vertexSize = (vertexSize + size_padding[texture]) & ~size_padding[texture];
        vertexSize += size_mapping[texture] * 2;
        vertexSize = (vertexSize + size_padding[position]) & ~size_padding[position];
        vertexSize += size_mapping[position] * 3;
        vertexSize = (vertexSize + size_padding[normal]) & ~size_padding[normal];
        vertexSize += size_mapping[normal] * 3;
        int maxsize = Math.max(size_mapping[weight],
        		Math.max((color != 0) ? ((color == 7) ? 4 : 2) : 0,
        		Math.max(size_padding[normal],
        		Math.max(size_padding[texture],
        				size_padding[position]))));

        vertexSize = (vertexSize + maxsize - 1) & ~(maxsize - 1);
    }

    public int getAddress(Memory mem, int i) {
        if (ptr_index != 0) {
            int addr = ptr_index + i * index;
            switch(index) {
                case 1: i = mem.read8(addr); break;
                case 2: i = mem.read16(addr); break;
                case 4: i = mem.read32(addr); break;
            }
        }

        return ptr_vertex + i * vertexSize;
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
            VideoEngine.log.debug("vertex "
                + String.format("%02x %02x %02x %02x %02x %02x %02x %02x %02x %02x %02x %02x ",
                    u0, u1, u2, u3, u4, u5, u6, u7, u8, u9, u10, u11, u12));
        }

        //VideoEngine.log.debug("skinning " + String.format("0x%08x", addr));
        for (int i = 0; i < skinningWeightCount; ++i) {
            switch (weight) {
            case 1:
                v.boneWeights[i] = mem.read8(addr); addr += 1;
                break;
            case 2:
            	addr = (addr + 1) & ~1;
                v.boneWeights[i] = mem.read16(addr); addr += 2;
                break;
            case 3:
            	addr = (addr + 3) & ~3;
                v.boneWeights[i] = Float.intBitsToFloat(mem.read32(addr)); addr += 4;
                break;
            }
            //System.err.println(String.format("Weight %.1f %.1f %.1f %.1f %.1f %.1f %.1f %.1f", v.boneWeights[0], v.boneWeights[1], v.boneWeights[2], v.boneWeights[3], v.boneWeights[4], v.boneWeights[5], v.boneWeights[6], v.boneWeights[7]));
        }

        //VideoEngine.log.debug("texture " + String.format("0x%08x", addr));
        switch(texture) {
            case 1:
                v.t[0] = (byte)mem.read8(addr); addr += 1;
                v.t[1] = (byte)mem.read8(addr); addr += 1;
                VideoEngine.log.trace("texture type 1 " + v.t[0] + ", " + v.t[1] + "");
                break;
            case 2:
            	addr = (addr + 1) & ~1;
                v.t[0] = (short)mem.read16(addr); addr += 2;
                v.t[1] = (short)mem.read16(addr); addr += 2;
                VideoEngine.log.trace("texture type 2 " + v.t[0] + ", " + v.t[1] + "");
                break;
            case 3:
            	addr = (addr + 3) & ~3;
                v.t[0] = Float.intBitsToFloat(mem.read32(addr)); addr += 4;
                v.t[1] = Float.intBitsToFloat(mem.read32(addr)); addr += 4;
                break;
        }

        //VideoEngine.log.debug("color " + String.format("0x%08x", addr));
        switch(color) {
            case 1: case 2: case 3: VideoEngine.log.warn("unimplemented color type " + color); addr += 1; break;
            case 4: case 5: VideoEngine.log.warn("unimplemented color type " + color);
            	addr = (addr + 1) & ~1;
            	addr += 2;
            	break;

            case 6: { // GU_COLOR_4444
            	addr = (addr + 1) & ~1;
                int packed = mem.read16(addr); addr += 2;
                v.c[0] = ((packed      ) & 0xf) / 15.0f;
                v.c[1] = ((packed >>  4) & 0xf) / 15.0f;
                v.c[2] = ((packed >>  8) & 0xf) / 15.0f;
                v.c[3] = ((packed >> 12) & 0xf) / 15.0f;
                VideoEngine.log.warn("color type " + color);
                break;
            }

            case 7: { // GU_COLOR_8888
                // 32-bit align here instead of on vertexSize, from actarus/sam
                addr = (addr + 3) & ~3;
                int packed = mem.read32(addr); addr += 4;
                v.c[0] = ((packed      ) & 0xff) / 255.0f;
                v.c[1] = ((packed >>  8) & 0xff) / 255.0f;
                v.c[2] = ((packed >> 16) & 0xff) / 255.0f;
                v.c[3] = ((packed >> 24) & 0xff) / 255.0f;
            	//VideoEngine.log.debug("color type 7 " + String.format("r=%.1f g=%.1f b=%.1f (%08X)", v.r, v.g, v.b, packed));
                break;
            }
        }

        //VideoEngine.log.debug("normal " + String.format("0x%08x", addr));
        switch(normal) {
            case 1:
                v.n[0] = (byte)mem.read8(addr); addr += 1;
                v.n[1] = (byte)mem.read8(addr); addr += 1;
                v.n[2] = (byte)mem.read8(addr); addr += 1;
                VideoEngine.log.warn("normal type 1 " + v.n[0] + ", " + v.n[1] + ", " + v.n[2] + "");
                break;
            case 2:
            	addr = (addr + 1) & ~1;
                v.n[0] = (short)mem.read16(addr); addr += 2;
                v.n[1] = (short)mem.read16(addr); addr += 2;
                v.n[2] = (short)mem.read16(addr); addr += 2;
                VideoEngine.log.warn("normal type 2 " + v.n[0] + ", " + v.n[1] + ", " + v.n[2] + "");
                break;
            case 3:
            	addr = (addr + 3) & ~3;
                v.n[0] = Float.intBitsToFloat(mem.read32(addr)); addr += 4;
                v.n[1] = Float.intBitsToFloat(mem.read32(addr)); addr += 4;
                v.n[2] = Float.intBitsToFloat(mem.read32(addr)); addr += 4;
                break;
        }

        //VideoEngine.log.debug("position " + String.format("0x%08x", addr));
        switch (position) {
            case 1:
                v.p[0] = (byte)mem.read8(addr); addr += 1;
                v.p[1] = (byte)mem.read8(addr); addr += 1;
                v.p[2] = (byte)mem.read8(addr); addr += 1;
                VideoEngine.log.trace("vertex type 1 " + v.p[0] + ", " + v.p[1] + ", " + v.p[2] + "");
                break;
            case 2:
            	addr = (addr + 1) & ~1;
                v.p[0] = (short)mem.read16(addr); addr += 2;
                v.p[1] = (short)mem.read16(addr); addr += 2;
                v.p[2] = (short)mem.read16(addr); addr += 2;
                VideoEngine.log.trace("vertex type 2 " + v.p[0] + ", " + v.p[1] + ", " + v.p[2] + "");
                break;
            case 3: // GU_VERTEX_32BITF
            	addr = (addr + 3) & ~3;
                v.p[0] = Float.intBitsToFloat(mem.read32(addr)); addr += 4;
                v.p[1] = Float.intBitsToFloat(mem.read32(addr)); addr += 4;
                v.p[2] = Float.intBitsToFloat(mem.read32(addr)); addr += 4;
                VideoEngine.log.trace("vertex type 3 " + v.p[0] + ", " + v.p[1] + ", " + v.p[2] + "");
                break;
        }

        //VideoEngine.log.debug("end " + String.format("0x%08x", addr) + " size=" + vertexSize);

        lastVertex = v;
        return v;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();

        if (texture_info[texture] != null)
            sb.append(texture_info[texture] + "|");
        if (color_info[color] != null)
            sb.append(color_info[color] + "|");
        if (normal_info[normal] != null)
            sb.append(normal_info[normal] + "|");
        if (vertex_info[position] != null)
            sb.append(vertex_info[position] + "|");
        if (weight_info[weight] != null)
            sb.append(weight_info[weight] + "|");
        if (index_info[index] != null)
            sb.append(index_info[index] + "|");
        if (transform_info[transform2D] != null)
            sb.append(transform_info[transform2D]);

        sb.append(" size=" + vertexSize);
        return sb.toString();
    }
}
