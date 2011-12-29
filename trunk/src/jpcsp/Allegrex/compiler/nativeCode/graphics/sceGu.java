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
package jpcsp.Allegrex.compiler.nativeCode.graphics;

import jpcsp.Memory;
import jpcsp.Allegrex.compiler.nativeCode.AbstractNativeCodeSequence;
import jpcsp.HLE.kernel.types.PspGeList;
import jpcsp.graphics.AsyncVertexCache;
import jpcsp.graphics.GeCommands;
import jpcsp.graphics.VideoEngine;
import jpcsp.graphics.RE.IRenderingEngine;
import jpcsp.memory.IMemoryReader;
import jpcsp.memory.IMemoryWriter;
import jpcsp.memory.MemoryReader;
import jpcsp.memory.MemoryWriter;

/**
 * @author gid15
 *
 */
public class sceGu extends AbstractNativeCodeSequence {
	private static final boolean writeTFLUSH = false;
	private static final boolean writeTSYNC = false;
	private static final boolean writeDUMMY = false;

	static private void sceGeListUpdateStallAddr(int addr) {
		// Simplification: we can update the stall address only if the VideoEngine
		// is processing one and only one GE list.
		VideoEngine videoEngine = VideoEngine.getInstance();
		if (videoEngine.numberDrawLists() == 0) {
			PspGeList geList = videoEngine.getCurrentList();
			if (geList != null && geList.getStallAddr() != 0) {
				addr &= Memory.addressMask;
				geList.setStallAddr(addr);
			}
		}
	}

	static public void sceGuDrawArray(int contextAddr1, int contextAddr2, int listCurrentOffset, int updateStall) {
		Memory mem = getMemory();
		int prim = getGprA0();
		int vtype = getGprA1();
		int count = getGprA2();
		int indices = getGprA3();
		int vertices = getGprT0();
		int context = mem.read32(getRelocatedAddress(contextAddr1, contextAddr2));
		int listCurrent = mem.read32(context + listCurrentOffset);
		int cmd;

		if (vtype != 0) {
			cmd = (GeCommands.VTYPE << 24) | (vtype & 0x00FFFFFF);
			mem.write32(listCurrent, cmd);
			listCurrent += 4;
		}

		if (indices != 0) {
			cmd = (GeCommands.BASE << 24) | ((indices >> 8) & 0x000F0000);
			mem.write32(listCurrent, cmd);
			listCurrent += 4;

			cmd = (GeCommands.IADDR << 24) | (indices & 0x00FFFFFF);
			mem.write32(listCurrent, cmd);
			listCurrent += 4;
		}

		if (vertices != 0) {
			cmd = (GeCommands.BASE << 24) | ((vertices >> 8) & 0x000F0000);
			mem.write32(listCurrent, cmd);
			listCurrent += 4;

			cmd = (GeCommands.VADDR << 24) | (vertices & 0x00FFFFFF);
			mem.write32(listCurrent, cmd);
			listCurrent += 4;
		}

		cmd = (GeCommands.PRIM << 24) | ((prim & 0x7) << 16) | count;
		mem.write32(listCurrent, cmd);
		listCurrent += 4;

		mem.write32(context + listCurrentOffset, listCurrent);

		if (updateStall != 0) {
			sceGeListUpdateStallAddr(listCurrent);
		}

		if (VideoEngine.getInstance().useAsyncVertexCache()) {
			AsyncVertexCache.getInstance().addAsyncCheck(prim, vtype, count, indices, vertices);
		}
	}

