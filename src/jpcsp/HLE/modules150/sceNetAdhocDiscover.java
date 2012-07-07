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

import static jpcsp.HLE.kernel.types.SceNetAdhocDiscoverParam.NET_ADHOC_DISCOVER_RESULT_PEER_FOUND;
import jpcsp.HLE.HLEFunction;
import jpcsp.HLE.TPointer;
import jpcsp.Emulator;
import jpcsp.Memory;
import jpcsp.Processor;
import jpcsp.HLE.Modules;
import jpcsp.HLE.kernel.types.SceNetAdhocDiscoverParam;
import jpcsp.HLE.modules.HLEModule;

import org.apache.log4j.Logger;

public class sceNetAdhocDiscover extends HLEModule {
    protected static Logger log = Modules.getLogger("sceNetAdhocDiscover");
    protected static final int NET_ADHOC_DISCOVER_STATUS_NONE = 0;
    protected static final int NET_ADHOC_DISCOVER_STATUS_IN_PROGRESS = 1;
    protected static final int NET_ADHOC_DISCOVER_STATUS_COMPLETED = 2;
    protected int status;
    protected SceNetAdhocDiscoverParam netAdhocDiscoverParam;
    protected long discoverStartMillis;
    protected static final int DISCOVER_DURATION_MILLIS = 2000;

    @Override
    public String getName() {
        return "sceNetAdhocDiscover";
    }

	@Override
	public void start() {
		status = NET_ADHOC_DISCOVER_STATUS_NONE;

		super.start();
	}

    @HLEFunction(nid = 0x941B3877, version = 150)
    public int sceNetAdhocDiscoverInitStart(TPointer param) {
    	netAdhocDiscoverParam = new SceNetAdhocDiscoverParam();
    	netAdhocDiscoverParam.read(Memory.getInstance(), param.getAddress());
    	log.warn(String.format("PARTIAL sceNetAdhocDiscoverInitStart param=%s(%s)", param, netAdhocDiscoverParam));

    	status = NET_ADHOC_DISCOVER_STATUS_IN_PROGRESS;
    	discoverStartMillis = Emulator.getClock().currentTimeMillis();

    	return 0;
    }

    @HLEFunction(nid = 0x52DE1B97, version = 150)
    public int sceNetAdhocDiscoverUpdate() {
    	log.warn(String.format("PARTIAL sceNetAdhocDiscoverUpdate"));

    	if (status == NET_ADHOC_DISCOVER_STATUS_IN_PROGRESS) {
    		long now = Emulator.getClock().currentTimeMillis();
    		if (now >= discoverStartMillis + DISCOVER_DURATION_MILLIS) {
    			// Fake a successful completion after some time
    			status = NET_ADHOC_DISCOVER_STATUS_COMPLETED;
    			netAdhocDiscoverParam.result = NET_ADHOC_DISCOVER_RESULT_PEER_FOUND;
    		}
    	}
    	netAdhocDiscoverParam.write(Memory.getInstance());

    	return 0;
    }

    @HLEFunction(nid = 0x944DDBC6, version = 150)
    public int sceNetAdhocDiscoverGetStatus() {
    	log.warn(String.format("PARTIAL sceNetAdhocDiscoverGetStatus returning %d", status));

    	return status;
    }

    @HLEFunction(nid = 0xA2246614, version = 150)
    public int sceNetAdhocDiscoverTerm() {
        log.warn(String.format("PARTIAL sceNetAdhocDiscoverTerm"));
        status = NET_ADHOC_DISCOVER_STATUS_NONE;

        return 0;
    }

    @HLEFunction(nid = 0xF7D13214, version = 150)
    public int sceNetAdhocDiscoverStop() {
    	log.warn(String.format("PARTIAL sceNetAdhocDiscoverStop"));

    	status = NET_ADHOC_DISCOVER_STATUS_COMPLETED;
    	netAdhocDiscoverParam.write(Memory.getInstance());

    	return 0;
    }

    @HLEFunction(nid = 0xA423A21B, version = 150)
    public int sceNetAdhocDiscoverRequestSuspend(Processor processor) {
    	log.warn(String.format("Unimplemented sceNetAdhocDiscoverRequestSuspend"));

    	return 0;
    }
}