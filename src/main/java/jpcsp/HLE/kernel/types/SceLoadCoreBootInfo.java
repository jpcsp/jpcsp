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
package jpcsp.HLE.kernel.types;

import jpcsp.HLE.TPointer;

public class SceLoadCoreBootInfo extends pspAbstractMemoryMappedStructure {
	// This structure seems to be the same as SceKernelRebootParam.
	/** 
     * Pointer to a memory block which will be cleared in case the system initialization via 
     * Loadcore fails.
     */
	public int memBase;
	/** The size of the memory block to clear. */
	public int memSize;
	/** Number of modules already loaded during boot process. */
	public int loadedModules;
	/** Number of modules to boot. */
	public int numModules;
	/** The modules to boot. */
	public TPointer startAddr;
	public TPointer endAddr;
	public int unknown24;
	public final byte reserved[] = new byte[3];
	/** The number of protected (?)modules.*/
	public int numProtects;
	/** Pointer to the protected (?)modules. */
	public TPointer protects;
	/** The ID of a protected info. */
	public int modProtId;
	/** The ID of a module's arguments? */
	public int modArgProtId;
	/** The PSP model as returned by sceKernelGetModel() */
	public int model;
	public int buildVersion;
	public int unknown52;
	/** The path/name of a boot configuration file. */
	public TPointer configFile;
	public int unknown60;
	public int dipswLo;
	public int dipswHi;
	public int unknown72;
	public int unknown76;
	public int cpTime;

	@Override
	protected void read() {
		memBase = read32(); // Offset 0
		memSize = read32(); // Offset 4
		loadedModules = read32(); // Offset 8
		numModules = read32(); // Offset 12
		startAddr = readPointer(); // Offset 16
		endAddr = readPointer(); // Offset 20
		unknown24 = read8(); // Offset 24
		read8Array(reserved); // Offset 25
		numProtects = read32(); // Offset 28
		protects = readPointer(); // Offset 32
		modProtId = read32(); // Offset 36
		modArgProtId = read32(); // Offset 40
		model = read32(); // Offset 44
		buildVersion = read32(); // Offset 48
		unknown52 = read32(); // Offset 52
		configFile = readPointer(); // Offset 56
		unknown60 = read32(); // Offset 60
		dipswLo = read32(); // Offset 64
		dipswHi = read32(); // Offset 68
		unknown72 = read32(); // Offset 72
		unknown76 = read32(); // Offset 76
		cpTime = read32(); // Offset 80
	}

	@Override
	protected void write() {
		write32(memBase);
		write32(memSize);
		write32(loadedModules);
		write32(numModules);
		writePointer(startAddr);
		writePointer(endAddr);
		write8((byte) unknown24);
		write8Array(reserved);
		write32(numProtects);
		writePointer(protects);
		write32(modProtId);
		write32(modArgProtId);
		write32(model);
		write32(buildVersion);
		write32(unknown52);
		writePointer(configFile);
		write32(unknown60);
		write32(dipswLo);
		write32(dipswHi);
		write32(unknown72);
		write32(unknown76);
		write32(cpTime);
	}

	@Override
	public int sizeof() {
		// Size of SceKernelRebootParam
		return 0x1000;
	}
}