	static public void sceGuDrawArrayN(int contextAddr1, int contextAddr2, int listCurrentOffset, int updateStall) {
		Memory mem = getMemory();
		int prim = getGprA0();
		int vtype = getGprA1();
		int count = getGprA2();
		int n = getGprA3();
		int indices = getGprT0();
		int vertices = getGprT1();
		int context = mem.read32(getRelocatedAddress(contextAddr1, contextAddr2));
		int listCurrent = mem.read32(context + listCurrentOffset);
		int cmd;

		IMemoryWriter listWriter = MemoryWriter.getMemoryWriter(listCurrent, (5 + n) << 2, 4);

		if (vtype != 0) {
			cmd = (GeCommands.VTYPE << 24) | (vtype & 0x00FFFFFF);
			listWriter.writeNext(cmd);
			listCurrent += 4;
		}

		if (indices != 0) {
			cmd = (GeCommands.BASE << 24) | ((indices >> 8) & 0x000F0000);
			listWriter.writeNext(cmd);
			listCurrent += 4;

			cmd = (GeCommands.IADDR << 24) | (indices & 0x00FFFFFF);
			listWriter.writeNext(cmd);
			listCurrent += 4;
		}

		if (vertices != 0) {
			cmd = (GeCommands.BASE << 24) | ((vertices >> 8) & 0x000F0000);
			listWriter.writeNext(cmd);
			listCurrent += 4;

			cmd = (GeCommands.VADDR << 24) | (vertices & 0x00FFFFFF);
			listWriter.writeNext(cmd);
			listCurrent += 4;
		}

		if (n > 0) {
			cmd = (GeCommands.PRIM << 24) | ((prim & 0x7) << 16) | count;
			for (int i = 0; i < n; i++) {
				listWriter.writeNext(cmd);
			}
			listCurrent += (n << 2);
		}

		mem.write32(context + listCurrentOffset, listCurrent);

		if (updateStall != 0) {
			sceGeListUpdateStallAddr(listCurrent);
		}
	}

	static public void sceGuDrawSpline(int contextAddr1, int contextAddr2, int listCurrentOffset, int updateStall) {
		Memory mem = getMemory();
		int vtype = getGprA0();
		int ucount = getGprA1();
		int vcount = getGprA2();
		int uedge = getGprA3();
		int vedge = getGprT0();
		int indices = getGprT1();
		int vertices = getGprT2();
		int context = mem.read32(getRelocatedAddress(contextAddr1, contextAddr2));
		int listCurrent = mem.read32(context + listCurrentOffset);
		int cmd;

		if (vtype != 0) {
			cmd = (GeCommands.VTYPE << 24) | (vtype & 0x00FFFFFF);
			mem.write32(listCurrent, cmd);
			listCurrent += 4;
		}

		if (indices != 0) {
			cmd = (GeCommands.BASE << 24) | ((indices >> 8) & 0x000F0000);
			mem.write32(listCurrent, cmd);
			listCurrent += 4;

			cmd = (GeCommands.IADDR << 24) | (indices & 0x00FFFFFF);
			mem.write32(listCurrent, cmd);
			listCurrent += 4;
		}

		if (vertices != 0) {
			cmd = (GeCommands.BASE << 24) | ((vertices >> 8) & 0x000F0000);
			mem.write32(listCurrent, cmd);
			listCurrent += 4;

			cmd = (GeCommands.VADDR << 24) | (vertices & 0x00FFFFFF);
			mem.write32(listCurrent, cmd);
			listCurrent += 4;
		}

		cmd = (GeCommands.SPLINE << 24) | (vedge << 18) | (uedge << 16) | (vcount << 8) | ucount;
		mem.write32(listCurrent, cmd);
		listCurrent += 4;

		mem.write32(context + listCurrentOffset, listCurrent);

		if (updateStall != 0) {
			sceGeListUpdateStallAddr(listCurrent);
		}
	}

