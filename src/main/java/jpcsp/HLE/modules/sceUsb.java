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

import jpcsp.HLE.BufferInfo;
import jpcsp.HLE.BufferInfo.LengthInfo;
import jpcsp.HLE.BufferInfo.Usage;
import jpcsp.HLE.CanBeNull;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.PspString;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer32;
import jpcsp.HLE.TPointerFunction;

import static jpcsp.HLE.Modules.sceUsbPspcmModule;

import org.apache.log4j.Logger;

import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.types.SceKernelThreadInfo;
import jpcsp.HLE.kernel.types.pspUsbDriver;
import jpcsp.hardware.Usb;

public class sceUsb extends HLEModule {
    public static Logger log = Modules.getLogger("sceUsb");

	public static final String PSP_USBBUS_DRIVERNAME = "USBBusDriver";

	public static final int PSP_USB_CONNECTION_NOT_ESTABLISHED = 0x001;
	public static final int PSP_USB_CONNECTION_ESTABLISHED = 0x002;
	public static final int PSP_USB_CABLE_DISCONNECTED = 0x010;
	public static final int PSP_USB_CABLE_CONNECTED = 0x020;
	public static final int PSP_USB_DEACTIVATED = 0x100;
	public static final int PSP_USB_ACTIVATED = 0x200;
	protected static final int WAIT_MODE_ANDOR_MASK = 0x1;
	protected static final int WAIT_MODE_AND = 0x0;
	protected static final int WAIT_MODE_OR = 0x1;

	protected boolean usbActivated = false;
	protected boolean usbStarted = false;
	protected int callbackId = -1;

	@Override
	public void start() {
		usbActivated = false;
		usbStarted = false;

		super.start();
	}

	protected int getUsbState() {
		int state = Usb.isCableConnected() ? PSP_USB_CABLE_CONNECTED : PSP_USB_CABLE_DISCONNECTED;

		// USB has been activated?
		state |= usbActivated ? PSP_USB_ACTIVATED : PSP_USB_DEACTIVATED;

		// USB has been started?
		state |= usbStarted ? PSP_USB_CONNECTION_ESTABLISHED : PSP_USB_CONNECTION_NOT_ESTABLISHED;

		return state;
	}

	protected boolean matchState(int waitState, int waitMode) {
		int state = getUsbState();
		if ((waitMode & WAIT_MODE_ANDOR_MASK) == WAIT_MODE_AND) {
			// WAIT_MODE_AND
			return (state & waitState) == waitState;
		}
		// WAIT_MODE_OR
		return (state & waitState) != 0;
	}

	protected void notifyCallback() {
		if (callbackId >= 0) {
			Modules.ThreadManForUserModule.hleKernelNotifyCallback(SceKernelThreadInfo.THREAD_CALLBACK_USB, getUsbState());
		}
	}

	/**
	 * Start a USB driver.
	 *
	 * @param driverName - name of the USB driver to start
	 * @param size - Size of arguments to pass to USB driver start
	 * @param args - Arguments to pass to USB driver start
	 *
	 * @return 0 on success
	 */
	@HLEFunction(nid = 0xAE5DE6AF, version = 150)
	public int sceUsbStart(String driverName, int size, @BufferInfo(lengthInfo = LengthInfo.previousParameter, usage = Usage.in) @CanBeNull TPointer args) {
		usbStarted = true;

		int result;
		if ("USBBusDriver".equals(driverName)) {
			result = 0;
		} else {
			pspUsbDriver usbDriver = Modules.sceUsbBusModule.getRegisteredUsbDriver(driverName);
			if (usbDriver == null) {
				log.error(String.format("sceUsbStart unknown driver '%s'", driverName));
				result = -1;
			} else {
				if (log.isDebugEnabled()) {
					log.debug(String.format("sceUsbStart on %s", usbDriver));
				}
				TPointerFunction start_func = usbDriver.start_func;
				if (start_func != null) {
					result = start_func.executeCallback(size, args.getAddress());
				} else {
					result = 0;
				}
			}
		}

		notifyCallback();

		return result;
	}

	/**
	 * Stop a USB driver.
	 *
	 * @param driverName - name of the USB driver to stop
	 * @param size - Size of arguments to pass to USB driver start
	 * @param args - Arguments to pass to USB driver start
	 *
	 * @return 0 on success
	 */
	@HLEFunction(nid = 0xC2464FA0, version = 150)
	public int sceUsbStop(String driverName, int size, @BufferInfo(lengthInfo = LengthInfo.previousParameter, usage = Usage.in) @CanBeNull TPointer args) {
		usbStarted = false;

		int result;
		if ("USBBusDriver".equals(driverName)) {
			result = 0;
		} else {
			pspUsbDriver usbDriver = Modules.sceUsbBusModule.getRegisteredUsbDriver(driverName);
			if (usbDriver == null) {
				log.error(String.format("sceUsbStop unknown driver '%s'", driverName));
				result = -1;
			} else {
				if (log.isDebugEnabled()) {
					log.debug(String.format("sceUsbStop on %s", usbDriver));
				}
				TPointerFunction stop_func = usbDriver.stop_func;
				if (stop_func != null) {
					result = stop_func.executeCallback(size, args.getAddress());
				} else {
					result = 0;
				}
			}
		}

		notifyCallback();

		return result;
	}

