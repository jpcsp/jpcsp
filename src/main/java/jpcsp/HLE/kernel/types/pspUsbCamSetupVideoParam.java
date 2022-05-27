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

/*
 * Parameter Structure for sceUsbCamSetupVideo().
 */
public class pspUsbCamSetupVideoParam extends pspAbstractMemoryMappedStructureVariableLength {
	public int resolution; // Resolution. One of PSP_USBCAM_RESOLUTION_*
	public int framerate;  // Framerate. One of PSP_USBCAM_FRAMERATE_*
	public int wb;         // White balance. One of PSP_USBCAM_WB_*
	public int saturation; // Saturation (0-255)
	public int brightness; // Brightness (0-255)
	public int contrast;   // Contrast (0-255)
	public int sharpness;  // Sharpness (0-255)
	public int effectmode; // Effect mode. One of PSP_USBCAM_EFFECTMODE_*
	public int framesize;  // Size of jpeg video frame
	public int unknown;    // Unknown. Set it to 0 at the moment.
	public int evlevel;    // Exposure value. One of PSP_USBCAM_EVLEVEL_*

	@Override
	protected void read() {
		super.read();
		resolution = read32();
		framerate = read32();
		wb = read32();
		saturation = read32();
		brightness = read32();
		contrast = read32();
		sharpness = read32();
		effectmode = read32();
		framesize = read32();
		unknown = read32();
		evlevel = read32();
	}

	@Override
	protected void write() {
		super.write();
		write32(resolution);
		write32(framerate);
		write32(wb);
		write32(saturation);
		write32(brightness);
		write32(contrast);
		write32(sharpness);
		write32(effectmode);
		write32(framesize);
		write32(unknown);
		write32(evlevel);
	}

	@Override
	public String toString() {
		return String.format("pspUsbCamSetupVideoParam[size=%d, resolution=%d, framerate=%d, wb=%d, saturation=%d, brightness=%d, contrast=%d, sharpness=%d, effectmode=%d, framesize=%d, evlevel=%d]", sizeof(), resolution, framerate, wb, saturation, brightness, contrast, sharpness, effectmode, framesize, evlevel);
	}
}
