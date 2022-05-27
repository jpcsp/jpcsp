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
package jpcsp.memory.mmio;

import java.io.IOException;

import jpcsp.Processor;
import jpcsp.Allegrex.compiler.RuntimeContextLLE;
import jpcsp.state.StateInputStream;
import jpcsp.state.StateOutputStream;

public class MMIOHandlerProxyOnCpu implements IMMIOHandler {
	private IMMIOHandler proxyOnMain;
	private IMMIOHandler proxyOnMe;

	public MMIOHandlerProxyOnCpu(IMMIOHandler proxyOnMain, IMMIOHandler proxyOnMe) {
		this.proxyOnMain = proxyOnMain;
		this.proxyOnMe = proxyOnMe;
	}

	public IMMIOHandler getInstance() {
		if (RuntimeContextLLE.isMediaEngineCpu()) {
			return proxyOnMe;
		}
		return proxyOnMain;
	}

	public IMMIOHandler getInstance(Processor processor) {
		if (processor.cp0.isMediaEngineCpu()) {
			return proxyOnMe;
		}
		return proxyOnMain;
	}

	@Override
	public int read8(int address) {
		return getInstance().read8(address);
	}

	@Override
	public int read16(int address) {
		return getInstance().read16(address);
	}

	@Override
	public int read32(int address) {
		return getInstance().read32(address);
	}

	@Override
	public int internalRead8(int address) {
		return getInstance().internalRead8(address);
	}

	@Override
	public int internalRead16(int address) {
		return getInstance().internalRead16(address);
	}

	@Override
	public int internalRead32(int address) {
		return getInstance().internalRead32(address);
	}

	@Override
	public void write8(int address, byte value) {
		getInstance().write8(address, value);
	}

	@Override
	public void write16(int address, short value) {
		getInstance().write16(address, value);
	}

	@Override
	public void write32(int address, int value) {
		getInstance().write32(address, value);
	}

	@Override
	public void read(StateInputStream stream) throws IOException {
		proxyOnMain.read(stream);
		proxyOnMe.read(stream);
	}

	@Override
	public void write(StateOutputStream stream) throws IOException {
		proxyOnMain.write(stream);
		proxyOnMe.write(stream);
	}

	@Override
	public void reset() {
		proxyOnMain.reset();
		proxyOnMe.reset();
	}
}
