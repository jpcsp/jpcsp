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
package jpcsp.HLE.modules150;

import jpcsp.HLE.CanBeNull;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLELogging;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.PspString;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer32;

import org.apache.log4j.Logger;

import jpcsp.HLE.Modules;
import jpcsp.HLE.modules.HLEModule;

@HLELogging
public class sceUsb extends HLEModule {
    public static Logger log = Modules.getLogger("sceUsb");

	@Override
	public String getName() {
		return "sceUsb";
	}

	public static final String PSP_USBBUS_DRIVERNAME = "USBBusDriver";

	public static final int PSP_USB_CONNECTION_ESTABLISHED = 0x002;
	public static final int PSP_USB_CABLE_CONNECTED = 0x020;
	public static final int PSP_USB_ACTIVATED = 0x200;

	protected boolean usbActivated = false;
	protected boolean usbStarted = false;

	protected int getUsbState() {
		// Simulate that a USB cacle is always connected
		int state = PSP_USB_CABLE_CONNECTED;

		// USB has been activated?
		if (usbActivated) {
			state |= PSP_USB_ACTIVATED;
		}

		// USB has been started?
		if (usbStarted) {
			state |= PSP_USB_CONNECTION_ESTABLISHED;
		}

		return state;
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
	@HLEUnimplemented
	@HLEFunction(nid = 0xAE5DE6AF, version = 150)
	public int sceUsbStart(PspString driverName, int size, @CanBeNull TPointer args) {
		usbStarted = true;

		return 0;
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
	@HLEUnimplemented
	@HLEFunction(nid = 0xC2464FA0, version = 150)
	public int sceUsbStop(PspString driverName, int size, @CanBeNull TPointer args) {
		usbStarted = false;

		return 0;
	}

	/**
	 * Get USB state
	 *
	 * @return OR'd PSP_USB_* constants
	 */
	@HLEFunction(nid = 0xC21645A4, version = 150)
	public int sceUsbGetState() {
		if (log.isDebugEnabled()) {
			log.debug(String.format("sceUsbGetState returning %d", getUsbState()));
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
	@HLEUnimplemented
	@HLEFunction(nid = 0x586DB82C, version = 150)
	public int sceUsbActivate(int pid) {
		usbActivated = true;

		return 0;
	}

	/**
	 * Deactivate USB driver.
	 *
	 * @param pid - Product ID for the default USB driver
	 *
	 * @return 0 on success
	 */
	@HLEUnimplemented
	@HLEFunction(nid = 0xC572A9C8, version = 150)
	public int sceUsbDeactivate(int pid) {
		usbActivated = false;

		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x5BE0E002, version = 150)
	public int sceUsbWaitState(int state, int waitMode, @CanBeNull TPointer32 timeoutAddr) {
		return 0;
	}

	@HLEUnimplemented
	@HLEFunction(nid = 0x1C360735, version = 150)
	public int sceUsbWaitCancel() {
		return 0;
	}
}