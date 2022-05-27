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

import java.io.IOException;

import jpcsp.state.StateInputStream;
import jpcsp.state.StateOutputStream;

public class MMIOHandlerMe0F8000 extends MMIOHandlerMeBase {
	private static final int STATE_VERSION = 0;
	private int unknown000;
	private int unknown004;
	private int unknown008;
	private int unknown00C;
	private int unknown074;
	private int unknown084;
	private int unknown088;
	private int unknown08C;
	private int unknown094;
	private int unknown09C;
	private int unknown0A0;
	private int unknown0A4;
	private int unknown0AC;
	private int unknown0B0;
	private int unknown0B4;
	private int unknown0B8;
	private int unknown0BC;
	private int unknown0C4;
	private int unknown0CC;
	private int unknown0E4;
	private int unknown0E8;
	private int unknown0EC;
	private int unknown0F4;
	private int unknown0F8;
	private int unknown0FC;
	private int unknown100;
	private int unknown118;
	private int unknown12C;
	private int unknown130;
	private int unknown134;
	private int unknown13C;
	private int unknown140;
	private int unknown144;
	private int unknown148;
	private int unknown174;
	private int unknown178;
	private int unknown17C;
	private int unknown184;
	private int unknown188;
	private int unknown18C;
	private int unknown190;

	public MMIOHandlerMe0F8000(int baseAddress) {
		super(baseAddress);
	}

	@Override
	public void read(StateInputStream stream) throws IOException {
		stream.readVersion(STATE_VERSION);
		unknown000 = stream.readInt();
		unknown004 = stream.readInt();
		unknown008 = stream.readInt();
		unknown00C = stream.readInt();
		unknown074 = stream.readInt();
		unknown084 = stream.readInt();
		unknown088 = stream.readInt();
		unknown08C = stream.readInt();
		unknown094 = stream.readInt();
		unknown09C = stream.readInt();
		unknown0A0 = stream.readInt();
		unknown0A4 = stream.readInt();
		unknown0AC = stream.readInt();
		unknown0B0 = stream.readInt();
		unknown0B4 = stream.readInt();
		unknown0B8 = stream.readInt();
		unknown0BC = stream.readInt();
		unknown0C4 = stream.readInt();
		unknown0CC = stream.readInt();
		unknown0E4 = stream.readInt();
		unknown0E8 = stream.readInt();
		unknown0EC = stream.readInt();
		unknown0F4 = stream.readInt();
		unknown0F8 = stream.readInt();
		unknown0FC = stream.readInt();
		unknown100 = stream.readInt();
		unknown118 = stream.readInt();
		unknown12C = stream.readInt();
		unknown130 = stream.readInt();
		unknown134 = stream.readInt();
		unknown13C = stream.readInt();
		unknown140 = stream.readInt();
		unknown144 = stream.readInt();
		unknown148 = stream.readInt();
		unknown174 = stream.readInt();
		unknown178 = stream.readInt();
		unknown17C = stream.readInt();
		unknown184 = stream.readInt();
		unknown188 = stream.readInt();
		unknown18C = stream.readInt();
		unknown190 = stream.readInt();
		super.read(stream);
	}

	@Override
	public void write(StateOutputStream stream) throws IOException {
		stream.writeVersion(STATE_VERSION);
		stream.writeInt(unknown000);
		stream.writeInt(unknown004);
		stream.writeInt(unknown008);
		stream.writeInt(unknown00C);
		stream.writeInt(unknown074);
		stream.writeInt(unknown084);
		stream.writeInt(unknown088);
		stream.writeInt(unknown08C);
		stream.writeInt(unknown094);
		stream.writeInt(unknown09C);
		stream.writeInt(unknown0A0);
		stream.writeInt(unknown0A4);
		stream.writeInt(unknown0AC);
		stream.writeInt(unknown0B0);
		stream.writeInt(unknown0B4);
		stream.writeInt(unknown0B8);
		stream.writeInt(unknown0BC);
		stream.writeInt(unknown0C4);
		stream.writeInt(unknown0CC);
		stream.writeInt(unknown0E4);
		stream.writeInt(unknown0E8);
		stream.writeInt(unknown0EC);
		stream.writeInt(unknown0F4);
		stream.writeInt(unknown0F8);
		stream.writeInt(unknown0FC);
		stream.writeInt(unknown100);
		stream.writeInt(unknown118);
		stream.writeInt(unknown12C);
		stream.writeInt(unknown130);
		stream.writeInt(unknown134);
		stream.writeInt(unknown13C);
		stream.writeInt(unknown140);
		stream.writeInt(unknown144);
		stream.writeInt(unknown148);
		stream.writeInt(unknown174);
		stream.writeInt(unknown178);
		stream.writeInt(unknown17C);
		stream.writeInt(unknown184);
		stream.writeInt(unknown188);
		stream.writeInt(unknown18C);
		stream.writeInt(unknown190);
		super.write(stream);
	}

