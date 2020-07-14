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
import jpcsp.HLE.TPointer32;

// PS1 Virtual Memory Card (VMC).
// See https://www.psdevwiki.com/ps3/PS1_Savedata
public class pspvmc extends HLEModule {
    public static Logger log = Modules.getLogger("pspvmc");
    private static final int PMV_MAGIC = 0x564D5000; // "\0PMV"

    @HLEUnimplemented
    @HLEFunction(nid = 0x38A87A12, version = 150)
    public int pspvmc_38A87A12(@BufferInfo(lengthInfo = LengthInfo.fixedLength, length = 128, usage = Usage.out) TPointer unknownBuffer) {
    	unknownBuffer.clear(128);
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x0820B291, version = 150)
    public int pspvmc_0820B291(@BufferInfo(lengthInfo = LengthInfo.fixedLength, length = 128, usage = Usage.inout) TPointer vmcBuffer, @BufferInfo(usage = Usage.out) TPointer32 vmcSizeAddr) {
    	if (vmcBuffer.getValue32(0) != PMV_MAGIC) {
    		return 0x80109021;
    	}
    	if (vmcBuffer.getValue32(8) != 0) {
    		return 0x80109022;
    	}
    	vmcSizeAddr.setValue(vmcBuffer.getValue32(4));

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x133FC171, version = 150)
    public int pspvmc_133FC171(int unknown) {
    	return 0;
    }
}
