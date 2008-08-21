/*
Function:
- HLE everything in http://psp.jim.sh/pspsdk-doc/pspctrl_8h.html


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
import jpcsp.Memory;

public class pspctrl {
    private static pspctrl instance;

    private int cycle;
    private int mode; // PspCtrlMode { PSP_CTRL_MODE_DIGITAL = 0, PSP_CTRL_MODE_ANALOG  }

    public static pspctrl get_instance() {
        if (instance == null) {
            instance = new pspctrl();
        }
        return instance;
    }

    private pspctrl() {
    }

    public void sceCtrlSetSamplingCycle(int a0)
    {
        Emulator.getProcessor().gpr[2] = cycle;
        cycle = a0;
    }

    public void sceCtrlGetSamplingCycle(int a0)
    {
        Memory.get_instance().write32(a0, cycle);
        Emulator.getProcessor().gpr[2] = 0;
    }

    public void sceCtrlSetSamplingMode(int a0)
    {
        Emulator.getProcessor().gpr[2] = mode;
        mode = a0;
    }

    public void sceCtrlGetSamplingMode(int a0)
    {
        Memory.get_instance().write32(a0, mode);
        Emulator.getProcessor().gpr[2] = 0;
    }

    public void sceCtrlPeekBufferPositive(int a0, int a1)
    {
        Memory mem = Memory.get_instance();
        int i;

        for (i = 0; i < a1; i++) {
            // TODO set timestamp, get buttons and analog state, probably from jpcsp.Controller class
            int TimeStamp = 0;
            int Buttons = 0;
            byte Lx = (byte)128;
            byte Ly = (byte)128;

            mem.write32(a0, TimeStamp);
            mem.write32(a0 + 4, Buttons);
            mem.write8(a0 + 8, Lx);
            mem.write8(a0 + 9, Ly);
            a0 += 16;
        }

        Emulator.getProcessor().gpr[2] = i;
    }

    public void sceCtrlPeekBufferNegative(int a0, int a1)
    {
        Memory mem = Memory.get_instance();
        int i;

        for (i = 0; i < a1; i++) {
            // TODO set timestamp, get buttons and analog state
            int TimeStamp = 0;
            int Buttons = 0;
            byte Lx = (byte)128;
            byte Ly = (byte)128;

            mem.write32(a0, TimeStamp);
            mem.write32(a0 + 4, ~Buttons);
            mem.write8(a0 + 8, Lx);
            mem.write8(a0 + 9, Ly);
            a0 += 16;
        }

        Emulator.getProcessor().gpr[2] = i;
    }

    public void sceCtrlReadBufferPositive(int a0, int a1)
    {
        Memory mem = Memory.get_instance();
        int i;

        for (i = 0; i < a1; i++) {
            // TODO set timestamp, get buttons and analog state
            int TimeStamp = 0;
            int Buttons = 0;
            byte Lx = (byte)128;
            byte Ly = (byte)128;

            mem.write32(a0, TimeStamp);
            mem.write32(a0 + 4, Buttons);
            mem.write8(a0 + 8, Lx);
            mem.write8(a0 + 9, Ly);
            a0 += 16;
        }

        Emulator.getProcessor().gpr[2] = i;
    }

    public void sceCtrlReadBufferNegative(int a0, int a1)
    {
        Memory mem = Memory.get_instance();
        int i;

        for (i = 0; i < a1; i++) {
            // TODO set timestamp, get buttons and analog state
            int TimeStamp = 0;
            int Buttons = 0;
            byte Lx = (byte)128;
            byte Ly = (byte)128;

            mem.write32(a0, TimeStamp);
            mem.write32(a0 + 4, ~Buttons);
            mem.write8(a0 + 8, Lx);
            mem.write8(a0 + 9, Ly);
            a0 += 16;
        }

        Emulator.getProcessor().gpr[2] = i;
    }

    private class SceCtrlData {
        private int TimeStamp;
        private int Buttons;
        private byte Lx;
        private byte Ly;
        private byte[] Rsrv; // 6 bytes

        private SceCtrlData(int TimeStamp, int Buttons, byte Lx, byte Ly) {
            this.TimeStamp = TimeStamp;
            this.Buttons = Buttons;
            this.Lx = Lx;
            this.Ly = Ly;
            // no use allocating this
            //Rsrv = new byte[6];
        }

        private void write(Memory mem, int address) {
            mem.write32(address, TimeStamp);
            mem.write32(address + 4, Buttons);
            mem.write8(address + 8, Lx);
            mem.write8(address + 9, Ly);
            // leaving Rsrv uninitialised
        }

        public int sizeof() {
            return 16;
        }
    }
}
