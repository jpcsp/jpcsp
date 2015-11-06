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

public class SceKernelLoadExecVSHParam extends pspAbstractMemoryMappedStructureVariableLength {
	public int args;
	public int argp;
	public int keyAddr;
	public String key;
	public int vshmainArgsSize;
	public int vshmainArgs;
	public int configFileAddr;
	public String configFile;
	public int unknown1;
	public int unknown2;

	@Override
	protected void read() {
		super.read();
		args = read32();
		argp = read32();
		keyAddr = read32();
		vshmainArgsSize = read32();
		vshmainArgs = read32();
		configFileAddr = read32();
		unknown1 = read32();
		unknown2 = read32();

		key = readStringZ(keyAddr);
		configFile = readStringZ(configFileAddr);
	}

	@Override
	protected void write() {
		super.write();
		write32(args);
		write32(argp);
		write32(keyAddr);
		write32(vshmainArgsSize);
		write32(vshmainArgs);
		write32(configFileAddr);
		write32(unknown1);
		write32(unknown2);
	}

	@Override
	public String toString() {
		return String.format("args=0x%X, argp=0x%08X, key=0x%08X('%s'), vshmainArgsSize=0x%X, vshmainArgs=0x%08X, configFile=0x%08X('%s'), unknown1=0x%X, unknown2=0x%X", args, argp, keyAddr, key, vshmainArgsSize, vshmainArgs, configFileAddr, configFile, unknown1, unknown2);
	}
}
