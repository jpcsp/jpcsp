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

import jpcsp.HLE.BufferInfo;
import jpcsp.HLE.BufferInfo.LengthInfo;
import jpcsp.HLE.BufferInfo.Usage;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLEModule;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.Modules;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer32;
import jpcsp.memory.IMemoryReader;
import jpcsp.memory.IMemoryWriter;
import jpcsp.memory.MemoryReader;
import jpcsp.memory.MemoryWriter;

public class sceMesgd extends HLEModule {
    public static Logger log = Modules.getLogger("sceMesgd");

    public int sceMesgd_driver_102DC8AF(byte[] buffer, int bufferOffset, int bufferSize, TPointer32 resultSizeAddr) {
    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0x102DC8AF, version = 150)
    public int sceMesgd_driver_102DC8AF(@BufferInfo(lengthInfo=LengthInfo.nextParameter, usage=Usage.inout) TPointer buffer, int bufferSize, @BufferInfo(usage=Usage.out) TPointer32 resultSizeAddr) {
    	byte[] bytes = new byte[bufferSize];
    	IMemoryReader memoryReader = MemoryReader.getMemoryReader(buffer, bufferSize, 1);
    	for (int i = 0; i < bufferSize; i++) {
    		bytes[i] = (byte) memoryReader.readNext();
    	}

    	int result = sceMesgd_driver_102DC8AF(bytes, 0, bufferSize, resultSizeAddr);
    	if (result != 0) {
    		return result;
    	}

    	int resultSize = resultSizeAddr.getValue();
    	IMemoryWriter memoryWriter = MemoryWriter.getMemoryWriter(buffer, resultSize, 1);
    	for (int i = 0; i < resultSize; i++) {
    		memoryWriter.writeNext(bytes[i]);
    	}
    	memoryWriter.flush();

    	return 0;
    }

    @HLEUnimplemented
    @HLEFunction(nid = 0xADD0CB66, version = 150)
    public int sceMesgd_driver_ADD0CB66() {
    	return 0;
    }
}
