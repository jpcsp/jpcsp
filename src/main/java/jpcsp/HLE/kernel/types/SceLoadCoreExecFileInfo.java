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
 * This structure represents executable file information used to load the file.
 */
public class SceLoadCoreExecFileInfo extends pspAbstractMemoryMappedStructure {
	/** The maximum number of segments a module can have. */
	public static final int SCE_KERNEL_MAX_MODULE_SEGMENT = 4;
	public int unknown0; //0
	/** The mode attribute of the executable file. One of ::SceExecFileModeAttr. */
	public int modeAttribute; //4
    /** The API type. */
    public int apiType; //8
    /** Unknown. */
    public int unknown12; //12
    /** The size of the executable, including the ~PSP header. */
    public int execSize; //16
    /** The maximum size needed for the decompression. */
    public int maxAllocSize; //20
    /** The memory ID of the decompression buffer. */
    public int decompressionMemId; //24
    /** Pointer to the compressed module data. */
    public TPointer fileBase; //28
    /** Indicates the ELF type of the executable. One of ::SceExecFileElfType. */
    public int elfType; //32 
    /** The start address of the TEXT segment of the executable in memory. */
    public TPointer topAddr; //36
    /**
     * The entry address of the module. It is the offset from the start of the TEXT segment to the 
     * program's entry point. 
     */
    public int entryAddr; //40
    /** Unknown. */
    public int unknown44;
    /** 
     * The size of the largest module segment. Should normally be "textSize", but technically can 
     * be any other segment. 
     */
    public int largestSegSize; //48
    /** The size of the TEXT segment. */
    public int textSize; //52
    /** The size of the DATA segment. */
    public int dataSize; //56
    /** The size of the BSS segment. */
    public int bssSize; //60
    /** The memory partition of the executable. */
    public int partitionId; //64
    /** 
     * Indicates whether the executable is a kernel module or not. Set to 1 for kernel module, 
     * 0 for user module. 
     */
    public int isKernelMod; //68
    /** 
     * Indicates whether the executable is decrypted or not. Set to 1 if it is successfully decrypted, 
     * 0 for encrypted. 
     */
    public int isDecrypted; //72
    /** The offset from the start address of the TEXT segment to the SceModuleInfo section. */
    public int moduleInfoOffset; //76
    /** The pointer to the module's SceModuleInfo section. */
    public TPointer moduleInfo; //80
    /** Indicates whether the module is compressed or not. Set to 1 if it is compressed, otherwise 0.*/
    public int isCompressed; //84
    /** The module's attributes. One or more of ::SceModuleAttribute and ::SceModulePrivilegeLevel. */
    public int modInfoAttribute; //88
    /** The attributes of the executable file. One of ::SceExecFileAttr. */
    public int execAttribute; //90
    /** The size of the decompressed module, including its headers. */
    public int decSize; //92
    /** Indicates whether the module is decompressed or not. Set to 1 for decompressed, otherwise 0. */
    public int isDecompressed; //96
    /** 
     * Indicates whether the module was signChecked or not. Set to 1 for signChecked, otherwise 0. 
     * A signed module has a "mangled" executable header, in other words, the "~PSP" signature can't 
     * be seen. 
     */
    public int isSignChecked; //100
    /** Unknown. */
    public int unknown104;
    /** The size of the GZIP compression overlap. */
    public int overlapSize; //108
    /** Pointer to the first resident library entry table of the module. */
    public TPointer exportsInfo; //112
    /** The size of all resident library entry tables of the module. */
    public int exportsSize; //116
    /** Pointer to the first stub library entry table of the module. */
    public TPointer importsInfo; //120
    /** The size of all stub library entry tables of the module. */
    public int importsSize; //124
    /** Pointer to the string table section. */
    public TPointer strtabOffset; //128
    /** The number of segments in the executable. */
    public int numSegments; //132
    /** Reserved. */
    public final byte[] padding = new byte[3]; //133
    /** An array containing the start address of each segment. */
    public final TPointer[] segmentAddr = new TPointer[SCE_KERNEL_MAX_MODULE_SEGMENT]; //136
    /** An array containing the size of each segment. */
    public final int[] segmentSize = new int[SCE_KERNEL_MAX_MODULE_SEGMENT]; //152
    /** The ID of the ELF memory block containing the TEXT, DATA and BSS segment. */
    public int memBlockId; //168
    /** An array containing the alignment information of each segment. */
    public final int[] segmentAlign = new int[SCE_KERNEL_MAX_MODULE_SEGMENT]; //172
    /** The largest value of the segmentAlign array. */
    public int maxSegAlign; //188

	@Override
	protected void read() {
		unknown0 = read32();
		modeAttribute = read32();
	    apiType = read32();
	    unknown12 = read32();
	    execSize = read32();
	    maxAllocSize = read32();
	    decompressionMemId = read32();
	    fileBase = readPointer();
	    elfType = read32();
	    topAddr = readPointer();
	    entryAddr = read32();
	    unknown44 = read32();
	    largestSegSize = read32();
	    textSize = read32();
	    dataSize = read32();
	    bssSize = read32();
	    partitionId = read32();
	    isKernelMod = read32();
	    isDecrypted = read32();
	    moduleInfoOffset = read32();
	    moduleInfo = readPointer();
	    isCompressed = read32();
	    modInfoAttribute = read16();
	    execAttribute = read16();
	    decSize = read32();
	    isDecompressed = read32();
	    isSignChecked = read32();
	    unknown104 = read32();
	    overlapSize = read32();
	    exportsInfo = readPointer();
	    exportsSize = read32();
	    importsInfo = readPointer();
	    importsSize = read32();
	    strtabOffset = readPointer();
	    numSegments = read8();
	    read8Array(padding);
	    readPointerArray(segmentAddr);
	    read32Array(segmentSize);
	    memBlockId = read32();
	    read32Array(segmentAlign);
	    maxSegAlign = read32();
	}

	@Override
	protected void write() {
		write32(unknown0);
		write32(modeAttribute);
	    write32(apiType);
	    write32(unknown12);
	    write32(execSize);
	    write32(maxAllocSize);
	    write32(decompressionMemId);
	    writePointer(fileBase);
	    write32(elfType);
	    writePointer(topAddr);
	    write32(entryAddr);
	    write32(unknown44);
	    write32(largestSegSize);
	    write32(textSize);
	    write32(dataSize);
	    write32(bssSize);
	    write32(partitionId);
	    write32(isKernelMod);
	    write32(isDecrypted);
	    write32(moduleInfoOffset);
	    writePointer(moduleInfo);
	    write32(isCompressed);
	    write16((short) modInfoAttribute);
	    write16((short) execAttribute);
	    write32(decSize);
	    write32(isDecompressed);
	    write32(isSignChecked);
	    write32(unknown104);
	    write32(overlapSize);
	    writePointer(exportsInfo);
	    write32(exportsSize);
	    writePointer(importsInfo);
	    write32(importsSize);
	    writePointer(strtabOffset);
	    write8((byte) numSegments);
	    write8Array(padding);
	    writePointerArray(segmentAddr);
	    write32Array(segmentSize);
	    write32(memBlockId);
	    write32Array(segmentAlign);
	    write32(maxSegAlign);
	}

	@Override
	public int sizeof() {
		return 192;
	}
}
