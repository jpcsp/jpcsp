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

import org.apache.log4j.Logger;

import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.Modules;

public class sceGpio extends HLEModule {
    public static Logger log = Modules.getLogger("sceGpio");

    @HLEUnimplemented
    @HLEFunction(nid = 0x317D9D2C, version = 150)
    public int sceGpioSetPortMode(int port, int mode) {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xFBC85E74, version = 660)
    public int sceGpioSetIntrMode(int interruptNumber, int mode) {
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x4250D44A, version = 150)
    public int sceGpioPortRead() {
    	// Has no parameters
        return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x103C3EB2, version = 150)
    public void sceGpioPortClear(int mask) {
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x310F0CCF, version = 150)
    public void sceGpioPortSet(int mask) {
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x1A730F20, version = 660)
    public int sceGpioAcquireIntr(int interruptNumber) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x95D7F3B8, version = 660)
    public int sceGpioDisableIntr() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x0C5B2EAF, version = 660)
    public int sceGpioDisableTimerCapture() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x2605F404, version = 660)
    public int sceGpioEnableTimerCapture() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x1BB49B65, version = 660)
    public int sceGpioQueryIntr() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x2B280C43, version = 660)
    public int sceGpioGetPortMode() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x2FFF2F8B, version = 660)
    public int sceGpioPortInvert() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x4DE658CC, version = 660)
    public int sceGpioGetCapturePort() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x9C4CFE04, version = 660)
    public int sceGpioSetCapturePort() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xA21E44CB, version = 660)
    public int sceGpioGetIntrMode() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xEF1F40F5, version = 660)
    public int sceGpioEnableIntr() {
    	return 0;
    }
}
