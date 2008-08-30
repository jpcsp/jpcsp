/*
Function:
- HLE everything in http://psp.jim.sh/pspsdk-doc/pspdisplay_8h.html


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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import jpcsp.Emulator;
import jpcsp.MainGUI;
import jpcsp.Memory;
import jpcsp.MemoryMap;

public class pspdisplay {
    private static pspdisplay instance;

    // PSP state
    private int mode, width, height;
    private int topaddr, bufferwidth, sync;
    private PspDisplayPixelFormats pixelformat;

    // HLE state, native rendering surface
    private ByteBuffer bb; // ByteBuffer backs TextureData (sent to pspdisplay_frame)

    // This was just going to be a window, but JOGL requires all operations to
    // take place inside its own thread, so texture manipulations are currently
    // done in there. It is good enough for frame buffer emulation using simple
    // boolean flags to defer operations to execute in the JOGL thread, but will
    // need redesigning for when we want to emulate GU -> OpenGL.
    //private pspdisplay_frame frame;
    private pspdisplay_glcanvas frame;
    private long lastUpdate;
    private long lastWrite;
    private int lastWriteCount;
    private int bottomaddr;
    private boolean refreshRequired;

    public static pspdisplay get_instance() {
        if (instance == null) {
            instance = new pspdisplay();
        }
        return instance;
    }

    private pspdisplay() {
    }

    public void Initialise() {
        // Create a window here
        // TODO do it the netbeans way (internal window)
        if (frame != null) {
            //frame.cleanup();
            frame = null;
        }
        //frame = new pspdisplay_frame();
        //frame = new pspdisplay_glcanvas().get_instance();

        // If we initialise these here we can remove checks for uninitialised variables later on
        mode = 0;
        width = 480;
        height = 272;
        topaddr = MemoryMap.START_VRAM;
        bufferwidth = 512;
        pixelformat = PspDisplayPixelFormats.PSP_DISPLAY_PIXEL_FORMAT_8888;
        sync = 0;
        bottomaddr = topaddr + bufferwidth * height * pixelformat.getBytesPerPixel();

        updateDispSettings();
    }

    private void updateDispSettings() {
        byte[] all = Memory.get_instance().videoram.array();

        bb = ByteBuffer.wrap(
            all,
            Memory.get_instance().videoram.arrayOffset() + topaddr - MemoryMap.START_VRAM,
            bottomaddr - topaddr).slice();
        bb.order(ByteOrder.LITTLE_ENDIAN);

        pspdisplay_glcanvas.get_instance().updateDispSettings(
            bb, width, height, bufferwidth, pixelformat.getValue(), topaddr);
    }

    public void step() {
        long now = System.currentTimeMillis();
        if (now - lastUpdate > 1000 / 60)
        {
            if (refreshRequired /*&&
                // Stopped writing to vram 10th second ago, or written enough times to get a complete frame
                (now - lastWrite > 100 || lastWriteCount >= 480 * 272)*/) {
                pspdisplay_glcanvas.get_instance().updateImage();
                //lastWriteCount = 0;
                refreshRequired = false;
            }
            lastUpdate = now;
        }
    }

    public void sceDisplaySetMode(int mode, int width, int height)
    {
        System.out.println("sceDisplaySetMode(mode=" + mode + ",width=" + width + ",height=" + height + ")");

        if (width <= 0 || height <= 0) {
            Emulator.getProcessor().gpr[2] = -1;
        } else {
            this.mode = mode;
            this.width = width;
            this.height = height;

            bottomaddr = topaddr + bufferwidth * height * pixelformat.getBytesPerPixel();
            refreshRequired = true;

            Emulator.getProcessor().gpr[2] = 0;
        }
    }

    public void sceDisplaySetFrameBuf(int topaddr, int bufferwidth, int pixelformat, int sync)
    {
        //System.out.println("sceDisplaySetFrameBuf");

        // Discard the kernel/cache bits
        topaddr = topaddr & 0x3fffffff;

        if (topaddr < MemoryMap.START_VRAM || topaddr >= MemoryMap.END_VRAM ||
            bufferwidth <= 0 || // TODO power of 2 check
            pixelformat < 0 || pixelformat > 3 ||
            sync < 0 || sync > 1)
        {
            Emulator.getProcessor().gpr[2] = -1;
        }
        else
        {
            this.topaddr = topaddr; // 0x44000000
            this.bufferwidth = bufferwidth; // power of 2 TODO check and return -1 on failure
            this.pixelformat = PspDisplayPixelFormats.findValue(pixelformat);
            this.sync = sync;

            bottomaddr = topaddr + bufferwidth * height * this.pixelformat.getBytesPerPixel();
            refreshRequired = true;

            //System.out.println("sceDisplaySetFrameBuf topaddr=" + Integer.toHexString(topaddr) + " psm=" + this.pixelformat + " bpp=" + this.pixelformat.getBytesPerPixel());
            updateDispSettings();

            Emulator.getProcessor().gpr[2] = 0;
        }
    }

    public void sceDisplayWaitVblankStart()
    {
        //System.out.println("sceDisplayWaitVblankStart");

        int micros;

        // TODO calculate time to next frame in microseconds, with 60 frames per second
        micros = 1;
        //ThreadMan.get_instance().ThreadMan_sceKernelDelayThread(micros);

        Emulator.getProcessor().gpr[2] = 0;
    }

    public void write8(int address, int data) {
        address &= 0x3fffffff;
        //if (address >= MemoryMap.START_VRAM && address <= MemoryMap.END_VRAM)
        {
            if (address >= topaddr && address < bottomaddr) {
                refreshRequired = true;
            }

            //lastWrite = System.currentTimeMillis();
            //lastWriteCount++;
        }
    }

    public void write16(int address, int data) {
        address &= 0x3fffffff;
        //if (address >= MemoryMap.START_VRAM && address <= MemoryMap.END_VRAM)
        {
            if (address >= topaddr && address < bottomaddr) {
                refreshRequired = true;
            }

            //lastWrite = System.currentTimeMillis();
            //lastWriteCount++;
        }
    }

    public void write32(int address, int data) {
        address &= 0x3fffffff;
        //if (address >= MemoryMap.START_VRAM && address <= MemoryMap.END_VRAM)
        {
            if (address >= topaddr && address < bottomaddr) {
                refreshRequired = true;
            }

            //lastWrite = System.currentTimeMillis();
            //lastWriteCount++;
        }
    }

    // Do we really need this enum? it's just overhead
    public enum PspDisplayPixelFormats {
        PSP_DISPLAY_PIXEL_FORMAT_565(0, 2),
        PSP_DISPLAY_PIXEL_FORMAT_5551(1, 2),
        PSP_DISPLAY_PIXEL_FORMAT_4444(2, 2),
        PSP_DISPLAY_PIXEL_FORMAT_8888(3, 4);

        private int value;
        private int bytesPerPixel;

        private PspDisplayPixelFormats(int value, int bytesPerPixel) {
            this.value = value;
            this.bytesPerPixel = bytesPerPixel;
        }

        public int getValue() {
            return value;
        }

        public int getBytesPerPixel() {
            return bytesPerPixel;
        }

        public static PspDisplayPixelFormats findValue(int pixelformat) {
            // int -> enum via reverse lookup, Java enums are stupid
            for (PspDisplayPixelFormats format : PspDisplayPixelFormats.values()) {
                if (format.getValue() == pixelformat) {
                    return format;
                }
            }
            return PSP_DISPLAY_PIXEL_FORMAT_8888;
        }
    }
}
