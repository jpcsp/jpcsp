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

public class pspUsbCamSetupMicParam extends pspAbstractMemoryMappedStructureVariableLength {
	public int unknown1;
	public int gain;
	public int unknown2;
	public int frequency;

	@Override
	protected void read() {
		super.read();
		unknown1 = read32();
		gain = read32();
		unknown2 = read32();
		frequency = read32();
	}

	@Override
	protected void write() {
		super.write();
		write32(unknown1);
		write32(gain);
		write32(unknown2);
		write32(frequency);
	}
}
