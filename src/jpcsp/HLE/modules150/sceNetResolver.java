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
package jpcsp.HLE.modules150;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.log4j.Logger;

import jpcsp.HLE.Modules;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.HLE.modules.HLEModuleFunction;
import jpcsp.HLE.modules.HLEModuleManager;
import jpcsp.HLE.modules.sceNetInet;
import jpcsp.util.Utilities;

import jpcsp.Memory;
import jpcsp.Processor;

import jpcsp.Allegrex.CpuState;

public class sceNetResolver implements HLEModule {
    protected static Logger log = Modules.getLogger("sceNetResolver");

    @Override
	public String getName() {
		return "sceNetResolver";
	}
	
	@Override
	public void installModule(HLEModuleManager mm, int version) {
		if (version >= 150) {
			mm.addFunction(sceNetResolverInitFunction, 0xF3370E61);
			mm.addFunction(sceNetResolverTermFunction, 0x6138194A);
			mm.addFunction(sceNetResolverCreateFunction, 0x244172AF);
			mm.addFunction(sceNetResolverDeleteFunction, 0x94523E09);
			mm.addFunction(sceNetResolverStartNtoAFunction, 0x224C5F44);
			mm.addFunction(sceNetResolverStartAtoNFunction, 0x629E2FB7);
			mm.addFunction(sceNetResolverStopFunction, 0x808F6063);
		}
	}
	
	@Override
	public void uninstallModule(HLEModuleManager mm, int version) {
		if (version >= 150) {
			mm.removeFunction(sceNetResolverInitFunction);
			mm.removeFunction(sceNetResolverTermFunction);
			mm.removeFunction(sceNetResolverCreateFunction);
			mm.removeFunction(sceNetResolverDeleteFunction);
			mm.removeFunction(sceNetResolverStartNtoAFunction);
			mm.removeFunction(sceNetResolverStartAtoNFunction);
			mm.removeFunction(sceNetResolverStopFunction);
		}
	}

	// TODO Implement multiple RID instances
	protected int dummyRid = 456;

	/**
	 * Inititalise the resolver library
	 *
	 * @return 0 on sucess, < 0 on error.
	 */
	public void sceNetResolverInit(Processor processor) {
		CpuState cpu = processor.cpu;

		if (log.isDebugEnabled()) {
			log.warn("sceNetResolverInit");
		}

		cpu.gpr[2] = 0;
	}

	/**
	 * Terminate the resolver library
	 *
	 * @return 0 on success, < 0 on error
	 */
	public void sceNetResolverTerm(Processor processor) {
		CpuState cpu = processor.cpu;

		if (log.isDebugEnabled()) {
			log.debug("sceNetResolverTerm");
		}

		cpu.gpr[2] = 0;
	}

	/**
	 * Create a resolver object
	 *
	 * @param rid - Pointer to receive the resolver id
	 * @param buf - Temporary buffer
	 * @param buflen - Length of the temporary buffer
	 *
	 * @return 0 on success, < 0 on error
	 */
	public void sceNetResolverCreate(Processor processor) {
		CpuState cpu = processor.cpu;
		Memory mem = Processor.memory;

		int pRid = cpu.gpr[4];
		int buffer = cpu.gpr[5];
		int bufferLength = cpu.gpr[6];

		if (log.isDebugEnabled()) {
			log.debug(String.format("sceNetResolverCreate pRid=0x%08X, buffer=0x%08X, bufferLength=5d", pRid, buffer, bufferLength));
		}

		mem.write32(pRid, dummyRid);

		cpu.gpr[2] = 0;
	}

	/**
	 * Delete a resolver
	 *
	 * @param rid - The resolver to delete
	 *
	 * @return 0 on success, < 0 on error
	 */
	public void sceNetResolverDelete(Processor processor) {
		CpuState cpu = processor.cpu;

		int rid = cpu.gpr[4];

		if (log.isDebugEnabled()) {
			log.debug(String.format("sceNetResolverDelete rid=%d", rid));
		}

		cpu.gpr[2] = 0;
	}

	/**
	 * Begin a name to address lookup
	 *
	 * @param rid - Resolver id
	 * @param hostname - Name to resolve
	 * @param addr - Pointer to in_addr structure to receive the address
	 * @param timeout - Number of seconds before timeout
	 * @param retry - Number of retires
	 *
	 * @return 0 on success, < 0 on error
	 */
	public void sceNetResolverStartNtoA(Processor processor) {
		CpuState cpu = processor.cpu;
		Memory mem = Processor.memory;

		int rid = cpu.gpr[4];
		int hostnameAddr = cpu.gpr[5];
		int addr = cpu.gpr[6];
		int timeout = cpu.gpr[7];
		int retry = cpu.gpr[8];
		String hostname = Utilities.readStringZ(hostnameAddr);

		if (log.isDebugEnabled()) {
			log.debug(String.format("sceNetResolverStartNtoA rid=%d, hostnameAddr=0x%08X('%s'), addr=0x%08X, timeout=%d, retry=%d", rid, hostnameAddr, hostname, addr, timeout, retry));
		}

		cpu.gpr[2] = 0;
		try {
			InetAddress inetAddress = InetAddress.getByName(hostname);
			int resolvedAddress = sceNetInet.bytesToInternetAddress(inetAddress.getAddress());
			mem.write32(addr, resolvedAddress);
			if (log.isDebugEnabled()) {
				log.debug(String.format("sceNetResolverStartNtoA returning address 0x%08X('%s')", resolvedAddress, sceNetInet.internetAddressToString(resolvedAddress)));
			} else if (log.isInfoEnabled()) {
				log.info(String.format("sceNetResolverStartNtoA resolved '%s' into '%s'", hostname, sceNetInet.internetAddressToString(resolvedAddress)));
			}
		} catch (UnknownHostException e) {
			log.error(e);
			cpu.gpr[2] = -1;
		}
	}

