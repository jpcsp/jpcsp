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
package jpcsp.util;

import static jpcsp.graphics.GeCommands.PRIM_LINE;
import static jpcsp.graphics.GeCommands.TFUNC_FRAGMENT_DOUBLE_ENABLE_COLOR_DOUBLED;
import static jpcsp.graphics.GeCommands.TFUNC_FRAGMENT_DOUBLE_ENABLE_COLOR_UNTOUCHED;
import static jpcsp.graphics.GeCommands.TFUNC_FRAGMENT_DOUBLE_TEXTURE_COLOR_ALPHA_IS_IGNORED;
import static jpcsp.graphics.GeCommands.TFUNC_FRAGMENT_DOUBLE_TEXTURE_COLOR_ALPHA_IS_READ;
import static jpcsp.graphics.GeCommands.VTYPE_COLOR_FORMAT_32BIT_ABGR_8888;
import static jpcsp.graphics.GeCommands.VTYPE_POSITION_FORMAT_16_BIT;
import static jpcsp.graphics.GeCommands.VTYPE_TRANSFORM_PIPELINE_RAW_COORD;

import org.apache.log4j.Logger;

import jpcsp.Memory;
import jpcsp.MemoryMap;
import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.modules.sceGe_user;
import jpcsp.HLE.modules150.SysMemUserForUser;
import jpcsp.HLE.modules150.SysMemUserForUser.SysMemInfo;
import jpcsp.graphics.GeCommands;
import jpcsp.graphics.GeContext;
import jpcsp.graphics.RE.IRenderingEngine;
import jpcsp.hardware.Screen;
import jpcsp.memory.IMemoryWriter;
import jpcsp.memory.MemoryWriter;

/**
 * Utility class to draw to the PSP GE from inside HLE functions.
 * Used for example by sceUtilitySavedata to render its user interface.
 * 
 * @author gid15
 *
 */
public class sceGu {
	private static Logger log = Logger.getLogger("sceGu");
	private SysMemInfo sysMemInfo;
	private int bottomAddr;
	private int topAddr;
	private int listAddr;
	private IMemoryWriter listWriter;
	private int listId = -1;

	public sceGu(int totalMemorySize) {
		sysMemInfo = Modules.SysMemUserForUserModule.malloc(SysMemUserForUser.KERNEL_PARTITION_ID, "sceGu", SysMemUserForUser.PSP_SMEM_Low, totalMemorySize, 0);
		if (sysMemInfo == null) {
			log.error(String.format("Cannot allocate sceGu memory, size=0x%X", totalMemorySize));
		}
	}

	public int sceGuGetMemory(int size) {
		size = Utilities.alignUp(size, 3);
		if (topAddr - size < listWriter.getCurrentAddress()) {
			if (log.isDebugEnabled()) {
				log.debug(String.format("sceGuGetMemory size=0x%X - not enough memory, available=0x%X", size, topAddr - listWriter.getCurrentAddress()));
			}
			// Not enough memory available
			return 0;
		}

		topAddr -= size;

		return topAddr;
	}

	public void free() {
		if (sysMemInfo != null) {
			Modules.SysMemUserForUserModule.free(sysMemInfo);
			sysMemInfo = null;
		}
		bottomAddr = 0;
		topAddr = 0;
		listWriter = null;
	}

	public void sendCommandi(int cmd, int argument) {
		listWriter.writeNext((cmd << 24) | (argument & 0x00FFFFFF));
	}

	public void sendCommandf(int cmd, float argument) {
		sendCommandi(cmd, Float.floatToRawIntBits(argument) >> 8);
	}

	protected void sendCommandBase(int cmd, int address) {
		sendCommandi(GeCommands.BASE, (address & 0xFF000000) >>> 8);
		sendCommandi(cmd, address & 0x00FFFFFF);
	}

