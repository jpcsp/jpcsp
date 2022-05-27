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
package jpcsp.HLE.modules;

import org.apache.log4j.Logger;

import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.Modules;

public class InitForKernel extends HLEModule {
	public static Logger log = Modules.getLogger("InitForKernel");
	public static final int SCE_INIT_APPLICATION_VSH = 0x100;
	public static final int SCE_INIT_APPLICATION_UPDATER = 0x110;
	public static final int SCE_INIT_APPLICATION_GAME = 0x200;
	public static final int SCE_INIT_APPLICATION_POPS = 0x300;
	public static final int SCE_INIT_APPLICATION_APP = 0x400;
	public static final int SCE_INIT_BOOT_FLASH = 0x00;
	public static final int SCE_INIT_BOOT_DISC = 0x20;
	public static final int SCE_INIT_BOOT_USBWLAN = 0x30;
	public static final int SCE_INIT_BOOT_MS = 0x40;
	public static final int SCE_INIT_BOOT_EF = 0x50;
	public static final int SCE_INIT_BOOT_FLASH3 = 0x80;
    // Unknown.
    public static final int SCE_INIT_APITYPE_UNK0x100       = 0x100;
    // GAME EBOOT.
    public static final int SCE_INIT_APITYPE_GAME_EBOOT     = 0x110;
    // GAME BOOT.
    public static final int SCE_INIT_APITYPE_GAME_BOOT      = 0x111;
    // Emulated EBOOT Memory-Stick.
    public static final int SCE_INIT_APITYPE_EMU_EBOOT_MS   = 0x112;
    // Emulated BOOT Memory-Stick.
    public static final int SCE_INIT_APITYPE_EMU_BOOT_MS    = 0x113;
    // Emulated EBOOT EF.
    public static final int SCE_INIT_APITYPE_EMU_EBOOT_EF   = 0x114;
    // Emulated BOOT EF.
    public static final int SCE_INIT_APITYPE_EMU_BOOT_EF    = 0x115;
    // NP-DRM Memory-Stick.
    public static final int SCE_INIT_APITYPE_NPDRM_MS       = 0x116; // Distributed programs and data through the Playstation Store.
    // NP-DRM EF.
    public static final int SCE_INIT_APITYPE_NPDRM_EF       = 0x118; // NP-DRM: PlayStation Network Platform Digital Rights Management
    // Executable on a disc.
    public static final int SCE_INIT_APITYPE_DISC           = 0x120;
    // Updater executable on a disc
    public static final int SCE_INIT_APITYPE_DISC_UPDATER   = 0x121;
    // Disc debugger.
    public static final int SCE_INIT_APITYPE_DISC_DEBUG     = 0x122;
    // NP-9660 game.
    public static final int SCE_INIT_APITYPE_DISC_EMU_MS1   = 0x123;
    // Unknown.
    public static final int SCE_INIT_APITYPE_DISC_EMU_MS2   = 0x124;
    // Unknown.
    public static final int SCE_INIT_APITYPE_DISC_EMU_EF1   = 0x125;
    // Unknown.
    public static final int SCE_INIT_APITYPE_DISC_EMU_EF2   = 0x126;
    // Game-sharing executable.
    public static final int SCE_INIT_APITYPE_USBWLAN        = 0x130;
    // Unknown.
    public static final int SCE_INIT_APITYPE_USBWLAN_DEBUG  = 0x131;
    // Unknown.
    public static final int SCE_INIT_APITYPE_UNK            = 0x132;
    // Unknown.
    public static final int SCE_INIT_APITYPE_UNK_DEBUG      = 0x133;
    // Unknown.
    public static final int SCE_INIT_APITYPE_MS1            = 0x140;
    // Unknown.
    public static final int SCE_INIT_APITYPE_MS2            = 0x141;
    // Unknown.
    public static final int SCE_INIT_APITYPE_MS3            = 0x142;
    // Applications (i.e. Comic Reader)
    public static final int SCE_INIT_APITYPE_MS4            = 0x143;
    // Playstation One executable.
    public static final int SCE_INIT_APITYPE_MS5            = 0x144;
    // Unknown.
    public static final int SCE_INIT_APITYPE_MS6            = 0x145;
    // Unknown.
    public static final int SCE_INIT_APITYPE_EF1            = 0x151;
    // Unknown.
    public static final int SCE_INIT_APITYPE_EF2            = 0x152;
    // Unknown.
    public static final int SCE_INIT_APITYPE_EF3            = 0x153;
    // Unknown.
    public static final int SCE_INIT_APITYPE_EF4            = 0x154;
    // Unknown.
    public static final int SCE_INIT_APITYPE_EF5            = 0x155;
    // Unknown.
    public static final int SCE_INIT_APITYPE_EF6            = 0x156;
    // Unknown.
    public static final int SCE_INIT_APITYPE_UNK_GAME1      = 0x160;
    // Unknown.
    public static final int SCE_INIT_APITYPE_UNK_GAME2      = 0x161;
    // Unknown.
    public static final int SCE_INIT_APITYPE_MLNAPP_MS      = 0x170;
    // Unknown.
    public static final int SCE_INIT_APITYPE_MLNAPP_EF      = 0x171;
    // Unknown.
    public static final int SCE_INIT_APITYPE_KERNEL_1       = 0x200;
    // Exit Game.
    public static final int SCE_INIT_APITYPE_VSH_1          = 0x210;
    // Exit VSH.
    public static final int SCE_INIT_APITYPE_VSH_2          = 0x220;
    // Kernel reboot.
    public static final int SCE_INIT_APITYPE_KERNEL_REBOOT  = 0x300;
    // Debug.
	public static final int SCE_INIT_APITYPE_DEBUG = 0x420; // doesn't start reboot
	private int applicationType = SCE_INIT_APPLICATION_GAME;
	private int bootFrom = SCE_INIT_BOOT_DISC;

