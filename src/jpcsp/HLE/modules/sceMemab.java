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

import java.util.Random;

import org.apache.log4j.Logger;

import jpcsp.Allegrex.compiler.RuntimeContext;
import jpcsp.HLE.BufferInfo;
import jpcsp.HLE.BufferInfo.LengthInfo;
import jpcsp.HLE.BufferInfo.Usage;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointer;

public class sceMemab extends HLEModule {
    public static Logger log = Modules.getLogger("sceMemab");
    private Random random = new Random();

    @HLEUnimplemented
    @HLEFunction(nid = 0x6DD7339A, version = 150)
    public int sceMemab_6DD7339A() {
    	// Has no parameters
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xD57856A7, version = 150)
    public int sceMemab_D57856A7() {
    	// Has no parameters
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xF742F283, version = 150)
    public int sceMemab_F742F283(int unknown1, int unknown2, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=16, usage=Usage.out) TPointer unknown3, int unknown4) {
    	unknown3.clear(16);

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x4B54EAAD, version = 150)
    public int sceMemab_4B54EAAD(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=192, usage=Usage.out) TPointer unknownOutput1, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=160, usage=Usage.out) TPointer unknownOutput2) {
    	unknownOutput1.clear(192);
    	RuntimeContext.debugMemory(unknownOutput1.getAddress(), 192);
    	unknownOutput2.clear(160);
    	RuntimeContext.debugMemory(unknownOutput2.getAddress(), 160);

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x9BF0C95D, version = 150)
    public int sceMemab_9BF0C95D(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=192, usage=Usage.out) TPointer unknownOutput1, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=160, usage=Usage.in) TPointer unknownInput, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=272, usage=Usage.out) TPointer unknownOutput2) {
    	RuntimeContext.debugMemory(unknownInput.getAddress(), 160);
    	unknownOutput1.clear(192);
    	RuntimeContext.debugMemory(unknownOutput1.getAddress(), 192);
    	unknownOutput2.clear(272);
    	RuntimeContext.debugMemory(unknownOutput2.getAddress(), 272);

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xC3981EE1, version = 150)
    public int sceMemab_C3981EE1(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=192, usage=Usage.inout) TPointer unknownInputOuput, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=272, usage=Usage.in) TPointer unknownInput, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=256, usage=Usage.out) TPointer unknownOutput) {
    	unknownInputOuput.clear(192);
    	RuntimeContext.debugMemory(unknownInputOuput.getAddress(), 192);
    	RuntimeContext.debugMemory(unknownInput.getAddress(), 272);
    	unknownOutput.clear(256);
    	RuntimeContext.debugMemory(unknownOutput.getAddress(), 256);

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x8ABE3445, version = 150)
    public int sceMemab_8ABE3445(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=192, usage=Usage.out) TPointer unknownOutput, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=96, usage=Usage.in) TPointer unknownInput) {
    	unknownOutput.clear(192);
    	RuntimeContext.debugMemory(unknownOutput.getAddress(), 192);
    	RuntimeContext.debugMemory(unknownInput.getAddress(), 96);

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x23E4659B, version = 150)
    public int sceMemab_23E4659B(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=192, usage=Usage.inout) TPointer unknownInputOutput1, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=256, usage=Usage.inout) TPointer unknownInputOutput2, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=96, usage=Usage.out) TPointer unknownOutput) {
    	unknownInputOutput1.clear(192);
    	RuntimeContext.debugMemory(unknownInputOutput1.getAddress(), 192);
    	unknownInputOutput2.clear(256);
    	RuntimeContext.debugMemory(unknownInputOutput2.getAddress(), 256);
    	unknownOutput.clear(96);
    	RuntimeContext.debugMemory(unknownOutput.getAddress(), 96);

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xCB5D3916, version = 150)
    public int sceMemab_CB5D3916(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=60, usage=Usage.inout) TPointer unknownInputOutput, @BufferInfo(lengthInfo=LengthInfo.nextParameter, usage=Usage.in) TPointer unknownInput, int inputLength, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=32, usage=Usage.out) TPointer unknownOutput, int unknown1, int unknown2) {
    	unknownInputOutput.clear(60);
    	RuntimeContext.debugMemory(unknownInputOutput.getAddress(), 60);
    	RuntimeContext.debugMemory(unknownInput.getAddress(), inputLength);
    	unknownOutput.clear(32);
    	RuntimeContext.debugMemory(unknownOutput.getAddress(), 32);

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xD47A50B1, version = 150)
    public int sceMemab_D47A50B1(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=76, usage=Usage.inout) TPointer unknownInputOutput, @BufferInfo(lengthInfo=LengthInfo.nextParameter, usage=Usage.in) TPointer unknownInput, int inputLength, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=32, usage=Usage.out) TPointer unknownOutput, int unknown1, int unknown2) {
    	unknownInputOutput.clear(76);
    	RuntimeContext.debugMemory(unknownInputOutput.getAddress(), 76);
    	RuntimeContext.debugMemory(unknownInput.getAddress(), inputLength);
    	unknownOutput.clear(32);
    	RuntimeContext.debugMemory(unknownOutput.getAddress(), 32);

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x3C15BC8C, version = 150)
    public int sceMemab_3C15BC8C(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=68, usage=Usage.inout) TPointer unknownInputOutput, @BufferInfo(lengthInfo=LengthInfo.nextParameter, usage=Usage.in) TPointer unknownInput, int inputLength, @BufferInfo(lengthInfo=LengthInfo.fixedLength, length=16, usage=Usage.in) TPointer unknownInput2, int unknown2) {
    	unknownInputOutput.clear(68);
    	RuntimeContext.debugMemory(unknownInputOutput.getAddress(), 68);
    	RuntimeContext.debugMemory(unknownInput.getAddress(), inputLength);
    	RuntimeContext.debugMemory(unknownInput2.getAddress(), 16);

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x16594684, version = 150)
    public int sceMemab_16594684(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=16, usage=Usage.out) TPointer buffer) {
    	// Generates 4 pseudo-random numbers (PSP_KIRK_CMD_PRNG)
    	for (int i = 0; i < 4; i++) {
    		buffer.setValue32(i << 2, random.nextInt());
    	}

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x9DE8C8CD, version = 150)
    public int sceMemab_9DE8C8CD(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=16, usage=Usage.in) TPointer xorKey, @BufferInfo(lengthInfo=LengthInfo.nextParameter, usage=Usage.inout) TPointer buffer, int bufferLength) {
    	// Encrypting (PSP_KIRK_CMD_ENCRYPT) the data in unknownInputOutput buffer
    	RuntimeContext.debugMemory(buffer.getAddress(), bufferLength);

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x9BF1A0A4, version = 150)
    public int sceMemab_9BF1A0A4(@BufferInfo(lengthInfo=LengthInfo.fixedLength, length=16, usage=Usage.in) TPointer xorKey, @BufferInfo(lengthInfo=LengthInfo.nextParameter, usage=Usage.inout) TPointer buffer, int bufferLength) {
    	// Decrypting (PSP_KIRK_CMD_DECRYPT) the data in unknownInputOutput buffer
    	RuntimeContext.debugMemory(buffer.getAddress(), bufferLength);

    	return 0;
    }
}
