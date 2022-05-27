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

import static jpcsp.Allegrex.compiler.RuntimeContext.setLog4jMDC;
import static jpcsp.HLE.HLEModuleManager.HLESyscallNid;
import static jpcsp.HLE.modules.IoFileMgrForUser.PSP_SEEK_SET;
import static jpcsp.memory.mmio.MMIOHandlerBaseMemoryStick.PAGE_SIZE;

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
import jpcsp.HLE.TPointerFunction;
import jpcsp.HLE.VFS.IVirtualFile;
import jpcsp.HLE.VFS.IVirtualFileSystem;
import jpcsp.HLE.VFS.WriteCacheVirtualFile;
import jpcsp.HLE.VFS.fat.Fat32VirtualFile;
import jpcsp.HLE.VFS.fat.FatVirtualFileSystem;
import jpcsp.HLE.VFS.local.LocalVirtualFile;
import jpcsp.HLE.VFS.local.LocalVirtualFileSystem;
import jpcsp.HLE.VFS.synchronize.SynchronizeVirtualFileSystems;
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
import jpcsp.filesystems.SeekableRandomFile;
import jpcsp.hardware.MemoryStick;
import jpcsp.memory.ByteArrayMemory;
import jpcsp.settings.Settings;
import jpcsp.state.IState;
import jpcsp.state.StateInputStream;
import jpcsp.state.StateOutputStream;
import jpcsp.util.HLEUtilities;
import jpcsp.util.Utilities;

public class sceMSstor extends HLEModule {
    public static Logger log = Modules.getLogger("sceMSstor");
	private static final int STATE_VERSION = 0;
    private byte[] dumpIoIoctl_0x02125803;
    private long position;
    private IVirtualFile vFile;
	private IVirtualFile vFileIpl;
    private Fat32ScanThread scanThread;
    private final Object writeLock = new Object();
    private SynchronizeVirtualFileSystems sync;
    private int FIRST_PAGE_LBA;
    private int NUMBER_OF_PAGES;

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
			setLog4jMDC();
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

    @Override
    public void read(StateInputStream stream) throws IOException {
    	stream.readVersion(STATE_VERSION);
    	dumpIoIoctl_0x02125803 = stream.readBytesWithLength();
    	position = stream.readLong();
		FIRST_PAGE_LBA = stream.readInt();
		NUMBER_OF_PAGES = stream.readInt();
    	boolean vFilePresent = stream.readBoolean();
    	if (vFilePresent) {
    		openFile();
    		((IState) vFile).read(stream);
    		sync.read(stream);
    	}

    	super.read(stream);
    }

    @Override
    public void write(StateOutputStream stream) throws IOException {
    	stream.writeVersion(STATE_VERSION);
    	stream.writeBytesWithLength(dumpIoIoctl_0x02125803);
    	stream.writeLong(position);
		stream.writeInt(FIRST_PAGE_LBA);
		stream.writeInt(NUMBER_OF_PAGES);

    	if (vFile != null) {
    		flush();

    		stream.writeBoolean(true);
    		((IState) vFile).write(stream);
    		sync.write(stream);
    	} else {
    		stream.writeBoolean(false);
    	}

    	super.write(stream);
    }

    private void flush() {
    	if (sync != null) {
    		sync.synchronize();
    	}
    }

    public void reset() {
		// Flush any pending writes before doing the reset
    	flush();

    	if (sync != null) {
    		sync.exit();
        	sync = null;
    	}

    	if (vFile != null) {
    		vFile.ioClose();
    		vFile = null;
    	}

    	if (vFileIpl != null) {
    		vFileIpl.ioClose();
    		vFileIpl = null;
    	}

    	scanThread = null;

    	hleInit();
    }

