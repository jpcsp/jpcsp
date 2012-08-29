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

import static jpcsp.HLE.kernel.types.SceKernelErrors.ERROR_NET_RESOLVER_BAD_ID;
import jpcsp.HLE.CheckArgument;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.HLELogging;
import jpcsp.HLE.HLEUnimplemented;
import jpcsp.HLE.PspString;
import jpcsp.HLE.SceKernelErrorException;
import jpcsp.HLE.TPointer;
import jpcsp.HLE.TPointer32;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;

import org.apache.log4j.Logger;

import jpcsp.HLE.kernel.managers.SceUidManager;
import jpcsp.HLE.kernel.types.SceKernelErrors;
import jpcsp.HLE.Modules;
import jpcsp.HLE.modules.HLEModule;
import jpcsp.HLE.modules.sceNetInet;

import jpcsp.Processor;

@HLELogging
public class sceNetResolver extends HLEModule {
    public static Logger log = Modules.getLogger("sceNetResolver");
    private static final String uidPurpose = "sceNetResolver-NetResolver";

    @Override
	public String getName() {
		return "sceNetResolver";
	}

    protected static class ResolverID {
        private int id;
        private boolean isRunning;

        public ResolverID (int id, boolean running) {
            this.id = id;
            this.isRunning = running;
        }

        public int getID() {
            return id;
        }

        public boolean getIDStatus () {
            return isRunning;
        }

        public void stop() {
            isRunning = false;
        }
    }

    protected HashMap<Integer, ResolverID> RIDs = new HashMap<Integer, ResolverID>();

    public int checkRid(int rid) {
        if (!RIDs.containsKey(rid)) {
        	throw new SceKernelErrorException(ERROR_NET_RESOLVER_BAD_ID);
        }

        return rid;
    }

    /**
	 * Initialize the resolver library
	 *
	 * @return 0 on success, < 0 on error.
	 */
	@HLEFunction(nid = 0xF3370E61, version = 150)
	public int sceNetResolverInit() {
		return 0;
	}

	/**
	 * Terminate the resolver library
	 *
	 * @return 0 on success, < 0 on error
	 */
	@HLEFunction(nid = 0x6138194A, version = 150)
	public int sceNetResolverTerm() {
		return 0;
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
	@HLEFunction(nid = 0x244172AF, version = 150)
	public int sceNetResolverCreate(TPointer32 pRid, TPointer buffer, int bufferLength) {
        int newID = SceUidManager.getNewUid(uidPurpose);
        ResolverID newRID = new ResolverID(newID, true);
        RIDs.put(newID, newRID);
		pRid.setValue(newRID.getID());

		return 0;
	}

	/**
	 * Delete a resolver
	 *
	 * @param rid - The resolver to delete
	 *
	 * @return 0 on success, < 0 on error
	 */
	@HLEFunction(nid = 0x94523E09, version = 150)
	public int sceNetResolverDelete(@CheckArgument("checkRid") int rid) {
		RIDs.remove(rid);
		SceUidManager.releaseUid(rid, uidPurpose);

		return 0;
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
	@HLEFunction(nid = 0x224C5F44, version = 150)
	public int sceNetResolverStartNtoA(@CheckArgument("checkRid") int rid, PspString hostname, TPointer32 addr, int timeout, int retry) {
		try {
			InetAddress inetAddress = InetAddress.getByName(hostname.getString());
			int resolvedAddress = sceNetInet.bytesToInternetAddress(inetAddress.getAddress());
			addr.setValue(resolvedAddress);
			if (log.isDebugEnabled()) {
				log.debug(String.format("sceNetResolverStartNtoA returning address 0x%08X('%s')", resolvedAddress, sceNetInet.internetAddressToString(resolvedAddress)));
			} else if (log.isInfoEnabled()) {
				log.info(String.format("sceNetResolverStartNtoA resolved '%s' into '%s'", hostname.getString(), sceNetInet.internetAddressToString(resolvedAddress)));
			}
		} catch (UnknownHostException e) {
			log.error(e);
			return SceKernelErrors.ERROR_NET_RESOLVER_INVALID_HOST;
		}

		return 0;
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
	@HLEFunction(nid = 0x629E2FB7, version = 150)
	public int sceNetResolverStartAtoN(@CheckArgument("checkRid") int rid, int addr, TPointer hostnameAddr, int hostnameLength, int timeout, int retry) {
		try {
			byte[] bytes = sceNetInet.internetAddressToBytes(addr);
			InetAddress inetAddress = InetAddress.getByAddress(bytes);
			String hostName = inetAddress.getHostName();
			hostnameAddr.setStringNZ(hostnameLength, hostName);
			if (log.isDebugEnabled()) {
				log.debug(String.format("sceNetResolverStartAtoN returning host name '%s'", hostName));
			}
		} catch (UnknownHostException e) {
			log.error(e);
			return SceKernelErrors.ERROR_NET_RESOLVER_INVALID_HOST;
		}

		return 0;
	}

	/**
	 * Stop a resolver operation
	 *
	 * @param rid - Resolver id
	 *
	 * @return 0 on success, < 0 on error
	 */
	@HLEFunction(nid = 0x808F6063, version = 150)
	public int sceNetResolverStop(@CheckArgument("checkRid") int rid) {
        ResolverID currentRID = RIDs.get(rid);
        if (!currentRID.getIDStatus()) {
        	return SceKernelErrors.ERROR_NET_RESOLVER_ALREADY_STOPPED;
        }

        currentRID.stop();

        return 0;
	}

	@HLEUnimplemented
    @HLEFunction(nid = 0x14C17EF9, version = 150)
    public int sceNetResolverStartNtoAAsync() {
		return 0;
    }

	@HLEUnimplemented
    @HLEFunction(nid = 0xAAC09184, version = 150)
    public int sceNetResolverStartAtoNAsync() {
		return 0;
    }

	@HLEUnimplemented
    @HLEFunction(nid = 0x4EE99358, version = 150)
    public int sceNetResolverPollAsync() {
		return 0;
    }

	@HLEUnimplemented
    @HLEFunction(nid = 0x12748EB9, version = 150)
    public int sceNetResolverWaitAsync(Processor processor) {
		return 0;
    }
}