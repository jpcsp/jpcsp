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

import static jpcsp.HLE.modules.sceUmdUser.PSP_UMD_READABLE;
import static jpcsp.HLE.modules.sceUmdUser.PSP_UMD_READY;
import static jpcsp.filesystems.umdiso.ISectorDevice.sectorLength;
import static jpcsp.memory.mmio.umd.MMIOHandlerUmdAta.ATA_SENSE_ASC_MEDIUM_NOT_PRESENT;
import static jpcsp.memory.mmio.umd.MMIOHandlerUmdAta.ATA_SENSE_KEY_NOT_READY;
import static jpcsp.memory.mmio.umd.MMIOHandlerUmdAta.ATA_SENSE_KEY_NO_SENSE;

import java.io.IOException;

import org.apache.log4j.Logger;

import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.HLE.BufferInfo;
import jpcsp.HLE.BufferInfo.LengthInfo;
import jpcsp.HLE.BufferInfo.Usage;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.kernel.types.IAction;
import jpcsp.HLE.kernel.types.SceKernelThreadInfo;
import jpcsp.HLE.modules.SysMemUserForUser.SysMemInfo;
import jpcsp.filesystems.umdiso.ISectorDevice;
import jpcsp.filesystems.umdiso.UmdIsoReader;
import jpcsp.scheduler.Scheduler;
import jpcsp.util.Utilities;

public class sceUmdMan extends HLEModule {
    public static Logger log = Modules.getLogger("sceUmdMan");
    protected SysMemInfo dummyAreaInfo;
    protected TPointer dummyArea;
    protected SysMemInfo dummyUmdDriveInfo;
    protected TPointer dummyUmdDrive;

    private static class TriggerCallbackAction implements IAction {
    	private int status;
    	private int callback;
    	private int callbackArg;
    	private int argument3;

		public TriggerCallbackAction(int status, int callback, int callbackArg, int argument3) {
			this.status = status;
			this.callback = callback;
			this.callbackArg = callbackArg;
			this.argument3 = argument3;
		}

		@Override
		public void execute() {
    		SceKernelThreadInfo thread = Modules.ThreadManForUserModule.getCurrentThread();
			Modules.ThreadManForUserModule.executeCallback(thread, callback, null, true, status, callbackArg, argument3);
		}
    }