	public void sceGuStart() {
		if (sysMemInfo != null) {
			bottomAddr = sysMemInfo.addr;
			topAddr = sysMemInfo.addr + sysMemInfo.size;
		} else {
			// Reserve memory for 2 complete screens (double buffering)
			int reservedSize = 512 * Screen.height * 4 * 2;

			// Use the rest of the VRAM
			bottomAddr = MemoryMap.START_VRAM + reservedSize;
			topAddr = bottomAddr + (MemoryMap.SIZE_VRAM - reservedSize);
		}

		listAddr = bottomAddr;
		listWriter = MemoryWriter.getMemoryWriter(listAddr, 4);
		listId = -1;

		Memory.getInstance().memset(bottomAddr, (byte) 0, topAddr - bottomAddr);

		// Init some values
		sceGuOffsetAddr(0);
		sendCommandi(GeCommands.BASE, 0);
	}

	public void sceGuFinish() {
		sendCommandi(GeCommands.FINISH, 0);
		sendCommandi(GeCommands.END, 0);

		if (topAddr < listWriter.getCurrentAddress()) {
			log.error(String.format("sceGu memory overwrite mem=%s, listAddr=0x%08X, topAddr=0x%08X", sysMemInfo, listWriter.getCurrentAddress(), topAddr));
		} else {
			if (log.isDebugEnabled()) {
				log.debug(String.format("sceGu memory usage free=0x%X, mem=%s, listAddr=0x%08X, topAddr=0x%08X", topAddr - listWriter.getCurrentAddress(), sysMemInfo, listWriter.getCurrentAddress(), topAddr));
			}
		}

		listWriter.flush();

		int saveContextAddr = sceGuGetMemory(GeContext.SIZE_OF);

		Memory mem = Memory.getInstance();
		listId = Modules.sceGe_userModule.hleGeListEnQueue(new TPointer(mem, listAddr), TPointer.NULL, -1, TPointer.NULL, saveContextAddr);
	}

	public boolean isListDrawing() {
		if (listId < 0) {
			return false;
		}

		int listState = Modules.sceGe_userModule.hleGeListSync(listId);

		if (log.isDebugEnabled()) {
			log.debug(String.format("sceGu list 0x%X: state %d", listId, listState));
		}

		return listState == sceGe_user.PSP_GE_LIST_DRAWING || listState == sceGe_user.PSP_GE_LIST_QUEUED;
	}

	private void sceGuSetFlag(int flag, int value) {
		switch (flag) {
			case IRenderingEngine.GU_ALPHA_TEST:          sendCommandi(GeCommands.ATE, value);   break;
			case IRenderingEngine.GU_DEPTH_TEST:          sendCommandi(GeCommands.ZTE, value);   break;
			case IRenderingEngine.GU_SCISSOR_TEST:                                               break;
			case IRenderingEngine.GU_STENCIL_TEST:        sendCommandi(GeCommands.STE, value);   break;
			case IRenderingEngine.GU_BLEND:               sendCommandi(GeCommands.ABE, value);   break;
			case IRenderingEngine.GU_CULL_FACE:           sendCommandi(GeCommands.BCE, value);   break;
			case IRenderingEngine.GU_DITHER:              sendCommandi(GeCommands.DTE, value);   break;
			case IRenderingEngine.GU_FOG:                 sendCommandi(GeCommands.FGE, value);   break;
			case IRenderingEngine.GU_CLIP_PLANES:         sendCommandi(GeCommands.CPE, value);   break;
			case IRenderingEngine.GU_TEXTURE_2D:          sendCommandi(GeCommands.TME, value);   break;
			case IRenderingEngine.GU_LIGHTING:            sendCommandi(GeCommands.LTE, value);   break;
			case IRenderingEngine.GU_LIGHT0:              sendCommandi(GeCommands.LTE0, value);  break;
			case IRenderingEngine.GU_LIGHT1:              sendCommandi(GeCommands.LTE1, value);  break;
			case IRenderingEngine.GU_LIGHT2:              sendCommandi(GeCommands.LTE2, value);  break;
			case IRenderingEngine.GU_LIGHT3:              sendCommandi(GeCommands.LTE3, value);  break;
			case IRenderingEngine.GU_LINE_SMOOTH:         sendCommandi(GeCommands.AAE, value);   break;
			case IRenderingEngine.GU_PATCH_CULL_FACE:     sendCommandi(GeCommands.PCE, value);   break;
			case IRenderingEngine.GU_COLOR_TEST:          sendCommandi(GeCommands.CTE, value);   break;
			case IRenderingEngine.GU_COLOR_LOGIC_OP:      sendCommandi(GeCommands.LOE, value);   break;
			case IRenderingEngine.GU_FACE_NORMAL_REVERSE: sendCommandi(GeCommands.RNORM, value); break;
			case IRenderingEngine.GU_PATCH_FACE:          sendCommandi(GeCommands.PFACE, value); break;
			case IRenderingEngine.GU_FRAGMENT_2X:                                                break;
		}
	}

