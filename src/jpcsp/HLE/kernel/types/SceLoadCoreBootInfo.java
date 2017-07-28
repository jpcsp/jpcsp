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
	/** 
     * Pointer to a memory block which will be cleared in case the system initialization via 
     * Loadcore fails.
     */
	public TPointer memBase;
	/** The size of the memory block to clear. */
	public int memSize;
	/** Number of modules already loaded during boot process. */
	public int loadedModules;
	/** Number of modules to boot. */
	public int numModules;
	/** The modules to boot. */
	public TPointer modules;
	public int unknown20;
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
	public int unknown44;
	public int buildVersion;
	public int unknown52;
	/** The path/name of a boot configuration file. */
	public TPointer configFile;
	public final int unknown60[] = new int[17];

	@Override
	protected void read() {
		memBase = readPointer(); // Offset 0
		memSize = read32(); // Offset 4
		loadedModules = read32(); // Offset 8
		numModules = read32(); // Offset 12
		modules = readPointer(); // Offset 16
		unknown20 = read32(); // Offset 20
		unknown24 = read8(); // Offset 24
		read8Array(reserved); // Offset 25
		numProtects = read32(); // Offset 28
		protects = readPointer(); // Offset 32
		modProtId = read32(); // Offset 36
		modArgProtId = read32(); // Offset 40
		unknown44 = read32(); // Offset 44
		buildVersion = read32(); // Offset 48
		unknown52 = read32(); // Offset 52
		configFile = readPointer(); // Offset 56
		read32Array(unknown60); // Offset 60
	}

	@Override
	protected void write() {
		writePointer(memBase);
		write32(memSize);
		write32(loadedModules);
		write32(numModules);
		writePointer(modules);
		write32(unknown20);
		write8((byte) unknown24);
		write8Array(reserved);
		write32(numProtects);
		writePointer(protects);
		write32(modProtId);
		write32(modArgProtId);
		write32(unknown44);
		write32(buildVersion);
		write32(unknown52);
		writePointer(configFile);
		write32Array(unknown60);
	}

	@Override
	public int sizeof() {
		return 128;
	}
}
