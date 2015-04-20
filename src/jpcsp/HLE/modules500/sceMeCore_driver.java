package jpcsp.HLE.modules500;

import org.apache.log4j.Logger;

import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLELogging;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.Modules;
import jpcsp.HLE.modules.HLEModule;

@HLELogging
public class sceMeCore_driver extends HLEModule {
	public static Logger log = Modules.getLogger("sceMeCore_driver");

	@Override
	public String getName() {
		return "sceMeCore_driver";
	}

	@HLEUnimplemented
    @HLEFunction(nid = 0x051C1601, version = 500)
    public int sceMeBootStart500(int unknown) {
    	return 0;
    }
}