	private void getMasterBootRecord(byte[] buffer, int offset) {
		TPointer pageBufferPointer = new ByteArrayMemory(buffer, offset).getPointer();

		// See description of MBR at
		// https://en.wikipedia.org/wiki/Master_boot_record

		// First partition entry
		TPointer partitionPointer = new TPointer(pageBufferPointer, 446);
		// Active partition
		partitionPointer.setValue8(0, (byte) 0x80);
    	// CHS address of first absolute sector in partition (not used by the PSP)
		partitionPointer.setValue8(1, (byte) 0x00);
		partitionPointer.setValue8(2, (byte) 0x00);
		partitionPointer.setValue8(3, (byte) 0x00);
		// Partition type: FAT32 with LBA
		partitionPointer.setValue8(4, (byte) 0x0C);
    	// CHS address of last absolute sector in partition (not used by the PSP)
		partitionPointer.setValue8(5, (byte) 0x00);
		partitionPointer.setValue8(6, (byte) 0x00);
		partitionPointer.setValue8(7, (byte) 0x00);
		// LBA of first absolute sector in the partition
		partitionPointer.setUnalignedValue32(8, FIRST_PAGE_LBA);
		// Number of sectors in partition
		partitionPointer.setUnalignedValue32(12, NUMBER_OF_PAGES);

		// Signature
		pageBufferPointer.setValue8(510, (byte) 0x55);
		pageBufferPointer.setValue8(511, (byte) 0xAA);
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
				// Is the memory stick inserted?
				outdata.setValue32(MemoryStick.isInserted());
				break;
    		case 0x02125009:
				// Is the memory stick locked?
    			outdata.setValue32(MemoryStick.isLocked());
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
				// Output value 0x11 or 0x41: the Memory Stick is locked
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
                    outdata.setStringNZ(12, 16, ""); // This value will be set in registry as /CONFIG/CAMERA/msid
    			}
    			break;
    		case 0x02125008:
				// Is the memory stick inserted?
    			outdata.setValue32(MemoryStick.isInserted());
    			break;
    		case 0x02125009:
				// Is the memory stick locked?
    			outdata.setValue32(MemoryStick.isLocked());
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

    public int hleMSstorRawIoRead(long offset, TPointer data, int len) {
    	int result;

    	if (offset < FIRST_PAGE_LBA * PAGE_SIZE) {
			vFileIpl.ioLseek(offset);
			result = vFileIpl.ioRead(data, len);
		} else {
			result = hleMSstorPartitionIoRead(offset - FIRST_PAGE_LBA * PAGE_SIZE, data, len);
		}

		return result;
    }

    public int hleMSstorRawIoRead(long offset, byte[] buffer, int bufferOffset, int len) {
    	int result;

    	if (offset < FIRST_PAGE_LBA * PAGE_SIZE) {
			vFileIpl.ioLseek(offset);
			result = vFileIpl.ioRead(buffer, bufferOffset, len);
		} else {
			result = hleMSstorPartitionIoRead(offset - FIRST_PAGE_LBA * PAGE_SIZE, buffer, bufferOffset, len);
		}

		return result;
    }

    public int hleMSstorRawIoWrite(long offset, TPointer data, int len) {
    	int result;

    	if (offset < FIRST_PAGE_LBA * PAGE_SIZE) {
    		if (log.isDebugEnabled()) {
    			log.debug(String.format("hleMSstorRawIoWrite IPL offset=0x%X, len=0x%X", offset, len));
    		}
			vFileIpl.ioLseek(offset);
			result = vFileIpl.ioWrite(data, len);
		} else {
			result = hleMSstorPartitionIoWrite(offset - FIRST_PAGE_LBA * PAGE_SIZE, data, len);
		}

		return result;
    }

    public int hleMSstorRawIoWrite(long offset, byte[] buffer, int bufferOffset, int len) {
    	int result;

    	if (offset < FIRST_PAGE_LBA * PAGE_SIZE) {
    		if (log.isDebugEnabled()) {
    			log.debug(String.format("hleMSstorRawIoWrite IPL offset=0x%X, len=0x%X", offset, len));
    		}
			vFileIpl.ioLseek(offset);
			result = vFileIpl.ioWrite(buffer, bufferOffset, len);
		} else {
			result = hleMSstorPartitionIoWrite(offset - FIRST_PAGE_LBA * PAGE_SIZE, buffer, bufferOffset, len);
		}

		return result;
    }

    public int hleMSstorPartitionIoRead(long offset, TPointer data, int len) {
    	if (vFile != null) {
    		scanThread.waitForCompletion();
			// synchronize the vFile to make sure that the ioLseek/ioRead combination is atomic
    		synchronized (vFile) {
            	vFile.ioLseek(offset);
        		len = vFile.ioRead(data, len);
			}
    	}

    	return len;
    }

    public int hleMSstorPartitionIoRead(long offset, byte[] buffer, int bufferOffset, int len) {
    	if (vFile != null) {
    		scanThread.waitForCompletion();
			// synchronize the vFile to make sure that the ioLseek/ioRead combination is atomic
    		synchronized (vFile) {
            	vFile.ioLseek(offset);
        		len = vFile.ioRead(buffer, bufferOffset, len);
			}
    	}

    	return len;
    }

    @HLEUnimplemented
    @HLEFunction(nid = HLESyscallNid, version = 150)
    public int hleMSstorPartitionIoWrite(pspIoDrvFileArg drvFileArg, @BufferInfo(lengthInfo=LengthInfo.nextParameter, usage=Usage.in) TPointer data, int len) {
    	if (vFile != null) {
    		scanThread.waitForCompletion();
    		synchronized (writeLock) {
        		len = vFile.ioWrite(data, len);
    			sync.notifyWrite();
			}
    	}

    	return len;
    }

