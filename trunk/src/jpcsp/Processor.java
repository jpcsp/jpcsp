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
package jpcsp;

public class Processor {

    public int pc;
    public int hi,  lo;
    public int cpuregisters[] = new int[32];//32 base registers

    Processor() {
        Memory.get_instance(); //intialaze memory
        reset();

    }

    private void reset() {
        //intialaze psp
        pc = 0x00000000;
        hi = lo = 0;
        for (int i = 0; i < 32; i++) {
            cpuregisters[i] = 0;//reset registers
        }

    }
}
