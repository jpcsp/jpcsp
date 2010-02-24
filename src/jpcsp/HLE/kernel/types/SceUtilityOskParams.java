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

public class SceUtilityOskParams extends pspAbstractMemoryMappedStructure {
	public pspUtilityDialogCommon base;
	public int unknown;
	public int oskDataAddr;
	public SceUtilityOskData oskData;

	public static class SceUtilityOskData extends pspAbstractMemoryMappedStructure {
		public int language;
		public int lines;
		public int descAddr;
		public String desc;
		public int inTextAddr;
		public String inText;
		public int outTextLength;
		public int outTextAddr;
		public String outText;
		public int result;
		public int outTextLimit;

		@Override
		protected void read() {
			readUnknown(8);
			language = read32();
			readUnknown(8);
			lines = read32();
			readUnknown(4);
			descAddr = read32();
			desc = readStringUTF16Z(descAddr);
			inTextAddr = read32();
			inText = readStringUTF16Z(inTextAddr);
			outTextLength = read32();
			outTextAddr = read32();
			outText = readStringUTF16Z(outTextAddr);
			result = read32();
			outTextLimit = read32();
		}

		@Override
		protected void write() {
			writeUnknown(8);
			write32(language);
			writeUnknown(8);
			write32(lines);
			writeUnknown(4);
			write32(descAddr);
			writeStringUTF16Z(descAddr, desc);
			write32(inTextAddr);
			writeStringUTF16Z(inTextAddr, inText);
			outTextLength = writeStringUTF16Z(outTextAddr, outText);
			write32(outTextLength);
			write32(outTextAddr);
			write32(result);
			write32(outTextLimit);
		}
		
		@Override
		public int sizeof() {
			return 13 * 4;
		}
	}

	@Override
	protected void read() {
		base = new pspUtilityDialogCommon();
		read(base);
		setMaxSize(base.size);

		unknown = read32();
		oskDataAddr = read32();
		if (oskDataAddr != 0) {
			oskData = new SceUtilityOskData();
			oskData.read(mem, oskDataAddr);
		} else {
			oskData = null;
		}
		readUnknown(8);
	}

	@Override
	protected void write() {
		setMaxSize(base.size);
		write(base);

		write32(unknown);
		write32(oskDataAddr);
		if (oskData != null && oskDataAddr != 0) {
			oskData.write(mem, oskDataAddr);
		}
		writeUnknown(8);
	}

	@Override
	public int sizeof() {
		return base.size;
	}
}
