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
package jpcsp.util;

import jpcsp.memory.IMemoryReader;
import jpcsp.memory.MemoryReader;

public class Hash {
	// 256 Random values
	private static final int[] salt = {
		0x3A5601D2, 0x2B3DCB11, 0xEA39A63C, 0x552CBCBF,
		0xEDBAFEB2, 0xD9681D5E, 0x6BCE43BC, 0xF35B9D44,
		0x13D021E5, 0x57967C85, 0xECD08AB0, 0x0668A455,
		0x2C57D2BC, 0x51CC3E09, 0x1D398CA5, 0xF4F67487,
		0xD0492F70, 0xF2815B60, 0x427E687E, 0x333E8EED,
		0x837A4C56, 0x78D8131E, 0x205C3ECD, 0xB4DBD2E2,
		0xD752B250, 0x8728F852, 0x287E9763, 0xB256B605,
		0x9D82D087, 0x75361430, 0xA5AE0C29, 0x4CEE7885,
		0x90545556, 0xC33BFDEF, 0xD3BE4D31, 0xB10AD99E,
		0x06F2A289, 0x2D220CF4, 0xC94FD207, 0xF4761062,
		0xF649D0C6, 0x2D790930, 0x64EACC4D, 0x39370CBC,
		0x460DF64B, 0x0109FDC5, 0x893E2F86, 0x04E044B1,
		0xFC63A4D9, 0x53AD11EF, 0x65A6E231, 0x2EA791E9,
		0xBCBDD2EF, 0xB83A8C21, 0x2DD06C1A, 0xF8DFE36E,
		0xCF14A43B, 0x55E6636E, 0xEE81B5EB, 0x94B5C7B2,
		0x4EDBC54D, 0xDF79032A, 0x63BDAE09, 0x6D9C12D6,
		0x24564E8D, 0xEADE24D1, 0x2E88E69D, 0xACE62529,
		0x2251626F, 0x33ACE426, 0x342280F0, 0xB1E195EE,
		0x9D2DFAE4, 0xB7DA719F, 0x8E8FF9FB, 0x07994661,
		0x51B59A13, 0x14FB0700, 0x5C40AC3E, 0x3B8820FC,
		0xB1BCD248, 0x1C8B0245, 0x13871AD0, 0xF02208F6,
		0x1551D92C, 0x68F44AC4, 0x43F359B7, 0x6F7DBE0B,
		0xC0649A36, 0x61A26493, 0xF47B2779, 0xB5D2B882,
		0xB9B8FC61, 0xC4B9D626, 0xF7118BF2, 0x852A416C,
		0x9FCB4F1F, 0x8F2DC43D, 0x92191068, 0x33D34B29,
		0xB65A128D, 0x3238E7FC, 0x1338E4DC, 0xF21CCE30,
		0x82C78EE7, 0x0DED435B, 0x1ECE86A2, 0xA1D2AE0E,
		0x59B8EF3D, 0x65E037C0, 0x90BABC33, 0xB7356AAE,
		0x147FD366, 0x9D2EE2E9, 0x4FE1FA42, 0x27E521DB,
		0xCC368D35, 0xC470E60F, 0xAAA5860C, 0x1CBCA503,
		0x72467CEB, 0x14EDFB48, 0x611BE8F4, 0xE821A73E,
		0x9D9340EB, 0x2ACCFD2C, 0xEF24894E, 0xE71B478D,
		0x0FFCACB4, 0xE23944BB, 0x8DDB7C7A, 0x30D07B66,
		0xAC94BB0A, 0x03E5817E, 0xAEAFE635, 0xD2241D70,
		0xA305EF76, 0x2A216FEB, 0xE3B0792B, 0x523CA48A,
		0xFB79DAF1, 0xDB2A120D, 0x95C6ECCF, 0x2D21C711,
		0xE716B7DB, 0x05B24E64, 0xFBD47C6F, 0x237A8E62,
		0xEF265132, 0x441C1909, 0x3A2EC382, 0xC61A9E9D,
		0x6F67A8FF, 0x57B88B15, 0xBA1D8B44, 0x99B5B6AC,
		0x8A301213, 0xA1E78C43, 0xC9EF5D84, 0xEEFCAC83,
		0xBE17B0F3, 0x5355F2D4, 0xA943F0BC, 0xF5D769A8,
		0x70389810, 0x14233DB2, 0xD788C162, 0x4BA3ABF6,
		0x8AC6F33B, 0xE8A64CD8, 0xED956C7D, 0xF4988E9D,
		0xE13BF657, 0x901ABBF0, 0x741A9F7A, 0x43EB1F6C,
		0x78B4834F, 0xF2F6C33D, 0x106FB13F, 0x1D508452,
		0x67BFBB4A, 0xF843C2BB, 0x65783880, 0x693E7521,
		0x7BE8EF1D, 0x3251DB84, 0x181C3352, 0xC4130D95,
		0xA80BA301, 0xD5C13B74, 0x4B6293FD, 0xDB593B95,
		0xD4379985, 0xD061FF10, 0x78E74715, 0x93EF4DB8,
		0x99D619C5, 0x3215CDA3, 0xF46306CB, 0xBC29660A,
		0xD420E4DC, 0x6EEC93A9, 0x6D8C8F80, 0x7E61D50F,
		0x790E8E96, 0xF167FF71, 0xF800099B, 0x0F05A107,
		0xA0DF2D6A, 0x8467A3FD, 0x2E52BC9B, 0xBD4BBD73,
		0xE5AF95A7, 0xF7F9E62C, 0xE9465F6F, 0xF3A9B6D7,
		0xB82893EC, 0x196C181A, 0x4F709A04, 0x2D00D6C2,
		0xFDC2DADC, 0x57B666C7, 0x19E08C20, 0x09AB040B,
		0x4F6F9812, 0xD0067DF6, 0x48956D6E, 0xA43EE35C,
		0xB800E453, 0xBCF60657, 0xF3369AD9, 0xED56FBF4,
		0x8FAC8302, 0x7C2861E7, 0xC8AEA51C, 0x181FE4A8,
		0x68B6A4D8, 0x53DD4A97, 0x6050349A, 0x2B2DBD2A,
		0x47F9A1D3, 0x7ABC372B, 0x0244D66A, 0x4A59778D,
		0x25DADE6F, 0x69F6AA65, 0xA07306B6, 0xBB6AB608,
		0xF08C5013, 0x631DD062, 0x8E0A0C45, 0x785B40F9,
		0xB27D54CE, 0x21EA0E43, 0x1FB77955, 0x134D592D
	};

