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
    private int uiPress = 0;
    private int uiRelease = 1;

    private byte Lx;
    private byte Ly;
    private int Buttons;
    
    public final static int PSP_CTRL_SELECT = 0x000001;
    public final static int PSP_CTRL_START = 0x000008;
    public final static int PSP_CTRL_UP = 0x000010;
    public final static int PSP_CTRL_RIGHT = 0x000020;
    public final static int PSP_CTRL_DOWN = 0x000040;
    public final static int PSP_CTRL_LEFT = 0x000080;
    public final static int PSP_CTRL_LTRIGGER = 0x000100;
    public final static int PSP_CTRL_RTRIGGER = 0x000200;
    public final static int PSP_CTRL_TRIANGLE = 0x001000;
    public final static int PSP_CTRL_CIRCLE = 0x002000;
    public final static int PSP_CTRL_CROSS = 0x004000;
    public final static int PSP_CTRL_SQUARE = 0x008000;
    public final static int PSP_CTRL_HOME = 0x010000;
    public final static int PSP_CTRL_HOLD = 0x020000;
    public final static int PSP_CTRL_NOTE = 0x800000;
    public final static int PSP_CTRL_SCREEN = 0x400000;
    public final static int PSP_CTRL_VOLUP = 0x100000;
    public final static int PSP_CTRL_VOLDOWN = 0x200000;
    public final static int PSP_CTRL_WLAN_UP = 0x040000;
    public final static int PSP_CTRL_REMOTE = 0x080000;
    public final static int PSP_CTRL_DISC = 0x1000000;
    public final static int PSP_CTRL_MS = 0x2000000;
  
    public static pspctrl get_instance() {
        if (instance == null) {
            instance = new pspctrl();
        }
        return instance;
    }

    private pspctrl() {
    }
    
    public void setButtons(byte Lx, byte Ly, int Buttons, boolean pressed)
    {
        this.Lx = Lx;
        this.Ly = Ly;
        this.Buttons = Buttons;
        
        if (pressed) {
            this.uiRelease = 0;
            this.uiPress = 1;
        } else {
            this.uiRelease = 1;
            this.uiPress = 0;
        }
    }
    
    public boolean isModeDigital() {
        if (mode == 0)
            return true;
        return false;
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

            mem.write32(a0, TimeStamp);
            mem.write32(a0 + 4, ~Buttons);
            mem.write8(a0 + 8, Lx);
            mem.write8(a0 + 9, Ly);
            a0 += 16;
        }

        Emulator.getProcessor().gpr[2] = i;
    }
    
    public void sceCtrlPeekLatch(int a0) {
        Memory mem = Memory.get_instance();
        
        mem.write32(a0, 0);             //uiMake
        mem.write32(a0 +4, 0);          //uiBreak
        mem.write32(a0 +8, uiPress);
        mem.write32(a0 +12, uiRelease);
        Emulator.getProcessor().gpr[2] = 0;
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
