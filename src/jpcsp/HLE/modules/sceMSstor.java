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
import java.io.IOException;
import java.io.InputStream;

import org.apache.log4j.Logger;

import jpcsp.Memory;
import jpcsp.NIDMapper;
import jpcsp.HLE.BufferInfo;
import jpcsp.HLE.CanBeNull;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.Modules;
import jpcsp.HLE.PspString;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.VFS.IVirtualFileSystem;
import jpcsp.HLE.VFS.fat.Fat32VirtualFile;
import jpcsp.HLE.VFS.local.LocalVirtualFileSystem;
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
import jpcsp.util.Utilities;

public class sceMSstor extends HLEModule {
    public static Logger log = Modules.getLogger("sceMSstor");
    private byte[] dumpIoIoctl_0x02125803;
    private long position;
    private Fat32VirtualFile vFile;
    private Fat32ScanThread scanThread;

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

    private static class Fat32ScanThread extends Thread {
    	private Fat32VirtualFile vFile;
    	private boolean completed;

		public Fat32ScanThread(Fat32VirtualFile vFile) {
			this.vFile = vFile;
			completed = false;
		}

		@Override
		public void run() {
			vFile.scan();
			completed = true;
		}

		public boolean isCompleted() {
			return completed;
		}

		public void waitForCompletion() {
			while (!isCompleted()) {
				Utilities.sleep(1, 0);
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
    			log.warn(String.format("A FORMAT of the Memory Stick was requested, ignoring the request"));
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
    public int hleMSstorStorageIoIoctl(pspIoDrvFileArg drvFileArg, int cmd, @CanBeNull @BufferInfo(lengthInfo=LengthInfo.nextParameter, usage=Usage.in) TPointer indata, int inlen, @CanBeNull @BufferInfo(lengthInfo=LengthInfo.nextParameter, usage=Usage.out) TPointer outdata, int outlen) {
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
    public int hleMSstorPartitionIoIoctl(pspIoDrvFileArg drvFileArg, int cmd, @CanBeNull @BufferInfo(lengthInfo=LengthInfo.nextParameter, usage=Usage.in) TPointer indata, int inlen, @CanBeNull @BufferInfo(lengthInfo=LengthInfo.nextParameter, usage=Usage.out) TPointer outdata, int outlen) {
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

        if (vFile != null) {
        	position = vFile.ioLseek(position);
        }

        return position;
    }

    @HLEUnimplemented
    @HLEFunction(nid = HLESyscallNid, version = 150)
    public int hleMSstorPartitionIoRead(pspIoDrvFileArg drvFileArg, @BufferInfo(lengthInfo=LengthInfo.returnValue, usage=Usage.out) TPointer data, int len) {
    	data.clear(len);

    	if (vFile != null) {
    		scanThread.waitForCompletion();
    		len = vFile.ioRead(data, len);
    	}

    	return len;
    }

    @HLEUnimplemented
    @HLEFunction(nid = HLESyscallNid, version = 150)
    public int hleMSstorPartitionIoWrite(pspIoDrvFileArg drvFileArg, @BufferInfo(lengthInfo=LengthInfo.nextParameter, usage=Usage.in) TPointer data, int len) {
    	if (vFile != null) {
    		scanThread.waitForCompletion();
    		len = vFile.ioWrite(data, len);
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

		dumpIoIoctl_0x02125803 = readBytes("ms.ioctl.0x02125803");

		IVirtualFileSystem vfs = new LocalVirtualFileSystem("ms0/", true);
		vFile = new Fat32VirtualFile(vfs);
		if (log.isDebugEnabled()) {
			log.debug(String.format("installDrivers vFile=%s", vFile));
		}

		scanThread = new Fat32ScanThread(vFile);
		scanThread.setName("Fat32VirtualFile Scan Thread");
		scanThread.setDaemon(true);
		scanThread.start();

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
