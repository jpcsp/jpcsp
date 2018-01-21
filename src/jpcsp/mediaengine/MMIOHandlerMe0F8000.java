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
package jpcsp.mediaengine;

public class MMIOHandlerMe0F8000 extends MMIOHandlerMeBase {
	private int unknown000;
	private int unknown004;
	private int unknown008;
	private int unknown00C;
	private int unknown074;
	private int unknown088;
	private int unknown08C;
	private int unknown094;
	private int unknown09C;
	private int unknown0A0;
	private int unknown0A4;
	private int unknown0AC;
	private int unknown0B4;
	private int unknown0B8;
	private int unknown0BC;
	private int unknown0C4;
	private int unknown0E4;
	private int unknown0E8;
	private int unknown0F4;
	private int unknown0FC;
	private int unknown100;
	private int unknown118;
	private int unknown12C;
	private int unknown130;
	private int unknown144;
	private int unknown148;
	private int unknown174;
	private int unknown178;
	private int unknown18C;
	private int unknown190;

	public MMIOHandlerMe0F8000(int baseAddress) {
		super(baseAddress);
	}

	@Override
	public void write32(int address, int value) {
		switch (address - baseAddress) {
			case 0x000: unknown000 = value; break;
			case 0x004: unknown004 = value; break;
			case 0x008: unknown008 = value; break;
			case 0x00C: unknown00C = value; break;
			case 0x074: unknown074 = value; break;
			case 0x088: unknown088 = value; break;
			case 0x08C: unknown08C = value; break;
			case 0x094: unknown094 = value; break;
			case 0x09C: unknown09C = value; break;
			case 0x0A0: unknown0A0 = value; break;
			case 0x0A4: unknown0A4 = value; break;
			case 0x0AC: unknown0AC = value; break;
			case 0x0B4: unknown0B4 = value; break;
			case 0x0B8: unknown0B8 = value; break;
			case 0x0BC: unknown0BC = value; break;
			case 0x0C4: unknown0C4 = value; break;
			case 0x0E4: unknown0E4 = value; break;
			case 0x0E8: unknown0E8 = value; break;
			case 0x0F4: unknown0F4 = value; break;
			case 0x0FC: unknown0FC = value; break;
			case 0x100: unknown100 = value; break;
			case 0x118: unknown118 = value; break;
			case 0x12C: unknown12C = value; break;
			case 0x130: unknown130 = value; break;
			case 0x144: unknown144 = value; break;
			case 0x148: unknown148 = value; break;
			case 0x174: unknown174 = value; break;
			case 0x178: unknown178 = value; break;
			case 0x18C: unknown18C = value; break;
			case 0x190: unknown190 = value; break;
			default: super.write32(address, value); break;
		}

		if (log.isTraceEnabled()) {
			log.trace(String.format("0x%08X - write32(0x%08X, 0x%08X) on %s", getPc() - 4, address, value, this));
		}
	}

	@Override
	public String toString() {
		StringBuilder s = new StringBuilder("MMIOHandlerMe0F8000 ");

		s.append(String.format(  "unknown000=0x%X", unknown000));
		s.append(String.format(", unknown004=0x%X", unknown004));
		s.append(String.format(", unknown008=0x%X", unknown008));
		s.append(String.format(", unknown00C=0x%X", unknown00C));
		s.append(String.format(", unknown074=0x%X", unknown074));
		s.append(String.format(", unknown088=0x%X", unknown088));
		s.append(String.format(", unknown08C=0x%X", unknown08C));
		s.append(String.format(", unknown094=0x%X", unknown094));
		s.append(String.format(", unknown09C=0x%X", unknown09C));
		s.append(String.format(", unknown0A0=0x%X", unknown0A0));
		s.append(String.format(", unknown0A4=0x%X", unknown0A4));
		s.append(String.format(", unknown0AC=0x%X", unknown0AC));
		s.append(String.format(", unknown0B4=0x%X", unknown0B4));
		s.append(String.format(", unknown0B8=0x%X", unknown0B8));
		s.append(String.format(", unknown0BC=0x%X", unknown0BC));
		s.append(String.format(", unknown0C4=0x%X", unknown0C4));
		s.append(String.format(", unknown0E4=0x%X", unknown0E4));
		s.append(String.format(", unknown0E8=0x%X", unknown0E8));
		s.append(String.format(", unknown0F4=0x%X", unknown0F4));
		s.append(String.format(", unknown0FC=0x%X", unknown0FC));
		s.append(String.format(", unknown100=0x%X", unknown100));
		s.append(String.format(", unknown118=0x%X", unknown118));
		s.append(String.format(", unknown12C=0x%X", unknown12C));
		s.append(String.format(", unknown130=0x%X", unknown130));
		s.append(String.format(", unknown144=0x%X", unknown144));
		s.append(String.format(", unknown148=0x%X", unknown148));
		s.append(String.format(", unknown174=0x%X", unknown174));
		s.append(String.format(", unknown178=0x%X", unknown178));
		s.append(String.format(", unknown18C=0x%X", unknown18C));
		s.append(String.format(", unknown190=0x%X", unknown190));

		return s.toString();
	}
}
