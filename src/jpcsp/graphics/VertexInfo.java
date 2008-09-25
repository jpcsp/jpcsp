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

import jpcsp.Emulator;
import jpcsp.Memory;

// Based on soywiz/pspemulator
public class VertexInfo {
    // vtype
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
        morphingVertexCount = (param >> 18) & 0x3;

        vertexSize = 0;
        vertexSize += size_mapping[weight] * skinningWeightCount;
        vertexSize += (color != 0) ? ((color == 7) ? 4 : 2) : 0;
        vertexSize += size_mapping[texture] * 2;
        vertexSize += size_mapping[position] * 3;
        vertexSize += size_mapping[normal] * 3;
    }

    public int getAddress(int i) {
        if (ptr_index != 0) {
            int addr = ptr_index + i * index;
            switch(index) {
                case 1: i = Emulator.getMemory().read8(addr); break;
                case 2: i = Emulator.getMemory().read16(addr); break;
                case 4: i = Emulator.getMemory().read32(addr); break;
            }
        }

        return ptr_vertex + i * vertexSize;
    }

    public VertexState readVertex(Memory mem, int addr) {
        VertexState v = new VertexState();

        switch(texture) {
            case 1:
                v.u = Emulator.getMemory().read8(addr); addr += 1;
                v.v = Emulator.getMemory().read8(addr); addr += 1;
                break;
            case 2:
                v.u = Emulator.getMemory().read16(addr); addr += 2;
                v.v = Emulator.getMemory().read16(addr); addr += 2;
                break;
            case 3:
                v.u = Float.intBitsToFloat(mem.read32(addr)); addr += 4;
                v.v = Float.intBitsToFloat(mem.read32(addr)); addr += 4;
                break;
        }

        switch(color) {
            case 1: case 2: case 3: VideoEngine.log.debug("unimplemented color type"); addr += 1; break;
            case 4: case 5: case 6: VideoEngine.log.debug("unimplemented color type"); addr += 2; break;
            case 7: { // GU_COLOR_8888
                int packed = mem.read32(addr); addr += 4;
                v.r = (float)((packed      ) & 0xff) / 255;
                v.g = (float)((packed >>  8) & 0xff) / 255;
                v.b = (float)((packed >> 16) & 0xff) / 255;
                v.a = (float)((packed >> 24) & 0xff) / 255;
                break;
            }
        }

        switch(normal) {
            case 1:
                v.nx = Emulator.getMemory().read8(addr); addr += 1;
                v.ny = Emulator.getMemory().read8(addr); addr += 1;
                v.nz = Emulator.getMemory().read8(addr); addr += 1;
                break;
            case 2:
                v.nx = Emulator.getMemory().read16(addr); addr += 2;
                v.ny = Emulator.getMemory().read16(addr); addr += 2;
                v.nz = Emulator.getMemory().read16(addr); addr += 2;
                break;
            case 3:
                v.nx = Float.intBitsToFloat(mem.read32(addr)); addr += 4;
                v.ny = Float.intBitsToFloat(mem.read32(addr)); addr += 4;
                v.nz = Float.intBitsToFloat(mem.read32(addr)); addr += 4;
                break;
        }

        switch(position) {
            case 1: VideoEngine.log.debug("unimplemented vertex type 1"); addr += 1; break;
            case 2: VideoEngine.log.debug("unimplemented vertex type 2"); addr += 2; break;
            case 3: { // GU_VERTEX_32BITF
                v.px = Float.intBitsToFloat(mem.read32(addr)); addr += 4;
                v.py = Float.intBitsToFloat(mem.read32(addr)); addr += 4;
                v.pz = Float.intBitsToFloat(mem.read32(addr)); addr += 4;
                break;
            }
        }

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

        sb.append(" size=" + vertexSize);
        return sb.toString();
    }
}