	/**
	 * Get USB state
	 *
	 * @return OR'd PSP_USB_* constants
	 */
	@HLEFunction(nid = 0xC21645A4, version = 150)
	public int sceUsbGetState() {
		if (log.isDebugEnabled()) {
			log.debug(String.format("sceUsbGetState returning 0x%X", getUsbState()));
		}

		return getUsbState();
	}

	@HLEFunction(nid = 0x4E537366, version = 150)
	public int sceUsbGetDrvList(int unknown1, int unknown2, int unknown3) {
		log.warn(String.format("Unimplemented sceUsbGetDrvList unknown1=0x%08X, unknown2=0x%08X, unknown3=0x%08X", unknown1, unknown2, unknown3));

		return 0;
	}

	/**
	 * Get state of a specific USB driver
	 *
	 * @param driverName - name of USB driver to get status from
	 *
	 * @return 1 if the driver has been started, 2 if it is stopped
	 */
	@HLEUnimplemented
	@HLEFunction(nid = 0x112CC951, version = 150)
	public int sceUsbGetDrvState(PspString driverName) {
		return 0;
	}

	/**
	 * Activate a USB driver.
	 *
	 * @param pid - Product ID for the default USB Driver
	 *
	 * @return 0 on success
	 */
	@HLEFunction(nid = 0x586DB82C, version = 150)
	public int sceUsbActivate(int pid) {
		return sceUsbActivateWithCharging(pid, false);
	}

	/**
	 * Deactivate USB driver.
	 *
	 * @return 0 on success
	 */
	@HLEFunction(nid = 0xC572A9C8, version = 150)
	public int sceUsbDeactivate() {
		usbActivated = false;
		notifyCallback();

		sceUsbPspcmModule.onUsbDeactivate();

		return 0;
	}

	@HLEFunction(nid = 0x5BE0E002, version = 150)
	public int sceUsbWaitState(int state, int waitMode, @CanBeNull TPointer32 timeoutAddr) {
		if (!matchState(state, waitMode)) {
			log.warn(String.format("Unimplemented sceUsbWaitState state=0x%X, waitMode=0x%X, timeoutAddr=%s - non-matching state not implemented", state, waitMode, timeoutAddr));
			Modules.ThreadManForUserModule.hleBlockCurrentThread(SceKernelThreadInfo.JPCSP_WAIT_USB);
			return 0;
		}

		int usbState = getUsbState();
		if (log.isDebugEnabled()) {
			log.debug(String.format("sceUsbWaitState returning 0x%X", usbState));
		}
		return usbState;
	}

	@HLEFunction(nid = 0x616F2B61, version = 150)
	public int sceUsbWaitStateCB(int state, int waitMode, @CanBeNull TPointer32 timeoutAddr) {
		if (!matchState(state, waitMode)) {
			log.warn(String.format("Unimplemented sceUsbWaitStateCB state=0x%X, waitMode=0x%X, timeoutAddr=%s - non-matching state not implemented", state, waitMode, timeoutAddr));
			Modules.ThreadManForUserModule.hleBlockCurrentThread(SceKernelThreadInfo.JPCSP_WAIT_USB);
			return 0;
		}

		int usbState = getUsbState();
		if (log.isDebugEnabled()) {
			log.debug(String.format("sceUsbWaitStateCB returning 0x%X", usbState));
		}
		return usbState;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x1C360735, version = 150)
	public int sceUsbWaitCancel() {
		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x8BFC3DE8, version = 150)
	public int sceUsb_8BFC3DE8(int callbackId, int unknown1, int unknown2) {
		// Registering a callback?
		if (Modules.ThreadManForUserModule.hleKernelRegisterCallback(SceKernelThreadInfo.THREAD_CALLBACK_USB, callbackId)) {
			this.callbackId = callbackId;
			notifyCallback();
		}

    	return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x89DE0DC5, version = 150)
	public int sceUsb_89DE0DC5(int callbackId) {
		// Unregistering a callback?
		if (this.callbackId == callbackId) {
			this.callbackId = -1;
		}

		return 0;
	}

	/**
	 * Activate a USB driver.
	 *
	 * @param pid      - Product ID for the default USB Driver
	 * @param charging - charging the PSP while the USB is connected?
	 *
	 * @return 0 on success
	 */
	@HLEFunction(nid = 0xE20B23A6, version = 150)
	public int sceUsbActivateWithCharging(int pid, boolean charging) {
		usbActivated = true;
		notifyCallback();

		// Used by usbpspcm
		if (pid == 0x1CB) {
			sceUsbPspcmModule.onUsbActivate(pid);
		}

		return 0;
	}

    @HLEUnimplemented
    @HLEFunction(nid = 0xEDA8A020, version = 150)
    public int sceUsbRestart(int delayMilliseconds) {
    	return 0;
    }
}