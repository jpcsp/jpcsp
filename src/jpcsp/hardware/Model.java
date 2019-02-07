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
package jpcsp.hardware;

import org.apache.log4j.Logger;

import jpcsp.Emulator;

public class Model {
	public static Logger log = Emulator.log;
	public static final int MODEL_PSP_FAT = 0;
	public static final int MODEL_PSP_SLIM = 1;
    public static final int MODEL_PSP_BRITE = 2;
    public static final int MODEL_PSP_BRITE2 = 3;
    public static final int MODEL_PSP_GO = 4;
	private static int model = MODEL_PSP_FAT;
	private static final ModelDescriptor modelDescriptors[] = new ModelDescriptor[5];

	private static class ModelDescriptor {
		public String modelName;
		public int generation;
		public int tachyonVersion;
		public int baryonVersion;
		public int pommelVersion;

		public ModelDescriptor(String modelName, int generation, int tachyonVersion, int baryonVersion, int pommelVersion) {
			this.modelName = modelName;
			this.generation = generation;
			this.tachyonVersion = tachyonVersion;
			this.baryonVersion = baryonVersion;
			this.pommelVersion = pommelVersion;
		}
	}

	static {
    	// See https://playstationdev.wiki/pspdevwiki/index.php?title=Motherboards#Tachyon_.2F_Baryon_.2F_Pommel.27s
		// Tachyon = 0x00140000, Baryon = 0x00030600, Pommel = 0x00000103 TA-079 v1 1g
		// Tachyon = 0x00200000, Baryon = 0x00030600, Pommel = 0x00000103 TA-079 v2 1g
		// Tachyon = 0x00200000, Baryon = 0x00040600, Pommel = 0x00000103 TA-079 v3 1g
		// Tachyon = 0x00300000, Baryon = 0x00040600, Pommel = 0x00000103 TA-081 1g
		// Tachyon = 0x00400000, Baryon = 0x00114000, Pommel = 0x00000112 TA-082 1g
		// Tachyon = 0x00400000, Baryon = 0x00121000, Pommel = 0x00000112 TA-086 1g
		// Tachyon = 0x00500000, Baryon = 0x0022B200, Pommel = 0x00000123 TA-085 v1 2g
		// Tachyon = 0x00500000, Baryon = 0x00234000, Pommel = 0x00000123 TA-085 v2 2g
		// Tachyon = 0x00500000, Baryon = 0x00243000, Pommel = 0x00000123 TA-088 v1, v2a 2g
		// Tachyon = ??????????, Baryon = ??????????, Pommel = ?????????? TA-088 v2b 2g
		// Tachyon = 0x00600000, Baryon = 0x00243000, Pommel = 0x00000123 TA-088 v3 2g
		// Tachyon = 0x00500000, Baryon = 0x00263100, Pommel = 0x00000132 TA-090 v1 2g
		// Tachyon = 0x00600000, Baryon = 0x00263100, Pommel = 0x00000132 TA-090 v2 3g
		// Tachyon = ??????????, Baryon = ??????????, Pommel = ?????????? TA-090 v3 3g
		// Tachyon = 0x00720000, Baryon = 0x00304000, Pommel = 0x00000133 TA-091 5g (Go N1000)
		// Tachyon = 0x00600000, Baryon = 0x00285000, Pommel = 0x00000133 TA-092 3g
		// Tachyon = 0x00810000, Baryon = 0x002C4000, Pommel = 0x00000141 TA-093 4g
		// Tachyon = ??????????, Baryon = ??????????, Pommel = ?????????? TA-094 5g (Go N1000)
		// Tachyon = ??????????, Baryon = ??????????, Pommel = ?????????? TA-095 v1 7g/9g
		// Tachyon = 0x00820000, Baryon = 0x002E4000, Pommel = 0x00000154 TA-095 v2 7g/9g
		// Tachyon = ??????????, Baryon = ??????????, Pommel = ?????????? TA-096 11g (Street E1000)
		modelDescriptors[MODEL_PSP_FAT   ] = new ModelDescriptor("MODEL_PSP_FAT"   , 1, 0x00300000, 0x00040600, 0x00000103);
		modelDescriptors[MODEL_PSP_SLIM  ] = new ModelDescriptor("MODEL_PSP_SLIM"  , 2, 0x00500000, 0x0022B200, 0x00000123);
		modelDescriptors[MODEL_PSP_BRITE ] = new ModelDescriptor("MODEL_PSP_BRITE" , 3, 0x00600000, 0x00263100, 0x00000132);
		modelDescriptors[MODEL_PSP_BRITE2] = new ModelDescriptor("MODEL_PSP_BRITE2", 9, 0x00820000, 0x002E4000, 0x00000154);
		modelDescriptors[MODEL_PSP_GO    ] = new ModelDescriptor("MODEL_PSP_GO"    , 5, 0x00720000, 0x00304000, 0x00000133);
	}

	public static int getModel() {
		return model;
	}

	public static void setModel(int model) {
		Model.model = model;
	}

	public static String getModelName(int model) {
		return modelDescriptors[model].modelName;
	}

	public static String getModelName() {
		return getModelName(model);
	}

	public static int getGeneration() {
		return modelDescriptors[model].generation;
	}

	public static int getTachyonVersion() {
		return modelDescriptors[model].tachyonVersion;
	}

	public static int getBaryonVersion() {
		return modelDescriptors[model].baryonVersion;
	}

	public static int getPommelVersion() {
		return modelDescriptors[model].pommelVersion;
	}
}
