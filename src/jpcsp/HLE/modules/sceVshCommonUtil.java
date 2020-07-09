package jpcsp.HLE.modules;

import org.apache.log4j.Logger;

import jpcsp.HLE.BufferInfo;
import jpcsp.HLE.CanBeNull;
import jpcsp.HLE.BufferInfo.LengthInfo;
import jpcsp.HLE.BufferInfo.Usage;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointer;

public class sceVshCommonUtil extends HLEModule {
    public static Logger log = Modules.getLogger("sceVshCommonUtil");

    @HLEUnimplemented
    @HLEFunction(nid = 0x649C3568, version = 150)
    public int sceVshCommonUtil_649C3568() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x91838DED, version = 150)
    public int sceVshCommonUtil_91838DED() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x91B254B2, version = 150)
    public int sceVshCommonUtil_91B254B2() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x937E715D, version = 150)
    public int sceVshCommonUtil_937E715D() {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xD3A016E1, version = 150)
    public int sceVshCommonUtil_D3A016E1(int characterSet, @CanBeNull @BufferInfo(lengthInfo = LengthInfo.fixedLength, length = 36, usage = Usage.out) TPointer unknownBuffer) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xF9E12DAA, version = 150)
    public int sceVshCommonUtil_F9E12DAA() {
    	return 0;
    }
}
