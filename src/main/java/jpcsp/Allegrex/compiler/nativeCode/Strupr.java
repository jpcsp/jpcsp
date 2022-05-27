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
package jpcsp.Allegrex.compiler.nativeCode;

import jpcsp.memory.IMemoryReader;
import jpcsp.memory.IMemoryWriter;
import jpcsp.memory.MemoryReader;
import jpcsp.memory.MemoryWriter;

/**
 * @author gid15
 *
 */
public class Strupr extends AbstractNativeCodeSequence {
    static public void callCopyWithLength(int errorCode1, int errorCode2) {
        int srcAddr = getGprA0();
        int dstAddr = getGprA1();
        int length = getGprA2();

        int lengthSrc = getStrlen(srcAddr);
        if (lengthSrc > length) {
        	setGprV0((errorCode1 << 16) | errorCode2);
        	return;
        }

        IMemoryReader memoryReader = MemoryReader.getMemoryReader(srcAddr, length, 1);
        IMemoryWriter memoryWriter = MemoryWriter.getMemoryWriter(dstAddr, length, 1);
        for (int i = 0; i < lengthSrc; i++) {
        	int c = toUpperCase[memoryReader.readNext()];
        	memoryWriter.writeNext(c);
        }
        memoryWriter.writeNext(0);
        memoryWriter.flush();
    }
}
