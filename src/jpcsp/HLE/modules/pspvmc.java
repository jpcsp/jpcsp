package jpcsp.HLE.modules;

import org.apache.log4j.Logger;

import jpcsp.HLE.BufferInfo;
import jpcsp.HLE.BufferInfo.LengthInfo;
import jpcsp.HLE.BufferInfo.Usage;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointer;

public class pspvmc extends HLEModule {
    public static Logger log = Modules.getLogger("pspvmc");

    @HLEUnimplemented
    @HLEFunction(nid = 0x38A87A12, version = 150)
    public int pspvmc_38A87A12(@BufferInfo(lengthInfo = LengthInfo.fixedLength, length = 128, usage = Usage.out) TPointer unknownBuffer) {
    	unknownBuffer.clear(128);
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x0820B291, version = 150)
    public int pspvmc_0820B291(int unknown0, int unknown1) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x133FC171, version = 150)
    public int pspvmc_133FC171(int unknown) {
    	return 0;
    }
}
