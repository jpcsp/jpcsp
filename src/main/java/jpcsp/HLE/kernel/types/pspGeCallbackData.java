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
package jpcsp.HLE.kernel.types;

public class pspGeCallbackData extends pspAbstractMemoryMappedStructure
{
    /** GE callback for the signal interrupt */
    public int signalFunction;
    /** GE callback argument for signal interrupt */
    public int signalArgument;
    /** GE callback for the finish interrupt */
    public int finishFunction;
    /** GE callback argument for finish interrupt */
    public int finishArgument;

    @Override
    protected void read() {
        signalFunction = read32();
        signalArgument = read32();
        finishFunction = read32();
        finishArgument = read32();
    }

    @Override
    public int sizeof() {
        return 4 * 4;
    }

    @Override
    protected void write() {
        write32(signalFunction);
        write32(signalArgument);
        write32(finishFunction);
        write32(finishArgument);
    }
}