	/**
	 * Generate a hashCode on a memory range using a rather simple but fast method.
	 * 
	 * @param hashCode		current hashCode value
	 * @param addr			start of the memory range to be hashed
	 * @param lengthInBytes	length of the memory range
	 * @return updated hashCode value
	 */
	public static int getHashCode(int hashCode, int addr, int lengthInBytes) {
		IMemoryReader memoryReader = MemoryReader.getMemoryReader(addr, lengthInBytes, 4);
		for (int i = 0, j = 0; i < lengthInBytes; i += 4, j++) {
			int value = memoryReader.readNext();
			hashCode ^= value + salt[j & 0xFF];
			hashCode += i + addr;
		}

		return hashCode;
	}

	/**
	 * Generate a hashCode on a memory range using a rather simple but fast method
	 * and a stride.
	 * 
	 * @param hashCode		current hashCode value
	 * @param addr			start of the memory range to be hashed
	 * @param lengthInBytes	length of the memory range
	 * @param strideInBytes stride (hash only 4 bytes every stride bytes)
	 * @return updated hashCode value
	 */
	public static int getHashCode(int hashCode, int addr, int lengthInBytes, int strideInBytes) {
		if (strideInBytes <= 4) {
			// There is no stride...
			return getHashCode(hashCode, addr, lengthInBytes);
		}

		int skip = (strideInBytes / 4) - 1;

		IMemoryReader memoryReader = MemoryReader.getMemoryReader(addr, lengthInBytes, 4);
		int step = (skip + 1) * 4;
		lengthInBytes -= lengthInBytes % strideInBytes;
		for (int i = 0, j = 0; i < lengthInBytes; i += step, j++) {
			int value = memoryReader.readNext();
			memoryReader.skip(skip);
			hashCode ^= value + salt[j & 0xFF];
			hashCode += i + addr;
		}

		return hashCode;
	}

