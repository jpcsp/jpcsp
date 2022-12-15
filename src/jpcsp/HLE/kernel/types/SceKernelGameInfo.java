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
		super.write(); // Size at offset 0

		write32(flags); // Offset 4
		writeStringNZ(16, str8); // Offset 8
		writeStringNZ(11, str24); // Offset 24
		write8((byte) 0); // Padding
		write32(unk36); // Offset 36
		writeStringNZ(8, qtgp2); // Offset 40
		writeStringNZ(16, qtgp3); // Offset 48
		write32(allowReplaceUmd); // Offset 64
		writeStringNZ(14, gameId); // Offset 68
		write8((byte) 0); // Padding
		write8((byte) 0); // Padding
		write32(unk84); // Offset 84
		writeStringNZ(8, str88); // Offset 88
		write32(umdCacheOn); // Offset 96
		write32(sdkVersion); // Offset 100
		write32(compilerVersion); // Offset 104
		write32(dnas); // Offset 108
		write32(unk112); // Offset 112
		writeStringNZ(64, str116); // Offset 116
		writeStringNZ(11, str180); // Offset 180
		write8((byte) 0); // Padding
		write8((byte) 0); // Padding
		write8((byte) 0); // Padding
		write8((byte) 0); // Padding
		write8((byte) 0); // Padding
		writeStringNZ(8, str196); // Offset 196
		writeStringNZ(8, unk204); // Offset 204
		write32(unk212); // Offset 212
		write32(unk216); // Offset 216
	}
}
