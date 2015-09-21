package jpcsp.HLE.modules;

import org.apache.log4j.Logger;

import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.Modules;

public class sceMeCore_driver extends HLEModule {
	public static Logger log = Modules.getLogger("sceMeCore_driver");

	
	@HLEUnimplemented
    @HLEFunction(nid = 0x5DFF5C50, version = 660)
    public int sceMeBootStart660(int unknown) {
    	return 0;
    }

	@HLEUnimplemented
    @HLEFunction(nid = 0x99E4DBFA, version = 635)
    public int sceMeBootStart635(int unknown) {
    	return 0;
    }
	
	@HLEUnimplemented
    @HLEFunction(nid = 0x3A2E60BB, version = 620)
    public int sceMeBootStart620(int unknown) {
    	return 0;
    }
	
	@HLEUnimplemented
    @HLEFunction(nid = 0x051C1601, version = 500)
    public int sceMeBootStart500(int unknown) {
    	return 0;
    }
	
	@HLEUnimplemented
    @HLEFunction(nid = 0x8988AD49, version = 395)
    public int sceMeBootStart395(int unknown) {
    	return 0;
    }

	@HLEUnimplemented
    @HLEFunction(nid = 0xD857CF93, version = 380)
    public int sceMeBootStart380(int unknown) {
    	return 0;
    }
	@HLEUnimplemented
    @HLEFunction(nid = 0xC287AD90, version = 371)
    public int sceMeBootStart371(int unknown) {
    	return 0;
    }
	
	@HLEUnimplemented
    @HLEFunction(nid = 0x47DB48C2, version = 150)
    public int sceMeBootStart(int unknown) {
    	return 0;
    }
}
