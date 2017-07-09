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

import static jpcsp.HLE.HLEModuleManager.HLESyscallNid;
import static jpcsp.HLE.modules.IoFileMgrForUser.PSP_SEEK_SET;
import static jpcsp.HLE.modules.ThreadManForUser.installHLESyscall;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

import org.apache.log4j.Logger;

import jpcsp.Memory;
import jpcsp.NIDMapper;
import jpcsp.HLE.BufferInfo;
import jpcsp.HLE.CanBeNull;
import jpcsp.HLE.DebugMemory;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.Modules;
import jpcsp.HLE.PspString;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.BufferInfo.LengthInfo;
import jpcsp.HLE.BufferInfo.Usage;
import jpcsp.HLE.kernel.Managers;
import jpcsp.HLE.kernel.types.IAction;
import jpcsp.HLE.kernel.types.SceKernelThreadInfo;
import jpcsp.HLE.kernel.types.pspIoDrv;
import jpcsp.HLE.kernel.types.pspIoDrvArg;
import jpcsp.HLE.kernel.types.pspIoDrvFileArg;
import jpcsp.HLE.kernel.types.pspIoDrvFuncs;
import jpcsp.HLE.modules.SysMemUserForUser.SysMemInfo;
import jpcsp.hardware.MemoryStick;
import jpcsp.util.Utilities;

public class sceMSstor extends HLEModule {
    public static Logger log = Modules.getLogger("sceMSstor");
    private byte[] dumpBlocks;
    private boolean dumpBlocksDirty;
    private byte[] dumpIoIoctl_0x02125803;
    private long position;
    private final static int sectorSize = 512;

    private static class AfterAddDrvController implements IAction {
    	private SceKernelThreadInfo thread;
    	private int sceIoAddDrv;
    	private int storageDrvAddr;
    	private int partitionDrvAddr;

		public AfterAddDrvController(SceKernelThreadInfo thread, int sceIoAddDrv, int storageDrvAddr, int partitionDrvAddr) {
			this.thread = thread;
			this.sceIoAddDrv = sceIoAddDrv;
			this.storageDrvAddr = storageDrvAddr;
			this.partitionDrvAddr = partitionDrvAddr;
		}

		@Override
		public void execute() {
			Modules.ThreadManForUserModule.executeCallback(thread, sceIoAddDrv, new AfterAddDrvStorage(thread, sceIoAddDrv, partitionDrvAddr), false, storageDrvAddr);
		}
    }

    private static class AfterAddDrvStorage implements IAction {
    	private SceKernelThreadInfo thread;
    	private int sceIoAddDrv;
    	private int partitionDrvAddr;

		public AfterAddDrvStorage(SceKernelThreadInfo thread, int sceIoAddDrv, int partitionDrvAddr) {
			this.thread = thread;
			this.sceIoAddDrv = sceIoAddDrv;
			this.partitionDrvAddr = partitionDrvAddr;
		}

		@Override
		public void execute() {
			Modules.ThreadManForUserModule.executeCallback(thread, sceIoAddDrv, null, false, partitionDrvAddr);
		}
    }

	public void exit() {
		if (dumpBlocks != null && dumpBlocksDirty) {
			try {
				OutputStream os = new FileOutputStream("ms.block");
				os.write(dumpBlocks);
				os.close();
			} catch (FileNotFoundException e) {
			} catch (IOException e) {
			}
		}
	}