	static public void sceGuCopyImage(int contextAddr1, int contextAddr2, int listCurrentOffset) {
		Memory mem = getMemory();
		int psm = getGprA0();
		int sx = getGprA1();
		int sy = getGprA2();
		int width = getGprA3();
		int height = getGprT0();
		int srcw = getGprT1();
		int src = getGprT2();
		int dx = getGprT3();
		int dy = getStackParam0();
		int destw = getStackParam1();
		int dest = getStackParam2();
		int context = mem.read32(getRelocatedAddress(contextAddr1, contextAddr2));
		int listCurrent = mem.read32(context + listCurrentOffset);
		int cmd;

		IMemoryWriter listWriter = MemoryWriter.getMemoryWriter(listCurrent, 32, 4);

		cmd = (GeCommands.TRXSBP << 24) | (src & 0x00FFFFFF); 
		listWriter.writeNext(cmd);

		cmd = (GeCommands.TRXSBW << 24) | ((src >> 8) & 0x000F0000) | srcw;
		listWriter.writeNext(cmd);

		cmd = (GeCommands.TRXPOS << 24) | (sy << 10) | sx;
		listWriter.writeNext(cmd);

		cmd = (GeCommands.TRXDBP << 24) | (dest & 0x00FFFFFF); 
		listWriter.writeNext(cmd);

		cmd = (GeCommands.TRXDBW << 24) | ((dest >> 8) & 0x000F0000) | destw;
		listWriter.writeNext(cmd);

		cmd = (GeCommands.TRXDPOS << 24) | (dy << 10) | dx;
		listWriter.writeNext(cmd);

		cmd = (GeCommands.TRXSIZE << 24) | ((height - 1) << 10) | (width - 1);
		listWriter.writeNext(cmd);

		cmd = (GeCommands.TRXKICK << 24) | (IRenderingEngine.sizeOfTextureType[psm] == 4 ? GeCommands.TRXKICK_32BIT_TEXEL_SIZE : GeCommands.TRXKICK_16BIT_TEXEL_SIZE);
		listWriter.writeNext(cmd);

		listWriter.flush();
		mem.write32(context + listCurrentOffset, listCurrent + 32);
	}

	static public void sceGuTexImage(int contextAddr1, int contextAddr2, int listCurrentOffset) {
		Memory mem = getMemory();
		int mipmap = getGprA0();
		int width = getGprA1();
		int height = getGprA2();
		int tbw = getGprA3();
		int tbp = getGprT0();
		int context = mem.read32(getRelocatedAddress(contextAddr1, contextAddr2));
		int listCurrent = mem.read32(context + listCurrentOffset);
		int cmd;

		cmd = ((GeCommands.TBP0 + mipmap) << 24) | (tbp & 0x00FFFFFF);
		mem.write32(listCurrent, cmd);
		listCurrent += 4;

		cmd = ((GeCommands.TBW0 + mipmap) << 24) | ((tbp >> 8) & 0x000F0000) | tbw;
		mem.write32(listCurrent, cmd);
		listCurrent += 4;

		// widthExp = 31 - CLZ(width)
		int widthExp = 31 - Integer.numberOfLeadingZeros(width);
		int heightExp = 31 - Integer.numberOfLeadingZeros(height);
		cmd = ((GeCommands.TSIZE0 + mipmap) << 24) | (heightExp << 8) | widthExp;
		mem.write32(listCurrent, cmd);
		listCurrent += 4;

		if (writeTFLUSH) {
			cmd = (GeCommands.TFLUSH << 24);
			mem.write32(listCurrent, cmd);
			listCurrent += 4;
		}

		mem.write32(context + listCurrentOffset, listCurrent);
	}

	static public void sceGuTexSync(int contextAddr1, int contextAddr2, int listCurrentOffset) {
		if (writeTSYNC) {
			Memory mem = getMemory();
			int context = mem.read32(getRelocatedAddress(contextAddr1, contextAddr2));
			int listCurrent = mem.read32(context + listCurrentOffset);
			int cmd;

			cmd = (GeCommands.TSYNC << 24);
			mem.write32(listCurrent, cmd);
			listCurrent += 4;

			mem.write32(context + listCurrentOffset, listCurrent);
		}
	}

	static public void sceGuTexMapMode(int contextAddr1, int contextAddr2, int listCurrentOffset, int texProjMapOffset, int texMapModeOffset) {
		Memory mem = getMemory();
		int texMapMode = getGprA0() & 0x3;
		int texShadeU = getGprA1();
		int texShadeV = getGprA2();
		int context = mem.read32(getRelocatedAddress(contextAddr1, contextAddr2));
		int listCurrent = mem.read32(context + listCurrentOffset);
		int cmd;

		int texProjMap = mem.read32(context + texProjMapOffset);
		mem.write32(context + texMapModeOffset, texMapMode);

		cmd = (GeCommands.TMAP << 24) | (texProjMap << 8) | texMapMode;
		mem.write32(listCurrent, cmd);
		listCurrent += 4;

		cmd = (GeCommands.TEXTURE_ENV_MAP_MATRIX << 24) | (texShadeV << 8) | texShadeU;
		mem.write32(listCurrent, cmd);
		listCurrent += 4;

		mem.write32(context + listCurrentOffset, listCurrent);
	}