	/**
	 * Generate a hashCode on a memory range using a more complex but slower method.
	 * 
	 * @param hashCode		current hashCode value
	 * @param addr			start of the memory range to be hashed
	 * @param lengthInBytes	length of the memory range
	 * @return updated hashCode value
	 */
	public static int getHashCodeComplex(int hashCode, int addr, int lengthInBytes) {
		IMemoryReader memoryReader = MemoryReader.getMemoryReader(addr, lengthInBytes, 4);
		int n = lengthInBytes / 4;
		for (int i = 0; i < n; i++) {
			int value = memoryReader.readNext();
			value = Integer.rotateLeft(value, i & 31);
			hashCode ^= value + i + addr;
			hashCode += i + addr;
		}

		return hashCode;
	}

	/**
	 * Generate a hashCode on a memory range using a more complex but slower method.
	 * This method also uses a stride to scan only parts of the memory range.
	 * 
	 * @param hashCode		current hashCode value
	 * @param addr			start of the memory range to be hashed
	 * @param lengthInBytes	length of the memory range
	 * @param strideInBytes stride (hash only 4 bytes every stride bytes)
	 * @return updated hashCode value
	 */
	public static int getHashCodeComplex(int hashCode, int addr, int lengthInBytes, int strideInBytes) {
		if (strideInBytes <= 4) {
			// There is no stride...
			return getHashCodeComplex(hashCode, addr, lengthInBytes);
		}

		int skip = (strideInBytes / 4) - 1;

		IMemoryReader memoryReader = MemoryReader.getMemoryReader(addr, lengthInBytes, 4);
		int n = lengthInBytes / strideInBytes;
		for (int i = 0; i < n; i++) {
			int value = memoryReader.readNext();
			memoryReader.skip(skip);
			value = Integer.rotateLeft(value, i & 31);
			hashCode ^= value + i + addr;
			hashCode += i + addr;
		}

		return hashCode;
	}

	/**
	 * Generate a hashCode on a memory range using a rather simple but fast method.
	 * The hashCode will be independent of the address, i.e. the same hashCode will
	 * be generated for the same data at different memory addresses.
	 * 
	 * @param hashCode		current hashCode value
	 * @param addr			start of the memory range to be hashed
	 * @param lengthInBytes	length of the memory range
	 * @return updated hashCode value
	 */
	public static int getHashCodeFloatingMemory(int hashCode, int addr, int lengthInBytes) {
		IMemoryReader memoryReader = MemoryReader.getMemoryReader(addr, lengthInBytes, 4);
		return getHashCodeFloatingMemory(hashCode, memoryReader, lengthInBytes);
	}

	/**
	 * Generate a hashCode on a memory range using a rather simple but fast method.
	 * The hashCode will be independent of the address, i.e. the same hashCode will
	 * be generated for the same data at different memory addresses.
	 * 
	 * @param hashCode		current hashCode value
	 * @param memoryReader	the memory reader for the values to be hashed
	 * @param lengthInBytes	length of the memory range
	 * @return updated hashCode value
	 */
	public static int getHashCodeFloatingMemory(int hashCode, IMemoryReader memoryReader, int lengthInBytes) {
		for (int i = 0; i < lengthInBytes; i += 4) {
			int value = memoryReader.readNext();
			hashCode ^= value + i;
			hashCode += i;
		}

		return hashCode;
	}
}