	@HLEUnimplemented
    @HLEFunction(nid = HLESyscallNid, version = 150)
    public int hleMSstorControllerIoInit(pspIoDrvArg drvArg) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = HLESyscallNid, version = 150)
    public int hleMSstorControllerIoDevctl(pspIoDrvFileArg drvFileArg, PspString devicename, int cmd, @CanBeNull @BufferInfo(lengthInfo=LengthInfo.nextParameter, usage=Usage.in) TPointer indata, int inlen, @CanBeNull @BufferInfo(lengthInfo=LengthInfo.nextParameter, usage=Usage.out) TPointer outdata, int outlen) {
    	switch (cmd) {
    		case 0x0203D802: // Set EventFlag named SceFatfsDetectMedium
    			int eventFlag = indata.getValue32();
    			// Event 0x1: memory stick inserted?
    			// Event 0x2: memory stick ejected?
    			Managers.eventFlags.sceKernelSetEventFlag(eventFlag, 0x1);
    			outdata.setValue32(0);
    			break;
    		case 0x02025801: // Check the MemoryStick's driver status
    			outdata.setValue32(4);
    			break;
    		case 0x2015807:
    			outdata.setValue32(1); // Unknown value: seems to be 0 or 1?
    			break;
    		case 0x0202580A:
    			outdata.clear(outlen);
    			break;
    		case 0x201580B:
    			// inlen == 20, outlen == 0
    			break;
            case 0x02025806: // Check if the device is inserted
                // 0 = Not inserted.
                // 1 = Inserted.
                outdata.setValue32(1);
            	break;
			default:
                log.warn(String.format("hleMSstorControllerIoDevctl 0x%08X unknown command on device '%s'", cmd, devicename));
				break;
    	}
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = HLESyscallNid, version = 150)
    public int hleMSstorStorageIoInit(pspIoDrvArg drvArg) {
    	return 0;
    }

    private void writeSector(byte[] sector, int offset, int sectorNumber) {
    	if (dumpBlocks != null) {
    		System.arraycopy(sector, offset, dumpBlocks, sectorNumber * sectorSize, sectorSize);
    	}
    }

    private int getFatSectors(int totalSectors, int sectorsPerCluster) {
    	int totalClusters = (totalSectors / sectorsPerCluster) + 1;
    	int fatSectors = (totalClusters / (sectorSize / 4)) + 1;

    	return fatSectors;
    }

    private void createBootSector(int sectorNumber, int reservedSectors, int totalSectors, int fsInfoSector, int sectorsPerCluster, int numOfFats) {
    	byte[] sector = new byte[sectorSize];

    	// Jump Code
    	sector[0] = (byte) 0xEB;
    	sector[1] = (byte) 0x58;
    	sector[2] = (byte) 0x90;
    	// OEM Name
    	sector[3] = (byte) ' ';
    	sector[4] = (byte) ' ';
    	sector[5] = (byte) ' ';
    	sector[6] = (byte) ' ';
    	sector[7] = (byte) ' ';
    	sector[8] = (byte) ' ';
    	sector[9] = (byte) ' ';
    	sector[10] = (byte) ' ';

    	// Bytes per sector
    	sector[11] = (byte) (sectorSize >> 0);
    	sector[12] = (byte) (sectorSize >> 8);

    	// Sectors per cluster
    	sector[13] = (byte) sectorsPerCluster;

    	// Reserved sectors
    	sector[14] = (byte) (reservedSectors >> 0);
    	sector[15] = (byte) (reservedSectors >> 8);

    	// Number of FATS
    	sector[16] = (byte) numOfFats;

    	// Max entries in root dir (unused by FAT32)
    	sector[17] = (byte) 0;
    	sector[18] = (byte) 0;

    	// Total sectors (use FAT32 count instead)
    	sector[19] = (byte) 0;
    	sector[20] = (byte) 0;

    	// Media type
    	sector[21] = (byte) 0xF8;

    	// Count of sectors used by the FAT table (unused by FAT32)
    	sector[22] = (byte) 0;
    	sector[23] = (byte) 0;

    	// Sectors per track (default)
    	sector[24] = (byte) 0x3F;
    	sector[25] = (byte) 0;

    	// Heads (default)
    	sector[26] = (byte) 0xFF;
    	sector[27] = (byte) 0;

    	// Hidden sectors
    	sector[28] = (byte) 0;
    	sector[29] = (byte) 0;
    	sector[30] = (byte) 0;
    	sector[31] = (byte) 0;

    	// Total sectors
    	sector[32] = (byte) (totalSectors >> 0);
    	sector[33] = (byte) (totalSectors >> 8);
    	sector[34] = (byte) (totalSectors >> 16);
    	sector[35] = (byte) (totalSectors >> 24);

    	// BPB_FATz32
    	final int fatSectors = getFatSectors(totalSectors, sectorsPerCluster);
    	sector[36] = (byte) (fatSectors >> 0);
    	sector[37] = (byte) (fatSectors >> 8);
    	sector[38] = (byte) (fatSectors >> 16);
    	sector[39] = (byte) (fatSectors >> 24);

    	// BPB_ExtFlags
    	sector[40] = (byte) 0;
    	sector[41] = (byte) 0;

    	// BPB_FSVer
    	sector[42] = (byte) 0;
    	sector[43] = (byte) 0;

    	// BPB_RootClus
    	final int rootDirFirstCluster = 2;
    	sector[44] = (byte) (rootDirFirstCluster >> 0);
    	sector[45] = (byte) (rootDirFirstCluster >> 8);
    	sector[46] = (byte) (rootDirFirstCluster >> 16);
    	sector[47] = (byte) (rootDirFirstCluster >> 24);

    	// BPB_FSInfo
    	sector[48] = (byte) (fsInfoSector >> 0);
    	sector[49] = (byte) (fsInfoSector >> 8);

    	// BPB_BkBootSec
    	sector[50] = (byte) 6;
    	sector[51] = (byte) 0;

    	// Drive number
    	sector[64] = (byte) 0;

    	// Boot signature
    	sector[66] = (byte) 0x29;

    	// Volume ID
    	sector[67] = (byte) 0x00;
    	sector[68] = (byte) 0x00;
    	sector[69] = (byte) 0x00;
    	sector[70] = (byte) 0x00;

    	// Volume name
    	sector[71] = (byte) ' ';
    	sector[72] = (byte) ' ';
    	sector[73] = (byte) ' ';
    	sector[74] = (byte) ' ';
    	sector[75] = (byte) ' ';
    	sector[76] = (byte) ' ';
    	sector[77] = (byte) ' ';
    	sector[78] = (byte) ' ';
    	sector[79] = (byte) ' ';
    	sector[80] = (byte) ' ';
    	sector[81] = (byte) ' ';

    	// File sys type
    	sector[82] = (byte) 'F';
    	sector[83] = (byte) 'A';
    	sector[84] = (byte) 'T';
    	sector[85] = (byte) '3';
    	sector[86] = (byte) '2';
    	sector[87] = (byte) ' ';
    	sector[88] = (byte) ' ';
    	sector[89] = (byte) ' ';

    	// Signature
    	sector[510] = (byte) 0x55;
    	sector[511] = (byte) 0xAA;

    	writeSector(sector, 0, sectorNumber);
    }

    private void createFsInfoSector(int sectorNumber) {
    	byte[] sector = new byte[sectorSize];

    	// FSI_LeadSig
    	sector[0] = (byte) 0x52;
    	sector[1] = (byte) 0x52;
    	sector[2] = (byte) 0x61;
    	sector[3] = (byte) 0x41;

    	// FSI_StrucSig
    	sector[484] = (byte) 0x72;
    	sector[485] = (byte) 0x72;
    	sector[486] = (byte) 0x41;
    	sector[487] = (byte) 0x61;

    	// FSI_Free_Count
    	sector[488] = (byte) 0xFF;
    	sector[489] = (byte) 0xFF;
    	sector[490] = (byte) 0xFF;
    	sector[491] = (byte) 0xFF;

    	// FSI_Nxt_Free
    	sector[492] = (byte) 0xFF;
    	sector[493] = (byte) 0xFF;
    	sector[494] = (byte) 0xFF;
    	sector[495] = (byte) 0xFF;

    	// Signature
    	sector[510] = (byte) 0x55;
    	sector[511] = (byte) 0xAA;

    	writeSector(sector, 0, sectorNumber);
    }

    private void eraseSectors(int sectorNumber, int count) {
    	byte[] sector = new byte[sectorSize];

    	for (int i = 0; i < count; i++) {
    		writeSector(sector, 0, sectorNumber + i);
    	}
    }

    private void eraseFat(int sectorNumber, int totalSectors, int sectorsPerCluster, int numOfFats) {
    	byte[] sector = new byte[sectorSize];

    	// Initialize default allocate / reserved clusters
    	sector[0] = (byte) 0xF8;
    	sector[1] = (byte) 0xFF;
    	sector[2] = (byte) 0xFF;
    	sector[3] = (byte) 0x0F;

    	sector[4] = (byte) 0xFF;
    	sector[5] = (byte) 0xFF;
    	sector[6] = (byte) 0xFF;
    	sector[7] = (byte) 0xFF;

    	sector[8] = (byte) 0xFF;
    	sector[9] = (byte) 0xFF;
    	sector[10] = (byte) 0xFF;
    	sector[11] = (byte) 0x0F;

    	writeSector(sector, 0, sectorNumber);

    	// Zero remaining FAT sectors
    	eraseSectors(sectorNumber + 1, getFatSectors(totalSectors, sectorsPerCluster) * numOfFats - 1);
    }

    private void formatStorage() {
    	if (dumpBlocks != null) {
    		Arrays.fill(dumpBlocks, (byte) 0);
    		dumpBlocksDirty = true;
    	}

		final int totalSectors = (int) (MemoryStick.getFreeSize() / sectorSize);
		final int reservedSectors = 32;
		final int bootSector = 0;
    	final int fsInfoSector = bootSector + 1;
    	final int sectorsPerCluster = 64;
    	final int numOfFats = 2;

    	// Sector 0: Boot sector
		createBootSector(bootSector, reservedSectors, totalSectors, fsInfoSector, sectorsPerCluster, numOfFats);

		// Initialize FSInfo sector
		createFsInfoSector(fsInfoSector);

		// Initialize FAT sectors
		eraseFat(bootSector + reservedSectors, totalSectors, sectorsPerCluster, numOfFats);

		// Erase Root directory
		eraseSectors(bootSector + reservedSectors + getFatSectors(totalSectors, sectorsPerCluster) * numOfFats, sectorsPerCluster);
    }

    @HLEUnimplemented
    @HLEFunction(nid = HLESyscallNid, version = 150)
    public int hleMSstorStorageIoDevctl(pspIoDrvFileArg drvFileArg, PspString devicename, int cmd, @CanBeNull @BufferInfo(lengthInfo=LengthInfo.nextParameter, usage=Usage.in) TPointer indata, int inlen, @CanBeNull @BufferInfo(lengthInfo=LengthInfo.nextParameter, usage=Usage.out) TPointer outdata, int outlen) {
    	switch (cmd) {
    		case 0x02125802:
    			outdata.setValue32(0); // ???
    			break;
    		case 0x0211D814:
    			// inlen == 4, outlen == 0
    			break;
    		case 0x0210D816: // Format Memory Stick
    			// inlen == 0, outlen == 0
    			formatStorage();
    			break;
            case 0x02025806: // Check if the device is inserted
                // 0 = Not inserted.
                // 1 = Inserted.
                outdata.setValue32(1);
            	break;
			default:
                log.warn(String.format("hleMSstorStorageIoDevctl 0x%08X unknown command on device '%s'", cmd, devicename));
				break;
    	}
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = HLESyscallNid, version = 150)
    public int hleMSstorStorageIoOpen(pspIoDrvFileArg drvFileArg, PspString fileName, int flags, int mode) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = HLESyscallNid, version = 150)
    public int hleMSstorStorageIoIoctl(pspIoDrvFileArg drvFileArg, int cmd, @CanBeNull @BufferInfo(lengthInfo=LengthInfo.nextParameter, usage=Usage.in) TPointer indata, int inlen, @CanBeNull @DebugMemory @BufferInfo(lengthInfo=LengthInfo.nextParameter, usage=Usage.out) TPointer outdata, int outlen) {
    	switch (cmd) {
			case 0x02125008:
				outdata.setValue32(1); // 0 or != 0
				break;
    		case 0x02125009:
    			outdata.setValue32(0); // 0 or != 0
    			break;
			default:
                log.warn(String.format("hleMSstorStorageIoIoctl 0x%08X unknown command", cmd));
				break;
    	}
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = HLESyscallNid, version = 150)
    public int hleMSstorStorageIoClose(pspIoDrvFileArg drvFileArg) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = HLESyscallNid, version = 150)
    public int hleMSstorPartitionIoInit(pspIoDrvArg drvArg) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = HLESyscallNid, version = 150)
    public int hleMSstorPartitionIoDevctl(pspIoDrvFileArg drvFileArg, PspString devicename, int cmd, @CanBeNull @BufferInfo(lengthInfo=LengthInfo.nextParameter, usage=Usage.in) TPointer indata, int inlen, @CanBeNull @BufferInfo(lengthInfo=LengthInfo.nextParameter, usage=Usage.out) TPointer outdata, int outlen) {
    	switch (cmd) {
    		case 0x02125802:
    			outdata.setValue32(0); // ???
    			break;
			default:
                log.warn(String.format("hleMSstorPartitionIoDevctl 0x%08X unknown command on device '%s'", cmd, devicename));
				break;
    	}
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = HLESyscallNid, version = 150)
    public int hleMSstorPartitionIoIoctl(pspIoDrvFileArg drvFileArg, int cmd, @CanBeNull @BufferInfo(lengthInfo=LengthInfo.nextParameter, usage=Usage.in) TPointer indata, int inlen, @CanBeNull @DebugMemory @BufferInfo(lengthInfo=LengthInfo.nextParameter, usage=Usage.out) TPointer outdata, int outlen) {
    	switch (cmd) {
    		case 0x02125001: // Mounted?
    			outdata.setValue32(1); // When returning 0, ERROR_ERRNO_DEVICE_NOT_FOUND is raised
    			break;
    		case 0x02125803:
    			outdata.clear(outlen);
    			if (dumpIoIoctl_0x02125803 != null) {
    				Utilities.writeBytes(outdata.getAddress(), outlen, dumpIoIoctl_0x02125803, 0);
    			} else {
    				outdata.setValue8(0, (byte) 0x02);
    			}
    			break;
    		case 0x02125008:
    			outdata.setValue32(1); // 0 or != 0
    			break;
    		case 0x02125009:
    			outdata.setValue32(0); // 0 or != 0
    			break;
			default:
                log.warn(String.format("hleMSstorPartitionIoIoctl 0x%08X unknown command", cmd));
				break;
    	}
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = HLESyscallNid, version = 150)
    public int hleMSstorPartitionIoOpen(pspIoDrvFileArg drvFileArg, PspString fileName, int flags, int mode) {
    	position = 0L;

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = HLESyscallNid, version = 150)
    public int hleMSstorPartitionIoClose(pspIoDrvFileArg drvFileArg) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = HLESyscallNid, version = 150)
    public long hleMSstorPartitionIoLseek(pspIoDrvFileArg drvFileArg, long offset, int whence) {
        switch (whence) {
        	case PSP_SEEK_SET:
            	position = offset;
            	break;
        	default:
        		log.warn(String.format("hleMSstorPartitionIoLseek unimplemented whence=0x%X", whence));
        		break;
        }

    	return position;
    }

    @HLEUnimplemented
    @HLEFunction(nid = HLESyscallNid, version = 150)
    public int hleMSstorPartitionIoRead(pspIoDrvFileArg drvFileArg, @BufferInfo(lengthInfo=LengthInfo.returnValue, usage=Usage.out) TPointer data, int len) {
    	data.clear(len);

    	if (dumpBlocks != null) {
    		Utilities.writeBytes(data.getAddress(), len, dumpBlocks, (int) position);
    	}

    	return len;
    }

    @HLEUnimplemented
    @HLEFunction(nid = HLESyscallNid, version = 150)
    public int hleMSstorPartitionIoWrite(pspIoDrvFileArg drvFileArg, @BufferInfo(lengthInfo=LengthInfo.nextParameter, usage=Usage.in) TPointer data, int len) {
    	if (dumpBlocks != null) {
    		Utilities.readBytes(data.getAddress(), len, dumpBlocks, (int) position);
    		dumpBlocksDirty = true;
    	}

    	return len;
    }

    private static byte[] readBytes(String fileName) {
    	byte[] bytes = null;
    	try {
    		File file = new File(fileName);
			InputStream is = new FileInputStream(file);
			bytes = new byte[(int) file.length()];
			is.read(bytes);
			is.close();
		} catch (FileNotFoundException e) {
		} catch (IOException e) {
		}

    	return bytes;
    }

    private void installIoFunctions(pspIoDrvFuncs controllerFuncs, pspIoDrvFuncs storageFuncs, pspIoDrvFuncs partitionFuncs) {
    	final int sizeIoFunctionStub = 12;
    	final int numberIoFunctions = 15;
		SysMemInfo memInfo = Modules.SysMemUserForUserModule.malloc(SysMemUserForUser.KERNEL_PARTITION_ID, "sceMSstor-IoFunctions", SysMemUserForUser.PSP_SMEM_Low, sizeIoFunctionStub * numberIoFunctions, 0);
		int addr = memInfo.addr;

		controllerFuncs.ioInit = addr;
		addr += sizeIoFunctionStub;
		controllerFuncs.ioDevctl = addr;
		addr += sizeIoFunctionStub;
		installHLESyscall(controllerFuncs.ioInit, this, "hleMSstorControllerIoInit");
		installHLESyscall(controllerFuncs.ioDevctl, this, "hleMSstorControllerIoDevctl");

		storageFuncs.ioInit = addr;
		addr += sizeIoFunctionStub;
		storageFuncs.ioDevctl = addr;
		addr += sizeIoFunctionStub;
		storageFuncs.ioOpen = addr;
		addr += sizeIoFunctionStub;
		storageFuncs.ioIoctl = addr;
		addr += sizeIoFunctionStub;
		storageFuncs.ioClose = addr;
		addr += sizeIoFunctionStub;
		installHLESyscall(storageFuncs.ioInit, this, "hleMSstorStorageIoInit");
		installHLESyscall(storageFuncs.ioDevctl, this, "hleMSstorStorageIoDevctl");
		installHLESyscall(storageFuncs.ioOpen, this, "hleMSstorStorageIoOpen");
		installHLESyscall(storageFuncs.ioIoctl, this, "hleMSstorStorageIoIoctl");
		installHLESyscall(storageFuncs.ioClose, this, "hleMSstorStorageIoClose");

		partitionFuncs.ioInit = addr;
		addr += sizeIoFunctionStub;
		partitionFuncs.ioDevctl = addr;
		addr += sizeIoFunctionStub;
		partitionFuncs.ioOpen = addr;
		addr += sizeIoFunctionStub;
		partitionFuncs.ioClose = addr;
		addr += sizeIoFunctionStub;
		partitionFuncs.ioIoctl = addr;
		addr += sizeIoFunctionStub;
		partitionFuncs.ioLseek = addr;
		addr += sizeIoFunctionStub;
		partitionFuncs.ioRead = addr;
		addr += sizeIoFunctionStub;
		partitionFuncs.ioWrite = addr;
		addr += sizeIoFunctionStub;
		installHLESyscall(partitionFuncs.ioInit, this, "hleMSstorPartitionIoInit");
		installHLESyscall(partitionFuncs.ioDevctl, this, "hleMSstorPartitionIoDevctl");
		installHLESyscall(partitionFuncs.ioOpen, this, "hleMSstorPartitionIoOpen");
		installHLESyscall(partitionFuncs.ioClose, this, "hleMSstorPartitionIoClose");
		installHLESyscall(partitionFuncs.ioIoctl, this, "hleMSstorPartitionIoIoctl");
		installHLESyscall(partitionFuncs.ioLseek, this, "hleMSstorPartitionIoLseek");
		installHLESyscall(partitionFuncs.ioRead, this, "hleMSstorPartitionIoRead");
		installHLESyscall(partitionFuncs.ioWrite, this, "hleMSstorPartitionIoWrite");
    }

    public void installDrivers() {
		Memory mem = Memory.getInstance();

		dumpBlocks = readBytes("ms.block");
		dumpBlocksDirty = false;
		dumpIoIoctl_0x02125803 = readBytes("ms.ioctl.0x02125803");

		pspIoDrv controllerDrv = new pspIoDrv();
		pspIoDrvFuncs controllerFuncs = new pspIoDrvFuncs();
		String controllerName = "mscmhc";
		String controllerDescription = "MS host controller";

		pspIoDrv storageDrv = new pspIoDrv();
		pspIoDrvFuncs storageFuncs = new pspIoDrvFuncs();
		String storageName = "msstor";
		String storageDescription = "MSstor whole dev";

		pspIoDrv partitionDrv = new pspIoDrv();
		pspIoDrvFuncs partitionFuncs = new pspIoDrvFuncs();
		String partitionName = "msstor0p";
		String partitionDescription = "MSstor partition #1";

		int length = 0;
		length += controllerDrv.sizeof() + controllerFuncs.sizeof() + controllerName.length() + 1 + controllerDescription.length() + 1;
		length += storageDrv.sizeof() + storageFuncs.sizeof() + storageName.length() + 1 + storageDescription.length() + 1;
		length += partitionDrv.sizeof() + partitionFuncs.sizeof() + partitionName.length() + 1 + partitionDescription.length() + 1;
		SysMemInfo memInfo = Modules.SysMemUserForUserModule.malloc(SysMemUserForUser.KERNEL_PARTITION_ID, "sceMSstor-mscmhc", SysMemUserForUser.PSP_SMEM_Low, length, 0);

		int controllerDrvAddr = memInfo.addr;
		int controllerFuncsAddr = controllerDrvAddr + controllerDrv.sizeof();
		int storageDrvAddr = controllerFuncsAddr + controllerFuncs.sizeof();
		int storageFuncsAddr = storageDrvAddr + storageDrv.sizeof();
		int partitionDrvAddr = storageFuncsAddr + controllerFuncs.sizeof();
		int partitionFuncsAddr = partitionDrvAddr + partitionDrv.sizeof();
		int controllerNameAddr = partitionFuncsAddr + partitionFuncs.sizeof();
		int controllerDescriptionAddr = controllerNameAddr + controllerName.length() + 1;
		int storageNameAddr = controllerDescriptionAddr + controllerDescription.length() + 1;
		int storageDescriptionAddr = storageNameAddr + storageName.length() + 1;
		int partitionNameAddr = storageDescriptionAddr + storageDescription.length() + 1;
		int partitionDescriptionAddr = partitionNameAddr + partitionName.length() + 1;

		installIoFunctions(controllerFuncs, storageFuncs, partitionFuncs);

		Utilities.writeStringZ(mem, controllerNameAddr, controllerName);
		Utilities.writeStringZ(mem, controllerDescriptionAddr, controllerDescription);
		controllerDrv.nameAddr = controllerNameAddr;
		controllerDrv.name = controllerName;
		controllerDrv.devType = 1;
		controllerDrv.unknown = 0;
		controllerDrv.descriptionAddr = controllerDescriptionAddr;
		controllerDrv.description = controllerDescription;
		controllerDrv.funcsAddr = controllerFuncsAddr;
		controllerDrv.ioDrvFuncs = controllerFuncs;
		controllerDrv.write(mem, controllerDrvAddr);

		Utilities.writeStringZ(mem, storageNameAddr, storageName);
		Utilities.writeStringZ(mem, storageDescriptionAddr, storageDescription);
		storageDrv.nameAddr = storageNameAddr;
		storageDrv.name = storageName;
		storageDrv.devType = 1;
		storageDrv.unknown = 0;
		storageDrv.descriptionAddr = storageDescriptionAddr;
		storageDrv.description = storageDescription;
		storageDrv.funcsAddr = storageFuncsAddr;
		storageDrv.ioDrvFuncs = storageFuncs;
		storageDrv.write(mem, storageDrvAddr);

		Utilities.writeStringZ(mem, partitionNameAddr, partitionName);
		Utilities.writeStringZ(mem, partitionDescriptionAddr, partitionDescription);
		partitionDrv.nameAddr = partitionNameAddr;
		partitionDrv.name = partitionName;
		partitionDrv.devType = 1;
		partitionDrv.unknown = 0;
		partitionDrv.descriptionAddr = partitionDescriptionAddr;
		partitionDrv.description = partitionDescription;
		partitionDrv.funcsAddr = partitionFuncsAddr;
		partitionDrv.ioDrvFuncs = partitionFuncs;
		partitionDrv.write(mem, partitionDrvAddr);

		int sceIoAddDrv = NIDMapper.getInstance().getAddressByName("sceIoAddDrv");
		if (sceIoAddDrv != 0) {
			SceKernelThreadInfo thread = Modules.ThreadManForUserModule.getCurrentThread();
			Modules.ThreadManForUserModule.executeCallback(thread, sceIoAddDrv, new AfterAddDrvController(thread, sceIoAddDrv, storageDrvAddr, partitionDrvAddr), false, controllerDrvAddr);
		}
	}
}
