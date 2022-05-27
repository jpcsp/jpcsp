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

public class sceG729 extends HLEModule {
    public static Logger log = Modules.getLogger("sceG729");

    @HLEUnimplemented
    @HLEFunction(nid = 0x13F1028A, version = 150)
    public int sceG729DecodeExit() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x17C11696, version = 150)
    public int sceG729DecodeInitResource() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x3489D1F3, version = 150)
    public int sceG729DecodeCore() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x55E14F75, version = 150)
    public int sceG729DecodeInit() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x5A409D1B, version = 150)
    public int sceG729EncodeExit() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x74804D93, version = 150)
    public int sceG729DecodeReset() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x890B86AE, version = 150)
    public int sceG729DecodeTermResource() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x8C87A2CA, version = 150)
    public int sceG729EncodeReset() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x94714D50, version = 150)
    public int sceG729EncodeTermResource() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xAA1E5462, version = 150)
    public int sceG729EncodeInitResource() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xCFCD367C, version = 150)
    public int sceG729EncodeInit() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xDB7259D5, version = 150)
    public int sceG729EncodeCore() {
    	return 0;
    }
}
