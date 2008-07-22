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
package jpcsp.Debugger;

/**
 *
 * @author George
 */
public class syscalls {
     static enum calls { 
               _sceKernelCreateThread(0x206d),
               _sceKernelStartThread(0x206f),
               _sceKernelExitThread(0x2071),
               _sceKernelUtilsMt19937Init(0x20bf)/*using 1.5firmware */;
    	
            //implement syscall
            private int value;
            calls(int val)
            {
                value=val;
            }
            int getValue()
            {
                return value;
            }
   }
}
