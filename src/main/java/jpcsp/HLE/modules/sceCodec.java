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

public class sceCodec extends HLEModule {
    public static Logger log = Modules.getLogger("sceCodec");

    @HLEUnimplemented
    @HLEFunction(nid = 0xBD8E0977, version = 150)
    public int sceCodecInitEntry() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x02133959, version = 150)
    public int sceCodecStopEntry() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x856E7487, version = 150)
    public int sceCodecOutputEnable() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x359C2B9F, version = 150)
    public int sceCodecOutputDisable() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xC513C747, version = 150)
    public int sceCodecInputEnable() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x31B2E41E, version = 150)
    public int sceCodecInputDisable() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x261C6EE8, version = 150)
    public int sceCodecSetOutputVolume() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x6D945509, version = 150)
    @HLEFunction(nid = 0x49C13ACF, version = 660)
    public int sceCodecSetHeadphoneVolume() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x40D5C897, version = 150)
    @HLEFunction(nid = 0xEACF7284, version = 660)
    public int sceCodecSetSpeakerVolume() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xDFBCACF3, version = 150)
    public int sceCodecSetFrequency() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x56494D70, version = 150)
    public int sceCodec_driver_56494D70() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x4515AE04, version = 150)
    public int sceCodec_driver_4515AE04() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xEEB91526, version = 150)
    @HLEFunction(nid = 0xD27707A8, version = 660)
    public int sceCodecSetVolumeOffset() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x3064C53D, version = 150)
    public int sceCodec_driver_3064C53D() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x20C61103, version = 150)
    @HLEFunction(nid = 0xE4456BC3, version = 660)
    public int sceCodecSelectVolumeTable() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xFCA6D35B, version = 660)
    public int sceCodec_driver_FCA6D35B() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x277DFFB6, version = 660)
    public int sceCodec_driver_277DFFB6() {
    	return 0;
    }

    @HLEFunction(nid = 0x376399B6, version = 660)
    public void sceCodec_driver_376399B6(boolean enable) {
    	if (enable) {
    		Modules.sceSysregModule.sceSysregAudioClkoutClkEnable();
    	} else {
    		Modules.sceSysregModule.sceSysregAudioClkoutClkDisable();
    	}
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x6FFC0FA4, version = 660)
    public int sceCodec_driver_6FFC0FA4() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xA88FD064, version = 660)
    public int sceCodec_driver_A88FD064() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xE61A4623, version = 660)
    public int sceCodec_driver_E61A4623() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xFC355DE0, version = 660)
    public int sceCodec_driver_FC355DE0() {
    	return 0;
    }
}