	/**
	 * Begin a address to name lookup
	 *
	 * @param rid -Resolver id
	 * @param addr - Pointer to the address to resolve
	 * @param hostname - Buffer to receive the name
	 * @param hostname_len - Length of the buffer
	 * @param timeout - Number of seconds before timeout
	 * @param retry - Number of retries
	 *
	 * @return 0 on success, < 0 on error
	 */
	public void sceNetResolverStartAtoN(Processor processor) {
		CpuState cpu = processor.cpu;
		Memory mem = Processor.memory;

		int rid = cpu.gpr[4];
		int addr = cpu.gpr[5];
		int hostnameAddr = cpu.gpr[6];
		int hostnameLength = cpu.gpr[7];
		int timeout = cpu.gpr[8];
		int retry = cpu.gpr[9];

		if (log.isDebugEnabled()) {
			log.debug(String.format("sceNetResolverStartAtoN rid=%d, addr=0x%08X, hostnameAddr=0x%08X, hostnameLength=%d, timeout=%d, retry=%d", rid, addr, hostnameAddr, hostnameLength, timeout, retry));
		}

		cpu.gpr[2] = 0;
		try {
			byte[] bytes = sceNetInet.internetAddressToBytes(addr);
			InetAddress inetAddress = InetAddress.getByAddress(bytes);
			String hostName = inetAddress.getHostName();
			Utilities.writeStringNZ(mem, hostnameAddr, hostnameLength, hostName);
			if (log.isDebugEnabled()) {
				log.debug(String.format("sceNetResolverStartAtoN returning host name '%s'", hostName));
			}
		} catch (UnknownHostException e) {
			log.error(e);
			cpu.gpr[2] = -1;
		}
	}

	/**
	 * Stop a resolver operation
	 *
	 * @param rid - Resolver id
	 *
	 * @return 0 on success, < 0 on error
	 */
	public void sceNetResolverStop(Processor processor) {
		CpuState cpu = processor.cpu;

		log.warn("Unimplemented NID function sceNetResolverStop [0x808F6063]");

		cpu.gpr[2] = 0xDEADC0DE;
	}

	public final HLEModuleFunction sceNetResolverInitFunction = new HLEModuleFunction("sceNetResolver", "sceNetResolverInit") {
		@Override
		public final void execute(Processor processor) {
			sceNetResolverInit(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceNetResolverModule.sceNetResolverInit(processor);";
		}
	};

	public final HLEModuleFunction sceNetResolverTermFunction = new HLEModuleFunction("sceNetResolver", "sceNetResolverTerm") {
		@Override
		public final void execute(Processor processor) {
			sceNetResolverTerm(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceNetResolverModule.sceNetResolverTerm(processor);";
		}
	};

	public final HLEModuleFunction sceNetResolverCreateFunction = new HLEModuleFunction("sceNetResolver", "sceNetResolverCreate") {
		@Override
		public final void execute(Processor processor) {
			sceNetResolverCreate(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceNetResolverModule.sceNetResolverCreate(processor);";
		}
	};

	public final HLEModuleFunction sceNetResolverDeleteFunction = new HLEModuleFunction("sceNetResolver", "sceNetResolverDelete") {
		@Override
		public final void execute(Processor processor) {
			sceNetResolverDelete(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceNetResolverModule.sceNetResolverDelete(processor);";
		}
	};

	public final HLEModuleFunction sceNetResolverStartNtoAFunction = new HLEModuleFunction("sceNetResolver", "sceNetResolverStartNtoA") {
		@Override
		public final void execute(Processor processor) {
			sceNetResolverStartNtoA(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceNetResolverModule.sceNetResolverStartNtoA(processor);";
		}
	};

	public final HLEModuleFunction sceNetResolverStartAtoNFunction = new HLEModuleFunction("sceNetResolver", "sceNetResolverStartAtoN") {
		@Override
		public final void execute(Processor processor) {
			sceNetResolverStartAtoN(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceNetResolverModule.sceNetResolverStartAtoN(processor);";
		}
	};

	public final HLEModuleFunction sceNetResolverStopFunction = new HLEModuleFunction("sceNetResolver", "sceNetResolverStop") {
		@Override
		public final void execute(Processor processor) {
			sceNetResolverStop(processor);
		}
		@Override
		public final String compiledString() {
			return "jpcsp.HLE.Modules.sceNetResolverModule.sceNetResolverStop(processor);";
		}
	};
};
