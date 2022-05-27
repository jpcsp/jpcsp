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

import jpcsp.util.Utilities;

// Based on https://github.com/RPCS3/rpcs3/blob/master/rpcs3/Emu/PSP2/Modules/sceNpCommon.h
public class SceNpAuthRequestParameter extends pspAbstractMemoryMappedStructureVariableLength {
	public int ticketVersionMajor;
	public int ticketVersionMinor;
	public String serviceId;
	public int serviceIdAddr;
	public int cookie;
	public int cookieSize;
	public String entitlementId;
	public int entitlementIdAddr;
	public int consumedCount;
	public int ticketCallback;
	public int callbackArgument;

	@Override
	protected void read() {
		super.read();
		ticketVersionMajor = read16();
		ticketVersionMinor = read16();
		serviceIdAddr = read32();
		cookie = read32();
		cookieSize = read32();
		entitlementIdAddr = read32();
		consumedCount = read32();
		ticketCallback = read32();
		callbackArgument = read32();

		if (serviceIdAddr != 0) {
			serviceId = Utilities.readStringZ(serviceIdAddr);
		} else {
			serviceId = null;
		}
		if (entitlementIdAddr != 0) {
			entitlementId = Utilities.readStringZ(entitlementIdAddr);
		} else {
			entitlementId = null;
		}
	}

	@Override
	protected void write() {
		super.write();
		write16((short) ticketVersionMajor);
		write16((short) ticketVersionMinor);
		write32(serviceIdAddr);
		write32(cookie);
		write32(cookieSize);
		write32(entitlementIdAddr);
		write32(consumedCount);
		write32(ticketCallback);
		write32(callbackArgument);
	}

	@Override
	public String toString() {
		return String.format("serviceId='%s', cookie=0x%08X(size=0x%X), entitlementId='%s', consumedCount=0x%X, ticketCallback=0x%08X, callbackArgument=0x%08X", serviceId == null ? "" : serviceId, cookie, cookieSize, entitlementId == null ? "" : entitlementId, consumedCount, ticketCallback, callbackArgument);
	}
}
