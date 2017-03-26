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

public class SceKernelGameInfo extends pspAbstractMemoryMappedStructureVariableLength {
	public static final int SIZEOF = 220;
	public int flags;
	public String str8;
	public String str24;
	public int unk36;
	public String qtgp2;
	public String qtgp3;
	public int allowReplaceUmd;
	public String gameId;
	public int unk84;
	public String str88;
	public int umdCacheOn;
	public int sdkVersion;
	public int compilerVersion;
	public int dnas;
	public int unk112;
	public String str116;
	public String str180;
	public String str196;
	public String unk204;
	public int unk212;
	public int unk216;

	@Override
	protected void write() {
		super.write();

		write32(flags);
		writeStringNZ(16, str8);
		writeStringNZ(11, str24);
		write8((byte) 0); // Padding
		write32(unk36);
		writeStringNZ(8, qtgp2);
		writeStringNZ(16, qtgp3);
		write32(allowReplaceUmd);
		writeStringNZ(14, gameId);
		write8((byte) 0); // Padding
		write8((byte) 0); // Padding
		write32(unk84);
		writeStringNZ(8, str88);
		write32(umdCacheOn);
		write32(sdkVersion);
		write32(compilerVersion);
		write32(dnas);
		write32(unk112);
		writeStringNZ(64, str116);
		writeStringNZ(11, str180);
		write8((byte) 0); // Padding
		write8((byte) 0); // Padding
		write8((byte) 0); // Padding
		write8((byte) 0); // Padding
		write8((byte) 0); // Padding
		writeStringNZ(8, str196);
		writeStringNZ(8, unk204);
		write32(unk212);
		write32(unk216);
	}
}
