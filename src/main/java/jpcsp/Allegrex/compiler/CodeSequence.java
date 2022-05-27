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
package jpcsp.Allegrex.compiler;

import java.util.LinkedList;
import java.util.List;

public class CodeSequence implements Comparable<CodeSequence> {
    private int startAddress;
    private int endAddress;
    private LinkedList<CodeInstruction> codeInstructions = new LinkedList<CodeInstruction>();

    public CodeSequence(int startAddress) {
        this.startAddress = startAddress;
        this.endAddress = startAddress;
    }

    public int getStartAddress() {
        return startAddress;
    }

    public int getEndAddress() {
        return endAddress;
    }

    public void setEndAddress(int endAddress) {
        this.endAddress = endAddress;
    }

    public int getLength() {
        return ((endAddress - startAddress) >> 2) + 1;
    }

    public boolean isInside(int address) {
        return (startAddress <= address && address <= endAddress);
    }

    public void addInstruction(CodeInstruction codeInstruction) {
        codeInstructions.add(codeInstruction);
    }

    public List<CodeInstruction> getInstructions() {
        return codeInstructions;
    }

    public CodeInstruction getCodeInstruction(int address) {
        for (CodeInstruction codeInstruction : codeInstructions) {
            if (codeInstruction.getAddress() == address) {
                return codeInstruction;
            }
        }

        return null;
    }

    @Override
    public int compareTo(CodeSequence codeSequence) {
        if (codeSequence == null) {
            return -1;
        }

        int length1 = getLength();
        int length2 = codeSequence.getLength();

        if (length1 < length2) {
            return 1;
        } else if (length1 > length2) {
            return -1;
        }

        return 0;
    }

    @Override
    public String toString() {
        return String.format("CodeSequence 0x%X - 0x%X (length %d)", getStartAddress(), getEndAddress(), getLength());
    }
}
