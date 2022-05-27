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
package jpcsp.HLE.VFS.memoryStick;

import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_MEMSTICK_DEVCTL_BAD_PARAMS;

import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.VFS.AbstractVirtualFileSystem;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.kernel.types.SceKernelThreadInfo;
import jpcsp.HLE.modules.ThreadManForUser;
import jpcsp.hardware.MemoryStick;

/**
 * Virtual File System implementing the PSP device mscmhc0.
 *
 * @author gid15
 *
 */
public class MemoryStickVirtualFileSystem extends AbstractVirtualFileSystem {

	@Override
	public int ioDevctl(String deviceName, int command, TPointer inputPointer, int inputLength, TPointer outputPointer, int outputLength) {
		int result;

		switch (command) {
	        // Check the MemoryStick's driver status (mscmhc0).
	        case 0x02025801: {
        		log.debug("ioDevctl check ms driver status");
	            if (outputPointer.isAddressGood()) {
	                // 0 = Driver busy.
	                // 1 = Driver ready.
	            	// 4 = ???
	            	outputPointer.setValue32(4);
	                result = 0;
	            } else {
	            	result = IO_ERROR;
	            }
	            break;
	        }
	        // Register MemoryStick's insert/eject callback (mscmhc0).
	        case 0x02015804: {
	            log.debug("ioDevctl register memorystick insert/eject callback (mscmhc0)");
	            ThreadManForUser threadMan = Modules.ThreadManForUserModule;
	            if (inputPointer.isAddressGood() && inputLength == 4) {
	                int cbid = inputPointer.getValue32();
	                final int callbackType = SceKernelThreadInfo.THREAD_CALLBACK_MEMORYSTICK;
	                if (threadMan.hleKernelRegisterCallback(callbackType, cbid)) {
	                    // Trigger the registered callback immediately.
	                    threadMan.hleKernelNotifyCallback(callbackType, cbid, MemoryStick.getStateMs());
	                    result = 0; // Success.
	                } else {
	                	result = SceKernelErrors.ERROR_MEMSTICK_DEVCTL_TOO_MANY_CALLBACKS;
	                }
	            } else {
	            	result = ERROR_MEMSTICK_DEVCTL_BAD_PARAMS;
	            }
	            break;
	        }
	        // Unregister MemoryStick's insert/eject callback (mscmhc0).
	        case 0x02015805: {
	            log.debug("ioDevctl unregister memorystick insert/eject callback (mscmhc0)");
	            ThreadManForUser threadMan = Modules.ThreadManForUserModule;
	            if (inputPointer.isAddressGood() && inputLength == 4) {
	                int cbid = inputPointer.getValue32();
	                if (threadMan.hleKernelUnRegisterCallback(SceKernelThreadInfo.THREAD_CALLBACK_MEMORYSTICK, cbid)) {
	                	result = 0; // Success.
	                } else {
	                	result = ERROR_MEMSTICK_DEVCTL_BAD_PARAMS; // No such callback.
	                }
	            } else {
	            	result = ERROR_MEMSTICK_DEVCTL_BAD_PARAMS;
	            }
	            break;
	        }
	        // Check if the device is inserted (mscmhc0).
	        case 0x02025806: {
	            log.debug("ioDevctl check ms inserted (mscmhc0)");
	            if (outputPointer.isAddressGood() && outputLength >= 4) {
	                // 0 = Not inserted.
	                // 1 = Inserted.
	            	outputPointer.setValue32(1);
	                result = 0;
	            } else {
	            	result = ERROR_MEMSTICK_DEVCTL_BAD_PARAMS;
	            }
	            break;
	        }
            // Unknown, used by flash0:/kd/utility.prx
            case 0x02015807: {
                log.debug("sceIoDevctl 0x02015807 (mscmhc0)");
                if (outputPointer.isAddressGood() && outputLength == 4) {
                	outputPointer.setValue32(0); // Unknown value: seems to be 0 or 1?
                	result = 0;
                } else {
                	result = ERROR_MEMSTICK_DEVCTL_BAD_PARAMS;
                }
                break;
            }
            // Unknown, used by flash0:/kd/utility.prx
            case 0x0201580B: {
                log.debug("sceIoDevctl 0x0201580B (mscmhc0)");
                if (inputPointer.isAddressGood() && inputLength == 20) {
                	result = 0;
                } else {
                	result = ERROR_MEMSTICK_DEVCTL_BAD_PARAMS;
                }
                break;
            }
            // Unknown, used by flash0:/kd/utility.prx
            case 0x0202580A: {
                log.debug("sceIoDevctl 0x0202580A (mscmhc0)");
                if (outputPointer.isAddressGood() && outputLength == 16) {
                	// When value1 or value2 are < 10000, sceUtilitySavedata is
                	// returning an error 0x8011032C (bad status).
                	// When value1 or value2 are > 10000, sceUtilitySavedata is
                	// returning an error 0x8011032A (the system has been shifted to sleep mode).
                	final int value1 = 10000;
                	final int value2 = 10000;
                	// When value3 or value4 are < 10000, sceUtilitySavedata is
                	// returning an error 0x8011032C (bad status)
                	// When value3 or value4 are > 10000, sceUtilitySavedata is
                	// returning an error 0x80110322 (the memory stick has been removed).
                	final int value3 = 10000;
                	final int value4 = 10000;
                	// No error is returned by sceUtilitySavedata only when
                	// all 4 values are set to 10000.

                    outputPointer.setValue32(0, value1);
                    outputPointer.setValue32(4, value2);
                    outputPointer.setValue32(8, value3);
                    outputPointer.setValue32(12, value4);
                    result = 0;
                } else {
                	result = -1;
                }
                break;
            }
	        default:
	        	result = super.ioDevctl(deviceName, command, inputPointer, inputLength, outputPointer, outputLength);
		}

		return result;
	}
}
