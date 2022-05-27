package jpcsp.HLE.modules;

import org.apache.log4j.Logger;

import jpcsp.HLE.BufferInfo;
import jpcsp.HLE.CanBeNull;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.BufferInfo.LengthInfo;
import jpcsp.HLE.BufferInfo.Usage;
import jpcsp.crypto.AMCTRL;
import jpcsp.crypto.CryptoEngine;

public class sceAmctrl extends HLEModule {
    public static Logger log = Modules.getLogger("sceAmctrl");
    private CryptoEngine crypto = new CryptoEngine();

    @HLEFunction(nid = 0x525B8218, version = 150)
    public int sceDrmBBMacInit(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=40, usage=Usage.out) TPointer ctxAddr, int mode) {
    	AMCTRL.BBMac_Ctx ctx = new AMCTRL.BBMac_Ctx();

    	int result = crypto.getAMCTRLEngine().hleDrmBBMacInit(ctx, mode);

    	ctx.write(ctxAddr);

    	return result;
    }

    @HLEFunction(nid = 0x58163FBE, version = 150)
    public int sceDrmBBMacUpdate(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=40, usage=Usage.inout) TPointer ctxAddr, @BufferInfo(lengthInfo=LengthInfo.nextParameter, usage=Usage.in) TPointer data, int size) {
    	AMCTRL.BBMac_Ctx ctx = new AMCTRL.BBMac_Ctx();
    	ctx.read(ctxAddr);

    	byte[] bytes = data.getArray8(size);
    	int result = crypto.getAMCTRLEngine().hleDrmBBMacUpdate(ctx, bytes, size);

    	ctx.write(ctxAddr);

    	return result;
    }

    @HLEFunction(nid = 0xEF95A213, version = 150)
    public int sceDrmBBMacFinal(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=40, usage=Usage.inout) TPointer ctxAddr, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=16, usage=Usage.out) TPointer hash, @CanBeNull @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=16, usage=Usage.in) TPointer key) {
    	AMCTRL.BBMac_Ctx ctx = new AMCTRL.BBMac_Ctx();
    	ctx.read(ctxAddr);

    	byte[] hashBytes = new byte[16];

    	byte[] keyBytes = null;
    	if (key.isNotNull()) {
    		keyBytes = key.getArray8(16);
    	}

    	int result = crypto.getAMCTRLEngine().hleDrmBBMacFinal(ctx, hashBytes, keyBytes);

    	hash.setArray(hashBytes);

    	ctx.write(ctxAddr);

    	return result;
    }

    @HLEFunction(nid = 0xF5186D8E, version = 150)
    public int sceDrmBBMacFinal2(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=40, usage=Usage.inout) TPointer ctxAddr, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=16, usage=Usage.in) TPointer hash, @CanBeNull @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=16, usage=Usage.in) TPointer key) {
    	AMCTRL.BBMac_Ctx ctx = new AMCTRL.BBMac_Ctx();
    	ctx.read(ctxAddr);

    	byte[] hashBytes = hash.getArray8(16);

    	byte[] keyBytes = null;
    	if (key.isNotNull()) {
    		keyBytes = key.getArray8(16);
    	}

    	int result = crypto.getAMCTRLEngine().hleDrmBBMacFinal2(ctx, hashBytes, keyBytes);

    	result = Modules.scePopsManModule.hookDrmBBMacFinal2(ctx, result);

    	ctx.write(ctxAddr);

    	return result;
    }

    @HLEFunction(nid = 0x1CCB66D2, version = 150)
    public int sceDrmBBCipherInit(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=24, usage=Usage.inout) TPointer ctxAddr, int encMode, int genMode, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=16, usage=Usage.inout) TPointer data, @CanBeNull @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=16, usage=Usage.in) TPointer key, int seed) {
    	AMCTRL.BBCipher_Ctx ctx = new AMCTRL.BBCipher_Ctx();
    	ctx.read(ctxAddr);

    	byte[] dataBytes = null;
    	if (data.isNotNull()) {
    		dataBytes = data.getArray8(16);
    	}

    	byte[] keyBytes = null;
    	if (key.isNotNull()) {
    		keyBytes = key.getArray8(16);
    	}

    	int result = crypto.getAMCTRLEngine().hleDrmBBCipherInit(ctx, encMode, genMode, dataBytes, keyBytes, seed);

    	data.setArray(dataBytes);

    	ctx.write(ctxAddr);

    	return result;
    }

    @HLEFunction(nid = 0x0785C974, version = 150)
    public int sceDrmBBCipherUpdate(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=24, usage=Usage.inout) TPointer ctxAddr, @BufferInfo(lengthInfo=LengthInfo.nextParameter, usage=Usage.inout) TPointer data, int dataLength) {
    	AMCTRL.BBCipher_Ctx ctx = new AMCTRL.BBCipher_Ctx();
    	ctx.read(ctxAddr);

    	byte[] dataBytes = data.getArray8(dataLength);

    	int result = crypto.getAMCTRLEngine().hleDrmBBCipherUpdate(ctx, dataBytes, dataLength);

    	data.setArray(dataBytes);

    	ctx.write(ctxAddr);

    	return result;
    }

    @HLEFunction(nid = 0x9951C50F, version = 150)
    public int sceDrmBBCipherFinal(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=24, usage=Usage.out) TPointer ctxAddr) {
    	AMCTRL.BBCipher_Ctx ctx = new AMCTRL.BBCipher_Ctx();
    	ctx.read(ctxAddr);

    	int result = crypto.getAMCTRLEngine().hleDrmBBCipherFinal(ctx);

    	ctx.write(ctxAddr);

    	return result;
    }
}