	static public void sceGuTexProjMapMode(int contextAddr1, int contextAddr2, int listCurrentOffset, int texProjMapOffset, int texMapModeOffset) {
		Memory mem = getMemory();
		int texProjMap = getGprA0() & 0x3;
		int context = mem.read32(getRelocatedAddress(contextAddr1, contextAddr2));
		int listCurrent = mem.read32(context + listCurrentOffset);
		int cmd;

		int texMapMode = mem.read32(context + texMapModeOffset);
		mem.write32(context + texProjMapOffset, texProjMap);

		cmd = (GeCommands.TMAP << 24) | (texProjMap << 8) | texMapMode;
		mem.write32(listCurrent, cmd);
		listCurrent += 4;

		mem.write32(context + listCurrentOffset, listCurrent);
	}

	static public void sceGuTexLevelMode(int contextAddr1, int contextAddr2, int listCurrentOffset) {
		Memory mem = getMemory();
		int mode = getGprA0();
		float bias = getFprF12();
		int context = mem.read32(getRelocatedAddress(contextAddr1, contextAddr2));
		int listCurrent = mem.read32(context + listCurrentOffset);
		int cmd;

		int offset = (int) (bias * 16.f);
		if (offset > 127) {
			offset = 127;
		} else if (offset < -128) {
			offset = -128;
		}

		cmd = (GeCommands.TBIAS << 24) | (offset << 16) | mode;
		mem.write32(listCurrent, cmd);
		listCurrent += 4;

		mem.write32(context + listCurrentOffset, listCurrent);
	}

	static public void sceGuMaterial(int contextAddr1, int contextAddr2, int listCurrentOffset) {
		int mode = getGprA0();
		int color = getGprA1();
		int context = getMemory().read32(getRelocatedAddress(contextAddr1, contextAddr2));
		sceGuMaterial(context, listCurrentOffset, mode, color);
	}

	static public void sceGuMaterial(int listCurrentOffset) {
		int context = getGprA0();
		int mode = getGprA1();
		int color = getGprA2();
		sceGuMaterial(context, listCurrentOffset, mode, color);
	}

	static private void sceGuMaterial(int context, int listCurrentOffset, int mode, int color) {
		Memory mem = getMemory();
		int listCurrent = mem.read32(context + listCurrentOffset);
		int cmd;
		int rgb = color & 0x00FFFFFF;

		if ((mode & 0x01) != 0) {
			cmd = (GeCommands.AMC << 24) | rgb;
			mem.write32(listCurrent, cmd);
			listCurrent += 4;

			cmd = (GeCommands.AMA << 24) | (color >>> 24);
			mem.write32(listCurrent, cmd);
			listCurrent += 4;
		}

		if ((mode & 0x02) != 0) {
			cmd = (GeCommands.DMC << 24) | rgb;
			mem.write32(listCurrent, cmd);
			listCurrent += 4;
		}

		if ((mode & 0x04) != 0) {
			cmd = (GeCommands.SMC << 24) | rgb;
			mem.write32(listCurrent, cmd);
			listCurrent += 4;
		}

		mem.write32(context + listCurrentOffset, listCurrent);
	}

	static public void sceGuSetMatrix(int contextAddr1, int contextAddr2, int listCurrentOffset) {
		int type = getGprA0();
		int matrix = getGprA1();
		int context = getMemory().read32(getRelocatedAddress(contextAddr1, contextAddr2));
		sceGuSetMatrix(context, listCurrentOffset, type, matrix);
	}

	static public void sceGuSetMatrix(int listCurrentOffset) {
		int context = getGprA0();
		int type = getGprA1();
		int matrix = getGprA2();
		sceGuSetMatrix(context, listCurrentOffset, type, matrix);
	}

