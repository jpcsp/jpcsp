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

	@HLEFunction(nid = 0x7233B5BC, version = 150)
	public int sceKernelApplicationType() {
		return SCE_INIT_APPLICATION_GAME;
	}

	@HLEFunction(nid = 0x27932388, version = 150)
	public int sceKernelBootFrom() {
		return SCE_INIT_BOOT_DISC;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0xA18A4A8B, version = 150)
	public int sceKernelInitDiscImage() {
		// Has no parameters
		return 0;
	}
}
