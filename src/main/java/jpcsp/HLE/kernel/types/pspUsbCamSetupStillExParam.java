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
 * Parameter Structure for sceUsbCamSetupStillEx().
 */
public class pspUsbCamSetupStillExParam extends pspAbstractMemoryMappedStructureVariableLength {
	public int unknown1;   // Unknown, set it to 9 at the moment.
	public int resolution; // Resolution. One of PSP_USBCAM_RESOLUTION_EX_*
	public int jpegsize;   // Size of the jpeg image
	public int complevel;  // JPEG compression level, a value from 1-63.
                           // 1 -> less compression, better quality;
                           // 63 -> max compression, worse quality
	public int unknown2;   // Unknown, set it to 0 at the moment.
	public int unknown3;   // Unknown, set it to 1 at the moment.
	public int flip;       // Flag that indicates whether to flip the image
	public int mirror;     // Flag that indicates whether to mirror the image
	public int delay;      // Delay to apply to take the picture. One of PSP_USBCAM_DELAY_*
	public int unknown4;   // Unknown, set it to 0 at the moment.
	public int unknown5;   // Unknown, set it to 0 at the moment.
	public int unknown6;   // Unknown, set it to 0 at the moment.
	public int unknown7;   // Unknown, set it to 0 at the moment.
	public int unknown8;   // Unknown, set it to 0 at the moment.

	@Override
	protected void read() {
		super.read();
		unknown1 = read32();
		resolution = read32();
		jpegsize = read32();
		complevel = read32();
		unknown2 = read32();
		unknown3 = read32();
		flip = read32();
		mirror = read32();
		delay = read32();
		unknown4 = read32();
		unknown5 = read32();
		unknown6 = read32();
		unknown7 = read32();
		unknown8 = read32();
	}

	@Override
	protected void write() {
		super.write();
		write32(unknown1);
		write32(resolution);
		write32(jpegsize);
		write32(complevel);
		write32(unknown2);
		write32(unknown3);
		write32(flip);
		write32(mirror);
		write32(delay);
		write32(unknown4);
		write32(unknown5);
		write32(unknown6);
		write32(unknown7);
		write32(unknown8);
	}

	@Override
	public String toString() {
		return String.format("pspUsbCamSetupStillExParam[size=%d, resolution=%d, jpegsize=%d, complevel=%d, flip=%d, mirror=%d, delay=%d]", sizeof(), resolution, jpegsize, complevel, flip, mirror, delay);
	}
}
