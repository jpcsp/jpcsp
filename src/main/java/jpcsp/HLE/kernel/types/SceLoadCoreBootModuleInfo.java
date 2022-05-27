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

/**
 * This structure is used to boot system modules during the initialization of Loadcore. It represents
 * a module object with all the necessary information needed to boot it.
 */
public class SceLoadCoreBootModuleInfo extends pspAbstractMemoryMappedStructure {
    /** The full path (including filename) of the module. */
    public TPointer modPath; //0
    /** The buffer with the entire file content. */
    public TPointer modBuf; //4
    /** The size of the module. */
    public int modSize; //8
    /** Unknown. */
    public TPointer unk12; //12
    /** Attributes. */
    public int attr; //16
    /** 
     * Contains the API type of the module prior to the allocation of memory for the module. 
     * Once memory is allocated, ::bootData contains the ID of that memory partition.
     */
    public int bootData; //20
    /** The size of the arguments passed to the module's entry function? */
    public int argSize; //24
    /** The partition ID of the arguments passed to the module's entry function? */
    public int argPartId; //28

	@Override
	protected void read() {
		modPath = readPointer();
		modBuf = readPointer();
		modSize = read32();
		unk12 = readPointer();
		attr = read32();
		bootData = read32();
		argSize = read32();
		argPartId = read32();
	}

	@Override
	protected void write() {
		writePointer(modPath);
		writePointer(modBuf);
		write32(modSize);
		writePointer(unk12);
		write32(attr);
		write32(bootData);
		write32(argSize);
		write32(argPartId);
	}

	@Override
	public int sizeof() {
		return 32;
	}
}
