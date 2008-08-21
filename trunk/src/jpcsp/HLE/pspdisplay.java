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

import java.nio.Buffer;
import java.nio.ByteBuffer;

import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.MemoryMap;

public class pspdisplay {
    private static pspdisplay instance;

    // PSP state
    private int mode, width, height;
    private int topaddr, bufferwidth, sync;
    private PspDisplayPixelFormats pixelformat;

    // HLE state, native rendering surface
    private byte[] framebufferram; // byte[] backs ByteBuffer
    private ByteBuffer bb; // ByteBuffer backs TextureData (sent to pspdisplay_frame)

    // This was just going to be a window, but JOGL requires all operations to
    // take place inside its own thread, so texture manipulations are currently
    // done in there. It is good enough for frame buffer emulation using simple
    // boolean flags to defer operations to execute in the JOGL thread, but will
    // need redesigning for when we want to emulate GU -> OpenGL.
    private pspdisplay_frame frame;

    private long lastUpdate;

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
        // TODO do it the netbeans way
        if (frame != null) {
            frame.cleanup();
            frame = null;
        }

        frame = new pspdisplay_frame();
    }

    public void step() {
        long now = System.currentTimeMillis();
        if (now - lastUpdate > 1000 / 60)
        {
            UpdateDisplay();
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

            // Allocate a 32-bit native frame buffer (we'll up sample as necessary)
            bb = ByteBuffer.allocate(512 * 512 * 4);
            framebufferram = bb.array();
            frame.createImage(bb);

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

        // TODO move this to step() and trigger at 60 frames per second
        //UpdateDisplay();

        Emulator.getProcessor().gpr[2] = 0;
    }


    private void set_native_pixel(byte[] buffer, int x, int y, byte r, byte g, byte b, byte a)
    {
        int addr = (x + y * bufferwidth) * 4;
        // OpenGL RGBA
        buffer[addr + 0] = r;
        buffer[addr + 1] = g;
        buffer[addr + 2] = b;
        buffer[addr + 3] = a;
    }

    public void UpdateDisplay() {
        if (pixelformat == null) {
            System.out.println("pixelformat is null");
            return;
        }

        // Convert memory at topaddr to native image
        int bytesPerPixel = pixelformat.getBytesPerPixel();
        byte[] videoram = Memory.get_instance().videoram;
        int addr = topaddr - MemoryMap.START_VRAM; // 0x04000000

        if (pixelformat == PspDisplayPixelFormats.PSP_DISPLAY_PIXEL_FORMAT_5551)
        {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++, addr += bytesPerPixel) {
                    int color =
                        (videoram[addr + 0] << 8) |
                        (videoram[addr + 1]);
                    byte r = (byte)(((color >> 11) & 0x1f) << 3);
                    byte g = (byte)(((color >>  6) & 0x1f) << 3);
                    byte b = (byte)(((color >>  1) & 0x1f) << 3);
                    byte a = (byte)(((color      ) & 0x01) * 255);
                    set_native_pixel(framebufferram, x, y, r, g, b, a);
                }
                addr += (bufferwidth - width) * bytesPerPixel;
            }
        }
        else if (pixelformat == PspDisplayPixelFormats.PSP_DISPLAY_PIXEL_FORMAT_565)
        {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++, addr += bytesPerPixel) {
                    int color =
                        (videoram[addr + 0] << 8) |
                        (videoram[addr + 1]);
                    byte r = (byte)(((color >> 11) & 0x1f) << 3);
                    byte g = (byte)(((color >>  5) & 0x20) << 2);
                    byte b = (byte)(((color      ) & 0x1f) << 3);
                    byte a = 0x00; // opaque
                    set_native_pixel(framebufferram, x, y, r, g, b, a);
                }
                addr += (bufferwidth - width) * bytesPerPixel;
            }
        }
        else if (pixelformat == PspDisplayPixelFormats.PSP_DISPLAY_PIXEL_FORMAT_4444)
        {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++, addr += bytesPerPixel) {
                    int color =
                        (videoram[addr + 0] << 8) |
                        (videoram[addr + 1]);
                    byte r = (byte)(((color >> 12) & 0x0f) << 4);
                    byte g = (byte)(((color >>  8) & 0x0f) << 4);
                    byte b = (byte)(((color >>  4) & 0x0f) << 4);
                    byte a = (byte)(((color      ) & 0x0f) << 4);
                    set_native_pixel(framebufferram, x, y, r, g, b, a);
                }
                addr += (bufferwidth - width) * bytesPerPixel;
            }
        }
        else if (pixelformat == PspDisplayPixelFormats.PSP_DISPLAY_PIXEL_FORMAT_8888)
        {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++, addr += bytesPerPixel) {
                    // minifire runs on 8888
                    byte r = videoram[addr + 0];
                    byte g = videoram[addr + 1];
                    byte b = videoram[addr + 2];
                    byte a = videoram[addr + 3];
                    set_native_pixel(framebufferram, x, y, r, g, b, a);
                }
                addr += (bufferwidth - width) * bytesPerPixel;
            }
        }


        // TESTING
        // alpha component doesn't seem to make any difference, but then i didnt set a blend func
        //set_native_pixel(framebufferram, 0, 0, (byte)255, (byte)0, (byte)0, (byte)128);
        //set_native_pixel(framebufferram, 479, 271, (byte)0, (byte)255, (byte)0, (byte)128);

        frame.updateImage();
    }

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
