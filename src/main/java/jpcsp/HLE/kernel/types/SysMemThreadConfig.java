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

public class SysMemThreadConfig extends pspAbstractMemoryMappedStructure {
	public int unknown0;
	// number of sysmem's export libraries - set in SysMemInit (from utopia)
	public int numExportLibs;
	// array of sysmem's export tables set in SysMemInit (from utopia)
	public final TPointer kernelLibs[] = new TPointer[8];
	// allocated in SysMemInit (from utopia)
	public TPointer loadCoreAddr;
	// offset in export_lib at which user libraries begin - set in SysMemInit (from utopia)
	public int numKernelLibs;
	public int unknown48;
	// SceUID (*allocPartitionMemory)(s32 mpid, char *name, u32 type, u32 size, u32 addr)
	public TPointer allocPartitionMemory;
	// void * (*getBlockHeadAddr)(SceUID id)
	public TPointer getBlockHeadAddr;
    // s32 (*ResizeMemoryBlock)(SceUID id, s32 leftShift, s32 rightShift);
	public TPointer resizeMemoryBlock;
	// loadcore stubs - set in kactivate before booting loadcore (from utopia)
    public TPointer loadCoreImportTables;
    // total size of stubs - set in kactivate before booting loadcore (from utopia)
    public int loadCoreImportTablesSize;
    // allocated in SysMemInit (from utopia)
    public TPointer initThreadStack;
    // set in kactivate before booting loadcore (from utopia)
    public TPointer sysMemExecInfo;
    // set in kactivate before booting loadcore (from utopia)
    public TPointer loadCoreExecInfo; 
    // s32 (*CompareSubType)(u32 tag)
    public TPointer compareSubType;
    // u32 (*CompareLatestSubType)(u32 tag);
    public TPointer compareLatestSubType;
    // s32 (*SetMaskFunction)(u32 unk1, vs32 *addr)
    public TPointer setMaskFunction;
    // void (*Kprintf)(const char *fmt, ...)
    public TPointer Kprintf;
    // s32 (*GetLengthFunction)(u8 *file, u32 size, u32 *newSize)
    public TPointer getLengthFunction;
    // s32 (*PrepareGetLengthFunction)(u8 *buf, u32 size)
    public TPointer prepareGetLengthFunction;
    public TPointer userLibs[] = new TPointer[3];

	@Override
	protected void read() {
		unknown0 = read32(); // Offset 0
		numExportLibs = read32(); // Offset 4
		readPointerArray(kernelLibs); // Offset 8
		loadCoreAddr = readPointer(); // Offset 40
		numKernelLibs = read32(); // Offset 44
		unknown48 = read32(); // Offset 48
		allocPartitionMemory = readPointer(); // Offset 52
		getBlockHeadAddr = readPointer(); // Offset 56
		resizeMemoryBlock = readPointer(); // Offset 60
		loadCoreImportTables = readPointer(); // Offset 64
		loadCoreImportTablesSize = read32(); // Offset 68
		initThreadStack = readPointer(); // Offset 72
		sysMemExecInfo = readPointer(); // Offset 76
		loadCoreExecInfo = readPointer(); // Offset 80
		compareSubType = readPointer(); // Offset 84
		compareLatestSubType = readPointer(); // Offset 88
		setMaskFunction = readPointer(); // Offset 92
		Kprintf = readPointer(); // Offset 96
		getLengthFunction = readPointer(); // Offset 100
		prepareGetLengthFunction = readPointer(); // Offset 104
		readPointerArray(userLibs); // Offset 108
	}

	@Override
	protected void write() {
		write32(unknown0);
		write32(numExportLibs);
		writePointerArray(kernelLibs);
		writePointer(loadCoreAddr);
		write32(numKernelLibs);
		write32(unknown48);
		writePointer(allocPartitionMemory);
		writePointer(getBlockHeadAddr);
		writePointer(resizeMemoryBlock);
		writePointer(loadCoreImportTables);
		write32(loadCoreImportTablesSize);
		writePointer(initThreadStack);
		writePointer(sysMemExecInfo);
		writePointer(loadCoreExecInfo);
		writePointer(compareSubType);
		writePointer(compareLatestSubType);
		writePointer(setMaskFunction);
		writePointer(Kprintf);
		writePointer(getLengthFunction);
		writePointer(prepareGetLengthFunction);
		writePointerArray(userLibs);
	}

	@Override
	public int sizeof() {
		return 112 + 4 * userLibs.length;
	}
}