	static private int sceGuSetMatrix4x4(IMemoryWriter listWriter, IMemoryReader matrixReader, int startCmd, int matrixCmd, int index) {
		listWriter.writeNext((startCmd << 24) + index);
		int cmd = matrixCmd << 24;
		for (int i = 0; i < 16; i++) {
			listWriter.writeNext(cmd | (matrixReader.readNext() >>> 8));
		}
		return 68;
	}

	static private int sceGuSetMatrix4x3(IMemoryWriter listWriter, IMemoryReader matrixReader, int startCmd, int matrixCmd, int index) {
		listWriter.writeNext((startCmd << 24) + index);
		int cmd = matrixCmd << 24;
		for (int i = 0; i < 4; i++) {
			for (int j = 0; j < 3; j++) {
				listWriter.writeNext(cmd | (matrixReader.readNext() >>> 8));
			}
			matrixReader.skip(1);
		}
		return 52;
	}

	static private void sceGuSetMatrix(int context, int listCurrentOffset, int type, int matrix) {
		Memory mem = getMemory();
		int listCurrent = mem.read32(context + listCurrentOffset);

		IMemoryWriter listWriter = MemoryWriter.getMemoryWriter(listCurrent, 68, 4);
		IMemoryReader matrixReader = MemoryReader.getMemoryReader(matrix, 64, 4);
		switch (type) {
			case 0:
				listCurrent += sceGuSetMatrix4x4(listWriter, matrixReader, GeCommands.PMS, GeCommands.PROJ, 0);
				break;
			case 1:
				listCurrent += sceGuSetMatrix4x3(listWriter, matrixReader, GeCommands.VMS, GeCommands.VIEW, 0);
				break;
			case 2:
				listCurrent += sceGuSetMatrix4x3(listWriter, matrixReader, GeCommands.MMS, GeCommands.MODEL, 0);
				break;
			case 3:
				listCurrent += sceGuSetMatrix4x3(listWriter, matrixReader, GeCommands.TMS, GeCommands.TMATRIX, 0);
				break;
		}
		listWriter.flush();

		mem.write32(context + listCurrentOffset, listCurrent);
	}

	static public void sceGuBoneMatrix(int contextAddr1, int contextAddr2, int listCurrentOffset) {
		int index = getGprA0();
		int matrix = getGprA1();
		int context = getMemory().read32(getRelocatedAddress(contextAddr1, contextAddr2));
		sceGuBoneMatrix(context, listCurrentOffset, index, matrix);
	}

	static public void sceGuBoneMatrix(int listCurrentOffset) {
		int context = getGprA0();
		int index = getGprA1();
		int matrix = getGprA2();
		sceGuBoneMatrix(context, listCurrentOffset, index, matrix);
	}

	static private void sceGuBoneMatrix(int context, int listCurrentOffset, int index, int matrix) {
		Memory mem = getMemory();
		int listCurrent = mem.read32(context + listCurrentOffset);

		IMemoryWriter listWriter = MemoryWriter.getMemoryWriter(listCurrent, 56, 4);
		if (writeDUMMY) {
			listWriter.writeNext(GeCommands.DUMMY << 24);
			listCurrent += 4;
		}
		IMemoryReader matrixReader = MemoryReader.getMemoryReader(matrix, 64, 4);
		listCurrent += sceGuSetMatrix4x3(listWriter, matrixReader, GeCommands.BOFS, GeCommands.BONE, index * 12);
		listWriter.flush();

		mem.write32(context + listCurrentOffset, listCurrent);
	}

	static public void sceGuDrawSprite(int contextAddr1, int contextAddr2, int listCurrentOffset, int wOffset, int hOffset, int dxOffset, int dyOffset) {
		int x = getGprA0();
		int y = getGprA1();
		int z = getGprA2();
		int u = getGprA3();
		int v = getGprT0();
		int flip = getGprT1();
		int rotation = getGprT2();
		int context = getMemory().read32(getRelocatedAddress(contextAddr1, contextAddr2));
		sceGuDrawSprite(context, listCurrentOffset, x, y, z, u, v, flip, rotation, wOffset, hOffset, dxOffset, dyOffset);
	}