	public void sceGuEnable(int flag) {
		sceGuSetFlag(flag, 1);
	}

	public void sceGuDisable(int flag) {
		sceGuSetFlag(flag, 0);
	}

	public void sceGuDrawArray(int prim, int vtype, int count, int indices, int vertices) {
		if (vtype != 0) {
			sendCommandi(GeCommands.VTYPE, vtype);
		}

		if (indices != 0) {
			sendCommandBase(GeCommands.IADDR, indices);
		}

		if (vertices != 0) {
			sendCommandBase(GeCommands.VADDR, vertices);
		}

		sendCommandi(GeCommands.PRIM, (prim << 16) | (count & 0xFFFF));
	}

	private int getExp(int val) {
		int i;
		for (i = 9; i > 0 && ((val >> i) & 1) == 0; i--) {
		}

		return i;
	}

	public void sceGuTexImage(int mipmap, int width, int height, int tbw, int tbp) {
		sendCommandi(GeCommands.TBP0 + mipmap, tbp & 0x00FFFFFF);
		sendCommandi(GeCommands.TBW0 + mipmap, ((tbp & 0xFF000000) >>> 8) | (tbw & 0xFFFF));
		sendCommandi(GeCommands.TSIZE0 + mipmap, (getExp(height) << 8) | getExp(width));
		sendCommandi(GeCommands.TFLUSH, 0);
	}

	public void sceGuTexMode(int tpsm, int maxMipmaps, boolean swizzle) {
		sendCommandi(GeCommands.TMODE, (maxMipmaps << 16) | (swizzle ? 1 : 0));
		sendCommandi(GeCommands.TPSM, tpsm);
	}

	public void sceGuClutMode(int cpsm, int shift, int mask, int start) {
		sendCommandi(GeCommands.CMODE, cpsm | (shift << 12) | (mask << 8) | (start << 16));
	}

	public void sceGuClutLoad(int numBlocks, int cbp) {
		sendCommandi(GeCommands.CBP, cbp & 0x00FFFFFF);
		sendCommandi(GeCommands.CBPH, (cbp & 0xFF000000) >>> 8);
		sendCommandi(GeCommands.CLOAD, numBlocks);
	}

	public void sceGuTexFunc(int textureFunc, boolean textureAlphaUsed, boolean textureColorDoubled) {
		sendCommandi(GeCommands.TFUNC, textureFunc | ((textureAlphaUsed ? TFUNC_FRAGMENT_DOUBLE_TEXTURE_COLOR_ALPHA_IS_READ : TFUNC_FRAGMENT_DOUBLE_TEXTURE_COLOR_ALPHA_IS_IGNORED) << 8) | ((textureColorDoubled ? TFUNC_FRAGMENT_DOUBLE_ENABLE_COLOR_DOUBLED : TFUNC_FRAGMENT_DOUBLE_ENABLE_COLOR_UNTOUCHED) << 16));
	}

