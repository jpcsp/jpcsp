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
 * Parameter Structure for sceUsbCamSetupStill().
 */
public class pspUsbCamSetupStillParam extends pspAbstractMemoryMappedStructureVariableLength {
	public int resolution; // Resolution. One of PSP_USBCAM_RESOLUTION_*
	public int jpegsize;   // Size of the jpeg image
	public int reverseflags; // Reverse effect to apply. Zero or more of PSP_USBCAM_FLIP, PSP_USBCAM_MIRROR
	public int delay;        // Delay to apply to take the picture. One of PSP_USBCAM_DELAY_*
	public int complevel;    // JPEG compression level, a value from 1-63.
                             // 1 -> less compression, better quality;
	                         // 63 -> max compression, worse quality

	@Override
	protected void read() {
		super.read();
		resolution = read32();
		jpegsize = read32();
		reverseflags = read32();
		delay = read32();
		complevel = read32();
	}

	@Override
	protected void write() {
		super.write();
		write32(resolution);
		write32(jpegsize);
		write32(reverseflags);
		write32(delay);
		write32(complevel);
	}

	@Override
	public String toString() {
		return String.format("pspUsbCamSetupStillParam[size=%d, resolution=%d, jpegsize=%d, reverseflags=%d, delay=%d, complevel=%d]", sizeof(), resolution, jpegsize, reverseflags, delay, complevel);
	}
}
