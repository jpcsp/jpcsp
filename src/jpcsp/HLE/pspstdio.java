/*
Function:
- HLE everything in http://psp.jim.sh/pspsdk-doc/pspstdio_8h.html


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
package jpcsp.HLE;

import jpcsp.Emulator;

/**
 *
 * @author shadow
 */
public class pspstdio {
    private static pspstdio instance;
    public static pspstdio get_instance() {
        if (instance == null) {
            instance = new pspstdio();
        }
        return instance;
    }
   public void sceKernelStdin()//Function to get the current standard in file no.
   {
       Emulator.getProcessor().cpu.gpr[2] =3;//not sure if it returns okay but seems so
   }
   public void sceKernelStdout ()//Function to get the current standard out file no.
   {
       Emulator.getProcessor().cpu.gpr[2] =1;//not sure if it returns okay but seems so
   }
   public void sceKernelStderr ()//Function to get the current standard err file no. 
   {
       Emulator.getProcessor().cpu.gpr[2] =2;//not sure if it returns okay but seems so
   }
   
}