    public int hleMSstorPartitionIoWrite(long offset, TPointer data, int len) {
    	if (vFile != null) {
    		scanThread.waitForCompletion();
    		synchronized (writeLock) {
    			// synchronize the vFile to make sure that the ioLseek/ioWrite combination is atomic
    			synchronized (vFile) {
        			position = vFile.ioLseek(offset);
            		len = vFile.ioWrite(data, len);
				}
    			sync.notifyWrite();
			}
    	}

    	return len;
    }

    public int hleMSstorPartitionIoWrite(long offset, byte[] buffer, int bufferOffset, int len) {
    	if (vFile != null) {
    		scanThread.waitForCompletion();
    		synchronized (writeLock) {
    			// synchronize the vFile to make sure that the ioLseek/ioWrite combination is atomic
    			synchronized (vFile) {
        			position = vFile.ioLseek(offset);
            		len = vFile.ioWrite(buffer, bufferOffset, len);
				}
    			sync.notifyWrite();
			}
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

		Memory mem = getMemory();
		controllerFuncs.ioInit = new TPointerFunction(mem, addr);
		addr += sizeIoFunctionStub;
		controllerFuncs.ioDevctl = new TPointerFunction(mem, addr);
		addr += sizeIoFunctionStub;
		HLEUtilities.getInstance().installHLESyscall(controllerFuncs.ioInit, this, "hleMSstorControllerIoInit");
		HLEUtilities.getInstance().installHLESyscall(controllerFuncs.ioDevctl, this, "hleMSstorControllerIoDevctl");

		storageFuncs.ioInit = new TPointerFunction(mem, addr);
		addr += sizeIoFunctionStub;
		storageFuncs.ioDevctl = new TPointerFunction(mem, addr);
		addr += sizeIoFunctionStub;
		storageFuncs.ioOpen = new TPointerFunction(mem, addr);
		addr += sizeIoFunctionStub;
		storageFuncs.ioIoctl = new TPointerFunction(mem, addr);
		addr += sizeIoFunctionStub;
		storageFuncs.ioClose = new TPointerFunction(mem, addr);
		addr += sizeIoFunctionStub;
		HLEUtilities.getInstance().installHLESyscall(storageFuncs.ioInit, this, "hleMSstorStorageIoInit");
		HLEUtilities.getInstance().installHLESyscall(storageFuncs.ioDevctl, this, "hleMSstorStorageIoDevctl");
		HLEUtilities.getInstance().installHLESyscall(storageFuncs.ioOpen, this, "hleMSstorStorageIoOpen");
		HLEUtilities.getInstance().installHLESyscall(storageFuncs.ioIoctl, this, "hleMSstorStorageIoIoctl");
		HLEUtilities.getInstance().installHLESyscall(storageFuncs.ioClose, this, "hleMSstorStorageIoClose");

		partitionFuncs.ioInit = new TPointerFunction(mem, addr);
		addr += sizeIoFunctionStub;
		partitionFuncs.ioDevctl = new TPointerFunction(mem, addr);
		addr += sizeIoFunctionStub;
		partitionFuncs.ioOpen = new TPointerFunction(mem, addr);
		addr += sizeIoFunctionStub;
		partitionFuncs.ioClose = new TPointerFunction(mem, addr);
		addr += sizeIoFunctionStub;
		partitionFuncs.ioIoctl = new TPointerFunction(mem, addr);
		addr += sizeIoFunctionStub;
		partitionFuncs.ioLseek = new TPointerFunction(mem, addr);
		addr += sizeIoFunctionStub;
		partitionFuncs.ioRead = new TPointerFunction(mem, addr);
		addr += sizeIoFunctionStub;
		partitionFuncs.ioWrite = new TPointerFunction(mem, addr);
		addr += sizeIoFunctionStub;
		HLEUtilities.getInstance().installHLESyscall(partitionFuncs.ioInit, this, "hleMSstorPartitionIoInit");
		HLEUtilities.getInstance().installHLESyscall(partitionFuncs.ioDevctl, this, "hleMSstorPartitionIoDevctl");
		HLEUtilities.getInstance().installHLESyscall(partitionFuncs.ioOpen, this, "hleMSstorPartitionIoOpen");
		HLEUtilities.getInstance().installHLESyscall(partitionFuncs.ioClose, this, "hleMSstorPartitionIoClose");
		HLEUtilities.getInstance().installHLESyscall(partitionFuncs.ioIoctl, this, "hleMSstorPartitionIoIoctl");
		HLEUtilities.getInstance().installHLESyscall(partitionFuncs.ioLseek, this, "hleMSstorPartitionIoLseek");
		HLEUtilities.getInstance().installHLESyscall(partitionFuncs.ioRead, this, "hleMSstorPartitionIoRead");
		HLEUtilities.getInstance().installHLESyscall(partitionFuncs.ioWrite, this, "hleMSstorPartitionIoWrite");
    }

    private Fat32VirtualFile openFile() {
    	if (sync != null) {
    		sync.exit();
    		sync = null;
    	}

		long totalSize = MemoryStick.getTotalSize();
		long totalNumberOfPages = totalSize / PAGE_SIZE;
		int PAGES_PER_BLOCK = 16;
		int NUMBER_OF_PHYSICAL_BLOCKS = (int) (totalNumberOfPages / PAGES_PER_BLOCK);
		if (totalNumberOfPages > 8192L) {
			for (PAGES_PER_BLOCK = 32; PAGES_PER_BLOCK < 0x8000; PAGES_PER_BLOCK <<= 1) {
				NUMBER_OF_PHYSICAL_BLOCKS = (int) (totalNumberOfPages / PAGES_PER_BLOCK);
				if (NUMBER_OF_PHYSICAL_BLOCKS < 0x10000) {
					break;
				}
			}
		}
		int BLOCK_SIZE = PAGES_PER_BLOCK * PAGE_SIZE / 1024; // Number of KB per block
		FIRST_PAGE_LBA = 2 * PAGES_PER_BLOCK;
		NUMBER_OF_PAGES = (NUMBER_OF_PHYSICAL_BLOCKS / 512 * 496 - 2) * BLOCK_SIZE * 2;
		NUMBER_OF_PAGES -= FIRST_PAGE_LBA;
		if (log.isDebugEnabled()) {
			log.debug(String.format("openFile totalSize=0x%X(%s), pagesPerBlock=0x%X, numberOfPhysicalBlocks=0x%X", totalSize, MemoryStick.getSizeKbString((int) (totalSize / 1024)), PAGES_PER_BLOCK, NUMBER_OF_PHYSICAL_BLOCKS));
		}

    	IVirtualFileSystem vfs = new LocalVirtualFileSystem(Settings.getInstance().getDirectoryMapping("ms0"), true);
		Fat32VirtualFile fat32VirtualFile = new Fat32VirtualFile("ms0:", vfs);
		vFile = new WriteCacheVirtualFile(log, fat32VirtualFile);
		fat32VirtualFile.setBaseVirtualFile(vFile);
		IVirtualFileSystem input = new FatVirtualFileSystem("ms0", vFile);
		sync = new SynchronizeVirtualFileSystems("ms0", input, vfs, writeLock);
		if (log.isDebugEnabled()) {
			log.debug(String.format("openFile vFile=%s", vFile));
		}

    	try {
			vFileIpl = new LocalVirtualFile(new SeekableRandomFile("ms.ipl.bin", "rw"));

			// Update the vFileIpl with the new master boot record
			final int iplFileSize = FIRST_PAGE_LBA * PAGE_SIZE;
			final byte[] iplBuffer = new byte[iplFileSize];
			getMasterBootRecord(iplBuffer, 0);
			vFileIpl.ioLseek(0L);
			vFileIpl.ioWrite(iplBuffer, 0, iplFileSize);
			vFileIpl.ioLseek(0L);
		} catch (FileNotFoundException e) {
			log.error("Error while opening the ms.ipl.bin file", e);
		}

    	return fat32VirtualFile;
    }

    public void hleInit() {
    	Fat32VirtualFile fat32VirtualFile = openFile();

		scanThread = new Fat32ScanThread(fat32VirtualFile);
		scanThread.setName("Fat32VirtualFile Scan Thread");
		scanThread.setDaemon(true);
		scanThread.start();
    }

    public void installDrivers() {
		Memory mem = Memory.getInstance();

		dumpIoIoctl_0x02125803 = readBytes("ms.ioctl.0x02125803");

		hleInit();

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

    /**
     * This is the function executed at module start (alias to "module_start")
     *
     * @param unknown1
     * @param unknown2
     * @param unknown3
     * @return
     */
    @HLEUnimplemented
    @HLEFunction(nid = 0x6FC1E8AE, version = 150)
    public int sceMSstorEntry(int unknown1, int unknown2, int unknown3) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x714782D6, version = 150)
    public int sceMSstorRegisterCLDMSelf(int unknown) {
    	return 0;
    }
}
