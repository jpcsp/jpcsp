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

public class sceClockgen extends HLEModule {
    public static Logger log = Modules.getLogger("sceClockgen");

    @HLEUnimplemented
    @HLEFunction(nid = 0x0FD28D8B, version = 150)
    public int sceClockgenGetRegValue(int index) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x29160F5D, version = 150)
    public int sceClockgenInit() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x36F9B49D, version = 150)
    public int sceClockgenEnd() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x3F6B7C6B, version = 150)
    public int sceClockgenSetProtocol(int prot) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x50F22765, version = 150)
    public int sceClockgenSetup() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x7FF82F6F, version = 150)
    public int sceClockgenLeptonClkEnable() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xA1D23B2C, version = 150)
    public int sceClockgenAudioClkEnable() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xC9AF3102, version = 150)
    public int sceClockgenSetSpectrumSpreading(int mode) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xCE36529C, version = 150)
    public int sceClockgenGetRevision() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xDAB6E612, version = 150)
    public int sceClockgenAudioClkSetFreq(int freq) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xDBE5F283, version = 150)
    public int sceClockgenLeptonClkDisable() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xDED4C698, version = 150)
    public int sceClockgenAudioClkDisable() {
    	return 0;
    }
}