	@Override
	public void write32(int address, int value) {
		switch (address - baseAddress) {
			case 0x000: unknown000 = value; break;
			case 0x004: unknown004 = value; break;
			case 0x008: unknown008 = value; break;
			case 0x00C: unknown00C = value; break;
			case 0x074: unknown074 = value; break;
			case 0x084: unknown084 = value; break;
			case 0x088: unknown088 = value; break;
			case 0x08C: unknown08C = value; break;
			case 0x094: unknown094 = value; break;
			case 0x09C: unknown09C = value; break;
			case 0x0A0: unknown0A0 = value; break;
			case 0x0A4: unknown0A4 = value; break;
			case 0x0AC: unknown0AC = value; break;
			case 0x0B0: unknown0B0 = value; break;
			case 0x0B4: unknown0B4 = value; break;
			case 0x0B8: unknown0B8 = value; break;
			case 0x0BC: unknown0BC = value; break;
			case 0x0C4: unknown0C4 = value; break;
			case 0x0CC: unknown0CC = value; break;
			case 0x0E4: unknown0E4 = value; break;
			case 0x0E8: unknown0E8 = value; break;
			case 0x0EC: unknown0EC = value; break;
			case 0x0F4: unknown0F4 = value; break;
			case 0x0F8: unknown0F8 = value; break;
			case 0x0FC: unknown0FC = value; break;
			case 0x100: unknown100 = value; break;
			case 0x118: unknown118 = value; break;
			case 0x12C: unknown12C = value; break;
			case 0x130: unknown130 = value; break;
			case 0x134: unknown134 = value; break;
			case 0x13C: unknown13C = value; break;
			case 0x140: unknown140 = value; break;
			case 0x144: unknown144 = value; break;
			case 0x148: unknown148 = value; break;
			case 0x174: unknown174 = value; break;
			case 0x178: unknown178 = value; break;
			case 0x17C: unknown17C = value; break;
			case 0x184: unknown184 = value; break;
			case 0x188: unknown188 = value; break;
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
		s.append(String.format(", unknown084=0x%X", unknown084));
		s.append(String.format(", unknown088=0x%X", unknown088));
		s.append(String.format(", unknown08C=0x%X", unknown08C));
		s.append(String.format(", unknown094=0x%X", unknown094));
		s.append(String.format(", unknown09C=0x%X", unknown09C));
		s.append(String.format(", unknown0A0=0x%X", unknown0A0));
		s.append(String.format(", unknown0A4=0x%X", unknown0A4));
		s.append(String.format(", unknown0AC=0x%X", unknown0AC));
		s.append(String.format(", unknown0B0=0x%X", unknown0B0));
		s.append(String.format(", unknown0B4=0x%X", unknown0B4));
		s.append(String.format(", unknown0B8=0x%X", unknown0B8));
		s.append(String.format(", unknown0BC=0x%X", unknown0BC));
		s.append(String.format(", unknown0C4=0x%X", unknown0C4));
		s.append(String.format(", unknown0CC=0x%X", unknown0CC));
		s.append(String.format(", unknown0E4=0x%X", unknown0E4));
		s.append(String.format(", unknown0E8=0x%X", unknown0E8));
		s.append(String.format(", unknown0EC=0x%X", unknown0EC));
		s.append(String.format(", unknown0F4=0x%X", unknown0F4));
		s.append(String.format(", unknown0F8=0x%X", unknown0F8));
		s.append(String.format(", unknown0FC=0x%X", unknown0FC));
		s.append(String.format(", unknown100=0x%X", unknown100));
		s.append(String.format(", unknown118=0x%X", unknown118));
		s.append(String.format(", unknown12C=0x%X", unknown12C));
		s.append(String.format(", unknown130=0x%X", unknown130));
		s.append(String.format(", unknown134=0x%X", unknown134));
		s.append(String.format(", unknown13C=0x%X", unknown13C));
		s.append(String.format(", unknown140=0x%X", unknown140));
		s.append(String.format(", unknown144=0x%X", unknown144));
		s.append(String.format(", unknown148=0x%X", unknown148));
		s.append(String.format(", unknown174=0x%X", unknown174));
		s.append(String.format(", unknown178=0x%X", unknown178));
		s.append(String.format(", unknown17C=0x%X", unknown17C));
		s.append(String.format(", unknown184=0x%X", unknown184));
		s.append(String.format(", unknown188=0x%X", unknown188));
		s.append(String.format(", unknown18C=0x%X", unknown18C));
		s.append(String.format(", unknown190=0x%X", unknown190));

		return s.toString();
	}
}
