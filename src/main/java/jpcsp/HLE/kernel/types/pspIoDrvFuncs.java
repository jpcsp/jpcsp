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

import jpcsp.HLE.TPointerFunction;

public class pspIoDrvFuncs extends pspAbstractMemoryMappedStructure {
	public TPointerFunction ioInit;
	public TPointerFunction ioExit;
	public TPointerFunction ioOpen;
	public TPointerFunction ioClose;
	public TPointerFunction ioRead;
	public TPointerFunction ioWrite;
	public TPointerFunction ioLseek;
	public TPointerFunction ioIoctl;
	public TPointerFunction ioRemove;
	public TPointerFunction ioMkdir;
	public TPointerFunction ioRmdir;
	public TPointerFunction ioDopen;
	public TPointerFunction ioDclose;
	public TPointerFunction ioDread;
	public TPointerFunction ioGetstat;
	public TPointerFunction ioChstat;
	public TPointerFunction ioRename;
	public TPointerFunction ioChdir;
	public TPointerFunction ioMount;
	public TPointerFunction ioUmount;
	public TPointerFunction ioDevctl;
	public TPointerFunction ioCancel;

	@Override
	protected void read() {
		ioInit = readPointerFunction();
		ioExit = readPointerFunction();
		ioOpen = readPointerFunction();
		ioClose = readPointerFunction();
		ioRead = readPointerFunction();
		ioWrite = readPointerFunction();
		ioLseek = readPointerFunction();
		ioIoctl = readPointerFunction();
		ioRemove = readPointerFunction();
		ioMkdir = readPointerFunction();
		ioRmdir = readPointerFunction();
		ioDopen = readPointerFunction();
		ioDclose = readPointerFunction();
		ioDread = readPointerFunction();
		ioGetstat = readPointerFunction();
		ioChstat = readPointerFunction();
		ioRename = readPointerFunction();
		ioChdir = readPointerFunction();
		ioMount = readPointerFunction();
		ioUmount = readPointerFunction();
		ioDevctl = readPointerFunction();
		ioCancel = readPointerFunction();
	}

	@Override
	protected void write() {
		writePointerFunction(ioInit);
		writePointerFunction(ioExit);
		writePointerFunction(ioOpen);
		writePointerFunction(ioClose);
		writePointerFunction(ioRead);
		writePointerFunction(ioWrite);
		writePointerFunction(ioLseek);
		writePointerFunction(ioIoctl);
		writePointerFunction(ioRemove);
		writePointerFunction(ioMkdir);
		writePointerFunction(ioRmdir);
		writePointerFunction(ioDopen);
		writePointerFunction(ioDclose);
		writePointerFunction(ioDread);
		writePointerFunction(ioGetstat);
		writePointerFunction(ioChstat);
		writePointerFunction(ioRename);
		writePointerFunction(ioChdir);
		writePointerFunction(ioMount);
		writePointerFunction(ioUmount);
		writePointerFunction(ioDevctl);
		writePointerFunction(ioCancel);
	}

	@Override
	public int sizeof() {
		return 88;
	}

	private static void toString(StringBuilder s, String name, TPointerFunction addr) {
		if (addr.isNotNull()) {
			if (s.length() > 0) {
				s.append(", ");
			}
			s.append(String.format("%s=%s", name, addr));
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
		toString(s, "ioCancel", ioCancel);

		return s.toString();
	}
}
