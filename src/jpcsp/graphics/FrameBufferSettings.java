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
package jpcsp.graphics;

import static jpcsp.graphics.VideoEngineUtilities.getPixelFormatBytes;

import java.nio.Buffer;

import jpcsp.Memory;

/**
 * Simple class containing the attributes related to a display frame buffer.
 * 
 * @author gid15
 *
 */
public class FrameBufferSettings {
    private int topAddr;
    private int bottomAddr;
    private int bufferWidth;
    private int width;
    private int height;
    private int pixelFormat;
    private Buffer pixels;
    private int size;

    public FrameBufferSettings(int topAddr, int bufferWidth, int width, int height, int pixelFormat) {
        this.topAddr = topAddr & Memory.addressMask;
        this.bufferWidth = bufferWidth;
        this.width = width;
        this.height = height;
        this.pixelFormat = pixelFormat;
        update();
    }

    public FrameBufferSettings(FrameBufferSettings copy) {
        topAddr = copy.topAddr;
        bottomAddr = copy.bottomAddr;
        bufferWidth = copy.bufferWidth;
        width = copy.width;
        height = copy.height;
        pixelFormat = copy.pixelFormat;
        pixels = copy.pixels;
        size = copy.size;
    }

    private void update() {
        size = bufferWidth * height * getPixelFormatBytes(pixelFormat);
        bottomAddr = topAddr + size;
        pixels = Memory.getInstance().getBuffer(topAddr, size);
    }

    public int getTopAddr() {
        return topAddr;
    }

    public int getBottomAddr() {
        return bottomAddr;
    }

    public int getBufferWidth() {
        return bufferWidth;
    }

    public int getPixelFormat() {
        return pixelFormat;
    }

    public Buffer getPixels() {
        return pixels;
    }

    public Buffer getPixels(int topAddr) {
    	if (this.topAddr == topAddr) {
    		return pixels;
    	}
    	return Memory.getInstance().getBuffer(topAddr, size);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getSize() {
    	return size;
    }

    public boolean isRawAddressInside(int address) {
        // vram address is lower than main memory so check the end of the buffer first, it's more likely to fail
        return address >= topAddr && address < bottomAddr;
    }

    public boolean isAddressInside(int address) {
        return isRawAddressInside(address & Memory.addressMask);
    }

    public void setDimension(int width, int height) {
        this.width = width;
        this.height = height;
        update();
    }

    @Override
    public String toString() {
        return String.format("0x%08X-0x%08X, %dx%d, bufferWidth=%d, pixelFormat=%d", topAddr, bottomAddr, width, height, bufferWidth, pixelFormat);
    }
}
