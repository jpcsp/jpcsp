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

/**
 * Access to the PSP Go 16GB internal memory (eflash)
 *
 */
public class sceEFlash extends HLEModule {
    public static Logger log = Modules.getLogger("sceEFlash");

    @HLEUnimplemented
    @HLEFunction(nid = 0x6ED51C3B, version = 661)
    public int sceEFlash_driver_6ED51C3B() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x8FB8591D, version = 661)
    public int sceEFlash_driver_8FB8591D() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x966AB475, version = 661)
    public int sceEFlash_driver_966AB475() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x63F1149A, version = 661)
    public int sceEFlash_driver_63F1149A() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x9317C91A, version = 661)
    public int sceEFlash_driver_9317C91A() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x01E0B50F, version = 661)
    public int sceEFlash_driver_01E0B50F() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x54C7E3BD, version = 661)
    public int sceEFlash_driver_54C7E3BD() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x9065E889, version = 661)
    public int sceEFlash_driver_9065E889() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x8411F4ED, version = 661)
    public int sceEFlash_driver_8411F4ED() {
    	return 0;
    }
}