	public void setApplicationType(int applicationType) {
		this.applicationType = applicationType;
	}

	public void setBootFrom(int bootFrom) {
		this.bootFrom = bootFrom;
	}

	@HLEFunction(nid = 0x7233B5BC, version = 150)
	public int sceKernelApplicationType() {
		return applicationType;
	}

	@HLEFunction(nid = 0x27932388, version = 150)
	public int sceKernelBootFrom() {
		return bootFrom;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0xA18A4A8B, version = 150)
	public int sceKernelInitDiscImage() {
		// Has no parameters
		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x7A2333AD, version = 150)
	public int sceKernelInitApitype() {
		// Has no parameters
		return SCE_INIT_APITYPE_GAME_EBOOT;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x5238F4CC, version = 150)
	public int sceKernelInitLptSummary() {
		// Has no parameters
		return 0;
	}

	/**
	 * Get a chunk's memory block ID.
	 * 
	 * @param chunkId The ID of the chunk which memory block ID you want to receive.
	 *                Between 0 - 15.
	 * 
	 * @return The memory block ID on success (greater than or equal to 0) or 
	 *         SCE_ERROR_KERNEL_ILLEGAL_CHUNK_ID.
	 */
	@HLEUnimplemented
	@HLEFunction(nid = 0x2C6E9FE9, version = 150)
	public int sceKernelGetChunk(int chunkId) {
		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x9D33A110, version = 660)
	public int sceKernelBootFromGo() {
		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0xD83A9BD7, version = 150)
	public int sceKernelInitParamSfo() {
		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x040C934B, version = 150)
	public int sceKernelQueryInitCB() {
		// Has no parameters
		return 0;
	}

	/**
	 * Register a chunk in the system.
	 *
	 * @param chunkId The ID of the chunk to hold the memory block ID. Between 0 - 15.
	 * @param blockId The memory block ID to register.
	 * @return        The blockId stored into the chunk on success, otherwise SCE_ERROR_KERNEL_ILLEGAL_CHUNK_ID.
	 */
	@HLEUnimplemented
	@HLEFunction(nid = 0x1D3256BA, version = 150)
	public int sceKernelRegisterChunk(int chunkId, int blockId) {
		return 0;
	}
}
