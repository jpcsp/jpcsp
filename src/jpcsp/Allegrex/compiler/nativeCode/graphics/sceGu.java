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
import jpcsp.HLE.modules.sceDisplay;
import jpcsp.graphics.GeCommands;
import jpcsp.graphics.VideoEngine;
import jpcsp.memory.IMemoryWriter;
import jpcsp.memory.MemoryWriter;

/**
 * @author gid15
 *
 */
public class sceGu extends AbstractNativeCodeSequence {
	static private void sceGeListUpdateStallAddr(int addr) {
		// Simplification: we can update the stall address only if the VideoEngine
		// is processing one and only one GE list.
		VideoEngine videoEngine = VideoEngine.getInstance();
		if (videoEngine.numberDrawLists() == 0) {
			PspGeList geList = videoEngine.getCurrentList();
			if (geList != null) {
				addr &= Memory.addressMask;
				geList.setStallAddr(addr);
			}
		}
	}

	static public void sceGuDrawArray(int contextAddr1, int contextAddr2, int listCurrentOffset) {
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

		sceGeListUpdateStallAddr(listCurrent);
	}

	static public void sceGuDrawArrayN(int contextAddr1, int contextAddr2, int listCurrentOffset) {
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

		if (n > 0) {
			cmd = (GeCommands.PRIM << 24) | ((prim & 0x7) << 16) | count;
			for (int i = 0; i < n; i++) {
				mem.write32(listCurrent, cmd);
				listCurrent += 4;
			}
		}

		mem.write32(context + listCurrentOffset, listCurrent);

		sceGeListUpdateStallAddr(listCurrent);
	}

	static public void sceGuDrawSpline(int contextAddr1, int contextAddr2, int listCurrentOffset) {
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

		sceGeListUpdateStallAddr(listCurrent);
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

		IMemoryWriter listWriter = MemoryWriter.getMemoryWriter(listCurrent, 28, 4);

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

		cmd = (GeCommands.TRXKICK << 24) | (psm == sceDisplay.PSP_DISPLAY_PIXEL_FORMAT_8888 ? GeCommands.TRXKICK_32BIT_TEXEL_SIZE : GeCommands.TRXKICK_16BIT_TEXEL_SIZE);
		listWriter.writeNext(cmd);

		listWriter.flush();
		mem.write32(context + listCurrentOffset, listCurrent + 28);
	}
}
