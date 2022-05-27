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

import jpcsp.HLE.TPointer;
import jpcsp.util.Utilities;

public class SceKernelLoadExecVSHParam extends pspAbstractMemoryMappedStructureVariableLength {
	public int args;
	public int argp;
	public TPointer keyAddr;
	public String key;
	public int vshmainArgsSize;
	public int vshmainArgs;
	public TPointer configFileAddr;
	public String configFile;
	public int unknownString;
	public int flags;
	public int extArgs;
	public int extArgp;
	public int opt11;

	@Override
	protected void read() {
		super.read();
		args = read32();
		argp = read32();
		keyAddr = readPointer();
		vshmainArgsSize = read32();
		vshmainArgs = read32();
		configFileAddr = readPointer();
		unknownString = read32();
		flags = read32();

		if (sizeof() >= 48) {
			extArgs = read32();
			extArgp = read32();
			opt11 = read32();
		}
		key = keyAddr.getStringZ();
		configFile = configFileAddr.getStringZ();
	}

	@Override
	protected void write() {
		super.write();
		write32(args);
		write32(argp);
		writePointer(keyAddr);
		write32(vshmainArgsSize);
		write32(vshmainArgs);
		writePointer(configFileAddr);
		write32(unknownString);
		write32(flags);

		if (sizeof() >= 48) {
			write32(extArgs);
			write32(extArgp);
			write32(opt11);
		}
	}

	@Override
	public String toString() {
		return String.format("args=0x%X, argp=0x%08X, key=%s('%s'), vshmainArgsSize=0x%X, vshmainArgs=0x%08X, configFile=%s('%s'), unknownString=0x%08X, flags=0x%X, extArgs=0x%X, extArgp=0x%08X, opt11=0x%X, vshmainArgs: %s", args, argp, keyAddr, key, vshmainArgsSize, vshmainArgs, configFileAddr, configFile, unknownString, flags, extArgs, extArgp, opt11, vshmainArgs == 0 ? "" : Utilities.getMemoryDump(vshmainArgs, vshmainArgsSize));
	}
}
