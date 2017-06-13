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

public class pspIoDrvFuncs extends pspAbstractMemoryMappedStructure {
	public int ioInit;
	public int ioExit;
	public int ioOpen;
	public int ioClose;
	public int ioRead;
	public int ioWrite;
	public int ioLseek;
	public int ioIoctl;
	public int ioRemove;
	public int ioMkdir;
	public int ioRmdir;
	public int ioDopen;
	public int ioDclose;
	public int ioDread;
	public int ioGetstat;
	public int ioChstat;
	public int ioRename;
	public int ioChdir;
	public int ioMount;
	public int ioUmount;
	public int ioDevctl;
	public int ioUnk21;

	@Override
	protected void read() {
		ioInit = read32();
		ioExit = read32();
		ioOpen = read32();
		ioClose = read32();
		ioRead = read32();
		ioWrite = read32();
		ioLseek = read32();
		ioIoctl = read32();
		ioRemove = read32();
		ioMkdir = read32();
		ioRmdir = read32();
		ioDopen = read32();
		ioDclose = read32();
		ioDread = read32();
		ioGetstat = read32();
		ioChstat = read32();
		ioRename = read32();
		ioChdir = read32();
		ioMount = read32();
		ioUmount = read32();
		ioDevctl = read32();
		ioUnk21 = read32();
	}

	@Override
	protected void write() {
		write32(ioInit);
		write32(ioExit);
		write32(ioOpen);
		write32(ioClose);
		write32(ioRead);
		write32(ioWrite);
		write32(ioLseek);
		write32(ioIoctl);
		write32(ioRemove);
		write32(ioMkdir);
		write32(ioRmdir);
		write32(ioDopen);
		write32(ioDclose);
		write32(ioDread);
		write32(ioGetstat);
		write32(ioChstat);
		write32(ioRename);
		write32(ioChdir);
		write32(ioMount);
		write32(ioUmount);
		write32(ioDevctl);
		write32(ioUnk21);
	}

	@Override
	public int sizeof() {
		return 88;
	}

	private static void toString(StringBuilder s, String name, int addr) {
		if (addr != 0) {
			if (s.length() > 0) {
				s.append(", ");
			}
			s.append(String.format("%s=0x%08X", name, addr));
		}
	}

	@Override
	public String toString() {
		StringBuilder s = new StringBuilder();

		toString(s, "ioInit", ioInit);
		toString(s, "ioExit", ioExit);
		toString(s, "ioOpen", ioOpen);
		toString(s, "ioClose", ioClose);
		toString(s, "ioRead", ioRead);
		toString(s, "ioWrite", ioWrite);
		toString(s, "ioLseek", ioLseek);
		toString(s, "ioIoctl", ioIoctl);
		toString(s, "ioRemove", ioRemove);
		toString(s, "ioMkdir", ioMkdir);
		toString(s, "ioRmdir", ioRmdir);
		toString(s, "ioDopen", ioDopen);
		toString(s, "ioDclose", ioDclose);
		toString(s, "ioDread", ioDread);
		toString(s, "ioGetstat", ioGetstat);
		toString(s, "ioChstat", ioChstat);
		toString(s, "ioRename", ioRename);
		toString(s, "ioChdir", ioChdir);
		toString(s, "ioMount", ioMount);
		toString(s, "ioUmount", ioUmount);
		toString(s, "ioDevctl", ioDevctl);
		toString(s, "ioUnk21", ioUnk21);

		return s.toString();
	}
}
