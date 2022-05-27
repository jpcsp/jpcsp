package jpcsp.HLE.modules;

import org.apache.log4j.Logger;

import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.Modules;

public class sceLFatFs extends HLEModule {
    public static Logger log = Modules.getLogger("sceLFatFs");

    @HLEUnimplemented
	@HLEFunction(nid = 0x8F0560E0, version = 150)
	public int sceLfatfsStop() {
    	return 0;
	}

    @HLEUnimplemented
	@HLEFunction(nid = 0x933F6E29, version = 150)
	public int sceLfatfsWaitReady() {
    	return 0;
	}
}