	static public void sceGuDrawSprite(int listCurrentOffset, int wOffset, int hOffset, int dxOffset, int dyOffset) {
		int context = getGprA0();
		int x = getGprA1();
		int y = getGprA2();
		int z = getGprA3();
		int u = getGprT0();
		int v = getGprT1();
		int flip = getGprT2();
		int rotation = getGprT3();
		sceGuDrawSprite(context, listCurrentOffset, x, y, z, u, v, flip, rotation, wOffset, hOffset, dxOffset, dyOffset);
	}

	static private void sceGuDrawSprite(int context, int listCurrentOffset, int x, int y, int z, int u, int v, int flip, int rotation, int wOffset, int hOffset, int dxOffset, int dyOffset) {
		Memory mem = getMemory();
		int listCurrent = mem.read32(context + listCurrentOffset);
		int w = mem.read32(context + wOffset);
		int h = mem.read32(context + hOffset);
		int dx = mem.read32(context + dxOffset);
		int dy = mem.read32(context + dyOffset);
		int cmd;

		IMemoryWriter listWriter = MemoryWriter.getMemoryWriter(listCurrent, 44, 4);

		int vertexAddress = listCurrent + 8;
		IMemoryWriter vertexWriter = MemoryWriter.getMemoryWriter(vertexAddress, 20, 2);
		int v0u = u;
		int v0v = v;
		int v0x = x;
		int v0y = y;
		int v1u = u + w;
		int v1v = v + h;
		int v1x = x + dx;
		int v1y = y + dy;

		if ((flip & 1) != 0) {
			int tmp = v0u;
			v0u = v1u;
			v1u = tmp;
		}
		if ((flip & 2) != 0) {
			int tmp = v0v;
			v0v = v1v;
			v1v = tmp;
		}
		switch (rotation) {
			case 1: {
				int tmp = v0y;
				v0y = v1y;
				v1y = tmp;
				break;
			}
			case 2: {
				int tmp = v0x;
				v0x = v1x;
				v1x = tmp;
				tmp = v0y;
				v0y = v1y;
				v1y = tmp;
				break;
			}
			case 3: {
				int tmp = v0x;
				v0x = v1x;
				v1x = tmp;
				break;
			}
		}

		vertexWriter.writeNext(v0u);
		vertexWriter.writeNext(v0v);
		vertexWriter.writeNext(v0x);
		vertexWriter.writeNext(v0y);
		vertexWriter.writeNext(z);
		vertexWriter.writeNext(v1u);
		vertexWriter.writeNext(v1v);
		vertexWriter.writeNext(v1x);
		vertexWriter.writeNext(v1y);
		vertexWriter.writeNext(z);
		vertexWriter.flush();

		int jumpAddr = vertexAddress + 20;
		cmd = (GeCommands.BASE << 24) | ((jumpAddr >> 8) & 0x00FF0000);
		listWriter.writeNext(cmd);

		cmd = (GeCommands.JUMP << 24) | (jumpAddr & 0x00FFFFFF);
		listWriter.writeNext(cmd);

		// Skip the 2 vertex entries
		listWriter.skip(5);

		cmd = (GeCommands.VTYPE << 24) | ((GeCommands.VTYPE_TRANSFORM_PIPELINE_RAW_COORD << 23) | (GeCommands.VTYPE_POSITION_FORMAT_16_BIT << 7) | GeCommands.VTYPE_TEXTURE_FORMAT_16_BIT);
		listWriter.writeNext(cmd);

		cmd = (GeCommands.BASE << 24) | ((vertexAddress >> 8) & 0x00FF0000);
		listWriter.writeNext(cmd);

		cmd = (GeCommands.VADDR << 24) | (vertexAddress & 0x00FFFFFF);
		listWriter.writeNext(cmd);

		cmd = (GeCommands.PRIM << 24) | (GeCommands.PRIM_SPRITES << 16) | 2;
		listWriter.writeNext(cmd);

		listWriter.flush();

		mem.write32(context + listCurrentOffset, jumpAddr + 16);
	}
}