    protected ISectorDevice getSectorDevice() {
		UmdIsoReader iso = Modules.sceUmdUserModule.getIsoReader();
		if (iso == null) {
			return null;
		}
		return iso.getSectorDevice();
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x65E2B3E0, version = 660)
    public int sceUmdMan_65E2B3E0() {
    	// Has no parameters
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x47E2B6D8, version = 150)
    public int sceUmdManGetUmdDrive(int unknown) {
    	if (dummyUmdDriveInfo == null) {
    		final int size = 36;
    		dummyUmdDriveInfo = Modules.SysMemUserForUserModule.malloc(SysMemUserForUser.KERNEL_PARTITION_ID, "sceUmdManGetUmdDrive", SysMemUserForUser.PSP_SMEM_Low, size, 0);
    		if (dummyUmdDriveInfo == null) {
    			return -1;
    		}
    		dummyUmdDrive = new TPointer(Memory.getInstance(), dummyUmdDriveInfo.addr);
    		dummyUmdDrive.clear(size);
    	}

    	// This will be the value of the first parameter passed to sceUmdMan_E3716915()
    	return dummyUmdDrive.getAddress();
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x3C8C523D, version = 150)
    public int sceUmdMan_3C8C523D(int wantedStatus, TPointer callback, TPointer callbackArg) {
    	if (getSectorDevice() != null) {
    		if (wantedStatus == PSP_UMD_READABLE || wantedStatus == PSP_UMD_READY) {
    			TriggerCallbackAction triggerCallbackAction = new TriggerCallbackAction(wantedStatus, callback.getAddress(), callbackArg.getAddress(), 0);
    			Emulator.getScheduler().addAction(Scheduler.getNow() + 1000, triggerCallbackAction);
    		}
    	}

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x3A05AC3C, version = 150)
    public boolean sceUmdMan_3A05AC3C() {
    	// Has no parameters
        return false;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x0DC8D26D, version = 150)
    public int sceUmdManWaitSema() {
    	// Has no parameters
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x9F106F73, version = 150)
    public int sceUmdManPollSema() {
    	// Has no parameters
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xB0A43DA7, version = 150)
    public int sceUmdManSignalSema() {
    	// Has no parameters
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xF31D8208, version = 150)
    public int sceUmdMan_F31D8208() {
    	// Has no parameters
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x6519F8D1, version = 150)
    public int sceUmdMan_6519F8D1(int timeout) {
    	// Calling sceKernelSetAlarm(timeout)
    	int alarmUid = 0;
        return alarmUid;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xE3716915, version = 150)
    public int sceUmdMan_E3716915(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=16, usage=Usage.in) TPointer umdDrive, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=12, usage=Usage.in) TPointer lbaParameters, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=sectorLength, usage=Usage.out) TPointer readBuffer1, int readSize, @BufferInfo(lengthInfo=LengthInfo.previousParameter, usage=Usage.out) TPointer readBuffer2, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=sectorLength, usage=Usage.out) TPointer readBuffer3, int flags) {
    	int sectorNumber = lbaParameters.getValue32(0);
    	int unknown1 = lbaParameters.getValue32(4);
    	int unknown2 = lbaParameters.getValue16(8);
    	int unknown3 = lbaParameters.getValue8(10);
    	int unknown4 = lbaParameters.getValue8(11);

    	if (log.isDebugEnabled()) {
    		log.debug(String.format("sceUmdMan_E3716915 LBA parameters: sectorNumber=0x%X, unknown1=0x%X, unknown2=0x%X, unknown3=0x%X, unknown4=0x%X", sectorNumber, unknown1, unknown2, unknown3, unknown4));
    	}

		ISectorDevice sectorDevice = getSectorDevice();
		if (sectorDevice == null) {
			log.warn(String.format("sceUmdMan_E3716915 no SectorDevice available"));
			return -1;
		}

    	int totalReadSize = 0;
    	if ((flags & 1) != 0) {
    		totalReadSize += sectorLength;
    	}
    	if ((flags & 2) != 0) {
    		totalReadSize += readSize;
    	}
    	if ((flags & 4) != 0) {
    		totalReadSize += sectorLength;
    	}

    	int totalReadSectors = Utilities.alignUp(totalReadSize, sectorLength - 1) / sectorLength;
    	byte[] dataBuffer = new byte[totalReadSectors * sectorLength];
    	try {
    		sectorDevice.readSectors(sectorNumber, totalReadSectors, dataBuffer, 0);
		} catch (IOException e) {
			log.error(e);
			return -1;
		}

    	int dataBufferOffset = 0;
    	if ((flags & 1) != 0) {
    		readBuffer1.setArray(0, dataBuffer, dataBufferOffset, sectorLength);
    		dataBufferOffset += sectorLength;
    	}
    	if ((flags & 2) != 0) {
    		readBuffer2.setArray(0, dataBuffer, dataBufferOffset, readSize);
    		dataBufferOffset += readSize;
    	}
    	if ((flags & 4) != 0) {
    		readBuffer3.setArray(0, dataBuffer, dataBufferOffset, sectorLength);
    		dataBufferOffset += sectorLength;
    	}

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x709E7035, version = 150)
    public int sceUmdMan_709E7035(int alarmUid) {
    	// Calling sceKernelCancelAlarm(alarmUid)
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xD1478023, version = 150)
    public int sceUmdMan_D1478023() {
    	// Has no parameters
		ISectorDevice sectorDevice = getSectorDevice();
		if (sectorDevice == null) {
			log.warn(String.format("sceUmdMan_D1478023 no SectorDevice available"));
			return -1;
		}

		int numSectors;
		try {
			numSectors = sectorDevice.getNumSectors();
		} catch (IOException e) {
			log.warn(String.format("sceUmdMan_D1478023 IO error %s", e));
			return -1;
		}

    	if (dummyAreaInfo == null) {
    		dummyAreaInfo = Modules.SysMemUserForUserModule.malloc(SysMemUserForUser.KERNEL_PARTITION_ID, "sceUmdMan_D1478023", SysMemUserForUser.PSP_SMEM_Low, 120, 0);
    		if (dummyAreaInfo == null) {
    			return -1;
    		}
    		dummyArea = new TPointer(Memory.getInstance(), dummyAreaInfo.addr);
    	}

		dummyArea.setValue8(100, (byte) 0x12);
    	dummyArea.setValue8(101, (byte) 0x34);
    	dummyArea.setValue8(102, (byte) 0x56);
    	dummyArea.setValue8(103, (byte) 0x78);
    	dummyArea.setValue32(108, 0x11111111);
    	dummyArea.setValue32(112, numSectors); // value returned by sceIoDevctl cmd=0x01F20003 (get total number of sectors)
    	dummyArea.setValue32(116, numSectors); // value returned by sceIoDevctl cmd=0x01F20002 (get current LBA)

    	return dummyArea.getAddress();
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x4FB913A3, version = 150)
    public int sceUmdManGetIntrStateFlag() {
    	// Has no parameters
        return 0; // Can return 0 or 4
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x8FF7C13A, version = 150)
    public int sceUmdMan_8FF7C13A() {
    	// Has no parameters
        return 0; // Can return 0 or 4
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x2CBE959B, version = 150)
    public int sceUmdExecReqSenseCmd(TPointer unknown, @BufferInfo(lengthInfo=LengthInfo.nextParameter, usage=Usage.out) TPointer resultAddr, int resultSize) {
    	if (resultSize < 18) {
    		return -1;
    	}

    	boolean mediumPresent = getSectorDevice() != null;

    	// Result of ATA_CMD_OP_REQUEST_SENSE command
    	resultAddr.clear(resultSize);
    	resultAddr.setUnsignedValue8(0, 0x80); // Valid bit, no Error Code
    	resultAddr.setUnsignedValue8(1, 0x00); // Reserved
		if (mediumPresent) {
			resultAddr.setUnsignedValue8(2, ATA_SENSE_KEY_NO_SENSE); // Successful command
		} else {
			resultAddr.setUnsignedValue8(2, ATA_SENSE_KEY_NOT_READY); // Medium not present
		}
		resultAddr.setUnalignedValue32(3, 0); // Information
		resultAddr.setUnsignedValue8(7, 10); // Additional Sense Length
		resultAddr.setUnalignedValue32(8, 0); // Command Specific Information
		if (mediumPresent) {
			resultAddr.setUnsignedValue8(12, 0); // Additional Sense Code
			resultAddr.setUnsignedValue8(13, 0); // Additional Sense Code Qualifier
		} else {
			resultAddr.setUnsignedValue8(12, ATA_SENSE_ASC_MEDIUM_NOT_PRESENT); // Additional Sense Code: medium not present
			resultAddr.setUnsignedValue8(13, 0x2); // Additional Sense Code Qualifier
		}
		resultAddr.setUnsignedValue8(14, 0); // Field Replaceable Unit Code
		resultAddr.setUnsignedValue8(15, 0); // SKSV / Sense Key Specific
		resultAddr.setUnsignedValue8(16, 0); // Sense Key Specific
		resultAddr.setUnsignedValue8(17, 0); // Sense Key Specific

    	// The official PSP Update EBOOT only accepts the following values
		// otherwise it displays an error "LPTFFFFFFF7".
		// Not sure of the meaning of those values.
		if (!mediumPresent && Emulator.getInstance().isPspOfficialUpdater()) {
			resultAddr.setUnsignedValue8(2, 9);
			resultAddr.setUnsignedValue8(12, 2);
			resultAddr.setUnsignedValue8(13, 0);
		}

		return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xC8D137FA, version = 150)
    public int sceUmdMan_C8D137FA(TPointer unknown) {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x2CE918B1, version = 150)
    public int sceUmdMan_2CE918B1() {
    	// Has no parameters
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xA7536109, version = 150)
    public int sceUmdMan_A7536109(int unknown) {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xE779ECEF, version = 150)
    public int sceUmdManGetInquiry(int unknown, int outputBufferLength, @BufferInfo(lengthInfo=LengthInfo.previousParameter, usage=Usage.out) TPointer outputBuffer) {
    	outputBuffer.clear(outputBufferLength);

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x921E7B7D, version = 150)
    public int sceUmdMan_driver_921E7B7D() {
    	// Has no parameters
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x8CFED611, version = 150)
    public int sceUmdManStart(int unknown) {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x1F9AFFF4, version = 150)
    public int sceUmdManMediaPresent(int unknown) {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xD372D6F3, version = 150)
    public int sceUmdMan_driver_D372D6F3(TPointer driveInformationAddress) {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x14D3381C, version = 150)
    public int sceUmdExecTestCmd() {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x1B19A313, version = 150)
    public int sceUmdExecInquiryCmd() {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x0D3EA203, version = 150)
    public int sceUmdManTerm() {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xCAD31025, version = 150)
    public int sceUmdManStop() {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x736AE133, version = 150)
    public int  sceUmdManLPNNegateWakeup() {
        return 0;
    }
}