	public void sceGuBlendFunc(int op, int src, int dest, int srcFix, int destFix) {
		sendCommandi(GeCommands.ALPHA, src | (dest << 4) | (op << 8));
		if (src >= GeCommands.ALPHA_FIX) {
			sendCommandi(GeCommands.SFIX, srcFix);
		}
		if (dest >= GeCommands.ALPHA_FIX) {
			sendCommandi(GeCommands.DFIX, destFix);
		}
	}

	public void sceGuOffsetAddr(int offsetAddr) {
		sendCommandi(GeCommands.OFFSET_ADDR, offsetAddr >>> 8);
	}

	public void sceGuTexEnvColor(int color) {
		sendCommandi(GeCommands.TEC, color & 0x00FFFFFF);
	}

	public void sceGuTexWrap(int u, int v) {
		sendCommandi(GeCommands.TWRAP, (v << 8) | u);
	}

	public void sceGuTexFilter(int min, int mag) {
		sendCommandi(GeCommands.TFLT, (mag << 8) | min);
	}

	public void sceGuDrawLine(int x0, int y0, int x1, int y1, int color) {
        int numberOfVertex = 2;
        int lineVertexAddr = sceGuGetMemory(12 * numberOfVertex);
        IMemoryWriter lineVertexWriter = MemoryWriter.getMemoryWriter(lineVertexAddr, 2);
        // Color
        lineVertexWriter.writeNext(color & 0xFFFF);
        lineVertexWriter.writeNext(color >>> 16);
        // Position
        lineVertexWriter.writeNext(x0);
        lineVertexWriter.writeNext(y0);
        lineVertexWriter.writeNext(0);
        // Align on 32-bit
        lineVertexWriter.writeNext(0);
        // Color
        lineVertexWriter.writeNext(color & 0xFFFF);
        lineVertexWriter.writeNext(color >>> 16);
        // Position
        lineVertexWriter.writeNext(x1);
        lineVertexWriter.writeNext(y1);
        lineVertexWriter.writeNext(0);
        lineVertexWriter.flush();
        // Align on 32-bit
        lineVertexWriter.writeNext(0);

        sceGuDisable(IRenderingEngine.GU_TEXTURE_2D);
		sceGuDrawArray(PRIM_LINE, (VTYPE_TRANSFORM_PIPELINE_RAW_COORD << 23) | (VTYPE_COLOR_FORMAT_32BIT_ABGR_8888 << 2) | (VTYPE_POSITION_FORMAT_16_BIT << 7), numberOfVertex, 0, lineVertexAddr);
	}

	public void sceGuDrawRectangle(int x0, int y0, int x1, int y1, int color) {
        int numberOfVertex = 2;
        int lineVertexAddr = sceGuGetMemory(12 * numberOfVertex);
        IMemoryWriter lineVertexWriter = MemoryWriter.getMemoryWriter(lineVertexAddr, 2);
        // Color
        lineVertexWriter.writeNext(color & 0xFFFF);
        lineVertexWriter.writeNext(color >>> 16);
        // Position
        lineVertexWriter.writeNext(x0);
        lineVertexWriter.writeNext(y0);
        lineVertexWriter.writeNext(0);
        // Align on 32-bit
        lineVertexWriter.writeNext(0);
        // Color
        lineVertexWriter.writeNext(color & 0xFFFF);
        lineVertexWriter.writeNext(color >>> 16);
        // Position
        lineVertexWriter.writeNext(x1);
        lineVertexWriter.writeNext(y1);
        lineVertexWriter.writeNext(0);
        lineVertexWriter.flush();
        // Align on 32-bit
        lineVertexWriter.writeNext(0);

        sceGuDisable(IRenderingEngine.GU_TEXTURE_2D);
		sceGuDrawArray(GeCommands.PRIM_SPRITES, (VTYPE_TRANSFORM_PIPELINE_RAW_COORD << 23) | (VTYPE_COLOR_FORMAT_32BIT_ABGR_8888 << 2) | (VTYPE_POSITION_FORMAT_16_BIT << 7), numberOfVertex, 0, lineVertexAddr);
	}
}
